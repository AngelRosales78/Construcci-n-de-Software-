"""Reconstruye fichas y exporta sus valores con procedencia usando solo snapshots locales."""
import json
from pathlib import Path
from django.core.management.base import BaseCommand
from api.models import Company
from api.company_snapshots import chart_prices,update_snapshot

class Command(BaseCommand):
    def handle(self,*args,**options):
        root=Path(__file__).resolve().parents[4];cache=root/'backend/data-cache';target=root/'docs/datos/empresas'
        def load(path):return json.loads(path.read_bytes()) if path.exists() else {}
        _,benchmark=chart_prices(load(cache/'benchmark-sp500.json'))
        count=0
        for company in Company.objects.order_by('ticker'):
            facts=load(cache/f'{company.ticker}-companyfacts.json')
            payload=load(cache/f'{company.ticker}-prices-actions.json')
            chart,rows=chart_prices(payload) if payload else ({},[])
            valuations=load(cache/f'{company.ticker}-valuations.json')
            result=update_snapshot(company,facts,chart,rows,valuations,benchmark)
            if result is None:continue
            metrics=company.financial_metrics
            values={field.name:getattr(metrics,field.name) for field in metrics._meta.fields if field.name not in ('id','company')}
            payload={'ticker':company.ticker,'values':values}
            (target/f'{company.ticker}-metrics.json').write_text(json.dumps(payload,ensure_ascii=False,indent=2,default=str),encoding='utf-8')
            count+=1
        self.stdout.write(f'{count} fichas reconstruidas y exportadas con fechas/fuentes.')
