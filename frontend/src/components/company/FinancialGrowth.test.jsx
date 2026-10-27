import React from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, render, screen, within, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import FinancialPillars from './FinancialPillars'
import api from '../../services/api'
vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }))
afterEach(() => { cleanup(); vi.resetAllMocks() })

test('crecimiento distingue variación negativa de falta de cuatro cierres', async () => {
  const period = { start: '2023-07-01', end: '2024-06-30' }
  api.get.mockResolvedValue({ data: { fiscal_year: 2024, period, available_years: [2024, 2023],
    pillars: [{ id: 'growth', label: 'Crecimiento', metrics: [
      { id: 'revenue_growth', label: 'Variación anual de ingresos', status: 'available', display_value: '-10,00',
        unit: 'percent', formula: '(Ingresos / Ingresos anteriores − 1) × 100', period, components: [] },
      { id: 'revenue_cagr_3y', label: 'Crecimiento compuesto a tres años', status: 'unavailable', value: null,
        explanation: 'Faltan cuatro cierres consecutivos.', period, components: [] },
    ] }] } })
  render(<MemoryRouter initialEntries={['/company/MSFT']}><Routes><Route path="/company/:ticker" element={<FinancialPillars />} /></Routes></MemoryRouter>)
  fireEvent.click(await screen.findByRole('button', { name: 'Mostrar Crecimiento' }))
  await screen.findByText('-10,00')
  expect(within(screen.getByRole('article', { name: 'Crecimiento compuesto a tres años' })).getByText('No disponible')).toBeTruthy()
  expect(screen.getByText(/no es el retorno de la acción/)).toBeTruthy()
})
