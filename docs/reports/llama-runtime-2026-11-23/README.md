# Runtime evidence scope

This directory began on 23 November; final runs continued on 24 November 2026. Use PS3_VALIDATION.md and validation-summary.json for the final implementation state.

- android-build.txt / android-lint.txt / android-lint.xml: fresh final build/test commands and remaining warning details; 86 host tests, zero failures/errors/skips, zero lint errors, five explained warnings.
- android-instrumentation.txt: final resource-required 18-test run, including real Llama, cancellation/overlap, injected input, Android Keystore, history recreation/resave, imported OCR/QR/PDF, actual provider PCAP replay and benign APK inspection. Android 14 Arm64 emulator, airplane mode; not physical-phone acceptance.
- llama-native-smoke.json: real load/generate return codes, model/device/ABI, observed latency and synthetic generated text.
- llama-pipeline-smoke.json: real service modes/explanations and first/warm observations; both exact results were saved/reopened/deleted through encrypted history.
- llama-varied-smoke.json: seven authored advisory contract cases, current policy five accepted/two rejected. This is not a detection-accuracy dataset. Local source binding is distinguished from model references.
- llama-qat-*.json: earlier QAT+LoRA candidate comparison, strict citation policy, one of seven accepted. It executed but was not activated. These results cannot be compared directly to the later five-of-seven policy.
- extraction-metrics.json: actual bounded provider imports and bundled local OCR/QR, with successful extraction and rejected/blank cases recorded separately. Synthetic fixtures, not representative-photo accuracy.
- network-replay-smoke.json: provider-imported safe four-port and forty-port SYN fixtures plus corrupt-input refusal; no packets transmitted or live capture claimed.
- emulator-startup.json / emulator-flow-profile.json: actual process-cold launch and separately sampled debug UI flow; OS caches may be warm, polling/automation/software graphics affect results. Not phone speed, battery, true peak memory or steady-state FPS.
- source-provenance.json: reviewed published conversion origin, immutable revision and pins; official Meta source was also downloaded, but no local export reproduction is claimed.
- security-osv.json: 143 locked Maven runtime packages, no indexed OSV findings at query time. Not complete native vulnerability coverage.
- debug-apk-signature.txt: final debug APK signature verification; release artifact is intentionally unsigned.
- android-native.json / android-pipeline.json: historical first successful generation copies. The newer llama-* reports supersede their timing/prompt-policy state.

The demonstration is an actual Android UI screen recording, with only a synthetic OTP request. The final source/APK/video bundle is `/Volumes/ORBIT/Nivoryn/submission-2026-11-24/`; its artifacts.json records content digests. No private user content, tokens, executable malware samples or model weights are included. Models remain a separately documented resource import.
