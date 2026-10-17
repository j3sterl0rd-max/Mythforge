# Website whitelist application

The new page is `/apply/`. It uses the existing `/contact/` page for support and requires no Discord or website account. The website remains marked “Server coming soon!”.

The public page, API gateway and private Cloudflare backend are deployed. The database and bot protection are provisioned, and the Windows connector is installed. Processing stays disabled until the remaining external integrations and real Minecraft delivery are tested. It must never claim approval merely because a request was accepted.

## What the owner needs to do next

1. **Completed October 17:** Cloudflare CLI login, private backend/database/bot-protection deployment and website service binding. Never paste an API token or password into chat.
2. Check Cloudflare Email Routing's verified destination inbox behind **admin@mythforge.win**. The preferred alert method uses a send-email binding restricted to that already verified inbox. Cloudflare documents verified-destination sends as free on all plans; arbitrary-recipient outbound sending requires Workers Paid. No paid plan or new email-service account has been enabled. If that free binding is unavailable for your setup, keep alerts queued until a free alternative is configured. See [Cloudflare pricing](https://developers.cloudflare.com/email-service/platform/pricing/).
3. Open the **private** deployment instructions on the server PC at `ops/private/whitelist/README.md`. Keep that directory and the original specification out of public repositories and client packs. It records the remaining integrations and exact activation tests.
4. After the backend is deployed, run the server's normal launcher and the private connector. It uses outbound HTTPS and a localhost-only whitelist bridge. **Do not open RCON or port 18766 on the router.**
5. Complete the live application test before enabling public submissions. A server-confirmed whitelist entry is required for the success message.

The private system has not been deployed or certified end to end. Local verification includes frontend display/timeout tests, private SQLite-backed workflow tests and actual-server bridge tests. See the local evidence records for exact counts. Domain credentials, review records and private implementation are deliberately absent from this public-safe guide.
