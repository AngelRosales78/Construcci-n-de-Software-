import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import FinancialAnalysis from './FinancialAnalysis'
import { analysisRepository } from '../../models/analysisRepository'

vi.mock('../../models/analysisRepository', () => ({ analysisRepository: { get: vi.fn() } }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
const claim = { text: 'Explicación de prueba con respaldo.', metric_ids: ['current_ratio'] }
const data = { status: 'available', validated: true, ticker: 'MSFT', fiscal_year: 2024, level: 'simple',
  period: { start: '2023-07-01', end: '2024-06-30' }, generated_at: '2026-10-26T12:00:00Z', sources: [],
  metrics: { current_ratio: { label: 'Liquidez corriente', display_value: '1,27', unit: 'times' } },
  analysis: { summary: { ...claim, text: '<script>texto no ejecutable</script>' },
    pillars: Object.fromEntries(['liquidity', 'profitability', 'solvency', 'efficiency', 'growth'].map(key => [key, claim])), strengths: [], risks: [] } }
const mount = () => render(<MemoryRouter><FinancialAnalysis fiscalYear={2024} /></MemoryRouter>)

test('el resumen se representa como texto y muestra las referencias del servidor', async () => {
  analysisRepository.get.mockResolvedValue(data); mount()
  fireEvent.click(screen.getByRole('button', { name: 'Generar explicación de IA' }))
  await screen.findByText('<script>texto no ejecutable</script>')
  expect(document.querySelector('.ai-panel script')).toBeNull()
  expect(within(screen.getByRole('region', { name: 'Resumen de IA' })).getByText(/Liquidez corriente: 1,27 veces/)).toBeTruthy()
})

test('un fallo al cambiar nivel conserva el análisis válido anterior', async () => {
  analysisRepository.get.mockResolvedValueOnce(data).mockRejectedValueOnce(new Error('Offline')); mount()
  fireEvent.click(screen.getByRole('button', { name: 'Generar explicación de IA' }))
  await screen.findByRole('region', { name: 'Resumen de IA' })
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'technical' } })
  await screen.findByRole('alert')
  expect(screen.getByText('<script>texto no ejecutable</script>')).toBeTruthy()
  expect(analysisRepository.get.mock.calls[1][2]).toBe('technical')
})

test('una respuesta no validada no se publica aunque incluya texto', async () => {
  analysisRepository.get.mockResolvedValue({ ...data, validated: false }); mount()
  fireEvent.click(screen.getByRole('button', { name: 'Generar explicación de IA' }))
  await screen.findByRole('alert')
  expect(screen.queryByRole('region', { name: 'Resumen de IA' })).toBeNull()
})
