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
    chart: { animation: false, zooming: { type: 'x' }, height: 380 },
    title: { text: `${ticker} · Cierre ajustado` },
    rangeSelector: { enabled: false },
    navigator: { enabled: true }, scrollbar: { enabled: true },
    xAxis: { crosshair: true }, yAxis: { title: { text: data?.currencies?.[0] || 'Precio' } },
    tooltip: { split: false, valueDecimals: 2, xDateFormat: '%d/%m/%Y' },
    plotOptions: { series: { animation: false, dataGrouping: { enabled: false } } },
    series: [{ name: 'Cierre ajustado', type: 'line', turboThreshold: 10000,
      data: (data?.history || []).map(point => [Date.parse(`${point.date}T00:00:00Z`), point.price]) }],
    accessibility: { enabled: false },
  }), [ticker, data])
  return <section className="price-chart" aria-label="Historial de precios">
    <h3>Historial de precios</h3>
    <div role="group" aria-label="Rango de precios">{['1M', '6M', '1Y', '5Y'].map(value =>
      <button key={value} aria-pressed={period === value} onClick={() => setPeriod(value)}>{value}</button>)}</div>
    {loading ? <p>Cargando historial...</p> : error ? <p role="alert">{error}</p> : !data?.history?.length
      ? <p>No hay precios importados para este rango.</p>
      : <>
        <p>Fuente: {data.sources.join(', ')} · {data.is_demo ? 'Datos de demostración' : 'Datos importados'}</p>
        <p>Arrastra sobre el gráfico para ampliar; usa el navegador inferior para cambiar el intervalo.</p>
        <HighchartsReact highcharts={Highcharts} constructorType="stockChart" options={options} />
      </>}
  </section>
}
