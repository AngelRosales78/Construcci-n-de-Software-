import { useEffect, useState } from 'react'
import { useRemoteResource } from './useRemoteResource'
import { currentOwner, errorMessage, sprint3Repository } from '../models/sprint3Repository'

export function useTagsViewModel(onFilter) {
  const [open, setOpen] = useState(false)
  const tags = useRemoteResource('/tags/', open, 2000)
  const empty = { name: '', color: '#158569', id: null }
  const [form, setForm] = useState(empty)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [deleting, setDeleting] = useState(null)
  const [filter, setFilter] = useState('')
  const data = tags.data?.schema_version === 'tags-v1' ? tags.data : null
  useEffect(() => {
    setForm(empty); setDeleting(null); setFilter(''); setPending(false); setError(''); onFilter(null)
  }, [tags.owner, onFilter])
  useEffect(() => {
    if (!data || !filter) return
    if (!data.tags.some(t => String(t.id) === filter)) { setFilter(''); onFilter(null); return }
    onFilter(new Set(data.assignments.filter(a => a.tag_ids.includes(Number(filter))).map(a => a.favorite_id)))
  }, [data, filter, onFilter])
  async function save(method, path, payload) {
    if (pending) return
    const owner = currentOwner()
    setPending(true); setError('')
    try {
      await sprint3Repository.send(method, path, payload)
      if (owner !== currentOwner()) return
      if (method !== 'put') setForm(empty)
      setDeleting(null); tags.reload()
    } catch (failure) { if (owner === currentOwner()) { setError(errorMessage(failure)); tags.reload() } }
    finally { if (owner === currentOwner()) setPending(false) }
  }
  function chooseFilter(value) {
    setFilter(value)
    onFilter(value ? new Set(data.assignments.filter(a => a.tag_ids.includes(Number(value))).map(a => a.favorite_id)) : null)
  }
  return { open, setOpen, tags, form, setForm, pending, error, deleting, setDeleting, filter, data, save, chooseFilter }
}
