import re
from django.shortcuts import get_object_or_404
from rest_framework import status
from rest_framework.exceptions import NotFound, ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.views import APIView
from rest_framework.response import Response
from .models import Company, Favorite


def favorite_data(favorite):
    return {'id': favorite.pk, 'ticker': favorite.company.ticker, 'name': favorite.company.name}


class FavoritesView(APIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request):
        rows = Favorite.objects.filter(user=request.user).select_related('company')
        response = Response({'schema_version': 'favorites-v1', 'owner_id': str(request.user.pk),
                             'results': [favorite_data(row) for row in rows]})
        response['Cache-Control'] = 'private, no-store'
        return response

    def post(self, request):
        ticker = request.data.get('ticker')
        if not isinstance(ticker, str) or not re.fullmatch(r'[A-Za-z0-9.\-]{1,10}', ticker.strip()):
            raise ValidationError({'ticker': 'Indique un símbolo válido del catálogo.'})
        company = get_object_or_404(Company, ticker=ticker.strip().upper())
        # get_or_create + restricción única resuelven reintentos y solicitudes simultáneas.
        favorite, created = Favorite.objects.get_or_create(user=request.user, company=company)
        return Response({'schema_version': 'favorites-v1', 'owner_id': str(request.user.pk),
                         'favorite': favorite_data(favorite)}, status=201 if created else 200)


class FavoriteDetailView(APIView):
    permission_classes = (IsAuthenticated,)

    def delete(self, request, favorite_id):
        _, deleted = Favorite.objects.filter(pk=favorite_id, user=request.user).delete()
        if not deleted.get(Favorite._meta.label):
            # Ausentes y ajenos tienen la misma respuesta.
            raise NotFound()
        return Response(status=status.HTTP_204_NO_CONTENT)
