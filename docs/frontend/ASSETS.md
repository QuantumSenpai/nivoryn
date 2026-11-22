# Assets and typography

All assets are served locally. There are no remote font or image requests.

## Original scenery

Generated with OpenAI ImageGen on 2026-11-20/21 for Nivoryn. No supplied reference
images were attached in this conversation. No named studio, character or brand
was used as a style reference. These are illustrations, not product recordings
or photographs of a real organisation. JPEG variants are resized for delivery.

- `app/src/main/res/drawable-nodpi/neighbourhood.jpg`, `web/assets/neighbourhood.jpg`:
  landscape scene. Final prompt: “Original hand-painted cel-shaded 2D anime-inspired
  background painting for NIVORYN. Quiet Indian neighbourhood bus shelter on the
  right, small cream houses with terracotta roofs, a bicycle parked by the shelter,
  leafy trees, distant blue-green hills, tall grasses and tiny white flowers.
  Broad vivid azure sky with luminous cumulus clouds. Warm midday light, subtle
  painted paper grain, no pixel art, characters, writing, logos, shields or UI.
  Original composition without emulating a particular studio. Left side airy
  open meadow. A pause before a journey.”
- `app/src/main/res/drawable-nodpi/neighbourhood_mobile.jpg`,
  `web/assets/neighbourhood-mobile.jpg`: separately generated portrait composition.
  Final prompt: “Original hand-painted cel-shaded 2D anime-inspired scenery for a
  mobile app, vertical 2:3 composition designed separately for a phone. Quiet
  Indian neighbourhood bus shelter under a leafy tree, cream houses, distant hills,
  a bicycle beside the shelter, grasses and flowers. Azure sky and sculptural
  cumulus clouds. Shelter and bicycle central-right and fully in frame. Warm
  afternoon light, subtle retro paint texture, no pixel art, characters, text,
  logos or UI. Do not imitate a named animation studio. Compose for mobile,
  not a crop of a landscape.”

Generation used the built-in tool, not an external paid API in application code.
Decorative scenery is excluded from screen-reader announcements. Static scenery
avoids idle render work; no frame-rate guarantee is claimed.

## Bundled fonts

Unmodified static regular TTF files from
https://github.com/notofonts/noto-fonts/tree/main/hinted/ttf:

- Noto Sans (`NotoSans/NotoSans-Regular.ttf`)
- Noto Serif (`NotoSerif/NotoSerif-Regular.ttf`)
- Noto Sans Devanagari (`NotoSansDevanagari/NotoSansDevanagari-Regular.ttf`)

Licensed under SIL Open Font License 1.1. Full licence bundled in
`app/src/main/assets/FONT_LICENSE.txt` and `web/assets/FONT_LICENSE.txt`.
Noto Sans is the functional UI family. Android 29+ has an explicit bundled
Devanagari fallback; API 26–28 selects the Devanagari family for Hindi and uses
platform fallback for uncovered glyphs. Noto Serif is limited to the wordmark.
Medium weights may be platform-synthesised from the bundled regular face.

## Guidance

Banking guidance was checked against the RBI KYC fraud advisory:
https://www.rbi.org.in/Scripts/BS_PressReleaseDisplay.aspx?prid=57244.
Official reporting handoff: https://cybercrime.gov.in/.
No report, call, payment or browser navigation starts automatically.
