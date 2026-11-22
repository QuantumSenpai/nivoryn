const {chromium}=require('@playwright/test');const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN || '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try {
 const report=[];const out='/private/tmp/nivoryn-motion';fs.mkdirSync(out,{recursive:true});
 for(const width of [390,1440]){
  const context=await browser.newContext({viewport:{width,height:900}});
  // Brave may randomise the reported memory bucket. Exercise the eligible
  // rendering path deterministically, and the limited-device path separately.
  await context.addInitScript(()=>Object.defineProperty(navigator,'deviceMemory',{value:8}));
  const page=await context.newPage();const cdp=await context.newCDPSession(page);await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
  await page.goto(process.env.NIVORYN_TEST_URL || 'http://127.0.0.1:4173');await page.evaluate(()=>document.fonts.ready);
  const samples=await page.evaluate(async()=>{
   const hero=document.querySelector('.hero'),stage=document.querySelector('.hero-stage');const travel=hero.offsetHeight-stage.offsetHeight;
   let previous=0;const deltas=[],blur=[];
   for(let i=0;i<90;i++){
    const now=await new Promise(requestAnimationFrame);if(previous)deltas.push(now-previous);previous=now;
    scrollTo(0,travel*i/89);
    blur.push(...[...document.querySelectorAll('.object img')].map(e=>parseFloat(getComputedStyle(e).filter.match(/blur\((.*?)px/)?.[1]||0)));
   }
   return {maxBlur:Math.max(...blur),p95FrameMs:deltas.sort((a,b)=>a-b)[Math.floor(deltas.length*.95)],samples:deltas.length};
  });
  console.log(JSON.stringify({width,device:await page.evaluate(()=>({memory:navigator.deviceMemory,saveData:navigator.connection?.saveData})),...samples}));
  assert.ok(samples.maxBlur>0,'object motion blur activates');assert.ok(samples.maxBlur<=(width===390?.65:1.4));
  await page.waitForTimeout(450);
  assert.equal(await page.locator('.phone-frame').evaluate(el=>getComputedStyle(el).filter),'none','blur settles at rest');
  assert.equal(await page.locator('.phone-screen').evaluate(el=>getComputedStyle(el).filter),'none','live UI stays sharp');
  await page.screenshot({path:`${out}/${width}-settled.png`});
  for(const p of [.25,.4,.5,.6,.8]){await page.evaluate(p=>scrollTo(0,(document.querySelector('.hero').offsetHeight-document.querySelector('.hero-stage').offsetHeight)*p),p);await page.waitForTimeout(80);assert.equal(await page.locator('.hero-initial,.hero-resolution').evaluateAll(es=>es.filter(e=>getComputedStyle(e).visibility==='visible'&&+getComputedStyle(e).opacity===1&&getComputedStyle(e).clipPath==='none').length),1,'one complete copy block remains readable at every handoff position');await page.screenshot({path:`${out}/${width}-${p}.png`});}
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(450);
  assert.equal(await page.locator('.hero-initial').getAttribute('aria-hidden'),'false');
  await page.emulateMedia({reducedMotion:'reduce'});await page.waitForTimeout(100);
  assert.equal(await page.locator('.hero-stage').evaluate(e=>getComputedStyle(e).position),'relative');
  assert.equal(await page.locator('.phone-frame').evaluate(e=>getComputedStyle(e).filter),'none');
  assert.equal(await page.locator('.phone-result').evaluate(e=>getComputedStyle(e).visibility),'visible');
  report.push({width,...samples,settledSharp:true,readingTextSharp:true,runtimeReducedMotion:true});await context.close();
 }
 const limited=await browser.newContext({viewport:{width:390,height:900}});
 await limited.addInitScript(()=>Object.defineProperty(navigator,'deviceMemory',{value:4}));
 const low=await limited.newPage();await low.goto(process.env.NIVORYN_TEST_URL || 'http://127.0.0.1:4173');
 const values=await low.evaluate(async()=>{const values=[];for(let i=0;i<60;i++){await new Promise(requestAnimationFrame);scrollTo(0,i*7);values.push(getComputedStyle(document.querySelector('.envelope-object img')).filter);}return values;});
 assert.ok(values.every(value=>value==='none'),'limited-memory devices keep blur disabled');
 await limited.close();
 fs.writeFileSync('/private/tmp/nivoryn-motion/report.json',JSON.stringify(report,null,2));console.log(JSON.stringify(report,null,2));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
