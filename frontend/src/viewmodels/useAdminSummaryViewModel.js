import { useEffect, useState } from 'react'
import { adminRepository } from '../models/adminRepository'

export function useAdminSummaryViewModel() {
  const [summary, setSummary] = useState(null)
  const [error, setError] = useState('')
  useEffect(() => {
    let mounted = true
    adminRepository.summary().then(data => mounted && setSummary(data))
      .catch(() => mounted && setError('Se necesita una cuenta de administrador para acceder.'))
    return () => { mounted = false }
  }, [])
  return { summary, error }
}
