from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.throttling import UserRateThrottle
from rest_framework.views import APIView
from django.conf import settings
from django.http import HttpResponseRedirect
from urllib.parse import urlencode
from uuid import UUID
from .models import PaymentOrder
from .payments import create_checkout, order_data, reconcile, verify_signature, PaymentUnavailable
from .subscription_views import PrivateSubscriptionView
from .access_views import subscription

class PaymentThrottle(UserRateThrottle):
    scope = 'payment_operations'
    rate = '12/min'

class PaymentInput(serializers.Serializer):
    request_id = serializers.UUIDField()
    def to_internal_value(self,data):
        if not isinstance(data,dict) or set(data)!={'request_id'}:
            raise serializers.ValidationError({'non_field_errors':['Solo se admite el identificador del intento; el importe y el resultado los verifica el servidor.']})
        return super().to_internal_value(data)

class PaymentCheckoutView(PrivateSubscriptionView):
    throttle_classes = [PaymentThrottle]
    def post(self,request):
        form=PaymentInput(data=request.data);form.is_valid(raise_exception=True)
        order=create_checkout(request.user,form.validated_data['request_id'])
        return Response({'payment':order_data(order),'subscription':subscription(request.user)},status=201)

class PaymentStatusView(PrivateSubscriptionView):
    throttle_classes = [PaymentThrottle]
    def get(self,request,order_id):
        order=get_object_or_404(PaymentOrder,user=request.user,pk=order_id)
        return Response({'payment':order_data(order),'subscription':subscription(request.user)})
    def post(self,request,order_id):
        if request.data:raise serializers.ValidationError('El resultado se consulta directamente con Mercado Pago.')
        order=get_object_or_404(PaymentOrder,user=request.user,pk=order_id)
        order=reconcile(order.pk)
        return Response({'payment':order_data(order),'subscription':subscription(request.user)})

class MercadoPagoWebhookView(APIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    def post(self,request):
        identifier=request.query_params.get('data.id','')
        if len(request.query_params.getlist('data.id'))!=1 or not verify_signature(
            request.headers.get('x-signature',''),request.headers.get('x-request-id',''),identifier):
            return Response({'detail':'Firma de notificación no válida.'},status=401)
        body=request.data
        if (not isinstance(body,dict) or body.get('live_mode') is not False or
            body.get('type') not in ('order','orders_v2') or not isinstance(body.get('data'),dict) or
            body['data'].get('id')!=identifier):
            return Response({'detail':'Notificación de prueba no válida.'},status=400)
        order=PaymentOrder.objects.filter(provider_id=identifier).first()
        if not order:return Response({'received':True})
        # Signature authenticates the event. Fulfillment still requires a fresh
        # provider GET with the expected seller, currency, reference and amount.
        with transaction.atomic():
            locked=PaymentOrder.objects.select_for_update().get(pk=order.pk)
            locked.notification_verified_at=timezone.now()
            locked.save(update_fields=['notification_verified_at'])
        reconcile(order.pk)
        return Response({'received':True})


class MercadoPagoReturnView(APIView):
    authentication_classes = []
    permission_classes = [AllowAny]
    def get(self, request):
        # Redirect to the configured local web app, never to a URL from the query.
        destination=settings.FRONTEND_URL.rstrip('/')+'/profile/subscription'
        try:
            operation=str(UUID(request.query_params.get('checkout','')))
            destination+='?'+urlencode({'checkout':operation})
        except (ValueError,TypeError,AttributeError):
            pass
        response=HttpResponseRedirect(destination)
        response['Cache-Control']='no-store'
        return response
