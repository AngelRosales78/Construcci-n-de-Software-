"""
Authentication views for the S&P 500 platform.

Implements all endpoints for Epica 01 (Web Auth):
- Register (HU01_01)
- Login with JWT (HU01_02)
- Password reset request (HU01_03)
- Password reset confirm
- Logout with Redis blacklist (HU01_04)
- Token refresh (HU01_05)
"""
import logging
from decimal import Decimal, localcontext
from rest_framework import status, generics
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated, IsAdminUser
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken, OutstandingToken, UntypedToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from django.contrib.auth import get_user_model
from django.contrib.auth.password_validation import validate_password
from django.core.exceptions import ValidationError as DjangoValidationError
from django.core.mail import send_mail
from django.db import transaction
from django.db.models import Q, F
from django.db.models.functions import Lower
from django.utils.html import strip_tags
from django.conf import settings
from django.urls import reverse
from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_encode, urlsafe_base64_decode
from django.utils.encoding import force_bytes, force_str
from django.utils import timezone
from django.utils.translation import gettext as _
from rest_framework.exceptions import ValidationError
from rest_framework_simplejwt.token_blacklist.models import BlacklistedToken

from .serializers import (
    RegisterSerializer,
    CustomTokenObtainPairSerializer,
    ChangePasswordSerializer,
    PasswordResetRequestSerializer,
    PasswordResetConfirmSerializer,
    UserProfileSerializer,
    CompanySerializer,
    SearchHistorySerializer,
)
from .redis_utils import blacklist_token, blacklist_jwt_token, get_autocomplete_suggestions
from .models import Company, SearchHistory, FinancialMetrics, PriceHistory

User = get_user_model()
logger = logging.getLogger(__name__)


# ─── HU01_01: User Registration ──────────────────────────────────────

class RegisterView(generics.CreateAPIView):
    """
    POST /api/v1/auth/register/

    Creates a user using the configured BCrypt-SHA256 password hasher.

    Request body:
    {
        "username": "john_doe",
        "email": "john@example.com",
        "password": "Str0ng!Pass#2024",
        "password_confirm": "Str0ng!Pass#2024",
        "first_name": "John",
        "last_name": "Doe",
        "user_type": "retail"
    }
    """
    permission_classes = (AllowAny,)
    serializer_class = RegisterSerializer

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        user = serializer.save()

        # Generate tokens for immediate login after registration
        refresh = CustomTokenObtainPairSerializer.get_token(user)

        return Response({
            'message': _('Registration successful. Welcome to S&P 500 Platform!'),
            'user': {
                'id': user.id,
                'username': user.username,
                'email': user.email,
                'first_name': user.first_name,
                'last_name': user.last_name,
                'user_type': user.user_type,
                'is_verified': user.is_verified,
                'role': 'admin' if user.is_staff or user.is_superuser else 'user',
            },
            'refresh': str(refresh),
            'access': str(refresh.access_token),
        }, status=status.HTTP_201_CREATED)


# ─── HU01_02: Login with JWT ─────────────────────────────────────────

class CustomTokenObtainPairView(TokenObtainPairView):
    """
    POST /api/v1/auth/token/

    Authenticates user and returns Access + Refresh JWT tokens.
    Also returns user profile data in the response.
    """
    serializer_class = CustomTokenObtainPairSerializer

    def post(self, request, *args, **kwargs):
        username_or_email = request.data.get('username', '')
        password = request.data.get('password', '')

        if not username_or_email or not password:
            return Response(
                {'error': 'Username and password are required.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Try to find user by username or email
        try:
            user = User.objects.get(
                Q(username=username_or_email) | Q(email=username_or_email)
            )
        except User.DoesNotExist:
            return Response(
                {'error': 'Invalid credentials.'},
                status=status.HTTP_401_UNAUTHORIZED
            )

        # Update last login IP
        client_ip = request.META.get('HTTP_X_FORWARDED_FOR',
                          request.META.get('REMOTE_ADDR', ''))
        if client_ip:
            user.last_login_ip = client_ip.split(',')[0].strip()
            user.save(update_fields=['last_login_ip'])

        # SimpleJWT authenticates with USERNAME_FIELD, even when an email was supplied.
        credentials = request.data.copy()
        credentials['username'] = user.username
        serializer = self.get_serializer(data=credentials)
        serializer.is_valid(raise_exception=True)
        return Response(serializer.validated_data, status=status.HTTP_200_OK)


# ─── HU01_03: Password Reset Request ─────────────────────────────────

class PasswordResetRequestView(generics.GenericAPIView):
    """
    POST /api/v1/auth/password/reset/

    Sends a password reset email with a token to the user's email address.
    """
    permission_classes = (AllowAny,)
    serializer_class = PasswordResetRequestSerializer

    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        email = serializer.validated_data['email']
        try:
            user = User.objects.get(email=email)
        except User.DoesNotExist:
            # Don't reveal if email exists
            return Response(
                {'message': 'If the email exists, a reset link has been sent.'},
                status=status.HTTP_200_OK
            )

        # Generate password reset token
        token = default_token_generator.make_token(user)
        uid = urlsafe_base64_encode(force_bytes(user.pk))

        # Build reset URL (frontend will handle the actual link)
        reset_url = f"{getattr(settings, 'FRONTEND_URL', 'http://localhost:5173')}/reset-password/{uid}/{token}"

        # Send email
        subject = 'Password Reset - S&P 500 Platform'
        html_message = f'''
            <html>
            <body>
                <h2>Password Reset Request</h2>
                <p>Click the link below to reset your password:</p>
                <a href="{reset_url}">Reset Password</a>
                <p>This link will expire in 15 minutes.</p>
                <p>If you did not request this, please ignore this email.</p>
            </body>
            </html>
        '''
        plain_message = f'Password reset link: {reset_url}'

        try:
            send_mail(
                subject=subject,
                message=plain_message,
                from_email=settings.DEFAULT_FROM_EMAIL,
                recipient_list=[user.email],
                html_message=html_message,
            )
            logger.info(f"Password reset email sent to {user.email}")
        except Exception as e:
            logger.error(f"Failed to send password reset email: {e}")

        return Response(
            {'message': 'If the email exists, a reset link has been sent.'},
            status=status.HTTP_200_OK
        )


class PasswordResetConfirmView(generics.GenericAPIView):
    """
    POST /api/v1/auth/password/reset/confirm/

    Confirms the password reset with a valid token and new password.
    """
    permission_classes = (AllowAny,)
    serializer_class = PasswordResetConfirmSerializer

    def post(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        token = serializer.validated_data['token']
        new_password = serializer.validated_data['new_password']

        # A user identifier alone must never authorize a password reset.
        if token.count(':') != 1:
            return Response(
                {'error': 'Invalid or expired token.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        uid_encoded, token_value = token.split(':', 1)
        try:
            uid = force_str(urlsafe_base64_decode(uid_encoded))
            if (not uid.isascii() or not uid.isdigit() or len(uid) > 19
                    or not 0 < int(uid) <= 2**63 - 1 or not token_value):
                raise ValueError('Invalid user identifier or token')
        except (ValueError, TypeError, UnicodeDecodeError, OverflowError):
            return Response(
                {'error': 'Invalid or expired token.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Serialize resets for the same user and revoke refresh tokens atomically.
        try:
            with transaction.atomic():
                user = User.objects.select_for_update().get(pk=int(uid), is_active=True)
                if not default_token_generator.check_token(user, token_value):
                    raise ValidationError({'error': 'Invalid or expired token.'})

                try:
                    validate_password(new_password, user=user)
                except DjangoValidationError as exc:
                    raise ValidationError({'new_password': exc.messages})

                user.set_password(new_password)
                user.save(update_fields=['password'])
                for outstanding in OutstandingToken.objects.filter(
                    user=user, expires_at__gt=timezone.now()
                ):
                    BlacklistedToken.objects.get_or_create(token=outstanding)
        except User.DoesNotExist:
            return Response(
                {'error': 'Invalid or expired token.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        return Response(
            {'message': 'Password has been reset successfully.'},
            status=status.HTTP_200_OK
        )


# ─── HU01_04: Logout with Token Blacklist ────────────────────────────

class LogoutView(generics.GenericAPIView):
    """
    POST /api/v1/auth/logout/

    Blacklists the refresh token in Redis and all associated access tokens.
    """
    permission_classes = (IsAuthenticated,)

    def post(self, request, *args, **kwargs):
        try:
            refresh_token = request.data.get('refresh_token') or request.data.get('refresh')
            if not refresh_token:
                return Response(
                    {'error': 'Refresh token is required.'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            try:
                # A different tab may already have rotated this signed refresh.
                # Check signature, expiry and type before allowing it to close its session.
                verified = UntypedToken(refresh_token)
                if verified.get('token_type') != 'refresh':
                    return Response({'error': 'Invalid session.'}, status=400)
                token = RefreshToken(refresh_token, verify=False)
                if str(token['user_id']) != str(request.user.pk):
                    return Response({'error': 'Invalid session.'}, status=400)
                token.blacklist()
                blacklist_token(refresh_token)
                blacklist_jwt_token(token)
                from .redis_utils import blacklist_session
                blacklist_session(token)
            except (InvalidToken, TokenError):
                return Response({'error': 'Invalid or expired session.'}, status=400)

            bearer = request.META.get('HTTP_AUTHORIZATION', '')
            if bearer.startswith('Bearer '):
                blacklist_token(bearer.split(' ', 1)[1])

            logger.info(f"User {request.user.username} logged out successfully.")

            return Response(
                {'message': 'Logout successful. Session has been invalidated.'},
                status=status.HTTP_200_OK
            )
        except Exception as e:
            logger.error(f"Logout error: {e}")
            return Response(
                {'error': 'An error occurred during logout.'},
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )


# ─── HU01_05: Token Refresh ──────────────────────────────────────────

class CustomTokenRefreshView(TokenRefreshView):
    """
    POST /api/v1/auth/refresh/

    Rotates the refresh token and issues a new access token.
    The old refresh token is blacklisted in Redis.
    """
    def post(self, request, *args, **kwargs):
        refresh_token = request.data.get('refresh')
        if not refresh_token:
            return Response(
                {'error': 'Refresh token is required.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            from .redis_utils import get_redis_connection
            token = RefreshToken(refresh_token)
            if token.get('sid') and get_redis_connection().exists(f"blacklist:session:{token['sid']}"):
                return Response({'error': 'Session has been invalidated.'}, status=401)
            # Blacklist old refresh token
            blacklist_token(refresh_token)

            response = super().post(request, *args, **kwargs)

            if response.status_code == 200:
                # Blacklist the old token via SimpleJWT
                try:
                    token = RefreshToken(refresh_token)
                    blacklist_jwt_token(token)
                except Exception:
                    pass

            return response
        except TokenError:
            return Response(
                {'error': 'Invalid or blacklisted refresh token.'},
                status=status.HTTP_401_UNAUTHORIZED
            )


# ─── Change Password ─────────────────────────────────────────────────

class ChangePasswordView(generics.GenericAPIView):
    """
    PATCH /api/v1/auth/change-password/

    Allows authenticated users to change their password.
    """
    permission_classes = (IsAuthenticated,)
    serializer_class = ChangePasswordSerializer

    def patch(self, request, *args, **kwargs):
        user = request.user
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)

        old_password = serializer.validated_data['old_password']
        new_password = serializer.validated_data['new_password']

        if not user.check_password(old_password):
            return Response(
                {'error': 'Current password is incorrect.'},
                status=status.HTTP_400_BAD_REQUEST
            )

        user.set_password(new_password)
        user.save()

        # Blacklist all existing tokens for this user
        tokens = OutstandingToken.objects.filter(user=user)
        for t in tokens:
            blacklist_jwt_token(t)

        return Response(
            {'message': 'Password changed successfully. Please login again.'},
            status=status.HTTP_200_OK
        )


# ─── User Profile ────────────────────────────────────────────────────

class AdminSummaryView(generics.GenericAPIView):
    permission_classes = (IsAdminUser,)

    def get(self, request):
        return Response({'users': User.objects.count(), 'companies': Company.objects.count()})


class UserProfileView(generics.RetrieveUpdateAPIView):
    """
    GET/PATCH /api/v1/auth/profile/

    Retrieves or updates the authenticated user's profile.
    """
    serializer_class = UserProfileSerializer

    def get_object(self):
        return self.request.user


# ─── HU03_01: Company Search ─────────────────────────────────────────

class CompanySearchView(generics.ListAPIView):
    """
    GET /api/v1/companies/search/

    Search S&P 500 companies by ticker or name using PostgreSQL Full-Text Search.
    Supports filtering by sector and ordering by market_cap.
    """
    serializer_class = CompanySerializer
    permission_classes = (IsAuthenticated,)

    def list(self, request, *args, **kwargs):
        # Validar antes de usar la caché; nunca devolver un resultado a un usuario sin permisos.
        self.get_queryset()
        import hashlib
        import json
        from redis.exceptions import RedisError
        from .redis_utils import get_redis_connection
        identity = json.dumps([request.user.pk, request.build_absolute_uri('/') ,
                               sorted(request.query_params.items())], ensure_ascii=False)
        key = 'sp500:search:' + hashlib.sha256(identity.encode()).hexdigest()
        from django.core.cache import caches
        local = caches['search_results']
        hit = local.get(key)
        if hit is not None:
            return Response(hit)
        connection = None
        try:
            connection = get_redis_connection()
            cached = connection.get(key)
            if isinstance(cached, (str, bytes)):
                data = json.loads(cached)
                local.set(key, data, timeout=5)
                return Response(data)
        except (RedisError, ValueError, TypeError):
            pass
        response = super().list(request, *args, **kwargs)
        if connection is not None:
            try:
                connection.setex(key, 30, json.dumps(response.data))
            except (RedisError, TypeError):
                pass
        local.set(key, response.data, timeout=5)
        return response

    def get_queryset(self):
        query = self.request.query_params.get('q', '').strip()
        sector = self.request.query_params.get('sector', '').strip()
        ordering = self.request.query_params.get('ordering', 'name').strip() or 'name'
        with_data = self.request.query_params.get('with_data', '').strip()

        sectors = {str(label).casefold(): value for value, label in Company.SECTOR_CHOICES}
        sector = sectors.get(sector.casefold(), sector.casefold())
        if sector and sector not in dict(Company.SECTOR_CHOICES):
            raise ValidationError({'sector': 'El sector no es válido.'})
        if ordering not in ('name', '-name', 'market_cap', '-market_cap'):
            raise ValidationError({'ordering': 'El orden no es válido.'})
        if len(query) > 200:
            raise ValidationError({'q': 'La búsqueda admite hasta 200 caracteres.'})
        if with_data not in ('', 'true', 'false'):
            raise ValidationError({'with_data': 'Use true o false.'})

        queryset = Company.objects.only('id', 'ticker', 'name', 'sector', 'market_cap', 'per', 'roe', 'logo_url')
        if with_data == 'true':
            queryset = queryset.filter(Q(market_cap__isnull=False) | Q(per__isnull=False) | Q(roe__isnull=False))
        if query:
            from django.contrib.postgres.search import SearchVector, SearchQuery, SearchRank
            vector = (SearchVector('name', weight='A', config='simple')
                      + SearchVector('ticker', weight='B', config='simple'))
            search = SearchQuery(query, config='simple')
            queryset = queryset.annotate(search_document=vector, rank=SearchRank(vector, search)).filter(
                Q(ticker=query.upper()) | Q(search_document=search)
            )
        if sector:
            queryset = queryset.filter(sector=sector)
        if ordering in ('name', '-name'):
            alphabetical = Lower('name').desc() if ordering == '-name' else Lower('name').asc()
            return queryset.order_by(alphabetical, 'ticker')
        if ordering:
            cap_order = F('market_cap').desc(nulls_last=True) if ordering.startswith('-') else F('market_cap').asc(nulls_last=True)
            return queryset.order_by(cap_order, 'ticker')
        if query:
            return queryset.order_by('-rank', 'ticker')
        return queryset.order_by(F('market_cap').desc(nulls_last=True), 'ticker')


# ─── HU03_05: Search History ─────────────────────────────────────────

class SearchHistoryView(generics.ListCreateAPIView):
    """
    GET/POST /api/v1/companies/history/

    Retrieve or create search history for the authenticated user.
    """
    serializer_class = SearchHistorySerializer
    permission_classes = (IsAuthenticated,)

    def get_queryset(self):
        return SearchHistory.objects.filter(user=self.request.user).order_by('-created_at', '-id')[:10]

    def list(self, request, *args, **kwargs):
        response = super().list(request, *args, **kwargs)
        from .search_cache import cache_history
        cache_history(request.user.pk, response.data['results'])
        return response

    def create(self, request, *args, **kwargs):
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        query = serializer.validated_data['query'].strip()
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.user.pk)
            SearchHistory.objects.filter(user=request.user, query__iexact=query).delete()
            item = SearchHistory.objects.create(user=request.user, query=query)
            retained = list(SearchHistory.objects.filter(user=request.user)
                            .order_by('-created_at', '-id').values_list('pk', flat=True)[:10])
            SearchHistory.objects.filter(user=request.user).exclude(pk__in=retained).delete()
            self._refresh_cache(request.user.pk)
        return Response(self.get_serializer(item).data, status=status.HTTP_201_CREATED)

    @staticmethod
    def _refresh_cache(user_id):
        from .search_cache import cache_history
        def refresh():
            rows = SearchHistorySerializer(SearchHistory.objects.filter(user_id=user_id)
                   .order_by('-created_at', '-id')[:10], many=True).data
            cache_history(user_id, rows)
        transaction.on_commit(refresh)


class SearchHistoryDeleteView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)

    def delete(self, request, history_id=None, *args, **kwargs):
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.user.pk)
            rows = SearchHistory.objects.filter(user=request.user)
            if history_id is not None:
                rows = rows.filter(pk=history_id)
                if not rows.exists():
                    return Response({'error': 'Búsqueda no encontrada.'}, status=404)
            rows.delete()
            SearchHistoryView._refresh_cache(request.user.pk)
        return Response(status=status.HTTP_204_NO_CONTENT)


# ─── HU03_03: Sectors List ───────────────────────────────────────────

class CompanySectorsView(generics.GenericAPIView):
    """
    GET /api/v1/companies/sectors/

    Returns list of available sectors for filtering.
    """
    permission_classes = (IsAuthenticated,)

    def get(self, request, *args, **kwargs):
        sectors = [{'value': s[0], 'label': s[1]} for s in Company.SECTOR_CHOICES]
        return Response({'sectors': sectors})


# ─── HU04_03: Altman-Z Score ─────────────────────────────────────────

def calculate_altman_z(company):
    """
    Calculate Altman-Z Score for bankruptcy prediction.

    Z = 1.2*X1 + 1.4*X2 + 3.3*X3 + 0.6*X4 + 1.0*X5

    Where:
    X1 = Working Capital / Total Assets
    X2 = Retained Earnings / Total Assets
    X3 = EBIT / Total Assets
    X4 = Market Value of Equity / Total Liabilities
    X5 = Sales / Total Assets
    """
    unknown = {'score': None, 'zone': 'unknown', 'risk_level': 'unknown'}
    fields = ('working_capital', 'total_assets', 'retained_earnings', 'ebit',
              'equity_value', 'total_liabilities', 'sales')
    try:
        values = {field: Decimal(str(getattr(company, field))) for field in fields}
        if not all(value.is_finite() for value in values.values()):
            return unknown
        if values['total_assets'] <= 0 or values['total_liabilities'] <= 0:
            return unknown
        if values['equity_value'] < 0 or values['sales'] < 0:
            return unknown
        with localcontext() as context:
            context.prec = 50
            assets = values['total_assets']
            ratios = (
                values['working_capital'] / assets,
                values['retained_earnings'] / assets,
                values['ebit'] / assets,
                values['equity_value'] / values['total_liabilities'],
                values['sales'] / assets,
            )
            weights = tuple(map(Decimal, ('1.2', '1.4', '3.3', '0.6', '1.0')))
            score = sum(weight * ratio for weight, ratio in zip(weights, ratios))
            if score > Decimal('2.99'):
                zone, risk = 'safe', 'low'
            elif score >= Decimal('1.81'):
                zone, risk = 'grey', 'medium'
            else:
                zone, risk = 'distress', 'high'
            return {
                'score': float(round(score, 2)),
                'zone': zone,
                'risk_level': risk,
                'components': {f'x{i}': float(round(ratio, 4))
                               for i, ratio in enumerate(ratios, start=1)},
            }
    except (ArithmeticError, TypeError, ValueError):
        return unknown


# ─── HU04_02/03/04: Company Overview ─────────────────────────────────

def financial_profile(altman_z):
    zones = {'safe': ('strong', 'Muy sólido', 'green'),
             'grey': ('moderate', 'Moderado', 'orange'),
             'distress': ('weak', 'Muy débil', 'red')}
    category, label, color = zones.get(altman_z['zone'], ('unknown', 'Sin datos suficientes', 'grey'))
    return {'category': category, 'label': label, 'color': color}


def company_identifier(value):
    if value.isascii() and value.isdigit() and len(value) <= 19 and int(value) <= 2**63-1:
        return {'pk': int(value)}
    return {'ticker': value.upper()}


def company_financial_data(company):
    metrics = getattr(company, 'financial_metrics', None)
    balance = metrics if metrics is not None else company
    score = calculate_altman_z(balance)
    result = {
        'id': company.pk, 'ticker': company.ticker, 'name': company.name,
        'sector': company.sector, 'sector_display': company.get_sector_display(),
        'logo_url': company.logo_url, 'altman_z': score, 'profile': financial_profile(score),
        'as_of': metrics.as_of if metrics else None,
        'source': metrics.source if metrics else 'Datos heredados sin verificar',
        'is_demo': metrics.is_demo if metrics else True,
    }
    for field in ('market_cap', 'per', 'roe', 'beta', 'dividend_yield'):
        value = getattr(balance, field, None)
        result[field] = float(value) if value is not None else None
    quote = company.latest_price
    result['quote'] = ({'date': quote.date, 'adjusted_close': float(quote.adjusted_close),
                        'currency': quote.currency, 'source': quote.source, 'is_demo': quote.is_demo}
                       if quote else None)
    return result


class CompanyOverviewView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    def get(self, request, ticker, *args, **kwargs):
        try:
            company = Company.objects.select_related('financial_metrics', 'latest_price').get(**company_identifier(ticker))
        except Company.DoesNotExist:
            return Response({'error': 'Empresa no encontrada.'}, status=404)
        return Response(company_financial_data(company))


class CompanySummaryView(CompanyOverviewView):
    """Ficha y cotización se cargan en una sola consulta con JOIN."""


class CompanyHistoryView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    def get(self, request, ticker, *args, **kwargs):
        import calendar
        from datetime import date
        try:
            company = Company.objects.get(**company_identifier(ticker))
        except Company.DoesNotExist:
            return Response({'error': 'Empresa no encontrada.'}, status=404)
        ranges = {'1M': 1, '6M': 6, '1Y': 12, '5Y': 60}
        period = request.query_params.get('range', '1M').upper()
        if period not in ranges:
            raise ValidationError({'range': 'Se admite 1M, 6M, 1Y o 5Y.'})
        end = timezone.localdate()
        month = end.year * 12 + end.month - 1 - ranges[period]
        year, month_zero = divmod(month, 12)
        start = date(year, month_zero+1, min(end.day, calendar.monthrange(year, month_zero+1)[1]))
        rows = list(PriceHistory.objects.filter(company=company, date__gte=start, date__lte=end)
                    .order_by('date').values('date', 'adjusted_close', 'volume', 'currency', 'source', 'is_demo'))
        history = [{'date': row['date'], 'price': float(row['adjusted_close']), 'volume': row['volume']} for row in rows]
        return Response({'ticker': company.ticker, 'name': company.name, 'range': period,
            'history': history, 'count': len(history), 'price_type': 'adjusted_close',
            'sources': sorted({row['source'] for row in rows}),
            'currencies': sorted({row['currency'] for row in rows}),
            'is_demo': any(row['is_demo'] for row in rows)})


# ─── HU03_02: Autocomplete ───────────────────────────────────────────

class CompanyAutocompleteView(generics.GenericAPIView):
    """
    GET /api/v1/companies/autocomplete/

    Returns autocomplete suggestions for company search using Redis cache.
    """
    permission_classes = (IsAuthenticated,)

    def get(self, request, *args, **kwargs):
        prefix = request.query_params.get('q', '').strip()
        if not prefix:
            return Response({'suggestions': []})

        if len(prefix) > 200:
            raise ValidationError({'q': 'La búsqueda admite hasta 200 caracteres.'})
        from .search_cache import autocomplete
        from redis.exceptions import RedisError
        try:
            suggestions = autocomplete(prefix)
        except RedisError:
            return Response({'error': 'El autocompletado no está disponible.'}, status=503)
        return Response({'suggestions': suggestions})
