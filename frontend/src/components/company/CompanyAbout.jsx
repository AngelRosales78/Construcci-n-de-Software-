export default function CompanyAbout({ company }) {
  const details = company.catalog_details || {}
  const facts = [['Actividad', details.industry_label || details.industry], ['Sede de referencia', details.headquarters], ['Fundación', details.founded], ['Identificador SEC · CIK', details.cik]]
    .filter(([, value]) => value)
  if (company.is_active !== false && company.is_index_member !== false && !company.description && !company.website_url && !facts.length) return null
  const sourcedDescription = company.description && company.description === details.imported_description
  return <section className="company-about" aria-label="Información de la empresa">
    {company.is_active === false && <p role="status">Empresa inactiva en el catálogo. Se conserva su ficha histórica y sus favoritos; no aparece en los buscadores.</p>}
    {company.is_index_member === false && <p>Empresa añadida al catálogo. Su pertenencia al índice S&amp;P 500 no se ha verificado.</p>}
    <div className="company-about-heading"><div><p className="section-eyebrow">LA EMPRESA</p><h2>Acerca de {company.name}</h2></div>
      {company.website_url && <a href={company.website_url} target="_blank" rel="noopener noreferrer">Visitar sitio web oficial <span aria-hidden="true">↗</span></a>}</div>
    {company.description && <p className="company-description">{company.description}</p>}
    {!!facts.length && <dl className="company-facts">{facts.map(([label, value]) => <div key={label}><dt>{label}</dt><dd>{value}</dd></div>)}</dl>}
    {(details.catalog_source_url || details.profile_source_url || details.regulatory_url || (sourcedDescription && details.description_source_url)) && <details className="company-profile-sources"><summary>Fuentes de la información de la empresa</summary>
      {details.catalog_source_url && <p><a href={details.catalog_source_url} target="_blank" rel="noopener noreferrer">Catálogo de referencia</a>{details.catalog_snapshot_date && ` · Instantánea del ${details.catalog_snapshot_date}`}. La actividad, sede y fundación corresponden a esa referencia.</p>}
      {details.profile_source_url && <p><a href={details.profile_source_url} target="_blank" rel="noopener noreferrer">Perfil público en Wikidata</a>{details.retrieved_at && ` · Consultado el ${new Date(details.retrieved_at).toLocaleDateString('es-PE')}`}.{sourcedDescription && details.description_origin === 'wikidata' && ' La reseña breve procede de este perfil.'}</p>}
      {sourcedDescription && details.description_source_url && <p><a href={details.description_source_url} target="_blank" rel="noopener noreferrer">Fuente de la reseña</a></p>}
      {company.website_url === details.imported_website && details.website_source_url && details.website_source_url !== details.profile_source_url && <p><a href={details.website_source_url} target="_blank" rel="noopener noreferrer">Referencia del sitio web de la empresa</a></p>}
      {details.regulatory_url && <p><a href={details.regulatory_url} target="_blank" rel="noopener noreferrer">Consultar informes de la empresa en SEC</a>. Este enlace no implica que todos sus balances estén cargados aquí.</p>}
    </details>}
  </section>
}
