import Icon from './Icon'

export default function ActionNotice({ message, children }) {
  if (!message) return null
  return <div className="action-notice" role="status" aria-atomic="true" key={message}>
    <span className="action-notice-symbol"><Icon name="check" size={18}/></span>
    <p>{message}{children && <> {children}</>}</p>
  </div>
}
