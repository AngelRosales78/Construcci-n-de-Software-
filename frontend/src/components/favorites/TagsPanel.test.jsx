import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen } from '@testing-library/react'
import api from '../../services/api'
import TagsPanel from './TagsPanel'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), post: vi.fn(), put: vi.fn() } }))
const onFilter = () => {}
const snapshot = { schema_version: 'tags-v1', owner_id: '1', version: 1,
  tags: [{ id: 2, name: 'Seguimiento', color: '#158569' }], assignments: [{ favorite_id: 1, tag_ids: [] }] }
beforeEach(() => {
  vi.resetAllMocks(); localStorage.setItem('user', JSON.stringify({ id: 1 }))
  api.get.mockResolvedValue({ data: snapshot })
})
afterEach(() => { cleanup(); localStorage.clear() })
async function open() {
  render(<TagsPanel items={[{ id: 1, ticker: 'GOOGL' }]} onFilter={onFilter} />)
  fireEvent.click(screen.getByText('Organizar favoritos con etiquetas'))
  await screen.findByLabelText('Nombre de etiqueta')
}
it('una asignación confirmada conserva el nombre que se estaba escribiendo', async () => {
  let finish
  api.put.mockImplementation(() => new Promise(resolve => { finish = resolve }))
  await open()
  fireEvent.click(screen.getByRole('checkbox', { name: 'Seguimiento' }))
  fireEvent.change(screen.getByLabelText('Nombre de etiqueta'), { target: { value: 'Lista en edición' } })
  await act(async () => { finish({ data: { ...snapshot, version: 2 } }) })
  expect(screen.getByLabelText('Nombre de etiqueta').value).toBe('Lista en edición')
})
it('un nombre duplicado muestra la validación del servidor y conserva el borrador', async () => {
  api.post.mockRejectedValue({ response: { status: 400, data: { name: ['Ya existe una etiqueta equivalente en tu cuenta.'] } } })
  await open()
  fireEvent.change(screen.getByLabelText('Nombre de etiqueta'), { target: { value: 'Seguimiento' } })
  fireEvent.click(screen.getByText('Crear etiqueta'))
  expect(await screen.findByRole('alert')).toHaveProperty('textContent', 'Ya existe una etiqueta equivalente en tu cuenta.')
  expect(screen.getByLabelText('Nombre de etiqueta').value).toBe('Seguimiento')
})
