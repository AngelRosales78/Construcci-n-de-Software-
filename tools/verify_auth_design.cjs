const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict')
const repo=path.resolve(__dirname,'..')
const {chromium}=require(require.resolve('playwright',{paths:[path.join(repo,'frontend')]}))
;(async()=>{
  const out=path.resolve(repo,'../output/findscore-auth')
  fs.mkdirSync(out,{recursive:true})
  const browser=await chromium.launch({channel:'msedge',headless:true})
  const errors=[],measurements=[]
  try {
    const page=await browser.newPage({viewport:{width:1440,height:1000}})
    page.on('pageerror',error=>errors.push(error.message))
    let submissions=0
    page.on('request',request=>{if(request.method()==='POST' && /\/auth\/(register|token|password)/.test(request.url()))submissions++})
    for(const route of ['login','register','forgot-password']){
      await page.goto('http://127.0.0.1:55193/'+route)
      await page.locator('.auth-header h1').waitFor()
      await page.evaluate(()=>document.fonts.ready)
      for(const [width,height] of [[1920,1080],[1440,1000],[1024,900],[768,1024],[390,844],[360,800],[320,720]]){
        await page.setViewportSize({width,height})
        await page.evaluate(()=>scrollTo(0,0))
        const values=await page.evaluate(()=>({
          overflow:document.documentElement.scrollWidth>innerWidth,
          card:document.querySelector('.auth-card').getBoundingClientRect().width,
          labels:[...document.querySelectorAll('.form-group label')].map(el=>parseFloat(getComputedStyle(el).fontSize)),
          controls:[...document.querySelectorAll('.form-group input,.form-group select')].map(el=>({font:parseFloat(getComputedStyle(el).fontSize),height:el.getBoundingClientRect().height})),
        }))
        assert.equal(values.overflow,false,route+' '+width)
        assert.ok(values.labels.every(size=>size>=15))
        assert.ok(values.controls.every(control=>control.font>=16 && control.height>=48))
        if(width===1920)assert.ok(values.card>=(route==='register'?700:550))
        if(route==='register'){
          const first=await page.locator('#firstName').boundingBox(),last=await page.locator('#lastName').boundingBox()
          if(width>=768)assert.ok(Math.abs(first.y-last.y)<2,'Nombres en la misma fila '+width)
          if(width<=390)assert.ok(last.y>first.y,'Campos apilados '+width)
        }
        await page.screenshot({path:path.join(out,`${route}-${width}.png`),fullPage:true,animations:'disabled'})
        measurements.push({route,width,card:values.card,overflow:values.overflow})
      }
    }
    await page.goto('http://127.0.0.1:55193/register')
    await page.setViewportSize({width:390,height:844})
    await page.getByRole('button',{name:'Crear cuenta',exact:true}).click()
    await page.locator('#username[aria-invalid=true]').waitFor()
    assert.ok(await page.locator('.error-text').count()>0)
    assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>innerWidth),false)
    const password=page.getByLabel('Contraseña',{exact:true})
    await password.fill('VistaLocal1!')
    await page.getByRole('button',{name:'Mostrar contraseña',exact:true}).click()
    assert.equal(await password.getAttribute('type'),'text')
    assert.equal(await password.inputValue(),'VistaLocal1!')
    await page.getByRole('button',{name:'Ocultar contraseña',exact:true}).click()
    assert.equal(await password.getAttribute('type'),'password')
    await page.screenshot({path:path.join(out,'registro-validacion-390.png'),fullPage:true,animations:'disabled'})
    await page.getByRole('link',{name:'Iniciar sesión',exact:true}).first().click()
    await page.waitForURL('**/login')
    assert.equal(await page.getByLabel('Usuario',{exact:true}).count(),1)
    await page.emulateMedia({reducedMotion:'reduce'})
    assert.equal(await page.locator('.auth-card').evaluate(el=>getComputedStyle(el).animationName),'none')
    assert.equal(submissions,0,'No se crea ninguna cuenta ni se envía ninguna contraseña en esta revisión.')
    assert.deepEqual(errors,[])
    fs.writeFileSync(path.join(out,'resultado.json'),JSON.stringify({verified_at:new Date().toISOString(),measurements,page_errors:errors,submissions},null,2))
    process.stdout.write('Acceso, registro y recuperación: 21 tamaños comprobados; texto legible, campos de 50 px, validación y movimiento reducido correctos.\n')
  }finally{await browser.close()}
})().catch(error=>{process.stderr.write(String(error)+'\n');process.exitCode=1})
