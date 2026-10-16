import React from 'react'
import axios, { AxiosError } from 'axios'
import { act, cleanup, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, expect, test, vi } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import ProtectedRoute from '../components/auth/ProtectedRoute'
import api from '../services/api'
import { clearSession, saveSession } from '../services/sessionStore'

const adapter = api.defaults.adapter
beforeEach(() => localStorage.clear())
afterEach(() => { cleanup(); vi.restoreAllMocks(); api.defaults.adapter = adapter; localStorage.clear() })
const session = { access: 'access-one', refresh: 'refresh-one', user: { username: 'user' } }
function openPrivatePage(element = <h1>Contenido privado</h1>) {
  render(<MemoryRouter initialEntries={['/private']}><Routes>
    <Route path="/login" element={<h1>Acceso público</h1>} />
    <Route element={<ProtectedRoute />}><Route path="/private" element={element} /></Route>
  </Routes></MemoryRouter>)
}
function rejectExpiredAccess(config) {
  return Promise.reject(new AxiosError('expired', 'ERR_BAD_REQUEST', config, null,
    { status: 401, data: { detail: 'expired' }, headers: {}, config }))
}

test('una ruta privada exige una sesión aunque se abra directamente', async () => {
  openPrivatePage()
  expect(await screen.findByText('Acceso público')).toBeTruthy()
  expect(screen.queryByText('Contenido privado')).toBeNull()
})
test('cerrar en otra pestaña retira el contenido privado sin recargar', async () => {
  saveSession(session); openPrivatePage()
  expect(screen.getByText('Contenido privado')).toBeTruthy()
  act(() => {
    localStorage.clear()
    window.dispatchEvent(new StorageEvent('storage', { key: null, storageArea: localStorage }))
  })
  expect(await screen.findByText('Acceso público')).toBeTruthy()
  expect(screen.queryByText('Contenido privado')).toBeNull()
})
test('el cierre en la misma pestaña también actualiza la protección', async () => {
  saveSession(session); openPrivatePage()
  act(clearSession)
  expect(await screen.findByText('Acceso público')).toBeTruthy()
})
test('cambiar de cuenta reinicia el estado privado de la pantalla anterior', async () => {
  function PrivateForm() {
    const [owner] = React.useState(() => JSON.parse(localStorage.getItem('user')).username)
    return <h1>Formulario de {owner}</h1>
  }
  saveSession(session); openPrivatePage(<PrivateForm />)
  expect(screen.getByText('Formulario de user')).toBeTruthy()
  act(() => saveSession({ access: 'other-access', refresh: 'other-refresh', user: { username: 'other' } }))
  expect(await screen.findByText('Formulario de other')).toBeTruthy()
  expect(screen.queryByText('Formulario de user')).toBeNull()
})
