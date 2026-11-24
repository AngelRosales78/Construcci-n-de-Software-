"""Checkout Pro Orders API, restricted to a verified Peruvian test seller."""
import hashlib
import hmac
import json
import re
from datetime import timedelta
from decimal import Decimal
from urllib.error import HTTPError, URLError
from urllib.parse import urlsplit, urlencode
from urllib.request import Request, build_opener, HTTPRedirectHandler
from django.conf import settings
from django.db import transaction
from django.utils import timezone
from rest_framework.exceptions import APIException
from .models import PaymentOrder, AccountPlan, User, SubscriptionTransaction

class PaymentUnavailable(APIException):
    status_code = 503
    default_detail = 'El pago de prueba no está disponible. Revisa la configuración e inténtalo de nuevo.'

class PaymentConflict(APIException):
    status_code = 409
    default_detail = 'Ya tienes una operación pendiente o Pro activo.'

class NoRedirect(HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def public_https(value):
    try:
        parsed=urlsplit(value)
    except (ValueError,TypeError):
        return False
    return bool(parsed.scheme=='https' and parsed.hostname and parsed.hostname not in ('localhost','127.0.0.1','::1') and
                not parsed.username and not parsed.password and not parsed.fragment and not parsed.query)

def setup_status():
    try:
        price=Decimal(settings.SIMULATED_PRO_PRICE)
        tariff_valid=(price.is_finite() and 0<price<Decimal('100000000') and price==price.quantize(Decimal('.01'))
                      and settings.SIMULATED_PRO_CURRENCY=='PEN' and 1<=settings.SIMULATED_PRO_DAYS<=365)
    except (ValueError,TypeError,ArithmeticError):
        tariff_valid=False
    checks = {
        'tarifa del plan en PEN':tariff_valid,
        'vendedor de prueba': bool(re.fullmatch(r'[0-9]{1,40}',settings.MERCADOPAGO_TEST_SELLER_ID)),
        'credencial del vendedor': bool(settings.MERCADOPAGO_ACCESS_TOKEN),
        'firma de notificaciones': len(settings.MERCADOPAGO_WEBHOOK_SECRET)>=16,
        'comprador de prueba': bool(re.fullmatch(r'[^@\s]+@testuser\.com',settings.MERCADOPAGO_TEST_BUYER_EMAIL)),
        'retorno HTTPS': public_https(settings.MERCADOPAGO_RETURN_URL),
        'notificaciones HTTPS': public_https(settings.MERCADOPAGO_WEBHOOK_URL) and
            urlsplit(settings.MERCADOPAGO_WEBHOOK_URL).path.endswith('/api/v1/payments/mercadopago/webhook/'),
    }
    missing=[key for key,valid in checks.items() if not valid]
    return {'provider':'mercadopago_test','test_mode':True,'ready':not missing and settings.PAYMENT_PROVIDER=='mercadopago_test',
            'message':'Pago de prueba listo para conectar con Mercado Pago.' if not missing else
            'La conexión con Mercado Pago está pendiente de configuración. No se realizará ningún cobro.',
            'missing':missing}

def require_setup():
    if not setup_status()['ready']: raise PaymentUnavailable()

def request_api(method,path,payload=None,key=None):
    if not re.fullmatch(r'/users/me|/v1/orders(?:/ORD[A-Za-z0-9]{1,75})?',path):
        raise PaymentUnavailable()
    headers={'Authorization':'Bearer '+settings.MERCADOPAGO_ACCESS_TOKEN,'Accept':'application/json'}
    if key:headers['X-Idempotency-Key']=str(key)
    raw=None
    if payload is not None:
        headers['Content-Type']='application/json';raw=json.dumps(payload).encode()
    request=Request('https://api.mercadopago.com'+path,data=raw,headers=headers,method=method)
    try:
        with build_opener(NoRedirect()).open(request,timeout=8) as response:
            data=response.read(1000001)
        if len(data)>1000000:raise ValueError()
        result=json.loads(data)
        if not isinstance(result,dict):raise ValueError()
        return result
    except (HTTPError,URLError,OSError,ValueError,TypeError) as error:
        # Provider bodies, card metadata and tokens must never enter responses/logs.
        raise PaymentUnavailable() from None

def verify_seller():
    require_setup()
    seller=request_api('GET','/users/me')
    if (str(seller.get('id'))!=settings.MERCADOPAGO_TEST_SELLER_ID or
        not isinstance(seller.get('tags'),list) or 'test_user' not in seller['tags'] or seller.get('site_id')!='MPE'):
        raise PaymentUnavailable('Solo se admite un vendedor de prueba de Perú. No se creará un pago con una cuenta real.')
    return str(seller['id'])

def valid_order_id(value):
    return isinstance(value,str) and re.fullmatch(r'ORDTST[A-Za-z0-9]{1,70}',value)

def checkout_link(value,order_id):
    parsed=urlsplit(value)
    from urllib.parse import parse_qs
    return (parsed.scheme=='https' and parsed.netloc in ('www.mercadopago.com.pe','mercadopago.com.pe')
        and parsed.path=='/checkout/v1/redirect' and parse_qs(parsed.query).get('order_id')==[order_id])

def active_pro(account):
    return account.plan=='pro' and account.active and (account.expires_at is None or account.expires_at>timezone.now())

def create_checkout(user,request_id):
    seller=verify_seller()  # Never POST before proving the seller is a test account.
    with transaction.atomic():
        owner=User.objects.select_for_update().get(pk=user.pk)
        account,_=AccountPlan.objects.get_or_create(user=owner)
        order=PaymentOrder.objects.filter(user=owner,request_id=request_id).first()
        if order:
            if order.provider_id:return order
            if order.seller_id!=seller:raise PaymentConflict('El vendedor de este intento cambió; revisa la operación anterior.')
        else:
            if active_pro(account) or PaymentOrder.objects.filter(user=owner,status__in=['new','ready','pending','review']).exists():
                raise PaymentConflict()
            amount=Decimal(settings.SIMULATED_PRO_PRICE)
            if (not amount.is_finite() or amount<=0 or amount>=Decimal('100000000') or amount!=amount.quantize(Decimal('.01'))
                or not 1<=settings.SIMULATED_PRO_DAYS<=365 or settings.SIMULATED_PRO_CURRENCY!='PEN'):
                raise PaymentUnavailable('La tarifa del plan no es válida para este pago de prueba.')
            order=PaymentOrder.objects.create(user=owner,request_id=request_id,seller_id=seller,amount=amount,
                currency='PEN',duration_days=settings.SIMULATED_PRO_DAYS)
            back=settings.MERCADOPAGO_RETURN_URL+'?'+urlencode({'checkout':str(order.id)})
            order.checkout_payload={
                'type':'online','processing_mode':'manual','capture_mode':'automatic_async',
                'external_reference':str(order.id),'total_amount':str(order.amount),
                'expiration_time':'P1D',
                'payer':{'email':settings.MERCADOPAGO_TEST_BUYER_EMAIL},
                'items':[{'title':'Acceso Pro por '+str(order.duration_days)+' días','quantity':1,'unit_price':str(order.amount)}],
                'config':{'online':{'success_url':back,'failure_url':back,'pending_url':back,'auto_return':'all'}},
            }
            order.save(update_fields=['checkout_payload'])
    result=request_api('POST','/v1/orders',order.checkout_payload,key=order.id)
    identifier=result.get('id')
    if (not valid_order_id(identifier) or result.get('external_reference')!=str(order.id) or
        str(result.get('user_id'))!=seller or result.get('currency')!=order.currency or
        result.get('country_code')!='PER' or not checkout_link(result.get('checkout_url',''),identifier)):
        raise PaymentUnavailable('Mercado Pago no confirmó una orden de prueba válida.')
    try:
        if Decimal(str(result.get('total_amount',-1)))!=order.amount:raise PaymentUnavailable()
    except (ValueError,TypeError,ArithmeticError):raise PaymentUnavailable() from None
    with transaction.atomic():
        locked=PaymentOrder.objects.select_for_update().get(pk=order.pk)
        if locked.provider_id and locked.provider_id!=identifier:raise PaymentConflict()
        locked.provider_id=identifier;locked.checkout_url=result['checkout_url']
        if locked.status=='new':locked.status='ready'
        locked.save(update_fields=['provider_id','checkout_url','status','updated_at'])
    return locked

def verify_signature(signature,request_id,data_id):
    if not valid_order_id(data_id) or not re.fullmatch(r'[A-Za-z0-9-]{1,100}',request_id or ''):return False
    if not isinstance(signature,str) or len(signature)>300:return False
    try:
        parts={}
        for fragment in signature.split(','):
            key,value=fragment.strip().split('=',1)
            if key in parts:return False
            parts[key]=value
        if set(parts)!={'ts','v1'} or not re.fullmatch(r'[0-9]{1,16}',parts['ts']) or not re.fullmatch(r'[a-fA-F0-9]{64}',parts['v1']):return False
        # Preserve identifier case, as required by current official SDK validators.
        manifest=f"id:{data_id};request-id:{request_id};ts:{parts['ts']};"
        expected=hmac.new(settings.MERCADOPAGO_WEBHOOK_SECRET.encode(),manifest.encode(),hashlib.sha256).hexdigest()
        return bool(settings.MERCADOPAGO_WEBHOOK_SECRET) and hmac.compare_digest(expected,parts['v1'].lower())
    except (ValueError,TypeError):return False

def verify_remote(order,data):
    try:
        if (data['id']!=order.provider_id or not valid_order_id(data['id']) or
            data['external_reference']!=str(order.id) or str(data['user_id'])!=order.seller_id or
            order.seller_id!=settings.MERCADOPAGO_TEST_SELLER_ID or data['currency']!=order.currency or
            data['country_code']!='PER' or data.get('live_mode') is True or
            Decimal(str(data['total_amount']))!=order.amount):
            raise ValueError()
        return data['status'],data.get('status_detail','')
    except (KeyError,TypeError,ValueError,ArithmeticError):
        raise PaymentUnavailable('La confirmación recibida no corresponde a esta operación de prueba.') from None

def reconcile(order_id):
    require_setup()
    order=PaymentOrder.objects.get(pk=order_id)
    if not order.provider_id:return order
    verify_seller()
    data=request_api('GET','/v1/orders/'+order.provider_id)
    status,detail=verify_remote(order,data)
    paid=False
    if status=='processed' and detail=='accredited':
        try:
            payments=data['transactions']['payments']
            paid=bool(payments) and Decimal(str(data['total_paid_amount']))==order.amount and all(
                p['status']=='processed' and p['status_detail']=='accredited' for p in payments
            ) and sum(Decimal(str(p['paid_amount'])) for p in payments)==order.amount
        except (KeyError,TypeError,ValueError,ArithmeticError):paid=False
        if not paid:raise PaymentUnavailable('La operación no confirma el importe completo. Pro todavía no se activó.')
    with transaction.atomic():
        if order.user_id:
            user=User.objects.select_for_update().filter(pk=order.user_id).first()
        else:user=None
        locked=PaymentOrder.objects.select_for_update().get(pk=order.pk)
        if locked.status in ('refunded','cancelled') and status not in ('refunded','canceled'):return locked
        if status=='refunded' or (status=='processed' and detail=='partially_refunded') or data.get('transactions',{}).get('chargebacks'):
            locked.status='refunded'
            if user and locked.receipt_id:
                account=AccountPlan.objects.select_for_update().filter(user=user).first()
                if account and account.source_transaction_id==locked.receipt_id:
                    account.active=False;account.auto_renew=False
                    account.save(update_fields=['active','auto_renew'])
        elif paid and locked.notification_verified_at and user and user.is_active:
            if not locked.receipt_id:
                account,_=AccountPlan.objects.select_for_update().get_or_create(user=user)
                if active_pro(account):
                    locked.status='review'  # Do not silently replace unrelated active access.
                else:
                    now=timezone.now();end=now+timedelta(days=locked.duration_days)
                    receipt=SubscriptionTransaction.objects.create(user=user,request_id=locked.id,outcome='approved',
                        amount=locked.amount,currency=locked.currency,duration_days=locked.duration_days,
                        starts_at=now,ends_at=end,email_status='pending')
                    locked.receipt=receipt;locked.status='approved'
                    account.plan='pro';account.active=True;account.expires_at=end
                    account.auto_renew=False;account.cancelled_at=None;account.source_transaction=receipt;account.save()
                    from .subscription_views import deliver_receipt
                    transaction.on_commit(lambda:deliver_receipt(receipt.id),robust=True)
            else:locked.status='approved'
        elif locked.receipt_id:
            # A late created/pending event never revokes already verified access.
            pass
        elif status in ('failed','canceled'):
            locked.status='declined' if status=='failed' else 'cancelled'
        else:
            locked.status='review' if paid else 'pending'
        locked.save(update_fields=['receipt','status','updated_at'])
    return locked

def order_data(order):
    labels={'new':'Preparando pago','ready':'Listo para pagar','pending':'Pendiente de confirmación',
            'review':'Esperando confirmación o revisión','approved':'Pago de prueba aprobado',
            'declined':'Pago rechazado','cancelled':'Pago cancelado o vencido','refunded':'Pago reembolsado'}
    return {'id':str(order.id),'request_id':str(order.request_id),'status':order.status,
            'status_label':labels.get(order.status,'Pendiente'),'provider':'mercadopago_test','test_mode':True,
            'amount':str(order.amount),'currency':order.currency,'duration_days':order.duration_days,
            'checkout_url':order.checkout_url if order.status in ('ready','pending') else '',
            'created_at':order.created_at,'receipt_id':str(order.receipt_id) if order.receipt_id else None}
