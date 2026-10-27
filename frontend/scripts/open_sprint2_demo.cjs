// Cuenta y perfil de Chrome exclusivos de la demostración; permanecen fuera de Git.
const fs = require('node:fs')
const path = require('node:path')
const { chromium } = require('playwright')
const root = path.resolve(__dirname, '../../..')
const runtime = path.join(root, 'presentation_runtime_sprint2')

;(async () => {
  const credentials = JSON.parse(fs.readFileSync(path.join(runtime, 'demo_credentials.json'), 'utf8'))
  const context = await chromium.launchPersistentContext(path.join(runtime, 'chrome-profile'), {
    executablePath: 'C:/Program Files/Google/Chrome/Application/chrome.exe',
    headless: false, viewport: { width: 1440, height: 1000 },
  })
  const page = context.pages()[0] || await context.newPage()
  await page.goto('http://127.0.0.1:55183/login')
  if (page.url().includes('/login')) {
    await page.locator('#username').fill(credentials.username)
    await page.locator('#password').fill(credentials.password)
    await page.getByRole('button', { name: 'Iniciar sesión', exact: true }).click()
    await page.waitForURL('**/dashboard')
  }
  await page.goto('http://127.0.0.1:55183/company/MSFT')
  await page.getByRole('button', { name: 'Resumen', exact: true }).waitFor()
  await page.bringToFront()
  console.log('Demostración abierta en Google Chrome: MSFT, pilares, simulaciones e IA local.')
  await context.waitForEvent('close', { timeout: 0 })
})().catch(error => { console.error(error.message); process.exitCode = 1 })
