"""Audit catalog coverage against immutable captures; do not generate missing figures."""
import csv
import hashlib
import json
from collections import Counter
from datetime import date
from pathlib import Path
from django.core.management.base import BaseCommand, CommandError
from django.db import transaction
from django.utils import timezone
from api.models import FinancialMetrics
from api.provider_financials import parse_capture, chart_capture
from api.financial_availability import diagnose, altman_status, INDICATORS
from api.views import calculate_altman_z
from api.management.commands.import_provider_financials import read_gzip

class Command(BaseCommand):
    help = 'Diagnosticar ausencias por empresa y guardar causas con identidad del resumen.'
    def add_arguments(self, parser):
        parser.add_argument('snapshot')
        parser.add_argument('--prices',required=True)
        parser.add_argument('--csv')
        parser.add_argument('--report')
        parser.add_argument('--dry-run',action='store_true')
    def handle(self,*args,**options):
        try:
            bundle=read_gzip(Path(options['snapshot'])); price_path=Path(options['prices'])
            if hashlib.sha256(price_path.read_bytes()).hexdigest()!=bundle['price_snapshot_sha256']:
                raise ValueError('No corresponde la copia de precios.')
            prices={e['ticker']:e for e in read_gzip(price_path)['companies']}
            index=bundle['benchmark']; raw=index['raw_response'].encode()
            if hashlib.sha256(raw).hexdigest()!=index['evidence']['sha256']:raise ValueError('Índice alterado.')
            benchmark,_=chart_capture('^GSPC',raw,timezone.localdate(),benchmark=True)
            entries={e['ticker']:e for e in bundle['companies']}
            if len(entries)!=len(bundle['companies']):raise ValueError('Empresas repetidas.')
            prepared=[]; rows=[]; counts={key:Counter() for key in (*INDICATORS,'altman')}
            for summary in FinancialMetrics.objects.select_related('company').order_by('company__ticker'):
                ticker=summary.company.ticker; entry=entries.get(ticker)
                if not entry:raise ValueError('Falta captura de '+ticker)
                records,_=parse_capture(ticker,entry['raw_response'].encode(),entry['evidence'],timezone.localdate())
                evidence=prices[ticker]; raw=evidence['raw_response'].encode()
                if hashlib.sha256(raw).hexdigest()!=evidence['sha256']:raise ValueError('Precios alterados.')
                chart,_=chart_capture(ticker,raw,timezone.localdate(),price_as_of=date.fromisoformat(evidence['price_as_of']))
                raw_chart=json.loads(raw)['chart']['result'][0]
                data=diagnose(summary,records,chart,raw_chart,benchmark)
                data['source_sha256']=entry['evidence']['sha256']
                state=altman_status(summary,calculate_altman_z(summary))
                for key,item in data['indicators'].items():counts[key][item['code']]+=1
                counts['altman'][state['code']]+=1
                rows.append([ticker,summary.company.name,*[data['indicators'][k]['label'] for k in INDICATORS],
                             state['label'],state.get('explanation',''),state.get('next_step','')])
                prepared.append((summary.pk,data))
        except (OSError,ValueError,KeyError,TypeError,ArithmeticError,IndexError) as error:
            raise CommandError('Auditoría inválida: '+str(error)) from error
        changed=0
        if not options['dry_run']:
            with transaction.atomic():
                for pk,data in prepared:
                    summary=FinancialMetrics.objects.select_for_update().get(pk=pk)
                    from api.financial_availability import fingerprint
                    if fingerprint(summary)!=data['fingerprint']:raise CommandError('El resumen cambió durante la auditoría.')
                    if summary.data_diagnostics!=data:
                        summary.data_diagnostics=data;summary.save(update_fields=['data_diagnostics']);changed+=1
        if options['csv']:
            with Path(options['csv']).open('w',newline='',encoding='utf-8-sig') as stream:
                writer=csv.writer(stream);writer.writerow(['Ticker','Empresa','Capitalización','PER','ROE','Beta','Dividendo','Altman-Z','Motivo Altman-Z','Siguiente paso Altman-Z']);writer.writerows(rows)
        report=dict(companies=len(prepared),updated=changed,dry_run=options['dry_run'],counts=counts)
        if options['report']:Path(options['report']).write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
        self.stdout.write(json.dumps(report,ensure_ascii=False))
