# Mythforge purchases and delivery

The live Minecraft address is play.mythforge.win. The public reward reference is https://mythforge.win/support/ and the provider storefront is https://mythforge.tebex.store.

## Installed integration

Production uses the official Tebex NeoForge 1.21.1 plugin, version 2.4.6. Its private configuration is mods/tebex/config.yml on the host; do not commit or distribute that file. The plugin authenticates successfully to the Mythforge Tebex account. No separate RCON adapter or custom payment poller is used. Public RCON is not required.

Four permanent one-time packages are configured: Ember ($8), Artisan ($16), Runeforged ($32), Mythic ($64), in a cumulative rank category. Eligible upgrades use the price difference, including Ember to Artisan for $8 before applicable taxes. Final totals and eligible credit are shown by Tebex.

The purchase command is `mythforge purchase {id} <rank> {transaction}_<rank>`. The removal, refund and chargeback commands are `mythforge revoke {transaction}_<rank>`. Those transaction keys are rank-specific, so a cumulative purchase containing multiple tiers cannot collide. The server accepts dashed and undashed Minecraft UUIDs and records repeat-safe entitlements. One-time items are claimed through /rewards. Offline entitlements reconcile on login. Paid reversals preserve earned progression; item grants are tracked to prevent replay or duplicate upgrade delivery.

## Provider review and verification

The package descriptions, category description and storefront banner have been rewritten for the live server. They explain quantities, commands, costs, cooldowns, travel/flight limits, additive allowances, free gameplay alternatives, permanent one-time billing and private support.

Tebex controls store approval and checkout availability. A catalog link is not evidence that payments are enabled or that a test transaction succeeded. The website can publish full package information and link to Tebex while the provider reviews the account. The legacy `commerce.deliveryVerified` and `commerce.policyReviewComplete` flags must not be set true merely to remove launch messaging; record actual verification before using them to enable any additional checkout functionality.

Once Tebex permits it, use its free test/manual payment facility to verify the official plugin roundtrip, offline delivery, repeat delivery, upgrade credit and revocation. Do not use a real-money payment as a test. If the account still refuses test payments, retain that limitation and wait for provider review rather than simulating success or installing another checkout system.

## Website source and publication

Package content lives in data/site.json and scripts/support-page.mjs. Changes are made in source and built into dist; npm run build and npm run check are required before push. Cloudflare deploys main automatically. Preserve the fixed beta roster, Markdown negotiation, Content Signals and API catalog.

Private server credentials, transaction ledgers and whitelist application data must never be copied into website files or public Git history.
