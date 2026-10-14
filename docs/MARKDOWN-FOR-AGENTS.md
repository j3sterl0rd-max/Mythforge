# Markdown content negotiation

The existing `website` Worker serves built page content as Markdown when explicitly requested with `Accept: text/markdown`. Browsers receive HTML by default. Markdown is generated from each finished page's main content during `npm run build`, using Turndown; navigation, scripts, forms, and footer chrome are removed. It stays synchronized with site and CurseForge updates.

`worker.mjs` negotiates the response; `wrangler.jsonc` binds the static files as `ASSETS` and runs the Worker before asset serving. The current Cloudflare deploy command can remain `npx wrangler deploy --assets ./dist --name website --compatibility-date 2026-10-13`: Wrangler reads the new configuration and entry point. The configuration's compatibility date applies when no CLI override is supplied.

Both page variants include `Vary: Accept`. Markdown receives `Content-Type: text/markdown; charset=utf-8` and retains security headers, but drops HTML validators, range/encoding headers, and content lengths. HEAD requests return the same representation type without a body. Static images and other assets pass through unchanged. Missing pages retain HTTP 404. Quality values are honored; explicit HTML preference wins over a lower Markdown quality. No exact tokenizer is available, so an invented `x-markdown-tokens` count is not emitted.

Cloudflare's zone-level Markdown for Agents feature is optional and is not required for this implementation. It requires an eligible paid zone plan. This Worker does not introduce a content-signal policy or change the site's AI-training permissions.

Validation:

```sh
npm run build
npm run check
curl -i https://mythforge.win/ -H "Accept: text/markdown"
curl -i https://mythforge.win/ -H "Accept: text/html"
```

The linked skill also recommends POSTing `{"url":"https://mythforge.win"}` to `https://isitagentready.com/api/scan` and inspecting `checks.contentAccessibility.markdownNegotiation.status` after deployment.

The static-only direct-upload ZIP still contains only static files. Uploading that ZIP without the Worker will serve HTML and directly accessible Markdown files, but cannot perform header-based content negotiation. Deploy through the connected Git repository to retain negotiation.

References:

- https://isitagentready.com/.well-known/agent-skills/markdown-negotiation/SKILL.md
- https://developers.cloudflare.com/fundamentals/reference/markdown-for-agents/
- https://developers.cloudflare.com/workers/static-assets/routing/worker-script/
