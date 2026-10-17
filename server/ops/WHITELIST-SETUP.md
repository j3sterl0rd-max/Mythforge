# Website whitelist application

The new page is `/apply/`. It uses the existing `/contact/` page for support and requires no Discord or website account. The website remains marked “Server coming soon!”.

The public page and API gateway are implemented. Processing stays disabled until the private backend and real Minecraft delivery are configured and tested. It must never claim approval merely because a request was accepted.

## What the owner needs to do next

1. **Later, from your computer:** open PowerShell and run `npx wrangler login`. Sign into the Cloudflare account that owns mythforge.win and approve the official Wrangler authorization. Tell Codex when it succeeds. This can wait until you are off your phone. Never paste an API token or password into chat.
2. Check Cloudflare Email Routing's verified destination inbox behind **admin@mythforge.win**. The preferred alert method uses a send-email binding restricted to that already verified inbox. Cloudflare documents verified-destination sends as free on all plans; arbitrary-recipient outbound sending requires Workers Paid. No paid plan or new email-service account has been enabled. If that free binding is unavailable for your setup, keep alerts queued until a free alternative is configured. See [Cloudflare pricing](https://developers.cloudflare.com/email-service/platform/pricing/).
3. Open the **private** deployment instructions on the server PC at `ops/private/whitelist/README.md`. Keep that directory and the original specification out of public repositories and client packs. It records the remaining integrations and exact activation tests.
4. After the backend is deployed, run the server's normal launcher and the private connector. It uses outbound HTTPS and a localhost-only whitelist bridge. **Do not open RCON or port 18766 on the router.**
5. Complete the live application test before enabling public submissions. A server-confirmed whitelist entry is required for the success message.

The private system has not been deployed or certified end to end. Local verification includes frontend display/timeout tests, private SQLite-backed workflow tests and actual-server bridge tests. See the local evidence records for exact counts. Domain credentials, review records and private implementation are deliberately absent from this public-safe guide.
