const {chromium}=require('@playwright/test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const base=process.env.NIVORYN_TEST_URL||'http://127.0.0.1:4173';
const out=process.env.NIVORYN_REPAIR_OUTPUT||'/private/tmp/nivoryn-motion-repair/verification';
fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN||'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try{for(const width of [320,390,834,1440]){
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width<760,hasTouch:width<760});
  await context.addInitScript(()=>{
   window.motionChecks={frames:0,listeners:{}};
   const raf=requestAnimationFrame;window.requestAnimationFrame=fn=>{window.motionChecks.frames++;return raf(fn)};
   const on=EventTarget.prototype.addEventListener;
   EventTarget.prototype.addEventListener=function(type,...args){
    if(this===window&&['scroll','resize'].includes(type))window.motionChecks.listeners[type]=(window.motionChecks.listeners[type]||0)+1;
    return on.call(this,type,...args);
   };
  });
  const page=await context.newPage(),errors=[],failed=[];
  page.on('pageerror',e=>errors.push(e.message));page.on('requestfailed',r=>failed.push({url:r.url(),reason:r.failure()?.errorText}));
  await page.goto(base);await page.evaluate(()=>document.fonts.ready);
  const forwardPoses=new Map();
  for(const p of [0,.25,.5,.55,.75,1,.75,.5,.25,0]){
   await page.evaluate(p=>{const h=document.querySelector('.hero'),s=document.querySelector('.hero-stage');scrollTo(0,h.offsetTop-(parseFloat(getComputedStyle(s).top)||0)+(h.offsetHeight-s.offsetHeight)*p)},p);
   await page.waitForTimeout(80);
   const pose=await page.evaluate(()=>{
    const phone=document.querySelector('.phone-object'),screen=document.querySelector('.phone-screen');
    const css=getComputedStyle(screen);const w=phone.offsetWidth,h=phone.offsetHeight;
    return {left:parseFloat(css.left)/w,top:parseFloat(css.top)/h,width:screen.offsetWidth/w,height:screen.offsetHeight/h,
     visibleCopy:[...document.querySelectorAll('.hero-initial,.hero-resolution')].filter(e=>getComputedStyle(e).visibility!=='hidden'&&+getComputedStyle(e).opacity>.99).length,
     screenVisible:[...document.querySelectorAll('.phone-screen-layer')].filter(e=>!e.hidden&&getComputedStyle(e).visibility!=='hidden'&&+getComputedStyle(e).opacity>.99).length,
     overflow:document.documentElement.scrollWidth>innerWidth,
     transforms:[...document.querySelectorAll('.object')].map(el=>el.style.transform)};
   });
   // Camera is orthographic, render 640×1200, ortho_scale 7.5: 160px/unit.
   // Inner screen mesh: 2.96×6.03, centre (0,-.02); image corners (83.2,120.8)–(556.8,1085.6).
   assert.ok(Math.abs(pose.left-.13)<.006,`inner bezel left at ${width}/${p}: ${JSON.stringify(pose)}`);
   assert.ok(Math.abs(pose.top-120.8/1200)<.006,'inner bezel top');
   assert.ok(Math.abs(pose.width-.74)<.008,'inner bezel width');
   assert.ok(Math.abs(pose.height-.804)<.008,'inner bezel height');
   assert.equal(pose.visibleCopy,1,`one complete copy block at ${width}/${p}`);
   assert.equal(pose.screenVisible,1,`one readable display at ${width}/${p}`);
   assert.equal(pose.overflow,false);
   if(forwardPoses.has(p))assert.deepEqual(pose.transforms,forwardPoses.get(p),`reversal restores exact pose at ${width}/${p}`);
   else forwardPoses.set(p,pose.transforms);
   if([0,.25,.5,.75,1].includes(p))await page.screenshot({path:`${out}/${width}-${p}.png`});
  }
  for(const [name,kind,quote] of [['View message example','message','Pay a fee'],['View link example','link','support-check.example'],['View screenshot example','screenshot','Share your OTP'],['View document example','document','Booking status']]){
   const button=page.locator(kind==='message'?'.message-slip':kind==='link'&&width>=760?'.url-object':kind==='document'&&width>=760?'.document-object':`.phone-example-controls [data-select="${kind}"]`);
   await button.scrollIntoViewIfNeeded();await page.waitForTimeout(80);
   // Test actual transformed, unoccluded pixels, rather than force-clicking a
   // bounding-box centre that may belong to another intentionally layered object.
   const point=await button.evaluate(el=>{const r=el.getBoundingClientRect();for(let y=.15;y<1;y+=.15)for(let x=.15;x<1;x+=.15){const p={x:r.left+r.width*x,y:r.top+r.height*y};if(p.y>0&&p.y<innerHeight&&el.contains(document.elementFromPoint(p.x,p.y)))return p;}return null;});
   assert.ok(point,`${kind} has an unobscured hit area at ${width}`);
   if(width<760)await page.touchscreen.tap(point.x,point.y);else await page.mouse.click(point.x,point.y);
   assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),kind);
   assert.ok((await page.locator('.phone-screen').innerText()).includes(quote));
   assert.ok(await page.locator('.phone-screen-layer').evaluate(el=>el.scrollHeight<=el.clientHeight+1),`all ${kind} content fits display at ${width}`);
   await page.evaluate(()=>scrollBy(0,100));await page.waitForTimeout(100);
   assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),kind,'explicit selection survives scroll');
   assert.equal(await page.getByRole('tab',{name:kind[0].toUpperCase()+kind.slice(1),exact:true}).getAttribute('aria-selected'),'true');
  }
  await page.getByRole('tab',{name:'Message',exact:true}).click();
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(100);
  assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),'message');
  await page.locator('.envelope-object').focus();await page.keyboard.press('Enter');
  assert.equal(await page.locator('.phone-screen').getAttribute('data-example'),'message','envelope keyboard selection');
  assert.ok(await page.locator('.envelope-object').evaluate(el=>getComputedStyle(el).outlineStyle!=='none'),'visible keyboard focus');
  if(width<760){
   const cdp=await context.newCDPSession(page);const before=await page.evaluate(()=>scrollY);
   await cdp.send('Input.synthesizeScrollGesture',{x:width/2,y:400,yDistance:-180,speed:600,gestureSourceType:'touch'});
   assert.ok(await page.evaluate(previous=>scrollY>previous,before),'native emulated touch scroll moves the page');
  }
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(100);
  assert.ok(await page.getByRole('button',{name:'Read example evidence',exact:true}).isVisible());
  await page.getByRole('button',{name:'Read example evidence',exact:true}).click();
  assert.ok(await page.getByRole('tabpanel').isVisible());
  for(let i=0;i<3;i++){
   await page.getByRole('link',{name:'Privacy',exact:true}).first().click();await page.evaluate(()=>document.fonts.ready);await page.waitForLoadState('networkidle');
   await page.goBack();await page.evaluate(()=>document.fonts.ready);await page.waitForLoadState('networkidle');
  }
  assert.equal(await page.locator('.phone-screen-layer:visible').count(),1,'no blank return');
  const beforePointer=await page.locator('.phone-object').getAttribute('style');
  await page.mouse.move(30,300);await page.mouse.move(width-30,500);await page.waitForTimeout(350);
  assert.equal(await page.locator('.phone-object').getAttribute('style'),beforePointer,'pointer does not compete with scroll pose');
  assert.deepEqual(await page.evaluate(()=>window.motionChecks.listeners),{scroll:1,resize:1},'one set of subscriptions after navigation');
  const frames=await page.evaluate(()=>window.motionChecks.frames);await page.waitForTimeout(150);
  assert.equal(await page.evaluate(()=>window.motionChecks.frames),frames,'no continuing RAF queue at rest');
  await page.evaluate(()=>{const h=document.querySelector('.hero'),s=document.querySelector('.hero-stage');scrollTo(0,h.offsetTop-(parseFloat(getComputedStyle(s).top)||0)+(h.offsetHeight-s.offsetHeight)*.75)});
  await page.waitForTimeout(100);
  const resizePose=await page.locator('.phone-object').getAttribute('style');
  await page.setViewportSize({width:width===1440?834:1440,height:900});await page.waitForTimeout(100);
  // Rounding a native scroll position to an integer can change a transform by a
  // fraction of a pixel; compare the actual matrix translation within 0.1px.
  const resized=await page.locator('.phone-object').evaluate(el=>new DOMMatrix(getComputedStyle(el).transform).m41);
  const originalX=Number(resizePose.match(/translate3d\(([-\d.]+)/)[1]);
  assert.ok(Math.abs(resized-originalX)<.1,'resize retains narrative pose');
  await page.setViewportSize({width,height:900});await page.waitForTimeout(100);
  await page.emulateMedia({reducedMotion:'reduce'});await page.evaluate(()=>scrollTo(0,0));
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  assert.ok(await page.locator('.phone-screen-layer:visible').count()===1,JSON.stringify(await page.locator('.phone-screen-layer').evaluate(el=>{const rows=[];for(let e=el;e;e=e.parentElement)rows.push({class:e.className,w:e.offsetWidth,h:e.offsetHeight,visibility:getComputedStyle(e).visibility});return rows;})));
  assert.deepEqual(errors,[]);assert.deepEqual(failed,[]);await context.close();
  console.log(`PASS ${width}: bezel registration, copy/display continuity, object/touch selection, persistent semantic state, screen action, navigation, resize, reduced motion`);
 }}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
