import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import FinancialPillars, { PillarMetric } from './FinancialPillars'
import DataSources from './DataSources'

const source = { source_kind: 'yahoo', period_basis: 'provider_normalized', url: 'https://finance.yahoo.com/quote/AAPL/financials/',
  tag: 'annualCurrentAssets', currency: 'USD', filed: null, accession: '' }
const reference = { start: null, end: '2026-09-30' }
const data = { fiscal_year: 2026, period: reference, period_basis: 'provider_normalized',
  period_note: 'Referencia anual normalizada. El inicio contable exacto y la publicación no se proporcionan.', currency: 'USD', available_years: [2026],
  annual_components: [{ id:'total_assets', label:'Activos totales', value:'404548000000', source },
    { id:'interest_expense', label:'Gasto por intereses', value:null, source },
    { id:'inventory', label:'Inventario', value:'0', source }], pillars: [] }
vi.mock('../../viewmodels/useCompanyDashboardViewModel', () => ({ useFinancialPillarsViewModel: () => ({
  data, year:'', setYear:vi.fn(), loading:false, error:'', retry:vi.fn(),
}) }))
vi.mock('./FinancialAnalysis', () => ({ default: () => null }))
afterEach(cleanup)

it('identifica la referencia y no atribuye etiquetas o publicaciones SEC al proveedor', () => {
  render(<PillarMetric metric={{ label:'Liquidez corriente', status:'available', display_value:'1,02', unit:'times',
    period:reference, period_basis:'provider_normalized', components:[{id:'current_assets',label:'Activos corrientes',value:'173857000000',source,period:reference}] }} />)
  fireEvent.click(screen.getByText('Ver fórmula, componentes y fuente'))
  expect(screen.getByText('Referencia anual del proveedor: 2026-09-30')).toBeTruthy()
  expect(screen.getByText(/Campo del proveedor: annualCurrentAssets/)).toBeTruthy()
  expect(screen.getByText(/Publicación no proporcionada/)).toBeTruthy()
  expect(screen.queryByText(/Etiqueta SEC/)).toBeNull()
})

it('presenta importes completos, distingue cero de ausencia y avisa sobre las fechas', () => {
  render(<FinancialPillars />)
  expect(screen.getByRole('note').textContent).toContain('normalizada')
  fireEvent.click(screen.getByText('Ver estados anuales importados'))
  expect(screen.getByRole('table').textContent).toContain('404,548,000,000')
  expect(screen.getByText('Sin dato')).toBeTruthy()
  expect(screen.getByText('0')).toBeTruthy()
  expect(screen.getByText(/no están expresados en millones/)).toBeTruthy()
})

it('distingue la referencia anual normalizada del cierre del precio', () => {
  render(<DataSources company={{ reference_basis:'provider_normalized',as_of:'2026-09-30',source:source.url,
    quote:{source:'https://finance.yahoo.com/quote/AAPL/history/',date:'2026-11-20',currency:'USD'} }} />)
  fireEvent.click(screen.getByText('Ver fuentes y cómo se obtuvieron los indicadores'))
  expect(screen.getByText('Referencia anual normalizada:')).toBeTruthy()
  expect(screen.getByText(/inicio contable exacto ni la fecha de publicación/)).toBeTruthy()
  expect(screen.getByText(/último cierre importado: 2026-11-20/)).toBeTruthy()
})
