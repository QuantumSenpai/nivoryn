const {chromium}=require('@playwright/test');
const fs=require('node:fs');
const path=require('node:path');
const label=process.argv[2]||'before';
const scenario=process.argv[6]||'scroll';
const systemFonts=process.argv[5]==='system';
const trigger=process.argv[4]||'immediate';
const out=path.join('/private/tmp/nivoryn-first-scroll',label);fs.mkdirSync(out,{recursive:true});
const base=process.argv[3]||process.env.NIVORYN_TEST_URL||'http://127.0.0.1:4173';
(async()=>{
 let browser;const results=[];
 try{for(const width of (process.env.PROFILE_WIDTHS||'1440').split(',').map(Number))for(let run=0;run<3;run++){
  browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:process.argv[7]!=='headed'});
  const context=await browser.newContext({viewport:{width,height:900},deviceScaleFactor:2,isMobile:width<760,hasTouch:width<760});
  await context.addInitScript(({viewportWidth,trigger,systemFonts,scenario})=>{
   window.cold={frames:[],tasks:[],passes:[],errors:[]};
   new PerformanceObserver(list=>window.cold.tasks.push(...list.getEntries().map(e=>({start:e.startTime,duration:e.duration})))).observe({type:'longtask',buffered:true});
   let started=false;
   const start=()=>{
    if(started||!document.querySelector('.phone-screen'))return;started=true;observer.disconnect();
    const style=document.createElement('style');style.textContent='.object img{filter:none!important}'+(systemFonts?'*{font-family:Arial!important}':'');document.head.append(style);
    performance.mark('cold-dom');
    const hero=document.querySelector('.hero'),stage=document.querySelector('.hero-stage');
    const begin=performance.now();let last=begin,passStart=begin,index=0,acted=false;
    window.cold.domAt=begin;
    const travel=viewportWidth<760?420:680;const pin=0;
    function tick(now){
     const t=now-passStart;
     if(last)window.cold.frames.push({at:now,ms:now-last,pass:index});last=now;
     const fraction=Math.min(1,t/650);const p=fraction<.5?fraction*2:(1-fraction)*2;
     scrollTo(0,pin+p*travel);
     if(scenario==='pointer')stage.dispatchEvent(new PointerEvent('pointermove',{clientX:viewportWidth*(.5+.35*Math.sin(t/150)),clientY:400,bubbles:true}));
     if(scenario==='selection'&&t>100&&!acted){acted=true;document.querySelector('.phone-example-controls [data-select=link]')?.click();}
     if(t<650)requestAnimationFrame(tick);
     else{
      performance.mark('cold-pass-'+index+'-end');window.cold.passes.push({start:passStart,end:now});
      if(index===0){index++;last=0;setTimeout(()=>{passStart=performance.now();performance.mark('cold-pass-1-start');requestAnimationFrame(tick)},250)}
      else{window.cold.done=true;}
     }
    }
    performance.mark('cold-pass-0-start');requestAnimationFrame(tick);
   };
   const observer=new MutationObserver(start);
   if(trigger==='paint')new PerformanceObserver(list=>{if(list.getEntries().some(e=>e.name==='first-contentful-paint'))start()}).observe({type:'paint',buffered:true});
   else{observer.observe(document,{childList:true,subtree:true});addEventListener('DOMContentLoaded',start,{once:true});}
  },{viewportWidth:width,trigger,systemFonts,scenario});
  const page=await context.newPage();const cdp=await context.newCDPSession(page);
  await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});await cdp.send('Network.enable');
  for(const cache of ['cold','warm']){
   await cdp.send('Network.setCacheDisabled',{cacheDisabled:cache==='cold'});
   await cdp.send('Tracing.start',{categories:'devtools.timeline,blink.user_timing,loading,cc,disabled-by-default-devtools.timeline,disabled-by-default-devtools.timeline.frame',transferMode:'ReturnAsStream'});
   if(cache==='cold')await page.goto(base,{waitUntil:'commit'});else await page.reload({waitUntil:'commit'});
   await page.waitForFunction(()=>window.cold?.done);
   const measured=await page.evaluate(()=>{
    function summarize(pass){const a=window.cold.frames.filter(f=>f.pass===pass).map(f=>f.ms).sort((a,b)=>a-b);const range=window.cold.passes[pass];return {p50:a[Math.floor(a.length*.5)],p95:a[Math.floor(a.length*.95)],max:Math.max(...a),samples:a.length,missed:a.reduce((n,v)=>n+Math.max(0,Math.round(v/(1000/60))-1),0),longTasks:window.cold.tasks.filter(e=>e.start<range.end&&e.start+e.duration>range.start)};}
    return {paint:performance.getEntriesByType('paint').map(e=>({name:e.name,time:e.startTime})),domAt:window.cold.domAt,first:summarize(0),second:summarize(1),resources:performance.getEntriesByType('resource').map(r=>({name:r.name,start:r.startTime,end:r.responseEnd})),marks:performance.getEntriesByType('mark').map(m=>({name:m.name,time:m.startTime}))};
   });
   const complete=new Promise(r=>cdp.once('Tracing.tracingComplete',r));await cdp.send('Tracing.end');const {stream}=await complete;let data='';
   for(;;){const r=await cdp.send('IO.read',{handle:stream});data+=r.data;if(r.eof)break;}await cdp.send('IO.close',{handle:stream});
   const trace=JSON.parse(data).traceEvents;const marks=trace.filter(e=>e.name==='cold-pass-0-start'||e.name==='cold-pass-0-end'||e.name==='cold-pass-1-start'||e.name==='cold-pass-1-end');
   const spans=[0,1].map(i=>({start:marks.find(e=>e.name===`cold-pass-${i}-start`)?.ts||0,end:marks.find(e=>e.name===`cold-pass-${i}-end`)?.ts||0}));
   measured.costs=spans.map(s=>{const stats={};for(const name of ['Layout','UpdateLayoutTree','Paint','RasterTask','Decode Image','ImageDecodeTask','Commit','Layerize','FunctionCall','EvaluateScript','ParseAuthorStyleSheet']){
    const es=trace.filter(e=>e.name===name&&e.ph==='X'&&e.ts>=s.start&&e.ts<=s.end);stats[name]={count:es.length,ms:es.reduce((n,e)=>n+(e.dur||0)/1000,0),max:Math.max(0,...es.map(e=>(e.dur||0)/1000))};}return stats});
   measured.startupCosts={};for(const e of trace.filter(e=>e.ph==='X'&&e.ts<spans[0].end&&e.dur>1000)){const s=measured.startupCosts[e.name]??={count:0,ms:0,max:0};s.count++;s.ms+=e.dur/1000;s.max=Math.max(s.max,e.dur/1000);}
   if(run===0)fs.writeFileSync(`${out}/${width}-${cache}-trace.json`,data);
   results.push({width,run:run+1,cache,...measured});fs.writeFileSync(`${out}/results.json`,JSON.stringify(results,null,2));
   console.log(JSON.stringify({width,run:run+1,cache,first:measured.first,second:measured.second,costs:measured.costs}));
  }
  await context.close();await browser.close();
 }}finally{await browser?.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
