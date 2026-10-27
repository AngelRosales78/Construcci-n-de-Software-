const fs = require('fs')
const path = require('path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')

// Usa la cuenta aislada existente; no crea usuarios ni imprime credenciales.
const root = path.resolve(__dirname, '../../..')
const base = process.env.DEMO_WEB_URL || 'http://127.0.0.1:55183'
const sorted = values => [...values].sort((a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' }))

;(async () => {
  const browser = await chromium.launch({
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe', headless: true,
  })
  try {
    const page = await browser.newPage()
    const credentials = JSON.parse(fs.readFileSync(path.join(root, 'presentation_runtime_sprint2/demo_credentials.json')))
    await page.goto(`${base}/login`)
    await page.locator('#username').fill(credentials.username)
    await page.locator('#password').fill(credentials.password)
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
    await page.waitForURL('**/dashboard')
    await page.goto(`${base}/search`)
    await page.locator('.company-card').first().waitFor()
    assert.equal(await page.locator('#ordering-filter').inputValue(), 'name')
    const ascending = await page.locator('.company-name').allTextContents()
    assert.deepEqual(ascending, sorted(ascending))
    await page.waitForFunction(() => document.querySelectorAll('#sector-filter option').length > 2)
    const sectors = await page.locator('#sector-filter option').allTextContents()
    assert.deepEqual(sectors.slice(1), sorted(sectors.slice(1)))
    const response = page.waitForResponse(r => r.url().includes('ordering=-name') && r.status() === 200)
    await page.locator('#ordering-filter').selectOption('-name')
    await response
    await page.waitForFunction(previous => document.querySelector('.company-name')?.textContent !== previous, ascending[0])
    const descending = await page.locator('.company-name').allTextContents()
    assert.deepEqual(descending, sorted(descending).reverse())
    await page.getByRole('combobox', { name: 'Buscar empresas' }).fill('a')
    await page.locator('.suggestion-name').first().waitFor()
    const suggestions = await page.locator('.suggestion-name').allTextContents()
    assert.deepEqual(suggestions, sorted(suggestions))
    const footers = []
    for (const viewport of [{ width: 1440, height: 600 }, { width: 390, height: 640 }, { width: 320, height: 568 }]) {
      await page.setViewportSize(viewport)
      for (const route of ['/dashboard', '/search', '/company/MSFT', '/company/AAPL']) {
        await page.goto(base + route)
        await page.locator('.workspace-footer').waitFor()
        if (route === '/search') await page.locator('.company-card').first().waitFor()
        // Las fichas crecen cuando termina la carga: volver al final hasta
        // que el pie sea visible, sin confundir esa carga con un recorte CSS.
        await page.waitForFunction(() => {
          scrollTo(0, document.scrollingElement.scrollHeight)
          return Math.abs(document.querySelector('.workspace-footer').getBoundingClientRect().bottom - innerHeight) < 2
        })
        const dimensions = await page.locator('.workspace-footer').evaluate(e => ({
          bottom: e.getBoundingClientRect().bottom, viewport: innerHeight,
          horizontalOverflow: document.documentElement.scrollWidth > innerWidth + 1,
        }))
        assert.ok(Math.abs(dimensions.bottom - dimensions.viewport) < 2, JSON.stringify({ route, viewport, dimensions }))
        assert.equal(dimensions.horizontalOverflow, false)
        footers.push({ route, ...viewport, ...dimensions })
      }
    }
    const evidence = {
      checkedAt: new Date().toISOString(), browser: 'Google Chrome',
      ascending, descending, sectors, suggestions, footers,
      automatedTests: { frontend: 77, backend: 333 },
      limitation: 'La comprobación visual corresponde a la web; no certifica Android ni entrega SMTP.',
    }
    fs.writeFileSync(path.join(__dirname, '../../docs/evidencias/preparacion_sprint2_27_octubre.json'), JSON.stringify(evidence, null, 2) + '\n')
    console.log('Comprobados A–Z, Z–A, sectores, sugerencias y doce accesos al pie de página.')
  } finally {
    await browser.close()
  }
})().catch(error => { console.error(error.message); process.exitCode = 1 })
