import React from 'react'
import { describe, it, expect, vi, afterEach } from 'vitest'
import { render, screen, cleanup } from '@testing-library/react'
import AltmanZGauge from './AltmanZGauge'

vi.mock('recharts', () => ({
  ResponsiveContainer: ({ children }) => <div>{children}</div>,
  PieChart: ({ children }) => <div>{children}</div>,
  Pie: ({ children, data }) => <div data-testid="segments" data-values={JSON.stringify(data)}>{children}</div>,
  Cell: () => null,
  Tooltip: () => null,
}))
afterEach(cleanup)

it('muestra la fecha propia y las fuentes del cálculo fiscal sin atribuirle el precio actual', () => {
  render(<AltmanZGauge score={11.5} zone="safe" reference={{period_start:'2025-09-28',period_end:'2026-09-26',price_date:'2026-09-25',currency:'USD',
    unadjusted_close:'341.07',shares_outstanding:'14532259000',inputs:{equity_value:'4956527417130'},method:'Acciones al cierre por precio sin ajustar.',locator:'Balance oficial',
    report_url:'https://www.sec.gov/report',price_url:'https://finance.yahoo.com/history'}} />)
  expect(screen.getByText(/2025-09-28 a 2026-09-26/)).toBeTruthy()
  expect(screen.getByText(/Sesión: 2026-09-25/)).toBeTruthy()
  expect(screen.getByRole('link',{name:'Consultar informe oficial'}).getAttribute('href')).toBe('https://www.sec.gov/report')
})
it.each([null, undefined, NaN, Infinity])('muestra datos insuficientes para %s', score => {
  render(<AltmanZGauge score={score} zone="unknown" />)
  expect(screen.getByText('No hay datos suficientes para calcular el indicador.')).toBeTruthy()
  expect(screen.queryByTestId('segments')).toBeNull()
})
it.each([-5.9, 0, 1.81, 2.99, 8])('conserva el puntaje %s y limita la escala visual', score => {
  render(<AltmanZGauge score={score} zone="distress" />)
  const data = JSON.parse(screen.getByTestId('segments').getAttribute('data-values'))
  expect(data.reduce((total, row) => total + row.value, 0)).toBeCloseTo(5)
  expect(data.map(row => row.name)).toEqual(['Peligro', 'Zona gris', 'Zona segura'])
  expect(data.every(row => row.value >= 0)).toBe(true)
  expect(screen.getAllByText(String(score)).length).toBeGreaterThan(0)
})
