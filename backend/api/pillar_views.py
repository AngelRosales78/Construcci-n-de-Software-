import re
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import generics
from rest_framework.exceptions import ValidationError
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from .models import Company
from .pillars import build_pillars


class CompanyPillarsView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)

    def get(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        year = request.query_params.get('fiscal_year')
        if year is not None:
            if not re.fullmatch(r'[0-9]{4}', year) or not 1900 <= int(year) <= timezone.localdate().year:
                raise ValidationError({'fiscal_year': 'Indique un año de cierre válido, sin fechas futuras.'})
            year = int(year)
        statements = {row.fiscal_year: row for row in company.annual_statements.all()}
        available = sorted(statements, reverse=True)
        if year is None:
            year = available[0] if available else None
        current = statements.get(year)
        previous = statements.get(year - 1) if year is not None else None
        return Response(build_pillars(company, current, previous, year, available, statements=statements))
