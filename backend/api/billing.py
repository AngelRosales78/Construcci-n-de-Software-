"""Simulated billing for Epic 11. No real charge is made and full card numbers are never stored."""
import logging
import uuid
from datetime import timedelta
from decimal import Decimal

from django.conf import settings
from django.core.mail import EmailMultiAlternatives
from django.db import transaction
from django.db.models import Q
from django.utils import timezone

from .models import Invoice, UserSubscription

logger = logging.getLogger(__name__)

CURRENCY = 'USD'
PRO_PRICE = Decimal('9.99')
PERIOD_DAYS = 30
# Published test number that always simulates a decline.
DECLINED_TEST_CARD = '4000000000000002'

PLANS = [
    {'id': 'free', 'name': 'Free', 'price': '0.00', 'popular': False,
     'description': 'Análisis individual de empresas del S&P 500.'},
    {'id': 'pro', 'name': 'Pro', 'price': str(PRO_PRICE), 'popular': True,
     'description': 'Comparación avanzada y reportes comparativos.'},
]
FEATURES = [
    ('search', 'Búsqueda y ficha técnica de las 503 empresas', True, True),
    ('pillars', 'Cinco pilares y puntaje de salud financiera', True, True),
    ('scenarios', 'Escenarios y simulación personalizada', True, True),
    ('analysis', 'Resumen y explicaciones generadas por IA', True, True),
    ('favorites', 'Favoritos, etiquetas y recientes sincronizados', True, True),
    ('company_pdf', 'Reporte PDF de una empresa', True, True),
    ('compare', 'Comparador side-by-side de dos empresas', False, True),
    ('comparison_pdf', 'Reporte PDF comparativo', False, True),
]


def plans_payload():
    return {'schema_version': 'plans-v1', 'currency': CURRENCY, 'period': 'month',
            'plans': [plan | {'features': [{'id': ident, 'label': label,
                                            'included': free if plan['id'] == 'free' else pro}
                                           for ident, label, free, pro in FEATURES]} for plan in PLANS]}


def luhn_valid(number):
    if not number.isdigit() or not 13 <= len(number) <= 19:
        return False
    total = 0
    for index, char in enumerate(reversed(number)):
        digit = int(char)
        if index % 2:
            digit *= 2
            if digit > 9:
                digit -= 9
        total += digit
    return total % 10 == 0


def card_brand(number):
    if number.startswith('4'):
        return 'Visa'
    if number[:2] in {'34', '37'}:
        return 'American Express'
    if 51 <= int(number[:2]) <= 55 or 2221 <= int(number[:4]) <= 2720:
        return 'Mastercard'
    return 'Tarjeta'


def invoice_payload(invoice):
    return {'number': invoice.number, 'transaction_id': invoice.transaction_id, 'kind': invoice.kind,
            'plan': invoice.plan, 'amount': str(invoice.amount), 'currency': invoice.currency,
            'card_brand': invoice.card_brand, 'card_last4': invoice.card_last4,
            'period_start': invoice.period_start.isoformat(), 'period_end': invoice.period_end.isoformat(),
            'created_at': invoice.created_at.isoformat(),
            'emailed': invoice.emailed_at is not None, 'simulated': True}


def _new_invoice(user, request_id, kind, start, end, brand='', last4=''):
    now = timezone.now()
    return Invoice.objects.create(
        user=user, request_id=request_id, kind=kind, plan='pro', amount=PRO_PRICE, currency=CURRENCY,
        number=f'INV-{now:%Y%m%d}-{uuid.uuid4().hex[:8].upper()}',
        transaction_id=f'TXN-{uuid.uuid4().hex[:20].upper()}',
        card_brand=brand, card_last4=last4, period_start=start, period_end=end)


def send_receipt(invoice):
    """HU11_03: e-mail the receipt with an HTML attachment. Failures never undo the simulated payment."""
    user = invoice.user
    if not user.email:
        return
    card = f'{invoice.card_brand} •••• {invoice.card_last4}' if invoice.card_last4 else 'Renovación automática'
    rows = [('Número de comprobante', invoice.number), ('Transacción', invoice.transaction_id),
            ('Plan', 'Pro mensual'), ('Importe', f'{invoice.amount} {invoice.currency}'), ('Medio', card),
            ('Periodo', f'{invoice.period_start:%d/%m/%Y} – {invoice.period_end:%d/%m/%Y}')]
    html = ('<html><body style="font-family:sans-serif"><h2>Comprobante de suscripción simulada</h2>'
            '<table>' + ''.join(f'<tr><td><b>{k}</b></td><td>{v}</td></tr>' for k, v in rows) +
            '</table><p>Pago simulado con fines académicos. No se realizó ningún cargo.</p></body></html>')
    text = 'Comprobante de suscripción simulada\n' + '\n'.join(f'{k}: {v}' for k, v in rows) + \
        '\nPago simulado con fines académicos. No se realizó ningún cargo.'
    try:
        message = EmailMultiAlternatives(f'Comprobante {invoice.number}', text,
                                         settings.DEFAULT_FROM_EMAIL, [user.email])
        message.attach_alternative(html, 'text/html')
        message.attach(f'{invoice.number}.html', html, 'text/html')
        message.send()
        Invoice.objects.filter(pk=invoice.pk).update(emailed_at=timezone.now())
        invoice.emailed_at = timezone.now()
    except Exception as error:  # pragma: no cover - depends on the mail backend
        logger.error('Receipt %s was not sent: %s', invoice.number, error)


@transaction.atomic
def purchase(user, request_id, number):
    existing = Invoice.objects.filter(user=user, request_id=request_id).first()
    if existing:
        return existing, False
    now = timezone.now()
    end = now + timedelta(days=PERIOD_DAYS)
    UserSubscription.objects.update_or_create(user=user, defaults={
        'plan': 'pro', 'active': True, 'period_start': now, 'valid_until': end,
        'auto_renew': True, 'cancelled_at': None, 'cancel_reason': '', 'granted': False})
    invoice = _new_invoice(user, request_id, 'purchase', now, end, card_brand(number), number[-4:])
    transaction.on_commit(lambda: send_receipt(invoice))
    return invoice, True


def settle(subscription):
    """Apply simulated renewals or the end of a cancelled period before reading the subscription."""
    if subscription is None or subscription.plan != 'pro' or subscription.granted:
        return subscription
    now = timezone.now()
    if subscription.valid_until > now:
        return subscription
    with transaction.atomic():
        current = UserSubscription.objects.select_for_update().get(pk=subscription.pk)
        if current.plan == 'pro' and not current.granted and current.valid_until <= now:
            if current.auto_renew and current.active:
                while current.valid_until <= now:
                    start, end = current.valid_until, current.valid_until + timedelta(days=PERIOD_DAYS)
                    _new_invoice(current.user, f'renewal-{start.isoformat()}', 'renewal', start, end)
                    current.period_start, current.valid_until = start, end
            else:
                current.plan, current.active = 'free', False
            current.save()
    return current


def assign_plan(user, plan):
    """HU13_02: administrative assignment. It never creates a charge, a receipt or a renewal."""
    UserSubscription.objects.update_or_create(user=user, defaults={
        'plan': plan, 'active': True, 'granted': plan == 'pro', 'period_start': None, 'valid_until': timezone.now(),
        'auto_renew': False, 'cancelled_at': None, 'cancel_reason': ''})


def pro_filter(now=None):
    """Same rule as access_info, for lists: paid and current, renewable, or assigned by an administrator."""
    current = (Q(usersubscription__valid_until__gt=now or timezone.now()) | Q(usersubscription__auto_renew=True)
               | Q(usersubscription__granted=True))
    rule = Q(usersubscription__plan='pro', usersubscription__active=True) & current
    if getattr(settings, 'LOCAL_DEVELOPMENT_PRO', False):
        rule |= Q(usersubscription__isnull=True)
    return rule
