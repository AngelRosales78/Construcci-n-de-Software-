import React from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, render, screen, within } from '@testing-library/react'
import PillarRadar from './PillarRadar'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  RadarChart: ({ children, data }) => <div data-testid="radar-data" data-values={JSON.stringify(data)}>{children}</div>,
  Radar: () => <div data-testid="radar-polygon" />,
  PolarGrid: () => null, PolarAngleAxis: () => null, PolarRadiusAxis: () => null,
}))
afterEach(cleanup)
const data = () => ({ fiscal_year: 2024, pillars: [
  ['liquidity', 'Liquidez', '0'], ['profitability', 'Rentabilidad', '25'],
  ['solvency', 'Solvencia', '50'], ['efficiency', 'Eficiencia', '75'], ['growth', 'Crecimiento', '100'],
].map(([id, label, value]) => ({ id, label, score: { status: 'available', value,
  display_value: Number(value).toFixed(2).replace('.', ','), parts: [] } })),
scoring_policy: { version: 'academic-pillar-scores-v1', explanation: 'Convención académica del proyecto.' } })

test('radar tiene cinco ejes, conserva nota cero y enlaces a cada detalle', () => {
  render(<PillarRadar data={data()} />)
  expect(screen.getByTestId('radar-polygon')).toBeTruthy()
  const values = JSON.parse(screen.getByTestId('radar-data').getAttribute('data-values'))
  expect(values.map(x => x.value)).toEqual([0, 25, 50, 75, 100])
  const table = screen.getByRole('table', { name: 'Notas de los pilares del ejercicio seleccionado' })
  expect(within(table).getByText('0,00')).toBeTruthy()
  expect(within(table).getByRole('link', { name: 'Crecimiento' }).getAttribute('href')).toBe('#pillar-growth')
})

test('una nota ausente no se convierte en cero ni cierra un polígono falso', () => {
  const missing = data()
  missing.pillars[4].score = { status: 'unavailable', value: null, display_value: 'No disponible',
    explanation: 'Faltan cuatro cierres consecutivos.' }
  render(<PillarRadar data={missing} />)
  expect(screen.queryByTestId('radar-polygon')).toBeNull()
  expect(JSON.parse(screen.getByTestId('radar-data').getAttribute('data-values'))[4].value).toBeNull()
  expect(screen.getByText(/Los datos ausentes no se representan como cero/)).toBeTruthy()
  expect(screen.getByText('Faltan cuatro cierres consecutivos.')).toBeTruthy()
})

test('nota fuera del rango no dibuja un radar engañoso', () => {
  const invalid = data(); invalid.pillars[0].score.value = '101'
  render(<PillarRadar data={invalid} />)
  expect(screen.queryByTestId('radar-polygon')).toBeNull()
})
