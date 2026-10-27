"""Preparación y cálculo trazable de las métricas de la ficha, sin llamadas en peticiones web."""
import math
import statistics
from datetime import date, datetime, timezone, timedelta
from decimal import Decimal, ROUND_HALF_UP
from .pillars import build_pillars


def chart_prices(payload):
    result=payload['chart']['result'][0]
    if result['meta']['currency']!='USD': raise ValueError('La cotización no está expresada en USD.')
    adjusted=result['indicators']['adjclose'][0]['adjclose']
    quote=result['indicators']['quote'][0]
    rows=[]
    for i,stamp in enumerate(result.get('timestamp',[])):
        when=datetime.fromtimestamp(stamp,timezone.utc).date()
        value=adjusted[i]
        if when>date.today() or value is None or not math.isfinite(value) or value<=0:continue
        rows.append({'date':when.isoformat(),'adjusted_close':f'{value:.6f}',
                     'close':quote.get('close',[None]*len(adjusted))[i],
                     'volume':quote.get('volume',[None]*len(adjusted))[i], 'currency':'USD'})
    return result,rows


def latest_valuation(payload, typ):
    candidates=[]
    for item in payload.get('timeseries',{}).get('result') or []:
        for row in item.get(typ,[]) or []:
            value=(row.get('reportedValue') or {}).get('raw')
            when=row.get('asOfDate')
            if value is not None and math.isfinite(value) and when and date.fromisoformat(when)<=date.today():
                candidates.append((when,Decimal(str(value))))
    return max(candidates,key=lambda r:r[0]) if candidates else None


def beta_value(rows, benchmark):
    stock={r['date']:float(r['adjusted_close']) for r in rows}
    index={r['date']:float(r['adjusted_close']) for r in benchmark}
    dates=sorted(set(stock)&set(index))[-253:]
    if len(dates)<181:return None
    a=[stock[b]/stock[a]-1 for a,b in zip(dates,dates[1:])]
    b=[index[y]/index[x]-1 for x,y in zip(dates,dates[1:])]
    variance=statistics.variance(b)
    return Decimal(str(statistics.covariance(a,b)/variance)) if variance>0 else None


def update_snapshot(company, facts, chart, rows, valuations, benchmark):
    from .models import FinancialMetrics,Company
    values={field:None for field in ['market_cap','per','roe','beta','dividend_yield','working_capital','total_assets',
                                     'retained_earnings','ebit','total_liabilities','sales','equity_value']}
    provenance={}
    quote_date=date.fromisoformat(rows[-1]['date']) if rows else None
    valuation_url=f'https://finance.yahoo.com/quote/{company.ticker}/key-statistics/'
    for field,types in [('market_cap',['trailingMarketCap','annualMarketCap']),('per',['trailingPeRatio','annualPeRatio'])]:
        found=next((v for t in types if (v:=latest_valuation(valuations,t)) is not None),None)
        if found and (field!='market_cap' or found[1]>=0):
            values[field]=found[1]
            provenance[field]={'as_of':found[0],'source':valuation_url,'method':'reported valuation series'}
    statements={s.fiscal_year:s for s in company.annual_statements.filter(is_demo=False)}
    current=statements[max(statements)] if statements else None
    if current:
        if current.reporting_note: provenance['reporting_entity']={'reason':current.reporting_note,'as_of':current.period_end.isoformat(),'source':current.source_url}
        pillars=build_pillars(company,current,statements.get(current.fiscal_year-1),current.fiscal_year,sorted(statements,reverse=True),statements)
        roe=next(m for p in pillars['pillars'] for m in p['metrics'] if m['id']=='roe')
        if roe['status']=='available':
            values['roe']=Decimal(roe['value'])
            provenance['roe']={'as_of':current.period_end.isoformat(),'source':current.source_url,'method':roe['formula_display'],'fiscal_year':current.fiscal_year}
        def component(key):
            raw=current.components.get(key,{}).get('value')
            return Decimal(raw) if raw is not None else None
        if roe['status']!='available':
            provenance['roe']={'status':'not_applicable' if roe.get('reason')=='nonpositive_equity' else 'unavailable', 'reason':roe.get('explanation') or 'Faltan componentes anuales comparables.', 'source':current.source_url,'as_of':current.period_end.isoformat()}
        assets,liabilities=component('current_assets'),component('current_liabilities')
        values['working_capital']=assets-liabilities if assets is not None and liabilities is not None else None
        values['total_assets']=component('total_assets');values['total_liabilities']=component('total_liabilities');values['sales']=component('revenue')
        before,interest=component('income_before_tax'),component('interest_expense')
        values['ebit']=before+interest if before is not None and interest is not None else None
        for row in facts.get('facts',{}).get('us-gaap',{}).get('RetainedEarningsAccumulatedDeficit',{}).get('units',{}).get('USD',[]):
            if row.get('accn')==current.accession and row.get('end')==current.period_end.isoformat():
                values['retained_earnings']=Decimal(str(row['val']))
                break
        # Market equity is used by Altman only if it belongs to the same annual balance date.
        if provenance.get('market_cap',{}).get('as_of')==current.period_end.isoformat():values['equity_value']=values['market_cap']
    if rows:
        value=beta_value(rows,benchmark)
        if value is not None:
            values['beta']=value
            provenance['beta']={'as_of':rows[-1]['date'],'source':'https://finance.yahoo.com/quote/%5EGSPC/history/',
                                'method':'covariance/variance of up to 252 matched daily adjusted returns versus S&P 500'}
        if value is None: provenance['beta']={'status':'unavailable','reason':'Se necesitan al menos 181 cierres coincidentes con el S&P 500 para esta estimación de beta.'}
        close=rows[-1].get('close')
        events=chart.get('events',{})
        since=quote_date-timedelta(days=365)
        splits=[event for event in events.get('splits',{}).values() if since<datetime.fromtimestamp(event['date'],timezone.utc).date()<=quote_date]
        if close and close>0 and not splits:
            dividends=sum(float(event['amount']) for event in events.get('dividends',{}).values()
                          if since<datetime.fromtimestamp(event['date'],timezone.utc).date()<=quote_date)
            values['dividend_yield']=Decimal(str(100*dividends/close))
            provenance['dividend_yield']={'as_of':quote_date.isoformat(),'source':f'https://finance.yahoo.com/quote/{company.ticker}/history/',
                                         'method':'recorded cash dividends over trailing 365 days / unadjusted closing price; percent'}
    if not (current or rows or provenance):return None
    for field,value in values.items():
        if value is not None:values[field]=value.quantize(Decimal('.0001') if field in ('beta','dividend_yield') else Decimal('.01'),rounding=ROUND_HALF_UP)
    for field in ('market_cap','per','roe','beta','dividend_yield'):
        if values[field] is None:provenance.setdefault(field, {'status':'unavailable','reason':'La fuente no ofrece componentes comparables suficientes para este indicador.'})
    source=current.source_url if current else valuation_url
    defaults=values|{'as_of':current.period_end if current else quote_date or date.today(),'source':source,'is_demo':False,'field_sources':provenance}
    instance=FinancialMetrics(company=company,**defaults)
    instance.full_clean(exclude=['company'],validate_unique=False,validate_constraints=False)
    FinancialMetrics.objects.update_or_create(company=company,defaults=defaults)
    Company.objects.filter(pk=company.pk).update(market_cap=values['market_cap'],per=values['per'],roe=values['roe'])
    return provenance
