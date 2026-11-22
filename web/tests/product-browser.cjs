const BASE=process.env.NIVORYN_TEST_URL || 'http://127.0.0.1:4173';
const {chromium}=require('@playwright/test');
const AxeBuilder=require('@axe-core/playwright').default;
const fs=require('node:fs');const assert=require('node:assert/strict');
const out=process.env.NIVORYN_BROWSER_OUTPUT || '/private/tmp/nivoryn-product-browser';fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN || '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try {
 const report=[];
 for(const width of [320,390,768,1024,1440]) {
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width<760,hasTouch:width<760});
  const page=await context.newPage();const errors=[],external=[];
  page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>{if(!r.url().startsWith(BASE))external.push(r.url())});
  await page.goto(BASE);await page.evaluate(()=>document.fonts.ready);
  assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,`overflow at ${width}`);
  assert.equal(await page.locator('h1').innerText(),'Pause before\nyou trust.');
  assert.equal(await page.getByRole('link',{name:'Download APK',exact:true}).first().getAttribute('href'), 'downloads/'+require('../release.json').filename);
  assert.notEqual(await page.getByRole('link',{name:'Download APK',exact:true}).first().getAttribute('download'),null);
  const build=await page.getByRole('link',{name:'Download APK',exact:true}).first().boundingBox();assert.ok(build.y<900,`initial build action ${width}`);
  await page.screenshot({path:`${out}/${width}-hero.png`});
  const start=await page.locator('.phone-object').evaluate(el=>getComputedStyle(el).transform);
  const travel=await page.evaluate(()=>document.querySelector('.hero').offsetHeight-document.querySelector('.hero-stage').offsetHeight);
  await page.evaluate(y=>scrollTo(0,y),travel*.82);await page.waitForTimeout(100);
  assert.equal(await page.locator('.phone-result').evaluate(el=>getComputedStyle(el).visibility),'visible');
  await page.screenshot({path:`${out}/${width}-evidence-stage.png`});
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(100);
  assert.equal(await page.locator('.phone-object').evaluate(el=>getComputedStyle(el).transform),start,'reverse scrolling restores start');
  const tabs=page.getByRole('tab');assert.equal(await tabs.count(),4);
  for(const kind of ['Link','Message','Screenshot','Document']){
   const tab=page.getByRole('tab',{name:kind,exact:true});if(width<760)await tab.tap();else await tab.click();
   assert.equal(await tab.getAttribute('aria-selected'),'true');
   const panel=page.getByRole('tabpanel');assert.equal(await panel.count(),1);
   assert.ok(await panel.getByText('Example result',{exact:true}).isVisible());
   await panel.getByText('What remains unknown',{exact:true}).click();
   assert.equal(await panel.locator('details').nth(1).getAttribute('open'),'');
  }
  const last=page.getByRole('tab',{name:'Document',exact:true});await last.focus();await page.keyboard.press('Home');
  assert.equal(await page.getByRole('tab',{name:'Link',exact:true}).getAttribute('aria-selected'),'true');
  await page.keyboard.press('ArrowRight');assert.equal(await page.getByRole('tab',{name:'Message',exact:true}).getAttribute('aria-selected'),'true');
  await page.locator('#examples').screenshot({path:`${out}/${width}-examples.png`});
  await page.getByRole('link',{name:'Privacy',exact:true}).first().click();assert.ok(page.url().endsWith('/privacy.html'));await page.goBack();await page.getByRole('tab',{name:'Message',exact:true}).waitFor();
  const axe=await new AxeBuilder({page}).withTags(['wcag2a','wcag2aa','wcag21aa']).analyze();
  const violations=axe.violations.map(v=>({id:v.id,impact:v.impact,nodes:v.nodes.map(n=>n.target)}));
  fs.writeFileSync(`${out}/${width}-axe.json`,JSON.stringify(violations,null,2));
  assert.deepEqual(violations,[],`accessibility ${width}`);
  for(const path of ['privacy.html','terms.html','notices.html','accessibility.html']) {const response=await page.goto(BASE+'/'+path);assert.equal(response.status(),200);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);}
  assert.deepEqual(errors,[]);assert.deepEqual(external,[]);
  report.push({width,overflow:false,reverseScroll:true,tabs:true,keyboard:true,touch:width<760,axe:0,externalRequests:0});await context.close();
 }
 const reduced=await browser.newContext({viewport:{width:390,height:844},reducedMotion:'reduce'});const r=await reduced.newPage();await r.goto(BASE);assert.equal(await r.locator('.hero-stage').evaluate(el=>getComputedStyle(el).position),'relative');assert.equal(await r.locator('.phone-result').evaluate(el=>getComputedStyle(el).visibility),'visible');await r.screenshot({path:`${out}/reduced-motion.png`,fullPage:true});await reduced.close();
 const nojs=await browser.newContext({viewport:{width:390,height:844},javaScriptEnabled:false});const n=await nojs.newPage();await n.goto(BASE);assert.equal(await n.locator('.example-panel:visible').count(),4);assert.ok(await n.getByRole('link',{name:'Download APK',exact:true}).first().isVisible());await n.screenshot({path:`${out}/no-javascript.png`});await nojs.close();
 const failed=await browser.newContext({viewport:{width:390,height:844}});const f=await failed.newPage();await f.route('**/*.webp',route=>route.abort());await f.goto(BASE);assert.ok(await f.getByRole('heading',{level:1}).isVisible());assert.ok(await f.getByRole('link',{name:'Download APK',exact:true}).first().isVisible());await failed.close();
 fs.writeFileSync(`${out}/report.json`,JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));} finally { await browser.close(); }
})().catch(e=>{console.error(e);process.exitCode=1});
