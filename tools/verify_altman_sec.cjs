const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict')
const repo = path.resolve(__dirname, '..')
const { chromium } = require(require.resolve('playwright', { paths: [path.join(repo, 'frontend')] }))
const web = 'http://127.0.0.1:55193'

;(async () => {
  const out = path.resolve(repo, '../output/altman-sec-ui')
  fs.mkdirSync(out, { recursive: true })
  const account = JSON.parse(fs.readFileSync(path.resolve(repo, '../presentation_runtime_sprint3/demo_credentials.json'), 'utf8').replace(/^\uFEFF/, '')).free
  const browser = await chromium.launch({ channel: 'msedge', headless: true })
  const errors = [], checked = []
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(web + '/login')
    await page.locator('#username').fill(account.username)
    await page.locator('#password').fill(account.password)
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
    await page.waitForURL('**/dashboard')
    for (const ticker of ['NVDA', 'TSLA', 'IBM', 'COST', 'SBUX', 'ADBE', 'ADP', 'AES']) {
      const response = page.waitForResponse(response => response.url().includes('/companies/' + ticker + '/summary/') && response.status() === 200)
      await page.goto(web + '/company/' + ticker)
      const summary = await (await response).json()
      assert.ok(Number.isFinite(summary.altman_z.score))
      assert.ok(summary.altman_z.reference.sec_evidence)
      const overview = page.locator('#company-overview')
      await overview.locator('.altman-score').waitFor()
      assert.equal(Number(await overview.locator('.altman-score').textContent()), summary.altman_z.score)
      assert.match(await overview.locator('.altman-date').textContent(), new RegExp(summary.altman_z.reference.period_end))
      await page.getByRole('button', { name: 'Salud financiera', exact: true }).click()
      await page.getByText('Ver datos y fuentes de Altman-Z', { exact: true }).click()
      const source = page.getByRole('link', { name: 'Consultar hechos XBRL de la SEC', exact: true })
      await source.waitFor()
      assert.equal(await source.getAttribute('href'), summary.altman_z.reference.sec_evidence.facts_url)
      assert.equal(await page.locator('.altman-sec-evidence code').count(), Object.keys(summary.altman_z.reference.sec_evidence.facts).length)
      for (const width of [1440, 390]) {
        await page.setViewportSize({ width, height: 1000 })
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, ticker + ' desbordamiento ' + width)
        if (ticker === 'NVDA' || ticker === 'IBM') await page.locator('.altman-detail').screenshot({ path: path.join(out, ticker + '-' + width + '.png') })
      }
      await page.setViewportSize({ width: 1440, height: 1000 })
      if (ticker === 'NVDA') {
        await page.getByText('Exportar reporte PDF de esta empresa', { exact: true }).click()
        await page.getByRole('checkbox', { name: 'Incluir gráficos vectoriales del rango activo' }).uncheck()
        await page.getByRole('button', { name: 'Solicitar PDF', exact: true }).click()
        await page.getByRole('button', { name: 'Descargar PDF verificado', exact: true }).waitFor({ timeout: 90000 })
        const download = page.waitForEvent('download')
        await page.getByRole('button', { name: 'Descargar PDF verificado', exact: true }).click()
        await (await download).saveAs(path.join(out, 'NVDA-reporte.pdf'))
      }
      checked.push({ ticker, score: summary.altman_z.score, period_end: summary.altman_z.reference.period_end })
    }
    for (const [ticker, code] of [['JPM', 'sector_model_review'], ['GOOGL', 'share_classes_review']]) {
      const response = page.waitForResponse(response => response.url().includes('/companies/' + ticker + '/summary/') && response.status() === 200)
      await page.goto(web + '/company/' + ticker)
      const summary = await (await response).json()
      assert.equal(summary.altman_z.score, null)
      assert.equal(summary.altman_z.availability.code, code)
      await page.locator('#company-overview .financial-health').waitFor()
      assert.equal(await page.locator('#company-overview .altman-score').count(), 0)
      assert.equal(await page.locator('#company-overview .altman-review-source').getAttribute('href'), summary.altman_z.availability.source_url)
    }
    assert.deepEqual(errors, [])
    fs.writeFileSync(path.join(out, 'resultado.json'), JSON.stringify({ checked, specific_review_states: true, pdf_downloaded: true, widths: [1440, 390], page_errors: errors }, null, 2))
    process.stdout.write('Ocho puntuaciones SEC verificadas con fuentes, escritorio y teléfono. PDF NVIDIA descargado. JPM y GOOGL conservan el motivo específico de revisión.\n')
  } finally { await browser.close() }
})().catch(error => { process.stderr.write(String(error) + '\n'); process.exitCode = 1 })
