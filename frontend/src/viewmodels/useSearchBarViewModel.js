import { useState, useEffect, useRef, useId } from 'react'
import { companyRepository } from '../models/companyRepository'

export function useSearchBarViewModel({ onSearch, onSubmitSearch = onSearch, initialValue = '' }) {
  const [query, setQuery] = useState(initialValue)
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [loading, setLoading] = useState(false)
  const wrapperRef = useRef(null)
  const listId = useId()
  const [activeIndex, setActiveIndex] = useState(-1)

  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        setShowSuggestions(false)
      }
    }
    document.addEventListener('mousedown', handleClickOutside)
    return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => { setQuery(initialValue) }, [initialValue])

  useEffect(() => {
    if (query.trim() === initialValue.trim()) return
    const timer = setTimeout(() => onSearch(query.trim()), 250)
    return () => clearTimeout(timer)
  }, [query, initialValue, onSearch])

  useEffect(() => {
    let active = true
    if (query.length < 1) {
      setSuggestions([])
      setShowSuggestions(false)
      setLoading(false)
      return
    }

    setActiveIndex(-1)
    const fetchSuggestions = async () => {
      setLoading(true)
      try {
        const response = await companyRepository.autocomplete(query)
        if (!active) return
        setSuggestions([...(response.suggestions || [])].sort((a, b) =>
          a.name.localeCompare(b.name, 'es', { sensitivity: 'base' }) || a.ticker.localeCompare(b.ticker)))
        setShowSuggestions(true)
      } catch (err) {
        if (active) setSuggestions([])
      } finally {
        if (active) setLoading(false)
      }
    }
    fetchSuggestions()
    return () => { active = false }
  }, [query])

  const handleSubmit = (e) => {
    e.preventDefault()
    setShowSuggestions(false)
    onSubmitSearch(query.trim())
  }

  const handleSelectSuggestion = (suggestion) => {
    setQuery(suggestion.ticker)
    setShowSuggestions(false)
    onSubmitSearch(suggestion.ticker)
  }

  const handleKeyDown = event => {
    if (event.key === 'Escape') { setShowSuggestions(false); setActiveIndex(-1) }
    if ((event.key === 'ArrowDown' || event.key === 'ArrowUp') && suggestions.length) {
      event.preventDefault()
      setShowSuggestions(true)
      setActiveIndex(index => event.key === 'ArrowDown'
        ? (index + 1) % suggestions.length : (index <= 0 ? suggestions.length - 1 : index - 1))
    }
    if (event.key === 'Enter' && showSuggestions && activeIndex >= 0) {
      event.preventDefault()
      handleSelectSuggestion(suggestions[activeIndex])
    }
  }
  return { query, setQuery, suggestions, showSuggestions, loading, wrapperRef, listId, activeIndex, setShowSuggestions, handleSubmit, handleSelectSuggestion, handleKeyDown }
}
