"""HU13: usuarios, permisos administrativos, perfil y eliminación propia."""
from io import BytesIO
from PIL import Image, ImageDraw, ImageOps, UnidentifiedImageError
from django.db import transaction
from django.db.models import Q
from django.http import HttpResponse
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import serializers
from rest_framework.exceptions import PermissionDenied, ValidationError
from rest_framework.pagination import PageNumberPagination
from rest_framework.permissions import BasePermission, IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework_simplejwt.token_blacklist.models import OutstandingToken
from .access_views import subscription
from .models import AccountPlan, AdministrationEvent, User, UserAvatar
from .personal_views import Conflict


def prepare_avatar(upload):
    if upload.size >= 2 * 1024 * 1024:
        raise ValidationError('La imagen debe ocupar menos de 2 MB.')
    try:
        image = Image.open(upload)
        if image.format not in ('JPEG', 'PNG', 'WEBP') or image.width * image.height > 10_000_000:
            raise ValueError('Formato o dimensiones no admitidos.')
        image.load()
        crop = ImageOps.fit(ImageOps.exif_transpose(image).convert('RGBA'), (256, 256))
        mask = Image.new('L', (256, 256), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, 255, 255), fill=255)
        normalized = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
        normalized.paste(crop, (0, 0), mask)
        result = BytesIO()
        normalized.save(result, format='PNG')
        return result.getvalue()
    except (OSError, UnidentifiedImageError, ValueError, Image.DecompressionBombError) as error:
        raise ValidationError('Selecciona una imagen JPEG, PNG o WebP válida, de hasta 10 megapíxeles.') from error


def is_administrator(user):
    return user.is_active and user.is_staff and user.is_superuser


class IsPlatformAdministrator(BasePermission):
    def has_permission(self, request, view):
        return request.user.is_authenticated and is_administrator(request.user)


def lock_administration(actor, subject_id):
    # Todas las operaciones comparten los administradores activos y el mismo orden de bloqueo.
    administrators = Q(is_active=True, is_staff=True, is_superuser=True)
    list(User.objects.select_for_update().filter(administrators | Q(pk=actor.pk) | Q(pk=subject_id)).order_by('pk'))
    current_actor = User.objects.get(pk=actor.pk)
    if not is_administrator(current_actor):
        raise PermissionDenied('Se requiere un administrador autorizado.')
    return get_object_or_404(User, pk=subject_id)


def protect_last_administrator(user, role, active):
    if is_administrator(user) and (role != 'admin' or not active):
        if User.objects.filter(is_active=True, is_staff=True, is_superuser=True).count() <= 1:
            raise Conflict('Debe conservarse al menos un administrador activo.')


def user_data(user):
    return {'id': user.pk, 'username': user.username, 'first_name': user.first_name,
            'last_name': user.last_name, 'email': user.email,
            'role': 'admin' if user.is_staff or user.is_superuser else 'user',
            'plan': subscription(user)['effective_plan'], 'is_active': user.is_active,
            'blocked_reason': user.blocked_reason}


class UserFilters(serializers.Serializer):
    q = serializers.CharField(max_length=120, required=False, default='', allow_blank=True)
    plan = serializers.ChoiceField(choices=('all', 'free', 'pro'), default='all')
    state = serializers.ChoiceField(choices=('all', 'active', 'blocked'), default='all')


class UserChange(serializers.Serializer):
    role = serializers.ChoiceField(choices=('admin', 'user'), required=False)
    plan = serializers.ChoiceField(choices=('free', 'pro'), required=False)
    is_active = serializers.BooleanField(required=False)
    reason = serializers.CharField(max_length=240, required=False, allow_blank=True)

    def to_internal_value(self, data):
        if not isinstance(data, dict) or set(data) - set(self.fields):
            raise ValidationError({'non_field_errors': ['Hay campos administrativos no admitidos.']})
        return super().to_internal_value(data)

    def validate(self, data):
        if not {'role', 'plan', 'is_active'} & set(data):
            raise ValidationError('Selecciona un cambio de rol, plan o estado.')
        if data.get('is_active') is False and not data.get('reason', '').strip():
            raise ValidationError({'reason': 'Indica un motivo para bloquear la cuenta.'})
        return data


class AdminUsersView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def get(self, request):
        form = UserFilters(data=request.query_params)
        form.is_valid(raise_exception=True)
        filters = form.validated_data
        users = User.objects.select_related('account_plan').order_by('id')
        if filters['q']:
            text = filters['q']
            users = users.filter(Q(username__icontains=text) | Q(email__icontains=text)
                                 | Q(first_name__icontains=text) | Q(last_name__icontains=text))
        if filters['state'] != 'all':
            users = users.filter(is_active=filters['state'] == 'active')
        pro = Q(account_plan__plan='pro', account_plan__active=True) & (
            Q(account_plan__expires_at__isnull=True) | Q(account_plan__expires_at__gt=timezone.now()))
        if filters['plan'] == 'pro':
            users = users.filter(pro)
        elif filters['plan'] == 'free':
            users = users.exclude(pro)
        pagination = PageNumberPagination()
        pagination.page_size = 20
        page = pagination.paginate_queryset(users, request, view=self)
        response = pagination.get_paginated_response([user_data(user) for user in page])
        response['Cache-Control'] = 'private, no-store'
        return response


class AdminUserView(APIView):
    permission_classes = (IsPlatformAdministrator,)

    def patch(self, request, user_id):
        form = UserChange(data=request.data)
        form.is_valid(raise_exception=True)
        changes = form.validated_data
        with transaction.atomic():
            user = lock_administration(request.user, user_id)
            before = user_data(user)
            role = changes.get('role', before['role'])
            active = changes.get('is_active', user.is_active)
            protect_last_administrator(user, role, active)
            revoke = ('role' in changes and role != before['role']) or (user.is_active and not active)
            if 'role' in changes:
                user.is_staff = user.is_superuser = role == 'admin'
            user.is_active = active
            if 'is_active' in changes:
                user.blocked_reason = changes.get('reason', '').strip() if not active else ''
            if revoke:
                user.session_version += 1
            user.save(update_fields=['is_staff', 'is_superuser', 'is_active', 'blocked_reason', 'session_version'])
            if 'plan' in changes:
                plan, _ = AccountPlan.objects.get_or_create(user=user)
                plan.plan, plan.active = changes['plan'], True
                plan.auto_renew, plan.cancelled_at, plan.expires_at = False, None, None
                plan.source_transaction = None
                plan.save()
            after = user_data(User.objects.select_related('account_plan').get(pk=user.pk))
            # El registro conserva estados, sin correo, nombre ni motivo libre tras una eliminación.
            fields = ('role', 'plan', 'is_active')
            AdministrationEvent.objects.create(actor=request.user, subject=user, action='user_changed',
                changes={field: {'before': before[field], 'after': after[field]} for field in fields if before[field] != after[field]})
        return Response(after, headers={'Cache-Control': 'private, no-store'})


class UserAvatarView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request):
        avatar = get_object_or_404(UserAvatar, user=request.user)
        response = HttpResponse(bytes(avatar.content), content_type='image/png')
        response['Cache-Control'] = 'private, no-store'
        response['X-Content-Type-Options'] = 'nosniff'
        return response


class DeleteOwnAccountView(APIView):
    permission_classes = (IsAuthenticated,)

    def delete(self, request):
        password = request.data.get('password')
        if not isinstance(password, str) or set(request.data) != {'password'}:
            raise ValidationError('Confirma únicamente tu contraseña actual.')
        with transaction.atomic():
            administrators = Q(is_active=True, is_staff=True, is_superuser=True)
            list(User.objects.select_for_update().filter(administrators | Q(pk=request.user.pk)).order_by('pk'))
            user = get_object_or_404(User, pk=request.user.pk)
            if not user.check_password(password):
                raise PermissionDenied('La contraseña actual no coincide.')
            protect_last_administrator(user, 'user', False)
            AdministrationEvent.objects.create(actor=user, subject=user, action='account_deleted')
            OutstandingToken.objects.filter(user=user).delete()
            user.delete()
        return Response(status=204)
