export default function LoadingState({ variant = 'company' }) {
  const catalog = variant === 'catalog'
  return <section className={`loading-state loading-state-${variant}`} role="status" aria-busy="true">
    <p>{catalog ? 'Cargando empresas…' : 'Cargando ficha financiera…'}</p>
    <div className="loading-placeholder" aria-hidden="true">
      {catalog ? <div className="loading-card-grid">{Array.from({length:6}, (_, index) => <div className="skeleton-surface" key={index}>
        <div className="skeleton-company"><span className="skeleton-logo"/><span className="skeleton-line skeleton-short"/></div>
        <span className="skeleton-line"/><span className="skeleton-line skeleton-medium"/>
        <div className="skeleton-values"><span className="skeleton-line"/><span className="skeleton-line skeleton-medium"/></div>
      </div>)}</div> : <>
        <div className="skeleton-company-header"><span className="skeleton-logo"/><div><span className="skeleton-line"/><span className="skeleton-line skeleton-short"/></div></div>
        <div className="skeleton-tabs"><span/><span/><span/><span/></div>
        <div className="skeleton-surface skeleton-reading"><span className="skeleton-line skeleton-short"/><span className="skeleton-line skeleton-title"/><span className="skeleton-line skeleton-medium"/><span className="skeleton-scale"/></div>
      </>}
    </div>
  </section>
}
