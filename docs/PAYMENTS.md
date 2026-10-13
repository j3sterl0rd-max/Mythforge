# Mythforge payment setup

The website is ready for hosted external checkout. It does not create a Tebex account, collect payment details, or deliver Minecraft commands. These steps complete the account and server side.

## Connect Tebex

1. Create a creator store at https://www.tebex.io/ for Minecraft Java Edition. Complete its operator, verification and payout setup. Keep the store unpublished while configuring it.
2. Use Tebex's current RCON Adapter for the NeoForge server: https://docs.tebex.io/creators/tebex-control-panel/game-servers/rcon-adapter . Official plugin lists do not establish NeoForge support; do not install a Bukkit or Fabric plugin in this pack.
3. Enable RCON in server.properties, choose a strong unique password, and restart the server. Run the adapter on the server machine and use 127.0.0.1 where possible. Restrict the RCON port to the adapter through the firewall; do not publish it as a website endpoint or expose it to players.
4. Follow the adapter's guided setup with the Tebex store secret and RCON connection. Keep every secret out of Git, site.json, static files and browser JavaScript.
5. Configure actual reward commands for the server's installed permission system. Verify command syntax on a test player before using it in Tebex. No guessed permission commands are supplied here.

## Separate paid and earned rewards

Use separate supporter and playtime entitlements. A paid grant, expiration, refund or chargeback must change only paid entitlements. It must never remove earned playtime permissions. /fly and AE2 kit ideas remain proposals; hours, contents, cooldowns and progression restrictions are not finalized. Cosmetic supporter packages can be prepared independently. Do not sell queue priority or publish an unimplemented lobby/fallback promise from earlier brainstorming.

Use repeat-safe permission grants rather than direct item-give commands. For any future kits, a server-side claim ledger should identify transaction, player UUID and reward; retries must not issue duplicate items. A status page, return URL or browser query is never proof of payment. The adapter transports commands; it is not a custom entitlement ledger.

Test paid grant, offline player delivery, reconnect/restart, repeated delivery, expiration, refund and chargeback. Verify that earned permissions survive revocation. Keep transaction logs and a staff support path. Do not activate untested rewards.

## Website activation

Edit data/site.json:

- links.store: the actual hosted HTTPS store URL.
- commerce.storeHosts: replace the example mythforge.tebex.io with your actual approved hostname. It is an allowlist, not an existing store.
- supporterPackages: edit names, contents, displayed prices, provider package URLs and stable unique IDs. Set active only for completed offers.
- policies.terms, policies.privacy, policies.refunds: publish operator-reviewed policies appropriate to your actual operation and provider terms. No legal policy is fabricated by this project.
- links.contact: currently Discord; set a dedicated contact if desired.
- commerce.deliveryVerified and commerce.policyReviewComplete: set true only after the relevant work is complete.
- commerce.enabled: true when opening the store.

Run npm run build and npm run check. The build refuses enabled commerce if policies, contact, delivery verification, approved store URLs or active package prices are missing. The flags record operator verification; they cannot perform it. /checkout/ explains the handoff and selects only configured packages. /purchase-help/ gives a private receipt-based support path.

## Publish an update

Run npm run package:upload to generate Mythforge-Direct-Upload.zip from dist/ only. Upload that ZIP as a new version of the existing Cloudflare Worker. Do not upload the source repository ZIP to the drag-and-drop uploader. Domain and Minecraft DNS records do not need changing.

## Sources

- https://docs.tebex.io/creators/tebex-control-panel/game-servers/rcon-adapter
- https://docs.tebex.io/creators/tebex-control-panel/game-servers/minecraft-java-edition

Provider fees, terms and account availability are determined by Tebex at setup; no fee promise is made here.