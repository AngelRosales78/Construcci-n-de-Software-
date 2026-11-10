"""HU10: datos personales compartidos, versiones y eventos idempotentes."""
import re
import unicodedata
from django.contrib.auth import get_user_model
from django.db import transaction
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework.exceptions import APIException, ValidationError
from rest_framework.response import Response
from rest_framework.views import APIView
from .models import Company, Favorite, FavoriteTag, PersonalState, RecentCompany, VisitEvent


class Conflict(APIException):
    status_code = 409
    default_detail = 'La lista cambió. Actualízala antes de volver a guardar.'


def lock_owner(user):
    get_user_model().objects.select_for_update().get(pk=user.pk)
    return PersonalState.objects.get_or_create(user=user)[0]


def bump(state):
    state.version += 1
    state.save(update_fields=['version'])


def tag_snapshot(user):
    state, _ = PersonalState.objects.get_or_create(user=user)
    tags = list(FavoriteTag.objects.filter(user=user).prefetch_related('favorites'))
    assignments = {row.pk: [] for row in Favorite.objects.filter(user=user)}
    for tag in tags:
        for favorite in tag.favorites.all():
            if favorite.pk in assignments:
                assignments[favorite.pk].append(tag.pk)
    return {'schema_version': 'tags-v1', 'owner_id': str(user.pk), 'version': state.version,
            'tags': [{'id': t.pk, 'name': t.name, 'color': t.color} for t in tags],
            'assignments': [{'favorite_id': key, 'tag_ids': value} for key, value in assignments.items()]}


def tag_fields(data):
    name, color = data.get('name'), data.get('color')
    if not isinstance(name, str) or not 1 <= len(name.strip()) <= 40:
        raise ValidationError({'name': 'Use un nombre de una a cuarenta letras o caracteres.'})
    name = ' '.join(unicodedata.normalize('NFKC', name).split())
    if not name or len(name) > 40 or any(unicodedata.category(c).startswith('C') for c in name):
        raise ValidationError({'name': 'El nombre no es válido.'})
    if not isinstance(color, str) or not re.fullmatch(r'#[0-9a-fA-F]{6}', color):
        raise ValidationError({'color': 'Use un color hexadecimal de seis dígitos.'})
    return {'name': name, 'normalized_name': name.casefold(), 'color': color.lower()}


def expected_version(value, state):
    if isinstance(value, bool) or not str(value).isdigit() or int(value) != state.version:
        raise Conflict()


class TagsView(APIView):
    def get(self, request):
        with transaction.atomic():
            lock_owner(request.user)
            return Response(tag_snapshot(request.user), headers={'Cache-Control': 'private, no-store'})

    @transaction.atomic
    def post(self, request):
        state = lock_owner(request.user)
        fields = tag_fields(request.data)
        if FavoriteTag.objects.filter(user=request.user, normalized_name=fields['normalized_name']).exists():
            raise ValidationError({'name': 'Ya existe una etiqueta equivalente en tu cuenta.'})
        if FavoriteTag.objects.filter(user=request.user).count() >= 50:
            raise ValidationError('Se admiten hasta cincuenta etiquetas por cuenta.')
        FavoriteTag.objects.create(user=request.user, **fields)
        bump(state)
        return Response(tag_snapshot(request.user), status=201)


class TagDetailView(APIView):
    @transaction.atomic
    def patch(self, request, tag_id):
        state = lock_owner(request.user)
        tag = get_object_or_404(FavoriteTag, pk=tag_id, user=request.user)
        expected_version(request.data.get('version'), state)
        fields = tag_fields(request.data)
        if FavoriteTag.objects.filter(user=request.user, normalized_name=fields['normalized_name']).exclude(pk=tag.pk).exists():
            raise ValidationError({'name': 'Ya existe una etiqueta equivalente en tu cuenta.'})
        for key, value in fields.items():
            setattr(tag, key, value)
        tag.save()
        bump(state)
        return Response(tag_snapshot(request.user))

    @transaction.atomic
    def delete(self, request, tag_id):
        state = lock_owner(request.user)
        tag = get_object_or_404(FavoriteTag, pk=tag_id, user=request.user)
        expected_version(request.query_params.get('version'), state)
        tag.delete()
        bump(state)
        return Response(tag_snapshot(request.user))


class FavoriteTagsView(APIView):
    @transaction.atomic
    def put(self, request, favorite_id):
        state = lock_owner(request.user)
        favorite = get_object_or_404(Favorite, pk=favorite_id, user=request.user)
        expected_version(request.data.get('version'), state)
        ids = request.data.get('tag_ids')
        if (not isinstance(ids, list) or len(ids) > 50 or
                any(type(i) is not int or i <= 0 for i in ids) or len(ids) != len(set(ids))):
            raise ValidationError({'tag_ids': 'Seleccione etiquetas válidas sin repetirlas.'})
        tags = list(FavoriteTag.objects.filter(user=request.user, pk__in=ids))
        if len(tags) != len(ids):
            raise ValidationError({'tag_ids': 'Una etiqueta no pertenece a tu cuenta o ya no existe.'})
        favorite.tags.set(tags)
        bump(state)
        return Response(tag_snapshot(request.user))


def recent_snapshot(user):
    return {'schema_version': 'recent-companies-v1', 'owner_id': str(user.pk), 'results': [
        {'ticker': row.company.ticker, 'name': row.company.name, 'visited_at': row.visited_at.isoformat()}
        for row in RecentCompany.objects.filter(user=user).select_related('company').order_by('-visited_at', '-pk')[:10]]}


class RecentCompaniesView(APIView):
    def get(self, request):
        return Response(recent_snapshot(request.user), headers={'Cache-Control': 'private, no-store'})


class CompanyVisitView(APIView):
    @transaction.atomic
    def post(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        ident = request.data.get('event_id')
        if not isinstance(ident, str) or not re.fullmatch(r'[A-Za-z0-9-]{1,80}', ident):
            raise ValidationError({'event_id': 'Use un identificador de visita válido.'})
        lock_owner(request.user)
        event, created = VisitEvent.objects.get_or_create(user=request.user, event_id=ident, defaults={'company': company})
        if event.company_id != company.pk:
            raise Conflict('Ese evento ya identifica la visita a otra empresa.')
        if created:
            RecentCompany.objects.update_or_create(user=request.user, company=company, defaults={'visited_at': timezone.now()})
            retained = list(RecentCompany.objects.filter(user=request.user).order_by('-visited_at', '-pk').values_list('pk', flat=True)[:10])
            RecentCompany.objects.filter(user=request.user).exclude(pk__in=retained).delete()
        return Response(recent_snapshot(request.user))
