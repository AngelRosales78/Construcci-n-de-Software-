import { expect, test } from 'vitest'

const views = import.meta.glob('../components/**/*.jsx', { query: '?raw', import: 'default', eager: true })
const viewModels = import.meta.glob('./use*ViewModel.js', { query: '?raw', import: 'default', eager: true })

test('las vistas no acceden directamente a HTTP, servicios de autenticación o almacenamiento', () => {
  for (const [file, source] of Object.entries(views)) {
    if (file.endsWith('.test.jsx')) continue
    expect(source, file).not.toMatch(/from\s+['"][^'"]*\/(?:services|models)\//)
    expect(source, file).not.toMatch(/\b(?:localStorage|sessionStorage|fetch)\b|\bapi\.(?:get|post|delete|patch)\(/)
  }
})

test('los ViewModels dependen del Model y no importan componentes visuales', () => {
  expect(Object.keys(viewModels)).toHaveLength(12)
  for (const [file, source] of Object.entries(viewModels)) {
    expect(source, file).toContain('../models/')
    expect(source, file).not.toMatch(/from\s+['"][^'"]*components\//)
  }
})
