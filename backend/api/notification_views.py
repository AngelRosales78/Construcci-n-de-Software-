from django.db import transaction
from django.db.models import Max
from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.exceptions import ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.utils.urls import replace_query_param
from rest_framework.views import APIView
from django.utils import timezone
from .market_views import ExactSerializer
from .models import Notification, NotificationPreferences, User
from .personal_views import Conflict
from .delivery_services import available_channels

CHANNEL_MESSAGES = {'email': 'El canal de correo para alertas aún no está habilitado.',
                    'push': 'Activa este canal cuando los avisos móviles estén disponibles en tu dispositivo.'}


def private(data, status=200):
    return Response(data,status=status,headers={'Cache-Control':'private, no-store'})


def notification_data(item):
    event = item.event
    return {'id': item.pk, 'type': 'rating_change', 'created_at': item.created_at,
        'read_at': item.read_at, 'ticker': event.company.ticker, 'company_name': event.company.name,
        'url': '/company/'+event.company.ticker, 'event_at': event.created_at,
        'is_demo': event.is_demo,
        'title': ('Ejemplo de aviso: ' if event.is_demo else 'Cambió el perfil de ')+event.company.ticker,
        'message': f"{event.before['label']} → {event.after['label']}. Perfil según Altman-Z.",
        'before': event.before, 'after': event.after}


class NotificationFilters(serializers.Serializer):
    through_id = serializers.IntegerField(min_value=0,required=False)


class NotificationsView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self,request):
        form=NotificationFilters(data=request.query_params); form.is_valid(raise_exception=True)
        base=Notification.objects.filter(user=request.user,web_visible=True,event__is_demo=False)
        anchor=form.validated_data.get('through_id')
        if anchor is None:
            anchor=base.aggregate(last=Max('id'))['last'] or 0
        rows=base.filter(id__lte=anchor).select_related('event__company').order_by('-id')
        page=PageNumberPagination(); page.page_size=10
        result=page.get_paginated_response([notification_data(item) for item in page.paginate_queryset(rows,request,self)])
        for field in ('next','previous'):
            if result.data[field]: result.data[field]=replace_query_param(result.data[field],'through_id',str(anchor))
        result.data.update(owner_id=str(request.user.pk),unread_count=base.filter(read_at__isnull=True).count(),read_through=anchor)
        result['Cache-Control']='private, no-store'
        return result


class ReadRequest(ExactSerializer):
    ids=serializers.ListField(child=serializers.IntegerField(min_value=1),max_length=100,required=False)
    all=serializers.BooleanField(default=False)
    through_id=serializers.IntegerField(min_value=0,required=False)

    def validate(self,data):
        if data['all']:
            if 'through_id' not in data or 'ids' in data: raise ValidationError('Indica el límite de la lista para marcar todos los avisos.')
        elif not data.get('ids') or 'through_id' in data:
            raise ValidationError('Indica los avisos propios que deseas marcar.')
        return data


class MarkNotificationsReadView(APIView):
    permission_classes=(IsAuthenticated,)

    def post(self,request):
        form=ReadRequest(data=request.data); form.is_valid(raise_exception=True)
        with transaction.atomic():
            get_object_or_404(User.objects.select_for_update(),pk=request.user.pk)
            rows=Notification.objects.filter(user=request.user,web_visible=True,event__is_demo=False)
            target=rows.filter(id__lte=form.validated_data['through_id']) if form.validated_data['all'] else rows.filter(id__in=form.validated_data['ids'])
            if not form.validated_data['all'] and target.count()!=len(set(form.validated_data['ids'])):
                return private({'detail':'Aviso no encontrado.'},404)
            changed=target.filter(read_at__isnull=True).update(read_at=timezone.now())
            return private({'owner_id':str(request.user.pk),'updated':changed,'unread_count':rows.filter(read_at__isnull=True).count()})


def preferences_data(user,row=None):
    return {'owner_id':str(user.pk),'version':row.version if row else 0,
        'rating_changes':row.rating_changes if row else True,
        'channels':{'web':row.web_enabled if row else True,'email':row.email_enabled if row else False,'push':row.push_enabled if row else False},
        'available_channels':available_channels(user),'channel_messages':CHANNEL_MESSAGES,
        'updated_at':row.updated_at if row else None}


class Channels(ExactSerializer):
    web=serializers.BooleanField()
    email=serializers.BooleanField()
    push=serializers.BooleanField()

    def validate(self,data):
        for channel,enabled in data.items():
            user=getattr(self.context.get('request'),'user',None)
            if enabled and not available_channels(user)[channel]: raise ValidationError({channel:CHANNEL_MESSAGES[channel]})
        return data


class PreferenceRequest(ExactSerializer):
    version=serializers.IntegerField(min_value=0)
    rating_changes=serializers.BooleanField()
    channels=Channels()


class NotificationPreferencesView(APIView):
    permission_classes=(IsAuthenticated,)

    def get(self,request):
        return private(preferences_data(request.user,NotificationPreferences.objects.filter(user=request.user).first()))

    def put(self,request):
        form=PreferenceRequest(data=request.data,context={'request':request}); form.is_valid(raise_exception=True)
        data=form.validated_data
        with transaction.atomic():
            get_object_or_404(User.objects.select_for_update(),pk=request.user.pk)
            row,_=NotificationPreferences.objects.get_or_create(user=request.user)
            if row.version!=data['version']: raise Conflict('Las preferencias cambiaron en otra pantalla. Recarga antes de guardar.')
            values={'rating_changes':data['rating_changes'],**{key+'_enabled':value for key,value in data['channels'].items()}}
            if any(getattr(row,key)!=value for key,value in values.items()):
                for key,value in values.items(): setattr(row,key,value)
                row.version+=1
                row.save()
            return private(preferences_data(request.user,row))
