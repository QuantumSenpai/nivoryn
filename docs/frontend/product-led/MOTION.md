# Motion polish — final

The hero uses **seventh-order scroll easing** (`p⁴(35−84p+70p²−20p³)`) with zero velocity and acceleration at endpoints. Position is a direct function of scroll; no delayed catch-up, no independent timeline, no scroll interception. Forward and reverse scrolling restore identical poses.

**Phone UI transition** (home → result) uses a **critically-damped spring** (`1−e⁻⁶ᵖ(1+6p)`, clamped to exactly 1 at p=1) for an organic ~0.35 s response.

## Three clean, non-overlapping scroll phases

| Phase | Scroll range | Motion |
|-------|--------------|--------|
| **Assemble** | 0.10 → 0.40 | Phone settles with cinematic tilt (6°→2°) + subtle scale; envelope slides in horizontally with rotation (-14°→-8°); subtle cursor parallax (4px/3px) |
| **Reveal** | 0.40 → 0.65 | Message slip lifts vertically (32px) with slight rotation; phone UI cross-fades (home→result) with spring |
| **Resolve** | 0.65 → 0.85 | Resolution copy rises; "Explore evidence" link activates |

**No overlap** — each phase completes before the next begins. Document and URL objects are static. Message slip fades during reveal. Envelope fades slightly during assemble.

## Live phone screen transition

- Two absolute-positioned layers (`.phone-screen-home`, `.phone-screen-result`) inside `.phone-screen`
- CSS cross-fade: `opacity` transition (220 ms, cubic-bezier 0.23, 1, 0.32, 1)
- JS only toggles `data-screen="home|result"` at p=0.5 — zero innerHTML manipulation
- `pointer-events` auto-swaps so only visible layer is interactive

## Micro-interactions everywhere

| Element | Interaction | Duration | Easing |
|---------|-------------|----------|--------|
| **Tabs** | Specimen cross-fade (transform + opacity) | 180 ms | cubic-bezier(0.23, 1, 0.32, 1) |
| **Tab hover** | Opacity 0.7 | 120 ms | ease |
| **Tab press** | Scale 0.98 | 60 ms | ease |
| **Example cards** | Lift -4px + shadow | 180 ms | cubic-bezier(0.23, 1, 0.32, 1) |
| **Buttons/links** | Press: translateY(1px) scale(0.995) | 80 ms | cubic-bezier(0.23, 1, 0.32, 1) |
| **Evidence details** | Slide-fade in | 180 ms | cubic-bezier(0.23, 1, 0.32, 1) |
| **Phone screen** | Cross-fade home↔result | 220 ms | cubic-bezier(0.23, 1, 0.32, 1) |

## Performance architecture

| Technique | Implementation |
|-----------|----------------|
| **Compositor promotion** | `will-change: transform, opacity` on 5 animated elements — set upfront |
| **GPU transforms** | All motion uses `translate3d()` — guaranteed compositor path |
| **Batched DOM writes** | Transforms + opacity collected and applied in single `queueMicrotask` |
| **Single RAF/frame** | Robust scheduler: one `requestAnimationFrame` per tick; cancels on visibility change, reduced-motion, hero off-screen |
| **Cursor parallax** | Smoothed follow (`cursorX += (target - cursorX) * 0.08`) — runs in RAF, zero scroll work |
| **Phone screen swap** | CSS-only cross-fade via `data-screen` toggle — no layout, no paint |
| **Sub-pixel precision** | Pixel/percentage values directly; no layout-triggering properties |
| **Passive scroll listener** | `{passive: true}` — zero main-thread work on scroll |

## Validation

- **8 unit tests pass** (choreography bounds, reversibility, spring behavior, scroll clamping, evidence provenance)
- **Browser checks at 320, 390, 768, 1024, 1440 px** — all pass: no overflow, reverse scroll restores pose, 4 tabs work with keyboard/touch, 0 axe AA violations, no external requests
- **Reduced-motion**: static hero with result visible, no transforms/filters
- **No-JS**: all 4 examples visible, build link works
- **Failed-image**: hero heading and CTA remain visible
- **Zero console errors** during scroll, tab interaction, resize, cursor movement

Reproduce: `node web/tests/motion-browser.cjs`, `npm run test:browser --prefix web`, `npm test --prefix web`, `npm run build --prefix web`.