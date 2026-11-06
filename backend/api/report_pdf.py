"""Real vector PDFs built exclusively from persisted financial facts and analysis snapshots."""
from datetime import timedelta
from decimal import Decimal
from io import BytesIO
import hashlib
import json
from xml.sax.saxutils import escape
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak
from reportlab.graphics.shapes import Drawing, PolyLine, String, Line
from pypdf import PdfReader
from .models import Company
from .comparison_views import pillars_for, comparison_data


def price_chart(companies, selected_range, width):
    drawing = Drawing(width, 135)
    series = []
    days = {'1M': 31, '6M': 183, '1Y': 365, '5Y': 1826}[selected_range]
    for company in companies:
        latest = company.prices.filter(is_demo=False).order_by('-date').first()
        rows = list(company.prices.filter(is_demo=False, date__gte=latest.date-timedelta(days=days)).order_by('date')) if latest else []
        series.append((company, rows))
    common = set.intersection(*(set(r.date for r in rows) for _, rows in series)) if series else set()
    if len(common) < 2:
        drawing.add(String(12, 70, 'Histórico real insuficiente para este rango; no se rellenan valores.', fontSize=9))
        return drawing
    dates = sorted(common)
    values = []
    for company, rows in series:
        points = {r.date: r.adjusted_close for r in rows}
        base = points[dates[0]]
        values.append([float((points[d]/base-1)*100) for d in dates])
    minimum, maximum = min(map(min, values)), max(map(max, values))
    span = maximum-minimum or 1
    drawing.add(Line(35, 25, width-15, 25, strokeColor=colors.grey))
    drawing.add(String(35, 10, dates[0].isoformat(), fontSize=8))
    drawing.add(String(width-80, 10, dates[-1].isoformat(), fontSize=8))
    drawing.add(String(35, 122, f'Retorno normalizado (%) - {selected_range} - fechas comunes', fontSize=9))
    drawing.add(String(0, 103, f'{maximum:.1f}%', fontSize=7))
    drawing.add(String(0, 29, f'{minimum:.1f}%', fontSize=7))
    for i, ((company, _), row) in enumerate(zip(series, values)):
        color = colors.HexColor('#00865a' if i == 0 else '#1769aa')
        coords = []
        for j, value in enumerate(row):
            coords.extend([35+(width-50)*j/(len(row)-1), 30+(value-minimum)/span*78])
        drawing.add(PolyLine(coords, strokeColor=color, strokeWidth=1.2))
        drawing.add(String(width-160, 112-i*12, company.ticker, fontSize=8, fillColor=color))
    return drawing


def generate_pdf(request, analysis):
    companies = [Company.objects.get(ticker=request['a'])]
    if request['b']:
        companies.append(Company.objects.get(ticker=request['b']))
    year = request['fiscal_year']
    comparison = request['kind'] == 'comparison'
    data = comparison_data(request['a'], request['b'], year) if comparison else None
    facts = [data['a'], data['b']] if comparison else [pillars_for(companies[0], year)]
    pagesize = landscape(A4) if comparison else A4
    width = pagesize[0]-72
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name='ReportBody', fontName='Helvetica', fontSize=9, leading=12, spaceAfter=6))
    styles.add(ParagraphStyle(name='ReportSmall', fontName='Helvetica', fontSize=8, leading=10, spaceAfter=5, wordWrap='CJK'))
    styles.add(ParagraphStyle(name='ReportTitle', fontName='Helvetica-Bold', fontSize=19, leading=23, textColor=colors.HexColor('#007f56'), spaceAfter=10))
    def para(text, style='ReportBody'):
        return Paragraph(escape(str(text)), styles[style])
    story = [para('Análisis financiero - ' + ' / '.join(c.ticker for c in companies), 'ReportTitle'),
             para(f'Ejercicio fiscal {year} | Moneda: {" / ".join(f["currency"] or "no informada" for f in facts)} | Datos anuales del servidor')]
    for company, fact in zip(companies, facts):
        period = fact['period']
        story.append(para(f'{company.name}: {period["start"] or "sin fecha"} a {period["end"] or "sin fecha"}', 'ReportSmall'))
    table = [[para('Pilar')] + [para(c.ticker) for c in companies] + ([para('Comparabilidad')] if comparison else [])]
    for index, pillar in enumerate(facts[0]['pillars']):
        row = [para(pillar['label'])] + [para(fact['pillars'][index]['score']['display_value'] + (' / 100' if fact['pillars'][index]['score']['status'] == 'available' else '')) for fact in facts]
        if comparison:
            decision = data['comparisons'][index]
            result = 'Mayor nota: ' + request[decision['result']] if decision['result'] in ('a', 'b') else 'Empate' if decision['result'] == 'tie' else 'No comparable: distinto periodo o datos insuficientes'
            row.append(para(result, 'ReportSmall'))
        table.append(row)
    widths = [width*.22, width*.18, width*.18, width*.42] if comparison else [width*.55, width*.45]
    grid = Table(table, colWidths=widths, hAlign='LEFT')
    grid.setStyle(TableStyle([('BACKGROUND', (0,0), (-1,0), colors.HexColor('#e3f6ed')),
                             ('GRID', (0,0), (-1,-1), .4, colors.HexColor('#b8cbc2')),
                             ('VALIGN', (0,0), (-1,-1), 'TOP'), ('TOPPADDING', (0,0), (-1,-1), 7),
                             ('BOTTOMPADDING', (0,0), (-1,-1), 7)]))
    story.extend([Spacer(1, 10), grid, Spacer(1, 10)])
    story.append(para('Metodología: ' + facts[0]['methodology_version'] + '; normalización: ' + facts[0]['scoring_policy']['version'], 'ReportSmall'))
    if request['include_charts']:
        story.append(price_chart(companies, request['range'], width))
    story.append(para('Escala académica del proyecto. No constituye asesoramiento financiero ni garantiza resultados.', 'ReportSmall'))
    story.append(PageBreak())
    story.append(para('Análisis y procedencia' if request['include_analysis'] else 'Procedencia y componentes', 'ReportTitle'))
    if request['include_analysis']:
        story.append(para(f'Origen: {request["analysis_origin"]}. Nivel: {request["analysis_level"]}.', 'ReportSmall'))
        paragraphs = analysis.get('paragraphs') if comparison else analysis.get('summary', []) + [p['text'] for p in analysis.get('pillars', [])]
        for text in paragraphs or []:
            story.append(para(text, 'ReportSmall'))
        story.append(para('Versión del análisis: ' + request['analysis_version'], 'ReportSmall'))
    else:
        for company, fact in zip(companies, facts):
            for pillar in fact['pillars']:
                values = '; '.join(f'{m["label"]}: {m["display_value"]}' for m in pillar['metrics'])
                story.append(para(f'{company.ticker} - {pillar["label"]}: {values}', 'ReportSmall'))
    for company in companies:
        statement = company.annual_statements.filter(fiscal_year=year, is_demo=False).first()
        if statement:
            story.append(para(f'{company.ticker} | Informe {year} | {statement.source_url}', 'ReportSmall'))
            story.append(para('Snapshot SHA-256: ' + statement.snapshot_sha256, 'ReportSmall'))
        if request['include_charts']:
            urls = company.prices.filter(is_demo=False).order_by().values_list('source', flat=True).distinct()[:5]
            for url in urls:
                story.append(para(f'{company.ticker} | Histórico: {url}', 'ReportSmall'))
    output = BytesIO()
    doc = SimpleDocTemplate(output, pagesize=pagesize, rightMargin=36, leftMargin=36, topMargin=30, bottomMargin=30,
                            title='Análisis financiero', author='Plataforma S&P 500')
    def footer(canvas, doc):
        canvas.setFont('Helvetica', 8)
        canvas.setFillColor(colors.grey)
        canvas.drawString(36, 16, f'Plataforma S&P 500 | {doc.page} | Datos del servidor, sin cifras inventadas')
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    content = output.getvalue()
    count = len(PdfReader(BytesIO(content)).pages)
    if comparison and count > 2:
        raise ValueError('El contenido comparativo supera las dos páginas permitidas.')
    if len(content) > 2_000_000:
        raise ValueError('El reporte supera el objetivo de dos MB.')
    return content, count, hashlib.sha256(json.dumps(facts, sort_keys=True).encode()).hexdigest()
