import React from 'react'
import { it, expect, vi, afterEach } from 'vitest'
import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import SearchHistory from './SearchHistory'
import SearchBar from './SearchBar'
import api from '../../services/api'
vi.mock('../../services/api',()=>({default:{get:vi.fn(),delete:vi.fn()}}))
afterEach(()=>{cleanup();localStorage.clear();vi.resetAllMocks()})
it('selecciona sugerencias con flechas y Enter y resalta coincidencias',async()=>{
 api.get.mockResolvedValue({data:{suggestions:[{ticker:'AAPL',name:'Apple Inc.'}]}})
 const submit=vi.fn()
 render(<SearchBar onSearch={vi.fn()} onSubmitSearch={submit}/> )
 const input=screen.getByRole('combobox')
 fireEvent.change(input,{target:{value:'AA'}})
 await screen.findByRole('option')
 fireEvent.keyDown(input,{key:'ArrowDown'})
 expect(screen.getByRole('option').getAttribute('aria-selected')).toBe('true')
 expect(screen.getByText('AA',{selector:'mark'})).toBeTruthy()
 fireEvent.keyDown(input,{key:'Enter'})
 expect(submit).toHaveBeenCalledWith('AAPL')
})
it('separa el historial local por cuenta y borra un elemento',async()=>{
 localStorage.setItem('user',JSON.stringify({id:7}))
 api.get.mockResolvedValue({data:{results:[{id:1,query:'AAPL'}]}})
 api.delete.mockResolvedValue({})
 render(<SearchHistory onSelectHistory={vi.fn()}/> )
 await screen.findByRole('button',{name:'AAPL'})
 expect(JSON.parse(localStorage.getItem('sp500:history:7'))).toHaveLength(1)
 api.get.mockResolvedValue({data:{results:[]}})
 fireEvent.click(screen.getByRole('button',{name:'Eliminar AAPL'}))
 await waitFor(()=>expect(api.delete).toHaveBeenCalledWith('/companies/history/1/'))
 await waitFor(()=>expect(JSON.parse(localStorage.getItem('sp500:history:7'))).toHaveLength(0))
})
it('usa únicamente la copia local de la cuenta activa cuando falla la red',async()=>{
 localStorage.setItem('user',JSON.stringify({id:8}))
 localStorage.setItem('sp500:history:7',JSON.stringify([{id:1,query:'PRIVADO'}]))
 localStorage.setItem('sp500:history:8',JSON.stringify([{id:2,query:'MIO'}]))
 api.get.mockRejectedValue(new Error('offline'))
 render(<SearchHistory onSelectHistory={vi.fn()}/> )
 await screen.findByRole('button',{name:'MIO'})
 expect(screen.queryByText('PRIVADO')).toBeNull()
})
