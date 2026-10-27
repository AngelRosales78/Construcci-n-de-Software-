"""Informe determinista de métricas verificadas. No simula un proveedor de IA."""
import re
from django.shortcuts import get_object_or_404
from django.utils import timezone
from rest_framework import generics, serializers
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.exceptions import ValidationError
from .models import Company
from .pillars import build_pillars
from .local_ai import optional_local_ai

class AnalysisInput(serializers.Serializer):
    fiscal_year = serializers.IntegerField(min_value=1900)
    level = serializers.ChoiceField(choices=['simple', 'technical'])
    def validate_fiscal_year(self, value):
        if value > timezone.localdate().year:
            raise serializers.ValidationError('No se admiten ejercicios futuros.')
        return value

def company_report(company, year, level):
    statements = {row.fiscal_year: row for row in company.annual_statements.filter(is_demo=False)}
    current = statements.get(year)
    if not current:
        raise ValidationError({'detail': 'Esta empresa no tiene un balance anual real cargado para ese ejercicio.',
                               'available_years': sorted(statements, reverse=True)})
    data = build_pillars(company, current, statements.get(year-1), year, sorted(statements, reverse=True), statements)
    sections, strengths, risks = [], [], []
    for pillar in data['pillars']:
        metrics = pillar['metrics']
        lines = []
        for metric in metrics:
            if metric['status'] == 'available':
                unit = ' %' if metric['unit'] == 'percent' else ' días' if metric['unit'] == 'days' else ''
                line = f"{metric['label']}: {metric['display_value']}{unit}."
                if level == 'technical':
                    line += f" Fórmula: {metric['formula_display']}."
            else:
                line = f"{metric['label']}: {metric.get('explanation') or 'No disponible.'}"
            lines.append(line)
        refs = [m['id'] for m in metrics]
        sections.append({'pillar_id': pillar['id'], 'text': ' '.join(lines), 'metric_ids': refs,
                         'availability': {m['id']: m['status'] for m in metrics},
                         'academic_signal': 'sin nota' if pillar['score'].get('value') is None else 'alta' if float(pillar['score']['value']) >= 75 else 'baja' if float(pillar['score']['value']) < 50 else 'intermedia'})
        score = pillar['score'].get('value')
        if score is not None and refs:
            item = {'text': f"{pillar['label']}: nota académica {pillar['score']['display_value']} / 100 según la política del proyecto.", 'metric_ids': refs}
            if float(score) >= 75: strengths.append(item)
            elif float(score) < 50: risks.append(item)
        if any(m['status'] == 'unavailable' for m in metrics):
            risks.append({'text': f"{pillar['label']}: hay indicadores sin datos suficientes; la evaluación es parcial.", 'metric_ids': refs})
    now = timezone.now().isoformat()
    return {'schema_version': 'analysis-v1', 'generation_mode': 'rules', 'provider': None,
            'generation_notice': 'Informe automático basado en reglas y métricas del servidor. No fue generado por IA; todavía no hay un proveedor configurado.',
            'ticker': company.ticker, 'fiscal_year': year, 'level': level,
            'snapshot_sha256': current.snapshot_sha256, 'metrics_version': data['methodology_version'], 'data_as_of': current.period_end.isoformat(),
            'generated_at': now, 'period': data['period'], 'validation': {'status': 'passed', 'checked_at': now},
            'summary': [f"{company.name} ({company.ticker}), ejercicio fiscal {year}, en {current.currency}. El análisis usa los componentes del informe anual indicado en las fuentes.",
                        'Las notas son una escala académica del proyecto. No representan una clasificación sectorial ni una recomendación de inversión.'] + ([current.reporting_note] if current.reporting_note else []),
            'pillars': sections, 'strengths': strengths[:5], 'risks': risks[:5],
            'sources': [{'label': f'Informe anual {year} · SEC', 'url': current.source_url}]}

class CompanyAnalysisView(generics.GenericAPIView):
    permission_classes = (IsAuthenticated,)
    serializer_class = AnalysisInput
    def post(self, request, ticker):
        company = get_object_or_404(Company, ticker=ticker.upper())
        serializer = self.get_serializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        return Response(optional_local_ai(company_report(company, serializer.validated_data['fiscal_year'], serializer.validated_data['level'])))
