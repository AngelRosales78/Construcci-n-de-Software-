import React from 'react'
import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, cleanup } from '@testing-library/react'
import AltmanZGauge from './AltmanZGauge'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  PieChart: ({ children }) => <div>{children}</div>,
  Pie: ({ children, data }) => <div data-testid="segments" data-values={JSON.stringify(data)}>{children}</div>,
  Cell: () => null,
}))
afterEach(cleanup)
it.each([null, undefined, NaN, Infinity])('muestra datos insuficientes para %s', score => {
  render(<AltmanZGauge score={score} zone="unknown" />)
  expect(screen.getByText('No hay datos suficientes para calcular el indicador.')).toBeTruthy()
  expect(screen.queryByTestId('segments')).toBeNull()
})
it.each([-5.9, 0, 1.81, 2.99, 8])('conserva el puntaje %s y limita la escala visual', score => {
  render(<AltmanZGauge score={score} zone="distress" />)
  const data = JSON.parse(screen.getByTestId('segments').getAttribute('data-values'))
  expect(data[0].value + data[1].value).toBe(5)
  expect(data.every(row => row.value >= 0)).toBe(true)
  expect(screen.getAllByText(String(score)).length).toBeGreaterThan(0)
})
