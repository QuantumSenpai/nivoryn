const {chromium}=require('@playwright/test');
const assert=require('node:assert/strict');
const base=process.argv[2]||'https://nivoryn.vercel.app';
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try{
  const page=await browser.newPage();
  for(const file of ['','privacy.html','terms.html','notices.html','accessibility.html']){
   const url=base+'/'+file,response=await page.goto(url);assert.equal(response.status(),200);
   assert.ok(!/noindex/.test(response.headers()['x-robots-tag']||''),'production pages remain indexable');
   assert.equal(await page.locator('link[rel=canonical]').getAttribute('href'),url);
   assert.equal(await page.locator('meta[property="og:url"]').getAttribute('content'),url);
   assert.equal(await page.locator('meta[name="description"]').count(),1);
   assert.equal(await page.locator('link[rel=icon][type="image/svg+xml"]').getAttribute('href'),'/favicon.svg');
  }
  for(const [file,type] of [['favicon.svg','image/svg+xml'],['favicon.ico','image/'],['favicon-48.png','image/png'],['apple-touch-icon.png','image/png'],['assets/brand/social-card.png','image/png'],['sitemap.xml','xml'],['robots.txt','text/plain']]){
   const response=await page.request.get(base+'/'+file);assert.equal(response.status(),200,file);
   assert.ok(response.headers()['content-type'].includes(type),file+' has correct MIME type');
  }
  const redirect=await page.request.get(base+'/index.html',{maxRedirects:0});assert.ok([301,308].includes(redirect.status()));
  console.log('PASS: five public canonical pages, metadata, crawlable favicon/social files, sitemap/robots and index redirect');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
