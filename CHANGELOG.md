# Release notes

## 0.2.0 · 2026-11-24

- Reorganised the native app around Scan/Paste, discoverable tool groups, Activity and grouped Settings, with deliberate light/dark surfaces and accessible feature selection.
- Added real camera capture and image import into the existing bounded OCR/QR review flow. Camera denial keeps import available and does not repeatedly prompt.
- Added guided, pinned Llama downloads with actual progress, Android-managed network recovery, size/hash verification and a real inference readiness check. Existing compatible files remain an advanced import option.
- Added opt-in foreground observation of aggregate network byte totals with bounded in-memory samples and a Stop control. This is usage observation, not destination-level intrusion detection, TLS inspection or blocking. PCAP replay remains separate.
- Replaced the extra in-app splash delay with direct entry after the platform splash, and added brief icon/selector feedback with reduced-motion alternatives.
- Preserved application identity and preview signing continuity. This remains a development-signed preview, not an owner-key production release.
- Consolidated public entry points onto nivoryn.vercel.app with reversible redirects and prevented Android-source Git pushes from replacing the packaged website with an empty root deployment.

Internet permission now supports the explicit model download. Content analysis still runs locally and must pass airplane-mode checks; the previous UID-wide no-Internet-permission boundary no longer applies.
