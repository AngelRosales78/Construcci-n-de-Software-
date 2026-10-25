// Run only against an isolated backend and a local test SMTP mailbox.
const fs = require('node:fs')
const assert = require('node:assert/strict')
const { execFileSync } = require('node:child_process')
const { chromium } = require('playwright')

const web = process.env.CLOSURE_WEB_URL || 'http://127.0.0.1:55180'
const api = process.env.CLOSURE_API_URL || 'http://127.0.0.1:58200/api/v1'
const mailbox = process.env.CLOSURE_MAILBOX
const python = process.env.CLOSURE_PYTHON
assert(mailbox && python, 'Provide a local test mailbox and Python interpreter.')
assert(['127.0.0.1','localhost'].includes(new URL(api).hostname), 'Use an isolated local API.')

;(async () => {
  const browser = await chromium.launch({ executablePath: 'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe', headless: true })
  const steps = [], errors = []
  try {
    const context = await browser.newContext()
    const page = await context.newPage()
    context.on('page', p => p.on('pageerror', e => errors.push(e.message)))
    page.on('pageerror', e => errors.push(e.message))
    const username = `cierre${Date.now()}`
    const email = `${username}@example.test`
    const previous = 'Anterior!Clave2026', next = 'Nueva!Clave2026'
    await page.goto(`${web}/register`)
    for (const [id, value] of Object.entries({firstName:'Prueba',lastName:'Local',username,email,password:previous,passwordConfirm:previous})) {
      await page.locator(`#${id}`).fill(value)
    }
    await page.getByRole('button',{name:'Crear cuenta',exact:true}).click()
    await page.waitForURL('**/dashboard')
    steps.push('Registro web crea cuenta y abre una sesión válida')
    const saved = await page.evaluate(() => ({access:localStorage.getItem('access_token'),refresh:localStorage.getItem('refresh_token')}))
    const second = await context.newPage()
    await second.goto(`${web}/dashboard`)
    await second.getByRole('link',{name:'Explorar empresas',exact:true}).click()
    await second.waitForURL('**/search')
    await page.getByRole('button',{name:'Cerrar Sesión',exact:true}).click()
    await page.waitForURL('**/login')
    await second.waitForURL('**/login')
    assert.equal(await second.locator('.workspace-shell').count(),0)
    await second.goBack()
    await second.waitForURL('**/login')
    await second.goto(`${web}/company/MSFT`)
    await second.waitForURL('**/login')
    assert.equal(await second.locator('.workspace-shell').count(),0)
    assert.equal((await context.request.get(`${api}/auth/profile/`,{headers:{Authorization:`Bearer ${saved.access}`}})).status(),401)
    assert.equal((await context.request.post(`${api}/auth/refresh/`,{data:{refresh:saved.refresh}})).status(),401)
    steps.push('Logout en una pestaña cierra la otra; Atrás y acceso directo no abren pantallas privadas; tokens revocados')
    await page.goto(`${web}/forgot-password`)
    await page.locator('#email').fill(email)
    await page.getByRole('button',{name:'Solicitar enlace',exact:true}).click()
    await page.getByText(/Solicitud recibida/).waitFor()
    const parseMail = String.raw`from pathlib import Path
from email import policy
from email.parser import BytesParser
import re,sys
for p in Path(sys.argv[1]).glob('*.eml'):
 m=BytesParser(policy=policy.default).parsebytes(p.read_bytes())
 if m['To']==sys.argv[2]:
  print(re.search(r'http://[^\s]+/reset-password/[^\s]+',m.get_body(preferencelist=('plain',)).get_content()).group(0));break`
    const link = execFileSync(python,['-c',parseMail,mailbox,email],{encoding:'utf8'}).trim()
    assert(link.startsWith(`${web}/reset-password/`),'SMTP message must contain the configured frontend link')
    await page.goto(link)
    await page.getByRole('heading',{name:'Nueva Contraseña',exact:true}).waitFor()
    await page.locator('#password').fill(next)
    await page.locator('#passwordConfirm').fill(next)
    await page.getByRole('button',{name:'Restablecer Contraseña',exact:true}).click()
    await page.getByText(/Contraseña restablecida exitosamente/).waitFor()
    await page.waitForURL('**/login')
    steps.push('Formulario solicita correo; SMTP entrega enlace; enlace abre el formulario y cambia la clave')
    const response = page.waitForResponse(r=>r.url().endsWith('/auth/token/') && r.request().method()==='POST')
    await page.locator('#username').fill(email)
    await page.locator('#password').fill(previous)
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click()
    assert.equal((await response).status(),401)
    assert(new URL(page.url()).pathname==='/login')
    await page.locator('#password').fill(next)
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click()
    await page.waitForURL('**/dashboard')
    await page.reload()
    await page.getByRole('button',{name:'Cerrar Sesión',exact:true}).waitFor()
    steps.push('La contraseña anterior falla; la nueva permite entrar por correo y conservar la sesión al recargar')
    await page.getByRole('button',{name:'Cerrar Sesión',exact:true}).click()
    await page.waitForURL('**/login')
    await page.goto(link)
    await page.locator('#password').fill('Tercera!Clave2026')
    await page.locator('#passwordConfirm').fill('Tercera!Clave2026')
    await page.getByRole('button',{name:'Restablecer Contraseña',exact:true}).click()
    await page.getByText('Invalid or expired token.').waitFor()
    steps.push('El enlace recibido por SMTP no se puede reutilizar')
    await page.goto(`${web}/login`)
    await page.locator('#username').fill(email)
    await page.locator('#password').fill(next)
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click()
    await page.waitForURL('**/dashboard')
    await second.goto(`${web}/dashboard`)
    let releaseRefresh, rotated
    let signalReady
    const refreshReady = new Promise(resolve => {signalReady=resolve})
    await second.route('**/auth/profile/',route=>route.fulfill({status:401,contentType:'application/json',body:JSON.stringify({detail:'expired'})}))
    await second.route('**/auth/refresh/',async route=>{
      const response=await route.fetch()
      assert.equal(response.status(),200)
      rotated=await response.json()
      signalReady()
      await new Promise(resolve=>{releaseRefresh=resolve})
      await route.fulfill({response})
    })
    const inFlight=second.evaluate(async()=>{
      const {default:client}=await import('/src/services/api.js')
      try {await client.get('/auth/profile/')} catch { /* Expected: session closed while response was pending. */ }
      return {access:localStorage.getItem('access_token'),refresh:localStorage.getItem('refresh_token')}
    })
    await refreshReady
    await page.getByRole('button',{name:'Cerrar Sesión',exact:true}).click()
    await page.waitForURL('**/login')
    await second.waitForURL('**/login')
    releaseRefresh()
    assert.deepEqual(await inFlight,{access:null,refresh:null})
    assert.equal((await context.request.get(`${api}/auth/profile/`,{headers:{Authorization:`Bearer ${rotated.access}`}})).status(),401)
    assert.equal((await context.request.post(`${api}/auth/refresh/`,{data:{refresh:rotated.refresh}})).status(),401)
    steps.push('Una renovación pendiente en otra pestaña no revive la sesión; el servidor rechaza también sus tokens rotados')
    assert.equal(errors.length,0,errors.join('\n'))
    const result = {passed:true,steps,consoleErrors:errors,scope:'SMTP local; no se certifica entrega de proveedor externo ni se envían correos externos'}
    if(process.env.CLOSURE_REPORT)fs.writeFileSync(process.env.CLOSURE_REPORT,JSON.stringify(result,null,2))
    console.log(JSON.stringify(result,null,2))
  } finally {await browser.close()}
})().catch(error=>{console.error(error);process.exitCode=1})
