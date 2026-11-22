# First reviewable slice · implementation and validation

Latest update: [motion polish and current motion validation](MOTION.md). Performance figures below are the first-slice baseline; current web unit suite has 7 passing tests.

## Review

- Website: `http://127.0.0.1:4173/` while the local review server is running. Source: `web/`; generated static build: `web/dist/`.
- Android debug preview 0.1.0: `app/build/outputs/apk/debug/app-debug.apk`. This is a local debug build, not a signed production release. Android 8.0+ (minSdk 26).
- Editable Blender file and license inventory: [ASSETS-LICENSES.md](ASSETS-LICENSES.md).
- Reference breakdown, storyboard, tokens and page/screen maps: [DIRECTION.md](DIRECTION.md).
- Review screenshots: `review/` beside this file. Raw browser runs: `/private/tmp/nivoryn-product-browser/`; native renders: `app/build/frontend-screenshots/`.

## Implemented in this slice

Original Blender product objects; red product assembly with reversible scroll transforms; a readable message-to-evidence handoff and stable result; black editorial explanation; white example section with Link/Message/Screenshot/Document tabs; honest source-build destination; Privacy, Terms, Open source and Accessibility pages. All four examples are pre-rendered into semantic HTML. No-JavaScript mode shows them all. Reduced-motion mode is static and readable. Data-saving mode does not enable the choreography. Only scroll events schedule animation work, and IntersectionObserver/visibility state stops off-screen work. No production web package was added.

Compose now uses the shared Manrope/DM Sans and neutral token system. Scenic imagery is removed from active screens and resource packaging. Four labelled outline-icon destinations are Home, Check, History, Settings. Paste/import and editable input retain the existing Kotlin contracts. A payment-demand example carries the exact phrase into its assessment, View/Hide evidence control, highlighted original input and next-step guidance. Check again and Back return to the existing review route. Delete input clears the current draft. Custom or edited examples still get an explicit unavailable state, never a canned verdict.

Two defects found and fixed during verification: the phone's redundant filter/perspective compositor plane hid live text in the browser; removing it restored text. Fading reading text briefly weakened contrast; the phone/copy now switch at a single narrative handoff at full contrast. A new native test reproduced review scroll reset (65px before vs 0px after); a SaveableStateHolder per route and returning to the existing review route fixed it.

## Checks and results

| Check | Result |
|---|---|
| `npm test --prefix web` | 5 tests pass, no skips. Includes exact example guards and scroll clamp/reversal. |
| `npm run build --prefix web` | Pass. Explicit runtime allowlist excludes scenery, reference footage, Blender files, original TTFs and tests. |
| `npm run test:browser --prefix web` | Pass at 320, 390, 768, 1024, 1440px. No page errors or external runtime requests. |
| Browser interaction | Four tabs change examples, evidence and limitations; keyboard arrows/Home/End; touch activation; focus; navigation/back; real build-link target; unknown-details expansion. |
| Scroll | Initial state restored after reverse scrolling; result state reached; ordinary scrolling retained. |
| Fallbacks | Reduced motion, JavaScript disabled, media requests failed; content and build link remain useful. |
| axe WCAG A/AA/2.1 AA | 0 violations on the tested homepage states at all five widths. Not a WCAG certification or manual screen-reader test. |
| Native Gradle | `:app:testDebugUnitTest :app:assembleDebug :app:lintDebug --offline` passes. 19 tests, 0 failures, 0 skipped. |
| Native behavioral coverage | Example provenance, edited/custom input rejection, four roots, exact input retention, evidence toggle, check-again, restored review scroll position, saved cases CRUD, recreation, text shares, dialog editing. |
| Native renders | Welcome, home, matching input/result, dark theme, Hindi, tablet; 1.6× font-scale primary-action test. Host Robolectric API 35, not hardware. |
| Lint | No errors. Existing/remaining warnings include dependency-version suggestions, backup/data-extraction configuration, URI KTX suggestions and unused resources. No suppressions were added. |
| Source hygiene | `git diff --check` passes. Existing mixed uncommitted work was preserved; no blanket commit, push or deployment. |
| Build destination | Git remote and `origin/main:docs/SETUP.md` confirm the repository/setup target. New source changes are local and have not been published. |

The available session has no Chrome DevTools MCP, so browser verification uses the project's existing Playwright installation with isolated headless Brave contexts. This does not inspect the user's normal browser profile.

## Performance, final source

Headless Brave on the host Mac; 4× CPU slowdown, 1.6Mbit/s download, 750Kbit/s upload, 150ms latency; cache disabled. Three cold contexts each at 390×900 and 1440×900. Raw data: [performance.json](performance.json).

- Mobile LCP: 996 / 856 / 876ms; CLS: 0 on all three.
- Desktop LCP: 888 / 888 / 904ms; CLS: 0.0016 on all three.
- Maximum observed tab-interaction Event Timing duration: 48ms across the six runs. This is a lab proxy, **not field INP**.
- Idle rAF p95: 16.7–16.8ms, approximately 78 samples/run. This does **not** establish scroll smoothness or 60Hz performance on a physical phone.
- Resource transfer: approximately 140.6KB plus the document response as reported by Resource Timing. Four WebP objects total 25,862 bytes; the two WOFF2 fonts total 83,696 bytes.

LCP and CLS met the requested targets in this specific lab setup. Hardware 60Hz interaction, field INP and production CDN behavior remain unmeasured. No Android device was attached (`adb devices -l` returned an empty list). No emulator or model was downloaded.

## Visual review and intentional exceptions

Inspected representative frames from the actual reference, then browser fold and scroll contact sheets at desktop/tablet/mobile, no-JS/reduced-motion views and native renders. Main headlines are two lines at the three inspector widths. Source phrases are retained as specimen body copy, not shortened into marketing headlines. The result is an original Nivoryn composition; it is not claimed to be objectively better than Superlist.

The design-skill inspector reports 0 failures with four documented allowances: Example provenance labels (explicitly required by the brief), real tab/disclosure separators, miniature duplicated phone UI, and the deliberate seven-element motion budget. Adding extra moving reading text simply to meet a generic eight-element quota would contradict the brief. Remaining warnings are retained with reasons: a real phone input needs an outline; miniature interface text naturally adds type sizes; secondary body text meets AA (about 6.5:1) rather than the inspector's 7:1 preference; no footage is needed for the chosen light render-layer path. Its FAQ balance/title warnings are false positives: the visible “Common questions.” heading occupies the left column, with functional questions on the right. All required content remains in document flow.

Qualitative self-review: the red/graphite hierarchy reads clearly, the same payment phrase provides the signature, and the monochrome native UI shares typography without cinematic pacing. The white example is the strongest proof of the product. Product-material edge contrast and the amount of first-screen object overlap are appropriate direction-review topics. No extra effects were added to inflate the visual score.

## Next slice, after direction review

Expand the existing saved-case surface into the requested searchable/filterable/exportable History; complete contextual import/error states (unsupported, oversized, import failure, cancelled, engine failure, missing model and offline limitation); finish model/connectivity/permissions settings and richer per-kind input previews. The current History destination retains the existing local demo-case behavior and is not represented as a completed new history system. Current UI accepts custom drafts and selected-file previews, but **custom checking, OCR, model inference, issuer verification, audio/deepfake and APK/malware checks are not implemented**.

The backend is not complete. Typed `CheckDraft`, `DemoRepository`, `DemoSample` and `DemoResult` remain the replaceable native boundary. Web examples have JSDoc types and never classify user input. Continue with this direction only after reviewing the first slice.
