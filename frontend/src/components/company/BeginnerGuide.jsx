import { useEffect, useRef, useState } from 'react'

export default function BeginnerGuide({ company, onExplore }) {
  const [step, setStep] = useState(null)
  const panel = useRef(null)
  const trigger = useRef(null)
  useEffect(() => { if (step !== null) panel.current?.focus() }, [step])
  const close = () => { setStep(null); trigger.current?.focus() }
  const ready = Number.isFinite(company.altman_z?.score) && company.altman_z?.zone !== 'unknown'
  return <section className="beginner-intro" aria-label="Empieza por aquí">
    <div><p className="section-eyebrow">EL NEGOCIO, EN UNA FRASE</p><h2>Conoce a {company.name || company.ticker}</h2>
      <p>{company.description || 'La reseña de esta empresa todavía no está disponible. Puedes consultar sus datos financieros y fuentes.'}</p>
    </div>
    <button className="primary-action" ref={trigger} onClick={() => setStep(0)}>Entender esta empresa <span aria-hidden="true">→</span></button>
    {step !== null && <div className="beginner-guide" ref={panel} tabIndex={-1} aria-label="Recorrido para entender la empresa">
      <ol className="beginner-steps" aria-label="Pasos del recorrido">{['Negocio', 'Cifras', 'Riesgos'].map((label,index) => <li key={label} aria-current={step === index ? 'step' : undefined}><span>{index+1}</span>{label}</li>)}</ol>
      {step === 0 && <div><h3>1. Conoce el negocio</h3><p>{company.description || 'Aún no hay una reseña disponible.'}</p><p>El precio de una acción y el tamaño de la empresa son cosas distintas. El precio mostrado es histórico, con su fecha; no es una cotización en tiempo real.</p></div>}
      {step === 1 && <div><h3>2. Lee las cifras con contexto</h3><p>Comienza por el valor en bolsa, la rentabilidad del patrimonio y la relación entre precio y beneficio. Las tarjetas explican qué mide cada cifra.</p><p>Una cifra alta no siempre significa una mejor inversión. Revisa su fecha y el negocio; «Sin datos» no significa cero.</p></div>}
      {step === 2 && <div><h3>3. Reconoce los límites</h3><p>{ready ? 'Altman-Z está disponible: es una clasificación académica del balance, no una recomendación de compra ni una garantía de solvencia.' : 'No podemos calcular Altman-Z con la información compatible disponible. Eso no significa que la empresa sea segura o esté en peligro.'}</p><p>El gráfico describe el pasado. Ni su evolución ni los indicadores garantizan resultados futuros.</p><button onClick={onExplore}>Explorar salud financiera</button></div>}
      <div className="beginner-guide-actions">{step > 0 && <button onClick={() => setStep(step-1)}>Anterior</button>}{step < 2 && <button onClick={() => setStep(step+1)}>{step === 0 ? 'Ver cifras →' : 'Ver riesgos →'}</button>}<button onClick={close}>Cerrar guía</button></div>
    </div>}
  </section>
}
