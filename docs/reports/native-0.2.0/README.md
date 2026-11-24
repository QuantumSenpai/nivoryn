# Nivoryn 0.2.0 — implemented changes and acceptance evidence

24 November 2026. Started from clean `627465f` / 0.1.3, versionCode 4. Current version is **0.2.0 / versionCode 5**, package `dev.kyzenn.nivoryn`. Existing detection, extraction, reasoning and encrypted-history services are retained. No code comments, signing secrets, model weights or private inputs were added.

**PS3 decision: Not ready — specific blockers remain.** Real Llama/ExecuTorch and offline workflows are demonstrated. Live packet-level intrusion detection, Android malware-behaviour monitoring and physical target validation are incomplete. Aggregate usage observation and static APK inspection are presented with their actual limits.

## Delivered application

Home now opens directly after the platform splash with Scan/Paste, real readiness and labelled feature groups. Check retains the same feature identities in a compact selector and leads through capture/import → editable review → analysis → evidence/result. Activity holds saved checks/cases. Settings groups appearance, accessibility, language, resources, network, privacy and About. Engine details have a dedicated resource destination.

Shared spacing, typography, shapes, tonal surfaces, action rows and brief motion retain the website’s red/near-black/paper identity. Dark surfaces have distinct levels and theme-aware warning text. Four stable feature tiles coordinate position/size between Home and Check; large text uses two columns. This uses stable Compose APIs, with no experimental shared-element migration, embedded website or 3D engine. Icons respond once to deliberate navigation; reduced motion removes movement. Incoming shares route promptly to Check. Draft/source metadata is cleared appropriately when changing input mode so an old QR cannot contaminate a new text check.

The exact Inspora animation was inaccessible: its description was available, media returned 429 and the original social link returned 403. No exact animation study is claimed. The actual Tempo listing video was saved on ORBIT and sampled. See [reference observations](../../NATIVE_MOTION_REFERENCE.md), [API decisions](../../API_COMPATIBILITY.md) and [initial inventory/screen map](IMPLEMENTATION.md).

| Current capability | Actual state | Boundary |
|---|---|---|
| Text/message and URL checks | Working | Local rules; incomplete coverage, no live ownership/reputation verification |
| Camera capture and selected image import | Working on emulator | Real Android camera; review precedes analysis; OEM behaviour pending |
| Latin/Devanagari OCR, QR, first-page PDF | Working within bounds | 20 MiB, bounded pixels/pages; QR never automatically opens |
| Llama-family reasoning with ExecuTorch 1.2.0 | Working after setup | Fresh install needs the pinned 1.14 GB pair and a successful real inference test |
| Generated advisory reliability / spam proxy | Experimental | Validated outputs or explicit curated fallback; not proof of fraud/safety |
| Encrypted history, exact snapshots and export | Working | Deliberate plaintext export leaves app-private storage |
| APK inspection | Working | Static indicators only, no execution or general behaviour analysis |
| Classic IPv4 PCAP import | Working | Bounded replay and experimental thresholds; separate from live observation |
| Live device byte totals | Working on emulator | Explicit foreground session; 60 in-memory samples; no payload/IP/DNS storage |
| Live intrusion/flow classification or blocking | Unavailable | No VPN/packet-forwarding engine; replay is never labelled live |
| Cross-app malware behaviour / TLS plaintext | Unavailable | No CA, decryption or complete-protection claim |
| Signed external rule feeds | Needs setup | Publisher key/feed provisioning; bundled local rules work |
| Audio / issuer authentication | Unavailable | No enabled action claims these are usable |

## Local AI setup

Open **Home → Set up local AI**. Review device/storage information and the bundled Llama licence, confirm the licence terms, then choose **Download and verify local AI**. Wi-Fi/unmetered transfer is the default; mobile data is optional and roaming disabled. Android shows real byte progress and manages interrupted-transfer retry/resume. Manual pause is explicitly unsupported. Cancel removes downloaded copies; advanced compatible-file import, retry/test, rollback and confirmed deletion remain available.

The exact upstream-linked Hugging Face revision, file sizes and SHA-256 pins are verified before private pair activation. A real synthetic explanation must then pass validation before “Ready for offline explanations” appears. The actual guided transfer completed, recovered after process termination and passed inference in **5,232 ms**. [Proof](guided-setup-proof.json), [download progress](model-download-progress.png), [ready screen](after-model-ready.png), [friendly setup guide](../../MODEL_SETUP.md).

Internet/network-state permission now supports explicit model downloads. The old UID-wide no-Internet guarantee no longer applies. No cloud inference/content-analysis endpoint was added. The subsequent required engine suite passed with airplane mode enabled and both Wi-Fi/mobile data disabled. Each result still names Llama, curated fallback or unavailable explanation honestly.

## Screen and feature acceptance matrix

Host: 99 tests, zero failures/errors/skips across 34 suites. Android: **20 tests passed**, comprising 2 connected camera/usage tests and 18 real-model-required tests offline. Builds: debug, instrumentation, minified preview and unsigned release succeeded. Two Linux observer tests and the packaging refusal test passed. Website: 17 unit tests plus production browser/download/SEO checks passed.

The current device is an **Android 17/API 37 Arm64 emulator with 16 KB pages, 4 GB configured RAM, two vCPUs and software graphics**, 1080×2400. No physical phone or Arm Linux target is attached. These are emulator results, not Oppo acceptance, battery data or production accuracy.

| Action/state | Result and evidence | Coverage limit |
|---|---|---|
| Home/Check/Activity/Settings; labelled tools; Back | Verified in host UI and actual native flows/screens | Not every OEM navigation behaviour |
| Cold/warm entry, share routing, authored-example safeguards | Verified in launch/share/flow tests | External app-specific share variants pending |
| Feature selector, text editing, mode switch, clear draft | Verified by host UI/restoration and stale-QR regression | Physical keyboard/switch access pending |
| Real camera permission deny → retry | Denial/import alternative verified; second attempt did not reprompt | Camera revocation during an active OEM capture unverified |
| Real camera → shutter → Done → review | Verified with Android Camera2; local OCR honestly reported unreadable scene | Representative physical photographs pending |
| Camera URI grants / local QR decode / cleanup | Verified by NativeSetupDeviceTest; explicit grants and real JPEG QR | Camera-app cancellation/recreation across OEMs pending |
| Gallery/file imports and editable extraction | Real content-provider tests passed for images/PDF/QR | Full OEM picker interaction matrix pending |
| Orientation, poor image, corrupt/oversized inputs, PDF bounds | Verified in extraction/boundary/device tests | Malicious blocking providers cannot always be forcibly interrupted |
| Local analysis → evidence → remediation | Real custom input and Llama tested offline | Heuristic warnings are not ground truth |
| Save/reopen/resave/recreate/delete/export | Encrypted snapshots and SAF/export lifecycle regressions passed | Destination providers control plaintext exported copies |
| Light/Dark/System, Hindi/English, 1.6× text | Actual switching, rotation and screenshots verified | Some detailed notices/setup remain English |
| Reduced motion / navigation during analysis / cancellation | Host state/request-gate tests and native runner cancellation passed | Frame-perfect rapid-tap/physical interaction acceptance pending |
| Missing model → real guided download → Ready | Verified; process-stop recovery and native inference proof | Wi-Fi loss mid-transfer, DownloadManager cancel/retry and actual disk exhaustion not fully device-tested |
| Corrupt/incompatible pair, low-space guards, partial import | Host lifecycle/hash/boundary tests passed | Host guards are not a real low-storage endurance test |
| Delete/rollback/retest model controls | Implemented; deletion and guided re-setup exercised | Every previous-version rollback combination not re-run on final device |
| Network opt-in, notification permission, start/stop | Actual UI + foreground notification/Stop test passed | Denied notification settings round trip pending |
| Network connectivity / IPv6 / DNS | HTTPS preserved; same network handle, IPv6 address and DNS server present | Not an IPv6 reachability or DNS inspection test; no mobile/Wi-Fi handover validation |
| Process interruption of observation | Non-sticky session architecture; no boot restart | OEM service-kill/low-memory policy validation pending |
| APK/capture tools, corrupt files | Existing Android provider suites passed | Static APK/replay only |
| TalkBack/focus/contrast | Labels, roles, headings and anchored controls implemented; large text inspected | Manual TalkBack/focus and formal native contrast audit still unverified |
| Exact public minified APK offline flow | Verified: real Llama, evidence, save/reopen/delete in airplane mode | [Exact installed SHA and results](distribution-ui-smoke.json) |

Raw evidence: [host summary](host-summary.json), [offline runner](offline-instrumentation.txt), [device summary](device-summary.json), [camera URI](camera-uri-evidence.json), [usage service](live-usage-evidence.json), [manual UI](manual-ui-acceptance.json), [APK gate](distribution-gate.json). No test/lint rule was globally disabled. Lint retains **0 errors / 6 existing warnings**: three upgrade suggestions, ChromeOS ABI guidance, conservative usable-space handling and base monochrome-icon guidance (API 33 variant supplies it). [Raw lint](lint-results-debug.txt).

## Before/after and demonstration

Before images are the supplied phone screenshots; after images are emulator captures at a different viewport. They show hierarchy/state changes, not a controlled same-device visual benchmark.

| Screen | Before | After |
|---|---|---|
| Home | [Phone](before-phone-home.png) | [Light](after-home-light.png) · [Dark](after-home-dark.png) |
| Check | [Phone](before-phone-check.png) | [Light](after-check-light.png) · [Dark](after-check-dark.png) · [Large text](after-check-large-text.png) |
| Settings | [Phone](before-phone-settings.png) | [Light](after-settings-light.png) · [Dark](after-settings-dark.png) |
| Real results/setup | — | [Offline result](after-result-offline.png) · [Model ready](after-model-ready.png) · [Camera review](scan-review.png) |

Actual new demo: `/Volumes/ORBIT/Nivoryn/captures/nivoryn-0.2.0-offline-demo.mp4`, **30.42 seconds**, synthetic input, airplane mode and real Llama required. The test shows evidence, generated mode/prose, encrypted save/reopen and deletion. A result frame at 14 seconds was visually inspected. [Recording digest/metadata](offline-demo.json). No private message or executable malware was used.

## Measured performance

Single observations on the stated emulator, build tools idle; OS file cache may be warm. Debug UI profiling includes automation and memory-polling overhead and does not establish phone smoothness.

| Measurement | Observed |
|---|---|
| Debug process-cold launch | Android TotalTime 869 ms; command wall 902 ms |
| Exact minified preview process-cold launch | Android TotalTime 730 ms; WaitTime 732 ms |
| Already-visible activity delivery | TotalTime 0 ms / WaitTime 4 ms; this is not a resume-from-background benchmark |
| First Check accessibility observation | 4,110 ms including ADB input/UIAutomator dump overhead; not touch-to-frame latency |
| Native Llama load / generation / first token | 871 / 1,720 / 1,065 ms |
| Pipeline first / warm explanation | 2,955 / 1,653 ms |
| Guided setup inference | 5,232 ms including readiness work |
| Synthetic OCR | Rotated Latin 23 ms; Devanagari 20 ms; page-one PDF 189 ms |
| Camera-URI JPEG QR | 392 ms, fixture/provider/decode path |
| Sampled peak UI-flow RSS / PSS | 1,473,552 / 1,369,201 KiB |
| Last populated frame sample | 118 frames, 29 janky; p50 25 ms, p95 53 ms, p99 200 ms |

The software-rendered debug flow exceeds a 16.7 ms frame budget in these observations. A smooth 60 Hz physical-device experience is **not verified**. Heavy extraction, model verification/loading and generation remain off the UI thread; the app releases its idle model when hidden. Physical release profiling remains required before performance acceptance. [Debug startup](emulator-startup.json), [exact preview startup](distribution-startup.json), [flow samples](emulator-flow-profile.json), [native model](llama-native-smoke.json), [pipeline](llama-pipeline-smoke.json), [OCR](extraction-metrics.json).

## Version, install and canonical deployment

Public **non-debuggable, minified Arm64 preview**, Android 8.0+: [Download 0.2.0](https://nivoryn.vercel.app/downloads/nivoryn-0.2.0-arm64-d5d9d17f66f6.apk), **25,572,542 bytes**. SHA-256: `d5d9d17f66f6d18a4d7855b6941a3cc3819367c1ee5cb1f797d1dd503d6356c8`. Existing preview certificate retained; it is development signing, not an owner-managed production release. The separate release variant is unsigned. ApplicationId and upgrade identity are unchanged. The final emulator update preserved installed models; exact distribution runtime evidence is recorded by the final smoke gate.

Canonical production is **https://nivoryn.vercel.app**, project `nivoryn`, deployment `dpl_4XSbW4zm2D7Y9pAM9wgwVnpUq6Jj`. Both confirmed duplicates, `nivoryn-vercel.vercel.app` and `nivoryn-public.vercel.app`, publicly return **308** to the canonical root. Their projects/history were preserved. Normal preview deployments are not duplicate sites.

The prebuilt Build Output API package is the single production target. Root/web Git auto-deployment is disabled to prevent repository-root pushes publishing a 404. Anonymous browser checks verified five canonical pages, sitemap/robots, social/favicon assets, `/index.html` redirect, APK MIME/disposition/bytes and exact hash. Main pages expose HSTS, nosniff, frame-deny and referrer-policy headers. Five widths passed keyboard, scrolling, reduced-motion/no-JS and automated accessibility checks. [Deployment inventory](deployment-inventory.json), [public headers](public-headers.json), [web results](web-verification.json), [repeatable deployment](../../DEPLOYMENT.md).

## PS3 submission ledger

Checked against the [official Bharat AI-SoC Challenge PS3 brief](https://armbharatchallenge.com/): on-device threat/phishing/scam checks, network anomalies/intrusions, malware behaviour, Llama-family advisory/reasoning through ExecuTorch, offline use and plain remediation; source, app, evaluation, technical report and demonstration evidence.

| Required item | Status | Evidence / remaining work |
|---|---|---|
| Source code | Verified | Native source, pinned dependencies, tests and reproducible build scripts |
| Installable app | Verified on emulator | Versioned preview, signature/alignment/JNI checks and actual update; physical OEM installation pending |
| Llama + ExecuTorch reasoning | Verified on emulator | Guided setup, real generated output, offline model-required suite and video |
| Offline text/URL/OCR/QR | Verified within scope | Real local extraction/analysis after setup; bounds and uncertainty disclosed |
| Network replay and aggregate live usage | Verified within scope | PCAP tests, foreground counters and connectivity-preserving observation |
| Live network intrusion/anomaly detection | Missing | No live destination/packet classifier or compatible forwarding engine; real Wi-Fi/mobile/IPv4/IPv6/DNS/VPN validation unavailable |
| Malware-behaviour monitoring | Missing | Static APK indicators and separate Linux observations do not fulfil Android behaviour monitoring |
| Plain-language remediation | Verified within tested cases | Evidence-linked approved actions and validated generated summary; fallback explicit |
| Evaluation | Verified for limited datasets | UCI held-out spam proxy and synthetic regression/guardrail fixtures; contemporary scam/network/malware effectiveness evaluation missing |
| Technical report | Verified | This report plus engine/provenance/evaluation documents; remaining gaps explicit |
| Demonstration video | Verified emulator demonstration | New 0.2.0 real offline flow; physical/complete PS3 demo missing |
| Physical Arm Android / Arm Linux acceptance | Blocked | No attached physical phone or Arm Linux target; accessibility, battery, network handover and release pacing pending |
| Submission account / eligibility / shortlist | Blocked | Account/eligibility not verified. Published Phase 1 deadline was 23 November 2026; Phase 2 is listed for 5 February 2027. No submission action was performed |

Historical classifier evaluation remains 4,128 train / 1,031 held-out rows, TP 113 / FP 0 / TN 903 / FN 15. This is an attributed historical English spam proxy, **not contemporary scam or multilingual accuracy**. Llama guardrails do not prove semantic entailment or eliminate false reassurance.

Reliable live packet monitoring cannot be claimed from the attached emulator and present code: there is no established packet-forwarding integration to validate across physical mobile/Wi-Fi networks. A TUN-only service would risk connectivity and was not added. The shipped usage observer is a real, bounded and explicitly limited capability. Full PS3 submission remains **not ready** until live intrusion coverage, malware-behaviour coverage, representative evaluation and target-hardware evidence are completed or formally accepted as limitations by the organisers.
