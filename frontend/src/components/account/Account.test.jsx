import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import ProfilePage from './ProfilePage'
import AdminUsersPage from './AdminUsersPage'
import api from '../../services/api'
import { authService } from '../../services/authService'
import { saveSession } from '../../services/sessionStore'

vi.mock('../../services/api', () => ({ default: { get: vi.fn(), patch: vi.fn(), delete: vi.fn() } }))
vi.mock('../layout/WorkspaceLayout', () => ({ default: ({ children }) => <main>{children}</main> }))
const person = { id: 9, username: 'persona', email: 'persona@example.test', first_name: 'Ana', last_name: '', role: 'user', plan: 'free', is_active: true }
beforeEach(() => {
  vi.resetAllMocks(); localStorage.clear()
  saveSession({ access: 'access', refresh: 'refresh', user: person })
  HTMLDialogElement.prototype.showModal = function () { this.setAttribute('open', '') }
  HTMLDialogElement.prototype.close = function () { this.removeAttribute('open') }
  URL.createObjectURL = vi.fn(() => 'blob:preview')
  URL.revokeObjectURL = vi.fn()
  api.get.mockImplementation(path => Promise.resolve({ data: path.startsWith('/admin/users/') ? { count: 1, results: [person] } : person }))
})
afterEach(() => { cleanup(); localStorage.clear() })
function open(Page) { render(<MemoryRouter><Page /></MemoryRouter>) }

it('confirma el cambio de rol antes de enviar y conserva separado el plan', async () => {
  api.patch.mockResolvedValue({ data: { ...person, role: 'admin' } })
  open(AdminUsersPage)
  fireEvent.change(await screen.findByLabelText('Rol de persona'), { target: { value: 'admin' } })
  expect(api.patch).not.toHaveBeenCalled()
  expect(screen.getByRole('dialog')).toBeTruthy()
  fireEvent.click(screen.getByRole('button', { name: 'Guardar cambio' }))
  await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/admin/users/9/', { role: 'admin' }))
  await screen.findByText('Cambio guardado y registrado.')
})

it('envía el motivo del bloqueo y permite cancelar sin cambios', async () => {
  api.patch.mockResolvedValue({ data: { ...person, is_active: false } })
  open(AdminUsersPage)
  fireEvent.click(await screen.findByText('Bloquear'))
  fireEvent.click(screen.getByRole('button', { name: 'Volver' }))
  expect(api.patch).not.toHaveBeenCalled()
  fireEvent.click(screen.getByText('Bloquear'))
  fireEvent.change(screen.getByLabelText('Motivo del bloqueo'), { target: { value: 'Revisión de prueba' } })
  fireEvent.click(screen.getByText('Guardar cambio'))
  await waitFor(() => expect(api.patch).toHaveBeenCalledWith('/admin/users/9/', { is_active: false, reason: 'Revisión de prueba' }))
})

it('guarda nombre y foto con FormData sin cambiar la identidad de sesión', async () => {
  api.patch.mockResolvedValue({ data: { ...person, first_name: 'Diego', avatar_url: '/avatar/' } })
  const identity = authService.getSessionIdentity()
  open(ProfilePage)
  await waitFor(() => expect(screen.getByLabelText('Nombre').value).toBe('Ana'))
  fireEvent.change(screen.getByLabelText('Nombre'), { target: { value: 'Diego' } })
  const photo = new File(['png'], 'foto.png', { type: 'image/png' })
  fireEvent.change(screen.getByLabelText('Foto de perfil'), { target: { files: [photo] } })
  await screen.findByAltText('Vista previa de la foto de perfil')
  fireEvent.click(screen.getByText('Guardar perfil'))
  await screen.findByText('Perfil guardado.')
  const [, form, options] = api.patch.mock.calls[0]
  expect(form.get('first_name')).toBe('Diego')
  expect(form.get('avatar')).toBe(photo)
  expect(options.headers['Content-Type']).toBeUndefined()
  expect(authService.getSessionIdentity()).toBe(identity)
  expect(JSON.parse(localStorage.getItem('user')).first_name).toBe('Diego')
})

it('descarta la foto previa cuando la siguiente supera el límite', async () => {
  api.patch.mockResolvedValue({ data: person })
  open(ProfilePage)
  const input = await screen.findByLabelText('Foto de perfil')
  fireEvent.change(input, { target: { files: [new File(['png'], 'foto.png')] } })
  await screen.findByAltText('Vista previa de la foto de perfil')
  fireEvent.change(input, { target: { files: [new File([new Uint8Array(2 * 1024 * 1024)], 'grande.png')] } })
  await screen.findByText('La imagen debe ocupar menos de 2 MB.')
  expect(screen.queryByAltText('Vista previa de la foto de perfil')).toBeNull()
  fireEvent.click(screen.getByText('Guardar perfil'))
  await waitFor(() => expect(api.patch).toHaveBeenCalled())
  expect(api.patch.mock.calls[0][1].has('avatar')).toBe(false)
})

it('exige contraseña y confirmación para eliminar únicamente la cuenta actual', async () => {
  api.delete.mockRejectedValue({ response: { status: 403, data: { detail: 'Contraseña incorrecta.' } } })
  open(ProfilePage)
  fireEvent.click(await screen.findByText('Solicitar eliminación'))
  expect(api.delete).not.toHaveBeenCalled()
  fireEvent.click(screen.getByText('Eliminar mi cuenta definitivamente'))
  expect(api.delete).not.toHaveBeenCalled()
  fireEvent.change(screen.getByLabelText('Contraseña actual'), { target: { value: 'incorrecta' } })
  fireEvent.click(screen.getByText('Eliminar mi cuenta definitivamente'))
  await waitFor(() => expect(api.delete).toHaveBeenCalledWith('/auth/account/', { data: { password: 'incorrecta' } }))
  expect(localStorage.getItem('access_token')).toBe('access')
  await waitFor(() => expect(screen.getByLabelText('Contraseña actual').value).toBe(''))
})

it('una respuesta tardía del perfil no modifica otra cuenta', async () => {
  let finish
  api.patch.mockImplementation(() => new Promise(resolve => { finish = resolve }))
  const pending = authService.updateProfile({ first_name: 'Vieja' })
  const rejected = expect(pending).rejects.toThrow('La cuenta cambió')
  saveSession({ access: 'new', refresh: 'new-refresh', user: { id: 20, first_name: 'Actual' } })
  await act(async () => { finish({ data: { id: 9, first_name: 'Vieja' } }) })
  await rejected
  expect(JSON.parse(localStorage.getItem('user')).first_name).toBe('Actual')
})
