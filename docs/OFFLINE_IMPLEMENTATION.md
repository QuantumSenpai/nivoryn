# Offline implementation — 22–23 November 2026

**0.2.0 update:** this document retains earlier setup/engine evidence. The [current acceptance report](reports/native-0.2.0/README.md) supersedes old permission, camera, model-import-only and UI statements. Explicit verified downloads now use Internet/network-state permission; analysis remains local. Camera and foreground aggregate usage are optional.


The selected architecture is **fully offline and account-free**. The user's NVIDIA NIM key was used with an earlier coding agent, not as an application backend. No key is needed by this implementation. Login, password recovery and a server were intentionally not introduced.

## What the previous checkout contained

The last three commits are `56a93f8`, `edaac7c` and `98ebc5b`, all dated 12 November 2026. They describe the README graphics and scaffold. Most website and Android frontend work was uncommitted. A source snapshot and pre-existing tracked diff were retained in `/private/tmp/nivoryn-offline-pass` before this pass. Nothing was reset, staged, committed or pushed.

The checkout had connected demo routes, files/audio previews, example-only cases and theme work. Detection, orchestration and reasoning files were placeholders. The app did not compile: navigation returned the wrong animation type, a closing brace was missing, several imports were invalid and the evidence/coverage components used unsupported APIs. Coverage also incorrectly presented rule and URL checks as completed. These issues were repaired without reverting the working flows. The website was not edited during this pass.

## Available behavior

| Input / feature | Behavior and boundary |
| --- | --- |
| Message | Bounded, deterministic English/Hindi rules. Normalises full-width characters and selected invisible separators while preserving original evidence quotes. Handles explicit secret requests, advance-fee language, digital-arrest threats and selected urgency/remote-access patterns. |
| Link | Parses the actual URI authority. Inspects user-info deception, numeric destinations, internationalised hosts, HTTP and excessive subdomains. Rejects malformed/unsupported schemes and invalid ports. No DNS, HTTP fetch, redirects or reputation lookup. |
| QR | ZXing decodes actual image pixels. The payload is editable and inspected as text / URL / UPI structure. No destination or payment is launched. |
| Image / PDF | Bundled ML Kit text recognition. App language selects Latin or Devanagari recognition. PDF processing covers the first page only. Review and correct extracted text before running a check. |
| Audio / APK | Existing audio playback and file review remain. No transcription, deepfake detection, APK execution, installation or malware scan. These inputs return an unavailable assessment. |
| Results | Local results and authored examples have separate provenance. Only checks that actually ran are marked completed. Missing checks and unverified identities remain visible. A no-warning result explicitly does not certify safety. |
| Saved checks | Only after pressing Save. Stores original text, kind, random ID and timestamp. AES-256-GCM with a non-exportable Android Keystore key; atomic writes; global in-process serialisation. At most 50 checks and a 1 MiB encrypted-store budget. Expiration runs on history reads after 30 days. Read/delete in History; clear all in Settings. |
| Demo cases | Existing example-only timelines and CRUD remain separate from real saved checks. No inferred relationship or fabricated Scam Story is added to private content. |
| Launch / navigation | Original Nivoryn mark enters and exits in a finite 580 ms sequence; no resource-loading gate. Reduced motion skips that delay. Page transitions use 220/150 ms motion, press feedback 120 ms. Bottom navigation enters gently. No infinite animated scanner or simulated telemetry. |
| Dashboard | Compact local readiness panel, four labelled content controls, editable input and clear review action. Paper/ink/red palette, Manrope and DM Sans follow the website. Scales to tablet and large text. |

## Security model

Untrusted boundaries are shared text, document-provider metadata/bytes, URI payloads and encrypted-store bytes. The app never interprets content as code, shell commands, model instructions, SQL or an automatic Android intent. Rules are heuristics rather than an adversarially complete classifier: different wording, language or obfuscation can evade them, and legitimate quoted material can produce warnings. There is no accuracy claim.

The final merged manifest must have **no INTERNET or ACCESS_NETWORK_STATE permission**, including those from OCR dependencies. Cleartext traffic and backups/device transfer are disabled. Document access uses the Android picker rather than broad storage permission. Metadata is not trusted for extraction limits: actual reads stop at 20 MB, images are downsampled with bounded dimensions, and PDF extraction is first-page only. PDF extraction temporarily copies the selected PDF into app-private cache and deletes it on normal completion; a process crash can leave that cache copy until it is cleared. File-provider sync behavior belongs to the provider app.

Saved history is in Android's no-backup directory. Authentication/decryption failure never silently replaces history. A compromised/rooted device, malicious keyboard, compromised OS, screen observation, a debug build and physical access outside the device's protections remain relevant threats. Neither deletion nor retention promises forensic secure erasure. Draft/navigation saved-instance state is not encrypted history and may persist with the Android task; clear the draft to discard it.

No server auth/CORS/session mechanisms are applicable because there is no server or account. A password screen would not secure the local engine. No private content is transmitted to NIM or another model service.

## Storage on this Mac

Source stays at `/Users/kyzenn/Projects/nivoryn`. The SDK and JDK already live under `/Volumes/ORBIT/Nivoryn`. Large Gradle caches/distribution and app build outputs now live in an APFS sparse bundle on ORBIT, avoiding the documented AppleDouble/class-file issue on its host filesystem:

| Existing path | ORBIT-backed target |
| --- | --- |
| `~/.gradle/caches` | `/Volumes/NivorynBuildCache/gradle-caches` |
| `~/.gradle/wrapper` | `/Volumes/NivorynBuildCache/gradle-wrapper` |
| `app/build` | `/Volumes/NivorynBuildCache/app-build` |
| `~/.android/avd/test.avd` | `/Volumes/NivorynBuildCache/android-test.avd` |

The image file is `/Volumes/ORBIT/Nivoryn/tooling/NivorynBuildCache.sparsebundle`. Its 16 GB value is a growth limit, not a 16 GB preallocation. No partition was reformatted. Existing paths are symlinks, so reports and APK paths stay the same. Mount ORBIT before building. Do not unplug it while Gradle is running. No Llama weights, new emulator or training dataset were downloaded during this pass. The previous agent had already installed an Android 14 ARM64 emulator and created a roughly 4 GB test AVD on the Mac; that AVD was preserved and moved to ORBIT. The helper gives Gradle/Kotlin the canonical SSD build path to avoid stale incremental indexes through symlink aliases.

```sh
zsh scripts/android.sh
# Optimized unsigned release, for signing with the owner's release key:
zsh scripts/android.sh :app:assembleRelease
```

The helper mounts the cache image when available and selects this Mac's JDK. Other machines can use their normal JDK/SDK and `./gradlew`. Gradle dependency versions are locked in `app/gradle.lockfile`. Never commit signing keys or API credentials. Debug APKs are for development; the unsigned release is not an installable signed production release.

To view the app in this Mac's existing emulator, run `zsh scripts/emulator.sh` in another terminal (add `-no-window` for headless testing). It handles the previous installation's nested emulator/system-image directories without changing them. Once it boots:

```sh
/Volumes/ORBIT/Nivoryn/android-sdk/platform-tools/adb -s emulator-5554 install -r app/build/outputs/apk/debug/app-debug.apk
/Volumes/ORBIT/Nivoryn/android-sdk/platform-tools/adb -s emulator-5554 shell am start -W -n dev.kyzenn.nivoryn/.MainActivity
```

The emulator helper uses a disposable read-only AVD session; it does not save changes to the original virtual device. A later restart requires reinstalling the APK.

## Remaining work and verification limits

Llama/ExecuTorch, a trained scam classifier, live reputation/booking/signature verification, custom-case relationship inference, export, audio transcription and malware analysis are not implemented. The available local engine is intentionally explicit about this. OCR/QR functionality does not make those checks available. There is no production-readiness or unhackability claim.

Host tests cover real rule/URL results, original quotes, obfuscation, prompt-injection content, malformed inputs, QR pixel decoding, encryption integrity, retention, history write protection, manifest permissions, sample isolation, navigation/restoration, large type and rendering. A Robolectric render is useful visual evidence, not proof of Android GPU pacing. Three additional instrumentation tests passed in the existing Android 14 ARM64 emulator at 390×844 in airplane mode: real bundled Latin OCR feeding the local evidence engine, non-exportable Android Keystore encryption with tamper rejection, and the app check/evidence UI flow. The emulator used SwiftShader; it is not physical GPU or secure-hardware validation. No physical phone was attached. Native Devanagari OCR, picker providers, TalkBack, touch responsiveness and startup/frame timing still need physical-device verification. ML Kit emitted blocked transport-scheduling warnings because network-state permission is absent; no network permission was granted to silence those warnings and no app fatal crash was found in the test logs.

Implementation references: [Android Compose transitions](https://developer.android.com/develop/ui/compose/animation/composables-modifiers), [bundled ML Kit OCR](https://developers.google.com/ml-kit/vision/text-recognition/v2/android), [ZXing releases](https://github.com/zxing/zxing/releases).


Runtime dependency audit: `python3 scripts/audit-maven.py` checks the locked debug/release Maven coordinates against [OSV](https://google.github.io/osv.dev/post-v1-querybatch/) and writes `app/build/reports/security-osv.json`. This checks known advisories, not unknown vulnerabilities, native-library completeness, model integrity or exploitability. Instrumentation can be repeated with `zsh scripts/android.sh :app:connectedDebugAndroidTest --offline` when an authorised test device is running.
