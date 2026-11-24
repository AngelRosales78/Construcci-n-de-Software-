const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const repo=path.resolve(__dirname,'..')
const {chromium}=require(require.resolve('playwright',{paths:[path.join(repo,'frontend')]}))
const output=path.resolve(repo,'../output/mercadopago')
const api='http://127.0.0.1:58020/api/v1',web='http://127.0.0.1:55193'
;(async()=>{
 fs.mkdirSync(output,{recursive:true})
 const account=JSON.parse(fs.readFileSync(path.resolve(repo,'../presentation_runtime_sprint3/demo_credentials.json'),'utf8').replace(/^\uFEFF/,'')).free
 const browser=await chromium.launch({channel:'msedge',headless:true})
 const errors=[],checks=[]
 try{
  const context=await browser.newContext({viewport:{width:1440,height:1000}})
  const login=await context.request.post(api+'/auth/token/',{data:account});assert.equal(login.status(),200)
  const access=(await login.json()).access,headers={Authorization:'Bearer '+access}
  const plansResponse=await context.request.get(api+'/plans/',{headers});assert.equal(plansResponse.status(),200)
  const plans=await plansResponse.json();assert.equal(plans.payment_provider,'mercadopago_test');assert.equal(plans.payment.ready,false)
  let response=await context.request.post(api+'/user/subscription/checkout/',{headers,data:{request_id:'00000000-0000-4000-8000-000000000001',outcome:'approved'}})
  assert.equal(response.status(),400)
  response=await context.request.post(api+'/user/subscription/checkout/',{headers,data:{request_id:'00000000-0000-4000-8000-000000000001'}})
  assert.equal(response.status(),503)
  checks.push('API real sin credenciales: configuración pendiente; no acepta aprobación desde el cliente ni crea un checkout.')
  const page=await context.newPage();page.on('pageerror',e=>errors.push(e.message))
  await page.goto(web+'/login');await page.locator('#username').fill(account.username);await page.locator('#password').fill(account.password)
  await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await page.waitForURL('**/dashboard')
  await page.goto(web+'/profile/subscription')
  await page.getByRole('heading',{name:'Pago de prueba pendiente de conexión'}).waitFor()
  assert.equal(await page.getByRole('button',{name:/^(Pago aún no disponible|Pro vigente)$/}).isDisabled(),true)
  for(const width of [1440,390]){await page.setViewportSize({width,height:1000});assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth),false);await page.screenshot({path:path.join(output,'pendiente-'+width+'.png'),fullPage:true})}
  checks.push('Página real: modo de prueba visible, sin activación directa y sin desbordamiento en escritorio y teléfono.')

  // Contract fixtures exercise the browser only. They do not contact Mercado
  // Pago, activate real accounts, or claim a completed provider transaction.
  const id='00000000-0000-4000-8000-000000000002'
  let order=null,paid=false
  const free={effective_plan:'free',active:true,expires_at:null}
  const fixtureOrder={id,request_id:id,status:'ready',status_label:'Listo para pagar',amount:'9.90',currency:'PEN',duration_days:30,checkout_url:'https://www.mercadopago.com.pe/checkout/v1/redirect?order_id=ORDTST01BROWSER'}
  await page.route('**/api/v1/plans/',route=>route.fulfill({json:{...plans,payment:{ready:true}}}))
  await page.route('**/api/v1/user/subscription/history/',route=>route.fulfill({json:{subscription:paid?{...free,effective_plan:'pro'}:free,receipts:[],payment_orders:order?[order]:[]}}))
  await page.route('**/api/v1/user/subscription/checkout/',route=>{
    assert.deepEqual(Object.keys(route.request().postDataJSON()),['request_id'])
    order={...fixtureOrder};return route.fulfill({json:{payment:order,subscription:free}})
  })
  await page.route('**/api/v1/user/subscription/payment-orders/'+id+'/',route=>{
    order={...fixtureOrder,status:paid?'approved':'pending',status_label:paid?'Pago de prueba aprobado':'Pendiente de confirmación',checkout_url:paid?'':fixtureOrder.checkout_url}
    return route.fulfill({json:{payment:order,subscription:paid?{...free,effective_plan:'pro'}:free}})
  })
  await page.reload()
  await page.getByRole('button',{name:'Elegir Pro',exact:true}).click()
  await page.getByRole('button',{name:'Preparar pago de prueba',exact:true}).click()
  const checkout=page.getByRole('link',{name:'Continuar en Mercado Pago ↗'})
  await checkout.waitFor();assert.equal(await checkout.getAttribute('href'),fixtureOrder.checkout_url)
  await page.getByRole('button',{name:'Verificar estado del pago',exact:true}).click()
  await page.getByRole('heading',{name:'Pendiente de confirmación',exact:true}).waitFor()
  await page.getByRole('heading',{name:'Plan Free',exact:true}).waitFor()
  await page.screenshot({path:path.join(output,'fixture-pendiente.png'),fullPage:true})
  paid=true
  await page.getByRole('button',{name:'Verificar estado del pago',exact:true}).click()
  await page.getByRole('heading',{name:'Plan Pro',exact:true}).waitFor()
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth),false)
  await page.screenshot({path:path.join(output,'fixture-aprobado.png'),fullPage:true})
  checks.push('Contrato de interfaz con respuestas controladas: checkout externo, pendiente mantiene Free y aprobación confirmada actualiza Pro; no se abrió un pago externo.')
  assert.deepEqual(errors,[])
  fs.writeFileSync(path.join(output,'resultado.json'),JSON.stringify({verified_at:new Date().toISOString(),checks,page_errors:errors,provider_payment_executed:false},null,2))
  process.stdout.write(checks.join('\n')+'\n')
 }finally{await browser.close()}
})().catch(error=>{process.stderr.write(String(error)+'\n');process.exitCode=1})
