# Product-led direction · first reviewable slice

## Evidence and intent
Reference inspected: `/Users/kyzenn/Downloads/superlist-website.mp4`, 1716×1076, 30 fps, 17.233 s. Contact sheet extracted at 0, 2, 4, 6, 8, 10, 12, 14, 16 seconds. The timestamps below describe the recording, not intended dwell times for Nivoryn.

- 0–2 s: flat saturated red, a central sculptural assembly with black faces and metal edges. Large white grotesk type overlaps the assembly; sparse full-width navigation anchors the viewport.
- 4–8 s: hard red-to-near-black handoff. A persistent left visual and headline balance a right-hand column of short editorial arguments. Depth comes from studio-lit objects rather than effects on the UI.
- 10 s: paper-white section with a large cutout subject. A substantial change of material and density resets attention.
- 12–16 s: black editorial, white hiring and red newsletter stages. These are reference pacing cues, not Nivoryn content requirements. Its cookie banner, capsule CTA and unrelated gadget selection are deliberately not adopted.
- Pacing: the recording scrolls quickly. It does not prove scroll distances or easing curves, and cannot establish mobile performance. Nivoryn gets its own readable holds, keyboard access and static fallback.

Subject inventory: a message's wording, the host hidden in a familiar URL, a local draft, the preserved original input, a conditional payment, a specimen document, an unverified issuer, an authored example, local-only storage, an unconnected engine. On this page, **the phrase that makes payment a condition of earnings becomes the physical bridge from message to evidence**.

Feeling curve: recognition (red product assembly) → scrutiny (message separates) → understanding (same phrase appears in the phone) → agency (a concrete next step) → confidence without false certainty (limits and source). Peak: “The same words moved from the message into the evidence.” No authored empty scroll holds.

Grammar: compact product stage, then editorial argument and working example. Not a scenic journey, autoplay film, gallery or full-page slide sequence. No scroll snapping. User brief supplies creative direction; composition details below are authored within that brief.

## Storyboard
Ranges are fractions of the hero's scrollable travel, not seconds. The result occupies the last 30% of its travel. Remaining sections use ordinary document flow.

| Shot | Start → end composition | Scroll | Point | Mobile | Reduced motion |
|---|---|---|---|---|---|
| Assembly | Red field; headline left, phone and envelope right → assembly opens slightly | 0–20% | Product inspects suspicious content | Phone and message only; headline above | Static opening; no entrance delay |
| Separate | Envelope behind phone → message rises toward reader; URL/document move to edges | 20–45% | Read the actual demand | Hide secondary objects; keep message legible | Both input and evidence printed in flow |
| Evidence | Phrase in message → matching phrase in steady phone result; explanation beside it | 45–70% | Upfront payment pressure | Small travel; no camera tilt | Highlight remains visible |
| Resolve | Phone comes forward → stable evidence and “Pause the payment” action | 70–100% | Evidence leads to action, not certainty | Full readable result; no tiny screen requirement | Static result |
| Editorial (next slice expansion) | Black handoff; pinned product visual + three concise paragraphs | Ordinary scroll; ≤1 extra viewport | On-device vs connected verification | Unpinned stacked content | Same static content |
| Working example | White section; input and result alongside four underlined tabs | Ordinary scroll | Each input type has distinct limits | Stacked input then result | Immediate state changes |
| Close | Red field; product resolves beside genuine build action | Ordinary scroll | Android/source destination | One object, simple CTA | Identical information |

## Shared tokens
| Role | Value |
|---|---|
| Marketing red | #F44748; dark ink on red for AA contrast |
| Black / paper / ink | #141414 / #F7F7F5 / #191919 |
| Secondary text | #595959 on paper; #BABABA on black |
| App primary | Graphite #191919; brand red is not the default danger state |
| Warning / error | #80520B / #A63434, always with descriptive text |
| Display | Manrope 500–650; short two-line headings |
| UI / body | DM Sans 400–600, website 16–18px, Android 14–16sp; Devanagari fallback retained |
| Space | 4, 8, 12, 16, 24, 32, 48, 64, 96 |
| Radii | 6 controls, 12 panels, 16 sheets |
| Controls | ≥48px/dp; press 120ms, hover/focus 160ms |
| Motion | tabs 220ms, state 200ms, Compose navigation 220ms; reduced motion removes travel |
| Icons | One original outline family using 1.75px strokes; labelled destinations |

## Website page map
`/` product launch, hero/evidence choreography, Example tabs, limitations and source/download close. `/privacy.html` actual local/network behavior. `/terms.html` demo scope and MIT license, no invented guarantees. `/notices.html` Open source and asset notices. `/accessibility.html` implemented support and testing limits. No tracking and no consent banner. Initial action: Build from source; no release APK URL is invented. A local debug APK is a validation artifact, not an official signed release.

## Native screen map
Home → prominent draft/paste/import, compact Link/Message/Screenshot/Document list. Check → preserved input/review → example preparation or unavailable custom check → assessment → evidence + highlighted original → guidance/edit. History → existing local saved cases retained while history list/search/export is expanded in the next slice. Settings → current appearance/language/reduced-motion/local storage controls, later explicit connectivity/models/permissions views. Roots: Home, Check, History, Settings; labelled outline icons, underline selection.

State contract: `CheckDraft`, `DemoRepository`, `DemoResult` stay Kotlin types. An exact sample ID and unchanged text are required for a demo verdict. Editing invalidates the example. Future `AssessmentEngine` integration must provide provenance; it is not part of this slice. Web uses typed JSDoc examples and never accepts user submissions as live analysis.

State map for full frontend: empty → importing → review → analysing → result; cancellation returns input; invalid MIME/oversize/import failure stay in input; missing-model and offline limitations offer model settings or an explicitly authored example. No artificial percentages. This first slice validates input→example→evidence→guidance and custom-input unavailable behavior. Expanded history, all import failure UI and model/connectivity settings follow direction review.

## Production approach
Blender-rendered, transparent object layers with a semantic DOM phone interface and scroll-driven transforms. No Three.js runtime, no post-processing, no second heavy render path, no scrubbing video decoder. Separately rendered optimized layers are also the poster fallback. This still-based choice is the lightest version of the permitted rendered approach and lets important text stay live and readable. Reassess only if review requires actual changing 3D camera geometry. Runtime byte budget ≤600KB for all initial product layers and fonts, with fixed image dimensions; original files/cache stay on ORBIT.

## Review gate
Deliver and screenshot desktop, tablet, mobile, reduced-motion and the matching Compose input/result before expanding. Verify actual controls and provenance; do not treat a working demo as a backend. No claim of outperforming the reference. No attached separate app reference has been assumed.

## Archive
Before editing: `/Volumes/ORBIT/Nivoryn/backups/product-led-before-20261121.zip`. Blender's existing file is `/Volumes/ORBIT/Nivoryn/cinematic/tech-city-v2/Nivoryn-Tech-City.blend`; inspected scenes: `Nivoryn_Cinematic`, `Nivoryn_Tech_City`, `Scene`. Keep these untouched, build the new scene in a separate file and append it to the live file only after isolating the scene. No production changes to previous hosted versions.
