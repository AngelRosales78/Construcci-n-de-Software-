from django.db import transaction
from django.shortcuts import get_object_or_404
from rest_framework import serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from .market_views import ExactSerializer
from .models import PushInstallation, User
from .delivery_services import available_channels


class InstallationInput(ExactSerializer):
    installation_id=serializers.UUIDField()
    token=serializers.CharField(min_length=20,max_length=4096,trim_whitespace=True)
    permission_granted=serializers.BooleanField()


class PushInstallationsView(APIView):
    permission_classes=(IsAuthenticated,)

    def get(self,request):
        rows=PushInstallation.objects.filter(user=request.user,active=True)
        return Response({'owner_id':str(request.user.pk),'configured':available_channels()['push'],
            'installations':[{'installation_id':str(row.installation_id),'permission_granted':row.permission_granted,'updated_at':row.updated_at} for row in rows]},
            headers={'Cache-Control':'private, no-store'})

    def post(self,request):
        form=InstallationInput(data=request.data);form.is_valid(raise_exception=True)
        data=form.validated_data
        with transaction.atomic():
            get_object_or_404(User.objects.select_for_update(),pk=request.user.pk)
            PushInstallation.objects.filter(token=data['token']).exclude(installation_id=data['installation_id']).delete()
            row,_=PushInstallation.objects.update_or_create(installation_id=data['installation_id'],defaults={
                'user':request.user,'token':data['token'],'permission_granted':data['permission_granted'],'active':True,
                'session_id':str(request.auth.get('sid',''))[:64] if request.auth else ''})
        return Response({'owner_id':str(request.user.pk),'installation_id':str(row.installation_id),'configured':available_channels()['push']},status=200,
                        headers={'Cache-Control':'private, no-store'})

    def delete(self,request,installation_id):
        PushInstallation.objects.filter(user=request.user,installation_id=installation_id).update(active=False)
        return Response(status=204)
