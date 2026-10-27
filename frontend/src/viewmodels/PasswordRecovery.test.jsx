import React from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import api from '../services/api'
import ResetPassword from '../components/auth/ResetPassword'

afterEach(() => { cleanup(); vi.restoreAllMocks() })

function openResetLink() {
  render(<MemoryRouter initialEntries={['/reset-password/MTI/signed-reset-token']}>
    <Routes><Route path="/reset-password/:uid/:token" element={<ResetPassword />} /></Routes>
  </MemoryRouter>)
}

test('el enlace del correo abre el formulario y envía el contrato real del backend', async () => {
  const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { message: 'ok' } })
  openResetLink()
  fireEvent.change(screen.getByLabelText('Nueva Contraseña'), { target: { value: 'Nueva!Clave2026' } })
  fireEvent.change(screen.getByLabelText('Confirmar Nueva Contraseña'), { target: { value: 'Nueva!Clave2026' } })
  fireEvent.click(screen.getByRole('button', { name: 'Restablecer Contraseña' }))
  await screen.findByText(/Contraseña restablecida exitosamente/)
  expect(post).toHaveBeenCalledWith('/auth/password/reset/confirm/', {
    token: 'MTI:signed-reset-token', new_password: 'Nueva!Clave2026', new_password_confirm: 'Nueva!Clave2026',
  })
})

test('un enlace vencido muestra el error del servidor sin anunciar éxito', async () => {
  vi.spyOn(api, 'post').mockRejectedValue({ response: { data: { error: 'Invalid or expired token.' } } })
  openResetLink()
  fireEvent.change(screen.getByLabelText('Nueva Contraseña'), { target: { value: 'Nueva!Clave2026' } })
  fireEvent.change(screen.getByLabelText('Confirmar Nueva Contraseña'), { target: { value: 'Nueva!Clave2026' } })
  fireEvent.click(screen.getByRole('button', { name: 'Restablecer Contraseña' }))
  await screen.findByText('Invalid or expired token.')
  expect(screen.queryByText(/Contraseña restablecida exitosamente/)).toBeNull()
})

test('confirmaciones distintas no se envían al servidor', async () => {
  const post = vi.spyOn(api, 'post')
  openResetLink()
  fireEvent.change(screen.getByLabelText('Nueva Contraseña'), { target: { value: 'Nueva!Clave2026' } })
  fireEvent.change(screen.getByLabelText('Confirmar Nueva Contraseña'), { target: { value: 'Otra!Clave2026' } })
  fireEvent.click(screen.getByRole('button', { name: 'Restablecer Contraseña' }))
  await waitFor(() => expect(screen.getByText('Las contraseñas deben coincidir.')).toBeTruthy())
  expect(post).not.toHaveBeenCalled()
})
