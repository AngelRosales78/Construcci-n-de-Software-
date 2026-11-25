import { useId,useState } from 'react'
import { useFinancialAssistantViewModel } from '../../viewmodels/useFinancialAssistantViewModel'
import './financial-assistant.css'

function Sources({ sources=[] }) {
  return sources.length>0 && <div className="assistant-sources" aria-label="Fuentes de esta explicación">{sources.map((source,index)=><a key={`${source.url}-${source.period}-${index}`} href={source.url} target="_blank" rel="noreferrer">{source.label}{source.period && ` · ${source.period}`}</a>)}</div>
}

function EvidenceCard({ fact }) {
  return <article className={`assistant-fact assistant-fact-${fact.kind}`}>
    <h4>{fact.title}</h4>{fact.comparison_values?.length ? <div className="assistant-comparison-values">{fact.comparison_values.map(item=><span key={item.ticker}><small>{item.ticker}</small><strong>{item.value}</strong></span>)}</div> : <strong className="assistant-value">{fact.value}</strong>}
    {fact.period?.end && <p className="assistant-date">{fact.period.start && `${fact.period.start} → `}{fact.period.end}</p>}
    {fact.periods && !fact.period?.end && <p className="assistant-date">{Object.entries(fact.periods).map(([ticker,period])=>`${ticker}: ${period.start || 'Inicio sin verificar'} → ${period.end || 'Sin cierre'}`).join(' · ')}</p>}
    <p>{fact.meaning}</p><p className="assistant-limit"><strong>Ten en cuenta:</strong> {fact.limit}</p>
    <Sources sources={fact.sources}/>
  </article>
}

function Answer({ response }) {
  const periodNote=Object.hasOwn(response.answer,'period_note') ? response.answer.period_note : response.period_note
  return <div className="assistant-answer">
    <p className="assistant-answer-context">{response.tickers.join(' y ')} · Ejercicio {response.fiscal_year} · {response.generation_mode==='local_ai_evidence_routing' ? 'IA con datos de esta pantalla' : response.generation_mode==='context_guard' ? 'Límite del contexto abierto' : 'Lectura calculada'}</p>
    {response.answer.messages.map((message,index)=><p className="assistant-message" key={index}>{message}</p>)}
    {!!response.answer.examples?.length && <section className="assistant-illustrations" aria-label="Ejemplos ilustrativos"><h4>Un ejemplo para entenderlo</h4><p>Estas cifras son ficticias; no describen a {response.tickers.join(' ni a ')}.</p>{response.answer.examples.map((example,index)=><p className="assistant-example" key={index}>{example}</p>)}</section>}
    <div className="assistant-answer-cards">{response.answer.cards.map(fact=><EvidenceCard key={fact.id} fact={fact}/>)}</div>
    {periodNote && <p className="assistant-period-note">{periodNote}</p>}
  </div>
}

export default function FinancialAssistant({ a, b, year, deferred=false,initialMode='summary',initialQuestion='' }) {
  const vm=useFinancialAssistantViewModel({a,b,year,deferred,initialMode,initialQuestion})
  year=year || vm.meta?.fiscal_year
  const id=useId()
  const [expandedTools,setExpandedTools]=useState(!initialQuestion)
  const modes=[['summary','Lectura rápida'],['question','Preguntar'],...(!b ? [['changes','Cambios entre años'],['learn','Aprender']] : [])]
  return <section className={`financial-assistant${b ? ' financial-assistant-comparison' : ''}`} aria-labelledby={`${id}-title`}>
    <div className="assistant-heading"><div><p className="section-eyebrow">ENTIENDE ANTES DE CONCLUIR</p><h2 id={`${id}-title`}>{b ? 'Entiende la comparación' : 'Tu asistente financiero'}</h2><p>Cifras, significado y límites. {a}{b && ` y ${b}`} · {year || 'Último ejercicio disponible'}.</p></div><span className="assistant-context-badge">{b ? 'Dos empresas' : 'Una empresa'} · Contexto acotado</span></div>
    {!vm.meta && !vm.busy && !vm.error && <button type="button" className="primary-action" onClick={vm.load}>Abrir asistente</button>}
    {vm.busy && <p role="status" className="assistant-loading">{['summary','question'].includes(vm.busy) ? 'La IA local está interpretando tu consulta. La primera respuesta puede tardar.' : 'Consultando los datos…'}</p>}
    {vm.error && <div className="assistant-error" role="alert"><p>{vm.error}</p><button type="button" onClick={vm.retry} disabled={!!vm.busy}>Reintentar consulta</button></div>}
    {vm.meta && <>
      {expandedTools && <nav className="assistant-modes" aria-label="Herramientas del asistente">{modes.map(([mode,label])=><button type="button" key={mode} aria-pressed={vm.mode===mode} aria-controls={`${id}-${mode}`} onClick={()=>vm.setMode(mode)}>{label}</button>)}</nav>}
      {vm.mode==='summary' && <div id={`${id}-summary`}>
        <p className="assistant-section-intro">Lectura automática de los datos disponibles. Las clasificaciones siguen las reglas académicas del proyecto.</p>
        <div className="assistant-overview">{vm.meta.overview.map(group=><section key={group.id} className={`assistant-overview-group assistant-overview-${group.id}`}><h3>{group.title}</h3>{group.message && <p>{group.message}</p>}{group.facts.map(fact=><EvidenceCard key={fact.id} fact={fact}/>)}</section>)}</div>
        <button type="button" className="primary-action" onClick={vm.summarize} disabled={!!vm.busy}>Pedir lectura de IA</button>
        {vm.summary && <Answer response={vm.summary}/>}
      </div>}
      {vm.mode==='question' && <div id={`${id}-question`}>
        <div className="assistant-question-heading"><h3>{initialQuestion && !expandedTools ? 'Tu resultado, explicado' : `Pregunta sobre ${b ? 'estas empresas' : a}`}</h3>{expandedTools && <button type="button" onClick={vm.reset}>Reiniciar conversación</button>}</div>
        {expandedTools && <><p>Solo utiliza los datos de esta pantalla. Puedes hacer preguntas de seguimiento.</p><div className="assistant-suggestions" aria-label="Preguntas sugeridas">{vm.meta.suggestions.map(question=><button type="button" key={question} onClick={()=>vm.ask(question)} disabled={!!vm.busy}>{question}</button>)}</div></>}
        <div className="assistant-conversation" role="log" aria-label="Conversación financiera" aria-live="polite" aria-busy={!!vm.busy}>
          {vm.turns.map((turn,index)=><article className="assistant-turn" key={index}><h4 className="assistant-user-question">{turn.question}</h4><Answer response={turn.response}/></article>)}
        </div>
        <form className="assistant-question-form" onSubmit={event=>{event.preventDefault();vm.ask(vm.draft)}}>
          <label htmlFor={`${id}-question-input`}>Tu pregunta</label><textarea id={`${id}-question-input`} value={vm.draft} onChange={event=>vm.setDraft(event.target.value)} maxLength={600} rows={3} placeholder="Por ejemplo: ¿qué significa su liquidez?" disabled={!!vm.busy}/>
          <div><small>{vm.draft.length}/600 · No incluyas información personal.</small><button className="primary-action" type="submit" disabled={!!vm.busy || !vm.draft.trim()}>Enviar pregunta</button></div>
        </form>
      </div>}
      {initialQuestion && !expandedTools && <button type="button" className="assistant-expand-tools" onClick={()=>setExpandedTools(true)}>Ver preguntas sugeridas y herramientas</button>}
      {vm.mode==='changes' && <div id={`${id}-changes`}>
        <h3>Qué cambió entre dos ejercicios</h3><p>Compara indicadores calculados con periodos compatibles. Un cambio no demuestra su causa.</p>
        <form className="assistant-years" onSubmit={event=>{event.preventDefault();vm.compareYears()}}><label>Ejercicio anterior<select aria-label="Ejercicio anterior" value={vm.fromYear} onChange={event=>vm.setFromYear(event.target.value)} disabled={!!vm.busy}><option value="">Selecciona un año</option>{vm.meta.available_years.filter(value=>value<Number(year)).map(value=><option key={value} value={value}>{value}</option>)}</select></label><div><small>Ejercicio posterior</small><strong>{year}</strong></div><button type="submit" className="primary-action" disabled={!!vm.busy || !vm.fromYear}>Ver cambios</button></form>
        {!vm.meta.available_years.some(value=>value<Number(year)) && <p>No hay otro ejercicio anterior cargado para esta selección.</p>}
        {vm.changes && <><p className="assistant-period-note">{vm.changes.period_note || `${vm.changes.from_year} → ${vm.changes.to_year} · Las diferencias se calculan fuera del modelo de IA.`}</p><div className="assistant-change-table" tabIndex="0" role="region" aria-label="Cambios entre ejercicios, desplazable horizontalmente"><table><caption>{a} · {vm.changes.from_year} frente a {vm.changes.to_year}</caption><thead><tr><th scope="col">Indicador</th><th scope="col">Antes → después</th><th scope="col">Lectura del cambio</th></tr></thead><tbody>{vm.changes.facts.map(fact=><tr key={fact.id}><th scope="row">{fact.title}</th><td>{fact.value}</td><td>{fact.meaning}<Sources sources={fact.sources}/></td></tr>)}</tbody></table></div><p className="assistant-limit">Los balances describen variaciones; no permiten afirmar qué decisiones o acontecimientos las causaron.</p></>}
      </div>}
      {vm.mode==='learn' && <div id={`${id}-learn`}>
        <h3>Aprende a interpretar las cifras</h3><p>Guías educativas revisadas. Los ejemplos son ficticios y no describen a {a}.</p>
        <div className="assistant-lessons" aria-label="Lecciones disponibles">{vm.meta.lessons.map(item=><button key={item.id} type="button" aria-pressed={vm.lesson?.id===item.id} disabled={!!vm.busy} onClick={()=>vm.learn(item.id)}>{item.title}</button>)}</div>
        {vm.lesson && <article className="assistant-lesson"><h4>{vm.lesson.title}</h4><p>{vm.lesson.explanation}</p><p className="assistant-example">{vm.lesson.example}</p><form onSubmit={event=>{event.preventDefault();vm.checkAnswer()}}><fieldset><legend>{vm.lesson.question}</legend>{vm.lesson.options.map((option,index)=><label key={option}><input type="radio" name={`${id}-quiz`} value={index} checked={String(vm.answer)===String(index)} onChange={()=>vm.setAnswer(index)} disabled={!!vm.busy}/>{option}</label>)}</fieldset><button type="submit" disabled={!!vm.busy || vm.answer===''}>Comprobar mi respuesta</button></form>{vm.lesson.assessment && <p role="status" className="assistant-feedback"><strong>{vm.lesson.assessment.correct ? 'Correcto.' : 'Revisemos la idea.'}</strong> {vm.lesson.assessment.feedback}</p>}</article>}
      </div>}
      <footer className="assistant-footer"><p>{vm.meta.disclaimer}</p><p>La IA identifica el tema de la pregunta; las cifras y explicaciones provienen de datos y reglas comprobables. No consulta noticias ni internet.</p></footer>
    </>}
  </section>
}
