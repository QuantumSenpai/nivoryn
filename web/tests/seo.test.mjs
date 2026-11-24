import {test} from 'node:test';
import assert from 'node:assert/strict';
import {readFileSync,existsSync} from 'node:fs';

const root=new URL('../',import.meta.url);
const read=file=>readFileSync(new URL(file,root),'utf8');
const origin='https://nivoryn.vercel.app';
const pages=['index.html','privacy.html','terms.html','notices.html','accessibility.html'];
const canonical=file=>origin+(file==='index.html'?'/':'/'+file);
const attributes=tag=>Object.fromEntries([...tag.matchAll(/([\w:-]+)="([^"]*)"/g)].map(m=>[m[1],m[2]]));

test('every indexable page has unique descriptions and consistent production social/canonical URLs',()=>{
 const titles=new Set(),descriptions=new Set();
 for(const file of pages){
  const html=read(file),head=html.match(/<head>([\s\S]*?)<\/head>/)[1];
  const title=head.match(/<title>(.*?)<\/title>/)[1];
  const meta=[...head.matchAll(/<meta\b[^>]*>/g)].map(m=>attributes(m[0]));
  const get=(key,value)=>meta.filter(m=>m[key]===value);
  assert.equal(get('name','description').length,1);const description=get('name','description')[0].content;
  assert.ok(description.length>=60&&description.length<=180);
  titles.add(title);descriptions.add(description);
  const links=[...head.matchAll(/<link\b[^>]*>/g)].map(m=>attributes(m[0]));
  assert.deepEqual(links.filter(l=>l.rel==='canonical').map(l=>l.href),[canonical(file)]);
  assert.equal(get('property','og:url')[0].content,canonical(file));
  assert.equal(get('property','og:title')[0].content,title);
  assert.equal(get('property','og:description')[0].content,description);
  assert.equal(get('name','twitter:card')[0].content,'summary_large_image');
  assert.equal(get('property','og:image')[0].content,origin+'/assets/brand/social-card.png');
  assert.ok(!get('name','robots')[0].content.includes('noindex'));
  for(const link of links.filter(l=>['icon','apple-touch-icon'].includes(l.rel)))assert.ok(existsSync(new URL('.'+link.href,root)),link.href);
 }
 assert.equal(titles.size,pages.length);assert.equal(descriptions.size,pages.length);
});

test('sitemap and robots expose the canonical pages without blocking favicon crawling',()=>{
 const locations=[...read('sitemap.xml').matchAll(/<loc>(.*?)<\/loc>/g)].map(m=>m[1]);
 assert.deepEqual(locations.sort(),pages.map(canonical).sort());
 assert.match(read('robots.txt'),/Allow: \/\n/);assert.doesNotMatch(read('robots.txt'),/Disallow: \/(?:\n|$)/);
 assert.ok(read('robots.txt').includes(origin+'/sitemap.xml'));
 const config=JSON.parse(read('vercel.json'));
 assert.ok(config.redirects.some(r=>r.source==='/index.html'&&r.destination==='/'&&r.permanent));
});

test('app structured data describes the actual tested release without fabricated ratings',()=>{
 const data=JSON.parse(read('index.html').match(/<script type="application\/ld\+json">([\s\S]*?)<\/script>/)[1]);
 const app=data['@graph'].find(x=>x['@type']==='SoftwareApplication');
 const release=JSON.parse(read('release.json'));
 assert.equal(app.softwareVersion,release.version);assert.equal(app.downloadUrl,origin+'/downloads/'+release.filename);
 assert.equal(app.operatingSystem,'Android 8.0 or newer');assert.equal(app.aggregateRating,undefined);assert.equal(app.review,undefined);
});

test('visible download versions agree with the release metadata',()=>{
 const release=JSON.parse(read('release.json'));
 for(const file of ['index.html','notices.html']){
  const visible=read(file).replace(/<script\b[^>]*>[\s\S]*?<\/script>/g,'').replace(/<[^>]*>/g,' ');
  const versions=[...visible.matchAll(/\b(?:Version |test build )(\d+\.\d+\.\d+)\b/g)].map(m=>m[1]);
  assert.ok(versions.length>0,file);
  assert.deepEqual([...new Set(versions)],[release.version],file);
 }
});

test('generated favicon and social images have real, correctly sized raster payloads',()=>{
 for(const [file,width,height] of [['favicon-48.png',48,48],['apple-touch-icon.png',180,180],['assets/brand/social-card.png',1200,630]]){
  const png=readFileSync(new URL(file,root));assert.equal(png.subarray(1,4).toString(),'PNG');
  assert.equal(png.readUInt32BE(16),width);assert.equal(png.readUInt32BE(20),height);
 }
 const ico=readFileSync(new URL('favicon.ico',root));assert.equal(ico.readUInt16LE(2),1);assert.equal(ico.readUInt16LE(4),3);
 for(let i=0;i<3;i++){
  assert.equal(ico[6+16*i],[16,32,48][i]);
  const offset=ico.readUInt32LE(6+16*i+12);assert.equal(ico.subarray(offset+1,offset+4).toString(),'PNG');
 }
});
