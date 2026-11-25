import { useEffect, useRef, useState } from 'react'
import { assistantRepository } from '../models/assistantRepository'

export function useFinancialAssistantViewModel({ a, b, year, deferred=false }) {
  const context={a,b,year}
  const key=`${a}:${b || ''}:${year || ''}`
  const sequence=useRef(0), controller=useRef(null), last=useRef(null)
  const [state,setState]=useState({key,meta:null,mode:'summary',turns:[],draft:'',busy:'',error:'',summary:null,changes:null,lesson:null,fromYear:'',answer:''})
  const current=state.key===key ? state : {key,meta:null,mode:'summary',turns:[],draft:'',busy:'',error:'',summary:null,changes:null,lesson:null,fromYear:'',answer:''}
  const patch=values=>setState(previous=>({...previous,...values}))
  function checked(result) {
    if (result?.schema_version!=='evidence-assistant-v1' || result.tickers?.join(',')!==[a,b].filter(Boolean).join(',')) throw new Error('context')
    if (result.scope!=='lesson' && year && Number(result.fiscal_year)!==Number(year)) throw new Error('year')
    return result
  }
  const request=async payload=>{
    const id=++sequence.current
    controller.current?.abort()
    controller.current=new AbortController()
    last.current=payload
    patch({key,busy:payload?.action || 'read',error:''})
    try {
      const result=checked(await (payload ? assistantRepository.send(context,payload,controller.current.signal) : assistantRepository.read(context,controller.current.signal)))
      if(id!==sequence.current)return
      if(payload?.action==='question') {
        if(result.status!=='available' || !result.answer) throw new Error('unavailable')
        setState(previous=>({...previous,turns:[...previous.turns,{question:payload.question,response:result}],draft:''}))
      } else if(payload?.action==='learn') patch({lesson:result.lesson,answer:payload.answer ?? ''})
      else if(payload?.action==='changes') patch({changes:result})
      else if(payload?.action==='summary') {
        if(result.status!=='available' || !result.answer)throw new Error('unavailable')
        patch({summary:result,meta:result})
      } else {
        if(!Array.isArray(result.facts))throw new Error('unavailable')
        patch({meta:result,fromYear:String(result.available_years.find(value=>value<Number(year)) || '')})
      }
    } catch(error) {
      if(id!==sequence.current || error.code==='ERR_CANCELED')return
      const status=error.response?.status
      patch({error:status===429 ? 'Has alcanzado el límite de consultas. Espera un momento; tu pregunta y tus resultados se conservan.'
        : status===403 ? 'Esta herramienta requiere el acceso del comparador. Verifica tu plan.'
          : 'No se pudo completar la consulta. Conservamos tu pregunta y el último resultado para que puedas reintentar.'})
    } finally {if(id===sequence.current)patch({busy:''})}
  }
  useEffect(()=>{
    sequence.current++;controller.current?.abort();last.current=null
    setState({key,meta:null,mode:'summary',turns:[],draft:'',busy:'',error:'',summary:null,changes:null,lesson:null,fromYear:'',answer:''})
    if(!deferred)request(null)
    return ()=>{sequence.current++;controller.current?.abort()}
  },[key,deferred])
  const ask=question=>{
    if(!question.trim() || current.busy)return
    patch({mode:'question',draft:question})
    return request({action:'question',question:question.trim(),history:current.turns.slice(-4).map(turn=>({question:turn.question,topics:turn.response.answer.topics}))})
  }
  const reset=()=>{sequence.current++;controller.current?.abort();last.current=null;patch({turns:[],draft:'',error:'',busy:''})}
  return {...current,load:()=>request(null),retry:()=>request(last.current),setMode:mode=>patch({mode,error:''}),
    setDraft:draft=>patch({draft}),ask,reset, summarize:()=>request({action:'summary'}),
    setFromYear:fromYear=>patch({fromYear,changes:null}),compareYears:()=>request({action:'changes',from_year:Number(current.fromYear),to_year:Number(year)}),
    learn:topic=>{patch({lesson:null,answer:''});return request({action:'learn',topic})},
    setAnswer:answer=>patch({answer}),checkAnswer:()=>request({action:'learn',topic:current.lesson.id,answer:Number(current.answer)})}
}
