import { Link } from 'react-router-dom'
import { useComparisonViewModel } from '../../viewmodels/useComparisonViewModel'
import WorkspaceLayout from '../layout/WorkspaceLayout'
import CompanyPicker from './CompanyPicker'
import ComparisonHistory from './ComparisonHistory'
import ReportPanel from '../reports/ReportPanel'

const labels = { liquidity: 'Liquidez', profitability: 'Rentabilidad', solvency: 'Solvencia', efficiency: 'Eficiencia', growth: 'Crecimiento' }
const displayNote = note => note?.status === 'available' ? `${note.display_value} / 100` : 'No disponible'

export default function ComparisonPage() {
  const vm = useComparisonViewModel()
  return <WorkspaceLayout><div className="comparison-page">
    <p className="section-eyebrow">COMPARADOR DE EMPRESAS</p><h1>Dos empresas, un mismo ejercicio</h1>
    <p>Selecciona dos empresas distintas. Las fechas y los datos disponibles determinan qué se puede comparar.</p>
    {vm.accessLoading && <p role="status">Verificando acceso…</p>}
    {vm.accessError && <div role="alert"><p>{vm.accessError}</p><button onClick={vm.refreshAccess}>Reintentar acceso</button></div>}
    {!vm.accessLoading && !vm.accessError && !vm.pro && <section className="comparison-access"><h2>Vista previa · Plan Free</h2>
      <p>Puedes preparar la selección. Consultar las dos empresas juntas requiere un plan Pro activo asignado por administración.</p>
      <p>Este avance no incluye contratación ni pagos.</p><button onClick={vm.refreshAccess}>Verificar mi plan</button></section>}
    {!vm.pro && <details className="sprint3-panel"><summary>Ver plan Pro</summary><p>Free permite consultar fichas y favoritos. Pro añade el comparador y su reporte PDF. El plan lo asigna administración; esta pantalla no cobra ni activa una suscripción.</p>
      <div aria-label="Ejemplo de estructura del comparador"><p>Vista previa ilustrativa, sin cifras de otras empresas:</p><p>Empresa A | Empresa B | Cinco pilares | Rendimiento histórico</p></div>
    </details>}
    {vm.pro && <p className="comparison-access">Plan Pro activo · Acceso verificado por el servidor</p>}
    <div className="comparison-selectors">
      <CompanyPicker side="A" selected={vm.a} onSelect={ticker => vm.select('a', ticker)} onRemove={() => vm.remove('a')} />
      <CompanyPicker side="B" selected={vm.b} onSelect={ticker => vm.select('b', ticker)} onRemove={() => vm.remove('b')} />
    </div>
    <div className="comparison-controls"><label>Ejercicio fiscal de cierre<input type="number" min="1900" max={new Date().getFullYear()} value={vm.year} onChange={event => vm.setYear(event.target.value)} /></label>
      <button type="button" onClick={vm.swap} disabled={!vm.a || !vm.b}>Intercambiar A y B</button></div>
    {!vm.validYear && <p role="alert">Indica un año de cuatro cifras sin fechas futuras.</p>}
    {vm.selectionError && <p role="alert">{vm.selectionError}</p>}
    {(!vm.a || !vm.b) && <p>Selecciona las dos empresas para continuar. La selección se conserva en la dirección de esta página.</p>}
    {vm.loading && <p role="status">Consultando las dos empresas…</p>}
    {vm.error && <div role="alert"><p>{vm.error}</p><button onClick={vm.retry}>Reintentar consulta</button><button onClick={vm.refreshAccess}>Verificar mi plan</button></div>}
    {vm.data && <section aria-label="Resultado de la selección" className="comparison-result">
      <h2>{vm.data.a.ticker} y {vm.data.b.ticker} · FY{vm.data.fiscal_year}</h2>
      <div className="comparison-periods">{['a', 'b'].map(side => <div key={side}><h3>{side.toUpperCase()} · {vm.data[side].ticker}</h3>
        <p>{vm.data[side].period?.start && vm.data[side].period?.end ? `${vm.data[side].period.start} → ${vm.data[side].period.end}` : 'Sin balance para este ejercicio'}</p>
        <Link to={`/company/${vm.data[side].ticker}`}>Ver ficha de {vm.data[side].ticker}</Link>
        <p>Ejercicios disponibles: {vm.data[side].available_years.join(', ') || 'ninguno'}</p>
      </div>)}</div>
      {vm.data.reason && <p className="data-notice" role="status">{vm.data.reason}</p>}
      <p className="comparison-table-hint">Desliza la tabla hacia los lados para ver las dos notas y el resultado.</p>
      <div className="scenario-table" tabIndex="0" role="region" aria-label="Tabla de notas, desplazable horizontalmente"><table><caption>Notas académicas por pilar · Solo se comparan períodos y versiones compatibles</caption>
        <thead><tr><th scope="col">Pilar</th><th scope="col">{vm.data.a.ticker}</th><th scope="col">{vm.data.b.ticker}</th><th scope="col">Resultado</th></tr></thead>
        <tbody>{vm.data.comparisons.map(row => <tr key={row.id}><th scope="row">{labels[row.id] || row.id}</th>
          <td>{displayNote(vm.data.a.pillars.find(p => p.id === row.id)?.score)}</td>
          <td>{displayNote(vm.data.b.pillars.find(p => p.id === row.id)?.score)}</td>
          <td>{row.status === 'available' ? row.result === 'tie' ? 'Empate · 0,00 puntos' : `${row.result === 'a' ? vm.data.a.ticker : vm.data.b.ticker}: mayor nota · A − B: ${row.delta} puntos`
            : <>No comparable<small>{row.reason}</small></>}</td>
        </tr>)}</tbody>
      </table></div>
      <p>Las notas siguen reglas académicas del proyecto y no son una recomendación de inversión.</p>
      <ComparisonHistory a={vm.a} b={vm.b} year={vm.year} />
      <ReportPanel a={vm.a} b={vm.b} year={Number(vm.year)} />
    </section>}
  </div></WorkspaceLayout>
}
