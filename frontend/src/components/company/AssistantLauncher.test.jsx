import React from 'react'
import {afterEach,beforeEach,expect,it,vi} from 'vitest'
import {act,cleanup,fireEvent,render,screen} from '@testing-library/react'
import {MemoryRouter} from 'react-router-dom'
import AssistantLauncher from './AssistantLauncher'
import {openAssistant} from '../../utils/assistantEvents'

vi.mock('./FinancialAssistant',()=>({default:props=><output aria-label="Contexto del asistente">{JSON.stringify(props)}</output>}))
vi.mock('../compare/CompanyPicker',()=>({default:({onSelect})=><button onClick={()=>onSelect('MSFT')}>Elegir Microsoft</button>}))
beforeEach(()=>{
 HTMLDialogElement.prototype.showModal=function(){this.setAttribute('open','')}
 HTMLDialogElement.prototype.close=function(){this.removeAttribute('open')}
})
afterEach(cleanup)
const mount=url=>render(<MemoryRouter initialEntries={[url]}><AssistantLauncher/></MemoryRouter>)

it('abre la empresa y el año de la pantalla, sin hacer una pregunta implícita',async()=>{
 mount('/company/AAPL?fiscal_year=2024')
 fireEvent.click(screen.getByRole('button',{name:/Asistente IA/}))
 const data=JSON.parse((await screen.findByLabelText('Contexto del asistente')).textContent)
 expect(data).toMatchObject({a:'AAPL',year:2024,initialQuestion:'',initialMode:'question'})
 expect(screen.getByRole('dialog').open).toBe(true)
 expect(document.documentElement.style.overflow).toBe('hidden')
 fireEvent.click(screen.getByRole('button',{name:'Cerrar asistente'}))
 expect(screen.queryByRole('dialog')).toBeNull()
 expect(document.documentElement.style.overflow).toBe('')
})
it('el acceso de una cifra transmite la pregunta y conserva su contexto explícito',async()=>{
 mount('/company/AAPL?fiscal_year=2024')
 await act(async()=>openAssistant({ticker:'MSFT',question:'¿Qué significa su Altman-Z?'}))
 const data=JSON.parse((await screen.findByLabelText('Contexto del asistente')).textContent)
 expect(data.a).toBe('MSFT')
 expect(data.year).toBeUndefined()
 expect(data.initialQuestion).toBe('¿Qué significa su Altman-Z?')
})
it('permite elegir una empresa desde inicio sin atribuirle datos de otra',async()=>{
 mount('/dashboard')
 fireEvent.click(screen.getByRole('button',{name:/Asistente IA/}))
 expect((await screen.findByRole('button',{name:'Consultar esta empresa →'})).disabled).toBe(true)
 fireEvent.click(await screen.findByRole('button',{name:'Elegir Microsoft'}))
 fireEvent.click(screen.getByRole('button',{name:'Consultar esta empresa →'}))
 expect(JSON.parse((await screen.findByLabelText('Contexto del asistente')).textContent).a).toBe('MSFT')
})
