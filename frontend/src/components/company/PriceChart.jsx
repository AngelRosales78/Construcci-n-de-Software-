import { useState, useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import Highcharts from 'highcharts/highstock'
import HighchartsReact from 'highcharts-react-official'
import api from '../../services/api'

export default function PriceChart() {
  const { ticker } = useParams()
  const [period, setPeriod] = useState('1M')
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  useEffect(() => {
    let active = true
    setLoading(true)
    setError('')
    setData(null)
    api.get(`/companies/${encodeURIComponent(ticker)}/history/?range=${period}`)
      .then(response => { if (active) setData(response.data) })
      .catch(() => { if (active) setError('No se pudo cargar el historial de precios.') })
      .finally(() => { if (active) setLoading(false) })
    return () => { active = false }
  }, [ticker, period])
  const options = useMemo(() => ({
    chart: { animation: false, zooming: { type: 'x' }, height: 380, backgroundColor: 'transparent', style: { fontFamily: 'Segoe UI, sans-serif' }, spacing: [24, 8, 8, 8] },
    title: { text: `${ticker} · Cierre ajustado`, align: 'left', style: { fontSize: '14px', fontWeight: '600', color: '#5d6c74' } },
    rangeSelector: { enabled: false },
    navigator: { enabled: true, maskFill: 'rgba(20,121,95,0.08)', series: { color: '#76b698', lineWidth: 1 }, handles: { backgroundColor: '#fff', borderColor: '#b6c9bf' } }, scrollbar: { enabled: true },
    xAxis: { crosshair: true, lineColor: '#dce5e0', tickColor: '#dce5e0' }, yAxis: { title: { text: data?.currencies?.[0] || 'Precio' }, gridLineColor: '#edf1ee' },
    tooltip: { split: false, valueDecimals: 2, xDateFormat: '%d/%m/%Y' },
    plotOptions: { series: { animation: false, dataGrouping: { enabled: false } } },
    series: [{ name: 'Cierre ajustado', type: 'area', threshold: null, color: '#13866a', lineWidth: 2, fillColor: { linearGradient: [0, 0, 0, 350], stops: [[0, 'rgba(19,134,106,0.16)'], [1, 'rgba(19,134,106,0.01)']] }, turboThreshold: 10000,
      data: (data?.history || []).map(point => [Date.parse(`${point.date}T00:00:00Z`), point.price]) }],
    accessibility: { enabled: false },
  }), [ticker, data])
  return <section className="price-chart" aria-label="Historial de precios">
    <div className="chart-heading"><div><p className="section-eyebrow">EVOLUCIÓN EN EL MERCADO</p><h3>Historial de precios</h3></div>
    <div role="group" aria-label="Rango de precios">{['1M', '6M', '1Y', '5Y'].map(value =>
      <button key={value} aria-pressed={period === value} onClick={() => setPeriod(value)}>{value}</button>)}</div></div>
    {loading ? <p>Cargando historial...</p> : error ? <p role="alert">{error}</p> : !data?.history?.length
      ? <p>No hay precios importados para este rango.</p>
      : <>
        <HighchartsReact highcharts={Highcharts} constructorType="stockChart" options={options} />
        <div className="chart-caption"><p>Arrastra para ampliar; usa el navegador inferior para cambiar el intervalo.</p><p>Fuente: {data.sources.join(', ')} · {data.is_demo ? 'Datos de demostración' : 'Datos importados'}</p></div>
      </>}
  </section>
}
