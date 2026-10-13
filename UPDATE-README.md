# Mythforge website update

Upload `Mythforge-Direct-Upload.zip` to the existing Cloudflare static deployment. It contains only the contents of `dist`, with `index.html` at its root. No Wrangler or build configuration is included in that upload.

The source project uses Node.js 22+ with no runtime dependencies:

```
npm run build
npm run check
npm run dev
```

Preview at http://localhost:8080. To package on Windows, run `python scripts/package-upload.py` after building and checking. `npm run package:upload` uses `python3` for compatibility with the included GitHub workflow.

`data/site.json` preserves the recovered official download/Discord links, rules, policies, gallery, status endpoint, release information, and supporter settings. `scripts/finish.mjs` applies the fixed eight-person beta roster, artwork placement, privacy disclosure, Minotar CSP allowance, and disclosed Wabbanode affiliate link after the main build. Future player records never alter this roster.

The hero is `public/assets/art/orrery-ravine.webp`; the thank-you illustration is `tester-tools.webp`. All six official gameplay gallery images now appear on the Modpack page. The attached hosting banner is not used because it promotes an individual excluded from the tester credits; the hosting link uses a matching text section without an unverified discount claim.

Player heads request public UUIDs from Minotar on each page visit; skin updates follow provider caching without rebuilding. Initials appear on load failures, including blocked requests. No referrer is sent. Privacy details appear on the Privacy page. Public HTML, accessible labels, imported CurseForge content, and dynamically inserted text use normal hyphens instead of em dashes.

CurseForge synchronization remains in `scripts/sync-curseforge.mjs` and `.github/workflows`. Set `CURSEFORGE_API_KEY` privately to run it; no API key was supplied or embedded. Authenticated synchronization succeeded on October 13, 2026, importing release notes and all six official gallery images. The latest stable release is 1.2.4; the server is explicitly configured to follow the latest release. Supporter packages and playtime rewards remain TBA.

Validation completed:

- 16 pages: navigation, assets, anchors, metadata, alt text, JavaScript syntax, clipboard, keyboard menu, and mocked online/offline/provider-failure status states.
- Browser: 15 main pages at 320, 390, 768, 1024, and 1440 pixels; no horizontal overflow or JavaScript errors.
- Exactly eight tester names and UUID images; excluded name absent from roster.
- All eight live avatar images loaded. Forced network failures displayed initials.
- Dynamically imported em dashes in text and accessibility labels became hyphens.
- Desktop and mobile screenshots are saved under `docs`.

This update is packaged locally; no Cloudflare deployment was performed. The recovered starter policies are retained, with avatar and affiliate disclosures added.

Follow-up: removed roster-management copy, fixed multiplayer label spacing, and replaced the avatar provider with Minotar after all eight UUID heads matched current Mojang skin pixels. Terms and privacy now clarify eligibility, submitted content, security reports, credit removal, privacy requests, retention purposes, and international providers. Comparison sources: https://craftdownunder.co/terms, https://craftdownunder.co/privacy, https://moddedmc.net/terms, https://moddedmc.net/privacy. No unverified retention deadlines, jurisdiction, analytics, or payment promises were added.
