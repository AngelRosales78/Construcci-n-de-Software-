"""Fuentes explícitas para sucesores legales y balances auditados de negocios escindidos."""
import hashlib
import json
import re
from pathlib import Path
from html.parser import HTMLParser
from urllib.request import Request,urlopen
from django.core.management.base import BaseCommand,CommandError
from django.core.management import call_command
from .sync_company_data import annual_inventory,TAGS

class Tables(HTMLParser):
    def __init__(self):
        super().__init__();self.tables=[];self.current=None;self.row=[];self.cell=None;self.depth=0
    def handle_starttag(self,t,a):
        if t=='table':
            self.depth+=1
            if self.depth==1:self.current=[]
        if self.depth==1 and t=='tr':self.row=[]
        if self.depth==1 and t in ('td','th'):self.cell=''
    def handle_data(self,d):
        if self.cell is not None:self.cell+=d
    def handle_endtag(self,t):
        if self.depth==1 and t in ('td','th') and self.cell is not None:self.row.append(' '.join(self.cell.split()));self.cell=None
        if self.depth==1 and t=='tr' and self.current is not None:self.current.append(self.row)
        if t=='table':
            if self.depth==1:self.tables.append(self.current)
            self.depth-=1

def numbers(row):
    values=[]
    for raw in row[1:]:
        raw=raw.strip().replace(',','')
        if raw in ('—','–','-'):values.append(0)
        elif re.fullmatch(r'\(?-?\d+(\.\d+)?\)?',raw):values.append((-1 if raw.startswith('(') else 1)*int(raw.strip('()')))
    return values

def find_row(table,label):
    rows=[row for row in table if row and row[0].casefold()==label.casefold()]
    if len(rows)!=1:raise ValueError(f'Fila ausente o ambigua: {label}')
    return numbers(rows[0])

def extract_registration(ticker,tables):
    if ticker=='HONA':
        income=next(t for t in tables if any(r[:2]==['','Years Ended December 31,'] for r in t)
                    and any(r and r[0]=='Net income attributable to Aerospace' and len(numbers(r))==3 for r in t)
                    and not any('Pro Forma' in ' '.join(r) for r in t))
        balance=next(t for t in tables if any('2025' in r and '2024' in r and len([v for v in r if v])==2 for r in t)
                     and any(r and r[0]=='Total equity attributable to Aerospace' for r in t)
                     and not any('2026' in ' '.join(r) for r in t))
        mapping={'current_assets':'Total current assets','current_liabilities':'Total current liabilities','cash':'Cash and cash equivalents',
                 'receivables':'Accounts receivable, less allowances of $33 and $37, respectively','inventory':'Inventories',
                 'total_assets':'Total assets','total_liabilities':'Total liabilities','book_equity':'Total equity attributable to Aerospace'}
        income_map={'net_income':'Net income attributable to Aerospace','revenue':'Net sales','cost_of_sales':'Total cost of products and services sold','income_before_tax':'Income before taxes'}
        equity=next(t for t in tables if any(r and r[0]=='Balance as of December 31, 2023' for r in t))
        end2023=find_row(equity,'Balance as of December 31, 2023');book2023=end2023[-1]-end2023[-2]
    else:
        balance=next(t for t in tables if any('December 31, 2025' in r and 'December 31, 2024' in r and len([v for v in r if v])==3 for r in t)
                     and any(r and r[0]=='Total Seed Business equity' for r in t))
        income=next(t for t in tables if any(r and r[0]=='Net income (loss) attributable to the Seed Business' and len(numbers(r))==3 for r in t)
                    and any(r and r[0]=='Net sales' and len(numbers(r))==3 for r in t)
                    and any('2023' in r for r in t))
        mapping={'current_assets':'Total current assets','current_liabilities':'Total current liabilities','cash':'Cash and cash equivalents',
                 'short_term_investments':'Marketable securities','receivables':'Accounts and notes receivable - net','inventory':'Inventories',
                 'total_assets':'Total Assets','book_equity':'Total Seed Business equity'}
        income_map={'net_income':'Net income (loss) attributable to the Seed Business','revenue':'Net sales','cost_of_sales':'Cost of goods sold',
                    'income_before_tax':'Income (loss) before income taxes','interest_expense':'Interest expense'}
        equity=next(t for t in tables if any(r and r[0]=='Balance at December 31, 2023' and len(numbers(r))==5 for r in t)
                    and any('TotalEquity' in r for r in t))
        end2023=find_row(equity,'Balance at December 31, 2023');book2023=end2023[-1]-end2023[-2]
    records=[]
    for index,year in enumerate((2025,2024,2023)):
        values={name:{'value':None,'tag':'No publicado en las tablas anuales auditadas','available':False} for name in TAGS}
        for name,label in income_map.items():
            values[name]={'value':find_row(income,label)[index]*1_000_000,'tag':'Tabla auditada: '+label,'available':True}
        if year in (2025,2024):
            for name,label in mapping.items():
                values[name]={'value':find_row(balance,label)[index]*1_000_000,'tag':'Tabla auditada: '+label,'available':True}
            if ticker=='VYLR':
                liability=find_row(balance,'Total current liabilities')[index]+find_row(balance,'Total noncurrent liabilities')[index]
                values['total_liabilities']={'value':liability*1_000_000,'tag':'Tabla auditada: pasivos corrientes + no corrientes','available':True}
            assert find_row(balance,'Total Assets' if ticker=='VYLR' else 'Total assets')[index]==values['total_liabilities']['value']/1_000_000+find_row(balance,'Total equity')[index]
        else:values['book_equity']={'value':book2023*1_000_000,'tag':'Estado auditado de patrimonio: saldo al 31/12/2023 sin participación no controladora','available':True}
        records.append({'fiscal_year':year,'start':f'{year}-01-01','end':f'{year}-12-31','unit':'USD','values':values})
    return records

class Command(BaseCommand):
    def handle(self,*args,**options):
        root=Path(__file__).resolve().parents[4];cache=root/'backend/data-cache';target=root/'docs/datos/empresas'
        headers={'User-Agent':'ConstruccionSoftwareAcademic/2.0 financial-data-import'}
        aliases=[('XOM',34088,'https://investor.exxonmobil.com/sec-filings/all-sec-filings/content/0001193125-26-292453/d54886dposasr.htm',
                  'Estados históricos de Exxon Mobil Corporation, principal subsidiaria operativa de ExxonMobil Holdings. Corresponden a la entidad predecesora, con la fecha y fuente indicadas.'),
                 ('SKYD',813828,'https://ir.paramount.com/static-files/9c0a5060-140f-4b5e-ac22-e7f89e6de398',
                  'Estados históricos de Paramount Global, entidad predecesora antes de la combinación con Skydance. No representan el grupo sucesor actual ni son comparables directamente con sus períodos posteriores a agosto de 2025.')]
        for ticker,cik,reference,note in aliases:
            url=f'https://data.sec.gov/api/xbrl/companyfacts/CIK{cik:010d}.json';path=cache/f'{ticker}-predecessor.json'
            if not path.exists():
                with urlopen(Request(url,headers=headers),timeout=30) as response:path.write_bytes(response.read())
            raw=path.read_bytes();inventory=annual_inventory(json.loads(raw),ticker,cik,raw)
            inventory['reporting_note']=note;inventory['entity_relationship_source']=reference
            file=target/f'{ticker}-annual.json';file.write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
            call_command('import_annual_financials',str(file),stdout=self.stdout)
        for ticker,cik,accession,document,filed,note in [
            ('HONA',2089271,'0001628280-26-041399','exhibit991-10x12ba2.htm','2026-06-08',
             'Estados combinados auditados del negocio Honeywell Aerospace antes de su separación. No son cifras pro forma ni estados de Honeywell completo; la estructura de capital posterior a la separación puede ser distinta.'),
            ('VYLR',2128626,'0001193125-26-396054','ck0002128626-ex99_1.htm','2026-09-21',
             'Estados combinados auditados del Seed Business antes de la separación de Vylor. Se excluyen las tablas de Corteva completo y los ajustes pro forma. La estructura de capital posterior a la separación puede ser distinta.')]:
            path=cache/f'{ticker}-audited.html'
            if not path.exists():
                accn_path=accession.replace('-', '')
                url=f'https://www.sec.gov/Archives/edgar/data/{cik}/{accn_path}/{document}'
                with urlopen(Request(url,headers=headers),timeout=30) as response:path.write_bytes(response.read())
            raw=path.read_bytes();parser=Tables();parser.feed(raw.decode())
            records=extract_registration(ticker,parser.tables)
            for row in records:row.update(accession=accession,filed=filed)
            url=f'https://www.sec.gov/Archives/edgar/data/{cik}/{accession.replace("-", "")}/{document}'
            inventory={'ticker':ticker,'source':url,'snapshot_sha256':hashlib.sha256(raw).hexdigest(),'annual_records':records,
                       'reporting_note':note,'selection':'Tablas anuales auditadas en USD millones; no se utilizan columnas pro forma.'}
            file=target/f'{ticker}-annual.json';file.write_text(json.dumps(inventory,ensure_ascii=False,indent=2),encoding='utf-8')
            call_command('import_annual_financials',str(file),stdout=self.stdout)
