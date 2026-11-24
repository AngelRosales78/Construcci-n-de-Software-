const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const repo = path.resolve(__dirname, '..')
const { chromium } = require(require.resolve('playwright', { paths: [path.join(repo, 'frontend')] }))
const web = 'http://127.0.0.1:55193'
const api = 'http://127.0.0.1:58020/api/v1'
const output = path.resolve(repo, '../output/catalog_enrichment')

;(async () => {
  fs.mkdirSync(output, { recursive: true })
  const accounts = JSON.parse(fs.readFileSync(path.resolve(repo, '../presentation_runtime_sprint3/demo_credentials.json'), 'utf8').replace(/^\uFEFF/, ''))
  const administrator = JSON.parse(fs.readFileSync(path.resolve(repo, '../presentation_runtime_sprint3/sprint4_admin_credentials.json'), 'utf8').replace(/^\uFEFF/, ''))
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = [], checks = []
  try {
    const context = await browser.newContext({ viewport: { width: 1440, height: 1000 } })
    const sessionResponse = await context.request.post(api + '/auth/token/', { data: administrator })
    assert.equal(sessionResponse.status(), 200)
    const session = await sessionResponse.json()
    const headers = { Authorization: 'Bearer ' + session.access }
    let next = api + '/admin/companies/', companies = []
    while (next) {
      const response = await context.request.get(next, { headers })
      assert.equal(response.status(), 200)
      const result = await response.json()
      companies.push(...result.results)
      next = result.next
    }
    assert.equal(companies.length, 504)
    assert.equal(new Set(companies.map(company => company.ticker)).size, 504)
    assert.equal(companies.filter(company => company.description.trim()).length, 504)
    assert.equal(companies.filter(company => /^https:\/\//.test(company.website_url)).length, 504)
    const withPrices = companies.filter(company => company.coverage.price_as_of)
    assert.equal(withPrices.length, 504)
    checks.push('504 empresas con información, sitio web HTTPS e histórico de precios; sin símbolos repetidos.')
    const page = await context.newPage()
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(web + '/login')
    await page.locator('#username').fill(accounts.free.username)
    await page.locator('#password').fill(accounts.free.password)
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
    await page.waitForURL('**/dashboard')
    for (const ticker of ['AAPL', 'MMM', 'XOM', 'ZTS', 'HIMS']) {
      const response = page.waitForResponse(response => response.url().includes(`/companies/${ticker}/summary/`) && response.status() === 200)
      await page.goto(web + '/company/' + ticker)
      const company = await (await response).json()
      await page.getByRole('heading', { name: 'Acerca de ' + company.name }).waitFor()
      assert.ok(company.description && company.website_url && company.quote)
      await page.locator('.highcharts-series').first().waitFor()
      await page.locator('.company-profile-sources>summary').click()
      await page.getByRole('link', { name: /Consultar informes de la empresa en SEC/ }).waitFor()
      if (ticker !== 'HIMS') {
        assert.ok(company.catalog_details.headquarters && company.catalog_details.founded)
      }
      assert.equal(company.per, null)
      assert.equal(company.roe, null)
      assert.equal(company.altman_z.zone, 'unknown')
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: 1000 })
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth), false)
        if (ticker === 'AAPL' || ticker === 'HIMS') await page.screenshot({ path: path.join(output, ticker + '-' + width + '.png'), fullPage: true })
      }
      if (ticker === 'AAPL') {
        await page.getByRole('button', { name: 'Simulaciones', exact: true }).click()
        const simulationResponse = page.waitForResponse(response => response.url().includes('/companies/AAPL/simulations/') && response.status() === 200)
        await page.getByRole('button', { name: 'Generar escenarios', exact: true }).click()
        const simulation = await (await simulationResponse).json()
        assert.equal(simulation.status, 'available')
        assert.ok(simulation.observations >= 253)
        checks.push('La simulación de Apple utiliza el histórico recién cargado; no necesita inventar un balance.')
      }
      await page.setViewportSize({ width: 1440, height: 1000 })
    }
    checks.push('Apple, 3M, ExxonMobil, Zoetis y Hims: perfiles, fuentes, gráfico y lectura en ventanas pequeñas verificados.')
    checks.push('PER, ROE y Altman-Z siguen ausentes cuando no hay balances válidos; no se rellenan con cero ni con estimaciones.')
    assert.deepEqual(errors, [])
    await context.request.post(api + '/auth/logout/', { data: { refresh_token: session.refresh }, headers })
    fs.writeFileSync(path.join(output, 'resultado.json'), JSON.stringify({ verified_at: new Date().toISOString(),
      companies: companies.length, descriptions: 504, websites: 504, companies_with_prices: withPrices.length,
      companies_with_summary: companies.filter(company => company.coverage.summary_fields > 0).length,
      companies_with_annuals: companies.filter(company => company.coverage.fiscal_years.length).length,
      checks, page_errors: errors, android_changed: false }, null, 2))
    checks.forEach(check => process.stdout.write(check + '\n'))
  } finally { await browser.close() }
})().catch(error => { process.stderr.write(error.stack + '\n'); process.exitCode = 1 })
