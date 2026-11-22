import {test} from 'node:test';
import assert from 'node:assert/strict';
import {dampPointer} from '../product-motion.mjs';
test('cursor damping is time based, bounded, reversible, and settles exactly',()=>{
 const one=dampPointer(0,1,16);
 const half=dampPointer(dampPointer(0,1,8),1,8);
 assert.ok(Math.abs(one-half)<1e-10);
 assert.ok(one>0&&one<1);
 assert.ok(dampPointer(one,-1,16)<one);
 let p=1;for(let i=0;i<20;i++)p=dampPointer(p,0,16);
 assert.equal(p,0,'no perpetual drift after leaving');
});
