const {chromium}=require('@playwright/test');
const fs=require('node:fs');
const path=require('node:path');
const label=process.argv[2]||'before';
const base=process.env.PROFILE_URL||'http://127.0.0.1:4173';
const out=path.join(process.env.NIVORYN_PROFILE_OUTPUT||'/private/tmp/nivoryn-motion-repair',label);fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 const runs=[];
 try{for(const width of [390,834,1440])for(const mode of (process.env.PROFILE_MODES?.split(',')||['normal','blur-off'])){
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width===390,hasTouch:width===390});
  await context.addInitScript(mode=>{
   window.profile={reads:0,rafCalls:0,updates:0,longTasks:[],scrolls:[]};
   const old=Element.prototype.getBoundingClientRect;
   Element.prototype.getBoundingClientRect=function(){if(this.matches?.('.hero,.hero-stage,.assembly,.phone-object'))window.profile.reads++;return old.call(this);};
   const raf=window.requestAnimationFrame;window.requestAnimationFrame=fn=>{window.profile.rafCalls++;return raf(fn);};
   new PerformanceObserver(list=>window.profile.longTasks.push(...list.getEntries().map(e=>({start:e.startTime,duration:e.duration})))).observe({type:'longtask',buffered:true});
   window.addEventListener('DOMContentLoaded',()=>{
    const style=document.createElement('style');
    if(mode==='blur-off')style.textContent='.object>img,.object .surface{filter:none!important}';
    if(mode==='overlay-frozen')style.textContent='.phone-screen *{opacity:1!important;visibility:visible!important;transition:none!important;transform:none!important} .phone-screen-home{display:none!important}';
    if(mode==='smoothing-off')style.textContent='.object,.phone-screen *{transition:none!important;animation:none!important}';
    document.head.append(style);
   });
   if(mode==='pointer-off')window.addEventListener('pointermove',e=>e.stopImmediatePropagation(),true);
  },mode);
  const page=await context.newPage();const cdp=await context.newCDPSession(page);const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
  await cdp.send('Network.enable');await cdp.send('Network.setCacheDisabled',{cacheDisabled:true});
  const cold=Date.now();await page.goto(base);await page.evaluate(()=>document.fonts.ready);const coldLoadMs=Date.now()-cold;
  const warm=Date.now();await cdp.send('Network.setCacheDisabled',{cacheDisabled:false});await page.reload();await page.evaluate(()=>document.fonts.ready);const warmLoadMs=Date.now()-warm;
  await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(250);
  if(mode==='normal'||mode==='blur-off'){
   for(const p of [0,.25,.5,.75,1]){
    await page.evaluate(p=>scrollTo(0,(document.querySelector('.hero').offsetHeight-document.querySelector('.hero-stage').offsetHeight)*p),p);await page.waitForTimeout(250);
    await page.screenshot({path:`${out}/${width}-${p}.png`});
   }
   await page.evaluate(()=>scrollTo(0,0));await page.waitForTimeout(150);
   await cdp.send('Tracing.start',{categories:'devtools.timeline,blink.user_timing,cc,disabled-by-default-devtools.timeline.frame',transferMode:'ReturnAsStream'});
  }
  const metrics=await page.evaluate(async()=>{
   const hero=document.querySelector('.hero'),stage=document.querySelector('.hero-stage');const travel=hero.offsetHeight-stage.offsetHeight;
   const samples=[];let last=0;window.profile.reads=0;window.profile.rafCalls=0;window.profile.longTasks=[];
   // Identical 4-second input: slow forward, fast out/back, stop/start, reversals,
   // lower section return, and pointer motion. No app-specific timing assumptions.
   const start=performance.now();
   await new Promise(resolve=>{function tick(now){const t=(now-start)/1000;if(last)samples.push(now-last);last=now;
    let p=t<1?t*.8:t<1.5?.8+(t-1)*1.6:t<2?1.6-(t-1.5)*3.2:t<2.3?0:t<3?.5+.5*Math.sin((t-2.3)*22):t<3.5?4:Math.max(0,4-(t-3.5)*8);
    scrollTo(0,p*travel);document.dispatchEvent(new PointerEvent('pointermove',{clientX:innerWidth*(.5+.45*Math.sin(t*9)),clientY:400,bubbles:true}));
    if(t<4)requestAnimationFrame(tick);else resolve();}requestAnimationFrame(tick);});
   await new Promise(r=>setTimeout(r,250));
   samples.sort((a,b)=>a-b);return {p50:samples[Math.floor(samples.length*.5)],p95:samples[Math.floor(samples.length*.95)],max:Math.max(...samples),samples:samples.length,missed:samples.reduce((n,v)=>n+Math.max(0,Math.round(v/(1000/60))-1),0),...window.profile};
  });
  let traceSummary=null;
  if(mode==='normal'||mode==='blur-off'){
   const completion=new Promise(resolve=>cdp.once('Tracing.tracingComplete',resolve));await cdp.send('Tracing.end');const {stream}=await completion;let data='';
   for(;;){const r=await cdp.send('IO.read',{handle:stream});data+=r.data;if(r.eof)break;}await cdp.send('IO.close',{handle:stream});
   fs.writeFileSync(`${out}/${width}-${mode}-trace.json`,data);const trace=JSON.parse(data).traceEvents;
   traceSummary={};for(const name of ['Layout','UpdateLayoutTree','Paint','CompositeLayers','FunctionCall','RunTask']){const es=trace.filter(e=>e.name===name&&e.ph==='X');traceSummary[name]={count:es.length,totalMs:es.reduce((n,e)=>n+(e.dur||0)/1000,0),maxMs:Math.max(0,...es.map(e=>(e.dur||0)/1000))};}
  }
  const row={width,mode,coldLoadMs,warmLoadMs,errors,...metrics,traceSummary};runs.push(row);fs.writeFileSync(`${out}/results.json`,JSON.stringify(runs,null,2));console.log(JSON.stringify({width,mode,p50:row.p50,p95:row.p95,missed:row.missed,reads:row.reads,longTasks:row.longTasks.length,traceSummary}));await context.close();
 }}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1;});
