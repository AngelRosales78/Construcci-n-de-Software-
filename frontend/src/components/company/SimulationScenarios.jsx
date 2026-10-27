import { useMemo } from 'react'
import Highcharts from 'highcharts'
import HighchartsReact from 'highcharts-react-official'
import { useSimulationViewModel } from '../../viewmodels/useSimulationViewModel'

const SCENARIOS = [['base', 'Base · percentil 50', '#75ff9e']]
export default function SimulationScenarios() {
  const { data, loading, error, simulate } = useSimulationViewModel()
  const options = useMemo(() => ({
    chart: { backgroundColor: 'transparent', height: 340, animation: false },
    title: { text: null }, credits: { enabled: false }, legend: { itemStyle: { color: '#dfe2eb' } },
    xAxis: { type: 'datetime', labels: { style: { color: '#bacbb9' } } },
    yAxis: { title: { text: data?.currency || 'Precio', style: { color: '#bacbb9' } }, labels: { style: { color: '#bacbb9' } }, gridLineColor: '#343e39' },
    tooltip: { shared: true, valueDecimals: 2, backgroundColor: '#262a31', style: { color: '#dfe2eb' } },
    series: SCENARIOS.filter(([key]) => data?.scenarios?.[key]).map(([key, name, color]) => ({
      name, color, data: data.scenarios[key].map(point => [Date.parse(`${point.date}T00:00:00Z`), point.price]), animation: false,
    })),
  }), [data])
  return <section className="simulation-panel" aria-labelledby="simulation-title">
    <p className="section-eyebrow">ESCENARIOS A DOCE MESES</p><h2 id="simulation-title">Simulación de precios</h2>
    <p>Monte Carlo genera trayectorias posibles a partir del histórico. Son escenarios académicos, no precios futuros garantizados.</p>
    <button className="primary-action" onClick={() => simulate()} disabled={loading}>{loading ? 'Calculando escenarios…' : 'Generar escenarios'}</button>
    {error && <p role="alert">{error}</p>}
    {loading && <p role="status">Generando las trayectorias del mismo conjunto…</p>}
    {data?.status === 'unavailable' && <p>No se dispone de un histórico válido suficiente. Se requieren al menos 253 cierres comparables; disponibles: {data.available_observations}.</p>}
    {data?.status === 'available' && <>
      <p>Origen: {data.as_of} · horizonte: {data.horizon_months} meses · {data.paths} trayectorias · {data.currency}</p>
      <HighchartsReact highcharts={Highcharts} options={options} />
      <details className="financial-help"><summary>Ver supuestos y procedencia de esta ejecución</summary><div className="help-content">
        <p>{data.assumptions}</p><p>Histórico: {data.historical_start} a {data.as_of} ({data.observations} cierres). Fuente: {data.sources.join(', ')}.</p>
        <p>Tasa anual continua: {(data.parameters.annual_return * 100).toFixed(2)} % · Volatilidad anual: {(data.parameters.annual_volatility * 100).toFixed(2)} %.</p>
        <p>Semilla: {data.seed} · Modelo: {data.model_version} · Ejecución: {data.simulation_id}</p>
        <p>Huella del histórico: {data.source_sha256}. {data.is_demo ? 'Serie de prueba identificada.' : 'Histórico importado; no es una cotización actual.'}</p>
      </div></details>
    </>}
  </section>
}
