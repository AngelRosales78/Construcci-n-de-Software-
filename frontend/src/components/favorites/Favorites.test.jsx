import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import api from '../../services/api'
import FavoriteButton from './FavoriteButton'
import FavoritesPage from './FavoritesPage'
import { useFavoritesViewModel } from '../../viewmodels/useFavoritesViewModel'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn() } }))
const favorite = { id: 8, ticker: 'MSFT', name: 'Microsoft' }
const page = rows => ({ data: { schema_version: 'favorites-v1', owner_id: '1', results: rows } })
function Control() {
  const favorites = useFavoritesViewModel()
  return <><FavoriteButton ticker="MSFT" favorites={favorites} />{favorites.error && <p role="alert">{favorites.error}</p>}</>
}
beforeEach(() => { vi.resetAllMocks(); api.get.mockResolvedValue(page([])) })
afterEach(cleanup)

it('espera confirmación real, evita doble clic y conserva el guardado al volver a montar', async () => {
  let finish
  api.post.mockImplementation(() => new Promise(resolve => { finish = resolve }))
  const mounted = render(<Control />)
  const button = screen.getByRole('button', { name: 'Guardar MSFT en favoritos' })
  await waitFor(() => expect(button.disabled).toBe(false))
  fireEvent.click(button); fireEvent.click(button)
  expect(api.post).toHaveBeenCalledTimes(1)
  expect(api.post).toHaveBeenCalledWith('/favorites/', { ticker: 'MSFT' })
  expect(screen.queryByRole('button', { name: 'MSFT guardado en favoritos' })).toBeNull()
  await act(async () => finish({ data: { schema_version: 'favorites-v1', favorite } }))
  expect(screen.getByRole('button', { name: 'MSFT guardado en favoritos' }).disabled).toBe(true)
  mounted.unmount()
  api.get.mockResolvedValue(page([favorite]))
  render(<MemoryRouter><FavoritesPage /></MemoryRouter>)
  expect(await screen.findByText('Microsoft')).toBeTruthy()
  expect(screen.getByRole('link', { name: 'Ver ficha →' }).getAttribute('href')).toBe('/company/MSFT')
})

it('no anuncia un guardado cuando falla el servidor y permite reintentar', async () => {
  api.post.mockRejectedValue(new Error('sin conexión'))
  render(<Control />)
  const button = screen.getByRole('button', { name: 'Guardar MSFT en favoritos' })
  await waitFor(() => expect(button.disabled).toBe(false))
  fireEvent.click(button)
  expect((await screen.findByRole('alert')).textContent).toContain('No se pudo confirmar')
  expect(screen.queryByRole('button', { name: 'MSFT guardado en favoritos' })).toBeNull()
  expect(button.disabled).toBe(false)
})

it('distingue una lista vacía de una consulta fallida', async () => {
  api.get.mockRejectedValue(new Error('servidor no disponible'))
  render(<MemoryRouter><FavoritesPage /></MemoryRouter>)
  await screen.findByRole('alert')
  expect(screen.queryByText(/Aún no guardaste/)).toBeNull()
  api.get.mockResolvedValue(page([]))
  fireEvent.click(screen.getByRole('button', { name: 'Reintentar favoritos' }))
  expect(await screen.findByText(/Aún no guardaste/)).toBeTruthy()
})
