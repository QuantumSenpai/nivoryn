import { exampleFor } from './product-examples.mjs';
import { smoothProgress, scrollProgress, createRafScheduler, decorativeBlur, dampPointer, heroPose } from './product-motion.mjs';

const hero = document.querySelector('.hero');
const stage = document.querySelector('.hero-stage');
const reduce = matchMedia('(prefers-reduced-motion: reduce)');
const dataSaving = navigator.connection?.saveData === true;
const tabList = document.querySelector('.example-tabs');
const tabs = [...document.querySelectorAll('.example-tabs [data-example]')];
const panels = [...document.querySelectorAll('.example-panel')];
const screen = document.querySelector('.phone-screen');
let selectedKind = 'message';
let selectionAnimations = [];
// Prepare the critical phone image asynchronously; never gate input.
Promise.allSettled([...document.querySelectorAll('.phone-frame')].map(image => image.decode())).then(() => performance.mark('hero-phone-decoded'));

// Selection owns content. Scrolling never changes the selected example or display.
function selectExample(kind, { focus = false, updateURL = true } = {}) {
  const example = exampleFor(kind);
  if (!example) return;
  const changed = selectedKind !== kind;
  selectedKind = kind;
  for (const tab of tabs) {
    const selected = tab.dataset.example === kind;
    tab.setAttribute('aria-selected', String(selected));
    tab.tabIndex = selected ? 0 : -1;
    if (selected && focus) tab.focus();
  }
  for (const panel of panels) panel.hidden = panel.id !== `example-${kind}`;
  for (const control of document.querySelectorAll('[data-select]')) {
    control.setAttribute('aria-pressed', String(control.dataset.select === kind));
  }
  if (screen && screen.dataset.example !== kind) {
    screen.dataset.example = kind;
    screen.querySelector('.phone-input').textContent = example.input;
    screen.querySelector('.phone-result h3').textContent = kind === 'message' || kind === 'screenshot' ? 'High risk' : kind === 'link' ? 'Suspicious' : 'Cannot assess';
    screen.querySelector('.phone-evidence b').textContent = example.evidence;
    screen.querySelector('.phone-evidence p').textContent = `“${example.quote}”`;
    screen.querySelector('.phone-next').textContent = example.next;
    const resolution = document.querySelector('.hero-resolution');
    resolution.querySelector('h2').textContent = kind === 'message' ? 'A promise.\nWith a price.' : example.title;
    resolution.querySelectorAll('p')[0].textContent = `“${example.quote}”`;
    resolution.querySelectorAll('p')[1].textContent = example.explanation;
  }
  if (updateURL) history.replaceState(null, '', `#example-${kind}`);
  selectionAnimations.forEach(animation => animation.cancel());
  selectionAnimations = [];
  if (changed && !reduce.matches) {
    for (const element of [screen?.querySelector('.phone-evidence'), document.querySelector(`#example-${kind} .example-input-card`)]) {
      if (element) selectionAnimations.push(element.animate([{ opacity: .82 }, { opacity: 1 }], { duration: 200, easing: 'cubic-bezier(.23,1,.32,1)' }));
    }
  }
}

if (tabList) {
  tabList.setAttribute('role', 'tablist');
  tabs.forEach((tab, index) => {
    tab.id = `tab-${tab.dataset.example}`;
    tab.setAttribute('role', 'tab');
    tab.setAttribute('aria-controls', `example-${tab.dataset.example}`);
    tab.addEventListener('click', event => { event.preventDefault(); selectExample(tab.dataset.example); });
    tab.addEventListener('keydown', event => {
      let next;
      if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
      if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
      if (event.key === 'Home') next = 0;
      if (event.key === 'End') next = tabs.length - 1;
      if (next !== undefined) { event.preventDefault(); selectExample(tabs[next].dataset.example, { focus: true }); }
    });
  });
  for (const panel of panels) {
    panel.setAttribute('role', 'tabpanel');
    panel.setAttribute('aria-labelledby', `tab-${panel.id.slice(8)}`);
    panel.tabIndex = 0;
  }
  const restoreSelection = () => selectExample(location.hash.startsWith('#example-') ? location.hash.slice(9) : 'message', { updateURL: false });
  addEventListener('hashchange', restoreSelection);
  addEventListener('popstate', restoreSelection);
  restoreSelection();
}

// Existing rendered objects are buttons; the phone's Screenshot control is the
// screenshot specimen. The wrapper owns travel, its child keeps the base tilt.
for (const [selector, kind] of [['.message-slip','message'], ['.envelope-object','message'], ['.url-object','link'], ['.document-object','document']]) {
  const object = document.querySelector(selector);
  if (!object) continue;
  object.dataset.select = kind;
  object.setAttribute('role', 'button');
  object.setAttribute('aria-label', `View ${kind} example`);
  object.tabIndex = 0;
  object.addEventListener('click', () => selectExample(kind));
  object.addEventListener('keydown', event => {
    if (event.key === 'Enter' || event.key === ' ') { event.preventDefault(); selectExample(kind); }
  });
}
for (const button of document.querySelectorAll('.phone-example-controls button')) {
  button.addEventListener('click', () => selectExample(button.dataset.select));
}
screen?.querySelector('.phone-action').addEventListener('click', () => {
  const panel = document.getElementById(`example-${selectedKind}`);
  panel.scrollIntoView({ block: 'center', behavior: 'instant' });
  panel.focus({ preventScroll: true });
});
for (const control of document.querySelectorAll('[data-select]')) control.setAttribute('aria-pressed', String(control.dataset.select === selectedKind));

if (hero && stage) {
  const phone = document.querySelector('.phone-object');
  const envelope = document.querySelector('.envelope-object');
  const message = document.querySelector('.message-slip');
  const documentObject = document.querySelector('.document-object');
  const urlObject = document.querySelector('.url-object');
  const initial = document.querySelector('.hero-initial');
  const resolution = document.querySelector('.hero-resolution');
  let start = 0, travel = 1, previousBeat, previousProgress;
  let visible = true, measured = false;
  const fine = matchMedia('(hover: hover) and (pointer: fine)');
  const responses = [...document.querySelectorAll('.object-response')];
  // Small depth offsets; the complete phone (body and screen) uses the steadiest plane.
  const depths = [.95, .8, .5, .18, .65];
  let cursor = { x: 0, y: 0 }, target = { x: 0, y: 0 }, cursorTime = 0;
  let bounds = { width: 1, height: 1, pinTop: 0 };
  function resetCursor() {
    cursor = { x: 0, y: 0 }; target = { x: 0, y: 0 }; cursorTime = 0;
    for (const response of responses) response.style.transform = 'none';
  }
  const surface = envelope.querySelector('img');
  let lastPosition = 0, lastTime = 0, settleTimer;
  const limitedDevice = navigator.deviceMemory <= 4;
  function clearBlur() { surface.style.filter = 'none'; }

  function showBeat(result) {
    if (previousBeat === result) return;
    previousBeat = result;
    // Cross-fade text layers using opacity only. Both layers stay in flow
    // with visibility:visible to avoid layout thrashing; opacity handles the cross-fade.
    initial.style.opacity = result ? '0' : '1';
    resolution.style.opacity = result ? '1' : '0';
    initial.inert = result;
    resolution.inert = !result;
    initial.setAttribute('aria-hidden', String(result));
    resolution.setAttribute('aria-hidden', String(!result));
  }

  const scheduler = createRafScheduler(now => {
    if (!measured) return;
    // Geometry is cached outside the frame callback. Scroll and local pointer
    // response share this driver; the display has no separate timeline.
    const p = reduce.matches || dataSaving ? 0 : scrollProgress(scrollY, start, start + travel);
    const scrollChanged = p !== previousProgress;
    previousProgress = p;
    const assemble = smoothProgress(scrollProgress(p, .1, .4));
    if (scrollChanged) {
      const pose = heroPose(p);
      for (const [element, position] of [[phone,pose.phone],[envelope,pose.envelope],[message,pose.message],[documentObject,pose.document],[urlObject,pose.url]]) {
        element.style.transform = `translate3d(${position[0]}px,${position[1]}px,0)`;
      }
      initial.style.transform = `translate3d(0,${-3 * smoothProgress(scrollProgress(p,.4,.5))}px,0)`;
      resolution.style.transform = `translate3d(0,${3 * (1-smoothProgress(scrollProgress(p,.5,.62)))}px,0)`;
      showBeat(p >= .5);
    }
    const pointerEnabled = fine.matches && !reduce.matches && !dataSaving;
    if (pointerEnabled) {
      const dt = cursorTime ? Math.min(64, now - cursorTime) : 16.667;
      cursor.x = dampPointer(cursor.x, target.x, dt);
      cursor.y = dampPointer(cursor.y, target.y, dt);
      cursorTime = now;
      const reading = 1 - .55 * smoothProgress(scrollProgress(p, .55, .7));
      responses.forEach((response, index) => {
        const depth = depths[index] * reading;
        response.style.transform = `translate3d(${(cursor.x * 6 * depth).toFixed(3)}px,${(cursor.y * 4 * depth).toFixed(3)}px,0) rotate(${(cursor.x * 1.4 * depth).toFixed(3)}deg)`;
      });
      if (cursor.x !== target.x || cursor.y !== target.y) scheduler.schedule();
      else cursorTime = 0;
    }
    // Only the envelope's rendered surface is filtered. Its label, phone body,
    // display, headings and every control remain sharp. No permanent blur layer.
    if (scrollChanged) {
      const position = assemble * Math.hypot(20.25, 11.25);
      const cap = reduce.matches || dataSaving || limitedDevice ? 0 : bounds.width < 760 ? .4 : .8;
      const blur = lastTime ? decorativeBlur(position - lastPosition, now - lastTime, cap) : 0;
      surface.style.filter = blur > .01 ? `blur(${blur.toFixed(3)}px)` : 'none';
      lastPosition = position; lastTime = now;
      clearTimeout(settleTimer);
      if (blur > .01) settleTimer = setTimeout(clearBlur, 80);
    }
  }, { isVisible: () => visible && !document.hidden });

  function measure() {
    // Read phase only: fonts, breakpoints and stage size can change these values.
    const pinTop = parseFloat(getComputedStyle(stage).top) || 0;
    start = hero.offsetTop - pinTop;
    travel = Math.max(1, hero.offsetHeight - stage.offsetHeight);
    bounds = { width: stage.clientWidth, height: stage.offsetHeight, pinTop };
    measured = true;
    performance.mark('hero-measured');
    scheduler.schedule();
  }
  function configure() {
    scheduler.cancel();
    previousProgress = undefined;
    clearTimeout(settleTimer); clearBlur(); lastTime = 0;
    resetCursor();
    document.documentElement.classList.toggle('motion-ready', !reduce.matches && !dataSaving);
    scheduler.schedule();
    // ResizeObserver measures after layout, rather than forcing a full document
    // layout during module evaluation. Native scrolling is never blocked.
  }
  addEventListener('scroll', scheduler.schedule, { passive: true });
  stage.addEventListener('pointermove', event => {
    if (!fine.matches || reduce.matches || dataSaving || !measured) return;
    // Cached stage bounds follow sticky travel arithmetically; no layout reads.
    const top = bounds.pinTop - Math.max(0, scrollY - start - travel);
    target = { x: Math.max(-1, Math.min(1, event.clientX / bounds.width * 2 - 1)), y: Math.max(-1, Math.min(1, (event.clientY - top) / bounds.height * 2 - 1)) };
    scheduler.schedule();
  }, { passive: true });
  stage.addEventListener('pointerleave', () => { target = { x: 0, y: 0 }; scheduler.schedule(); });
  addEventListener('blur', resetCursor);
  fine.addEventListener('change', resetCursor);
  addEventListener('resize', () => {
    const p = scrollProgress(scrollY, start, start + travel);
    const pinned = p > 0 && p < 1;
    measure();
    // A breakpoint changes pin geometry, not the current narrative pose. Adjust
    // only during an actual viewport resize; ordinary scrolling stays native.
    if (pinned && !reduce.matches && !dataSaving) scrollTo(0, start + p * travel);
  }, { passive: true });
  addEventListener('pageshow', () => observer.observe(stage));
  document.addEventListener('visibilitychange', () => {
    if (document.hidden) { scheduler.cancel(); clearTimeout(settleTimer); clearBlur(); lastTime = 0; resetCursor(); }
    else observer.observe(stage);
  });
  reduce.addEventListener('change', configure);
  const observer = new ResizeObserver(measure);
  observer.observe(stage);
  const visibility = new IntersectionObserver(([entry]) => {
    visible = entry.isIntersecting;
    if (visible) scheduler.schedule();
    else { scheduler.cancel(); clearTimeout(settleTimer); clearBlur(); resetCursor(); }
  });
  visibility.observe(hero);
  // Pin dimensions are CSS-defined; ResizeObserver handles geometry changes.
  configure();
}
