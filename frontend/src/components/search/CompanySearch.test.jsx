import React from 'react'
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup, act } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import api from '../../services/api'
import CompanySearch from './CompanySearch'
import SearchBar from './SearchBar'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() } }))
const company = (ticker) => ({ ticker, name: `Empresa ${ticker}`, market_cap: 100, sector_display: 'Technology' })
const result = (rows = [], next = null, count = rows.length) => ({ data: { results: rows, next, count } })
let search
beforeEach(() => {
  vi.clearAllMocks()
  search = vi.fn().mockResolvedValue(result([company('AAA')]))
  api.get.mockImplementation(url => {
    if (url.includes('/sectors/')) return Promise.resolve({ data: { sectors: [{ value: 'technology', label: 'Technology' }] } })
    if (url.includes('/autocomplete/')) return Promise.resolve({ data: { suggestions: [] } })
    if (url.includes('/history/')) return Promise.resolve(result([{ id: 1, query: 'MSFT' }]))
    return search(url)
  })
  api.post.mockResolvedValue({ data: {} })
})
afterEach(() => { cleanup(); vi.useRealTimers() })
const mount = () => render(<MemoryRouter><CompanySearch /></MemoryRouter>)

it('muestra resultados e historial paginados sin repetir búsquedas al montar', async () => {
  mount()
  await screen.findByText('Empresa AAA')
  expect(screen.getByRole('button', { name: 'MSFT' })).toBeTruthy()
  expect(search).toHaveBeenCalledTimes(1)
})
it('aplica sector sin texto de búsqueda y permite quitarlo', async () => {
  mount()
  await screen.findByText('Empresa AAA')
  fireEvent.change(screen.getByLabelText('Sector:'), { target: { value: 'technology' } })
  await waitFor(() => expect(search.mock.lastCall[0]).toContain('sector=technology'))
  fireEvent.change(screen.getByLabelText('Sector:'), { target: { value: '' } })
  await waitFor(() => expect(search.mock.lastCall[0]).not.toContain('sector='))
  expect(api.post).not.toHaveBeenCalled()
})
it('cambia orden y reinicia la página conservando el sector', async () => {
  search.mockResolvedValue(result([company('AAA')], 'next', 25))
  mount()
  await screen.findByText('Empresa AAA')
  fireEvent.change(screen.getByLabelText('Sector:'), { target: { value: 'technology' } })
  await waitFor(() => expect(search.mock.lastCall[0]).toContain('sector=technology'))
  await waitFor(() => expect(screen.getByRole('button', { name: 'Siguiente' }).disabled).toBe(false))
  fireEvent.click(screen.getByRole('button', { name: 'Siguiente' }))
  await waitFor(() => expect(search.mock.lastCall[0]).toContain('page=2'))
  fireEvent.change(screen.getByLabelText('Ordenar por:'), { target: { value: 'market_cap' } })
  await waitFor(() => expect(search.mock.lastCall[0]).toContain('ordering=market_cap&page=1&sector=technology'))
})
it('consulta después de 250 ms sin una petición por cada tecla', async () => {
  vi.useFakeTimers()
  const onSearch = vi.fn()
  render(<SearchBar onSearch={onSearch} />)
  const input = screen.getByPlaceholderText('Buscar por ticker o nombre...')
  fireEvent.change(input, { target: { value: 'A' } })
  act(() => vi.advanceTimersByTime(200))
  fireEvent.change(input, { target: { value: 'AA' } })
  act(() => vi.advanceTimersByTime(249))
  expect(onSearch).not.toHaveBeenCalled()
  act(() => vi.advanceTimersByTime(1))
  expect(onSearch).toHaveBeenCalledTimes(1)
  expect(onSearch).toHaveBeenCalledWith('AA')
})
it('selecciona el historial y guarda solamente la búsqueda confirmada', async () => {
  mount()
  await screen.findByRole('button', { name: 'MSFT' })
  fireEvent.click(screen.getByRole('button', { name: 'MSFT' }))
  await waitFor(() => expect(search.mock.lastCall[0]).toContain('q=MSFT'))
  expect(screen.getByPlaceholderText('Buscar por ticker o nombre...').value).toBe('MSFT')
  expect(api.post).toHaveBeenCalledTimes(1)
  expect(api.post).toHaveBeenCalledWith('/companies/history/', { query: 'MSFT' })
})
it('ignora una respuesta antigua que llega después de cambiar el filtro', async () => {
  let resolveOld
  search.mockImplementationOnce(() => new Promise(resolve => { resolveOld = resolve }))
  mount()
  await screen.findByRole('option', { name: 'Tecnología' })
  fireEvent.change(screen.getByLabelText('Sector:'), { target: { value: 'technology' } })
  await screen.findByText('Empresa AAA')
  await act(async () => resolveOld(result([company('OLD')])))
  expect(screen.queryByText('Empresa OLD')).toBeNull()
  expect(screen.getByText('Empresa AAA')).toBeTruthy()
})
it('muestra un error al fallar la consulta', async () => {
  search.mockRejectedValue(new Error('sin conexión'))
  mount()
  expect((await screen.findByRole('alert')).textContent).toContain('No se pudieron cargar')
})
it('muestra un resultado vacío sin fallar', async () => {
  search.mockResolvedValue(result())
  mount()
  await screen.findByText('No se encontraron empresas')
  expect(screen.getByRole('button', { name: 'Siguiente' }).disabled).toBe(true)
})
