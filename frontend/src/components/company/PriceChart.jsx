import { useMemo } from 'react'
import { usePriceChartViewModel } from '../../viewmodels/usePriceChartViewModel'
import Highcharts from 'highcharts/highstock'
import HighchartsReact from 'highcharts-react-official'


export default function PriceChart() {
  const { ticker, period, setPeriod, data, loading, error } = usePriceChartViewModel()
  const options = useMemo(() => ({
    chart: { animation: false, zooming: { type: 'x' }, height: 380, backgroundColor: 'transparent', style: { fontFamily: 'Segoe UI, sans-serif' }, spacing: [24, 8, 8, 8] },
    title: { text: `${ticker} · Cierre ajustado`, align: 'left', style: { fontSize: '14px', fontWeight: '600', color: '#bacbb9' } },
    rangeSelector: { enabled: false },
    navigator: { enabled: true, maskFill: 'rgba(20,121,95,0.08)', series: { color: '#75ff9e', lineWidth: 1 }, handles: { backgroundColor: '#262a31', borderColor: '#536158' } }, scrollbar: { enabled: true },
    xAxis: { labels: { style: { color: '#bacbb9' } }, crosshair: true, lineColor: '#343e39', tickColor: '#343e39' }, yAxis: { labels: { style: { color: '#bacbb9' } }, title: { style: { color: '#bacbb9' }, text: data?.currencies?.[0] || 'Precio' }, gridLineColor: '#262a31' },
    tooltip: { backgroundColor: '#262a31', borderColor: '#536158', style: { color: '#dfe2eb' }, split: false, valueDecimals: 2, xDateFormat: '%d/%m/%Y' },
    plotOptions: { series: { animation: false, dataGrouping: { enabled: false } } },
    series: [{ name: 'Cierre ajustado', type: 'area', threshold: null, color: '#00e676', lineWidth: 2, fillColor: { linearGradient: [0, 0, 0, 350], stops: [[0, 'rgba(0,230,118,0.22)'], [1, 'rgba(0,230,118,0.01)']] }, turboThreshold: 10000,
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
