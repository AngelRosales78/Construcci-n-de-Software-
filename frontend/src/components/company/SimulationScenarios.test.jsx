import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SimulationScenarios from './SimulationScenarios'
import { simulationRepository } from '../../models/simulationRepository'

vi.mock('../../models/simulationRepository', () => ({ simulationRepository: { create: vi.fn() } }))
vi.mock('highcharts-react-official', () => ({ default: ({ options }) => <div data-testid="scenario-chart">{options.series.filter(row => row.visible).map(row => row.name).join(', ')}</div> }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
const point = { date: '2027-10-12', price: 123.45 }
const result = { status: 'available', currency: 'USD', as_of: '2026-10-12', horizon_months: 12, paths: 2000,
  scenarios: { base: [point], optimistic: [point], pessimistic: [point] },
  final_prices: { base: point, optimistic: point, pessimistic: point },
  sources: ['Fixture identificada'], parameters: { annual_return: .1, annual_volatility: .2 } }
const mount = () => render(<MemoryRouter><SimulationScenarios /></MemoryRouter>)

test('la leyenda oculta curvas sin solicitar otra simulación y conserva la tabla final', async () => {
  simulationRepository.create.mockResolvedValue(result); mount()
  fireEvent.click(screen.getByRole('button', { name: 'Generar escenarios' }))
  await screen.findByRole('table')
  fireEvent.click(screen.getByLabelText('Optimista · percentil 90'))
  expect(screen.getByTestId('scenario-chart').textContent).not.toContain('Optimista')
  expect(simulationRepository.create).toHaveBeenCalledTimes(1)
  expect(screen.getAllByRole('row')).toHaveLength(4)
})

test('los controles envían porcentajes como tasas y restablecer recupera los parámetros históricos', async () => {
  simulationRepository.create.mockResolvedValue(result); mount()
  fireEvent.change(screen.getByLabelText('Tasa anual del modelo'), { target: { value: '15' } })
  fireEvent.change(screen.getByLabelText('Volatilidad anual'), { target: { value: '0' } })
  fireEvent.click(screen.getByRole('button', { name: 'Generar escenarios' }))
  await screen.findByRole('table')
  expect(simulationRepository.create.mock.calls[0][1]).toEqual({ seed: 2026, annual_return: .15, annual_volatility: 0 })
  fireEvent.click(screen.getByRole('button', { name: 'Restablecer supuestos históricos' }))
  await waitFor(() => expect(simulationRepository.create).toHaveBeenCalledTimes(2))
  expect(simulationRepository.create.mock.calls[1][1]).toEqual({ seed: 2026 })
})

test('una serie insuficiente muestra el motivo y no dibuja curvas inventadas', async () => {
  simulationRepository.create.mockResolvedValue({ status: 'unavailable', available_observations: 0 }); mount()
  fireEvent.click(screen.getByRole('button', { name: 'Generar escenarios' }))
  await screen.findByText(/No se dispone de un histórico válido/)
  expect(screen.queryByTestId('scenario-chart')).toBeNull()
})
