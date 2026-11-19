import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import ConfirmationDialog from './ConfirmationDialog'
import { useAdminCompaniesViewModel } from '../../viewmodels/useAdminCompaniesViewModel'
import { useMarketDataViewModel } from '../../viewmodels/useMarketDataViewModel'
import MarketDataControls from './MarketDataControls'
import { sectorLabel } from '../../utils/financialPresentation'
import './account.css'

const metricNames = { market_cap: 'Capitalización', per: 'PER', roe: 'ROE', beta: 'Beta', dividend_yield: 'Rendimiento por dividendo' }
const updateStates = { succeeded: 'Completada', running: 'En curso', failed: 'Fallida' }

export default function AdminCompaniesPage() {
  const vm = useAdminCompaniesViewModel()
  const market = useMarketDataViewModel(vm.resource.owner, vm.resource.reload)
  const appliedQuery = vm.params.get('q') || ''
  const [query, setQuery] = useState(appliedQuery)
  useEffect(() => setQuery(appliedQuery), [appliedQuery])
  const sectors = [...(vm.sectors.data?.sectors || [])].sort((a,b) => sectorLabel(a.value,a.label).localeCompare(sectorLabel(b.value,b.label),'es'))
  return <WorkspaceLayout><div className="account-page">
    <Link to="/admin-panel">Volver a administración</Link><h1>Empresas y datos</h1>
    <p>Administra el catálogo y comprueba qué datos tiene cada empresa.</p>
    <MarketDataControls market={market} sectors={sectors} usage={vm.resource.data?.market_provider} />
    {vm.sectors.error && <p role="alert">{vm.sectors.error} <button type="button" onClick={vm.sectors.reload}>Reintentar sectores disponibles</button></p>}
    <form className="account-filters" onSubmit={event => { event.preventDefault(); vm.update({ q: query }) }}>
      <label>Empresa o ticker<input value={query} maxLength={120} onChange={event => setQuery(event.target.value)} /></label><button>Buscar</button>
      <label>Listado<select value={vm.params.get('scope') || 'all'} onChange={event => vm.update({ scope: event.target.value })}>
        <option value="all">Catálogo completo</option><option value="pilot">15 empresas piloto</option></select></label>
      <label>Estado<select value={vm.params.get('state') || 'all'} onChange={event => vm.update({ state: event.target.value })}>
        <option value="all">Todos los estados</option><option value="active">Activas</option><option value="inactive">Inactivas</option></select></label>
      <label>Datos<select value={vm.params.get('coverage') || 'all'} onChange={event => vm.update({ coverage: event.target.value })}>
        <option value="all">Todas las coberturas</option><option value="loaded">Con datos cargados</option><option value="missing">Sin datos cargados</option></select></label>
    </form>
    {(vm.error || vm.resource.error) && <p role="alert">{vm.error || vm.resource.error}</p>}
    {vm.resource.loading && <p role="status">Consultando empresas…</p>}
    {vm.message && <p role="status">{vm.message}</p>}
    {vm.resource.data && <>
      <p>{vm.resource.data.count} empresas encontradas · {vm.resource.data.pilot.selected} de {vm.resource.data.pilot.target} seleccionadas para el piloto.</p>
      <p className="catalog-note">La selección piloto es una propuesta. Pertenecer al catálogo o al piloto no significa tener indicadores completos. Las fechas corresponden a datos importados.</p>
      <div className="account-table"><table><thead><tr><th>Empresa</th><th>Estado</th><th>Cobertura y procedencia</th><th>Acciones</th></tr></thead>
        <tbody>{vm.resource.data.results.map(company => <tr key={company.id}>
          <td><Link to={`/company/${encodeURIComponent(company.ticker)}`}>{company.name} · {company.ticker}</Link>
            <small>{sectorLabel(company.sector,company.sector_display)}{company.is_pilot ? ' · Piloto' : ''}</small></td>
          <td><span className={`catalog-state ${company.is_active ? 'active' : 'inactive'}`}>{company.is_active ? 'Activa' : 'Inactiva'}</span></td>
          <td><div>Resumen: {company.coverage.summary_fields}/{company.coverage.summary_total} indicadores · {company.coverage.summary_as_of || 'Sin balance de resumen'}</div>
            <div>Ejercicios: {company.coverage.fiscal_years.join(', ') || 'Sin estados anuales'}</div>
            <div>Precios hasta: {company.coverage.price_as_of || 'Sin precios'}</div>
            {company.is_index_member === false && <small>Alta manual · pertenencia al índice sin verificar.</small>}
            {company.last_update && <small>Última consulta: {company.last_update.finished_at ? new Date(company.last_update.finished_at).toLocaleString('es-PE') : 'En curso'} · {updateStates[company.last_update.status] || company.last_update.status}
              {company.last_update.status === 'failed' && <> · {company.last_update.message}</>}</small>}
            <details><summary>Ver fuentes y fechas</summary>
              {!company.coverage.sources.length && <p>Sin fuentes importadas.</p>}
              {company.coverage.sources.map(source => <p className="catalog-source" key={source}>{source}</p>)}
              {Object.entries(company.coverage.field_sources).map(([field, value]) => <p key={field}>{metricNames[field] || field} · {value.as_of} · {value.method}<br /><a href={value.source} target="_blank" rel="noopener noreferrer">Consultar fuente del indicador</a></p>)}
            </details></td>
          <td><button disabled={vm.busy || market.busy} onClick={() => vm.choose(company,true)}>Editar {company.ticker}</button>
            <button disabled={vm.busy || market.busy} onClick={() => vm.choose(company)}>{company.is_active ? 'Desactivar' : 'Activar'} {company.ticker}</button>
            <button disabled={vm.busy || market.busy} onClick={() => market.chooseRefresh(company)}>Actualizar precios {company.ticker}</button></td>
        </tr>)}</tbody></table></div>
      {!vm.resource.data.results.length && <p>No hay empresas con esos filtros.</p>}
      <div className="account-actions"><button disabled={!vm.resource.data.previous || vm.resource.loading} onClick={() => vm.go(-1)}>Anterior</button>
        <span>Página {vm.params.get('page') || 1}</span><button disabled={!vm.resource.data.next || vm.resource.loading} onClick={() => vm.go(1)}>Siguiente</button></div>
    </>}
    <ConfirmationDialog open={Boolean(vm.pending)} title={vm.pending?.edit ? 'Editar empresa' : 'Confirmar estado de empresa'}
      busy={vm.busy} onCancel={vm.cancel} onConfirm={vm.confirm} confirmLabel="Guardar empresa">
      {vm.pending && <><p>{vm.pending.company.name} · {vm.pending.company.ticker}</p>
        {vm.pending.edit ? <>
          <label>Descripción<textarea rows={4} maxLength={3000} value={vm.form.description} onChange={event => vm.field('description',event.target.value)} /></label>
          <label>Sector<select required disabled={!vm.sectors.data} value={vm.form.sector} onChange={event => vm.field('sector',event.target.value)}>
            {sectors.map(sector => <option key={sector.value} value={sector.value}>{sectorLabel(sector.value,sector.label)}</option>)}</select></label>
          {vm.sectors.error && <p role="alert">{vm.sectors.error} <button type="button" onClick={vm.sectors.reload}>Reintentar sectores</button></p>}
          <label>Sitio web oficial<input type="url" maxLength={500} placeholder="https://empresa.com" value={vm.form.website_url} onChange={event => vm.field('website_url',event.target.value)} /></label>
        </> : <p>{vm.form.is_active ? 'Volverá a aparecer en los buscadores.' : 'Dejará de aparecer en los buscadores. Su ficha, favoritos y datos históricos se conservarán.'}</p>}
      </>}{vm.pending && vm.error && <p role="alert">{vm.error}</p>}
    </ConfirmationDialog>
  </div></WorkspaceLayout>
}
