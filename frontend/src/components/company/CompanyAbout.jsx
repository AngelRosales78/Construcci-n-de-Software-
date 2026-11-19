export default function CompanyAbout({ company }) {
  if (company.is_active !== false && !company.description && !company.website_url) return null
  return <section className="company-about" aria-label="Información de la empresa">
    {company.is_active === false && <p role="status">Empresa inactiva en el catálogo. Se conserva su ficha histórica y sus favoritos; no aparece en los buscadores.</p>}
    {company.description && <p>{company.description}</p>}
    {company.website_url && <a href={company.website_url} target="_blank" rel="noopener noreferrer">Visitar sitio web oficial</a>}
  </section>
}
