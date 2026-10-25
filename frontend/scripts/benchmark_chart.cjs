const fs = require('node:fs')
const { chromium } = require('playwright')
;(async () => {
  const access = JSON.parse(fs.readFileSync(process.env.BENCHMARK_ACCESS_FILE, 'utf8'))
  const browser = await chromium.launch({ headless: true, executablePath: process.env.BENCHMARK_BROWSER_EXECUTABLE })
  try {
    const context = await browser.newContext()
    await context.addInitScript(data => {
      localStorage.setItem('access_token', data.access)
      localStorage.setItem('user', JSON.stringify(data.user))
    }, access)
    const page = await context.newPage()
    const errors = []
    page.on('pageerror', error => errors.push(error.message))
    await page.goto(`${process.env.BENCHMARK_FRONTEND_URL || 'http://127.0.0.1:55173'}/company/T000`)
    await page.getByRole('button', { name: '5 años', exact: true }).waitFor()
    const start = performance.now()
    const responsePromise = page.waitForResponse(response => response.url().includes('/history/?range=5Y'))
    await page.getByRole('button', { name: '5 años', exact: true }).click()
    const response = await responsePromise
    if (!response.ok()) throw new Error(`API: ${response.status()}`)
    const data = await response.json()
    if (data.history.length <= 1000) throw new Error('La serie debe superar 1000 puntos')
    await page.locator('.highcharts-series path.highcharts-graph').first().waitFor({ state: 'visible' })
    const elapsed = performance.now() - start
    if (errors.length) throw new Error(errors.join('; '))
    const result = { points: data.history.length, elapsed_ms: Number(elapsed.toFixed(2)),
      browser: browser.version(), url: '/company/T000', budget_ms: 2000, passed: elapsed < 2000 }
    fs.writeFileSync(process.env.BENCHMARK_OUTPUT || 'chart_benchmark.json', JSON.stringify(result, null, 2))
    console.log(JSON.stringify(result))
    if (!result.passed) process.exitCode = 1
    await page.screenshot({ path: process.env.BENCHMARK_SCREENSHOT || 'chart_benchmark.png', fullPage: true })
  } finally { await browser.close() }
})().catch(error => { console.error(error.message); process.exitCode = 1 })
