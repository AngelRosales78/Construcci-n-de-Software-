import FinancialHealthIndicator from './FinancialHealthIndicator'

function RiskExplanation() {
  return <details className="financial-help"><summary>¿Cómo se calcula Altman-Z?</summary><div className="help-content"><p>Z = 1,2X₁ + 1,4X₂ + 3,3X₃ + 0,6X₄ + X₅</p><ul><li>X₁: capital de trabajo / activos totales.</li><li>X₂: beneficios retenidos / activos totales.</li><li>X₃: beneficio antes de intereses e impuestos / activos totales.</li><li>X₄: valor bursátil del patrimonio / pasivos totales.</li><li>X₅: ventas / activos totales.</li></ul><p>Zonas: atención por debajo de 1,81; intermedia de 1,81 a 2,99; favorable por encima de 2,99.</p><p>La fórmula original se diseñó para manufactureras cotizadas. Su uso aquí es académico y no se ha validado como predictor para esta empresa. El puntaje no es un porcentaje ni garantiza solvencia.</p></div></details>
}
export default function AltmanZGauge({ score, zone, reference, availability }) {
  if (score == null || !Number.isFinite(score) || zone === 'unknown') {
    return <div className="altmanz-gauge"><h3>Altman-Z Score</h3>
      {!availability && <p>No hay datos suficientes para calcular el indicador.</p>}<FinancialHealthIndicator altmanZ={{score, zone: 'unknown', availability}} />{availability?.next_step && <p>Siguiente paso: {availability.next_step}</p>}<RiskExplanation /></div>
  }
  return (
    <div className="altmanz-gauge">
      <h3>Altman-Z Score</h3>
      <FinancialHealthIndicator altmanZ={{score, zone, reference}} />
      <RiskExplanation />
      {reference && <details className="financial-help"><summary>Ver datos y fuentes de Altman-Z</summary><div className="help-content">
        <p>Período contable: {reference.period_start} a {reference.period_end}. Moneda: {reference.currency}.</p>
        <p>Precio sin ajustar: {Number(reference.unadjusted_close).toLocaleString('es-PE', {maximumFractionDigits: 2})} USD · Sesión: {reference.price_date}.</p>
        <p>Acciones al cierre: {Number(reference.shares_outstanding).toLocaleString('es-PE')}.</p>
        <ul>{Object.entries(reference.inputs).map(([key, value]) => <li key={key}>{({working_capital:'Capital de trabajo',total_assets:'Activos totales',retained_earnings:'Beneficios retenidos',ebit:'EBIT',equity_value:'Valor bursátil del patrimonio',total_liabilities:'Pasivos totales',sales:'Ventas'})[key]}: {Number(value).toLocaleString('es-PE', {maximumFractionDigits:2})} USD</li>)}</ul>
        <p>{reference.method}</p><p>{reference.locator}</p>
        <a href={reference.report_url} target="_blank" rel="noreferrer">Consultar informe oficial</a>{' · '}<a href={reference.price_url} target="_blank" rel="noreferrer">Consultar histórico de precios</a>
      </div></details>}
    </div>
  )
}
