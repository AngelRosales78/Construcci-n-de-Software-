import { useMemo, useState } from 'react'
import Highcharts from 'highcharts/highstock'
import HighchartsReact from 'highcharts-react-official'
import { useSimulationViewModel } from '../../viewmodels/useSimulationViewModel'

const SCENARIOS = [['base', 'Base · percentil 50', '#75ff9e'], ['optimistic', 'Optimista · percentil 90', '#64b8ff'], ['pessimistic', 'Pesimista · percentil 10', '#ffb95f']]
export default function SimulationScenarios() {
  const { data, loading, error, simulate, draft, update } = useSimulationViewModel()
  const [hidden, setHidden] = useState({})
  const options = useMemo(() => ({
    chart: { backgroundColor: 'transparent', height: 340, animation: false },
    title: { text: null }, credits: { enabled: false }, legend: { enabled: false },
    xAxis: { type: 'datetime', labels: { style: { color: '#bacbb9' } } },
    yAxis: { title: { text: data?.currency || 'Precio', style: { color: '#bacbb9' } }, labels: { style: { color: '#bacbb9' } }, gridLineColor: '#343e39' },
    tooltip: { shared: true, valueDecimals: 2, backgroundColor: '#262a31', style: { color: '#dfe2eb' } },
    series: SCENARIOS.filter(([key]) => data?.scenarios?.[key]).map(([key, name, color]) => ({
      name, color, visible: !hidden[key], data: data.scenarios[key].map(point => [Date.parse(`${point.date}T00:00:00Z`), point.price]), animation: false,
    })),
  }), [data, hidden])
  return <section className="simulation-panel" aria-labelledby="simulation-title">
    <p className="section-eyebrow">ESCENARIOS A DOCE MESES</p><h2 id="simulation-title">Simulación de precios</h2>
    <p>Compara tres escenarios a partir del histórico. Son estimaciones, no precios garantizados.</p>
    <form className="simulation-controls" onInvalid={event => { const settings = event.target.closest('details'); if (settings) settings.open = true }} onSubmit={event => { event.preventDefault(); simulate() }}>
      <button className="primary-action" type="submit" disabled={loading}>{loading ? 'Calculando escenarios…' : 'Generar escenarios'}</button>
      <details className="simulation-settings"><summary>Ajustar supuestos <small>Opcional</small></summary><div className="simulation-settings-fields">
      <label>Tasa anual continua del modelo (%)<input aria-label="Tasa anual del modelo" type="number" min="-100" max="150" step="0.1" value={draft.annual_return} onChange={event => update('annual_return', event.target.value)} placeholder="Estimada del histórico" disabled={loading} /><small>Límite: -100 a 150 %. No es crecimiento de ventas.</small></label>
      <label>Volatilidad anual (%)<input aria-label="Volatilidad anual" type="number" min="0" max="200" step="0.1" value={draft.annual_volatility} onChange={event => update('annual_volatility', event.target.value)} placeholder="Estimada del histórico" disabled={loading} /><small>Límite: 0 a 200 %. Variación del precio.</small></label>
      <button type="button" onClick={() => simulate(true)} disabled={loading}>Restablecer supuestos históricos</button>
      </div></details>
    </form>
    {error && <p role="alert">{error}</p>}
    {loading && <p role="status">Generando las trayectorias del mismo conjunto…</p>}
    {data?.status === 'unavailable' && <p>No se dispone de un histórico válido suficiente. Se requieren al menos 253 cierres comparables; disponibles: {data.available_observations}.</p>}
    {data?.status === 'available' && <>
      <p>{data.horizon_months} meses desde el cierre del {data.as_of} · {data.currency}</p>
      <fieldset className="scenario-legend"><legend>Curvas visibles (ocultarlas no recalcula)</legend>{SCENARIOS.map(([key, label, color]) => <label key={key}><input type="checkbox" checked={!hidden[key]} onChange={() => setHidden(previous => ({ ...previous, [key]: !previous[key] }))} /><span className="scenario-swatch" style={{ backgroundColor: color }} aria-hidden="true" /> {label}</label>)}</fieldset>
      <details className="pillar-concept"><summary>Qué significan los escenarios y sus percentiles</summary><p>P50 es el valor central. P10 y P90 muestran extremos de referencia de la distribución simulada; no son límites de pérdida o ganancia.</p><p>La tasa anual expresa el crecimiento continuo supuesto del precio; la volatilidad describe su variación. Si no ajustas los controles, se estiman del histórico.</p></details>
      <HighchartsReact highcharts={Highcharts} options={options} />
      <div className="scenario-table"><table><caption>Precios finales simulados · {data.currency} · misma ejecución</caption><thead><tr><th>Escenario</th><th>Fecha</th><th>Precio</th></tr></thead><tbody>{SCENARIOS.map(([key, label]) => <tr key={key}><th scope="row">{label}</th><td>{data.final_prices[key].date}</td><td>{data.final_prices[key].price.toLocaleString('es-PE', { maximumFractionDigits: 2, minimumFractionDigits: 2 })}</td></tr>)}</tbody></table></div>
      <details className="financial-help"><summary>Ver supuestos y procedencia de esta ejecución</summary><div className="help-content">
        <p>Monte Carlo genera muchas trayectorias posibles. P10, P50 y P90 resumen su distribución por mes; no son pérdidas mínimas ni ganancias máximas.</p>
        <p>{data.assumptions}</p><p>Histórico: {data.historical_start} a {data.as_of} ({data.observations} cierres). Fuente: {data.sources.join(', ')}.</p>
        <p>Tasa anual continua: {(data.parameters.annual_return * 100).toFixed(2)} % · Volatilidad anual: {(data.parameters.annual_volatility * 100).toFixed(2)} %.</p>
        <p>Semilla: {data.seed} · Modelo: {data.model_version} · Ejecución: {data.simulation_id}</p>
        <p>Huella del histórico: {data.source_sha256}. {data.is_demo ? 'Serie de prueba identificada.' : 'Histórico importado; no es una cotización actual.'}</p>
      </div></details>
    </>}
  </section>
}
