# Third-party notices

Versioned runtime/resource inventory and upstream licence links: [docs/DEPENDENCIES_PS3.md](docs/DEPENDENCIES_PS3.md). Resolved dependencies are pinned in `app/gradle.lockfile`.

- ExecuTorch: upstream BSD-style licence and native third-party notices.
- Kotlin, AndroidX/Compose, EXIF and ZXing: Apache-2.0.
- ML Kit Latin/Devanagari: Google ML Kit SDK terms; bundled offline resources are not relicensed as app source.
- Llama 3.2: separate Community Licence/AUP; bundled licence/notice and visible “Built with Llama” attribution. Published conversion and official source provenance are in docs/MODEL_SETUP.md.
- UCI SMS Spam Collection: CC BY 4.0, Almeida & Hidalgo (2011), DOI 10.24432/C5CC84; bundled SMS_CLASSIFIER_NOTICE.txt.
- Existing Manrope/DM Sans/Noto assets: SIL OFL; retain bundled FONT_LICENSE.txt.

These notices do not replace upstream licences. A complete native/transitive redistribution review remains an owner release task; the local debug APK is a test artifact and the production release still requires owner signing.
