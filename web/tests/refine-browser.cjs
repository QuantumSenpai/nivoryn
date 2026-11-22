const {chromium}=require('@playwright/test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const base=process.argv[2]||'http://127.0.0.1:4173';
const out='/private/tmp/nivoryn-beat-refine/verification';fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try{for(const width of [390,834,1440]){
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width<760,hasTouch:width<760});
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base);await page.evaluate(()=>document.fonts.ready);
  for(const kind of ['Link','Message','Screenshot','Document']){
   await page.getByRole('tab',{name:kind,exact:true}).click();await page.waitForTimeout(250);
   const card=page.locator('.example-panel:not([hidden]) .example-input-card');const hit=card.locator('..');
   await hit.scrollIntoViewIfNeeded();await page.mouse.move(0,0);await page.waitForTimeout(280);
   const rest=await card.evaluate(el=>({angle:getComputedStyle(el).rotate,w:el.offsetWidth,h:el.offsetHeight}));
   assert.equal(await hit.getAttribute('tabindex'),null,'non-interactive preview is not a keyboard stop');
   await hit.hover();await page.waitForTimeout(300);
   const hover=await card.evaluate(el=>({angle:getComputedStyle(el).rotate,y:new DOMMatrix(getComputedStyle(el).transform).m42,shadow:+getComputedStyle(el,'::before').opacity,w:el.offsetWidth,h:el.offsetHeight,shadowEvents:getComputedStyle(el,'::before').pointerEvents}));
   if(width>=760){assert.ok(Math.abs(parseFloat(hover.angle))<.01);assert.equal(hover.y,-5);assert.equal(hover.shadow,1);}
   else assert.equal(hover.angle,rest.angle,'touch does not retain hover motion');
   assert.equal(hover.w,rest.w);assert.equal(hover.h,rest.h);assert.equal(hover.shadowEvents,'none');
   await page.mouse.move(0,0);await page.waitForTimeout(70);await hit.hover();await page.waitForTimeout(60);await page.mouse.move(0,0);await page.waitForTimeout(300);
   assert.equal(await card.evaluate(el=>getComputedStyle(el).rotate),rest.angle,'interrupted leave restores exact original tilt');
   if(width>=760){
    // A real-action fixture checks focus-within and transformed clickability;
    // production specimens remain non-interactive and gain no fake links.
    await card.evaluate(el=>{const a=document.createElement('a');a.href='#download';a.textContent='Test specimen action';a.dataset.fixture='true';el.append(a)});
    const action=card.locator('[data-fixture]');await action.focus();await page.waitForTimeout(300);await action.click();assert.ok(page.url().endsWith('#download'));await page.evaluate(()=>document.querySelector('[data-fixture]').remove());
   }
  }
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(100);
  const node=await page.locator('.phone-object').elementHandle();const size=await page.locator('.hero-stage').evaluate(el=>el.offsetHeight);
  const poses=new Map();
  for(const p of [.39,.49,.499,.501,.51,.62,.7,.51,.499,.49,.39]){
   await page.evaluate(p=>{const h=document.querySelector('.hero'),s=document.querySelector('.hero-stage');scrollTo(0,h.offsetTop-(parseFloat(getComputedStyle(s).top)||0)+(h.offsetHeight-s.offsetHeight)*p)},p);await page.waitForTimeout(70);
   const state=await page.evaluate(()=>{
    const copies=[...document.querySelectorAll('.hero-initial,.hero-resolution')];
    return {opacities:copies.map(e=>+getComputedStyle(e).opacity),hidden:copies.map(e=>e.getAttribute('aria-hidden')),visibility:copies.map(e=>getComputedStyle(e).visibility),height:document.querySelector('.hero-stage').offsetHeight,pose:[...document.querySelectorAll('.object')].map(e=>e.style.transform),timelines:copies.flatMap(e=>e.getAnimations()).length};
   });
   assert.equal(state.height,size,'text handoff never changes pin geometry');
   assert.equal(state.opacities.filter(v=>v===1).length,1,'one complete readable copy, no blank/colliding headlines');
   assert.equal(state.opacities.reduce((a,b)=>a+b),1);
   assert.equal(state.hidden.filter(v=>v==='false').length,1,'one text variant in accessibility tree');
   assert.deepEqual(state.visibility,['visible','visible'],'text layers persist instead of invalidating on every crossing');
   assert.equal(state.timelines,0,'no threshold-triggered timed animation');
   if(poses.has(p))assert.deepEqual(state.pose,poses.get(p),'exact reverse fan-out');else poses.set(p,state.pose);
   assert.ok(await node.evaluate(el=>el===document.querySelector('.phone-object')),'phone is never remounted');
  }
  if(width>=760){const gap=await page.evaluate(()=>document.querySelector('.hero-foot').getBoundingClientRect().top-document.querySelector('.url-object .object-response').getBoundingClientRect().bottom);assert.ok(gap>=12,'expanded URL remains clear of supporting copy');}
  await page.screenshot({path:`${out}/${width}-hero.png`});
  await page.emulateMedia({reducedMotion:'reduce'});await page.getByRole('tab',{name:'Message',exact:true}).click();const card=page.locator('#example-message .example-input-card');const angle=await card.evaluate(el=>getComputedStyle(el).rotate);await card.locator('..').hover();await page.waitForTimeout(180);assert.equal(await card.evaluate(el=>getComputedStyle(el).rotate),angle);assert.equal(await card.evaluate(el=>new DOMMatrix(getComputedStyle(el).transform).m42),0);
  assert.deepEqual(errors,[]);await context.close();console.log(`PASS ${width}: hover/re-entry/focus/actions, stable text layers, semantic stage, fan-out reversal, reduced motion`);
 }}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
