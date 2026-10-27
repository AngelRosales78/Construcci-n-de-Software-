import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { simulationRepository } from '../models/simulationRepository'

export function useSimulationViewModel() {
  const { ticker } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [draft, setDraft] = useState({ annual_return: '', annual_volatility: '' })
  const request = useRef(0)
  useEffect(() => {
    request.current++
    setData(null); setError(''); setLoading(false); setDraft({ annual_return: '', annual_volatility: '' })
    return () => { request.current++ }
  }, [ticker])
  const simulate = async (restore = false) => {
    const parameters = { seed: 2026 }
    if (!restore) {
      for (const [key, value] of Object.entries(draft)) {
        if (value !== '') parameters[key] = Number(value) / 100
      }
      if (Object.entries(parameters).some(([key, value]) => !Number.isFinite(value) ||
          (key === 'annual_return' && (value < -1 || value > 1.5)) ||
          (key === 'annual_volatility' && (value < 0 || value > 2)))) {
        setError('Revisa los límites: tasa de -100 a 150 % y volatilidad de 0 a 200 %.'); return
      }
    }
    if (restore) setDraft({ annual_return: '', annual_volatility: '' })
    const id = ++request.current
    setLoading(true); setError('')
    try {
      const result = await simulationRepository.create(ticker, parameters)
      if (id === request.current) setData(result)
    } catch {
      if (id === request.current) setError('No se pudo generar la simulación. Puedes reintentar; se conserva el último resultado válido.')
    } finally { if (id === request.current) setLoading(false) }
  }
  return { data, loading, error, simulate, draft, update: (key, value) => setDraft(previous => ({ ...previous, [key]: value })) }
}
