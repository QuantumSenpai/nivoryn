const {chromium}=require(process.env.NIVORYN_PLAYWRIGHT_MODULE || '@playwright/test');
const fs=require('node:fs');const assert=require('node:assert/strict');
(async()=>{const browser=await chromium.launch({executablePath:process.env.CHROME_BIN || undefined,headless:true});const out=process.env.NIVORYN_BROWSER_OUTPUT || '/private/tmp/nivoryn-browser';fs.mkdirSync(out,{recursive:true});let checks=[];
for(const width of [320,390,768,1024,1440]){
 const context=await browser.newContext({viewport:{width,height:900},colorScheme:'light'});const page=await context.newPage();const errors=[],requests=[];
 page.on('pageerror',e=>errors.push(e.message));page.on('request',r=>requests.push(r.url()));
 await page.goto('http://127.0.0.1:4173');await page.evaluate(()=>document.fonts.ready);
 assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true,`overflow ${width}`);
 assert.equal(await page.locator('img').evaluateAll(imgs=>imgs.every(i=>i.complete&&i.naturalWidth>0)),true);
 await page.screenshot({path:`${out}/${width}-home.png`,fullPage:true});
 await page.locator('#review').click();await page.getByRole('button',{name:'Show example result',exact:true}).click();await page.getByRole('heading',{name:/High risk/}).waitFor();await page.locator('#demo').screenshot({path:`${out}/${width}-result.png`});
 await page.locator('#message').fill('My private edited draft');await page.locator('#review').click();await page.getByRole('button',{name:'View checking availability'}).click();await page.getByRole('heading',{name:'Cannot assess'}).waitFor();assert.equal(await page.getByText('Custom analysis is not connected yet. No verdict has been assigned to your input.').isVisible(),true);
 await page.locator('#story-toggle').click();assert.equal(await page.locator('#payment-message').isVisible(),true);await page.locator('#story-toggle').click();assert.equal(await page.locator('#payment-message').isVisible(),false);
 await page.locator('[data-open-preferences]').last().click();await page.keyboard.press('Tab');assert.equal(await page.evaluate(()=>document.querySelector('#preferences').contains(document.activeElement)),true);await page.locator('#appearance').selectOption('dark');await page.locator('#reduced-motion').check();await page.getByRole('button',{name:'Done',exact:true}).click();assert.equal(await page.locator('[data-open-preferences]').last().evaluate(el=>el===document.activeElement),true);
 await page.reload();assert.equal(await page.locator('html').getAttribute('data-theme'),'dark');assert.equal(await page.locator('html').getAttribute('data-reduced-motion'),'true');
 if(width===390){await page.screenshot({path:`${out}/${width}-dark.png`});await page.locator('.menu-toggle').click();assert.equal(await page.locator('.menu-toggle').getAttribute('aria-expanded'),'true');await page.keyboard.press('Escape');assert.equal(await page.locator('.menu-toggle').getAttribute('aria-expanded'),'false');}
 for(const path of ['privacy.html','accessibility.html','notices.html']){const r=await page.goto('http://127.0.0.1:4173/'+path);assert.equal(r.status(),200);assert.equal(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),true);}
 assert.deepEqual(errors,[]);assert.deepEqual(requests.filter(url=>!url.startsWith('http://127.0.0.1:4173')),[]);
 checks.push({width,overflow:false,errors:0,externalRequests:0,demo:true,customDraft:true,story:true,preferences:true,policies:true});await context.close();
}
await browser.close();fs.writeFileSync(out+'/report.json',JSON.stringify(checks,null,2));console.log(JSON.stringify(checks,null,2));})().catch(e=>{console.error(e);process.exitCode=1});
