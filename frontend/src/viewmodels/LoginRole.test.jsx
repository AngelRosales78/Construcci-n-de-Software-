import React from 'react'
import { afterEach, expect, test, vi } from 'vitest'
import { act, cleanup, renderHook, waitFor } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { authRepository } from '../models/authRepository'
import { useLoginViewModel } from './useLoginViewModel'

vi.mock('../models/authRepository', () => ({ authRepository: { login: vi.fn() } }))
afterEach(() => { cleanup(); vi.resetAllMocks() })

test.each([['admin', '/admin-panel'], ['user', '/dashboard']])('redirige %s según el rol confirmado por la API', async (role, destination) => {
  authRepository.login.mockResolvedValue({ user: { role } })
  const wrapper = ({ children }) => <MemoryRouter initialEntries={['/login']}>{children}</MemoryRouter>
  const { result } = renderHook(() => ({ login: useLoginViewModel(), location: useLocation() }), { wrapper })
  await act(async () => result.current.login.onSubmit({ username: 'user', password: 'valid' }))
  await waitFor(() => expect(result.current.location.pathname).toBe(destination))
})

test('un error de autenticación conserva la pantalla de acceso', async () => {
  authRepository.login.mockRejectedValue({ response: { data: { error: 'Credenciales inválidas' } } })
  const wrapper = ({ children }) => <MemoryRouter initialEntries={['/login']}>{children}</MemoryRouter>
  const { result } = renderHook(() => ({ login: useLoginViewModel(), location: useLocation() }), { wrapper })
  await act(async () => result.current.login.onSubmit({ username: 'user', password: 'wrong' }))
  expect(result.current.location.pathname).toBe('/login')
  expect(result.current.login.error).toBe('Credenciales inválidas')
})
