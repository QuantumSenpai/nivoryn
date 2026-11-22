const {chromium}=require('@playwright/test');
const fs=require('node:fs');
const path=require('node:path');
const label=process.argv[2]||'before',base=process.argv[3]||'http://127.0.0.1:4173';
const out=path.join('/private/tmp/nivoryn-beat-refine',label);fs.mkdirSync(out,{recursive:true});
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:false});
 const results=[];
 try{for(const mode of ['normal','normal','normal','text-frozen','objects-frozen','next-section-frozen']){
  const context=await browser.newContext({viewport:{width:1440,height:900},deviceScaleFactor:2});
  await context.addInitScript(mode=>{
   window.beatLab={frames:[],boundaries:[],tasks:[],sizes:[]};
   new PerformanceObserver(list=>window.beatLab.tasks.push(...list.getEntries().map(e=>({start:e.startTime,duration:e.duration})))).observe({type:'longtask',buffered:true});
   let started=false;const observer=new MutationObserver(()=>{
    if(started||!document.querySelector('.phone-screen'))return;started=true;observer.disconnect();
    const style=document.createElement('style');style.textContent='.object img{filter:none!important}';
    if(mode==='text-frozen')style.textContent+='.hero-initial{opacity:1!important;visibility:visible!important}.hero-resolution{opacity:0!important;visibility:hidden!important}';
    if(mode==='objects-frozen')style.textContent+='.object,.object-response{transform:none!important}';
    if(mode==='next-section-frozen')style.textContent+='.editorial{content-visibility:hidden!important}';
    document.head.append(style);
    new ResizeObserver(entries=>window.beatLab.sizes.push({at:performance.now(),height:entries[0].contentRect.height})).observe(document.querySelector('.hero-stage'));
    let last=0,lastP=0;const start=performance.now();performance.mark('beats-start');
    function tick(now){
     const t=now-start;let phase,p;
     if(t<800){phase='first';p=.1+.55*t/800;}
     else if(t<2000){phase='slow-reverse';p=.5+.12*Math.sin((t-800)/1200*Math.PI*4);}
     else if(t<2400){phase='fast';p=(1-Math.cos((t-2000)/400*Math.PI*2))/2;}
     else {phase='second';p=.1+.55*Math.min(1,(t-2400)/800);}
     if(last)window.beatLab.frames.push({at:now,ms:now-last,phase,p});last=now;
     if((p>=.5)!==(lastP>=.5)){window.beatLab.boundaries.push(now);performance.mark('text-boundary');}lastP=p;
     scrollTo(0,p*680);
     if(t<3200)requestAnimationFrame(tick);else {performance.mark('beats-end');window.beatLab.done=true;}
    }requestAnimationFrame(tick);
   });observer.observe(document,{childList:true,subtree:true});
  },mode);
  const page=await context.newPage(),errors=[];page.on('pageerror',e=>errors.push(e.message));
  const cdp=await context.newCDPSession(page);await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});await cdp.send('Network.enable');await cdp.send('Network.setCacheDisabled',{cacheDisabled:true});
  await cdp.send('Tracing.start',{categories:'devtools.timeline,blink.user_timing,cc,disabled-by-default-devtools.timeline,disabled-by-default-devtools.timeline.frame',transferMode:'ReturnAsStream'});
  await page.goto(base,{waitUntil:'commit'});await page.waitForFunction(()=>window.beatLab?.done);
  const result=await page.evaluate(()=>{
   function stats(a){const f=a.map(x=>x.ms).sort((a,b)=>a-b);return {samples:f.length,p50:f[Math.floor(f.length*.5)],p95:f[Math.floor(f.length*.95)],worst:Math.max(...f),missed:f.reduce((n,v)=>n+Math.max(0,Math.round(v/(1000/60))-1),0)}}
   const phases={};for(const p of ['first','slow-reverse','fast','second'])phases[p]=stats(window.beatLab.frames.filter(f=>f.phase===p));
   return {...window.beatLab,phases,boundaryFrames:stats(window.beatLab.frames.filter(f=>window.beatLab.boundaries.some(t=>Math.abs(t-f.at)<60)))};
  });
  const complete=new Promise(r=>cdp.once('Tracing.tracingComplete',r));await cdp.send('Tracing.end');const {stream}=await complete;let data='';for(;;){const r=await cdp.send('IO.read',{handle:stream});data+=r.data;if(r.eof)break;}await cdp.send('IO.close',{handle:stream});
  const events=JSON.parse(data).traceEvents;const marks=events.filter(e=>e.name==='text-boundary').map(e=>e.ts);
  result.boundaryCosts={};for(const name of ['Layout','UpdateLayoutTree','Paint','RasterTask','FunctionCall']){const e=events.filter(e=>e.ph==='X'&&e.name===name&&marks.some(t=>Math.abs(e.ts-t)<60000));result.boundaryCosts[name]={count:e.length,ms:e.reduce((n,e)=>n+(e.dur||0)/1000,0),max:Math.max(0,...e.map(e=>(e.dur||0)/1000))};}
  const id=results.filter(r=>r.mode===mode).length+1;fs.writeFileSync(`${out}/${mode}-${id}-trace.json`,data);
  results.push({mode,run:id,errors,...result});fs.writeFileSync(`${out}/results.json`,JSON.stringify(results,null,2));
  console.log(JSON.stringify({mode,run:id,phases:result.phases,boundary:result.boundaryFrames,costs:result.boundaryCosts,sizes:result.sizes}));
  await context.close();
 }}finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
