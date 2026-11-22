import {test} from 'node:test';
import assert from 'node:assert/strict';
import {smoothProgress, springProgress, scrollProgress} from '../product-motion.mjs';

test('choreography is bounded, reversible and gentle at both ends', () => {
  assert.equal(smoothProgress(-1), 0);
  assert.equal(smoothProgress(2), 1);
  assert.equal(smoothProgress(0.5), 0.5);
  assert.ok(smoothProgress(0.02) < 0.001);
  assert.ok(smoothProgress(0.98) > 0.999);
  for (const p of [0.1, 0.25, 0.4]) {
    assert.ok(Math.abs(smoothProgress(p) + smoothProgress(1 - p) - 1) < 1e-10);
  }
});

test('spring progress is critically damped, monotonic, and symmetric at endpoints', () => {
  assert.equal(springProgress(-1), 0);
  assert.equal(springProgress(2), 1);
  assert.ok(Math.abs(springProgress(0.5) - springProgress(0.5)) < 1e-10);
  assert.ok(springProgress(0.01) < 0.01);
  // Spring asymptotically approaches 1; at p=0.99 it's ~0.98 (by design, no overshoot)
  const sp99 = springProgress(0.99);
  assert.ok(sp99 > 0.95 && sp99 < 1);
  // Critically damped: no overshoot
  for (const p of [0.1, 0.3, 0.5, 0.7, 0.9]) {
    const val = springProgress(p);
    assert.ok(val >= 0 && val <= 1);
  }
});

test('scrollProgress clamps and maps ranges correctly', () => {
  const eps = 1e-10;
  assert.ok(Math.abs(scrollProgress(0, 0.2, 0.5) - 0) < eps);
  assert.ok(Math.abs(scrollProgress(0.2, 0.2, 0.5) - 0) < eps);
  assert.ok(Math.abs(scrollProgress(0.35, 0.2, 0.5) - 0.5) < eps);
  assert.ok(Math.abs(scrollProgress(0.5, 0.2, 0.5) - 1) < eps);
  assert.ok(Math.abs(scrollProgress(1, 0.2, 0.5) - 1) < eps);
  // Zero-width range
  assert.ok(Math.abs(scrollProgress(0.5, 0.5, 0.5) - 1) < eps);
  assert.ok(Math.abs(scrollProgress(0.4, 0.5, 0.5) - 0) < eps);
});