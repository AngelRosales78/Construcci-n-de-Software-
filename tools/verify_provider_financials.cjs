const fs = require('node:fs')
const path = require('node:path')
const assert = require('node:assert/strict')
const repo = path.resolve(__dirname, '..')
const { chromium } = require(require.resolve('playwright', { paths: [path.join(repo, 'frontend')] }))
const output = path.resolve(repo, '../output/provider_financials')
const api = 'http://127.0.0.1:58020/api/v1', web = 'http://127.0.0.1:55193'

;(async () => {
  fs.mkdirSync(output, { recursive: true })
  const state = path.resolve(repo, '../presentation_runtime_sprint3')
  const accounts = JSON.parse(fs.readFileSync(path.join(state,'demo_credentials.json'),'utf8').replace(/^\uFEFF/,''))
  const administrator = JSON.parse(fs.readFileSync(path.join(state,'sprint4_admin_credentials.json'),'utf8').replace(/^\uFEFF/,''))
  const browser = await chromium.launch({ channel:'msedge',headless:true })
  const checks = [], errors = [], statistics = { companies:0, with_annuals:0, with_summary:0 }
  try {
    const context = await browser.newContext({ viewport:{width:1440,height:1000} })
    const login = await context.request.post(api+'/auth/token/',{data:administrator})
    assert.equal(login.status(),200)
    const admin = await login.json(), headers = {Authorization:'Bearer '+admin.access}
    let next = api+'/admin/companies/'
    while(next) {
      const response = await context.request.get(next,{headers}); assert.equal(response.status(),200)
      const data = await response.json()
      statistics.companies += data.results.length
      statistics.with_annuals += data.results.filter(company=>company.coverage.fiscal_years.length).length
      statistics.with_summary += data.results.filter(company=>company.coverage.summary_fields>0).length
      next = data.next
    }
    assert.deepEqual(statistics,{companies:504,with_annuals:504,with_summary:504})
    const page = await context.newPage()
    page.on('pageerror',error=>errors.push(error.message))
    async function signIn(account) {
      await page.goto(web+'/login')
      await page.locator('#username').fill(account.username); await page.locator('#password').fill(account.password)
      await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click(); await page.waitForURL('**/dashboard')
    }
    async function screenshot(name) {
      assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth),false,'Desbordamiento '+name)
      await page.screenshot({path:path.join(output,name+'.png'),fullPage:true})
    }
    await signIn(accounts.free)
    for(const ticker of ['AAPL','AMZN','JPM','WMT','CAT','KO','MCD','FDXF','VYLR','HIMS']) {
      const resource = page.waitForResponse(response=>response.url().includes(`/companies/${ticker}/summary/`)&&response.status()===200)
      await page.goto(web+'/company/'+ticker)
      const summary = await (await resource).json()
      assert.equal(summary.reference_basis,'provider_normalized')
      assert.ok(summary.market_cap>0)
      for(const field of ['market_cap','per','roe','beta','dividend_yield']) {
        if(summary[field]!=null) assert.ok(summary.field_sources[field]?.as_of && /^https:\/\//.test(summary.field_sources[field].source))
      }
      if (['AAPL','AMZN','WMT','CAT','KO'].includes(ticker)) {
        assert.ok(summary.altman_z.score > 0)
        assert.ok(summary.altman_z.reference.period_start && summary.altman_z.reference.price_date)
      } else assert.equal(summary.altman_z.score,null)
      for (const key of ['market_cap','per','roe','beta','dividend_yield']) {
        assert.ok(summary.indicator_availability[key]?.status)
        if(summary[key]==null) assert.ok(summary.indicator_availability[key].explanation)
      }
      if(summary.altman_z.score==null) assert.ok(summary.altman_z.availability.explanation)
      await page.getByText('Referencia anual del proveedor',{exact:true}).waitFor()
      const absenceLabels=await page.locator('.metric-unavailable').allTextContents()
      for(const key of ['market_cap','per','roe','beta','dividend_yield']) {
        if(summary[key]==null) assert.ok(absenceLabels.includes(summary.indicator_availability[key].label),'Falta motivo visible de '+key)
      }
      if(summary.altman_z.score==null) assert.equal(await page.locator('#company-overview .financial-health>strong').textContent(),summary.altman_z.availability.label)
      if(ticker==='AAPL') {
        await screenshot('01-apple-indicadores')
        await page.getByRole('button',{name:'Salud financiera',exact:true}).click()
        await page.getByText('Ver datos y fuentes de Altman-Z',{exact:true}).click()
        await page.getByRole('link',{name:'Consultar informe oficial',exact:true}).waitFor()
      }
      await page.getByRole('button',{name:'Salud financiera',exact:true}).click()
      await page.locator('.financial-pillars>.provider-period-note').waitFor()
      const annualResponse = await context.request.get(api+`/companies/${ticker}/pillars/`,{headers})
      const annual = await annualResponse.json()
      assert.ok(annual.available_years.length)
      assert.equal(annual.period.start,null)
      assert.equal(annual.period_basis,'provider_normalized')
      assert.ok(annual.annual_components.some(component=>component.value!=null))
      for(const component of annual.annual_components) {
        assert.equal(component.source.filed,null)
        assert.equal(component.source.accession,'')
        assert.equal(component.source.source_kind,'yahoo')
      }
      await page.getByText('Ver estados anuales importados',{exact:true}).click()
      await page.locator('.annual-statement table').waitFor()
      if(ticker==='AAPL'||ticker==='JPM') await screenshot(ticker+'-estados-anuales')
      await page.setViewportSize({width:390,height:1000})
      if(ticker==='AAPL') await screenshot('03-apple-ventana-estrecha')
      else assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth),false)
      await page.setViewportSize({width:1440,height:1000})
      if(ticker==='AAPL') {
        await page.getByText('Exportar reporte PDF de esta empresa',{exact:true}).click()
        await page.getByRole('checkbox',{name:'Incluir gráficos vectoriales del rango activo'}).uncheck()
        await page.getByRole('button',{name:'Solicitar PDF',exact:true}).click()
        await page.getByRole('button',{name:'Descargar PDF verificado',exact:true}).waitFor({timeout:90000})
        const download = page.waitForEvent('download')
        await page.getByRole('button',{name:'Descargar PDF verificado',exact:true}).click()
        await (await download).saveAs(path.join(output,'AAPL-reporte.pdf'))
        checks.push('PDF individual de Apple generado y descargado con las nuevas referencias.')
      }
    }
    checks.push('504 empresas con estados anuales e indicadores; diez fichas revisadas en escritorio y ventana estrecha, con diagnóstico de los valores ausentes.')
    checks.push('Las referencias normalizadas conservan fechas ausentes; Altman-Z usa referencias fiscales independientes en Apple, Amazon, Walmart, Caterpillar y Coca-Cola.')
    await page.getByRole('button',{name:'Cerrar Sesión',exact:true}).click(); await page.waitForURL('**/login')
    await signIn(accounts.pro)
    let response = page.waitForResponse(response=>new URL(response.url()).pathname==='/api/v1/compare/'&&response.status()===200)
    await page.goto(web+'/compare?a=AMZN&b=JPM&fiscal_year=2025')
    const normalized = await (await response).json()
    assert.ok(normalized.comparisons.every(row=>row.status==='not_comparable'))
    await page.getByRole('heading',{name:'AMZN y JPM · FY2025'}).waitFor(); await screenshot('04-comparacion-normalizada')
    response = page.waitForResponse(response=>new URL(response.url()).pathname==='/api/v1/compare/'&&response.status()===200)
    await page.goto(web+'/compare?a=GOOGL&b=META&fiscal_year=2024')
    const verified = await (await response).json()
    assert.ok(verified.comparisons.some(row=>row.status==='available'))
    assert.ok(verified.a.period.start && verified.b.period.start)
    checks.push('Las referencias normalizadas no declaran ganadores entre empresas; GOOGL/META FY2024 conserva la comparación con períodos oficiales completos.')
    await context.request.post(api+'/auth/logout/',{data:{refresh_token:admin.refresh},headers})
    assert.deepEqual(errors,[])
    fs.writeFileSync(path.join(output,'resultado.json'),JSON.stringify({verified_at:new Date().toISOString(),statistics,checks,page_errors:errors,android_changed:false},null,2))
    checks.forEach(check=>process.stdout.write(check+'\n'))
  } finally { await browser.close() }
})().catch(error=>{process.stderr.write(error.stack+'\n');process.exitCode=1})
