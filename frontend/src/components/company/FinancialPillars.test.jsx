import React from 'react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import FinancialPillars from './FinancialPillars'
import api from '../../services/api'

vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }))
afterEach(cleanup)
beforeEach(() => vi.resetAllMocks())

const response = (value = '1,10') => ({
  fiscal_year: 2024, period: { start: '2023-07-01', end: '2024-06-30' }, available_years: [2024, 2023],
  pillars: [{ id: 'liquidity', label: 'Liquidez', metrics: [{
    id: 'quick_ratio', label: 'Prueba ácida', status: 'available', display_value: value,
    unit: 'times', formula: '(cash + short_term_investments + receivables) / current_liabilities',
    period: { start: '2023-07-01', end: '2024-06-30' }, components: [],
  }] }],
})
const mount = () => render(<MemoryRouter initialEntries={['/company/MSFT']}><Routes><Route path="/company/:ticker" element={<FinancialPillars />} /></Routes></MemoryRouter>)

test('presenta el decimal del servidor sin recalcular y el periodo anual', async () => {
  api.get.mockResolvedValue({ data: response() })
  mount()
  await screen.findByText('1,10')
  expect(screen.getByText(/Ejercicio: 2024/).textContent).toContain('2024-06-30')
  expect(api.get).toHaveBeenCalledWith('/companies/MSFT/pillars/')
  expect(screen.getByText('veces')).toBeTruthy()
})

test('un valor cero es distinto de un dato faltante', async () => {
  const data = response('0,00')
  data.pillars[0].metrics.push({ id: 'current_ratio', label: 'Liquidez corriente',
    status: 'unavailable', value: null, explanation: 'El divisor es cero.', components: [] })
  api.get.mockResolvedValue({ data })
  mount()
  await screen.findByText('0,00')
  expect(within(screen.getByRole('article', { name: 'Liquidez corriente' })).getByText('No disponible')).toBeTruthy()
  expect(screen.getByText('El divisor es cero.')).toBeTruthy()
})

test('muestra error y permite reintentar la consulta de pilares', async () => {
  api.get.mockRejectedValueOnce(new Error('sin conexión')).mockResolvedValue({ data: response() })
  mount()
  await screen.findByRole('alert')
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar pilares' }))
  await screen.findByText('1,10')
  expect(screen.queryByRole('alert')).toBeNull()
})

test('cambiar ejercicio pide al servidor ese año y retira las cifras anteriores', async () => {
  let finish
  api.get.mockResolvedValueOnce({ data: response() }).mockImplementationOnce(() => new Promise(resolve => { finish = resolve }))
  mount()
  await screen.findByText('1,10')
  fireEvent.change(screen.getByLabelText('Ejercicio fiscal'), { target: { value: '2023' } })
  await screen.findByText('Cargando pilares financieros...')
  expect(screen.queryByText('1,10')).toBeNull()
  await waitFor(() => expect(api.get).toHaveBeenLastCalledWith('/companies/MSFT/pillars/?fiscal_year=2023'))
  const data = response('2,20'); data.fiscal_year = 2023
  finish({ data })
  await screen.findByText('2,20')
})

test('presenta rentabilidad negativa y ausencia de referencia sectorial con explicación', async () => {
  const data = response()
  data.pillars.push({ id: 'profitability', label: 'Rentabilidad',
    sector_reference: { status: 'unavailable', explanation: 'No hay muestra comparable suficiente.' },
    metrics: [{ id: 'roe', label: 'ROE', status: 'available', display_value: '-25,00', unit: 'percent',
      components: [], formula_display: 'Beneficio neto / Patrimonio promedio × 100', period: data.period }] })
  api.get.mockResolvedValue({ data })
  mount()
  await screen.findByText('-25,00')
  expect(screen.getByText(/Referencia sectorial no disponible/)).toBeTruthy()
  expect(screen.getByText(/Beneficio neto \/ Patrimonio promedio/)).toBeTruthy()
})

test('identifica solvencia por separado de Altman y muestra umbrales explicados', async () => {
  const data = response()
  data.pillars.push({ id: 'solvency', label: 'Solvencia', metrics: [{
    id: 'interest_coverage', label: 'Cobertura de intereses', status: 'available', display_value: '4,00',
    unit: 'times', components: [], period: data.period, formula: '(EBIT) / Intereses',
    thresholds: { lower: '1', upper: '3', direction: 'higher', version: 'academic-thresholds-v1',
      band: 'high', explanation: 'Reglas académicas sin óptimo universal.' },
  }] })
  api.get.mockResolvedValue({ data })
  mount()
  await screen.findByText('4,00')
  expect(screen.getByText(/distintos del indicador Altman Z/)).toBeTruthy()
  expect(screen.getByText(/Umbrales academic-thresholds-v1/)).toBeTruthy()
  expect(screen.getByText('Tramo favorable según regla académica')).toBeTruthy()
})
