import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { simulationRepository } from '../models/simulationRepository'

export function useSimulationViewModel() {
  const { ticker } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const request = useRef(0)
  useEffect(() => {
    request.current++
    setData(null); setError(''); setLoading(false)
    return () => { request.current++ }
  }, [ticker])
  const simulate = async () => {
    const id = ++request.current
    setLoading(true); setError('')
    try {
      const result = await simulationRepository.create(ticker, { seed: 2026 })
      if (id === request.current) setData(result)
    } catch {
      if (id === request.current) setError('No se pudo generar la simulación. Puedes reintentar; se conserva el último resultado válido.')
    } finally { if (id === request.current) setLoading(false) }
  }
  return { data, loading, error, simulate }
}
