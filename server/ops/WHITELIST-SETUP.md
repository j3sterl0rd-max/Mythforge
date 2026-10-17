# Website whitelist application

The new page is `/apply/`. It uses the existing `/contact/` page for support and requires no Discord or website account. The website remains marked “Server coming soon!”.

The public page, API gateway and private Cloudflare backend are live. The database, bot protection, email notifications and Windows connector are configured. A real browser application passed automatic screening and received approval only after Minecraft saved and confirmed its whitelist entry.

Submissions are currently paused at the owner's request while the launch copy is stopped. The original live test server remains available. Start the launch copy and re-enable the private backend's `ENABLED` setting when applications should resume.

## Operation

1. **Completed October 17:** Cloudflare CLI login, private backend/database/bot-protection deployment and website service binding. Never paste an API token or password into chat.
2. **Completed October 17:** The owner received both the direct email test and the queued application-system notification at the existing verified destination behind **admin@mythforge.win**. No paid plan or new email-service account was enabled.
3. Keep the **private** deployment instructions, records and original specification out of public repositories and client packs. Current evidence and service status are in `ops/private/whitelist/DEPLOYMENT-STATUS.md` on the server PC.
4. After the backend is deployed, run the server's normal launcher and the private connector. It uses outbound HTTPS and a localhost-only whitelist bridge. **Do not open RCON or port 18766 on the router.**
5. Applications update the launch copy's whitelist. The original test server is separate. Keep the launch copy and connector running for timely approvals; restarting the original test server alone does not start the launch-copy bridge. During the two-server transition, the launch copy uses a temporary 5 GiB heap to limit combined memory use.

Verification includes a real production browser submission with Turnstile, automatic screening without a staff override, persisted Minecraft delivery, 68 private SQLite-backed workflow checks, and frontend display/timeout tests. Mojang identity resolution runs through the authenticated local connector because the cloud-hosted identity requests were blocked. Domain credentials, review records and private implementation are deliberately absent from this public-safe guide.
