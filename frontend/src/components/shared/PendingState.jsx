export default function PendingState({ children, className = '' }) {
  return <p role="status" className={`pending-state ${className}`}>
    <span className="pending-mark" aria-hidden="true"><i/><i/><i/></span>
    <span>{children}</span>
  </p>
}
