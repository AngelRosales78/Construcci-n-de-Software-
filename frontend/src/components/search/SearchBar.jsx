import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import api from '../../services/api'

export default function SearchBar({ onSearch, initialValue = '' }) {
  const [query, setQuery] = useState(initialValue)
  const [suggestions, setSuggestions] = useState([])
  const [showSuggestions, setShowSuggestions] = useState(false)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
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

  useEffect(() => {
    if (query.length < 2) {
      setSuggestions([])
      return
    }

    const timer = setTimeout(async () => {
      setLoading(true)
      try {
        const response = await api.get(`/companies/autocomplete/?q=${encodeURIComponent(query)}`)
        setSuggestions(response.data.suggestions || [])
        setShowSuggestions(true)
      } catch (err) {
        setSuggestions([])
      } finally {
        setLoading(false)
      }
    }, 300)

    return () => clearTimeout(timer)
  }, [query])

  const handleSubmit = (e) => {
    e.preventDefault()
    if (query.trim()) {
      setShowSuggestions(false)
      onSearch(query.trim())
    }
  }

  const handleSelectSuggestion = (suggestion) => {
    setQuery(suggestion.ticker)
    setShowSuggestions(false)
    onSearch(suggestion.ticker)
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
