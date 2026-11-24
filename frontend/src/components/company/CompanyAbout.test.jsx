import { afterEach, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import CompanyAbout from './CompanyAbout'

afterEach(cleanup)

it('muestra actividad, sede, fundación y fuentes aunque no haya indicadores financieros', () => {
  render(<CompanyAbout company={{ name: '3M', catalog_details: {
    industry_label: 'Conglomerados industriales', headquarters: 'Saint Paul, Minnesota', founded: '1902', cik: '0000066740',
    catalog_source_url: 'https://github.com/datasets/s-and-p-500-companies', catalog_snapshot_date: '2026-10-12',
    regulatory_url: 'https://www.sec.gov/edgar/browse/?CIK=66740',
  } }} />)
  expect(screen.getByRole('heading', { name: 'Acerca de 3M' })).toBeTruthy()
  expect(screen.getByText('Conglomerados industriales')).toBeTruthy()
  expect(screen.getByText('Saint Paul, Minnesota')).toBeTruthy()
  expect(screen.getByText('1902')).toBeTruthy()
  expect(screen.getByRole('link', { name: /Consultar informes/ }).getAttribute('href')).toContain('66740')
  expect(screen.queryByRole('link', { name: /sitio web oficial/ })).toBeNull()
})

it('no atribuye una descripción editada por el equipo al perfil público', () => {
  render(<CompanyAbout company={{ name: 'Microsoft', description: 'Reseña propia.', catalog_details: {
    imported_description: 'Reseña de origen.', description_origin: 'wikidata', profile_source_url: 'https://www.wikidata.org/wiki/Q2283',
  } }} />)
  expect(screen.getByText('Reseña propia.')).toBeTruthy()
  expect(screen.queryByText(/La reseña breve procede de este perfil/)).toBeNull()
})

it('conserva la fuente de una reseña revisada desde la web de la empresa', () => {
  render(<CompanyAbout company={{ name: 'Hims & Hers', description: 'Atención personalizada.', catalog_details: {
    imported_description: 'Atención personalizada.', description_source_url: 'https://investors.hims.com/overview/default.aspx',
  } }} />)
  expect(screen.getByRole('link', { name: 'Fuente de la reseña' }).getAttribute('href')).toContain('investors.hims.com')
})
