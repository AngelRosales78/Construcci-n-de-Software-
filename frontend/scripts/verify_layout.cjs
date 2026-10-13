const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const { chromium } = require('playwright')
;(async () => {
  const credentials = JSON.parse(fs.readFileSync(process.env.PRESENTATION_CREDENTIALS_FILE, 'utf8'))
  const output = process.env.LAYOUT_OUTPUT
  fs.mkdirSync(output, { recursive: true })
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 } })
    const errors = [], checks = []
    page.on('pageerror', error => errors.push(error.message))
    for (const [device, width, height] of [['escritorio', 1440, 1000], ['movil', 390, 844]]) {
      await page.setViewportSize({ width, height })
      for (const route of ['login', 'register']) {
        await page.goto(`http://127.0.0.1:55173/${route}`)
        await page.locator('.auth-tabs').waitFor()
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)
        assert.equal(overflow, false, `${device}: ${route} tiene desbordamiento horizontal`)
        const file = `interfaz_${device}_${route}.png`
        await page.screenshot({ path: path.join(output, file), fullPage: true })
        checks.push({ device, width, route, horizontal_overflow: overflow, screenshot: file })
      }
    }
    await page.setViewportSize({ width: 1440, height: 1000 })
    await page.goto('http://127.0.0.1:55173/login')
    await page.locator('#username').fill(credentials.username)
    await page.locator('#password').fill(credentials.password)
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
    await page.waitForURL('**/dashboard')
    for (const [device, width, height] of [['escritorio', 1440, 1000], ['movil', 390, 844]]) {
      await page.setViewportSize({ width, height })
      for (const [route, ready] of [['search', '.company-card'], ['company/MSFT', '.highcharts-graph'], ['dashboard', '.onboarding-grid']]) {
        await page.goto(`http://127.0.0.1:55173/${route}`)
        await page.locator(ready).first().waitFor({ state: 'visible' })
        if (route === 'search') {
          await page.getByRole('region', { name: 'Búsquedas recientes' }).waitFor()
          await page.getByRole('img', { name: 'Logo de Microsoft', exact: true }).waitFor()
          for (const image of await page.locator('.company-card img').all()) {
            await image.scrollIntoViewIfNeeded()
          }
          await page.waitForFunction(() => [...document.querySelectorAll('.company-card img')].every(image => image.complete && image.naturalWidth > 0))
          await page.evaluate(() => window.scrollTo(0, 0))
        }
        if (route.startsWith('company/')) await page.locator('.recharts-pie .recharts-sector').first().waitFor({ state: 'visible' })
        await page.evaluate(() => document.fonts.ready)
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1)
        assert.equal(overflow, false, `${device}: ${route} tiene desbordamiento horizontal`)
        const file = `interfaz_${device}_${route.replace('/', '_')}.png`
        await page.screenshot({ path: path.join(output, file), fullPage: route.startsWith('company/') })
        checks.push({ device, width, route, horizontal_overflow: overflow, screenshot: file })
      }
    }
    assert.deepEqual(errors, [])
    const report = { recorded_at: new Date().toISOString(), passed: true, checks, page_errors: errors }
    fs.writeFileSync(path.join(output, 'interfaz_validacion.json'), JSON.stringify(report, null, 2))
    console.log(JSON.stringify(report))
  } finally { await browser.close() }
})().catch(error => { console.error(error.stack); process.exitCode = 1 })
