import React from 'react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'
import { useCompanyDashboardViewModel, useFinancialPillarsViewModel } from './useCompanyDashboardViewModel'
import { usePriceChartViewModel } from './usePriceChartViewModel'

vi.mock('../models/companyRepository', () => ({ companyRepository: { summary: vi.fn(), pillars: vi.fn(), prices: vi.fn() } }))
vi.mock('../models/sprint3Repository', () => ({ sprint3Repository: { send: vi.fn() } }))
import { sprint3Repository } from '../models/sprint3Repository'

beforeEach(() => {
  vi.resetAllMocks()
  companyRepository.summary.mockResolvedValue({ ticker: 'MSFT' })
  companyRepository.pillars.mockResolvedValue({ available_years: [2025, 2024] })
  companyRepository.prices.mockResolvedValue({ history: [] })
  sprint3Repository.send.mockResolvedValue({})
})
afterEach(cleanup)

function open(search = '', state = { searchReturn: '/search?q=MSFT&sector=technology&page=2' }) {
  const wrapper = ({ children }) => <MemoryRouter initialEntries={[{ pathname: '/company/MSFT', search, state }]}>
    <Routes><Route path="/company/:ticker" element={children} /></Routes>
  </MemoryRouter>
  return renderHook(() => ({
    company: useCompanyDashboardViewModel(), pillars: useFinancialPillarsViewModel(),
    prices: usePriceChartViewModel(), location: useLocation(),
  }), { wrapper })
}

test('la sección se recupera del enlace y conserva la búsqueda al cambiar ejercicio y rango', async () => {
  const { result } = open('?section=pillars&fiscal_year=2024&range=5Y')
  await waitFor(() => expect(result.current.company.loading).toBe(false))
  expect(result.current.company.section).toBe('pillars')
  act(() => result.current.company.setSection('analysis'))
  act(() => result.current.pillars.setYear('2025'))
  act(() => result.current.prices.setPeriod('6M'))
  const params = new URLSearchParams(result.current.location.search)
  expect(Object.fromEntries(params)).toEqual({ section: 'analysis', fiscal_year: '2025', range: '6M' })
  expect(result.current.company.returnTo).toBe('/search?q=MSFT&sector=technology&page=2')
  expect(result.current.location.state.searchReturn).toBe(result.current.company.returnTo)
  expect(companyRepository.summary).toHaveBeenCalledTimes(1)
  expect(sprint3Repository.send).toHaveBeenCalledTimes(1)
})

test('atrás y adelante recuperan la sección anterior de la ficha', async () => {
  const { result } = open()
  await waitFor(() => expect(result.current.company.loading).toBe(false))
  act(() => result.current.company.setSection('pillars'))
  act(() => result.current.company.setSection('analysis'))
  act(() => result.current.company.navigate(-1))
  await waitFor(() => expect(result.current.company.section).toBe('pillars'))
  act(() => result.current.company.navigate(1))
  await waitFor(() => expect(result.current.company.section).toBe('analysis'))
})

test('un enlace con una sección desconocida muestra el resumen y no permite retornos externos', () => {
  const { result } = open('?section=unknown', { searchReturn: 'https://example.test/' })
  expect(result.current.company.section).toBe('overview')
  expect(result.current.company.returnTo).toBe('/search')
})

test('permite recuperar la ficha tras un error de red sin duplicar la visita', async () => {
  companyRepository.summary.mockRejectedValueOnce(new Error('Offline')).mockResolvedValue({ ticker: 'MSFT' })
  const { result } = open('?section=pillars')
  await waitFor(() => expect(result.current.company.error).toBeTruthy())
  act(() => result.current.company.retryCompany())
  await waitFor(() => expect(result.current.company.company?.ticker).toBe('MSFT'))
  expect(result.current.company.error).toBeNull()
  expect(result.current.company.section).toBe('pillars')
  expect(sprint3Repository.send).toHaveBeenCalledTimes(1)
})
