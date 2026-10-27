# CurseForge sync

Current setup: the six-hour `sync-modlist.yml` workflow is active, and `main` is connected to Cloudflare Workers Builds. The redesigned homepage also checks releases when visitors arrive. See [REDESIGN.md](REDESIGN.md) for the current behavior and caching; the original API-importer notes below describe the earlier setup.

Current snapshot: latest release 1.2.4, published October 13, 2026; RAM 9–10 GB; Java 21; Minecraft 1.21.1; NeoForge; verified Discord invitation; release summaries and gallery images.

Run `npm run sync:curseforge` with CURSEFORGE_API_KEY supplied privately in the environment, then `npm run package:upload`. Official API documentation: https://docs.curseforge.com/rest-api/ . Apply for an API key through CurseForge's developer access process. Do not place it in site.json or browser JavaScript.

The supplied GitHub workflow checks daily, fetches the official API, and builds an upload ZIP as an Actions artifact. Set the CURSEFORGE_API_KEY repository secret and enable Actions. The workflow is not active until the project is uploaded to GitHub. It does not deploy to your Cloudflare account: dashboard uploads still require uploading the resulting ZIP. To publish automatically, connect the repository to Cloudflare's build pipeline and configure the secret there, or configure a deployment workflow with your own Cloudflare credentials.

It synchronizes latest stable compatible pack release, release dates and notes, the unique Discord invitation in the project description, and explicitly stated RAM/Java requirements. Gallery images are downloaded from the project’s official ForgeCDN assets and bundled locally. Failed fetches stop the sync before config is written. Raw description HTML is not executed on the website.

The operator has declared that the server always follows the latest release. serverTracksLatest is enabled, so website builds and successful syncs use latestVersion for the server requirement. This is an operator policy, not a protocol-derived pack version. Its listing also cannot establish server status, player count, uptime, prices, policies, permissions, or playtime unlock thresholds.
