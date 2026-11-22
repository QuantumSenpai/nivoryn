/** Scroll-bound utilities. Frame pacing is measured in tests, not inferred from CSS. */

/** Time-based, interruptible cursor response. Snaps to neutral instead of drifting forever. */
export function dampPointer(current, target, elapsedMs) {
  const next = current + (target - current) * (1 - Math.exp(-Math.max(0, elapsedMs) / 32));
  return Math.abs(next - target) < .001 ? target : next;
}

/** Blur only a moving decorative surface; elapsed time makes it refresh-rate independent. */
export function decorativeBlur(distance, elapsedMs, cap) {
  if (elapsedMs <= 0 || cap <= 0) return 0;
  const speed = Math.abs(distance) / elapsedMs;
  return speed > .025 ? Math.min(cap, (speed - .025) * 4) : 0;
}

/** Existing travel expanded ~13.5% (10-15% range); formerly static supports get small bounded separation. */
export function heroPose(progress) {
  const assemble = smoothProgress(scrollProgress(progress, .1, .4));
  const reveal = smoothProgress(scrollProgress(progress, .4, .65));
  const fanOut = 1.135; // ~13.5% increase for stronger fan-out
  return {
    phone: [-8 * assemble * fanOut, -6 * assemble * fanOut],
    envelope: [-20.25 * assemble * fanOut, 11.25 * assemble * fanOut],
    message: [-6 * reveal * fanOut, -27 * reveal * fanOut],
    document: [8 * assemble * fanOut, -5 * assemble * fanOut],
    url: [6 * assemble * fanOut, 5 * assemble * fanOut]
  };
}

/**
 * 7th-order polynomial easing: zero velocity & acceleration at endpoints.
 * Smoother entry/exit than 5th-order.
 */
export function smoothProgress(value) {
  const p = Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
  return p * p * p * p * (35 - p * (84 - p * (70 - 20 * p)));
}

/**
 * Critically-damped spring approximation for UI transitions.
 * k=6 gives ~0.35s response feel, no overshoot.
 * Returns exactly 1 at p=1 for scroll-bound completeness.
 */
export function springProgress(value) {
  const p = Number.isFinite(value) ? Math.max(0, Math.min(1, value)) : 0;
  if (p >= 1) return 1;
  if (p <= 0) return 0;
  const k = 6;
  return 1 - Math.exp(-k * p) * (1 + k * p);
}

/**
 * Scroll progress helper: maps scroll position to 0-1 within a range.
 */
export function scrollProgress(p, start, end) {
  if (end <= start) return p >= start ? 1 : 0;
  const t = (p - start) / (end - start);
  return t < 0 ? 0 : t > 1 ? 1 : t;
}

/**
 * Creates a high-performance RAF scheduler that:
 * - Runs at most once per frame
 * - Cancels cleanly on visibility change / reduced motion
 * - Applies DOM writes SYNCHRONOUSLY within the RAF callback (not microtask)
 *   to guarantee they hit the same frame.
 */
export function createRafScheduler(updateFn, { reduceMotion, isVisible } = {}) {
  let rafId = 0;
  let pending = false;

  function schedule() {
    if (pending) return;
    if (reduceMotion?.matches) return;
    if (isVisible && !isVisible()) return;
    pending = true;
    rafId = requestAnimationFrame(run);
  }

  function run(now) {
    pending = false;
    updateFn(now);
  }

  function cancel() {
    if (rafId) cancelAnimationFrame(rafId);
    rafId = 0;
    pending = false;
  }

  return { schedule, cancel };
}

/**
 * Applies transforms SYNCHRONOUSLY (not in microtask) to guarantee
 * they apply within the same frame as the RAF callback.
 * Uses translate3d for GPU compositing.
 */
export function applyTransforms(pairs) {
  for (const [el, transform] of pairs) {
    if (el && transform !== undefined) {
      el.style.transform = transform;
    }
  }
}

/**
 * Applies opacity SYNCHRONOUSLY.
 */
export function applyOpacity(pairs) {
  for (const [el, opacity] of pairs) {
    if (el && opacity !== undefined) {
      el.style.opacity = String(opacity);
    }
  }
}
