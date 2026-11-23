import WorkspaceLayout from '../layout/WorkspaceLayout'
import { useMonitoringViewModel } from '../../viewmodels/useMonitoringViewModel'
import './account.css'

const format = value => value == null ? 'No informado' : Number(value).toLocaleString('es-PE')
const when = value => value ? new Date(value).toLocaleString('es-PE') : '—'
const providers = { local_ai: 'Motor de análisis local', yahoo_chart: 'Datos de mercado' }
export default function AdminMonitoringPage() {
  const vm = useMonitoringViewModel()
  const data = vm.resource.data
  return <WorkspaceLayout><section className="monitoring-page">
    <p className="section-eyebrow">CONTROL DE LA PLATAFORMA</p><h1>Uso y operación</h1>
    <p className="section-description">Consumo observado, acceso de las cuentas y estado de los servicios.</p>
    <nav className="company-sections" aria-label="Secciones de monitoreo">
      {[['ai', 'Inteligencia artificial'], ['plans', 'Planes'], ['budgets', 'Presupuestos'], ['subscriptions', 'Suscripciones'], ['errors', 'Registros']].map(([key, label]) =>
        <button key={key} onClick={() => vm.choose(key)} aria-pressed={vm.section === key}>{label}</button>)}
    </nav>
    {vm.section === 'ai' && <div className="monitoring-filters"><label>Desde<input type="date" value={vm.start} onChange={e => vm.setStart(e.target.value)} /></label><label>Hasta<input type="date" value={vm.end} onChange={e => vm.setEnd(e.target.value)} /></label></div>}
    {vm.section === 'subscriptions' && <label className="monitoring-filters">Mes<input type="month" value={vm.month} onChange={e => vm.setMonth(e.target.value)} /></label>}
    {vm.section === 'errors' && <label className="monitoring-filters">Nivel<select value={vm.level} onChange={e => vm.setLevel(e.target.value)}><option value="all">Todos</option><option value="warning">Advertencias</option><option value="error">Errores</option></select></label>}
    {vm.resource.loading && <p role="status">Consultando información…</p>}
    {(vm.resource.error || vm.error) && <div role="alert"><p>{vm.resource.error || vm.error}</p><button onClick={vm.resource.reload}>Volver a consultar</button></div>}
    {vm.message && <p role="status">{vm.message}</p>}
    {data && <>
      {vm.section === 'ai' && <>
        <div className="monitoring-stats"><article><small>Llamadas al motor</small><strong>{format(data.calls)}</strong></article><article><small>Sin tarifa calculable</small><strong>{format(data.unpriced_calls)}</strong></article>
          <article><small>Coste estimado</small><strong>{data.estimated_costs.length ? data.estimated_costs.map(row => `${row.currency} ${row.amount}`).join(' · ') : 'No calculado'}</strong></article></div>
        <p className="monitoring-note">{data.explanation}</p>
        {!data.calls ? <div className="product-empty"><h2>Aún no hay llamadas en este período</h2><p>El consumo aparecerá al generar un nuevo análisis con el motor. Consultar uno guardado no cuenta como otra generación.</p></div> :
          <div className="scenario-table"><table><caption>Consumo por proveedor y modelo</caption><thead><tr><th>Proveedor</th><th>Modelo</th><th>Llamadas</th><th>Tokens de entrada</th><th>Tokens de salida</th><th>Sin medición completa</th></tr></thead><tbody>{data.breakdown.map(row => <tr key={row.provider+row.model}><td>{providers[row.provider] || row.provider}</td><td>{row.model}</td><td>{format(row.calls)}</td><td>{format(row.input_tokens)}</td><td>{format(row.output_tokens)}</td><td>{format(row.unreported_tokens)}</td></tr>)}</tbody></table></div>}
      </>}
      {vm.section === 'plans' && <><div className="monitoring-stats"><article><small>Cuentas registradas</small><strong>{data.total}</strong></article><article><small>Cuentas bloqueadas</small><strong>{data.blocked}</strong></article></div>
        <p className="monitoring-note">{data.rule}</p>{data.results.map(row => <article className="plan-distribution" key={row.plan}><div><strong>{row.plan === 'pro' ? 'Pro' : 'Free'}</strong><span>{row.count} cuentas · {row.percentage} %</span></div><progress aria-label={`Porcentaje ${row.plan}`} value={row.percentage} max="100" /></article>)}
        {!data.total && <p>No hay cuentas registradas.</p>}</>}
      {vm.section === 'budgets' && <><p className="monitoring-note">{data.explanation}</p><div className="budget-grid">{data.results.map(row => <article className="budget-card" key={row.provider}>
        <div className="budget-heading"><h2>{providers[row.provider] || row.provider}</h2><span className={`status-label status-${row.status}`}>{row.status === 'exhausted' ? 'Límite alcanzado' : row.status === 'warning' ? 'Consumo alto' : 'Disponible'}</span></div>
        <p><strong>{format(row.used)}</strong> de {format(row.limit)} llamadas · {row.percentage} %</p><progress value={Math.min(100, Number(row.percentage))} max="100" aria-label={`Consumo ${row.provider}`} />
        <p>Reinicio: {when(row.resets_at)} · {row.timezone}</p><button onClick={() => vm.setPending({ ...row, limit: row.limit })}>Ajustar límite</button>
      </article>)}</div>
        {vm.pending && <form className="sprint3-panel" onSubmit={vm.saveBudget}><h2>Ajustar presupuesto</h2><label>Llamadas por día<input type="number" min="1" max="1000000" required value={vm.pending.limit} onChange={e => vm.setPending({ ...vm.pending, limit: e.target.value })} /></label><label>Zona de reinicio<input required value={vm.pending.timezone} onChange={e => vm.setPending({ ...vm.pending, timezone: e.target.value })} /></label><button disabled={vm.busy} type="submit">{vm.busy ? 'Guardando…' : 'Guardar límite'}</button><button type="button" disabled={vm.busy} onClick={() => vm.setPending(null)}>Cancelar</button></form>}
      </>}
      {vm.section === 'subscriptions' && <><div className="monitoring-stats"><article><small>Planes incluidos</small><strong>{data.plans_included}</strong></article><article><small>Valor estimado del mes</small><strong>{data.results.length ? data.results.map(row => `${row.currency} ${row.amount}`).join(' · ') : '0,00'}</strong></article></div><p className="monitoring-note">{data.explanation}</p></>}
      {vm.section === 'errors' && <>{!data.results.length ? <div className="product-empty"><h2>Sin registros para este filtro</h2><p>Los incidentes operativos aparecerán aquí con contexto depurado.</p></div> : <div className="scenario-table"><table><caption>Registros · conservación de {data.retention_days} días</caption><thead><tr><th>Fecha</th><th>Nivel</th><th>Servicio</th><th>Detalle</th></tr></thead><tbody>{data.results.map(row => <tr key={row.id}><td>{when(row.created_at)}</td><td><span className={`status-label status-${row.level}`}>{row.level === 'warning' ? 'Advertencia' : 'Error'}</span></td><td>{providers[row.component] || row.component}</td><td><strong>{row.summary}</strong><small>{row.code}</small><button onClick={() => vm.setLogId(row.id)}>Ver detalle</button></td></tr>)}</tbody></table></div>}
        {vm.logId && <section className="sprint3-panel" aria-label="Detalle del registro"><h2>Detalle del registro</h2>
          {vm.log.loading && <p role="status">Consultando detalle…</p>}{vm.log.error && <p role="alert">{vm.log.error}</p>}
          {vm.log.data && <dl><dt>Servicio</dt><dd>{providers[vm.log.data.component] || vm.log.data.component}</dd><dt>Fecha</dt><dd>{when(vm.log.data.created_at)}</dd><dt>Detalle</dt><dd>{vm.log.data.summary}</dd>
            <dt>Código</dt><dd>{vm.log.data.code}</dd>{Object.entries(vm.log.data.context).map(([key, value]) => <div key={key}><dt>{key === 'http_status' ? 'Estado HTTP' : key === 'attempt' ? 'Intento' : 'Admite reintento'}</dt><dd>{String(value)}</dd></div>)}</dl>}
          <button onClick={() => vm.setLogId(null)}>Cerrar detalle</button></section>}
        <nav className="pagination" aria-label="Páginas de registros"><button disabled={!data.previous} onClick={() => vm.setPage(vm.page-1)}>Anterior</button><span>Página {vm.page}</span><button disabled={!data.next} onClick={() => vm.setPage(vm.page+1)}>Siguiente</button></nav></>}
      <p className="monitoring-note">Actualizado: {when(data.generated_at)}</p>
    </>}
  </section></WorkspaceLayout>
}
