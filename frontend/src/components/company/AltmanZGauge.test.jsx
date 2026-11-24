import React from 'react'
import { it, expect, afterEach } from 'vitest'
import { render, screen, cleanup } from '@testing-library/react'
import AltmanZGauge from './AltmanZGauge'

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
  expect(screen.queryByLabelText('Resultado de esta empresa')).toBeNull()
})
it.each([[-5.9,'distress','Atención'], [0,'distress','Atención'], [1.81,'grey','Intermedia'], [2.99,'grey','Intermedia'], [12.7,'safe','Favorable']])('conserva el puntaje %s sin convertirlo en una nota limitada', (score,zone,label) => {
  render(<AltmanZGauge score={score} zone={zone} />)
  expect(screen.getByText(String(score))).toBeTruthy()
  expect(screen.getByText('Mayor que 2,99').closest('details')).toBeNull()
  expect(screen.getByLabelText('Resultado de esta empresa').parentElement.textContent).toContain(label)
  expect(screen.getByText('No es un porcentaje ni una nota sobre 10.')).toBeTruthy()
})
