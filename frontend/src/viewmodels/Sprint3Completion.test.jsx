import React from 'react'
import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { act, cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import api from '../services/api'
import { useFavoritesViewModel } from './useFavoritesViewModel'
import { useReportViewModel } from './useReportViewModel'
import { sprint3Repository } from '../models/sprint3Repository'
import { webcrypto } from 'node:crypto'

vi.mock('../services/api', () => ({ default: { get: vi.fn(), post: vi.fn(), delete: vi.fn() } }))
const favorite = { id: 1, ticker: 'MSFT', name: 'Microsoft' }
const page = results => ({ data: { schema_version: 'favorites-v1', owner_id: '1', results } })
beforeEach(() => { vi.resetAllMocks(); localStorage.clear(); sessionStorage.clear(); localStorage.setItem('user', JSON.stringify({ id: 1 })) })
afterEach(() => { cleanup(); vi.useRealTimers(); vi.unstubAllGlobals(); localStorage.clear(); sessionStorage.clear() })

function FavoritesControl() {
  const vm = useFavoritesViewModel()
  return <><p>{vm.items.map(i => i.ticker).join(',') || 'Vacío'}</p><button disabled={!vm.ready} onClick={() => vm.add('MSFT')}>Guardar</button></>
}

it('los favoritos se refrescan en foco y periódicamente sin reiniciar sesión', async () => {
  vi.useFakeTimers()
  api.get.mockResolvedValue(page([]))
  await act(async () => { render(<FavoritesControl />) })
  api.get.mockResolvedValue(page([favorite]))
  await act(async () => { window.dispatchEvent(new Event('focus')) })
  expect(screen.getByText('MSFT')).toBeTruthy()
  api.get.mockResolvedValue(page([]))
  await act(async () => { await vi.advanceTimersByTimeAsync(2100) })
  expect(screen.getByText('Vacío')).toBeTruthy()
})

it('una confirmación tardía de la cuenta anterior no aparece en otra cuenta', async () => {
  api.get.mockResolvedValue(page([]))
  let finish
  api.post.mockImplementation(() => new Promise(resolve => { finish = resolve }))
  render(<FavoritesControl />)
  await waitFor(() => expect(screen.getByRole('button').disabled).toBe(false))
  fireEvent.click(screen.getByRole('button'))
  localStorage.setItem('user', JSON.stringify({ id: 2 }))
  await act(async () => { window.dispatchEvent(new Event('sp500-session-changed')); finish({ data: { schema_version: 'favorites-v1', favorite } }) })
  expect(screen.queryByText('MSFT')).toBeNull()
})

function ReportControl() {
  const vm = useReportViewModel({ a: 'MSFT', b: null, fiscal_year: 2024 })
  const options = { range: '1Y', include_charts: true, include_analysis: false, analysis_level: 'simple', analysis_version: null, analysis_origin: null }
  return <><button onClick={() => vm.submit(options)}>Solicitar</button><button onClick={() => vm.submit(options, true)}>Reintentar</button>
    <p>{vm.error}</p><p>{vm.job?.status}</p></>
}

it('una solicitud PDF incierta se reintenta con el mismo identificador', async () => {
  api.post.mockRejectedValueOnce(new Error('Respuesta perdida')).mockImplementation(async (url, request) => ({
    data: { schema_version: 'report-job-v1', owner_id: '1', id: 'job1', request, status: 'queued' },
  }))
  api.get.mockImplementation(async () => ({ data: { schema_version: 'report-job-v1', owner_id: '1', id: 'job1', request: api.post.mock.calls[1][1], status: 'ready' } }))
  render(<ReportControl />)
  fireEvent.click(screen.getByText('Solicitar'))
  await screen.findByText(/Reintentar conserva el identificador/)
  fireEvent.click(screen.getByText('Solicitar'))
  expect(api.post).toHaveBeenCalledTimes(1)
  fireEvent.click(screen.getByText('Reintentar'))
  await screen.findByText('ready')
  expect(api.post.mock.calls[0][1].request_id).toBe(api.post.mock.calls[1][1].request_id)
})

it('un binario con otro hash no se ofrece como PDF válido', async () => {
  vi.stubGlobal('crypto', webcrypto)
  const bytes = new TextEncoder().encode('%PDF-1.4\nfixture\n%%EOF')
  api.get.mockResolvedValue({ data: bytes.buffer, headers: { 'content-type': 'application/pdf' } })
  await expect(sprint3Repository.pdf({ id: 'job1', size_bytes: bytes.length, content_sha256: '0'.repeat(64) })).rejects.toThrow(/no coincide/)
})
