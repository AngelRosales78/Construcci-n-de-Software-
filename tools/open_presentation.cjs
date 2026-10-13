const path = require('node:path')
const fs = require('node:fs')
const repo = path.resolve(__dirname, '..')
const { chromium } = require(require.resolve('playwright', { paths: [path.join(repo, 'frontend')] }))
;(async () => {
 const data = JSON.parse(fs.readFileSync(process.env.PRESENTATION_ACCESS_FILE, 'utf8'))
 const context = await chromium.launchPersistentContext(process.env.PRESENTATION_PROFILE, {
   executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: false,
 })
 await context.addInitScript(access => {
   localStorage.setItem('access_token', access.access)
   localStorage.setItem('user', JSON.stringify(access.user))
 }, data)
 const page = context.pages()[0] || await context.newPage()
 await page.goto('http://127.0.0.1:55173/search')
 await page.getByText('Empresa de prueba 499', { exact: true }).waitFor()
 console.log('Demo de busqueda abierta y verificada; sesion preparada.')
 await context.waitForEvent('close', { timeout: 0 })
})().catch(error => { console.error(error.message); process.exitCode = 1 })
