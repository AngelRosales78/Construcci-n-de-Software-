import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, render, screen } from '@testing-library/react'
import BeginnerGuide from './BeginnerGuide'
afterEach(cleanup)
it('recorre negocio, cifras y riesgos sin convertir datos ausentes en seguridad', () => {
  const explore=vi.fn()
  render(<BeginnerGuide company={{description:'Actividad documentada.',altman_z:{score:null,zone:'unknown'}}} onExplore={explore} />)
  fireEvent.click(screen.getByRole('button',{name:'Entender esta empresa'}))
  expect(screen.getByRole('heading',{name:'1. Conoce el negocio'})).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:/^Ver cifras/}))
  expect(screen.getByRole('heading',{name:'2. Lee las cifras con contexto'})).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:/^Ver riesgos/}))
  expect(screen.getByText(/Eso no significa que la empresa sea segura/)).toBeTruthy()
  fireEvent.click(screen.getByRole('button',{name:'Explorar salud financiera'}))
  expect(explore).toHaveBeenCalledOnce()
  fireEvent.click(screen.getByRole('button',{name:'Cerrar guía'}))
  expect(screen.queryByLabelText('Recorrido para entender la empresa')).toBeNull()
  expect(document.activeElement).toBe(screen.getByRole('button',{name:'Entender esta empresa'}))
})
