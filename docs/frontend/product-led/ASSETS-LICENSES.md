# Assets, dependencies and reproduction

## Original product assets

Authoring file: `/Volumes/ORBIT/Nivoryn/cinematic/product-led-v1/Nivoryn-Product-Evidence.blend` (approximately 7.5MB).
Active scene in that file: `Nivoryn_Product_Evidence_v1`. Existing `Nivoryn_Cinematic`, `Nivoryn_Tech_City` and `Scene` were retained. The original tech-city file was read, never overwritten. A subsequent attempt to append the scene to the live Blender session found its MCP socket closed; the editable saved file is the deliverable, not an assumed change to the live session.

Four original mesh collections: PHONE, ENVELOPE, URL, DOCUMENT. Bevels, material nodes, lighting, orthographic camera and assembly-control empties remain editable. No model, texture or brand asset was copied from Superlist. The supplied reference video is not a runtime asset.

Reproduce on this machine (the script writes only the separately named product file):

```sh
/Applications/Blender.app/Contents/MacOS/Blender --background \
 /Volumes/ORBIT/Nivoryn/cinematic/tech-city-v2/Nivoryn-Tech-City.blend \
 --python docs/frontend/product-led/product-scene.py
```

Raw RGBA PNGs: `…/product-led-v1/renders/`. Runtime derivatives: `web/assets/product/{phone,envelope,url,document}.webp`, encoded with `cwebp -q 84`. Four layers total approximately 26KB on disk (filesystem allocation reports more). The phone content is live HTML, not burned into a video. All image dimensions are declared; the source page and ordinary CSS composition are the poster fallback. No Three.js, video decoder, scrub sequence, autoplay audio or post-processing is shipped.

Previous scenic frontend source and assets: `/Volumes/ORBIT/Nivoryn/backups/product-led-before-20261121.zip`. The unused scenery image files were also moved out of Android resource packaging and website runtime assets into `assets/archive/previous-scenery/`. Original asset provenance remains in the archived `docs/frontend/ASSETS.md`.

## Fonts

Downloaded from the authoritative Google Fonts repository:

- `https://github.com/google/fonts/tree/main/ofl/manrope`, `Manrope[wght].ttf`. Manrope copyright holders, SIL OFL 1.1. Limited variable weight axis to 500–650.
- `https://github.com/google/fonts/tree/main/ofl/dmsans`, `DMSans[opsz,wght].ttf`. DM Sans copyright holders, SIL OFL 1.1. Limited variable weight axis to 400–600; optical size pinned to 14.
- Noto Sans Devanagari, already bundled. SIL OFL 1.1; retained for Hindi/system fallback. Earlier Noto font notices are retained.

Android gets bounded-axis TTF builds. Website gets WOFF2 builds of those fonts (49,204 + 34,492 bytes). FontTools performs axis instantiation and WOFF2 encoding; glyph sets are retained. Full original OFL files live beside the web fonts and in `app/src/main/assets/`. No remote font requests. Original font files in the source tree are excluded from `web/dist` by the explicit build allowlist.

## Dependencies and licenses

No production web package was added. Website: native browser APIs (CSS transforms, IntersectionObserver, requestAnimationFrame only in response to scroll). Android remains Kotlin / Jetpack Compose; existing dependency versions are unchanged.

- Nivoryn original source and product assets: repository MIT license.
- AndroidX Activity, Compose and Material3: Apache-2.0, existing BOM 2025.12.01 and activity-compose 1.12.2.
- Kotlin / Kotlin Gradle plugins: Apache-2.0, existing project toolchain.
- Playwright 1.65.1: Apache-2.0, existing test-only dependency.
- `@axe-core/playwright` 4.14.0 and axe-core: MPL-2.0, existing test-only dependencies. Actual installed package notices remain authoritative.
- Blender 5.3.0: GPL-3.0-or-later authoring tool; the Blender binary is not bundled in the product.
- libwebp/cwebp: BSD-style license, encoding tool only.
- FontTools 4.68.0 (MIT), Brotli 1.2.1 (MIT) and Pillow 13.0.0 (HPND/MIT-CMU): temporary font optimization/review tooling, not runtime dependencies.

## Runtime budget and behavior

Only optimized font/image derivatives, page HTML, CSS and two small modules are copied to `web/dist`. No reference video, original Blender files, render caches, scenery archives or test files are copied. The local Node server binds to loopback and deliberately uses no-store during review. Production cache/CDN behavior has not been deployed or measured. Byte ranges are not required by this chosen still-layer path. The source-build link is the real project setup document; no signed production APK URL is invented. A current local debug APK is generated as a review artifact, separate from the marketing release promise.
