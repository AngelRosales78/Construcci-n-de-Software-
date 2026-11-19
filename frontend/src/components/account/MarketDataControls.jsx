import ConfirmationDialog from './ConfirmationDialog'
import { sectorLabel } from '../../utils/financialPresentation'

export default function MarketDataControls({ market, sectors, usage }) {
  return <section className="catalog-market" aria-label="Actualización y alta de empresas">
    {usage && <p>Consultas de precios hoy: {usage.calls}/{usage.limit}. Límite interno diario, con reinicio a las 00:00 UTC.</p>}
    <p>La actualización consulta precios históricos ajustados. Los indicadores y balances conservan sus propias fechas.</p>
    {market.busy && <p role="status">Consultando y validando datos… Esta operación puede tardar unos segundos.</p>}
    {market.message && <p role="status">{market.message}</p>}
    {market.error && <p role="alert">{market.error}</p>}
    {market.attempt && !market.busy && <button onClick={market.retry}>Recuperar la misma operación</button>}
    <details><summary>Añadir una empresa</summary>
      <form className="account-filters" onSubmit={event => { event.preventDefault(); market.verify() }}>
        <label>Ticker a verificar<input maxLength={10} required disabled={market.busy} value={market.ticker} onChange={event => market.setTicker(event.target.value)} placeholder="Ejemplo: DUOL" /></label>
        <label>Sector de la nueva empresa<select required disabled={market.busy || !sectors.length} value={market.sector} onChange={event => market.setSector(event.target.value)}>
          <option value="">Selecciona un sector</option>{sectors.map(sector => <option key={sector.value} value={sector.value}>{sectorLabel(sector.value,sector.label)}</option>)}
        </select></label>
        <button type="submit" disabled={market.busy}>Verificar ticker</button>
      </form>
      <p>El nombre y los precios se verifican con Yahoo Finance. El sector lo asigna el administrador. El alta no verifica pertenencia al S&amp;P 500 ni crea ratios sin balances.</p>
      {market.verification && <div className="catalog-verification">
        <h2>{market.verification.result.name} · {market.verification.ticker}</h2>
        <p>{market.verification.result.price_count} precios en {market.verification.result.currency}, hasta {market.verification.result.price_as_of}.</p>
        <a href={market.verification.result.source_url} target="_blank" rel="noopener noreferrer">Consultar fuente verificada</a>
        <p>La verificación es válida durante diez minutos. Todavía no se añadió la empresa.</p>
        <button disabled={market.busy} onClick={market.chooseCreate}>Confirmar alta</button>
      </div>}
    </details>
    <ConfirmationDialog open={Boolean(market.pending)} busy={market.busy} onCancel={market.cancel} onConfirm={market.confirm}
      title={market.pending?.kind === 'create' ? 'Añadir empresa verificada' : 'Actualizar precios históricos'}
      confirmLabel={market.pending?.kind === 'create' ? 'Añadir al catálogo' : 'Actualizar precios'}>
      {market.pending?.kind === 'refresh' ? <p>Se consultarán hasta cinco años de cierres ajustados de {market.pending.company.ticker}. Los balances no se actualizarán en esta operación.</p>
        : <p>Se añadirá la empresa verificada con sus precios. No se asignará una calificación financiera sin datos suficientes.</p>}
      {market.pending && market.error && <p role="alert">{market.error}</p>}
    </ConfirmationDialog>
  </section>
}
