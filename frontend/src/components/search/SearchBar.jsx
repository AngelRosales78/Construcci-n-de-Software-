import { useState, useEffect, useRef, useId } from 'react'
import api from '../../services/api'

export default function SearchBar({ onSearch, onSubmitSearch = onSearch, initialValue = '' }) {
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
    const highlight = text => {
    const index = text.toLowerCase().indexOf(query.trim().toLowerCase())
    if (index < 0 || !query.trim()) return text
    return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.trim().length)}</mark>{text.slice(index + query.trim().length)}</>
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
  return () => document.removeEventListener('mousedown', handleClickOutside)
  }, [])

  useEffect(() => { setQuery(initialValue) }, [initialValue])

  useEffect(() => {
    if (query.trim() === initialValue.trim()) return
    const timer = setTimeout(() => onSearch(query.trim()), 250)
    const highlight = text => {
    const index = text.toLowerCase().indexOf(query.trim().toLowerCase())
    if (index < 0 || !query.trim()) return text
    return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.trim().length)}</mark>{text.slice(index + query.trim().length)}</>
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
        const response = await api.get(`/companies/autocomplete/?q=${encodeURIComponent(query)}`)
        if (!active) return
        setSuggestions(response.data.suggestions || [])
        setShowSuggestions(true)
      } catch (err) {
        if (active) setSuggestions([])
      } finally {
        if (active) setLoading(false)
      }
    }
    fetchSuggestions()
    const highlight = text => {
    const index = text.toLowerCase().indexOf(query.trim().toLowerCase())
    if (index < 0 || !query.trim()) return text
    return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.trim().length)}</mark>{text.slice(index + query.trim().length)}</>
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

  const highlight = text => {
    const index = text.toLowerCase().indexOf(query.trim().toLowerCase())
    if (index < 0 || !query.trim()) return text
    return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.trim().length)}</mark>{text.slice(index + query.trim().length)}</>
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
  return (
    <div className="search-bar" ref={wrapperRef}>
      <form onSubmit={handleSubmit}>
        <div className="search-input-wrapper">
          <input
            type="text"
            role="combobox" aria-label="Buscar empresas" aria-autocomplete="list"
            aria-expanded={showSuggestions && suggestions.length > 0}
            aria-controls={listId}
            aria-activedescendant={activeIndex >= 0 ? `${listId}-${activeIndex}` : undefined}
            onKeyDown={handleKeyDown}
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
        <ul id={listId} role="listbox" className="search-suggestions">
          {suggestions.map((s, index) => (
            <li
              key={s.ticker}
              id={`${listId}-${index}`} role="option" aria-selected={activeIndex === index}
              onClick={() => handleSelectSuggestion(s)}
              className="search-suggestion-item"
            >
              <span className="suggestion-ticker">{highlight(s.ticker)}</span>
              <span className="suggestion-name">{highlight(s.name)}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
