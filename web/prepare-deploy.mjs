import {readFile, mkdir, cp, copyFile, writeFile, readdir} from 'node:fs/promises';
import {createReadStream} from 'node:fs';
import {createHash} from 'node:crypto';
import {resolve, join} from 'node:path';
import assert from 'node:assert/strict';
import {execFileSync} from 'node:child_process';

const root=import.meta.dirname;
const output=resolve(process.argv[2] || '/Volumes/NivorynBuildCache/nivoryn-vercel');
const apk=resolve(root,'../app/build/outputs/apk/preview/app-preview.apk');
const release=JSON.parse(await readFile(join(root,'release.json'),'utf8'));
assert.equal(release.buildVariant,'preview','Only the non-debuggable signed preview may be published');
execFileSync('python3',[resolve(root,'../scripts/verify-distribution-apk.py'),apk,'--version',release.version,'--version-code',String(release.versionCode)],{stdio:'inherit'});
const digest=createHash('sha256');let bytes=0;
for await(const chunk of createReadStream(apk)){digest.update(chunk);bytes+=chunk.length;}
assert.equal(digest.digest('hex'),release.sha256,'APK changed: update and reverify release metadata before publishing');
assert.equal(bytes,release.bytes);
assert.match(release.filename,/^nivoryn-[\d.]+-arm64-[a-f0-9]{12}\.apk$/);
const html=await readFile(join(root,'dist/index.html'),'utf8');
assert.equal(html.split(`href="downloads/${release.filename}" download`).length-1,2,'Both download buttons must use this tested APK');
await mkdir(output,{recursive:true});
const entries=await readdir(output);
assert.ok(entries.length===0 || entries.includes('.nivoryn-package'),'Refusing to package over an unrelated directory');
await writeFile(join(output,'.nivoryn-package'),'Nivoryn static website and verified preview APK\n');
for(const entry of await readdir(join(root,'dist')))await cp(join(root,'dist',entry),join(output,entry),{recursive:true});
await mkdir(join(output,'downloads'),{recursive:true});
// Never retain a second large APK in this upload: refuse rather than deleting it.
const old=(await readdir(join(output,'downloads'))).filter(file=>file.endsWith('.apk')&&file!==release.filename);
assert.equal(old.length,0,'Use a fresh package directory for a different APK version');
await copyFile(apk,join(output,'downloads',release.filename));
await writeFile(join(output,'downloads/SHA256SUMS.txt'),`${release.sha256}  ${release.filename}\n`);
await copyFile(join(root,'vercel.json'),join(output,'vercel.json'));
await writeFile(join(output,'.vercelignore'),'.nivoryn-package\n.env\n.env.*\n.vercel\n.git\n.gitignore\n');
console.log(JSON.stringify({directory:output,apk:release.filename,bytes,sha256:release.sha256}));
