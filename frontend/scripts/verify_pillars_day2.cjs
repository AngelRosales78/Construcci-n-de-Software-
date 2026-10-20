// Recorrido real contra servicios LOCALES aislados con los CSV e inventario SEC.
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')
const web = process.env.PILLARS_WEB_URL || 'http://127.0.0.1:55190'
const api = process.env.PILLARS_API_URL || 'http://127.0.0.1:58220/api/v1'
const report = process.env.PILLARS_REPORT
assert(report, 'Indique PILLARS_REPORT fuera del repositorio.')
for (const url of [web, api]) assert(['127.0.0.1', 'localhost'].includes(new URL(url).hostname))

;(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
  const steps = [], errors = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(`${web}/register`)
    const username = `pilares${Date.now()}`
    for (const [id, value] of Object.entries({ firstName: 'Prueba', lastName: 'Pilares', username,
      email: `${username}@example.test`, password: 'Temporal!Solo2026', passwordConfirm: 'Temporal!Solo2026' })) {
      await page.locator(`#${id}`).fill(value)
    }
    await page.getByRole('button', { name: 'Crear cuenta', exact: true }).click()
    await page.waitForURL('**/dashboard')
    await page.goto(`${web}/search`)
    await page.locator('.company-card').filter({ hasText: 'Microsoft' }).first().click()
    await page.waitForURL('**/company/MSFT')
    await page.getByRole('article', { name: 'Liquidez corriente', exact: true }).waitFor()
    const token = await page.evaluate(() => localStorage.getItem('access_token'))
    const result = await context.request.get(`${api}/companies/MSFT/pillars/?fiscal_year=2024`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    assert.equal(result.status(), 200)
    const body = await result.json()
    const expected = { current_ratio: '1,27', quick_ratio: '1,06', roe: '37,13', roa: '19,07',
      net_margin: '35,96', liabilities_to_equity: '0,91', interest_coverage: '37,72' }
    for (const metric of body.pillars.flatMap(p => p.metrics)) {
      assert.equal(metric.display_value, expected[metric.id])
      await page.getByRole('article', { name: metric.label, exact: true }).getByText(expected[metric.id], { exact: false }).waitFor()
    }
    assert.equal(body.overall_score.value, null)
    steps.push('MSFT FY2024 muestra siete indicadores iguales al cálculo independiente y a la API real')
    await page.getByRole('article', { name: 'Prueba ácida', exact: true }).locator('summary').click()
    assert((await page.getByRole('article', { name: 'Prueba ácida', exact: true }).innerText()).includes('2024-07-30'))
    assert((await page.getByRole('article', { name: 'Prueba ácida', exact: true }).getByRole('link', { name: 'Consultar fuente' }).first().getAttribute('href')).startsWith('https://data.sec.gov/'))
    steps.push('Fórmula, componentes, publicación y enlace SEC visibles en la web')
    await page.getByLabel('Ejercicio fiscal').selectOption('2020')
    await page.getByText(/Ejercicio: 2020/).waitFor()
    await page.getByRole('article', { name: 'ROE', exact: true }).getByText('No disponible', { exact: true }).waitFor()
    await page.getByRole('article', { name: 'Cobertura de intereses', exact: true }).getByText('No disponible', { exact: true }).waitFor()
    steps.push('Cambiar a FY2020 conserva los faltantes y no reutiliza cifras del ejercicio anterior')
    await page.getByLabel('Ejercicio fiscal').selectOption('2024')
    await page.getByRole('article', { name: 'ROE', exact: true }).getByText('37,13', { exact: false }).waitFor()
    const screenshotDir = path.dirname(report)
    await page.locator('.financial-pillars').screenshot({ path: path.join(screenshotDir, 'pilares-desktop.png') })
    await page.setViewportSize({ width: 390, height: 844 })
    await page.locator('.financial-pillars').screenshot({ path: path.join(screenshotDir, 'pilares-mobile.png') })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth))
    steps.push('Vista web a 1440 y 390 píxeles sin desbordamiento horizontal')
    await page.goto(`${web}/company/AAPL`)
    await page.getByText('No hay balances anuales importados para esta empresa.').waitFor()
    assert.equal(await page.locator('.pillar-value').filter({ hasText: 'No disponible' }).count(), 7)
    steps.push('Empresa sin balances muestra siete indicadores no disponibles, sin cifras de Microsoft')
    const pattern = '**/companies/MSFT/pillars/'
    await page.route(pattern, route => route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":"Fallo temporal de prueba"}' }))
    await page.goto(`${web}/company/MSFT`)
    await page.getByRole('button', { name: 'Reintentar pilares' }).waitFor()
    await page.unroute(pattern)
    await page.getByRole('button', { name: 'Reintentar pilares' }).click()
    await page.getByRole('article', { name: 'Liquidez corriente', exact: true }).getByText('1,27', { exact: false }).waitFor()
    steps.push('Fallo de red deliberado y reintento recuperan los indicadores reales')
    assert.equal((await context.request.get(`${api}/companies/MSFT/pillars/`)).status(), 401)
    assert.equal(errors.length, 0)
    fs.writeFileSync(report, JSON.stringify({ result: 'passed', steps, javascriptErrors: errors,
      metricsFY2024: expected, scope: 'Web y servidor local; no acredita integración Android' }, null, 2))
    console.log(`Recorridos aprobados: ${steps.length}; errores JavaScript: ${errors.length}`)
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
