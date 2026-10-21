// Validación real del día 3 contra servicios LOCALES con el inventario SEC de MSFT.
const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')
const web = process.env.PILLARS_WEB_URL || 'http://127.0.0.1:55191'
const api = process.env.PILLARS_API_URL || 'http://127.0.0.1:58221/api/v1'
const report = process.env.PILLARS_REPORT
assert(report, 'Indique PILLARS_REPORT fuera del repositorio.')
for (const value of [web, api]) assert(['127.0.0.1', 'localhost'].includes(new URL(value).hostname))
const expectedMetrics = { asset_turnover: '0,53', inventory_days: '9,22', revenue_growth: '15,67', revenue_cagr_3y: '13,40' }
const expectedNotes = { liquidity: '81,87', profitability: '100,00', solvency: '88,65', efficiency: '73,96', growth: '81,78' }

;(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
  const steps = [], errors = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(`${web}/register`)
    const username = `radar${Date.now()}`
    for (const [id, value] of Object.entries({ firstName: 'Prueba', lastName: 'Radar', username,
      email: `${username}@example.test`, password: 'Temporal!Solo2026', passwordConfirm: 'Temporal!Solo2026' })) await page.locator(`#${id}`).fill(value)
    await page.getByRole('button', { name: 'Crear cuenta', exact: true }).click()
    await page.waitForURL('**/dashboard')
    await page.goto(`${web}/search`)
    await page.locator('.company-card').filter({ hasText: 'Microsoft' }).first().click()
    await page.waitForURL('**/company/MSFT')
    await page.locator('.overall-score-value').getByText('85,25', { exact: false }).waitFor()
    const token = await page.evaluate(() => localStorage.getItem('access_token'))
    const response = await context.request.get(`${api}/companies/MSFT/pillars/?fiscal_year=2024`, { headers: { Authorization: `Bearer ${token}` } })
    assert.equal(response.status(), 200)
    const data = await response.json()
    assert.equal(data.pillars.length, 5)
    assert.equal(data.overall_score.display_value, '85,25')
    assert.equal(data.overall_score.version, 'academic-pillar-scores-v1')
    for (const p of data.pillars) {
      assert.equal(p.score.display_value, expectedNotes[p.id])
      for (const metric of p.metrics.filter(m => expectedMetrics[m.id])) {
        assert.equal(metric.display_value, expectedMetrics[metric.id])
        await page.getByRole('article', { name: metric.label, exact: true }).getByText(metric.display_value, { exact: false }).waitFor()
      }
    }
    steps.push('Eficiencia, Crecimiento, cinco notas y puntaje FY2024 coinciden con el cálculo independiente y la API')
    const history = page.getByRole('table', { name: 'Eficiencia en hasta tres ejercicios disponibles' })
    assert.equal(await history.getByRole('row').count(), 4)
    assert((await history.innerText()).includes('2022'))
    await page.getByRole('article', { name: 'Crecimiento compuesto a tres años' }).locator('summary').click()
    assert.equal(await page.getByRole('article', { name: 'Crecimiento compuesto a tres años' }).getByRole('link', { name: 'Consultar fuente' }).count(), 4)
    steps.push('Serie de tres ejercicios y cuatro fuentes anuales del crecimiento compuesto visibles')
    const polygon = page.locator('.recharts-radar-polygon')
    await polygon.waitFor()
    assert((await polygon.boundingBox()).width > 50)
    await page.getByRole('link', { name: 'Abrir detalle de Crecimiento', exact: true }).click()
    assert(page.url().endsWith('#pillar-growth'))
    const folder = path.dirname(report)
    await page.locator('.pillar-radar-section').screenshot({ path: path.join(folder, 'radar-dia3-desktop.png') })
    await page.locator('.overall-score').screenshot({ path: path.join(folder, 'puntaje-dia3-desktop.png') })
    steps.push('Radar real con polígono de cinco ejes y enlace desde el eje al detalle del pilar')
    await page.getByLabel('Ejercicio fiscal').selectOption('2022')
    await page.getByText(/Ejercicio: 2022/).waitFor()
    await page.locator('.overall-score-value').getByText('No disponible', { exact: true }).waitFor()
    assert.equal(await polygon.count(), 0)
    assert.equal((await context.request.get(`${api}/companies/MSFT/pillars/?fiscal_year=2022`, { headers: { Authorization: `Bearer ${token}` } })).status(), 200)
    assert((await page.locator('.overall-score').innerText()).includes('Crecimiento'))
    steps.push('FY2022 sin cuatro cierres no publica puntaje ni polígono; identifica Crecimiento pendiente')
    await page.getByLabel('Ejercicio fiscal').selectOption('2024')
    await page.locator('.overall-score-value').getByText('85,25', { exact: false }).waitFor()
    await page.setViewportSize({ width: 390, height: 844 })
    await page.locator('.pillar-radar-section').screenshot({ path: path.join(folder, 'radar-dia3-mobile.png') })
    await page.locator('.overall-score').screenshot({ path: path.join(folder, 'puntaje-dia3-mobile.png') })
    assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth))
    steps.push('Web a 1440 y 390 píxeles conserva radar, tablas y puntaje sin desbordamiento horizontal')
    await page.goto(`${web}/company/AAPL`)
    await page.getByText('No hay balances anuales importados para esta empresa.').waitFor()
    await page.locator('.overall-score-value').getByText('No disponible', { exact: true }).waitFor()
    assert.equal(await page.locator('.recharts-radar-polygon').count(), 0)
    assert.equal(await page.locator('.pillar-value').filter({ hasText: 'No disponible' }).count(), 11)
    steps.push('Empresa sin datos muestra once indicadores y puntaje no disponibles, sin cifras heredadas de MSFT')
    const pattern = '**/companies/MSFT/pillars/'
    await page.route(pattern, route => route.fulfill({ status: 503, contentType: 'application/json', body: '{"error":"Fallo de prueba"}' }))
    await page.goto(`${web}/company/MSFT`)
    await page.getByRole('button', { name: 'Reintentar pilares' }).waitFor()
    await page.unroute(pattern)
    await page.getByRole('button', { name: 'Reintentar pilares' }).click()
    await page.locator('.overall-score-value').getByText('85,25', { exact: false }).waitFor()
    assert.equal((await context.request.get(`${api}/companies/MSFT/pillars/`)).status(), 401)
    assert.equal(errors.length, 0)
    steps.push('Fallo temporal y reintento recuperan el resultado; consulta sin sesión rechazada')
    fs.writeFileSync(report, JSON.stringify({ result: 'passed', steps, javascriptErrors: errors,
      expectedMetrics, expectedNotes, expectedOverall: '85,25',
      scope: 'Servidor y web locales; no acredita Android ni aceptación de metodología por el profesor' }, null, 2))
    console.log(`Recorridos aprobados: ${steps.length}; errores JavaScript: ${errors.length}`)
  } finally { await browser.close() }
})().catch(error => { console.error(error); process.exitCode = 1 })
