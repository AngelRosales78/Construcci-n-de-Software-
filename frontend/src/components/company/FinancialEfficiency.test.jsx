import React from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, render, screen, within, fireEvent } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import FinancialPillars from './FinancialPillars'
import api from '../../services/api'
vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }))
afterEach(() => { cleanup(); vi.resetAllMocks() })

test('eficiencia muestra unidades, serie disponible y No aplica distinto de cero', async () => {
  const period = { start: '2023-07-01', end: '2024-06-30' }
  const turnover = { id: 'asset_turnover', label: 'Rotación de activos', status: 'available',
    display_value: '1,33', unit: 'times', period, formula: 'Ingresos / Activos promedio', components: [] }
  const days = { id: 'inventory_days', label: 'Días de inventario', status: 'not_applicable',
    unit: 'days', period, formula: 'Inventario promedio / Costo × 365', components: [],
    explanation: 'Actividad de servicios sin inventario según clasificación documentada.' }
  api.get.mockResolvedValue({ data: { fiscal_year: 2024, period, available_years: [2024, 2022],
    pillars: [{ id: 'efficiency', label: 'Eficiencia', metrics: [turnover, days], history: [
      { fiscal_year: 2024, period, metrics: [turnover, days] },
      { fiscal_year: 2022, period: { end: '2022-06-30' }, metrics: [
        { ...turnover, status: 'unavailable' }, { ...days, status: 'available', display_value: '0,00' }] },
    ] }] } })
  render(<MemoryRouter initialEntries={['/company/MSFT']}><Routes><Route path="/company/:ticker" element={<FinancialPillars />} /></Routes></MemoryRouter>)
  fireEvent.click(await screen.findByRole('button', { name: 'Mostrar Eficiencia' }))
  await screen.findByRole('article', { name: 'Rotación de activos' })
  expect(within(screen.getByRole('article', { name: 'Días de inventario' })).getByText('No aplica')).toBeTruthy()
  const table = screen.getByRole('table', { name: 'Eficiencia en hasta tres ejercicios disponibles' })
  expect(within(table).getByText('0,00 días')).toBeTruthy()
  expect(within(table).queryByText('2023')).toBeNull()
  expect(within(table).getAllByRole('row')).toHaveLength(3)
})
