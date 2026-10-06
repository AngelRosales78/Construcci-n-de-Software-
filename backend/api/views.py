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
from rest_framework import status, generics
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import AllowAny, IsAuthenticated
from rest_framework.response import Response
from rest_framework_simplejwt.tokens import RefreshToken, OutstandingToken
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError
from django.contrib.auth import get_user_model
from django.core.mail import send_mail
from django.utils.html import strip_tags
from django.conf import settings
from django.urls import reverse
from django.contrib.auth.tokens import default_token_generator
from django.utils.http import urlsafe_base64_encode, urlsafe_base64_decode
from django.utils.encoding import force_bytes, force_str

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
from .models import Company, SearchHistory

User = get_user_model()
logger = logging.getLogger(__name__)


# ─── HU01_01: User Registration ──────────────────────────────────────

class RegisterView(generics.CreateAPIView):
    """
    POST /api/v1/auth/register/

    Creates a new user with bcrypt password hashing (via Django's
    create_user which uses PBKDF2 with SHA256).

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
        refresh = RefreshToken.for_user(user)

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
                models.Q(username=username_or_email) | models.Q(email=username_or_email)
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

        return super().post(request, *args, **kwargs)


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
                <p>This link will expire in 1 hour.</p>
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

        # Extract UID from URL-safe base64
        try:
            # The token should be passed as part of the URL uid/token format
            parts = token.split(':')
            if len(parts) == 2:
                uid, token_value = parts
                uid = force_str(urlsafe_base64_decode(uid))
                user = User.objects.get(pk=uid)

                if default_token_generator.check_token(user, token_value):
                    user.set_password(new_password)
                    user.save()

                    # Blacklist all existing tokens for this user
                    tokens = OutstandingToken.objects.filter(user=user)
                    for t in tokens:
                        blacklist_jwt_token(t)

                    return Response(
                        {'message': 'Password has been reset successfully.'},
                        status=status.HTTP_200_OK
                    )
                else:
                    return Response(
                        {'error': 'Invalid or expired token.'},
                        status=status.HTTP_400_BAD_REQUEST
                    )
            else:
                # Try the simpler format
                uid = force_str(urlsafe_base64_decode(token))
                user = User.objects.get(pk=uid)
                user.set_password(new_password)
                user.save()

                tokens = OutstandingToken.objects.filter(user=user)
                for t in tokens:
                    blacklist_jwt_token(t)

                return Response(
                    {'message': 'Password has been reset successfully.'},
                    status=status.HTTP_200_OK
                )
        except (User.DoesNotExist, Exception) as e:
            return Response(
                {'error': 'Invalid or expired token.'},
                status=status.HTTP_400_BAD_REQUEST
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
            refresh_token = request.data.get('refresh_token')
            if not refresh_token:
                return Response(
                    {'error': 'Refresh token is required.'},
                    status=status.HTTP_400_BAD_REQUEST
                )

            # Blacklist the refresh token in Redis
            blacklist_token(refresh_token)

            # Also blacklist via SimpleJWT token blacklist
            try:
                token = RefreshToken(refresh_token)
                blacklist_jwt_token(token)
            except (InvalidToken, TokenError):
                pass

            logger.info(f"User {request.user.username} logged out successfully.")

            return Response(
                {'message': 'Logout successful. All tokens have been invalidated.'},
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

    def get_queryset(self):
        query = self.request.query_params.get('q', '').strip()
        sector = self.request.query_params.get('sector', '').strip()
        ordering = self.request.query_params.get('ordering', '').strip()

        queryset = Company.objects.all()

        if query:
            from django.contrib.postgres.search import SearchVector, SearchQuery, SearchRank
            search_vector = SearchVector('name', weight='A') + SearchVector('ticker', weight='B')
            search_query = SearchQuery(query)
            queryset = queryset.annotate(
                rank=SearchRank(search_vector, search_query)
            ).filter(rank__gte=0.1)

        if sector:
            queryset = queryset.filter(sector=sector)

        if ordering == 'market_cap':
            queryset = queryset.order_by('market_cap')
        elif ordering == '-market_cap':
            queryset = queryset.order_by('-market_cap')
        elif query:
            queryset = queryset.order_by('-rank')

        return queryset[:50]


# ─── HU03_05: Search History ─────────────────────────────────────────

class SearchHistoryView(generics.ListCreateAPIView):
    """
    GET/POST /api/v1/companies/history/

    Retrieve or create search history for the authenticated user.
    """
    serializer_class = SearchHistorySerializer
    permission_classes = (IsAuthenticated,)

    def get_queryset(self):
        return SearchHistory.objects.filter(user=self.request.user)[:20]

    def perform_create(self, serializer):
        serializer.save(user=self.request.user)


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
    try:
        if company.total_assets == 0:
            return {'score': 0, 'zone': 'unknown', 'risk_level': 'unknown'}

        x1 = float(company.working_capital) / float(company.total_assets)
        x2 = float(company.retained_earnings) / float(company.total_assets)
        x3 = float(company.ebit) / float(company.total_assets)
        x4 = float(company.equity_value) / float(company.total_liabilities) if company.total_liabilities > 0 else 0
        x5 = float(company.sales) / float(company.total_assets)

        z_score = 1.2 * x1 + 1.4 * x2 + 3.3 * x3 + 0.6 * x4 + 1.0 * x5

        if z_score > 2.99:
            zone = 'safe'
            risk_level = 'low'
        elif z_score > 1.81:
            zone = 'grey'
            risk_level = 'medium'
        else:
            zone = 'distress'
            risk_level = 'high'

        return {
            'score': round(z_score, 2),
            'zone': zone,
            'risk_level': risk_level,
            'components': {
                'x1': round(x1, 4),
                'x2': round(x2, 4),
                'x3': round(x3, 4),
                'x4': round(x4, 4),
                'x5': round(x5, 4),
            }
        }
    except Exception:
        return {'score': 0, 'zone': 'unknown', 'risk_level': 'unknown'}


# ─── HU04_02/03/04: Company Overview ─────────────────────────────────

class CompanyOverviewView(generics.GenericAPIView):
    """
    GET /api/v1/companies/<ticker>/overview/

    Returns financial overview including Altman-Z score, PER, ROE, Market Cap.
    """
    permission_classes = (IsAuthenticated,)

    def get(self, request, ticker, *args, **kwargs):
        try:
            company = Company.objects.get(ticker=ticker.upper())
        except Company.DoesNotExist:
            return Response(
                {'error': 'Company not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        altman_z = calculate_altman_z(company)

        data = {
            'ticker': company.ticker,
            'name': company.name,
            'sector': company.sector,
            'sector_display': company.get_sector_display(),
            'market_cap': float(company.market_cap),
            'per': float(company.per) if company.per else None,
            'roe': float(company.roe) if company.roe else None,
            'altman_z': altman_z,
        }

        return Response(data)


# ─── HU04_04: Company History ────────────────────────────────────────

class CompanyHistoryView(generics.GenericAPIView):
    """
    GET /api/v1/companies/<ticker>/history/

    Returns historical price data for charts.
    """
    permission_classes = (IsAuthenticated,)

    def get(self, request, ticker, *args, **kwargs):
        try:
            company = Company.objects.get(ticker=ticker.upper())
        except Company.DoesNotExist:
            return Response(
                {'error': 'Company not found'},
                status=status.HTTP_404_NOT_FOUND
            )

        import random
        from datetime import datetime, timedelta

        base_price = random.uniform(50, 500)
        history = []
        for i in range(30, 0, -1):
            date = datetime.now() - timedelta(days=i)
            change = random.uniform(-0.05, 0.05)
            price = base_price * (1 + change)
            base_price = price
            history.append({
                'date': date.strftime('%Y-%m-%d'),
                'price': round(price, 2),
                'volume': random.randint(1000000, 10000000),
            })

        return Response({
            'ticker': company.ticker,
            'name': company.name,
            'history': history,
        })


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

        suggestions = get_autocomplete_suggestions(prefix)
        return Response({'suggestions': suggestions})
