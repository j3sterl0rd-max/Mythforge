# Status and policies

## Live status

The static Server page calls https://api.mcstatus.io/v2/status/java/play.mythforge.win?query=false . The response supplies online/offline, player totals and retrieval time. CORS access was verified; a real query returned online and 2/10 during implementation. Counts are never hard-coded. No RCON, secrets, extra Minecraft mod, or UDP query setup is required. The existing Minecraft TCP port must remain publicly reachable.

The provider does not supply game latency in this response, so the empty ping field was removed. HTTP response duration is not Minecraft ping. Provider errors display status unavailable rather than claiming the server is offline. Requests refresh every minute while the page is visible. See https://mcstatus.io/docs for caching and service behavior.

Pack version follows latestVersion under serverTracksLatest. A normal Minecraft status version is the game/protocol version (1.21.1), not the pack version. Successful CurseForge sync updates both latestVersion and currentVersion.

## Original rules

General rules were adapted in original wording after reviewing two networks hosting Prominence II:
- https://www.apollonetwork.org/rules
- https://www.greatbeyond.org/rules

Keep legitimate modpack abilities distinct from unauthorized cheats. No network-specific XP-farm ban, claim command, fixed farm size, or punishment duration was imported. Abandoned-base decisions are staff-managed; no invented inactivity deadline is posted.

## Starter policies

Terms, privacy and refund pages now contain original plain-language starter policies for the current non-selling site and server. They are not a legal review or assurance of compliance. Checkout remains disabled and policyReviewComplete remains false. Tebex’s actual regional checkout terms and package terms must be checked before launch. Do not promise “no refunds under any circumstances” or copy another server’s legal terms.

Before paid launch, confirm the operator’s legal/business identity and a durable support email; actual host access and logging; log/backup retention and deletion handling; intended audience and how under-13 cases are handled; geographic obligations; and the chosen provider’s terms. The current pages use Procyonine and Discord as the available contact, without inventing a company or email. A 13+ sentence alone would not settle child-privacy obligations.

Sources:
- https://checkout.tebex.io/terms
- https://www.tebex.io/support-customer
- https://www.ftc.gov/business-guidance/resources/complying-coppa-frequently-asked-questions

Ordinary pack releases require no legal-page update unless data handling, service terms or purchases change.
