# Automatic release history

The existing scheduled workflow refreshes published mod lists and changelog records every six hours. Older records and notes are preserved. Run it manually from GitHub Actions > Refresh published mods and changelog.

For full official note text, add a free CurseForge Core API key as the GitHub Actions repository secret `CURSEFORGE_API_KEY`. Obtain it via https://console.curseforge.com/. Without a key, public release metadata still updates and entries link to the official file notes. CFWidget does not supply per-file changelogs. Missing upstream notes are never fabricated or used to erase archived notes.

The workflow builds and checks the site before committing. Cloudflare deploys the resulting main-branch update.
