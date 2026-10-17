# Mythforge public content API

Read published Mythforge website pages without scraping page layout. This read-only API needs no authentication. Send GET to a page URL with Accept: text/markdown for main content, or Accept: text/html for the default browser representation. HEAD returns headers without a body.

Example: curl -H "Accept: text/markdown" https://mythforge.win/modpack/

## Discovery

- Catalog: https://mythforge.win/.well-known/api-catalog
- OpenAPI: https://mythforge.win/api/openapi.json
- Project overview: https://mythforge.win/llms.txt

## Pages

- https://mythforge.win/apply/
- https://mythforge.win/beta-testers/
- https://mythforge.win/changelog/
- https://mythforge.win/checkout/
- https://mythforge.win/contact/
- https://mythforge.win/getting-started/
- https://mythforge.win/
- https://mythforge.win/links/
- https://mythforge.win/modpack/
- https://mythforge.win/privacy/
- https://mythforge.win/purchase-help/
- https://mythforge.win/refunds/
- https://mythforge.win/rules/
- https://mythforge.win/server/
- https://mythforge.win/support/
- https://mythforge.win/terms/

## Response behavior

Successful pages return HTTP 200. Missing pages return HTTP 404 and an HTML or Markdown error page according to Accept. Canonical page routes may redirect to a trailing slash. Vary: Accept separates cache representations. Static images and other files retain their usual media types. Markdown and HTML reflect the latest deployed website snapshot, rather than a live query to CurseForge or Minecraft.

The site publishes Content-Signal: ai-train=no, search=yes, ai-input=yes. Respect these preferences, cache responses where practical, and avoid excessive requests. There are no write operations, checkout endpoints, private player records, or game-server control endpoints. Contact admin@mythforge.win for questions.
