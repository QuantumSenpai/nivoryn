const {chromium}=require('@playwright/test');const fs=require('node:fs');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.CHROME_BIN || '/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});const runs=[];
 for(const width of [390,1440])for(let run=0;run<3;run++){
  const context=await browser.newContext({viewport:{width,height:900},isMobile:width===390,hasTouch:width===390});
  await context.addInitScript(()=>{
   window.lab={lcp:0,cls:0,interactions:[],longTasks:0};
   new PerformanceObserver(list=>{for(const e of list.getEntries())window.lab.lcp=e.startTime;}).observe({type:'largest-contentful-paint',buffered:true});
   new PerformanceObserver(list=>{for(const e of list.getEntries())if(!e.hadRecentInput)window.lab.cls+=e.value;}).observe({type:'layout-shift',buffered:true});
   new PerformanceObserver(list=>{for(const e of list.getEntries())if(e.interactionId)window.lab.interactions.push(e.duration);}).observe({type:'event',durationThreshold:16,buffered:true});
   new PerformanceObserver(list=>{window.lab.longTasks+=list.getEntries().length;}).observe({type:'longtask',buffered:true});
  });
  const page=await context.newPage();const cdp=await context.newCDPSession(page);
  await cdp.send('Network.enable');await cdp.send('Network.setCacheDisabled',{cacheDisabled:true});
  await cdp.send('Network.emulateNetworkConditions',{offline:false,latency:150,downloadThroughput:1600000/8,uploadThroughput:750000/8});await cdp.send('Emulation.setCPUThrottlingRate',{rate:4});
  await page.goto('http://127.0.0.1:4173');await page.evaluate(()=>document.fonts.ready);await page.waitForTimeout(700);
  const initial=await page.evaluate(()=>({...window.lab,bytes:performance.getEntriesByType('resource').reduce((n,r)=>n+r.transferSize,0)}));
  await page.getByRole('tab',{name:'Message',exact:true}).click();await page.getByRole('tab',{name:'Document',exact:true}).click();await page.getByRole('tab',{name:'Link',exact:true}).click();await page.waitForTimeout(200);
  const interactions=await page.evaluate(()=>window.lab.interactions);
  await page.evaluate(()=>new Promise(resolve=>{window.frameDeltas=[];let last=0;const end=performance.now()+1300;function frame(now){if(last)window.frameDeltas.push(now-last);last=now;if(now<end)requestAnimationFrame(frame);else resolve();}requestAnimationFrame(frame)}));
  const frames=await page.evaluate(()=>({p95:window.frameDeltas.sort((a,b)=>a-b)[Math.floor(window.frameDeltas.length*.95)],count:window.frameDeltas.length}));
  runs.push({width,run:run+1,lcpMs:Math.round(initial.lcp),cls:Number(initial.cls.toFixed(4)),resourceTransferBytes:initial.bytes,maxObservedInteractionMs:Math.max(0,...interactions),idleRafP95Ms:Math.round(frames.p95*100)/100,frameSamples:frames.count,longTasksThroughInitialLoad:initial.longTasks});await context.close();
 }
 fs.mkdirSync('/private/tmp/nivoryn-product-browser',{recursive:true});fs.writeFileSync('/private/tmp/nivoryn-product-browser/performance.json',JSON.stringify({conditions:'Headless Brave on host Mac; 4x CPU slowdown, 1.6Mbit/s down, 750Kbit/s up, 150ms latency, cache disabled. Three cold contexts per width. Event Timing interaction maximum is a lab proxy, not field INP. Idle rAF intervals are not a hardware scroll smoothness measurement.',runs},null,2));console.log(JSON.stringify(runs,null,2));await browser.close();
})().catch(e=>{console.error(e);process.exitCode=1});
