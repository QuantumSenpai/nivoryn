# Nivoryn Guard - Implementation Plan

Current task: native 0.2.0 UX, camera, guided verified Llama setup and canonical deployment. See [the implementation ledger](docs/reports/native-0.2.0/IMPLEMENTATION.md) and [acceptance report](docs/reports/native-0.2.0/README.md). The older checklist below is historical planning; unimplemented VPN alerts and broader monitoring are not shipped claims.


The owner confirmed on 2026-11-24 that the immediate task is to finish and verify 0.1.3. That stabilization update is complete; see [the recovery and regression report](docs/reports/android-0.1.3/README.md). The phases below are an unimplemented future roadmap, not shipped capabilities.

## Phase 1: UI/Animation Overhaul + Live Network Test
### Week 1-2

#### 1.1 Animation System Enhancement
- [ ] Create `AnimationConstants.kt` - centralized animation specs
- [ ] Create `AnimatedText.kt` - typographical animations (letter-by-letter, word-by-word, slide/fade)
- [ ] Create `IntroOutroAnimations.kt` - app launch/exit animations
- [ ] Create `GearRotation.kt` - rotating gear for settings
- [ ] Create `MicroAnimations.kt` - press, hover, focus, transition micro-interactions
- [ ] Update `Theme.kt` - add new easing curves, durations

#### 1.2 UI Redesign - Card/Box System
- [ ] Create `UICards.kt` - ElevatedCard, OutlinedCard, FilledCard, GradientCard
- [ ] Create `UIBoxes.kt` - SectionBox, EvidenceBox, WarningBox, InfoBox
- [ ] Create `UIShapes.kt` - custom shapes for visual separation
- [ ] Update `Color.kt` - add semantic colors for cards/boxes
- [ ] Redesign `HomeScreen` / `CheckScreen` with new card system
- [ ] Redesign `ResultScreen` with evidence cards
- [ ] Redesign `SettingsScreen` with card layout

#### 1.3 Live Network Monitoring
- [ ] Add `NetworkMonitorService.kt` - foreground service for network monitoring
- [ ] Add `NetworkAlert.kt` - alert system for suspicious network activity
- [ ] Add `NetworkMonitorScreen.kt` - UI for network monitoring
- [ ] Add required permissions (FOREGROUND_SERVICE, NETWORK_STATE)
- [ ] Create notification channel for alerts
- [ ] Integrate with Settings (enable/disable)

#### 1.4 Splash Screen Enhancement
- [ ] Enhance `SplashScreen.kt` with typographical animation
- [ ] Add brand mark animation (draw path animation)
- [ ] Add staggered entrance for text elements

---

## Phase 2: Camera/Document Scanning + Model Import UX
### Week 2-3

#### 2.1 Camera Integration
- [ ] Add `CameraScreen.kt` - CameraX integration
- [ ] Add `DocumentScanner.kt` - ML Kit document detection + perspective correction
- [ ] Add `QRScanner.kt` - ZXing live camera QR scanning
- [ ] Add camera permission handling
- [ ] Create scan button in main check flow

#### 2.2 Model Import UX Enhancement
- [ ] Redesign `ModelSetupScreen.kt` with card-based layout
- [ ] Add visual progress indicators for import
- [ ] Add step-by-step guided import flow
- [ ] Add resource verification visual feedback
- [ ] Add "Test Inference" with animated results

#### 2.3 Document Scanning Flow
- [ ] Integrate camera → review → OCR → check pipeline
- [ ] Add multi-page PDF support
- [ ] Add manual crop/adjustment

---

## Phase 3: Performance Optimization + Testing + Deploy
### Week 3-4

#### 3.1 Performance Optimization
- [ ] Profile and optimize Compose recompositions
- [ ] Implement `derivedStateOf` for expensive calculations
- [ ] Optimize image loading (Coil/Fresco)
- [ ] Reduce APK size (enable R8 fully, remove unused resources)
- [ ] Optimize LlamaRunner initialization (lazy loading)
- [ ] Add baseline profiles

#### 3.2 Compatibility Testing
- [ ] Test on API 26 (Android 8.0) - minSdk
- [ ] Test on API 28, 29, 30, 31, 33, 34, 35, 36
- [ ] Test on different screen sizes (phone, tablet, foldable)
- [ ] Test RTL (Hindi)
- [ ] Test reduced motion, high contrast, large text
- [ ] Test dark/light theme switching

#### 3.3 Animation Testing
- [ ] Test all animation flows end-to-end
- [ ] Verify no jank (60fps/120fps)
- [ ] Test reduced motion disables all animations
- [ ] Test orientation changes during animations
- [ ] Test back navigation during transitions

#### 3.4 Build & Deploy
- [ ] Build preview APK (`./gradlew :app:assemblePreview`)
- [ ] Verify with `scripts/verify-distribution-apk.py`
- [ ] Update `web/release.json` with new metadata
- [ ] Run `web/prepare-deploy.mjs`
- [ ] Deploy to Vercel
- [ ] Run all browser tests (download, product, SEO)

---

## File Storage Strategy
- **ORBIT External SSD** (`/Volumes/NivorynBuildCache/`):
  - APK builds (`app/build/outputs/`)
  - Gradle cache (`.gradle/`)
  - Vercel deployment package (`nivoryn-vercel/`)
  - Model files (`.pte`, tokenizer)
  - Test artifacts, screenshots
- **Mac Internal** (`/Users/kyzenn/Projects/nivoryn/`):
  - Source code
  - Documentation
  - Small config files
  - Git repository

---

## Current Status (2026-11-24)
- ✅ v0.1.3 deployed to https://nivoryn.vercel.app
- ✅ Download verified (SHA-256 match, 26 MB Arm64)
- ✅ Product browser tests pass (5 viewports, a11y, reduced-motion, no-JS)
- ✅ SEO tests pass
- ✅ 90 Android host tests, 18 resource-required Android 14 tests and 17 website tests passed
- ✅ Exact minified APK passed offline Llama/evidence/history flows on Android 14 and Android 17 with 16 KB pages
- ✅ Removed unfinished monitor/camera wiring and unused dependencies; verified packaging and model-transfer repairs
- ⏸ Future roadmap below awaits a separate implementation task
