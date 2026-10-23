# Automatic release history

The existing scheduled workflow refreshes published mod lists and changelog records every six hours. Older records and notes are preserved. Run it manually from GitHub Actions > Refresh published mods and changelog.

For full official note text, add a free CurseForge Core API key as the GitHub Actions repository secret `CURSEFORGE_API_KEY`. Obtain it via https://console.curseforge.com/. Without a key, public release metadata still updates and entries link to the official file notes. CFWidget does not supply per-file changelogs. Missing upstream notes are never fabricated or used to erase archived notes.

The workflow builds and checks the site before committing. To guarantee deployment of scheduled bot commits, add a restricted Cloudflare Workers deployment API token as the GitHub repository secret `CLOUDFLARE_API_TOKEN`. Use Cloudflare's Edit Cloudflare Workers token template, restricted to the Mythforge account/zone. The workflow already supplies the public account ID and deploys only the configured `website` Worker. Never use your Cloudflare global API key.

Alternatively, verify that Cloudflare Workers Builds is connected to this repository's main branch and deploys commits made by GitHub Actions. The current site was deployed and verified with the existing local Cloudflare OAuth login. No GitHub repository secrets were configured at the time of this update.
