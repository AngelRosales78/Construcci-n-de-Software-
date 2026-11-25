const fs = require('node:fs'), path = require('node:path'), assert = require('node:assert/strict')
const repo = path.resolve(__dirname, '..')
const { chromium } = require(require.resolve('playwright', { paths: [path.join(repo, 'frontend')] }))
const web = 'http://127.0.0.1:55193'

;(async () => {
  const out = path.resolve(repo, '../output/visual-identity')
  fs.mkdirSync(out, { recursive: true })
  const credentials = JSON.parse(fs.readFileSync(path.resolve(repo, '../presentation_runtime_sprint3/demo_credentials.json'), 'utf8').replace(/^\uFEFF/, '')).pro
  const browser = await chromium.launch({channel:'msedge',headless:true})
  const checks = [], errors = [], sizes = []
  try {
    const page = await browser.newPage({viewport:{width:1440,height:1000}})
    page.on('pageerror', error => errors.push(error.message))
    const noOverflow = async name => {
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, name)
    }
    await page.goto(web+'/login')
    await page.locator('#username').fill(credentials.username)
    await page.locator('#password').fill(credentials.password)
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click()
    await page.waitForURL('**/dashboard')
    await page.evaluate(() => document.fonts.ready)
    const fonts = await page.evaluate(() => ({body:getComputedStyle(document.body).fontFamily, heading:getComputedStyle(document.querySelector('h1')).fontFamily, sans:document.fonts.check('16px "Source Sans 3"')}))
    assert.match(fonts.body,/Source Sans 3/)
    assert.match(fonts.heading,/Source Sans 3/)
    assert.ok(fonts.sans)
    assert.equal(await page.locator('.sector-tile').count(),6)
    for (const width of [1440,768,390,360]) {
      await page.setViewportSize({width,height:844})
      await noOverflow('Inicio '+width)
      await page.screenshot({path:path.join(out,`inicio-${width}.png`)})
      await page.locator('.sector-explorer').scrollIntoViewIfNeeded()
      await page.screenshot({path:path.join(out,`sectores-${width}.png`)})
    }
    await page.setViewportSize({width:1440,height:1000})
    const sectors = await page.locator('.sector-tile').evaluateAll(links => links.map(link=>new URL(link.href).searchParams.get('sector')))
    for (const sector of sectors) {
      await page.goto(web+'/dashboard')
      const response = page.waitForResponse(response => new URL(response.url()).pathname === '/api/v1/companies/search/' && new URL(response.url()).searchParams.get('sector') === sector)
      await page.locator(`.sector-tile[href='/search?sector=${sector}']`).click()
      const payload = await (await response).json()
      assert.ok(payload.results.length>0,sector)
      assert.ok(payload.results.every(company=>company.sector===sector),sector)
      await page.locator('.company-card').first().waitFor()
      assert.equal(await page.getByLabel('Sector:',{exact:true}).inputValue(),sector)
      assert.equal(await page.locator('.sector-shortcuts button[aria-pressed=true]').count(),1)
      await page.goBack()
      assert.equal(new URL(page.url()).pathname,'/dashboard')
    }
    checks.push('Seis sectores llevan a resultados reales y sincronizan filtros; volver conserva la navegación.')
    await page.goto(web+'/company/AAPL')
    const slider = page.getByRole('slider',{name:'Consultar un precio histórico',exact:true})
    await slider.waitFor()
    assert.equal(await page.locator('.dashboard-header .company-mark img').evaluate(img=>img.complete && img.naturalWidth>0),true)
    let historyRequests=0
    page.on('request', request=>{if(request.url().includes('/companies/AAPL/history/')) historyRequests++})
    const before = await slider.getAttribute('aria-valuetext')
    await slider.focus()
    await page.keyboard.press('ArrowLeft')
    const after = await slider.getAttribute('aria-valuetext')
    assert.notEqual(after,before)
    assert.equal(historyRequests,0)
    assert.equal(await page.locator('.chart-inspection time').textContent(),after.split(':')[0])
    await page.keyboard.press('ArrowRight')
    assert.equal(await slider.getAttribute('aria-valuetext'),before)
    await page.getByRole('button',{name:'5 años',exact:true}).click()
    await slider.waitFor()
    assert.ok(Number(await slider.getAttribute('max'))>1000)
    const chartY=(await page.locator('.price-chart').boundingBox()).y
    const metricsY=(await page.locator('.terminal-fundamentals').boundingBox()).y
    assert.ok(chartY<metricsY)
    for (const width of [1440,768,390,360]) {
      await page.setViewportSize({width,height:844})
      await page.evaluate(()=>scrollTo(0,0))
      await noOverflow('Ficha '+width)
      const outcome=await page.locator('#company-overview .financial-health > strong').boundingBox()
      assert.ok(outcome.y+outcome.height<=844,'Clasificación inicial '+width)
      await page.screenshot({path:path.join(out,`ficha-${width}.png`)})
      await page.locator('.price-chart').scrollIntoViewIfNeeded()
      await page.screenshot({path:path.join(out,`grafico-${width}.png`)})
      sizes.push({width,classification_bottom:outcome.y+outcome.height,overflow:false})
    }
    checks.push('Histórico real de cinco años; consulta con flechas cambia fecha y precio sin solicitudes adicionales.')
    await page.setViewportSize({width:1440,height:1000})
    for (const selection of ['a=GOOGL&b=META&fiscal_year=2024','a=AMZN&b=JPM&fiscal_year=2025']) {
      const response = page.waitForResponse(response=>new URL(response.url()).pathname==='/api/v1/compare/' && response.status()===200)
      await page.goto(web+'/compare?'+selection)
      const payload = await (await response).json()
      const result=page.getByRole('region',{name:'Resultado de la selección',exact:true})
      await result.waitFor()
      const expected=payload.comparisons.reduce((count,row)=>count+(row.status==='available' ? ['a','b'].filter(side=>{
        const score=payload[side].pillars.find(p=>p.id===row.id)?.score
        return score?.status==='available' && score.value!=null && Number.isFinite(Number(score.value)) && Number(score.value)>=0 && Number(score.value)<=100
      }).length:0),0)
      assert.equal(await result.locator('.comparison-score-track').count(),expected)
      if (selection.includes('GOOGL')) assert.ok(expected>0)
      else assert.equal(expected,0)
      for (let i=0;i<payload.comparisons.length;i++) {
        const row=payload.comparisons[i]
        const cells=result.locator('tbody tr').nth(i).locator('td')
        for (const [index,side] of ['a','b'].entries()) {
          const score=payload[side].pillars.find(p=>p.id===row.id)?.score
          const text=score?.status==='available' ? `${score.display_value} / 100` : 'No disponible'
          assert.match(await cells.nth(index).innerText(),new RegExp(text.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')))
        }
      }
      for (const width of [1440,390,360]) {
        await page.setViewportSize({width,height:1000})
        await noOverflow('Comparación '+width)
        await result.locator('.scenario-table').scrollIntoViewIfNeeded()
        await page.screenshot({path:path.join(out,`comparacion-${payload.a.ticker}-${width}.png`)})
      }
      if (selection.includes('GOOGL')) {
        await page.setViewportSize({width:1440,height:1000})
        const historyResponse=page.waitForResponse(response=>new URL(response.url()).pathname==='/api/v1/compare/history/' && response.status()===200)
        await page.getByRole('button',{name:'Consultar rendimiento',exact:true}).click()
        const history=await (await historyResponse).json()
        assert.equal(history.status,'available')
        const historical=page.getByRole('region',{name:'Rendimiento comparativo',exact:true})
        await historical.locator('.highcharts-container').waitFor()
        await historical.scrollIntoViewIfNeeded()
        await page.screenshot({path:path.join(out,'rendimiento-comparativo.png')})
        assert.equal(await historical.getByRole('combobox').inputValue(),'1Y')
      }
    }
    checks.push('Las barras coinciden con las notas de la API y desaparecen cuando los periodos no son comparables.')
    await page.emulateMedia({reducedMotion:'reduce'})
    assert.equal(await page.locator('.workspace-content button').first().evaluate(el=>getComputedStyle(el).transitionDuration),'0s')
    checks.push('Source Sans 3 se carga localmente; respeta movimiento reducido y no genera errores de JavaScript.')
    assert.deepEqual(errors,[])
    fs.writeFileSync(path.join(out,'resultado.json'),JSON.stringify({verified_at:new Date().toISOString(),checks,fonts,sizes,page_errors:errors},null,2))
    checks.forEach(check=>process.stdout.write(check+'\n'))
  } finally { await browser.close() }
})().catch(error=>{process.stderr.write(String(error)+'\n');process.exitCode=1})
