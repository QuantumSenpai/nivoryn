const {chromium}=require('@playwright/test');
const fs=require('node:fs');
const {createHash}=require('node:crypto');
const assert=require('node:assert/strict');
const release=require('../release.json');
const base=process.argv[2]||'http://127.0.0.1:4177';
(async()=>{
 const downloads='/Volumes/NivorynBuildCache/nivoryn-download-check';fs.mkdirSync(downloads,{recursive:true});
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true,downloadsPath:downloads});
 try{
  const page=await browser.newPage();await page.goto(base);
  const links=page.getByRole('link',{name:'Download APK',exact:true});assert.equal(await links.count(),2);
  const href=await links.last().getAttribute('href');assert.equal(href,`downloads/${release.filename}`);
  const response=await page.request.head(new URL(href,base).href);assert.equal(response.status(),200);
  assert.equal(Number(response.headers()['content-length']),release.bytes);
  if(base.startsWith('https:')){
   assert.match(response.headers()['content-type'],/application\/vnd.android.package-archive/);
   assert.match(response.headers()['content-disposition'],/attachment/);
  }
  const pending=page.waitForEvent('download');await links.last().click();const download=await pending;
  assert.equal(download.suggestedFilename(),release.downloadName);
  const path=await download.path();assert.equal(await download.failure(),null);
  const hash=createHash('sha256');for await(const chunk of fs.createReadStream(path))hash.update(chunk);
  assert.equal(hash.digest('hex'),release.sha256,'downloaded APK must be byte-for-byte the tested build');
  assert.equal(fs.statSync(path).size,release.bytes);
  console.log(JSON.stringify({url:new URL(href,base).href,bytes:release.bytes,sha256:release.sha256,download:true}));
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exitCode=1});
