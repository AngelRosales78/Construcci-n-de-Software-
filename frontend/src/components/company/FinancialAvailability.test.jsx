import { afterEach, expect, it } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import MetricCards from './MetricCards'
import FinancialHealthIndicator from './FinancialHealthIndicator'
afterEach(cleanup)
it('explica ausencias con motivos concretos y conserva el cero real', () => {
  render(<MetricCards company={{roe:0,beta:null,indicator_availability:{beta:{label:'Histórico insuficiente',explanation:'Faltan sesiones comunes con el índice.'}}}} />)
  expect(screen.getByText('Histórico insuficiente')).toBeTruthy()
  expect(screen.getByText('Faltan sesiones comunes con el índice.').closest('details')).toBeNull()
  expect(screen.getByText('0%')).toBeTruthy()
})
it('identifica el componente pendiente sin asignar una zona favorable', () => {
  render(<FinancialHealthIndicator altmanZ={{score:null,zone:'unknown',availability:{label:'Valoración del cierre pendiente',explanation:'Falta verificar el valor bursátil del cierre.'}}} />)
  expect(screen.getByText('Valoración del cierre pendiente')).toBeTruthy()
  expect(screen.getByText('Falta verificar el valor bursátil del cierre.')).toBeTruthy()
  expect(screen.queryByText('Zona favorable según Altman-Z')).toBeNull()
})
