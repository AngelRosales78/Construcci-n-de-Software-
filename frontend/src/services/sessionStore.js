// Both browser tabs and the current tab observe the same session lifecycle.
const SESSION_EVENT = 'sp500-session-changed'
const SESSION_KEYS = ['access_token', 'refresh_token', 'user']

export function saveSession(data) {
  localStorage.setItem('access_token', data.access)
  localStorage.setItem('refresh_token', data.refresh)
  localStorage.setItem('user', JSON.stringify(data.user))
  window.dispatchEvent(new Event(SESSION_EVENT))
}

export function clearSession() {
  SESSION_KEYS.forEach(key => localStorage.removeItem(key))
  window.dispatchEvent(new Event(SESSION_EVENT))
}

export function hasSession() {
  return Boolean(localStorage.getItem('access_token') && localStorage.getItem('refresh_token'))
}

export function subscribeSession(listener) {
  const onStorage = event => {
    if ((!event.storageArea || event.storageArea === localStorage) &&
        (event.key === null || SESSION_KEYS.includes(event.key))) listener()
  }
  window.addEventListener('storage', onStorage)
  window.addEventListener(SESSION_EVENT, listener)
  return () => {
    window.removeEventListener('storage', onStorage)
    window.removeEventListener(SESSION_EVENT, listener)
  }
}
