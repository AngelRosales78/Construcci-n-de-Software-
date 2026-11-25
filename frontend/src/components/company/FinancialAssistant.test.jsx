import React from 'react'
import {afterEach,beforeEach,expect,it,vi} from 'vitest'
import {act,cleanup,fireEvent,render,screen,waitFor,within} from '@testing-library/react'
import FinancialAssistant from './FinancialAssistant'
import {assistantRepository} from '../../models/assistantRepository'

vi.mock('../../models/assistantRepository',()=>({assistantRepository:{read:vi.fn(),send:vi.fn()}}))
const fact={id:'current_ratio',topic:'current_ratio',pillar:'liquidity',title:'Liquidez corriente',value:'2,00 veces',kind:'strength',
 meaning:'<script>Texto que no se ejecuta</script>',limit:'No todo es efectivo.',period:{start:'2023-07-01',end:'2024-06-30'},sources:[{label:'Fuente del balance',url:'https://data.sec.gov/source',period:'2024-06-30'}]}
const meta={schema_version:'evidence-assistant-v1',scope:'company',tickers:['MSFT'],fiscal_year:2024,available_years:[2024,2023],
 facts:[fact],overview:[{id:'strength',title:'Qué destaca',facts:[fact]}],suggestions:['¿Cómo está su liquidez?'],lessons:[{id:'liquidity',title:'Entender la liquidez'}],disclaimer:'Describe datos históricos.'}
const response={...meta,status:'available',generation_mode:'local_ai_evidence_routing',answer:{topics:['liquidity'],cards:[fact],messages:[]}}
const lesson={id:'liquidity',title:'Entender la liquidez',explanation:'Recursos frente a obligaciones.',example:'Ejemplo ilustrativo: empresa ficticia.',
 question:'¿Todo está en efectivo?',options:['Sí.','No.']}
beforeEach(()=>{vi.resetAllMocks();assistantRepository.read.mockResolvedValue(meta);assistantRepository.send.mockResolvedValue(response)})
afterEach(cleanup)
const mount=props=>render(<FinancialAssistant a="MSFT" year={2024} {...props}/> )
const questions=async()=>{await screen.findByRole('button',{name:'Preguntar',exact:true});fireEvent.click(screen.getByRole('button',{name:'Preguntar',exact:true}))}
const ask=async text=>{fireEvent.change(screen.getByLabelText('Tu pregunta'),{target:{value:text}});fireEvent.click(screen.getByRole('button',{name:'Enviar pregunta',exact:true}));await waitFor(()=>expect(screen.getByLabelText('Tu pregunta').disabled).toBe(false))}

it('la lectura muestra cifras, límites y fuentes como texto sin ejecutar contenido',async()=>{
 mount();await screen.findByText('2,00 veces')
 expect(screen.getByText('<script>Texto que no se ejecuta</script>')).toBeTruthy()
 expect(document.querySelector('.financial-assistant script')).toBeNull()
 expect(screen.getByRole('link',{name:/Fuente del balance/}).getAttribute('href')).toBe('https://data.sec.gov/source')
 expect(screen.getByText(/No todo es efectivo/)).toBeTruthy()
 expect(assistantRepository.send).not.toHaveBeenCalled()
})
it('conserva el contexto de seguimiento y permite reiniciar la conversación',async()=>{
 mount();await questions();await ask('¿Cómo está su liquidez?')
 await ask('¿Podrías explicarlo más?')
 const payload=assistantRepository.send.mock.calls[1][1]
 expect(payload.history).toEqual([{question:'¿Cómo está su liquidez?',topics:['liquidity']}])
 expect(assistantRepository.send.mock.calls[1][0]).toEqual({a:'MSFT',b:undefined,year:2024})
 expect(within(screen.getByRole('log')).getAllByRole('article').length).toBeGreaterThan(1)
 fireEvent.click(screen.getByRole('button',{name:'Reiniciar conversación'}))
 expect(within(screen.getByRole('log')).queryByText('¿Podrías explicarlo más?')).toBeNull()
 await ask('¿Qué falta?')
 expect(assistantRepository.send.mock.lastCall[1].history).toEqual([])
})
it('un fallo conserva la pregunta y el resultado anterior y permite reintentar',async()=>{
 assistantRepository.send.mockResolvedValueOnce(response).mockRejectedValueOnce(new Error('Offline')).mockResolvedValueOnce(response)
 mount();await questions();await ask('Primera pregunta')
 await ask('Segunda pregunta')
 expect(await screen.findByRole('alert')).toBeTruthy()
 expect(screen.getByLabelText('Tu pregunta').value).toBe('Segunda pregunta')
 expect(within(screen.getByRole('log')).getByText('Primera pregunta')).toBeTruthy()
 fireEvent.click(screen.getByRole('button',{name:'Reintentar consulta'}))
 await waitFor(()=>expect(screen.getByLabelText('Tu pregunta').value).toBe(''))
 expect(within(screen.getByRole('log')).getByText('Segunda pregunta')).toBeTruthy()
})
it('descarta respuestas antiguas al cambiar empresa o año',async()=>{
 let finish
 assistantRepository.send.mockImplementationOnce(()=>new Promise(resolve=>{finish=resolve}))
 const view=mount();await questions()
 fireEvent.change(screen.getByLabelText('Tu pregunta'),{target:{value:'Pregunta anterior'}})
 fireEvent.click(screen.getByRole('button',{name:'Enviar pregunta',exact:true}))
 const signal=assistantRepository.send.mock.calls[0][2]
 assistantRepository.read.mockResolvedValue({...meta,tickers:['AAPL'],fiscal_year:2025,overview:[],facts:[]})
 view.rerender(<FinancialAssistant a="AAPL" year={2025}/>)
 await screen.findByText(/AAPL · 2025/)
 await act(async()=>finish(response))
 expect(signal.aborted).toBe(true)
 expect(screen.queryByText('Pregunta anterior')).toBeNull()
 expect(screen.queryByText('2,00 veces')).toBeNull()
})
it('solicita dos años explícitos y muestra la diferencia calculada por el servidor',async()=>{
 assistantRepository.send.mockResolvedValue({...meta,scope:'changes',from_year:2023,to_year:2024,generation_mode:'calculated_evidence',facts:[{...fact,meaning:'Aumentó 1,00 veces.',value:'1,00 → 2,00 veces'}]})
 mount();await screen.findByRole('button',{name:'Cambios entre años'});fireEvent.click(screen.getByRole('button',{name:'Cambios entre años'}))
 fireEvent.click(screen.getByRole('button',{name:'Ver cambios',exact:true}))
 await screen.findByText('Aumentó 1,00 veces.')
 expect(assistantRepository.send.mock.lastCall[1]).toEqual({action:'changes',from_year:2023,to_year:2024})
 expect(screen.getByText(/no permiten afirmar qué decisiones/)).toBeTruthy()
})
it('las lecciones identifican ejemplos ficticios y verifican la respuesta elegida',async()=>{
 assistantRepository.send.mockResolvedValueOnce({...meta,scope:'lesson',lesson}).mockResolvedValueOnce({...meta,scope:'lesson',lesson:{...lesson,assessment:{correct:true,feedback:'Puede incluir inventario.'}}})
 mount();await screen.findByRole('button',{name:'Aprender',exact:true});fireEvent.click(screen.getByRole('button',{name:'Aprender',exact:true}))
 fireEvent.click(screen.getByRole('button',{name:'Entender la liquidez'}))
 await screen.findByText('Ejemplo ilustrativo: empresa ficticia.')
 fireEvent.click(screen.getByRole('radio',{name:'No.',exact:true}))
 fireEvent.click(screen.getByRole('button',{name:'Comprobar mi respuesta'}))
 await screen.findByText('Puede incluir inventario.')
 expect(assistantRepository.send.mock.lastCall[1]).toEqual({action:'learn',topic:'liquidity',answer:1})
})
it('el asistente comparativo conserva ambas empresas y se carga solo al abrirlo',async()=>{
 assistantRepository.read.mockResolvedValue({...meta,scope:'comparison',tickers:['MSFT','AAPL'],overview:[]})
 mount({b:'AAPL',deferred:true})
 expect(assistantRepository.read).not.toHaveBeenCalled()
 fireEvent.click(screen.getByRole('button',{name:'Abrir asistente',exact:true}))
 await screen.findByRole('button',{name:'Preguntar',exact:true})
 expect(assistantRepository.read.mock.lastCall[0]).toEqual({a:'MSFT',b:'AAPL',year:2024})
 expect(screen.queryByRole('button',{name:'Cambios entre años'})).toBeNull()
})
