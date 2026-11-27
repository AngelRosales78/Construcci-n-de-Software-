const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const repo=path.resolve(__dirname,'..'),phase=process.argv[2]==='before'?'before':'after'
const {chromium}=require(require.resolve('playwright',{paths:[path.join(repo,'frontend')]}))
;(async()=>{
  const out=path.resolve(repo,'../output/interface-polish',phase)
  fs.mkdirSync(out,{recursive:true})
  const account=JSON.parse(fs.readFileSync(path.resolve(repo,'../presentation_runtime_sprint3/demo_credentials.json'),'utf8').replace(/^\uFEFF/,'')).pro
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const results=[],errors=[]
  try{
    const page=await browser.newPage({viewport:{width:1440,height:1000}})
    page.on('pageerror',error=>errors.push(error.message))
    await page.goto('http://127.0.0.1:55193/login')
    await page.locator('#username').fill(account.username)
    await page.locator('#password').fill(account.password)
    await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click()
    await page.waitForURL('**/dashboard')
    const pages=[
      ['/search','explorador','.company-card'],
      ['/compare?a=GOOGL&b=META&fiscal_year=2024','comparador','.comparison-result'],
      ['/profile','perfil','.profile-preview, .account-page form'],
      ['/favorites','favoritos','.favorites-empty, .favorites-list'],
      ['/notifications','alertas','.notification-page .notification-links'],
      ['/profile/notifications','preferencias','.notification-preferences'],
      ['/profile/subscription','suscripcion','.subscription-table'],
    ]
    for(const [url,name,ready] of pages){
      await page.goto('http://127.0.0.1:55193'+url)
      await page.locator(ready).first().waitFor()
      if(name==='explorador')await page.locator('.company-card-wrapper > .favorite-button:not(:disabled)').first().waitFor()
      if(name==='alertas')await page.locator('.notification-items, .alerts-empty-actions').first().waitFor()
      for(const width of [1440,768,390,360]){
        await page.setViewportSize({width,height:1000})
        await page.evaluate(()=>document.fonts.ready)
        await page.evaluate(()=>scrollTo(0,0))
        assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false,name+' '+width)
        const metrics={name,width}
        if(name==='explorador'){
          metrics.first_result_y=(await page.locator('.company-card').first().boundingBox()).y
          if(phase==='after')assert.equal(await page.locator('.company-card .metric-label').evaluateAll(labels=>labels.some(el=>el.scrollWidth>el.clientWidth+1)),false,'Etiquetas completas '+width)
          if(phase==='after'&&width<=390)assert.ok(metrics.first_result_y<760,'La primera empresa debe quedar cerca del inicio '+width)
        }
        if(name==='comparador')metrics.picker_height=(await page.locator('.comparison-picker').first().boundingBox()).height
        if(phase==='after')assert.equal(await page.locator('html').evaluate(el=>getComputedStyle(el).colorScheme),'light')
        await page.screenshot({path:path.join(out,`${name}-${width}.png`),animations:'disabled'})
        results.push(metrics)
      }
    }
    await page.setViewportSize({width:390,height:1000})
    await page.goto('http://127.0.0.1:55193/search?q=AAPL&with_data=true')
    await page.locator('.company-card').first().waitFor()
    await page.getByLabel('Sector:',{exact:true}).selectOption('technology')
    assert.equal(new URL(page.url()).searchParams.get('with_data'),'true')
    assert.equal(new URL(page.url()).searchParams.get('q'),'AAPL')
    await page.getByLabel('Ordenar por:',{exact:true}).selectOption('market_cap')
    assert.equal(new URL(page.url()).searchParams.get('sector'),'technology')
    await page.getByRole('button',{name:'Quitar sector',exact:true}).click()
    assert.equal(new URL(page.url()).searchParams.has('sector'),false)
    assert.equal(new URL(page.url()).searchParams.get('ordering'),'market_cap')
    assert.deepEqual(errors,[])
    fs.writeFileSync(path.join(out,'resultado.json'),JSON.stringify({verified_at:new Date().toISOString(),phase,results,page_errors:errors},null,2))
    process.stdout.write(JSON.stringify({phase,mobile:results.filter(row=>row.width===390),page_errors:errors})+'\n')
  }finally{await browser.close()}
})().catch(error=>{process.stderr.write(String(error)+'\n');process.exitCode=1})
