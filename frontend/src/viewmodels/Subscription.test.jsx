import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import SubscriptionPage from '../components/subscription/SubscriptionPage'
import { subscriptionRepository } from '../models/subscriptionRepository'

vi.mock('../models/subscriptionRepository', () => ({ subscriptionRepository: { load: vi.fn(), checkout: vi.fn(), cancel: vi.fn() } }))
vi.mock('../components/layout/WorkspaceLayout', () => ({ default: ({ children }) => <main>{children}</main> }))
const free = { effective_plan: 'free', active: true, features: [], expires_at: null }
const pro = { effective_plan: 'pro', active: true, features: ['compare'], expires_at: '2099-01-01T00:00:00Z', auto_renew: true }
const plans = { amount: '9.90', currency: 'PEN', duration_days: 30, features: [{ label: 'PDF comparativo', free: false, pro: true }] }
const receipt = { id: 'receipt-1', outcome: 'approved', currency: 'PEN', amount: '9.90', created_at: '2026-11-17T12:00:00Z', starts_at: '2026-11-17T12:00:00Z', ends_at: '2026-12-17T12:00:00Z', email_status: 'pending' }
beforeEach(() => { vi.resetAllMocks(); subscriptionRepository.load.mockResolvedValue({ plans, subscription: free, receipts: [] }) })
afterEach(cleanup)
function open() { render(<MemoryRouter><SubscriptionPage /></MemoryRouter>) }

it('muestra tarifa del servidor y abre checkout desde Elegir Pro', async () => {
  open()
  await screen.findByText('PEN 9.90 simulados por 30 días')
  fireEvent.click(screen.getByText('Elegir Pro'))
  expect(screen.getByRole('button', { name: 'Simular aprobación' })).toBeTruthy()
  expect(screen.getByRole('button', { name: 'Simular rechazo' })).toBeTruthy()
})

it('reintenta el mismo identificador cuando se pierde una respuesta y muestra comprobante imprimible', async () => {
  subscriptionRepository.checkout.mockRejectedValueOnce(new Error('Red')).mockResolvedValueOnce({ receipt, subscription: pro })
  open()
  fireEvent.click(await screen.findByText('Elegir Pro'))
  fireEvent.click(screen.getByText('Simular aprobación'))
  await screen.findByText('Reintentar la misma operación')
  fireEvent.click(screen.getByText('Reintentar la misma operación'))
  await screen.findByText('Simulación aprobada. Tu plan Pro está activo.')
  expect(subscriptionRepository.checkout.mock.calls[0][0]).toEqual(subscriptionRepository.checkout.mock.calls[1][0])
  expect(screen.getByText('receipt-1')).toBeTruthy()
  const print = vi.spyOn(window, 'print').mockImplementation(() => {})
  fireEvent.click(screen.getByText('Imprimir o guardar como PDF'))
  expect(print).toHaveBeenCalledTimes(1)
  print.mockRestore()
})

it('confirma cancelación y conserva Pro vigente con fecha visible', async () => {
  subscriptionRepository.load.mockResolvedValue({ plans, subscription: pro, receipts: [] })
  subscriptionRepository.cancel.mockResolvedValue({ ...pro, auto_renew: false })
  open()
  fireEvent.click(await screen.findByText('Cancelar renovación'))
  expect(subscriptionRepository.cancel).not.toHaveBeenCalled()
  fireEvent.click(screen.getByText('Confirmar cancelación'))
  await waitFor(() => expect(subscriptionRepository.cancel).toHaveBeenCalledTimes(1))
  await screen.findByText('Renovación cancelada. Conservas Pro hasta el vencimiento indicado.')
})

it('cambia a la cuenta actual y oculta historial de la anterior', async () => {
  subscriptionRepository.load.mockResolvedValueOnce({ plans, subscription: pro, receipts: [receipt] })
  open()
  fireEvent.click(await screen.findByText('Ver operación'))
  expect(screen.getByText('receipt-1')).toBeTruthy()
  window.dispatchEvent(new Event('sp500-session-changed'))
  await waitFor(() => expect(screen.queryByText('receipt-1')).toBeNull())
  await screen.findByText('Plan Free')
})

it('conserva el intento tras un error 500 y recupera el mismo comprobante', async () => {
  subscriptionRepository.checkout.mockRejectedValueOnce({ response: { status: 500 } })
    .mockResolvedValueOnce({ receipt, subscription: pro })
  open()
  fireEvent.click(await screen.findByText('Elegir Pro'))
  fireEvent.click(screen.getByText('Simular aprobación'))
  fireEvent.click(await screen.findByText('Reintentar la misma operación'))
  await screen.findByText('receipt-1')
  expect(subscriptionRepository.checkout.mock.calls[0][0]).toEqual(subscriptionRepository.checkout.mock.calls[1][0])
})

it('volver al navegador mientras se procesa el pago conserva su confirmación', async () => {
  let finish
  subscriptionRepository.checkout.mockImplementation(() => new Promise(resolve => { finish = resolve }))
  open()
  fireEvent.click(await screen.findByText('Elegir Pro'))
  fireEvent.click(screen.getByText('Simular aprobación'))
  await act(async () => { window.dispatchEvent(new Event('focus')) })
  await act(async () => { finish({ receipt, subscription: pro }) })
  await screen.findByText('receipt-1')
  expect(screen.getByText('Simulación aprobada. Tu plan Pro está activo.')).toBeTruthy()
})
