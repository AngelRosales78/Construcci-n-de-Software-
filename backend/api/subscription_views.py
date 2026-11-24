"""Epic 11: plans, simulated payment, receipts, subscription status and cancellation."""
import re
from datetime import date

from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response

from .billing import DECLINED_TEST_CARD, invoice_payload, luhn_valid, plans_payload, purchase, settle
from .comparison_views import access_info
from .models import Invoice, UserSubscription

REQUEST_ID = re.compile(r'^[A-Za-z0-9-]{8,64}$')
CANCEL_REASONS = {'price', 'not_using', 'missing_features', 'temporary', 'other'}


def fail(message):
    # Android and Web show the "detail" text of a 400 response.
    raise ValidationError({'detail': message})


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def plans(request):
    """HU11_02: static comparison of the Free and Pro plans."""
    return Response(plans_payload())


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def simulate_payment(request):
    """HU11_01: validate a test card with Luhn and upgrade the account to Pro immediately."""
    data = request.data
    request_id = str(data.get('request_id', ''))
    if not REQUEST_ID.match(request_id):
        fail('Solicitud sin identificador válido.')
    previous = Invoice.objects.filter(user=request.user, request_id=request_id).first()
    if previous:
        return Response({'schema_version': 'payment-v1', 'owner_id': str(request.user.pk),
                         'invoice': invoice_payload(previous), 'subscription': access_info(request.user)})
    number = re.sub(r'[\s-]', '', str(data.get('card_number', '')))
    holder = str(data.get('holder', '')).strip()
    cvc = str(data.get('cvc', '')).strip()
    try:
        month, year = int(data.get('exp_month')), int(data.get('exp_year'))
    except (TypeError, ValueError):
        fail('Fecha de vencimiento no válida.')
    if year < 100:
        year += 2000
    if len(holder) < 3 or len(holder) > 80:
        fail('Escribe el nombre del titular como figura en la tarjeta.')
    if not luhn_valid(number):
        fail('El número de tarjeta no es válido.')
    if not 1 <= month <= 12 or (year, month) < (date.today().year, date.today().month) or year > date.today().year + 20:
        fail('La tarjeta está vencida o la fecha no es válida.')
    if not re.fullmatch(r'\d{4}' if number[:2] in {'34', '37'} else r'\d{3}', cvc):
        fail('El código de seguridad no es válido.')
    if number == DECLINED_TEST_CARD:
        fail('Pago rechazado por el emisor (simulación). Prueba con otra tarjeta.')
    subscription = settle(UserSubscription.objects.filter(user=request.user).first())
    if subscription and subscription.plan == 'pro' and subscription.active and subscription.valid_until > timezone.now():
        fail('Tu plan Pro ya está activo. Podrás contratarlo de nuevo cuando termine el periodo actual.')
    invoice, _ = purchase(request.user, request_id, number)
    return Response({'schema_version': 'payment-v1', 'owner_id': str(request.user.pk),
                     'invoice': invoice_payload(invoice), 'subscription': access_info(request.user)},
                    status=status.HTTP_201_CREATED)


@api_view(['GET'])
@permission_classes([IsAuthenticated])
def invoices(request, number=None):
    """HU11_03: the account's receipts, newest first."""
    access_info(request.user)  # applies pending simulated renewals first
    if number:
        return Response(invoice_payload(get_object_or_404(Invoice, user=request.user, number=number)))
    return Response({'schema_version': 'invoices-v1', 'owner_id': str(request.user.pk),
                     'results': [invoice_payload(i) for i in request.user.invoices.all()[:50]]})


@api_view(['POST'])
@permission_classes([IsAuthenticated])
def cancel_subscription(request):
    """HU11_05: stop renewal while keeping Pro until the end of the paid period."""
    reason = str(request.data.get('reason', 'other'))
    if reason not in CANCEL_REASONS:
        fail('Motivo de cancelación no válido.')
    comment = str(request.data.get('comment', '')).strip()[:150]
    subscription = settle(UserSubscription.objects.filter(user=request.user).first())
    if not subscription or subscription.plan != 'pro' or not subscription.active:
        fail('No tienes una suscripción Pro activa.')
    if subscription.auto_renew:
        subscription.auto_renew = False
        subscription.cancelled_at = timezone.now()
        subscription.cancel_reason = f'{reason}: {comment}' if comment else reason
        subscription.save(update_fields=['auto_renew', 'cancelled_at', 'cancel_reason'])
    return Response(access_info(request.user))
