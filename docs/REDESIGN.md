# Website redesign and automatic updates

The October 27, 2026 redesign keeps the existing static generator, Cloudflare Worker, application-service binding, store, and public page URLs. The shared theme uses violet, ivory, and restrained gold. The homepage uses the existing pack logo and title artwork, with images from the official pack gallery. No artwork was generated or extracted from the supplied mockup.

## Where changes belong

- `scripts/homepage.mjs`: homepage sections and small inline SVG icons.
- `scripts/community-page.mjs`: beta tester credits and the fixed ten-player roster.
- `scripts/modpack-page.mjs`: modpack overview, requirements, encounters, and gallery captions.
- `scripts/supporter-layout.mjs`: rank comparisons and the full reward/command layouts; checkout remains in `scripts/support-page.mjs`.
- `public/assets/site.css`: shared styles; the readable redesign section follows the legacy page styles.
- `scripts/build.mjs`: shared navigation, existing page templates, and generated release snapshot.
- `public/assets/site.js`: navigation, clipboard, filtering, server status, and visitor-time release checks.
- `release-service.mjs`: public release feed validation, edge caching, and verified fallback.
- `data/site.json`: operator settings, requirements, server version, links, and changelog.
- `data/mods.json`: verified published client manifest and included projects.

Build with `npm run build`; run the complete verification suite with `npm run check`. The build also regenerates Markdown, SEO metadata, and the public content catalog. Commit source and `dist/` together, as before.

## What updates automatically

Each homepage or modpack-page visit checks `/api/release`. The Worker queries the same public CFWidget feed already used by the scheduled importer. It accepts only stable client ZIPs for the configured Minecraft version and NeoForge, validates project identity, and never replaces a verified file with an older one. It builds release links against the fixed official CurseForge project URL. No API key is required or sent to the browser.

Successful results are cached inside the Cloudflare Worker for five minutes. Browser requests and responses use `no-store` so the zone's longer browser cache settings cannot hold back visitor-time checks. A returning background tab checks again after five minutes. Timeouts, invalid metadata, and provider failures return the last verified build snapshot; the page explains that it is showing verified information. CFWidget can lag behind CurseForge publication, so a fresh upload is not guaranteed to appear immediately. A feed failure retries on a subsequent request.

The homepage version, release link, and latest release date update together. The precise mod count stays labeled with its own verified manifest version; it never claims to describe an unimported release. The existing active GitHub workflow checks every six hours and imports the client ZIP to refresh the full mod list, release labels, and changelog, builds and checks the result, and publishes through the existing deployment setup.

The server's required pack version remains separate: `currentVersion` is the version installed on the actual game server. `serverTracksLatest` remains `false`. Update `currentVersion` when the game server is upgraded; publishing a client release alone must not tell players that the server has upgraded. Minecraft, Java, loader, and allocated RAM come from the shared configuration rather than homepage literals.

## Deployment

The repository is `j3sterl0rd-max/Mythforge`, production branch `main`. GitHub reports a successful **Workers Builds: website** integration for the pre-redesign revision, and the scheduled release workflow is active with recent successful runs. Pushing to `main` triggers the existing Cloudflare build. The Worker remains named `website`, serves `dist/`, and retains `WHITELIST_SERVICE` pointing at `mythforge-whitelist`.

The local static preview uses the same release handler but does not proxy production whitelist submissions. Application unavailability in that preview is expected. The production application backend is preserved and tested with the existing controlled application scenarios.

## Verification

- Full build and checks: all 18 page representations, local links and anchors, metadata, structured data, JavaScript, application-service routing and form states, Markdown/HTML negotiation, clipboard, keyboard menu, and live-status states.
- New deterministic checks: release compatibility, malformed and stale feeds, outage fallback, cache hits and writes, HEAD and unsupported methods, client updates, link validation, and independent server version.
- Browser review: 36 page/viewport combinations at 320, 768, and 1024 pixels, with one H1 per page and no horizontal overflow. Homepage also visually reviewed at 390 pixels and the default desktop viewport.
- Browser interactions: mobile menu, Escape dismissal, address copying with visible feedback, mod search, and real server status. No real whitelist application or purchase was submitted.
- Review captures: `docs/redesign-desktop.png` and `docs/redesign-mobile.png`.

## Site-wide refinement

The follow-up polish removes the desktop More menu: Overview, Modpack, Getting Started, Server, Whitelist, Rules, Support, and Changelog are direct navigation links. Mobile navigation shows these same links without a second submenu. Footer links are grouped by purpose.

Beta credits are now generated as a normal page with their own canonical URL, rather than copying the homepage and replacing its metadata. The old green workbench artwork is replaced by the pack's violet title artwork, and the original ten testers remain credited with player heads and local initial fallbacks.

The modpack overview now has a compact requirements bar, readable gameplay cards, a dedicated encounter section, and descriptive gallery captions. The supporter page adds four comparison cards, a sticky rank index, two-column reward details, and keyboard-operable command disclosures. Prices, reward definitions, upgrade rules, gameplay access, checkout URLs, and service bindings remain unchanged. Repeated marketing sections previously appended by the SEO build were removed while preserving metadata, structured data, and public routes.

Typography, page headings, numbered setup steps, rules, search fields, release cards, anchor offsets, and mobile layouts are consistent throughout the site. All 18 routes were checked at 320, 390, 768, 901, 1024, and 1440 pixels: 108 combinations, with one main heading and no horizontal overflow. Direct navigation, the complete tester roster, mobile menu/Escape, rank anchors, and command disclosures were also checked in the browser.
