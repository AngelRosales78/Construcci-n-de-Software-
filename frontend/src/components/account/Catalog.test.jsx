import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AdminCompaniesPage from './AdminCompaniesPage'
import CompanyAbout from '../company/CompanyAbout'
import api from '../../services/api'
import { saveSession } from '../../services/sessionStore'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), patch: vi.fn(), post: vi.fn() } }))
vi.mock('../layout/WorkspaceLayout', () => ({ default: ({ children }) => <main>{children}</main> }))
const company = { id: 3, ticker: 'MSFT', name: 'Microsoft', sector: 'technology', description: '', website_url: '', is_active: true, is_pilot: true,
  coverage: { summary_fields: 1, summary_total: 5, summary_as_of: '2024-12-31', fiscal_years: [2024], price_as_of: null, sources: ['SEC FY2024'], field_sources: {} } }
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear()
  saveSession({ access: 'access', refresh: 'refresh', user: { id: 9, role: 'admin' } })
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open','') }
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open') }
  api.get.mockImplementation(path => Promise.resolve({ data: path.includes('/companies/sectors/')
    ? { sectors: [{ value: 'technology', label: 'Technology' }, { value: 'energy', label: 'Energy' }] }
    : { count: 1, results: [company], pilot: { selected: 15, target: 15 } } }))
  api.patch.mockResolvedValue({ data: company })
})
afterEach(() => { cleanup(); localStorage.clear() })
const open = () => render(<MemoryRouter><AdminCompaniesPage /></MemoryRouter>)

it('distingue piloto y cobertura sin inventar precios ni fuentes', async () => {
  open()
  await screen.findByText('Microsoft · MSFT')
  expect(screen.getByText('Precios hasta: Sin precios')).toBeTruthy()
  expect(screen.getByText(/Resumen: 1\/5 indicadores/)).toBeTruthy()
  expect(screen.getByText(/no significa tener indicadores completos/)).toBeTruthy()
  fireEvent.change(screen.getByLabelText('Listado'), { target: { value: 'pilot' } })
  await waitFor(() => expect(api.get.mock.calls.some(([path]) => path.includes('scope=pilot') && path.includes('page=1'))).toBe(true))
})

it('confirma la desactivación y cancelar no envía cambios', async () => {
  open()
  fireEvent.click(await screen.findByText('Desactivar MSFT'))
  expect(api.patch).not.toHaveBeenCalled()
  expect(screen.getByText(/Su ficha, favoritos y datos históricos se conservarán/)).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Volver', exact: true }))
  expect(api.patch).not.toHaveBeenCalled()
  fireEvent.click(screen.getByText('Desactivar MSFT'))
  fireEvent.click(screen.getByText('Guardar empresa'))
  await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/admin/companies/3/', { is_active: false }))
  await screen.findByText('Empresa guardada. Los buscadores usan el catálogo actualizado.')
})

it('edita únicamente campos descriptivos y ordena los sectores en español', async () => {
  open()
  fireEvent.click(await screen.findByText('Editar MSFT'))
  fireEvent.change(screen.getByLabelText('Descripción'), { target: { value: 'Empresa de software.' } })
  fireEvent.change(screen.getByLabelText('Sitio web oficial'), { target: { value: 'https://www.microsoft.com' } })
  const select = screen.getByLabelText('Sector')
  expect(Array.from(select.options).map(option => option.text)).toEqual(['Energía','Tecnología'])
  fireEvent.click(screen.getByText('Guardar empresa'))
  await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/admin/companies/3/', {
    description: 'Empresa de software.', website_url: 'https://www.microsoft.com', sector: 'technology' }))
})

it('muestra la información editada y el estado inactivo en la ficha pública', () => {
  render(<CompanyAbout company={{ ...company, is_active: false, description: 'Descripción publicada.', website_url: 'https://www.microsoft.com' }} />)
  expect(screen.getByText('Descripción publicada.')).toBeTruthy()
  expect(screen.getByRole('status').textContent).toContain('no aparece en los buscadores')
  expect(screen.getByRole('link',{name:'Visitar sitio web oficial'}).getAttribute('href')).toBe('https://www.microsoft.com')
})

it('confirma actualización de precios y recupera el mismo identificador tras perder respuesta', async () => {
  api.post.mockRejectedValueOnce(new Error('Red interrumpida')).mockResolvedValueOnce({ data: { operation: {
    id: 'op-1', status: 'succeeded', message: 'Precios actualizados.' } } })
  open()
  fireEvent.click(await screen.findByText('Actualizar precios MSFT'))
  expect(api.post).not.toHaveBeenCalled()
  expect(screen.getByText(/Los balances no se actualizarán/)).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:'Actualizar precios',exact:true}))
  fireEvent.click(await screen.findByText('Recuperar la misma operación'))
  await screen.findByText('Precios actualizados.')
  expect(api.post.mock.calls[0][1].request_id).toBe(api.post.mock.calls[1][1].request_id)
  expect(api.post.mock.calls[0][0]).toBe('/admin/companies/3/refresh/')
})

it('verifica el ticker antes del alta y usa únicamente la verificación confirmada', async () => {
  api.post.mockResolvedValueOnce({ data: { operation: { id: 'verified-1', status: 'ready', ticker: 'NEW',
    message: 'Ticker verificado.', result: { name: 'Empresa verificada', price_count: 3, price_as_of: '2026-11-18', currency: 'USD', source_url: 'https://example.test/prices' } } } })
    .mockResolvedValueOnce({ data: { operation: { id: 'verified-1', status: 'succeeded', message: 'Empresa añadida.' } } })
  open()
  fireEvent.click(screen.getByText('Añadir una empresa'))
  fireEvent.change(screen.getByLabelText('Ticker a verificar'),{target:{value:'new'}})
  await waitFor(() => expect(screen.getByLabelText('Sector de la nueva empresa').options.length).toBe(3))
  fireEvent.change(screen.getByLabelText('Sector de la nueva empresa'),{target:{value:'technology'}})
  fireEvent.click(screen.getByRole('button',{name:'Verificar ticker',exact:true}))
  await screen.findByText('Empresa verificada · NEW')
  expect(api.post).toHaveBeenCalledTimes(1)
  expect(api.post.mock.calls[0][1]).toMatchObject({ticker:'NEW',sector:'technology'})
  expect(screen.getByText(/Todavía no se añadió la empresa/)).toBeTruthy()
  fireEvent.click(screen.getByText('Confirmar alta'))
  expect(api.post).toHaveBeenCalledTimes(1)
  fireEvent.click(screen.getByRole('button',{name:'Añadir al catálogo',exact:true}))
  await screen.findByText('Empresa añadida.')
  expect(api.post.mock.calls[1].slice(0,2)).toEqual(['/admin/companies/',{verification_id:'verified-1'}])
})

it('cambiar el sector invalida una verificación previa sin añadir la empresa', async () => {
  api.post.mockResolvedValue({ data:{ operation:{ id:'verified-2',status:'ready',ticker:'NEW',message:'Verificado.',
    result:{ name:'Empresa verificada',price_count:3,currency:'USD',price_as_of:'2026-11-18',source_url:'https://example.test/prices' } } } })
  open(); fireEvent.click(screen.getByText('Añadir una empresa'))
  fireEvent.change(screen.getByLabelText('Ticker a verificar'),{target:{value:'NEW'}})
  await waitFor(() => expect(screen.getByLabelText('Sector de la nueva empresa').options.length).toBe(3))
  fireEvent.change(screen.getByLabelText('Sector de la nueva empresa'),{target:{value:'technology'}})
  fireEvent.click(screen.getByText('Verificar ticker'))
  await screen.findByText('Confirmar alta')
  fireEvent.change(screen.getByLabelText('Sector de la nueva empresa'),{target:{value:'energy'}})
  expect(screen.queryByText('Confirmar alta')).toBeNull()
  expect(api.post).toHaveBeenCalledTimes(1)
})
