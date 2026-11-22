const {chromium}=require('@playwright/test');
const fs=require('node:fs');
const path=require('node:path');
const root=path.resolve(__dirname,'..');
const base=process.argv[2]||'http://127.0.0.1:4178';
(async()=>{
 const browser=await chromium.launch({executablePath:'/Applications/Brave Browser.app/Contents/MacOS/Brave Browser',headless:true});
 try{
  const page=await browser.newPage({deviceScaleFactor:1});
  await page.goto(base+'/favicon.svg');
  const images=[];
  for(const size of [16,32,48]){
   await page.setViewportSize({width:size,height:size});
   await page.locator('svg').evaluate((el,s)=>{el.setAttribute('width',s);el.setAttribute('height',s)},size);
   const png=await page.screenshot({omitBackground:true});images.push({size,png});
   if(size===48)fs.writeFileSync(path.join(root,'favicon-48.png'),png);
  }
  const header=Buffer.alloc(6+16*images.length);header.writeUInt16LE(1,2);header.writeUInt16LE(images.length,4);
  let offset=header.length;
  images.forEach(({size,png},i)=>{const at=6+16*i;header[at]=size;header[at+1]=size;header.writeUInt16LE(1,at+4);header.writeUInt16LE(32,at+6);header.writeUInt32LE(png.length,at+8);header.writeUInt32LE(offset,at+12);offset+=png.length});
  fs.writeFileSync(path.join(root,'favicon.ico'),Buffer.concat([header,...images.map(x=>x.png)]));
  await page.setViewportSize({width:180,height:180});
  await page.locator('svg').evaluate(el=>{el.setAttribute('width',180);el.setAttribute('height',180);el.querySelector('rect').setAttribute('rx',0)});
  await page.screenshot({path:path.join(root,'apple-touch-icon.png')});
  await page.setViewportSize({width:1200,height:630});await page.goto(base+'/assets/brand/social-card.svg');await page.evaluate(()=>document.fonts.ready);
  await page.screenshot({path:path.join(root,'assets/brand/social-card.png')});
  for(const [name,height] of [['banner',400],['flow',250]]){
   await page.goto(base+'/notices.html');await page.setViewportSize({width:1200,height});
   const svg=fs.readFileSync(path.join(root,'../assets/readme',name+'.svg'),'utf8');
   await page.setContent(`<html><head><style>@font-face{font-family:Manrope;src:url('${base}/assets/fonts/manrope.woff2')}body{margin:0}svg{display:block}*{animation:none!important}</style></head><body>${svg}</body></html>`);
   await page.evaluate(()=>document.fonts.ready);await page.screenshot({path:path.join(root,'../assets/readme',name+'-preview.png')});
  }
  console.log('Rendered SVG favicon variants, social preview and README previews.');
 }finally{await browser.close()}
})().catch(e=>{console.error(e);process.exitCode=1});
