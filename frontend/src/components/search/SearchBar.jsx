import { useState, useEffect, useRef } from 'react'
import api from '../../services/api'

export default function SearchBar({ onSearch, onSubmitSearch = onSearch, initialValue = '' }) {
  const [query, setQuery] = useState(initialValue)
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [loading, setLoading] = useState(false)
  const wrapperRef = useRef(null)

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
    if (query.length < 2) {
      setSuggestions([])
      setShowSuggestions(false)
      setLoading(false)
      return
    }

    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const response = await api.get(`/companies/autocomplete/?q=${encodeURIComponent(query)}`)
        if (!active) return
        setSuggestions(response.data.suggestions || [])
        setShowSuggestions(true)
      } catch (err) {
        if (active) setSuggestions([])
      } finally {
        if (active) setLoading(false)
      }
    }, 300)

    return () => { active = false; clearTimeout(timer) }
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

  return (
    <div className="search-bar" ref={wrapperRef}>
      <form onSubmit={handleSubmit}>
        <div className="search-input-wrapper">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onFocus={() => suggestions.length > 0 && setShowSuggestions(true)}
            placeholder="Buscar por ticker o nombre..."
            className="search-input"
          />
          {loading && <span className="search-loading">...</span>}
          <button type="submit" className="search-button">Buscar</button>
        </div>
      </form>

      {showSuggestions && suggestions.length > 0 && (
        <ul className="search-suggestions">
          {suggestions.map((s) => (
            <li
              key={s.ticker}
              onClick={() => handleSelectSuggestion(s)}
              className="search-suggestion-item"
            >
              <span className="suggestion-ticker">{s.ticker}</span>
              <span className="suggestion-name">{s.name}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
