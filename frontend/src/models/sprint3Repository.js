import api from '../services/api'

export function currentOwner() {
  try { return String(JSON.parse(localStorage.getItem('user'))?.id || '') } catch { return '' }
}

export function errorMessage(error) {
  const data = error.response?.data
  const detail = data?.detail || Object.values(data && typeof data === 'object' ? data : {}).flat().find(value => typeof value === 'string')
  return typeof detail === 'string' ? detail : error.response?.status === 409
    ? 'Los datos cambiaron. Actualiza la información antes de guardar.'
    : 'No se pudo completar la solicitud. Comprueba la conexión y reintenta.'
}

function owned(data) {
  if (data?.owner_id && currentOwner() && data.owner_id !== currentOwner()) throw new Error('Respuesta de otra cuenta.')
  return data
}

export const sprint3Repository = {
  async get(path, signal) { return owned((await api.get(path, { signal, timeout: 180000 })).data) },
  async send(method, path, data) { return owned((await api[method](path, data, { timeout: 180000 })).data) },
  async pdf(job) {
    const response = await api.get(`/reports/jobs/${encodeURIComponent(job.id)}/file/`, { responseType: 'arraybuffer' })
    const bytes = new Uint8Array(response.data)
    const hash = Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)), x => x.toString(16).padStart(2, '0')).join('')
    if (!String(response.headers['content-type']).includes('application/pdf') || bytes.length !== job.size_bytes
      || new TextDecoder().decode(bytes.slice(0, 5)) !== '%PDF-' || hash !== job.content_sha256
      || !new TextDecoder().decode(bytes.slice(-1024)).includes('%%EOF')) throw new Error('El PDF no coincide con el archivo anunciado.')
    return new Blob([bytes], { type: 'application/pdf' })
  },
}
