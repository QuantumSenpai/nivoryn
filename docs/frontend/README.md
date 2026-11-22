> **Native app update — 2026-11-23:** local rules, URL inspection, bundled OCR/QR and encrypted saved checks are now connected. See [current implementation and limits](../OFFLINE_IMPLEMENTATION.md). The disconnected-engine statements below describe the earlier frontend milestone. The website was preserved during the native/offline pass.

# Nivoryn frontend

> Current direction (2026-11-21): [product-led first slice](product-led/DIRECTION.md), [validation and review](product-led/VALIDATION.md). The scenic companion described below is archived; the active website is promotional and has no web-scanning flow.

The primary deliverable is native Kotlin + Jetpack Compose in `:app`.

[Verification results and remaining limitations](VERIFICATION.md).
Owner-private [companion preview](https://nivoryn-pause.hollowborage2.chatgpt.site).
The companion site in `web/` is buildless HTML/CSS/JavaScript with no runtime
framework or service dependencies. No model download is needed.

## Run Android

Use the JDK 17, SDK 36 and Gradle wrapper described in `docs/SETUP.md`. On this Mac:

```sh
export JAVA_HOME='/Volumes/ORBIT/Nivoryn/tooling/jdk/jdk-17.0.17+10/Contents/Home'
./gradlew :app:assembleDebug :app:testDebugUnitTest :app:lintDebug --console plain
```

APK: `app/build/outputs/apk/debug/app-debug.apk`.
Install with an authorised phone using `adb install -r <apk>`, then launch
`dev.kyzenn.nivoryn/.MainActivity`. No device is currently attached. Host tests
launch the activity under Robolectric API 35; this does not replace a phone run.
No emulator or model was downloaded.

## Run the companion

Node 22+:

```sh
cd web
npm run dev
```

Open http://127.0.0.1:4173. The server binds only to loopback.
`npm run build` produces `web/dist/` for static hosting. Neither running nor
building the website needs dependency installation.

```sh
npm test
npm ci --ignore-scripts
CHROME_BIN='/Applications/Brave Browser.app/Contents/MacOS/Brave Browser' npm run test:browser
```

`CHROME_BIN` can point to another installed Chromium-family browser. Without it,
Playwright expects an installed test Chromium. Do not download an emulator or
large browser image merely for this task. Browser screenshots/reports go to
`/private/tmp/nivoryn-browser` by default; override `NIVORYN_BROWSER_OUTPUT`.

## Boundaries and state

`domain/AnalysisModels.kt` defines `CheckDraft`, `DemoResult`, `DemoCase`, and the
small `DemoRepository` interface. `data/FrontendDemoRepository.kt` returns an
example result only for an exact, predefined sample ID + text match with no
attachment. Editing any example clears its sample identity. Custom content gets
“Cannot assess” with an explicit disconnected-engine explanation, never a fixed
sample verdict. Future engine results should use a separate implementation and
explicit provenance; do not relax this demo guard.

`integration/ShareInputParser.kt` accepts text shares and reads selected document
metadata. Android's document picker provides file access without broad storage,
camera, microphone or notification permissions. Image and first-page PDF preview
run off the main thread with bounded image dimensions. Supported audio uses
MediaPlayer, never autoplay, and pauses when backgrounded. APKs are not installed
or executed. OCR, QR decoding, transcription and signature checks are unavailable.

Draft/navigation/file references use saved-instance state. Cases and preferences
use app-private SharedPreferences. Cases save demo IDs and case names only. Clear
cases in Settings; Clear draft removes temporary input and releases attachment
access. Android Clear storage resets all app data. No export feature exists.

The website keeps drafts and examples in memory and stores only explicitly
changed display preferences in localStorage. It sets no cookies and contains no
optional trackers, so no consent banner is shown. Hosting metadata requires owner
review if the deployment environment adds logging/authentication.

## Design system

Native: `ui/theme/`, shared controls in `ui/components/Controls.kt`.
Web: semantic CSS custom properties at the start of `web/style.css`.
Paper `#F7F8F3`, ink `#23313A`, action `#263E50`, restrained interactive blue
`#155E91`; semantic warning/danger/positive colours adjust in dark mode.
Buttons use 6dp/px corners, fields 8, dialogs 12. No pill navigation or status
chips. Press feedback 120ms, page entry 220ms, exit 150ms, disclosures 200ms.
System and explicit reduced-motion settings disable displacement. Scenery remains
static. Font and artwork sources/prompts: `ASSETS.md`.

## Coverage and known limits

Connected native routes: welcome, check, examples, review (text, screenshot, QR,
link, document, audio, APK), demo processing/cancellation, result, cases/timeline,
guidance, settings, model availability, privacy, accessibility and notices.
Only authored examples show findings. QR, document and APK detail examples are
labelled and never represented as extracted information about a selected file.

English/Hindi navigation and main flow copy work; source evidence and detailed
technical notices remain English. Tablets use a navigation rail and a bounded
reading width; dedicated two-pane list/detail navigation is not included.
Document preview shows the first page, with zoom controls; it is not a full PDF
reader. Image zoom has buttons and pinch support; no OCR runs. Files over the
Android provider's supported capability may show an honest unavailable state.

Physical-device install/launch, picker providers, playback codecs, TalkBack,
switch access, hardware keyboard focus and frame timing remain unverified.
No production-readiness, detection accuracy or FPS claim is made.
