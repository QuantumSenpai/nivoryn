import {test} from 'node:test';
import assert from 'node:assert/strict';
import {heroPose} from '../product-motion.mjs';
const fanOut = 1.135;
test('fan-out is bounded, anchored, and deterministic through reversals',()=>{
 assert.ok(heroPose(0).phone.every(value=>value===0));
 assert.deepEqual(heroPose(1).phone,[-8 * fanOut,-6 * fanOut]);
 assert.deepEqual(heroPose(1).message,[-6 * fanOut,-27 * fanOut]);
 assert.deepEqual(heroPose(1).document,[8 * fanOut,-5 * fanOut]);
 assert.deepEqual(heroPose(1).url,[6 * fanOut,5 * fanOut]);
 for(const p of [0,.25,.5,.75,1,.5,0]){
  assert.deepEqual(heroPose(p),heroPose(p),'same progress restores exactly the same pose');
  for(const v of Object.values(heroPose(p)).flat())assert.ok(Number.isFinite(v)&&Math.abs(v)<=27 * fanOut);
 }
 assert.deepEqual(heroPose(-1),heroPose(0));assert.deepEqual(heroPose(2),heroPose(1));
});
