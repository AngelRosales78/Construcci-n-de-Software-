"""Actualizar cobertura desde la base real, incluyendo motivos de ausencia por indicador."""
import json
from pathlib import Path
from datetime import datetime,timezone
from django.core.management.base import BaseCommand
from api.models import Company

class Command(BaseCommand):
    def handle(self,*args,**options):
        root=Path(__file__).resolve().parents[4];path=root/'docs/datos/empresas/coverage.json'
        report=json.loads(path.read_text(encoding='utf-8')) if path.exists() else {'companies':{}}
        counters={key:0 for key in ('market_cap','per','roe','beta','dividend_yield')};ready=0
        for company in Company.objects.order_by('ticker'):
            old=report['companies'].get(company.ticker,{})
            metrics=getattr(company,'financial_metrics',None)
            prices=company.prices.filter(is_demo=False).count()
            years=list(company.annual_statements.filter(is_demo=False).values_list('fiscal_year',flat=True))
            available=[field for field in counters if metrics and getattr(metrics,field) is not None]
            for field in available:counters[field]+=1
            status='ready' if prices>=10 and years else 'partial'
            if status=='ready':ready+=1
            notices=old.get('source_notices',[]) + old.get('errors',[])
            report['companies'][company.ticker]={'status':status,'prices':prices,'annual_years':years,'available_metrics':available,
                'source_notices':list(dict.fromkeys(notices)), 'reporting_note':metrics.field_sources.get('reporting_entity',{}).get('reason') if metrics else None,
                'missing_reasons':{field:metrics.field_sources.get(field,{}).get('reason') for field in counters if field not in available} if metrics else {},'errors':[]}
        report.update(checked_at=datetime.now(timezone.utc).isoformat(),catalog_count=Company.objects.count(),ready_count=ready,field_coverage=counters)
        path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        self.stdout.write(f'Catálogo: {report["catalog_count"]}. Balances e históricos: {ready}. Métricas: {counters}.')
