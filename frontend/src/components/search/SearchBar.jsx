import { useSearchBarViewModel } from '../../viewmodels/useSearchBarViewModel'

export default function SearchBar({ onSearch, onSubmitSearch = onSearch, initialValue = '' }) {
  const { query, setQuery, suggestions, showSuggestions, loading, wrapperRef, listId, activeIndex, setShowSuggestions, handleSubmit, handleSelectSuggestion, handleKeyDown } = useSearchBarViewModel({ onSearch, onSubmitSearch, initialValue })
  const highlight = text => {
    const index = text.toLowerCase().indexOf(query.trim().toLowerCase())
    if (index < 0 || !query.trim()) return text
    return <>{text.slice(0, index)}<mark>{text.slice(index, index + query.trim().length)}</mark>{text.slice(index + query.trim().length)}</>
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
