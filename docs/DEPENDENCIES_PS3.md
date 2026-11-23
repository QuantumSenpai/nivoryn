# Runtime and resource licence inventory

Resolved Android coordinates are pinned by app/gradle.lockfile; this is not a claim that all transitive/native notices have been fully audited.

| Component | Version / source | Licence / redistribution boundary |
|---|---|---|
| Application | repository LICENSE | Follow the repository licence |
| Kotlin / AndroidX / Compose | Kotlin 2.2.21; Compose BOM 2025.12.01; activity 1.12.2 | Apache-2.0; retain upstream notices |
| ExecuTorch Android | org.pytorch:executorch-android:1.2.0 | BSD-style upstream licence plus bundled third-party native notices; [upstream LICENSE](https://github.com/pytorch/executorch/blob/v1.2.0/LICENSE) |
| fbjni | 0.7.0, locked runtime transitive | Apache-2.0; verify packaged notices before distribution |
| ML Kit Latin / Devanagari | 16.0.1, bundled resources | Google ML Kit SDK terms apply; model/resources are not covered by the app's source licence. [Terms](https://developers.google.com/ml-kit/terms), [Android setup](https://developers.google.com/ml-kit/vision/text-recognition/v2/android). Redistribution/licence review remains a release task; not described as open-source OCR |
| ZXing core | 3.5.4 | Apache-2.0; [source](https://github.com/zxing/zxing) |
| Llama weights | Official Meta 1B SpinQuant source downloaded; tested published ExecuTorch conversion pinned in trusted-model.json | Llama 3.2 Community Licence/AUP; licence + notice bundled with app/resources. [Source/licence](https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct-SpinQuant_INT4_EO8); conversion provenance in MODEL_SETUP.md |
| Fonts | existing Manrope / DM Sans / Noto assets | SIL OFL 1.1; existing bundled FONT_LICENSE.txt |
| Linux tcpdump / libpcap | system-provided, not bundled | BSD-style licences; install from your distribution |

No CICIDS, UNSW-NB15, NSL-KDD or third-party malware datasets are downloaded or used. Synthetic network fixtures contain no executable payloads. Empty evaluation JSONL files are not an evaluated dataset. There is no trained network classifier or measured detection-accuracy claim.

Lint's newer-version suggestions are not automatically applied: runtime/export compatibility is pinned, and framework migration is outside this increment. Existing style/resource suggestions remain visible. Native debug-symbol stripping warnings originate in packaged upstream binaries; no blanket lint suppression or test disabling was introduced.

## Continuation additions / corrections

- AndroidX EXIF is now explicitly pinned to 1.4.2 (Apache-2.0), replacing the old 1.0.0 transitive/platform parser usage; [official release notes](https://developer.android.com/jetpack/androidx/releases/exifinterface).
- The UCI SMS corpus **is now used** for a separately labelled spam-proxy classifier, under the CC BY 4.0 attribution stated by [UCI](https://archive.ics.uci.edu/dataset/228/sms+spam+collection). Original corpus/readme/provenance remain on ORBIT; coefficients + attribution are bundled. Prior statements that no trained classifier/public corpus existed describe the earlier increment only. No CICIDS/UNSW/NSL or malware executable corpus is used.
- Official Meta SpinQuant 1B download succeeded after owner-authorised authentication. The separately published conversion linked by upstream ExecuTorch was verified by pinned hashes and actual Android emulator generation. It is not a local export. Llama 3.2 licence/AUP applies separately from app/runtime; no Llama 2 tokenizer conversion is used. See MODEL_SETUP.md for exact revisions and compatibility limits.
- Pack-signing tooling relies on system OpenSSL, while Android verifies SHA256withRSA through the platform. Publisher private keys are never bundled or generated as production credentials by the app.
