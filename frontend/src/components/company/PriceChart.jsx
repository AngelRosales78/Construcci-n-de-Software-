import { useEffect, useMemo, useRef, useState } from 'react'
import { usePriceChartViewModel } from '../../viewmodels/usePriceChartViewModel'
import Highcharts from 'highcharts/highstock'
import HighchartsReact from 'highcharts-react-official'

function PriceSource({ source }) {
  try {
    const url = new URL(source)
    if (['http:', 'https:'].includes(url.protocol)) {
      const label = url.hostname.endsWith('.finance.yahoo.com') || url.hostname === 'finance.yahoo.com' ? 'Yahoo Finance' : url.hostname
      return <a href={url.href} target="_blank" rel="noreferrer">{label}</a>
    }
  } catch { /* Import metadata can also be a plain-text source citation. */ }
  return source
}

export default function PriceChart({ active = true }) {
  const { ticker, period, setPeriod, data, loading, error } = usePriceChartViewModel()
  const [inspection, setInspection] = useState(null)
  const chart = useRef(null)
  useEffect(() => {
    if (!active) return
    const frame = requestAnimationFrame(() => chart.current?.reflow())
    return () => cancelAnimationFrame(frame)
  }, [active, data])
  const history = data?.history || []
  const historyKey = `${ticker}:${period}:${history[0]?.date}:${history.at(-1)?.date}`
  const selectedIndex = inspection?.key === historyKey ? Math.min(inspection.index, history.length - 1) : history.length - 1
  const inspected = history[selectedIndex]
  const options = useMemo(() => ({
    chart: { animation: false, zooming: { type: 'x' }, height: 380, backgroundColor: 'transparent', style: { fontFamily: 'Source Sans 3, sans-serif' }, spacing: [12, 8, 8, 8] },
    title: { text: `${ticker} · Cierre ajustado`, align: 'left', style: { fontSize: '14px', fontWeight: '600', color: '#bacbb9' } },
    rangeSelector: { enabled: false },
    navigator: { enabled: true, maskFill: 'rgba(220,154,118,0.08)', series: { color: '#dc9a76', lineWidth: 1 }, handles: { backgroundColor: '#2e4738', borderColor: '#91a28b' } }, scrollbar: { enabled: true },
    xAxis: { labels: { style: { color: '#bacbb9' } }, crosshair: true, lineColor: '#343e39', tickColor: '#343e39', plotLines: inspected ? [{ value: Date.parse(`${inspected.date}T00:00:00Z`), color: '#dc9a76', width: 1, dashStyle: 'ShortDash', zIndex: 4 }] : [] }, yAxis: { labels: { style: { color: '#bacbb9' } }, title: { style: { color: '#bacbb9' }, text: data?.currencies?.[0] || 'Precio' }, gridLineColor: '#262a31' },
    tooltip: { backgroundColor: '#262a31', borderColor: '#536158', style: { color: '#dfe2eb' }, split: false, valueDecimals: 2, xDateFormat: '%d/%m/%Y' },
    plotOptions: { series: { animation: false, dataGrouping: { enabled: false } } },
    series: [{ name: 'Cierre ajustado', type: 'area', threshold: null, color: '#dc9a76', lineWidth: 2, fillColor: { linearGradient: [0, 0, 0, 350], stops: [[0, 'rgba(220,154,118,0.23)'], [1, 'rgba(220,154,118,0.01)']] }, turboThreshold: 10000,
      data: (data?.history || []).map(point => [Date.parse(`${point.date}T00:00:00Z`), point.price]) }],
    accessibility: { enabled: false },
  }), [ticker, data, inspected?.date])
  return <section className="price-chart" aria-label="Historial de precios">
    <div className="chart-heading"><div><p className="section-eyebrow">EVOLUCIÓN EN EL MERCADO</p><h3>Historial de precios</h3></div>
    <div role="group" aria-label="Rango de precios">{Object.entries({ '1M': '1 mes', '6M': '6 meses', '1Y': '1 año', '5Y': '5 años' }).map(([value, label]) =>
      <button key={value} data-range={value} aria-pressed={period === value} onClick={() => setPeriod(value)}>{label}</button>)}</div></div>
    {loading ? <p role="status">Cargando historial...</p> : error ? <p role="alert">{error}</p> : !data?.history?.length
      ? <p>No hay precios importados para este rango.</p>
      : <>
        <div className="chart-inspection" aria-live="polite"><div><small>Sesión consultada</small><strong>{Number(inspected.price).toLocaleString('es-PE', {minimumFractionDigits:2,maximumFractionDigits:2})} <span>{data.currencies?.[0]}</span></strong></div><time dateTime={inspected.date}>{inspected.date}</time></div>
        <p className="chart-date-range">Periodo disponible: {data.history[0].date} → {data.history[data.history.length - 1].date} · {data.history.length} sesiones</p>
        <HighchartsReact highcharts={Highcharts} constructorType="stockChart" options={options} callback={instance => { chart.current = instance }} />
        <label className="chart-session-control">Consultar un precio histórico<input type="range" aria-label="Consultar un precio histórico" min="0" max={history.length - 1} value={selectedIndex} aria-valuetext={`${inspected.date}: ${inspected.price} ${data.currencies?.[0] || ''}`} onChange={event => setInspection({key:historyKey,index:Number(event.target.value)})} /><span>La línea vertical marca la sesión. Usa el control o las flechas del teclado.</span></label>
        <div className="chart-caption"><p>Arrastra para ampliar; usa el navegador inferior para cambiar el intervalo.</p><p>Fuente: {data.sources.map((source,index) => <span key={`${source}-${index}`}>{index > 0 && ', '}<PriceSource source={source} /></span>)} · {data.is_demo ? 'Datos pendientes de verificar' : 'Datos importados'}</p></div>
      </>}
    <details className="financial-help chart-help"><summary>¿Qué muestra este gráfico?</summary><div className="help-content"><p>El cierre ajustado es un precio histórico que el proveedor corrige por dividendos y divisiones de acciones, para facilitar comparaciones entre fechas.</p><p>Los botones cambian el periodo consultado y mantienen puntos diarios. Se muestran sesiones bursátiles disponibles; no se inventan precios para fechas sin cotización.</p><p>El eje horizontal muestra fechas y el vertical, precios en la moneda indicada. Arrastra sobre el gráfico para ampliar un intervalo.</p></div></details>
  </section>
}
