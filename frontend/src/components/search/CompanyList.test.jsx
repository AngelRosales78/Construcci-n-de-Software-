import { afterEach, expect, test } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import CompanyList from './CompanyList'

afterEach(cleanup)
const show = company => render(<MemoryRouter><CompanyList companies={[{
  ticker: 'MSFT', name: 'Microsoft', sector: 'technology', sector_display: 'Technology', ...company,
}]} loading={false} /></MemoryRouter>)

test('usa el logo verificado local y mantiene el cero financiero', () => {
  show({ market_cap: 0 })
  expect(screen.getByRole('img').getAttribute('src')).toBe('/company-logos/MSFT.png')
  expect(screen.getByText('$0.00B')).toBeTruthy()
  expect(screen.getByText('Indicadores disponibles')).toBeTruthy()
})

test('muestra ticker y dato ausente cuando falta el logo o la capitalización', () => {
  show({ ticker: 'VMRK', name: 'Vivmark Residential', market_cap: null })
  expect(screen.queryByRole('img')).toBeNull()
  expect(screen.getByLabelText('Logo no disponible para Vivmark Residential')).toBeTruthy()
  expect(screen.getByText('Información financiera pendiente')).toBeTruthy()
})

test('reemplaza una imagen fallida por el ticker visible', () => {
  show({})
  fireEvent.error(screen.getByRole('img'))
  expect(screen.queryByRole('img')).toBeNull()
  expect(screen.getByLabelText('Logo no disponible para Microsoft')).toBeTruthy()
})

test('distingue los balances anuales sin presentar ratios del resumen inexistentes', () => {
  show({ ticker: 'GOOGL', name: 'Alphabet', has_annual_data: true, has_price_data: true })
  expect(screen.getByText('Balances anuales disponibles')).toBeTruthy()
  expect(screen.queryByText('Información financiera pendiente')).toBeNull()
  expect(screen.queryByText('PER')).toBeNull()
})

test('identifica una empresa con precios pero sin resumen ni balances', () => {
  show({ has_annual_data: false, has_price_data: true })
  expect(screen.getByText('Solo precios históricos')).toBeTruthy()
})
