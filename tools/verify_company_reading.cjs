const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const repo=path.resolve(__dirname,'..'),{chromium}=require(require.resolve('playwright',{paths:[path.join(repo,'frontend')]}));
(async()=>{const out=path.resolve(repo,'../output/reading-design');fs.mkdirSync(out,{recursive:true});const creds=JSON.parse(fs.readFileSync(path.resolve(repo,'../presentation_runtime_sprint3/demo_credentials.json'),'utf8').replace(/^\uFEFF/,'')).free;
const browser=await chromium.launch({channel:'msedge',headless:true});try{const page=await browser.newPage({viewport:{width:1440,height:1000}});
await page.goto('http://127.0.0.1:55193/login');await page.locator('#username').fill(creds.username);await page.locator('#password').fill(creds.password);await page.getByRole('button',{name:'Iniciar sesión',exact:true}).click();await page.waitForURL('**/dashboard');
await page.goto('http://127.0.0.1:55193/company/AAPL');await page.locator('.metric-value').first().waitFor();await page.waitForTimeout(800);
const desktop=await page.locator('.terminal-fundamentals').boundingBox();assert.ok(desktop.y<750,'Indicadores demasiado abajo');
assert.equal(await page.locator('.company-context').evaluate(el=>el.open),false);
assert.equal(await page.locator('.metric-help').first().evaluate(el=>el.open),false);
await page.screenshot({path:path.join(out,'01-resumen-escritorio.png')});
await page.getByRole('group',{name:'Rango de precios'}).getByRole('button',{name:'1 año',exact:true}).click();await page.waitForTimeout(300);
await page.getByRole('button',{name:'Salud financiera',exact:true}).click();await page.locator('.provider-period-note').first().waitFor();await page.screenshot({path:path.join(out,'02-salud-financiera.png')});
await page.getByRole('button',{name:'Resumen',exact:true}).click();await page.locator('.metric-help').first().locator('summary').focus();await page.keyboard.press('Enter');assert.equal(await page.locator('.metric-help').first().evaluate(el=>el.open),true);await page.keyboard.press('Enter');
for(const width of [390,768]){await page.setViewportSize({width,height:1000});await page.evaluate(()=>window.scrollTo(0,0));assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth>document.documentElement.clientWidth),false);assert.equal(await page.locator('.provider-reference-inline').isVisible(),true,'La advertencia debe seguir visible');await page.screenshot({path:path.join(out,'resumen-'+width+'.png')});}
fs.writeFileSync(path.join(out,'resultado.json'),JSON.stringify({indicators_desktop_y:desktop.y,keyboard_disclosure:true,widths:[1440,768,390],overflow:false},null,2));process.stdout.write('Jerarquía inicial, teclado y tres tamaños comprobados.\n');
}finally{await browser.close()}})().catch(e=>{process.stderr.write(String(e)+'\n');process.exitCode=1});
