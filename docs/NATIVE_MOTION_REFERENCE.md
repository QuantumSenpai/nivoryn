# Native motion reference study

On 24 November 2026 the browser connector still reported its host unavailable. Native Brave automation successfully opened the supplied [Seesaw Tempo v2 listing](https://www.seesaw.website/websites/tempo-v2) and followed its live link to [Tempo](https://tempo.com/?ref=seesaw). This updates the earlier report that reference inspection was blocked.

Actual interactions observed: Products menu activation, Escape dismissal, page scrolling, and the Earn → Routes product selector. Sequential live screenshots were sampled during those actions; this was not an inference from one gallery screenshot. The product transition changed heading contrast, collapsed the previous description, expanded the selected description and replaced the illustration inside a stable panel. The last sampled Routes states had settled while the surrounding heading and panel width stayed fixed. The header stayed pinned during page scroll. A continuous logo marquee and playing video were observed separately.

Observation request timestamps for the product selection were 109, 556 and 655 ms after input; capture itself adds latency. These establish a coarse observed state sequence, **not exact CSS duration/easing or a frame-accurate benchmark**. Native app durations below are design decisions, not claimed measurements copied from Tempo.

Nivoryn translates the useful principles to a task app: immediate press/selection feedback, 120 ms input selection, a short navigation handoff, ~180–240 ms disclosures, stable long reading text, and retained input/result/history state. System-disabled animation and the app's reduced-motion preference remove spatial motion. No marquee, autoplay video, hover requirement, proprietary fonts/assets, website scroll imitation or Tempo palette is imported. The existing Nivoryn red/near-black/paper/neutral tokens remain the source of identity.

Native effects and state are tested by the Compose host suite; the real Android result flow is recorded. Physical frame pacing, accessibility and interruption testing remain owner acceptance work. Software-rendered emulator frame samples are explicitly scoped and do not prove a smooth 60 Hz phone experience.

## 0.2.0 spatial selector

The Inspora carousel-to-top-navigation description was accessible, but its exact reference animation was not: media requests returned HTTP 429 and the original social link returned 403; the browser host was unavailable. No exact animation study is claimed. The actual Tempo listing video (84 seconds) was saved on ORBIT and sampled at 5 and 30 seconds to inspect grouped neutral panels, menu and customer-card composition.

Home’s stable four feature identities move from a labelled 2×2 group into a compact Check selector using coordinated position/size animation on the existing stable Compose stack. Large text uses two columns. No experimental shared-element API or heavy 3D was introduced. Bottom navigation remains the root system; the selector chooses input modes within Check. Navigation glyphs animate once per deliberate selection with anchored touch targets. Platform splash leads directly to usable Home, while shared content routes directly to Check.
