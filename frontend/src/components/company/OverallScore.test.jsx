import React from 'react'
import { afterEach, expect, test } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import OverallScore from './OverallScore'
afterEach(cleanup)

test('presenta el redondeo del servidor sin recalcular a partir de notas visibles', () => {
  render(<OverallScore fiscalYear={2024} score={{ status: 'available', value: '50.005', display_value: '50,01',
    version: 'academic-pillar-scores-v1', period: { start: '2023-07-01', end: '2024-06-30' },
    contributions: [{ id: 'growth', label: 'Crecimiento', display_value: '50,01', weight_display: '20 %' }] }} />)
  expect(screen.getByText('50,01', { exact: false, selector: '.overall-score-value' })).toBeTruthy()
  expect(screen.getByText(/distinto de Altman Z/)).toBeTruthy()
  expect(screen.getByText(/Peso 20 %/)).toBeTruthy()
})

test('una nota ausente muestra el pilar pendiente sin publicar promedio parcial', () => {
  render(<OverallScore fiscalYear={2024} score={{ status: 'unavailable', value: null, display_value: 'No disponible',
    missing_pillars: ['growth'], contributions: [{ id: 'growth', label: 'Crecimiento', display_value: 'No disponible', weight_display: '20 %' }],
    explanation: 'Se requieren cinco notas válidas.', version: 'academic-pillar-scores-v1' }} />)
  expect(screen.getByText('No disponible', { selector: '.overall-score-value' })).toBeTruthy()
  expect(screen.getByText('Notas pendientes: Crecimiento.')).toBeTruthy()
})
