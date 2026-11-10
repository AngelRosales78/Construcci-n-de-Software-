"""PDF vectorial desde un snapshot inmutable; nunca consulta ni genera IA al dibujar."""
from io import BytesIO
from html import escape
import math
from datetime import date
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, Flowable, PageBreak
from pypdf import PdfReader

GREEN = colors.HexColor('#158569')
BLUE = colors.HexColor('#2967b3')
LABELS = {'liquidity': 'Liquidez', 'profitability': 'Rentabilidad', 'solvency': 'Solvencia', 'efficiency': 'Eficiencia', 'growth': 'Crecimiento'}


class VectorChart(Flowable):
    def __init__(self, series, width=470, height=175, unit='%', radar=False):
        Flowable.__init__(self)
        self.series, self.width, self.height, self.unit, self.radar = series, width, height, unit, radar

    def draw(self):
        c = self.canv
        c.setFont('Helvetica', 8)
        if not self.series or not any(points for _, points in self.series):
            c.drawString(12, self.height / 2, 'Sin datos suficientes para este gráfico.')
            return
        palette = [GREEN, BLUE]
        if self.radar:
            cx, cy, radius = self.width / 2, self.height / 2 + 14, min(self.height / 2 - 42, 80)
            xy = lambda i, value: (cx + radius * value / 100 * math.sin(i * 2 * math.pi / 5), cy + radius * value / 100 * math.cos(i * 2 * math.pi / 5))
            c.setStrokeColor(colors.lightgrey)
            for tick in (25, 50, 75, 100):
                path = c.beginPath()
                for i in range(6):
                    point = xy(i % 5, tick)
                    path.moveTo(*point) if i == 0 else path.lineTo(*point)
                c.drawPath(path)
            for i, label in enumerate(LABELS.values()):
                x, y = xy(i, 122)
                c.setFillColor(colors.black)
                c.drawCentredString(x, y, label)
            for index, (label, values) in enumerate(self.series):
                if len(values) != 5 or any(v is None for v in values):
                    c.drawString(8, 8 + index * 12, label + ': radar no disponible; faltan notas.')
                    continue
                c.setStrokeColor(palette[index % 2]); c.setDash([] if index == 0 else [5, 3])
                path = c.beginPath()
                for i in range(6):
                    point = xy(i % 5, float(values[i % 5]))
                    path.moveTo(*point) if i == 0 else path.lineTo(*point)
                c.drawPath(path)
            c.setDash([])
            return
        values = [float(v) for _, points in self.series for _, v in points]
        if not values:
            return
        low, high = min(values), max(values)
        span = high - low or max(1, abs(high) * .05)
        dates = sorted({d for _, points in self.series for d, _ in points})
        positions = {d: date.fromisoformat(d).toordinal() - date.fromisoformat(dates[0]).toordinal() for d in dates}
        left, bottom = 52, 28
        width, height = self.width - 70, self.height - 60
        c.setStrokeColor(colors.lightgrey); c.line(left, bottom, left + width, bottom)
        for tick in range(5):
            value = low + span * tick / 4
            y = bottom + height * tick / 4
            c.line(left, y, left + width, y); c.setFillColor(colors.black)
            c.drawRightString(left - 5, y - 3, f'{value:.2f} {self.unit}')
        for index, (label, points) in enumerate(self.series):
            c.setStrokeColor(palette[index % 2]); c.setFillColor(palette[index % 2]); c.setDash([] if index == 0 else [5, 3])
            c.setLineWidth(1.2); path = c.beginPath()
            for i, (d, value) in enumerate(points):
                x = left + positions[d] / max(1, positions[dates[-1]]) * width
                y = bottom + (float(value) - low) / span * height
                path.moveTo(x, y) if i == 0 else path.lineTo(x, y)
            c.drawPath(path); c.drawString(left + index * 220, self.height - 12, label)
        c.setDash([]); c.setFillColor(colors.black)
        c.drawString(left, 12, dates[0]); c.drawRightString(left + width, 12, dates[-1])


def render_report(snapshot, request):
    output = BytesIO()
    comparison = request['kind'] == 'comparison'
    page_size = landscape(A4) if comparison else A4
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(name='Small', fontSize=8, leading=11, spaceAfter=5))
    styles['BodyText'].fontSize = 9; styles['BodyText'].leading = 12
    def p(text, style='BodyText'):
        return Paragraph(escape(str(text if text is not None else 'No disponible')), styles[style])
    def table(rows, widths):
        t = Table([[p(cell, 'Small') for cell in row] for row in rows], colWidths=widths, repeatRows=1, hAlign='LEFT')
        t.setStyle(TableStyle([('BACKGROUND', (0, 0), (-1, 0), colors.HexColor('#e9f4ee')),
                               ('VALIGN', (0, 0), (-1, -1), 'TOP'), ('LINEBELOW', (0, 0), (-1, 0), .6, GREEN),
                               ('BOTTOMPADDING', (0, 0), (-1, -1), 5)]))
        return t
    companies = snapshot['companies']
    width = page_size[0] - 72
    story = [p('S&P 500 Finance - Informe histórico', 'Title'), Spacer(1, 8)]
    story += [p(' / '.join(f"{row['ticker']} - {row['name']}" for row in companies), 'Heading2'),
              p(f"Ejercicio solicitado: {request['fiscal_year']} | Generado: {snapshot['generated_at']}"),
              p('Datos históricos importados. Notas académicas; no constituyen una recomendación de inversión.', 'Small')]
    for row in companies:
        period = row['pillars']['period']
        story.append(p(f"{row['ticker']}: {period.get('start') or 'Sin inicio'} a {period.get('end') or 'Sin cierre'}; {row['pillars'].get('currency', 'USD')}"))
    if comparison:
        pair = snapshot['comparison']
        rows = [['Pilar', companies[0]['ticker'], companies[1]['ticker'], 'Resultado de la comparación']]
        for item in pair['comparisons']:
            scores = [next(pillar for pillar in c['pillars']['pillars'] if pillar['id'] == item['id'])['score'] for c in companies]
            label = item['reason'] if item['status'] != 'available' else ('Empate' if item['result'] == 'tie' else f"Mayor nota: {companies[0 if item['result'] == 'a' else 1]['ticker']}; A - B: {item['delta']} puntos")
            rows.append([LABELS[item['id']], *[s.get('display_value') or 'No disponible' for s in scores], label])
        story += [Spacer(1, 10), table(rows, [120, 85, 85, width - 290])]
    else:
        row = companies[0]
        summary = row['summary']
        story += [p('Ficha técnica', 'Heading2'), table([['Indicador', 'Valor de referencia'],
            *[[label, summary.get(key)] for key, label in [('per', 'PER'), ('roe', 'ROE (%)'), ('market_cap', 'Capitalización'),
               ('beta', 'Beta'), ('dividend_yield', 'Dividendo (%)')]],
            ['Altman-Z', (summary.get('altman_z') or {}).get('score')],
            ['Perfil Altman-Z', (summary.get('profile') or {}).get('label')],
            ['Sector', summary.get('sector_display')], ['Balance del resumen', summary.get('as_of')]], [180, width - 180])]
        quote = summary.get('quote')
        if quote:
            story.append(p(f"Último cierre ajustado importado: {quote['adjusted_close']} {quote['currency']} - {quote['date']}", 'Small'))
        story.append(p('Fuente del resumen: ' + str(summary.get('source') or 'Sin fuente disponible'), 'Small'))
        for pillar in row['pillars']['pillars']:
            story.append(p(pillar['label'] + ' - Nota: ' + (pillar['score'].get('display_value') or 'No disponible'), 'Heading2'))
            for metric in pillar['metrics']:
                unit = {'percent': '%', 'times': 'veces'}.get(metric.get('unit'), metric.get('unit', ''))
                story.append(p(f"{metric['label']}: {metric.get('display_value') or 'No disponible'} {unit}"))
                story.append(p(metric.get('formula_display') or metric.get('formula') or metric.get('reason') or '', 'Small'))
                for component in metric.get('components', []):
                    story.append(p(f"{component['label']}: {component['value']} {component.get('unit', '')}; {component['period']['start']} a {component['period']['end']}", 'Small'))
    if request['include_charts']:
        if comparison:
            story.append(PageBreak())
        story.append(p('Gráficos vectoriales - ' + request['range'], 'Heading2'))
        if comparison:
            hist = snapshot['history']
            series = [(c['ticker'], [(v['date'], v[side]) for v in hist['points']]) for c, side in zip(companies, ('a', 'b'))]
            scores = [(row['ticker'], [p['score'].get('value') if p['score']['status'] == 'available' else None for p in row['pillars']['pillars']]) for row in companies]
            story.append(Table([[VectorChart(series, width=width * .60, height=165),
                                 VectorChart(scores, width=width * .38, height=165, radar=True)]],
                               colWidths=[width * .61, width * .39]))
            if hist['status'] != 'available': story.append(p(hist['reason'], 'Small'))
        else:
            history = companies[0]['history']
            story.append(VectorChart([(companies[0]['ticker'], [(v['date'], v['price']) for v in history])], width=width, unit='USD'))
        if not comparison:
            scores = [(row['ticker'], [p['score'].get('value') if p['score']['status'] == 'available' else None for p in row['pillars']['pillars']]) for row in companies]
            story.append(VectorChart(scores, width=width, height=200, radar=True))
        story.append(p('Radar de 0 a 100. Una línea continua y otra discontinua distinguen las empresas; no se sustituyen notas ausentes por cero.', 'Small'))
    if request['include_analysis']:
        analysis = snapshot['analysis']
        story.append(p('Análisis - ' + {'ai': 'IA local', 'mixed': 'Origen mixto', 'rules': 'Reglas'}[analysis['origin']], 'Heading2'))
        story.append(p('Versión: ' + analysis['version'], 'Small'))
        for paragraph in analysis['paragraphs']:
            story.append(p(paragraph))
    story.append(p('Fuentes y metodología', 'Heading2'))
    story.append(p('annual-ratios-v2 / academic-pillar-scores-v1. Cinco pilares con pesos de 20 %. Altman-Z es un indicador diferente.', 'Small'))
    for url in snapshot['sources']:
        story.append(Paragraph(f'<link href="{escape(url, quote=True)}" color="#158569">{escape(url)}</link>', styles['Small']))
    def footer(canvas, document):
        canvas.saveState(); canvas.setFont('Helvetica', 8)
        canvas.drawString(36, 19, 'S&P 500 Finance | Datos históricos y cobertura declarada')
        canvas.drawRightString(page_size[0] - 36, 19, f'Página {document.page}')
        canvas.restoreState()
    doc = SimpleDocTemplate(output, pagesize=page_size, rightMargin=36, leftMargin=36, topMargin=30, bottomMargin=35,
                            title='Informe financiero ' + request['a'], author='S&P 500 Finance', pageCompression=1)
    doc.build(story, onFirstPage=footer, onLaterPages=footer)
    content = output.getvalue()
    pages = len(PdfReader(BytesIO(content)).pages)
    if comparison and pages > 2:
        raise ValueError('El contenido supera dos páginas. Quite opciones o reduzca el análisis antes de exportar.')
    return content, {'page_count': pages, 'orientation': 'landscape' if comparison else 'portrait'}
