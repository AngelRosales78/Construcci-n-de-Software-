import { forwardRef, useState } from 'react'

const PasswordInput = forwardRef(function PasswordInput({ visibilityLabel = 'contraseña', ...props }, ref) {
  const [visible, setVisible] = useState(false)
  const explain = props.autoComplete === 'new-password' && visibilityLabel === 'contraseña'
  const description = [props['aria-describedby'], explain ? `${props.id}-requirements` : null].filter(Boolean).join(' ') || undefined
  return <div className="password-field">
    <input {...props} aria-describedby={description} ref={ref} type={visible ? 'text' : 'password'} />
    <button type="button" aria-label={`${visible ? 'Ocultar' : 'Mostrar'} ${visibilityLabel}`} aria-pressed={visible} onClick={() => setVisible(value => !value)}>{visible ? 'Ocultar' : 'Mostrar'}</button>
    {explain && <small id={`${props.id}-requirements`} className="auth-hint">Al menos 8 caracteres, con mayúscula, minúscula, número y símbolo.</small>}
  </div>
})

export default PasswordInput
