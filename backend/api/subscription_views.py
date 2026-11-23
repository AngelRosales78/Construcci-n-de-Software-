"""Suscripciones de demostración y comprobantes privados."""
from datetime import timedelta
from decimal import Decimal
from django.conf import settings
from django.core.mail import EmailMultiAlternatives, get_connection
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from django.utils.html import escape
from rest_framework import serializers
from rest_framework.exceptions import APIException
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from .access_views import subscription
from .models import AccountPlan, SubscriptionTransaction, User


class SubscriptionConflict(APIException):
    status_code = 409
    default_detail = 'La operación no corresponde al estado actual de la suscripción.'


def receipt_data(record):
    return {'id': str(record.id), 'request_id': str(record.request_id), 'plan': 'pro',
            'simulated': True, 'outcome': record.outcome, 'amount': str(record.amount),
            'currency': record.currency, 'duration_days': record.duration_days,
            'starts_at': record.starts_at, 'ends_at': record.ends_at,
            'created_at': record.created_at, 'email_status': record.email_status}


def deliver_receipt(record_id):
    # El recibo persiste aunque el transporte de correo falle. Un comando permite reintentar.
    with transaction.atomic():
        record = SubscriptionTransaction.objects.select_for_update(of=('self',)).select_related('user').get(pk=record_id)
        if record.user_id is None:
            return
        if record.outcome != 'approved' or record.email_status in ('sent', 'local'):
            return
        message = (f'Comprobante de activación {record.id}\nPlan Pro\n'
                   f'Valor de referencia: {record.currency} {record.amount}\n'
                   f'Vigencia: {record.starts_at.isoformat()} a {record.ends_at.isoformat()}\n'
                   'Activación SIN CARGO MONETARIO. Renovación mediante nueva confirmación manual.')
        try:
            if not record.user.email:
                raise ValueError('La cuenta no tiene correo.')
            email = EmailMultiAlternatives('Confirmación de acceso Pro', message,
                                          settings.DEFAULT_FROM_EMAIL, [record.user.email],
                                          connection=get_connection(timeout=5))
            email.attach_alternative('<h1>Tu acceso Pro</h1><pre>' + escape(message) + '</pre>', 'text/html')
            if email.send() != 1:
                raise ValueError('El transporte no confirmó el envío.')
            record.email_status = 'local' if 'console.' in settings.EMAIL_BACKEND or 'filebased.' in settings.EMAIL_BACKEND else 'sent'
        except Exception:
            record.email_status = 'pending'
        record.save(update_fields=['email_status'])


class CheckoutInput(serializers.Serializer):
    request_id = serializers.UUIDField()
    outcome = serializers.ChoiceField(choices=('approved', 'declined'))

    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) != set(self.fields):
            raise serializers.ValidationError({'non_field_errors': ['Solo se admiten el identificador y el resultado ficticio de la operación.']})
        return super().to_internal_value(data)


class PrivateSubscriptionView(APIView):
    permission_classes = (IsAuthenticated,)

    def finalize_response(self, request, response, *args, **kwargs):
        response = super().finalize_response(request, response, *args, **kwargs)
        response['Cache-Control'] = 'private, no-store'
        return response


class PlansView(PrivateSubscriptionView):
    def get(self, request):
        return Response({'simulated': True, 'amount': str(Decimal(settings.SIMULATED_PRO_PRICE)),
                         'currency': settings.SIMULATED_PRO_CURRENCY, 'duration_days': settings.SIMULATED_PRO_DAYS,
                         'features': [
                             {'label': 'Fichas, gráficos y análisis individual', 'free': True, 'pro': True},
                             {'label': 'Favoritos, etiquetas y recientes', 'free': True, 'pro': True},
                             {'label': 'Simulaciones y PDF individual', 'free': True, 'pro': True},
                             {'label': 'Comparador, rendimiento y conclusión de dos empresas', 'free': False, 'pro': True},
                             {'label': 'PDF comparativo', 'free': False, 'pro': True}]})


class CheckoutView(PrivateSubscriptionView):
    def post(self, request):
        form = CheckoutInput(data=request.data)
        form.is_valid(raise_exception=True)
        values = form.validated_data
        with transaction.atomic():
            # Serializa todas las operaciones de una cuenta, incluso su primera suscripción.
            user = User.objects.select_for_update().get(pk=request.user.pk)
            record = SubscriptionTransaction.objects.filter(user=user, request_id=values['request_id']).first()
            if record:
                if record.outcome != values['outcome']:
                    raise SubscriptionConflict('Este identificador ya corresponde a otro resultado simulado.')
                return Response({'receipt': receipt_data(record), 'subscription': subscription(user)})
            now = timezone.now()
            account, _ = AccountPlan.objects.get_or_create(user=user)
            if account.plan == 'pro' and account.active and (account.expires_at is None or account.expires_at > now):
                raise SubscriptionConflict('Ya tienes Pro activo. Puedes revisar su vigencia en tu perfil.')
            approved = values['outcome'] == 'approved'
            end = now + timedelta(days=settings.SIMULATED_PRO_DAYS) if approved else None
            record = SubscriptionTransaction.objects.create(user=user, **values,
                amount=Decimal(settings.SIMULATED_PRO_PRICE), currency=settings.SIMULATED_PRO_CURRENCY,
                duration_days=settings.SIMULATED_PRO_DAYS, starts_at=now if approved else None,
                ends_at=end, email_status='pending' if approved else 'not_required')
            if approved:
                account.plan, account.active, account.expires_at = 'pro', True, end
                account.auto_renew, account.cancelled_at = True, None
                account.save(update_fields=['plan', 'active', 'expires_at', 'auto_renew', 'cancelled_at'])
                transaction.on_commit(lambda: deliver_receipt(record.id), robust=True)
        record.refresh_from_db()
        return Response({'receipt': receipt_data(record), 'subscription': subscription(user)}, status=201)


class SubscriptionHistoryView(PrivateSubscriptionView):
    def get(self, request):
        records = SubscriptionTransaction.objects.filter(user=request.user)
        return Response({'subscription': subscription(request.user), 'receipts': [receipt_data(r) for r in records]})


class ReceiptView(PrivateSubscriptionView):
    def get(self, request, receipt_id):
        record = get_object_or_404(SubscriptionTransaction, pk=receipt_id, user=request.user)
        return Response(receipt_data(record))


class CancelSubscriptionView(PrivateSubscriptionView):
    def post(self, request):
        if request.data:
            raise serializers.ValidationError('Esta operación no admite cambios de plan ni de fechas.')
        with transaction.atomic():
            User.objects.select_for_update().get(pk=request.user.pk)
            account = AccountPlan.objects.filter(user=request.user).first()
            if account and account.plan == 'pro':
                if account.cancelled_at is None:
                    if account.expires_at is None:
                        account.expires_at = timezone.now() + timedelta(days=settings.SIMULATED_PRO_DAYS)
                    account.auto_renew = False
                    account.cancelled_at = timezone.now()
                    account.save(update_fields=['auto_renew', 'cancelled_at', 'expires_at'])
        return Response(subscription(request.user))
