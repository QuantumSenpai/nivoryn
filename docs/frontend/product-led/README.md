# Product-led first slice

Review the actual video breakdown and [storyboard, shared tokens and page/screen maps](DIRECTION.md), then inspect the implementation at http://127.0.0.1:4173/ (`npm run dev --prefix web`). No new deployment has been made.

- [Desktop website](review/website-desktop.jpg)
- [Mobile website](review/website-mobile.jpg)
- [Evidence handoff](review/website-evidence.jpg)
- [Interactive examples](review/website-examples.jpg)
- [Native home](review/android-home.png)
- [Native input](review/android-input.png)
- [Native result](review/android-result.png)
- [Motion polish and profile](MOTION.md)
- [Validation, limitations and next slice](VALIDATION.md)
- [Editable assets, reproduction and licenses](ASSETS-LICENSES.md)

Website source stays in `web/`, without a framework migration. The Compose source stays in the existing Android `:app` module. The local debug preview APK is `app/build/outputs/apk/debug/app-debug.apk`. Build it using the existing Gradle wrapper and JDK/SDK setup; it is not an official production release.

The new Blender file is `/Volumes/ORBIT/Nivoryn/cinematic/product-led-v1/Nivoryn-Product-Evidence.blend`. Runtime WebPs are in `web/assets/product`; raw files and render caches remain on ORBIT. Previous work is archived at `/Volumes/ORBIT/Nivoryn/backups/product-led-before-20261121.zip`.

Review this slice before expanding history, settings and all unavailable/error flows. The native `DemoRepository` boundary remains replaceable. Authored examples do not imply a working detection backend.
