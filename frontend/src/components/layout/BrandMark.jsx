export default function BrandMark({ className = '' }) {
  return <svg className={`brand-mark ${className}`} viewBox="0 0 40 40" width="40" height="40" aria-hidden="true">
    <path d="M4 4h32v32H4z" fill="none" stroke="currentColor" strokeWidth="1.5" />
    <path d="M12 29V12h17M12 20h12" fill="none" stroke="currentColor" strokeWidth="3" />
    <path d="M27 27h5v5h-5z" fill="var(--brand-mark-accent, currentColor)" />
  </svg>
}
