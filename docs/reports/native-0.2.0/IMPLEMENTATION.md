# Native 0.2.0 implementation and acceptance ledger

Starting point: clean main at 627465f, version 0.1.3/versionCode 4. No physical Android device is connected. The available Android 14 and Android 17 Arm64 emulators are functional test targets, not phone performance evidence.

| Capability at intake | Actual state | Destination in 0.2.0 |
|---|---|---|
| Text, URL, OCR/QR, first-page PDF | Working within documented bounds | Check workspace → review → real analysis |
| Llama/ExecuTorch | Needs setup on a fresh installation; emulator inference previously verified | Guided local AI setup → pinned download/import → verification → real inference |
| Classifier and generated advice | Experimental | Evidence/results with actual explanation mode |
| Encrypted history/export | Working | Activity |
| Static APK inspection | Working, static indicators only | Home → APK inspection |
| Network capture import | Working, bounded IPv4 PCAP replay | Network → Capture import |
| Android live packet/VPN inspection | Unavailable; no forwarding engine | Clearly disclosed limitation, never represented as replay |
| Camera capture | Missing | Home → Scan → camera/import → review |
| Live usage observation | Missing | Network → explicit start/stop → bounded byte-rate samples |
| Audio, issuer authentication, cross-app malware behaviour | Unavailable | Disclosed; no usable feature claims |

Screen map: Home (Scan/Paste, feature tiles, concise readiness, tools/activity/resources) → Check (same feature identities in a compact selector) → Review → Result. Bottom roots: Home, Check, Activity, Settings. Secondary destinations: Scan, Network, APK inspection, Resources, Local AI, Rules, About/privacy/accessibility.

Implementation decisions: stable coordinated Compose transitions rather than experimental shared elements; grouped tonal surfaces with square-soft corners; delegated Android camera capture with narrow FileProvider grants; Android DownloadManager for OS-managed retry/resume and real byte progress, explicit cancellation, no claim of manual pause support; local AI readiness requires real validated inference; TrafficStats aggregates for observation without installing a VPN, collecting payloads or changing routing. Aggregate usage has no destination/DNS/TLS visibility and is not packet threat detection or blocking.

The new download feature explicitly requires Internet permission. Analysis continues locally and must pass airplane-mode tests; the previous UID-wide no-Internet-permission guarantee cannot remain true. No content-analysis endpoint is introduced.

Validation and readiness results will be appended as increments complete. The full official PS3 brief must be checked against available submission evidence; unsupported capabilities remain blockers where required.
