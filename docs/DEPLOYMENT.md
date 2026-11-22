# Website and APK downloads

Public website: **https://nivoryn.vercel.app**

The website and the current signed development/test APK are served together as static files by Vercel. Visitors can use either Download APK button, with no Vercel account required. The binary is not committed to Git. Android 8.0 or newer is required; this is an early-access test build, not a signed production release.

Release metadata is in `web/release.json`. The APK filename includes its SHA-256 prefix, and the download section links to `downloads/SHA256SUMS.txt`. The package helper checks the entire APK against the recorded hash and size before publishing; it refuses a different binary or unrelated output directory. APK responses use the Android package MIME type and attachment disposition. Versioned APK files have immutable caching; download artifacts are excluded from search indexing.

## Repeat a website deployment

On this Mac, mount ORBIT and its NivorynBuildCache image as described in [the offline guide](OFFLINE_IMPLEMENTATION.md). The APK and large deployment staging directory stay on the SSD.

```sh
npm test --prefix web
npm run build --prefix web
node web/prepare-deploy.mjs
vercel link --yes --project nivoryn --scope quantumsenpais-projects --cwd /Volumes/NivorynBuildCache/nivoryn-vercel
vercel deploy --prod --yes --scope quantumsenpais-projects --cwd /Volumes/NivorynBuildCache/nivoryn-vercel
```

Only the prepared static package is uploaded. Android sources, SDKs, tests, Git data, environment files and CLI credentials are excluded. `vercel link` can generate `.env.local`; `.vercelignore` explicitly excludes `.env` and `.env.*`, along with the private `.vercel` directory. Do not serve credential-bearing deployment directories with a public development server.

For a different APK release, update `web/release.json`, both download URLs, the exact APK header route in `web/vercel.json` and the SoftwareApplication download metadata. Reverify the APK signature and runtime behavior, then use a fresh package directory so old large binaries do not accumulate in a single CLI upload. A production Android release also needs the owner's release-signing key. Multiple long-lived APK versions can later move to a dedicated release/object store; that is not required for this first test build.

## Checks after deployment

```sh
NIVORYN_TEST_URL=https://nivoryn.vercel.app node web/tests/product-browser.cjs
node web/tests/download-browser.cjs https://nivoryn.vercel.app
node web/tests/seo-browser.cjs https://nivoryn.vercel.app
```

The download check uses an anonymous browser, downloads the actual bytes to a temporary SSD location and compares SHA-256 with the tested APK. It does not use Vercel authentication or a protection-bypass token. The website suite checks five widths, keyboard/touch selection, scrolling, reduced motion, no-JavaScript fallback and automated accessibility. The SEO check verifies public canonical pages, favicon/social assets, MIME types, sitemap/robots and the permanent `/index.html` redirect.

## Brand and SEO assets

`web/favicon.svg` is the primary, solid red/ink Nivoryn mark, with 16/32/48px ICO variants, a 48px PNG and an Apple touch icon. Social metadata uses `web/assets/brand/social-card.png` at 1200×630. Regenerate these and the README preview PNGs from their original SVGs with a local server running:

```sh
python3 -m http.server 4178 --bind 127.0.0.1 --directory web
# Another terminal:
node web/scripts/render-brand.cjs http://127.0.0.1:4178
```

The five HTML pages have unique titles/descriptions and production canonical/social URLs. The homepage includes factual WebSite and SoftwareApplication structured data, without invented ratings. `robots.txt` references the canonical `sitemap.xml`. This is technical SEO setup, not a claim of indexing, rankings or rich-result eligibility. Search Console ownership/submission is not configured by this deployment.

README graphics, badges and Mermaid colours follow the app/website palette. GitHub still controls the Markdown page's own light/dark theme. Source and README edits remain uncommitted until deliberately reviewed and committed; deployment does not push unrelated work.
