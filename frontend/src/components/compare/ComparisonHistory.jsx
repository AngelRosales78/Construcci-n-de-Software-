import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import Highcharts from 'highcharts/highstock'
import HighchartsReact from 'highcharts-react-official'
import { useRemoteResource } from '../../viewmodels/useRemoteResource'

export default function ComparisonHistory({ a, b, year }) {
  const [params, setParams] = useSearchParams()
  const range = params.get('range') || '1Y'
  const [requested, setRequested] = useState(false)
  const [cursor, setCursor] = useState(0)
  const resource = useRemoteResource(`/compare/history/?${new URLSearchParams({ a, b, fiscal_year: year, range })}`, requested)
  const data = resource.data?.a === a && resource.data?.b === b ? resource.data : null
  const options = useMemo(() => ({ chart: { height: 320, backgroundColor: 'transparent', animation: false },
    title: { text: 'Rendimiento desde una fecha común (%)' },
    xAxis: { type: 'datetime' }, yAxis: { title: { text: 'Rendimiento (%)' } },
    tooltip: { shared: true, valueSuffix: ' %', valueDecimals: 2 },
    series: [['a', a], ['b', b]].map(([side, name], i) => ({ name, dashStyle: i ? 'ShortDash' : 'Solid',
      data: (data?.points || []).map(p => [Date.parse(p.date + 'T00:00:00Z'), p[side]]), animation: false })),
    credits: { enabled: false }, accessibility: { enabled: false },
  }), [data, a, b])
  const point = data?.points?.[Math.min(cursor, data.points.length - 1)]
  return <section className="sprint3-panel" aria-label="Rendimiento comparativo">
    <h2>Rendimiento histórico</h2>
    <p>Fechas comunes, sin rellenar huecos. Ambas series comienzan en 0 %.</p>
    <label>Rango del comparador<select value={range} onChange={e => setParams(previous => {
      const next = new URLSearchParams(previous); next.set('range', e.target.value); return next
    })}>{['1M', '6M', '1Y', '5Y'].map(value => <option key={value}>{value}</option>)}</select></label>
    <button onClick={() => { setRequested(true); resource.reload() }}>Consultar rendimiento</button>
    {resource.loading && <p role="status">Consultando precios…</p>}
    {resource.error && <p role="alert">{resource.error}</p>}
    {data?.status === 'unavailable' && <p role="status">{data.reason}</p>}
    {data?.status === 'available' && <>
      <p>Fecha base: {data.base_date} · {data.points.length} fechas comunes · {data.is_demo ? 'Serie de prueba' : 'Precios históricos importados'}</p>
      <HighchartsReact highcharts={Highcharts} options={options} />
      <label>Consultar fecha del histórico<input type="range" min="0" max={data.points.length - 1} value={Math.min(cursor, data.points.length - 1)} onChange={e => setCursor(Number(e.target.value))} /></label>
      {point && <p aria-live="polite">{point.date}: {a} {point.a.toFixed(2)} % · {b} {point.b.toFixed(2)} %</p>}
      <p>Fuentes: {data.sources.join(' · ')}. El rendimiento pasado no garantiza resultados futuros.</p>
    </>}
  </section>
}
