import { test } from 'node:test';
import assert from 'node:assert/strict';
import { decorativeBlur, createRafScheduler } from '../product-motion.mjs';

test('decorative blur uses physical velocity, caps cost, and is zero at rest', () => {
  assert.equal(decorativeBlur(0, 16, .6), 0);
  assert.equal(decorativeBlur(.1, 16, .6), 0, 'slow reading motion remains sharp');
  assert.ok(decorativeBlur(4, 16, .6) > 0);
  assert.equal(decorativeBlur(100, 16, .6), .6);
  assert.equal(decorativeBlur(4, 0, .6), 0);
  assert.equal(decorativeBlur(4, 16, 0), 0, 'reduced motion / limited device disables blur');
  assert.equal(decorativeBlur(4, 16, .6), decorativeBlur(2, 8, .6), 'refresh-rate independent velocity');
  assert.equal(decorativeBlur(-4, 16, .6), decorativeBlur(4, 16, .6), 'reversal responds immediately');
});

test('frame scheduler coalesces input, does not idle-loop, and cancels on suspension', () => {
  let calls = 0, callback, cancelled;
  globalThis.requestAnimationFrame = fn => { calls++; callback = fn; return calls; };
  globalThis.cancelAnimationFrame = id => { cancelled = id; };
  let updates = 0;
  const scheduler = createRafScheduler(() => updates++);
  for (let i=0;i<100;i++) scheduler.schedule();
  assert.equal(calls, 1);
  callback(16);
  assert.equal(updates, 1);
  assert.equal(calls, 1, 'settled scene does not schedule another frame');
  scheduler.schedule();scheduler.cancel();
  assert.equal(cancelled, 2);
});
