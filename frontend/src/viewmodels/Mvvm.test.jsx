import React from 'react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { companyRepository } from '../models/companyRepository'
import { useCompanySearchViewModel } from './useCompanySearchViewModel'
import { useCompanyDashboardViewModel } from './useCompanyDashboardViewModel'

vi.mock('../models/companyRepository', () => ({ companyRepository: { search: vi.fn(), summary: vi.fn() } }))
vi.mock('../models/historyRepository', () => ({ historyRepository: { save: vi.fn() } }))
beforeEach(() => vi.resetAllMocks())
afterEach(cleanup)
const searchWrapper = ({ children }) => <MemoryRouter>{children}</MemoryRouter>

test('el ViewModel consulta el Model y expone estado y acciones sin renderizar una pantalla', async () => {
  companyRepository.search.mockResolvedValue({ results: [{ ticker: 'MSFT' }], count: 25, next: 'page2' })
  const { result } = renderHook(() => useCompanySearchViewModel(), { wrapper: searchWrapper })
  await waitFor(() => expect(result.current.loading).toBe(false))
  expect(result.current.companies[0].ticker).toBe('MSFT')
  act(() => result.current.nextPage())
  await waitFor(() => expect(companyRepository.search.mock.lastCall[0].get('page')).toBe('2'))
  act(() => result.current.handleFilterChange({ sector: 'technology', ordering: 'market_cap' }))
  await waitFor(() => expect(result.current.params.page).toBe(1))
  expect(result.current.params.sector).toBe('technology')
})

test('una respuesta anterior no sobrescribe la búsqueda más reciente', async () => {
  let resolveOld
  companyRepository.search.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
  companyRepository.search.mockResolvedValue({ results: [{ ticker: 'AAPL' }], count: 1 })
  const { result } = renderHook(() => useCompanySearchViewModel(), { wrapper: searchWrapper })
  act(() => result.current.handleSearch('AAPL'))
  await waitFor(() => expect(result.current.companies[0]?.ticker).toBe('AAPL'))
  await act(async () => resolveOld({ results: [{ ticker: 'MSFT' }], count: 1 }))
  expect(result.current.companies[0].ticker).toBe('AAPL')
})

test('el ViewModel de ficha expone un error del Model y termina el estado de carga', async () => {
  companyRepository.summary.mockRejectedValue({ response: { data: { error: 'Empresa no encontrada.' } } })
  const wrapper = ({ children }) => <MemoryRouter initialEntries={['/company/UNKNOWN']}><Routes><Route path="/company/:ticker" element={children} /></Routes></MemoryRouter>
  const { result } = renderHook(() => useCompanyDashboardViewModel(), { wrapper })
  await waitFor(() => expect(result.current.loading).toBe(false))
  expect(result.current.company).toBeNull()
  expect(result.current.error).toBe('Empresa no encontrada.')
  expect(companyRepository.summary).toHaveBeenCalledWith('UNKNOWN')
})
