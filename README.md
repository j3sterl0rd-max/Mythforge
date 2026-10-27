# Mythforge: Beyond the Horizon

For the October 2026 redesign, live release checks, current GitHub/Cloudflare deployment, and verification results, see [docs/REDESIGN.md](docs/REDESIGN.md).

The complete official website project for **mythforge.win**, with a public-server address of **play.mythforge.win**. Thirteen statically generated HTML pages, plain CSS, and a small progressive-enhancement JavaScript file. No framework, external fonts, analytics, runtime libraries, or payment forms.

## Start here

The built `dist/` directory is included and ready to deploy. Edit `data/site.json`, then rebuild. Do not edit generated HTML: rebuilding replaces it.

Requires **Node.js 22 or newer**. No `npm install` is required to build, run locally, or check the project.

```sh
npm run build
npm run check
npm run dev
```

Open **http://localhost:8080**. Stop with Ctrl+C. Use a local HTTP server rather than opening files directly, because URLs are rooted at `/`.

## Project layout

```text
mythforge-site/
  data/site.json                All editable site information
  public/assets/site.css        Shared design tokens, layout and responsive CSS
  public/assets/site.js         Copy, menu, release search and optional live status
  public/assets/art/            Pack artwork and replacement assets
  scripts/build.mjs             Shared layout and page templates; outputs HTML
  scripts/check.mjs             Link, asset, markup and interaction checks
  scripts/serve.mjs             Dependency-free local HTTP server with 404 support
  dist/                        Ready-to-host generated static files
  wrangler.jsonc                Cloudflare Workers Static Assets configuration
  package.json                 Build, check, preview and deploy commands
  docs/                        Editing reference and validation notes
```

Pages: Home, Modpack, Server, Getting Started, Support the Server, Rules, Changelog, Official Links, Contact, Terms, Privacy, Refunds, and the custom 404.

## Configure the site

Edit **`data/site.json`**, using valid JSON (no comments or trailing commas), then run `npm run build`.

| Key | Purpose |
| --- | --- |
| `siteUrl` | Canonical HTTPS origin; used by SEO tags, social images and sitemap |
| `serverAddress` | Server IP/hostname shown throughout the site and copied by buttons |
| `minecraftVersion`, `loader`, `loaderVersion`, `javaVersion` | Client requirements; initialized from supplied Specifics.txt |
| `currentVersion` | Required public-server pack version; `null` until confirmed |
| `recommendedRam` | Allocated **client** memory string, for example `"8 GB"`; `null` until confirmed |
| `uptimeNote` | Availability/maintenance explanation; no uptime percentage is invented |
| `links.curseforge` | Official HTTPS pack listing |
| `links.discord` | Official HTTPS Discord invitation |
| `links.store` | External HTTPS checkout/store URL |
| `links.contact` | Official HTTPS support page or `mailto:` address |
| `status` | Optional status endpoint, timeout, refresh interval |
| `art` | Asset locations and gallery entries |
| `supporterPackages`, `playtimeUnlocks` | Packages and separate proposed playtime rewards |
| `rulesDraft`, `rules` | Draft notice and editable rules |
| `changelog` | Verified release notes, newest first |
| `policies.terms`, `.privacy`, `.refunds` | Final plain-text policies; paragraphs separated by blank lines |

Missing external links open an explanatory Official Links page. No fabricated Discord, CurseForge, checkout or contact URL is included. Package buttons appear only when an active package has a valid checkout URL. Version numbers present in the supplied archives do **not** establish the live public server version; the site intentionally leaves that configurable.

## Deploy with Cloudflare Workers / Static Assets

This is an assets-only Worker; there is no server-side JavaScript entrypoint. `wrangler.jsonc` points at `dist/`, uses automatic HTML trailing-slash handling, and serves `404.html` with a real 404 response for unmatched routes.

### GitHub-connected deployment (recommended)

1. Create an empty GitHub repository, for example `mythforge-website`. Do not initialize a second README if uploading this Git history.
2. Upload the **contents** of this project directory to the repository root, including `public/`, `data/`, `scripts/`, `dist/`, and `wrangler.jsonc`. The project is already initialized with Git. Or use:

   ```sh
   git remote add origin YOUR_GITHUB_REPOSITORY_URL
   git push -u origin main
   ```

3. In Cloudflare, open **Workers & Pages → Create application → Import a repository**. Connect GitHub and select the repository.
4. Use these settings:

   | Setting | Value |
   | --- | --- |
   | Worker name | `mythforge` (must match `name` in wrangler.jsonc) |
   | Production branch | `main` |
   | Root directory | Repository root (leave blank if project is at root) |
   | Build command | `npm run build && npm run check` |
   | Deploy command | `npx wrangler@4 deploy` |
   | Node version | 22 or newer |

5. Save and deploy. Check the supplied `workers.dev` URL first. Cloudflare's native build integration manages deployment authentication; never commit API tokens.
6. In the Worker’s **Settings → Domains & Routes**, add **mythforge.win** as a Custom Domain. The domain must be in your Cloudflare account. Follow Cloudflare’s DNS prompts and verify HTTPS after activation.
7. Optionally add **www.mythforge.win** and a Cloudflare redirect to the canonical apex. Keep the **play.mythforge.win** Minecraft DNS record separate; do not route the Minecraft hostname to this website Worker.
8. Future pushes to `main` trigger a build and deployment. Use branches for reviews and enable Cloudflare preview builds if wanted.

No remote repository or domain has been changed by this deliverable.

### CLI deployment

With Node.js installed and your Cloudflare account ready:

```sh
npm run build
npm run check
npx wrangler@4 login
npm run deploy
```

Wrangler is fetched only for deployment. You can install and pin a chosen version as a dev dependency later and commit its lockfile. Hosting serves only `dist/`; configuration/source files outside it are not public assets.

Cloudflare references, checked October 13, 2026:

- [Static Assets](https://developers.cloudflare.com/workers/static-assets/)
- [Workers Builds / GitHub integration](https://developers.cloudflare.com/workers/ci-cd/builds/)
- [Build configuration](https://developers.cloudflare.com/workers/ci-cd/builds/configuration/)
- [404 handling](https://developers.cloudflare.com/workers/static-assets/routing/static-site-generation/)

## Add or replace artwork

Put files in **`public/assets/art/`**. Use simple filenames without spaces, then set paths like `"assets/art/hero.webp"` in `data/site.json`. Rebuild to copy assets to `dist/`.

| Setting / file | Placement |
| --- | --- |
| `art.hero` | Full-width homepage artwork; 16:9 image, ideally ≤ 500 KB WebP |
| `art.logo` | Optional image logo in header; `null` keeps the restrained text wordmark |
| `art.icon` | Modpack information panel |
| `art.boss` | Boss feature art; `null` shows the intentional placeholder |
| `art.sectionBackground` | Homepage editorial artwork; falls back to hero |
| `art.screenshots` | Modpack screenshot gallery; empty list shows the curated-gallery placeholder |
| `public/assets/art/favicon.png` | Browser icon; replace with your own 32×32 image |
| `public/assets/art/social-preview.jpg` | Open Graph/Twitter image; replace with a 1200×630 image |

Existing background, logo, icon and favicon were extracted from the supplied **1.2.3** pack. The midground and foreground layers are preserved as optional art assets. No generic stock artwork was introduced. The social preview is a resized/cropped version of the pack’s existing background. Confirm you have rights to all artwork before public distribution.

Gallery example:

```json
"screenshots": [
  {
    "src": "assets/art/refuge.webp",
    "alt": "Describe the actual screenshot for someone who cannot see it",
    "caption": "The refuge, beyond the horizon"
  }
]
```

## Rules, releases and supporter packages

See [docs/EDITING.md](docs/EDITING.md) for complete data examples. Rules and policies are clearly labeled draft/unpublished until replaced. No prices, release dates, feature counts, or compliance claims have been invented.

## Future server status

Set `status.endpoint` to an **absolute HTTPS** endpoint you control or a trusted provider adapter. It must return this JSON shape:

```json
{
  "online": true,
  "players": { "online": 3, "max": 25 },
  "version": "YOUR_CONFIRMED_VERSION",
  "ping": 48
}
```

The values above illustrate a contract; they are never shipped as displayed status. `online` must be a boolean. Other values may be omitted. Ping is **provider-reported milliseconds**, not a browser-to-Minecraft measurement. A failed request displays *Status unavailable*, not *Offline*. No endpoint means no status request, no timer and no simulated statistics.

For a different provider schema, normalize it through an endpoint adapter before connecting it. Cross-origin endpoints must permit the website’s origin via CORS. No secrets belong in this client-side config. An API requiring authentication needs a separate server-side Worker proxy. The generated CSP permits the configured endpoint's origin; rebuild after changing it. Refresh defaults to 60 seconds, pauses requests when the tab is hidden, and uses a six-second timeout.

## Design and accessibility

Shared CSS variables control the palette, spacing and type. Responsive layouts adapt at 1100, 900 and 600 pixels; feature grids become one column on phones. Native disclosure controls, semantic headings, landmarks, visible focus states, keyboard menu closing, live clipboard feedback, reduced-motion support, and manual copy fallback are included. Full page content and links work without JavaScript.

No cookies, accounts, local storage, analytics, or card collection are implemented. `_headers` supplies CSP, framing restrictions and privacy-related browser permissions for Cloudflare Static Assets. Final operator policies and payment terms still need to be published.

## Validation

`npm run check` checks all pages for missing local links/assets/anchors, SEO tags, image alt text, duplicate IDs, JavaScript syntax, and key responsive/accessibility CSS. It exercises copy success/fallback, keyboard menu behavior, and online/offline/invalid/error status responses in a controlled JavaScript harness.

See [docs/VALIDATION.md](docs/VALIDATION.md) for what was verified and the remaining browser QA limits. These checks are not a substitute for testing the deployed site in your own browsers.

## Add a page

1. Add a `page('/new-page/', title, description, content)` declaration in `scripts/build.mjs`.
2. Use existing `head`, `external`, `address`, `faq`, `.container`, `.section`, `.panel` and `.prose` components.
3. Add its route to `navItems` if it belongs in the main navigation.
4. Build and check. Page metadata, shared navigation/footer and sitemap are generated automatically.

Automatic Cloudflare deployment enabled.

## Automatically populated mod list

The Modpack page renders `data/mods.json` at build time, so its complete list and CurseForge links also work without JavaScript and appear in the Markdown representation. JavaScript adds local filtering by project name, creator and type.

`.github/workflows/sync-modlist.yml` checks for a new stable NeoForge client release every six hours, or on a manual workflow dispatch. It discovers releases through the free [CFWidget metadata API](https://cfwidget.com/#api), then reads `manifest.json`, `modlist.html`, and bundled mod metadata from the official ForgeCDN client ZIP. No CurseForge API key or running Minecraft server is required. Libraries, data packs and resource packs are included and labeled. Server-only additions and unreleased local pack edits are excluded.

The importer validates project identity, archive size, Minecraft version, stable release type, manifest counts and project-link hosts. Unchanged releases do not download the ZIP again. An unavailable source fails the workflow and preserves the last verified list; a cached feed behind an already verified release retains that newer release. The list, latest-version labels and release link are synchronized together, built, checked and committed with their generated pages to trigger the normal Cloudflare deployment. The existing authenticated `sync:curseforge` importer remains available for the broader release-notes/gallery synchronization.

Run `python scripts/sync-modlist.py`, then `npm run build` and `npm run check`, to refresh manually. Python 3.11 or newer is required. A matching official ZIP already downloaded can be supplied with `--archive PATH`.

CFWidget can lag behind a newly published CurseForge file. After verifying a newer stable client file on the official project page, an operator can import it immediately using `--file-id ID --file-name 'EXACT ZIP NAME'`. The archive size and modification date are checked against the official ForgeCDN response. Future automatic checks retain that verified file until the feed catches up; they never downgrade it.

## Hosted checkout foundations

Official CurseForge and Discord links are configured. See docs/PAYMENTS.md for Tebex setup, RCON delivery, separate paid/earned entitlements, and activating reviewed packages. Checkout is closed until those requirements are met. No store account or server delivery has been configured by this website build.

For Cloudflare dashboard upload, run `npm run package:upload` (Python 3 required), then upload the generated Mythforge-Direct-Upload.zip. It contains only built static assets with index.html at the root.
