import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import api from '../../services/api'
import ComparisonPage from './ComparisonPage'

vi.mock('../../services/api', () => ({ default: { get: vi.fn() } }))
const access = plan => ({ data: { schema_version: 'subscription-v1', plan, active: true,
  owner_id: '1', features: plan === 'pro' ? ['compare'] : [], valid_until: new Date(Date.now()+300000).toISOString() } })
const payload = (a, b, year = 2024) => ({ data: { schema_version: 'comparison-v1', fiscal_year: year,
  a: { schema_version: 'pillars-v1', ticker: a, fiscal_year: year, period: { start: '2024-01-01', end: '2024-12-31' }, available_years: [2024], pillars: [] },
  b: { schema_version: 'pillars-v1', ticker: b, fiscal_year: year, period: { start: '2024-01-01', end: '2024-12-31' }, available_years: [2024], pillars: [] }, comparisons: [] } })
function Location() { const value = useLocation(); return <output aria-label="Dirección">{value.search}</output> }
function mount(url = '/compare') { return render(<MemoryRouter initialEntries={[url]}><ComparisonPage /><Location /></MemoryRouter>) }
beforeEach(() => {
  vi.resetAllMocks()
  api.get.mockImplementation(url => {
    if (url === '/user/subscription/') return Promise.resolve(access('pro'))
    if (url.startsWith('/compare/')) {
      const params = new URLSearchParams(url.split('?')[1])
      return Promise.resolve(payload(params.get('a'), params.get('b'), Number(params.get('fiscal_year'))))
    }
    return Promise.resolve({ data: { results: [{ ticker: 'AAA', name: 'Empresa Alfa' }, { ticker: 'BBB', name: 'Empresa Beta' }] } })
  })
})
afterEach(cleanup)

it('restaura selección desde URL, consulta el ejercicio e intercambia y quita empresas', async () => {
  mount('/compare?a=AAA&b=BBB&fiscal_year=2024')
  await screen.findByRole('heading', { name: 'AAA y BBB · FY2024' })
  expect(api.get).toHaveBeenCalledWith('/compare/?a=AAA&b=BBB&fiscal_year=2024')
  fireEvent.click(screen.getByRole('button', { name: 'Intercambiar A y B' }))
  await screen.findByRole('heading', { name: 'BBB y AAA · FY2024' })
  expect(screen.getByLabelText('Dirección').textContent).toContain('a=BBB&b=AAA')
  fireEvent.click(screen.getByRole('button', { name: 'Quitar empresa A' }))
  await waitFor(() => expect(screen.queryByRole('region', { name: 'Resultado de la selección' })).toBeNull())
  expect(screen.getByLabelText('Dirección').textContent).not.toContain('a=')
})

it('ofrece vista previa Free sin consultar los datos Pro', async () => {
  api.get.mockResolvedValue(access('free'))
  mount('/compare?a=AAA&b=BBB&fiscal_year=2024')
  await screen.findByRole('heading', { name: 'Vista previa · Plan Free' })
  expect(api.get.mock.calls.some(([url]) => url.startsWith('/compare/'))).toBe(false)
})

it('permite búsqueda con teclado y rechaza una empresa repetida', async () => {
  mount('/compare?a=AAA&fiscal_year=2024')
  const input = screen.getByRole('combobox', { name: 'Buscar empresa B' })
  fireEvent.change(input, { target: { value: 'empresa' } })
  await screen.findByRole('option', { name: 'AAA Empresa Alfa' })
  fireEvent.keyDown(input, { key: 'ArrowDown' }); fireEvent.keyDown(input, { key: 'Enter' })
  expect((await screen.findByRole('alert')).textContent).toContain('dos empresas distintas')
  fireEvent.change(input, { target: { value: 'empresa' } })
  await screen.findByRole('option', { name: 'BBB Empresa Beta' })
  fireEvent.keyDown(input, { key: 'ArrowDown' }); fireEvent.keyDown(input, { key: 'ArrowDown' }); fireEvent.keyDown(input, { key: 'Enter' })
  await screen.findByRole('heading', { name: 'AAA y BBB · FY2024' })
})

it('descarta una consulta anterior al cambiar la selección', async () => {
  let finishOld
  api.get.mockImplementation(url => url === '/user/subscription/' ? Promise.resolve(access('pro'))
    : url.includes('a=AAA') ? new Promise(resolve => { finishOld = resolve }) : Promise.resolve(payload('BBB', 'AAA')))
  mount('/compare?a=AAA&b=BBB&fiscal_year=2024')
  await waitFor(() => expect(finishOld).toBeTypeOf('function'))
  fireEvent.click(screen.getByRole('button', { name: 'Intercambiar A y B' }))
  await screen.findByRole('heading', { name: 'BBB y AAA · FY2024' })
  await act(async () => finishOld(payload('AAA', 'BBB')))
  expect(screen.queryByRole('heading', { name: 'AAA y BBB · FY2024' })).toBeNull()
})

it('conserva la URL ante un fallo y reintenta sin cambiar empresas', async () => {
  let failing = true
  api.get.mockImplementation(url => url === '/user/subscription/' ? Promise.resolve(access('pro'))
    : failing ? Promise.reject(new Error('desconectado')) : Promise.resolve(payload('AAA', 'BBB')))
  mount('/compare?a=AAA&b=BBB&fiscal_year=2024')
  await screen.findByRole('alert')
  expect(screen.getByLabelText('Dirección').textContent).toContain('a=AAA&b=BBB')
  failing = false
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar consulta' }))
  await screen.findByRole('heading', { name: 'AAA y BBB · FY2024' })
})

it('no sustituye un año inválido ni acepta datos de otra selección', async () => {
  mount('/compare?a=AAA&b=BBB&fiscal_year=3000')
  await screen.findByText('Plan Pro activo · Acceso verificado por el servidor')
  expect((await screen.findByRole('alert')).textContent).toContain('fechas futuras')
  expect(api.get.mock.calls.some(([url]) => url.startsWith('/compare/'))).toBe(false)
  api.get.mockImplementation(url => Promise.resolve(url === '/user/subscription/' ? access('pro') : payload('WRONG', 'BBB')))
  fireEvent.change(screen.getByLabelText('Ejercicio fiscal de cierre'), { target: { value: '2024' } })
  expect((await screen.findByRole('alert')).textContent).toContain('No se pudo consultar')
})
