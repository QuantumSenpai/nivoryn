const {chromium}=require('@playwright/test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const base=process.argv[2]||process.env.NIVORYN_TEST_URL||'http://127.0.0.1:4173';
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try{for(const width of [390,834,1440]){
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width<760,hasTouch:width<760});
  await context.addInitScript(()=>{window.scheduled=0;const raf=requestAnimationFrame;window.requestAnimationFrame=fn=>{window.scheduled++;return raf(fn)}});
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base);await page.waitForFunction(()=>performance.getEntriesByName('hero-measured').length);
  await page.evaluate(()=>new Promise(r=>requestAnimationFrame(()=>requestAnimationFrame(r))));
  fs.mkdirSync('/private/tmp/nivoryn-first-scroll/visual',{recursive:true});
  const outer=await page.locator('.phone-object').getAttribute('style');
  await page.mouse.move(width*.85,400);await page.waitForTimeout(350);
  const pose=await page.locator('.phone-object .object-response').evaluate(el=>{
   const t=new DOMMatrix(getComputedStyle(el).transform),s=el.querySelector('.phone-screen');
   return {x:t.m41,y:t.m42,screenParent:s.parentElement===el,left:parseFloat(getComputedStyle(s).left)/el.offsetWidth,width:s.offsetWidth/el.offsetWidth};
  });
  await page.screenshot({path:`/private/tmp/nivoryn-first-scroll/visual/${width}-cursor.png`});
  assert.equal(await page.locator('.phone-object').getAttribute('style'),outer,'cursor never overwrites scroll travel');
  assert.ok(pose.screenParent,'phone body and fitted display share response transform');
  assert.ok(Math.abs(pose.left-.13)<.006&&Math.abs(pose.width-.74)<.008,'registration unchanged');
  if(width<760)assert.equal(pose.x,0,'coarse pointer is static');else assert.ok(pose.x>0&&pose.x<1.2,'phone response is restrained');
  if(width>=760){
   const doc=await page.locator('.document-object .object-response').evaluate(el=>new DOMMatrix(getComputedStyle(el).transform).m41);
   assert.ok(doc>pose.x*2,'depth dependent response');
   const clearance=await page.evaluate(()=>document.querySelector('.hero-foot').getBoundingClientRect().top-document.querySelector('.url-object .object-response').getBoundingClientRect().bottom);
   assert.ok(clearance>=12,`URL/supporting copy clearance at ${width}: ${clearance}`);
  }
  await page.locator('.hero-stage').dispatchEvent('pointerleave');await page.waitForTimeout(350);
  assert.equal(await page.locator('.phone-object .object-response').evaluate(el=>new DOMMatrix(getComputedStyle(el).transform).m41),0,'leave settles exactly');
  await page.screenshot({path:`/private/tmp/nivoryn-first-scroll/visual/${width}-neutral.png`});
  const settled=await page.evaluate(()=>window.scheduled);await page.waitForTimeout(120);
  assert.equal(await page.evaluate(()=>window.scheduled),settled,'no perpetual cursor loop');
  await page.mouse.move(width*.2,350);await page.evaluate(()=>dispatchEvent(new Event('blur')));
  assert.equal(await page.locator('.phone-object .object-response').evaluate(el=>new DOMMatrix(getComputedStyle(el).transform).m41),0,'blur resets immediately');
  for(const kind of ['link','screenshot','document','message']){
   await page.locator(`.phone-example-controls [data-select="${kind}"]`).click();
   assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),kind);
   await page.evaluate(()=>scrollBy(0,60));await page.waitForTimeout(240);
   assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),kind,'scroll preserves explicit choice');
  }
  await page.emulateMedia({reducedMotion:'reduce'});await page.mouse.move(width*.9,400);await page.waitForTimeout(100);
  assert.equal(await page.locator('.phone-object .object-response').evaluate(el=>getComputedStyle(el).transform),'none');
  assert.deepEqual(errors,[]);await context.close();console.log(`PASS ${width}: cursor depth, shared phone geometry, leave/blur, bounded loop, selection, reduced motion`);
 }
 const context=await browser.newContext({viewport:{width:1440,height:900}});
 await context.addInitScript(()=>{HTMLImageElement.prototype.decode=()=>new Promise(()=>{});});
 const page=await context.newPage();await page.goto(base);await page.waitForFunction(()=>performance.getEntriesByName('hero-measured').length);
 await page.evaluate(()=>scrollTo(0,300));await page.waitForTimeout(100);
 assert.ok((await page.locator('.phone-object').getAttribute('style')).includes('translate3d(-8'),'pending decode does not gate scrolling');
 assert.ok(await page.locator('.phone-screen').isVisible(),'useful display while preparation pending');
 await context.close();console.log('PASS pending-image preparation: native scrolling and useful content are not blocked');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
