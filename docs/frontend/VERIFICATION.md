# Frontend verification — 2026-11-21

## Native Android

Final command: `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:lintDebug --console plain`
with the existing ORBIT JDK 17 / SDK 36 and normal local Gradle cache described
in SETUP.md. **Passed.** Debug APK generated, lint has no errors. Remaining lint
warnings concern pinned dependency upgrades, optional KTX suggestions, old unused
foundation strings and explicit backup-rule advice. No lint suppressions/baseline
were added. Android backup remains disabled; manufacturer transfer behaviour
requires device review.

17 tests passed:

- Five domain tests: custom input cannot receive an example verdict; edited
  examples lose result identity; all eight examples have honest coverage and
  quoted evidence; empty-input validation; unique ordered case items.
- Four connected Compose flows: sample review/result/back, edited sample custom
  state, original draft preserved while exploring examples, case create/rename/
  named-delete confirmation.
- Three state/integration tests: saved-instance recreation of review + mixed-script
  draft, local case persistence/deletion, styled text shares bounded to 12,000
  characters without inheriting a sample verdict.
- One dialog edit regression test.
- One large-text flow at Compose font scale 1.6: primary actions reachable and
  mixed-script text editable.
- Three native rendering tests: welcome/home/result, tablet home, dark/Hindi home.

Tests launch MainActivity or Compose content in Robolectric API 35 on macOS.
Native graphics renders were opened and inspected. Generated screenshots are in
`app/build/frontend-screenshots/`: welcome, home, result, tablet-home, dark-home,
hindi-home. The tablet home uses a separate two-column composition and rail.

During diagnosis, an otherwise identical minimal dialog repeatedly measured
under Robolectric's synthetic `w390dp-h844dp-port` display configuration. Dialog
interaction tests use Robolectric's default display; size-specific rendering tests
remain separate. Native graphics is required for the bundled Android font-fallback
API. This is not evidence of physical-device behaviour in either direction.

`adb devices -l` succeeded with an empty device list. **No physical-phone install,
launch, TalkBack, keyboard/switch-access, picker-provider, PDF codec or audio-codec
run was verified.** No emulator or model was downloaded. Frame-time, peak whole-app
memory and production readiness were not measured or claimed.

## Companion website

- `npm test`: three deterministic demo/state tests passed.
- `npm run build`: static output generated successfully.
- `npm run test:browser` with the installed Brave browser: passed at **320, 390,
  768, 1024 and 1440px**. Checked no horizontal overflow or broken images, sample
  review/result, edited custom input, story add/remove, dialog keyboard containment
  and focus restoration, display preference persistence, mobile menu Escape,
  and all privacy/accessibility/notices routes.
- No browser errors and no third-party runtime requests in those flows. Fonts and
  scenery load from the site origin. No application cookies or optional trackers
  are implemented. Only explicitly changed display preferences use localStorage.
- axe WCAG A/AA/2.1 AA audit at 390px: zero violations on the landing page and all
  three policy pages in both light and dark themes (eight page/theme audits).
  This does not certify accessibility or replace manual assistive-technology use.
- Responsive screenshots and machine-readable reports are under
  `/private/tmp/nivoryn-browser/`. Native screenshots and desktop/mobile website
  layouts were opened and inspected; tablet composition and portrait framing were
  improved based on those renders.
- The optional design-skill inspector also ran. Its stylistic rules objected to
  the requested thin dividers, restrained image count and static scenery; these
  were retained to honour the user's explicit content/design/performance brief.
  The initially missing mobile image was fixed before the five-width passing run.
- Companion deployed owner-private through Sites:
  https://nivoryn-pause.hollowborage2.chatgpt.site . Platform access/session behaviour
  is separate from the site code and is noted in the privacy draft.

## Boundaries and limits

No real detection, inference, cloud APIs, accounts, payments, analytics or backend
were implemented. OCR, QR decoding, transcription, booking/signature verification
and APK risk detection remain visibly unavailable. Selected-file sample text is
never represented as extraction from that file. No arbitrary input gets an authored
sample verdict. English/Hindi main-flow presentation works; detailed technical
notices and source evidence remain English. Scenery is static, not continuously
animated. PDF preview is first-page only. Cases save authored demo items only.

No repository changes were committed or pushed to the user's GitHub repository;
pre-existing uncommitted work was preserved. The Sites helper used a separate
staging checkout for its own source publication.
