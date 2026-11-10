import re
import hashlib
import json
from django.db import transaction
from django.utils import timezone
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from rest_framework.response import Response
from .models import Company, Favorite
from .personal_views import lock_owner, bump


def favorite_data(favorite):
    return {'id': favorite.pk, 'ticker': favorite.company.ticker, 'name': favorite.company.name}


class FavoritesView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request):
        rows = Favorite.objects.filter(user=request.user).select_related('company')
        results = [favorite_data(row) for row in rows]
        response = Response({'schema_version': 'favorites-v1', 'owner_id': str(request.user.pk),
                             'version': hashlib.sha256(json.dumps(results, sort_keys=True).encode()).hexdigest(),
                             'fetched_at': timezone.now().isoformat(), 'results': results})
        response['Cache-Control'] = 'private, no-store'
        return response

    @transaction.atomic
    def post(self, request):
        state = lock_owner(request.user)
        ticker = request.data.get('ticker')
        if not isinstance(ticker, str) or not re.fullmatch(r'[A-Za-z0-9.\-]{1,10}', ticker.strip()):
            raise ValidationError({'ticker': 'Indique un símbolo válido del catálogo.'})
        company = get_object_or_404(Company, ticker=ticker.strip().upper())
        # get_or_create + restricción única resuelven reintentos y solicitudes simultáneas.
        favorite, created = Favorite.objects.get_or_create(user=request.user, company=company)
        if created:
            bump(state)
        return Response({'schema_version': 'favorites-v1', 'owner_id': str(request.user.pk),
                         'favorite': favorite_data(favorite)}, status=201 if created else 200)


class FavoriteDetailView(APIView):
    permission_classes = (IsAuthenticated,)

    @transaction.atomic
    def delete(self, request, favorite_id):
        state = lock_owner(request.user)
        _, deleted = Favorite.objects.filter(pk=favorite_id, user=request.user).delete()
        if not deleted.get(Favorite._meta.label):
            # Ausentes y ajenos tienen la misma respuesta.
            raise NotFound()
        bump(state)
        return Response(status=status.HTTP_204_NO_CONTENT)
