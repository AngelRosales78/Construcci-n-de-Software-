import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import AdminCompaniesPage from './AdminCompaniesPage'
import CompanyAbout from '../company/CompanyAbout'
import api from '../../services/api'
import { saveSession } from '../../services/sessionStore'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), patch: vi.fn() } }))
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
