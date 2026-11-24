"""Authenticated personal collections shared by Android and Web."""
import re
import unicodedata
from django.conf import settings
from django.db import transaction
from django.db.models import F
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.decorators import api_view, permission_classes
from rest_framework.permissions import IsAuthenticated
from rest_framework.permissions import AllowAny
from rest_framework.response import Response
from rest_framework.exceptions import ValidationError, APIException
from .models import Company, Favorite, FavoriteTag, PersonalVersion, CompanyVisit, VisitReceipt


class Conflict(APIException):
    status_code = 409
    default_detail = 'Los datos cambiaron. Actualiza antes de repetir la operación.'


@api_view(['GET'])
@permission_classes([AllowAny])
def service_health(request):
    return Response({'schema_version': 'android-services-v1', 'sprint': 3,
                     'project': getattr(settings, 'ANDROID_PROJECT_NAME', None),
                     'services': ['favorites', 'tags', 'recent', 'compare', 'conclusion', 'subscription', 'reports', 'payments']})


def favorite_row(row):
    return {'id': row.pk, 'ticker': row.company.ticker, 'name': row.company.name}


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def favorites(request):
    base = {'schema_version': 'favorites-v1', 'owner_id': str(request.user.pk)}
    if request.method == 'GET':
        return Response(base | {'results': [favorite_row(x) for x in Favorite.objects.filter(user=request.user).select_related('company')]})
    ticker = request.data.get('ticker')
    if not isinstance(ticker, str) or not re.fullmatch(r'[A-Z0-9][A-Z0-9.\-]{0,9}', ticker):
        raise ValidationError({'ticker': 'Símbolo no válido.'})
    company = get_object_or_404(Company, ticker=ticker)
    row, created = Favorite.objects.get_or_create(user=request.user, company=company)
    return Response(base | {'favorite': favorite_row(row)}, status=201 if created else 200)


@api_view(['DELETE'])
@permission_classes([IsAuthenticated])
def favorite_delete(request, pk):
    row = get_object_or_404(Favorite, user=request.user, pk=pk)
    with transaction.atomic():
        row.delete()
        PersonalVersion.objects.get_or_create(user=request.user)
        PersonalVersion.objects.filter(user=request.user).update(version=F('version') + 1)
    return Response(status=204)


def tags_snapshot(user):
    state, _ = PersonalVersion.objects.get_or_create(user=user)
    tags = FavoriteTag.objects.filter(user=user).order_by('id')
    rows = Favorite.objects.filter(user=user).prefetch_related('tags')
    return {'schema_version': 'tags-v1', 'owner_id': str(user.pk), 'version': str(state.version),
            'tags': [{'id': t.pk, 'name': t.name, 'color': t.color} for t in tags],
            'assignments': [{'favorite_id': f.pk, 'tag_ids': sorted(t.pk for t in f.tags.all())} for f in rows]}


def claim_version(user, expected):
    state, _ = PersonalVersion.objects.get_or_create(user=user)
    if not isinstance(expected, str) or expected != str(state.version):
        raise Conflict()
    if not PersonalVersion.objects.filter(pk=state.pk, version=state.version).update(version=F('version') + 1):
        raise Conflict()


@api_view(['GET', 'POST', 'PATCH', 'DELETE'])
@permission_classes([IsAuthenticated])
def tags(request, pk=None):
    if request.method == 'GET' and pk is None:
        return Response(tags_snapshot(request.user))
    if (pk is None and request.method != 'POST') or (pk is not None and request.method not in ('PATCH', 'DELETE')):
        return Response(status=405)
    with transaction.atomic():
        row = get_object_or_404(FavoriteTag, user=request.user, pk=pk) if pk else None
        claim_version(request.user, request.query_params.get('version') if request.method == 'DELETE' else request.data.get('version'))
        if request.method == 'DELETE':
            row.delete()
        else:
            name, color = request.data.get('name'), request.data.get('color')
            if not isinstance(name, str) or not 1 <= len(name.strip()) <= 40 or any(unicodedata.category(c) == 'Cc' for c in name):
                raise ValidationError({'name': 'Usa de uno a cuarenta caracteres sin controles.'})
            if not isinstance(color, str) or not re.fullmatch(r'#[0-9a-fA-F]{6}', color):
                raise ValidationError({'color': 'Usa un color hexadecimal de seis dígitos.'})
            name = re.sub(r'\s+', ' ', name.strip())
            normalized = unicodedata.normalize('NFC', name).lower()
            if FavoriteTag.objects.filter(user=request.user, normalized_name=normalized).exclude(pk=pk).exists():
                raise Conflict('Ya existe una etiqueta con ese nombre.')
            if row is None:
                if FavoriteTag.objects.filter(user=request.user).count() >= 100:
                    raise ValidationError('Máximo de cien etiquetas por cuenta.')
                row = FavoriteTag(user=request.user)
            row.name, row.color, row.normalized_name = name, color, normalized
            row.save()
        return Response(tags_snapshot(request.user), status=201 if pk is None else 200)


@api_view(['PUT'])
@permission_classes([IsAuthenticated])
def assign_tags(request, pk):
    with transaction.atomic():
        favorite = get_object_or_404(Favorite, user=request.user, pk=pk)
        ids = request.data.get('tag_ids')
        if not isinstance(ids, list) or len(ids) > 100 or any(type(i) is not int or i <= 0 for i in ids) or len(set(ids)) != len(ids):
            raise ValidationError({'tag_ids': 'Lista de identificadores únicos no válida.'})
        selected = FavoriteTag.objects.filter(user=request.user, pk__in=ids)
        if selected.count() != len(ids):
            raise ValidationError({'tag_ids': 'Todas las etiquetas deben pertenecer a tu cuenta.'})
        claim_version(request.user, request.data.get('version'))
        favorite.tags.set(selected)
        return Response(tags_snapshot(request.user))


def recent_snapshot(user):
    rows = CompanyVisit.objects.filter(user=user).select_related('company').order_by('-visited_at', '-id')[:10]
    return {'schema_version': 'recent-companies-v1', 'owner_id': str(user.pk),
            'results': [{'ticker': r.company.ticker, 'name': r.company.name, 'visited_at': r.visited_at.isoformat()} for r in rows]}


@api_view(['GET', 'POST'])
@permission_classes([IsAuthenticated])
def recent(request, ticker=None):
    if request.method == 'GET' and ticker is None:
        return Response(recent_snapshot(request.user))
    if request.method != 'POST' or ticker is None:
        return Response(status=405)
    event = request.data.get('event_id')
    if not isinstance(event, str) or not re.fullmatch(r'[a-zA-Z0-9-]{1,80}', event):
        raise ValidationError({'event_id': 'Identificador de visita no válido.'})
    company = get_object_or_404(Company, ticker=ticker)
    with transaction.atomic():
        receipt, created = VisitReceipt.objects.get_or_create(user=request.user, event_id=event, defaults={'company': company})
        if receipt.company_id != company.pk:
            raise Conflict('Este identificador de visita ya corresponde a otra empresa.')
        if created:
            CompanyVisit.objects.update_or_create(user=request.user, company=company, defaults={'visited_at': timezone.now()})
        return Response(recent_snapshot(request.user))
