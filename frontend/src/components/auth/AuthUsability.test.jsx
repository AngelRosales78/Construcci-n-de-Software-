import { afterEach, expect, test } from 'vitest'
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import Login from './Login'
import Register from './Register'

afterEach(cleanup)

test('los errores se anuncian en español y se asocian al campo', async () => {
  render(<MemoryRouter><Login /></MemoryRouter>)
  fireEvent.click(screen.getByRole('button', { name: 'Iniciar sesión', exact: true }))
  await screen.findByText('Escribe tu nombre de usuario.')
  const input = screen.getByLabelText('Usuario')
  expect(input.getAttribute('aria-invalid')).toBe('true')
  expect(document.getElementById(input.getAttribute('aria-describedby')).textContent).toBe('Escribe tu nombre de usuario.')
  expect(screen.getByText('Escribe tu contraseña.').getAttribute('role')).toBe('alert')
})

test('mostrar y ocultar conserva la contraseña sin enviar el formulario', async () => {
  render(<MemoryRouter><Login /></MemoryRouter>)
  const input = screen.getByLabelText('Contraseña')
  fireEvent.change(input, { target: { value: 'TextoLocal!' } })
  fireEvent.click(screen.getByRole('button', { name: 'Mostrar contraseña', exact: true }))
  expect(input.type).toBe('text')
  expect(input.value).toBe('TextoLocal!')
  fireEvent.click(screen.getByRole('button', { name: 'Ocultar contraseña', exact: true }))
  expect(input.type).toBe('password')
  await waitFor(() => expect(screen.queryByRole('alert')).toBeNull())
})

test('el registro distingue campos opcionales y explica las reglas fuera del placeholder', () => {
  render(<MemoryRouter><Register /></MemoryRouter>)
  expect(screen.getByLabelText('Nombre (opcional)')).toBeTruthy()
  expect(screen.getByLabelText('Apellido (opcional)')).toBeTruthy()
  expect(screen.getByText(/Al menos 8 caracteres/)).toBeTruthy()
  expect(screen.getByText(/no otorga permisos de administrador/)).toBeTruthy()
})
