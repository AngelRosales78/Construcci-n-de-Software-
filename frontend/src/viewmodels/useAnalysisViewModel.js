import { useState, useEffect, useRef } from 'react'
import { useParams } from 'react-router-dom'
import { analysisRepository } from '../models/analysisRepository'

export function useAnalysisViewModel(fiscalYear) {
  const { ticker } = useParams()
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const request = useRef(0)
  useEffect(() => {
    request.current++; setData(null); setError(''); setLoading(false)
    return () => { request.current++ }
  }, [ticker, fiscalYear])
  const generate = async () => {
    const id = ++request.current
    setLoading(true); setError('')
    try {
      const result = await analysisRepository.get(ticker, fiscalYear, 'simple')
      if (id !== request.current) return
      if (result.status !== 'available' || result.validated !== true) {
        setError(result.message || 'No hay un análisis validado disponible.'); return
      }
      setData(result)
    } catch {
      if (id === request.current) setError('El proveedor no respondió o el análisis no superó la validación. Puedes reintentar. Se conserva el último análisis válido, si existe.')
    } finally { if (id === request.current) setLoading(false) }
  }
  return { data, loading, error, generate }
}
