# Mythforge launch operations

This copy is launch staging. The existing live test server is a separate Java process on 192.168.1.25:25565. Do not terminate that process while testing this copy. The public website deliberately says **Server coming soon!**. Purchases remain disabled.

## Implemented

- Fresh world and random seed; copied worlds, player files, operator lists, bans, caches and old logs were reset. Preserve the new world: it contains the new pregeneration and Refuge protection.
- NeoForge 21.1.253 / Minecraft 1.21.1. FTB Ranks and a server-only Mythforge module implement the accepted current rank/reward specifications, safe RTP, public warps, active-time accounting, durable reward mail, transaction receipts and refund downgrade logic.
- Ember $8, Artisan $16, Runeforged $32, Mythic $64 USD, permanent and cumulative. One-time item grants do not repeat after a refund or downgrade. Earned allowances survive paid-rank removal. Homes and public warps now add the highest current purchased rank bonus to natural earned allowances. Purchased home bonuses are 2/3/4/5; public warp bonuses are 1/2/3/5. They do not stack across purchased tiers. Claims start at 64 and force-load allowance at 2; +8 claims/10 active hours and +1 load/50 active hours continue indefinitely. Paid bonuses use the highest current paid tier once. Team allowances use the largest member; force loads require an online team member.
- 56 vote-crate outcomes totaling 10,000 weight; registry-validated item pools, 20 playtime milestones, paid kits, relic upgrades, and pending inventory delivery. Crate rewards use actual installed registry IDs, including `gachamachine:gacha_token`.
- Fiw AntiCheat with cheat-client, autoclicker and schematic-printer detection. Anti-xray, fullbright and ordinary pack utilities are excluded. Neruina contains ticking faults; GriefLogger records local actions, GLRA's web endpoint is disabled. Hourly AdvancedBackups retain up to 14 days / 168 backups / 50 GB, whichever limit applies.
- Overworld 4,225 processed chunks, Nether 1,089, End 9,409, total 14,723. RTP also generates destinations outside that area asynchronously.
- Sable/Rapier reentrancy mitigation addresses an observed chunk-generation deadlock. This is a local compatibility patch, not a guarantee against all Sable faults.

## Start this copy safely

Use `ops/start-staging.ps1`. It checks this copy's bind address and port, then starts Java 21 using the configured 6-12 GB heap. The Minecraft listener is **127.0.0.1:25566**, the contraption graph workshop is **127.0.0.1:18765**, and Votifier is **127.0.0.1:8193**. RCON and query are disabled. Whitelist is enabled and starts empty. In the console, run `whitelist add YOUR_JAVA_NAME`, then connect locally to `127.0.0.1:25566`.

Never run two processes against the same world. Stop with the console command `stop`; wait until Java exits before changing JARs or world configuration. The server watchdog now allows 120 seconds for a tick before creating crash diagnostics.

## Client preparation and mandatory acceptance testing

Fiw AntiCheat is by Fi3w0: https://modrinth.com/mod/fiw-anticheat . Preserve its attribution/license when distributing the companion in a pack. Its author describes it as a mod verification layer; a malicious custom client can lie.

Install the current Mythforge client pack and add `client-addons/fiw-anticheat-neoforge-1.21.1-2.1.0.jar` to that instance's mods directory. This companion is required; the original published pack does not include it. Missing companion clients are disconnected after 30 seconds. Publish a new pack release including it, or give testers the official companion download instructions before inviting them. Do not distribute server-only Mythforge, Tebex, logger or backup JARs to clients.

Use two actual Java accounts, both whitelisted, plus console commands. No human client session has been driven by this setup work. Runtime registry/permission/ledger tests are useful but do not replace the following:

1. Run `mythforge audit` and `mythforge selftest` in the console. Inspect `ops/evidence/reward-audit.json` and `selftest.json`. The self-test uses an isolated fake player and restores real ledger state; run only while staging has no players.
2. Test a default non-OP player: `/give`, `/op`, quest editing, forced team membership, server configuration, other people's ender chests and administrative claim commands must fail. Verify ordinary quests, claims, parties, `/back`, homes and inventory commands work. Test staff roles separately; avoid OP for routine staff.
3. Grant a disposable staging entitlement with `mythforge purchase UUID artisan staging_artisan`, then verify `/rtp nether`; default and Ember must be denied. Grant Runeforged using transaction `staging_runeforged` and verify `/rtp end`. Test movement cancellation, combat cancellation, dangerous ground, occupied claims, portals and Nether roof exclusion. Revoke both transactions afterward.
4. Create a public warp inside the owner's claim. A second account must visit while the owner is offline. Delete/reassign the claim and ensure consent is rejected. Verify safe JourneyMap waypoints require previously visited chunks. FTB Essentials `/back`, homes and TPA retain their own destination logic; separately test those mod interactions, especially cross-dimension travel and claim entry.
5. Check every milestone using the console-only `mythforge settime PLAYER HOURS` test helper, then reset the disposable player's time. Fill the inventory to check `/mail`, relog, restart, and confirm grants do not repeat. Runtime slot IDs `hands`, `necklace` and `trinket` were validated; check their actual extra capacities in the client UI and equipped Relics leveling; slot naming and pack compatibility require visual verification.
6. Remain idle, ride a repeating movement loop, hold a repeated click action, then genuinely explore/build. Idle/repeated activity must stop earning time and non-OP inactivity must disconnect after 15 minutes. Sophisticated varied macros cannot be perfectly distinguished from human input; use staff review and anticheat reports.
7. Before 30 hours, attempt Create shafts/belts/machines, Sable assemblies, deployer placement and schematic/contraption imports. Repeat after unlocking. Event-based restrictions cannot guarantee coverage of every mod's direct Java writes or custom packets. Schematicannons stay banned regardless of rank. Test claim protection against fluids, fake players and contraptions as well.
8. Test claim-limited flight, timed flight expiry, combat, logout/restart, and legitimate pack flight abilities. Test heal near bosses, XP repair with enchantments/components, colored rename (`&a` etc.), and personal time/weather versus world controls. Nearby living entities with at least 150 max health conservatively block heal within 32 blocks; tune after boss testing.
9. Repeat purchase command with the same transaction, upgrade, revoke the higher rank, restart, then check lower paid rank and earned progression remain. Test inventories overflowing paid kits, all allowed relic voucher choices and a refunded transaction replay. Never use a real payment for these console tests.
10. The inherited pack still emits numerous server-side class and loot/recipe parse warnings during successful startup (602 ERROR-labelled lines on the final boot). These are outside the new reward registry audit and need pack-maintainer review with real client play; do not treat a successful boot as a clean modpack certification. Use `spark profiler start --timeout 120` during client play and chunk generation. Save findings, inspect Neruina incidents and logger retention, and only open public access after the pack behaves correctly.

## Tebex

The published storefront is https://mythforge.tebex.store/ . Public responses currently say store unavailable because review has not been completed. Four packages are configured and disabled: Ember 7739191, Artisan 7739196, Runeforged 7739207, Mythic 7739216. Category 3473736 is Permanent Supporter Ranks. Cumulative pricing and suppression of cheaper already-surpassed packages were saved and verified.

The official NeoForge Tebex plugin is installed. Its private local configuration is `mods/tebex/config.yml`. `tebex info` successfully identifies Mythforge and USD. Do not copy secrets to Git, screenshots, chat, the website or a support ticket. The originally supplied general API key did not authenticate the game server; the existing Game Servers secret is the one used here. Only the official plugin polls the command queue.

For each rank, verify these commands on server Mythforge, offline execution enabled, zero required slots, zero delay:

| Event | Command |
| --- | --- |
| Purchase | `mythforge purchase {id} ember {transaction}_ember` |
| Expiry/removal | `mythforge revoke {transaction}_ember` |
| Refund | `mythforge revoke {transaction}_ember` |
| Chargeback | `mythforge revoke {transaction}_ember` |

Replace `ember` consistently with the other rank IDs. `{id}` is the Java UUID, not the username. Do not change transaction suffixes after sales begin. For a cumulative upgrade, only newly acquired tier rewards are delivered. Revocation removes paid privileges while leaving earned time and delivered items recorded; consumable rewards already used are not clawed back.

Finish owner review: verify the support contact, business/store details and required product-policy compliance in Tebex. Review the category and descriptions, enable packages only for the controlled Tebex test workflow, and use Tebex's own zero-charge test/manual-payment workflow where offered. Verify an actual command arrives, delivers, survives restart and revokes on refund. Then submit the completed storefront for review. Identity, wallet verification, legal acceptance and payout details require the account owner. Do not buy Plus, enable a paid subscription, add a card or perform a paid transaction for setup. This build used free setup features; an existing Plus trial was visible, so verify its terms and expiration in your account before relying on trial-only options.

The public website no longer publishes playtime schedules or vote-crate tables. Supporter rewards describe gameplay alternatives provided by the pack; some named convenience commands remain rank-only in this server module. Review that distinction when answering players.

Leave website `commerce.enabled` and `deliveryVerified` false until the real Tebex queue roundtrip and owner review are complete. The website store buttons remain closed. When ready, set verified package URLs, allowed store hostname, enabled flags and policy review status in `website/data/site.json`; build/check before publishing. Do not advertise payment availability before Tebex approves the store.

## Voting sites and network delivery

Create owner accounts and register the final public Java server on Minecraft-MP, MinecraftServers.org, Planet Minecraft, Minecraft-Server-List, TopG and TopModdedServers. Registration requires the final server address and usually external reachability/ownership verification, so listings were not fabricated while staging is localhost-only.

For each listing, use the final public host and Votifier TCP port 8193. Prefer Votifier v2 with the existing token from `config/votifier/config.json`; for v1, provide only `config/votifier/public.pem`. Never upload `private.pem`. The bridge runs `mythforge vote {player}` and stores offline votes until login. Open/forward TCP 8193 only during the production cutover; localhost staging cannot receive internet votes. Do not change the keys after sites are configured.

At cutover change Votifier host to your server's intended listening interface, add a narrow Windows Firewall TCP 8193 rule, and forward that port in the router if hosting at home. Test each site's test-vote button, including an offline username and inventory overflow. Confirm one key, open `/crate`, relog/restart and ensure consumption persists. Publish verified voting links in Discord and update the in-game voting help when ready. The public website does not list voting pages or crate rewards. Staff can add a disposable key using console `mythforge vote ONLINE_PLAYER` for local crate testing; remove staging-only rewards afterward.

## Discord roles and moderation

Game ranks are configured; Discord roles require your Discord server and verified Minecraft-account linking. Create Ember/Artisan/Runeforged/Mythic roles, configure a trusted account-linking bot/integration with minimum role-management permissions, and place its role above only those four roles. Verify ownership of the Minecraft UUID before granting a role. Until linking exists, manually grant/revoke roles using Tebex receipts and the verified game UUID. Never give rank buyers moderator permissions. Remove roles on refund/chargeback and reconcile after bot downtime. No Discord credentials or bot tokens have been created or published here.

Assign staff in the console with FTB Ranks' documented `ftbranks add NAME RANK` syntax (use tab completion/help to confirm installed syntax). Rank IDs: `junior_mod`, `moderator`, `admin`; owner is the existing OP condition. Junior moderators kick/mute, moderators additionally ban/pardon, admins get inspection/performance/logging tools. Review the generated `world/serverconfig/ftbranks/README.txt` command inventory before changing grants. `mythforge` management commands are console-only even for OP players.

## Backups, incidents and recovery

Manual backup retesting passed after enabling save flushing and excluding transient `.neoforge-tmp` / `.tmp` files. Hourly backups use server uptime and require activity; empty-server intervals may be skipped. World backups include the Mythforge ledger and per-world ranks. They do not cover every root-level mod configuration, SQLite database or private Tebex/Votifier key. Run `ops/backup-settings.ps1` with the server stopped to create a separate private settings archive. Keep world backups plus settings together and copy them off this computer to storage you control. Local copies cannot protect against disk failure. Do not upload raw archives to a public GitHub repository.

Before launch, create a manual world backup using AdvancedBackups' console help, stop this copy and restore into a separate test directory using its differential-chain restore utility. Preserve the full chain and manifest; do not extract only the latest differential ZIP. Bind the restored test to another localhost port, verify seed/chunks, Refuge claims, inventories, ledger and ranks, and compare file counts. Never restore into a running world or over the original live test server. An offline full ZIP was extracted and all 434 files matched SHA-256. A restored-server boot and differential-chain restore remain to be tested.

Neruina logs identify suspended ticking entities/block entities. Automatic deletion is disabled. Preserve incident diagnostics, investigate the mod/item and location, repair on an offline copy, then resume/remove only the affected object after a backup. GriefLogger's database and AdvancedBackups settings are local. GLRA's HTTP service is disabled to avoid exposing world/player data. Review disk usage weekly and purge logs through a deliberate retention policy after backing up required evidence.

## Production cutover

1. Complete the client acceptance tests, vote listings, Tebex test/review, Discord linking and restore drill. Build a spawn later as requested. Make an offline full backup of this fresh generated world and settings.
2. Announce maintenance, then stop the original live test server normally. Confirm its port is released. Do not run the copied server on the same address while that original process is alive.
3. Set this copy's `server-ip` to the intended host interface (blank binds all interfaces) and `server-port=25565`. Keep online-mode true and RCON disabled. Retain whitelist until controlled testers confirm the switch. Review player capacity and memory with actual profiles before raising max-players beyond 10; Artisan+ has two reserved overflow slots, not an external queue.
4. Configure router/firewall for TCP 25565 and vote port 8193; leave the contraption graph workshop on localhost. Set DNS A/AAAA correctly, or a Minecraft SRV record for the desired subdomain. Point DNS only to a reachable address you control. Do not publish this staging localhost address.
5. Start using the Java arguments, verify Minecraft status and a real remote client, verify backups/votes/Tebex, then disable whitelist only when ready. Keep administrative services private. Enable purchase links only after delivery verification and store approval. Explicitly request a website status change when you want to replace “Server coming soon!”.
6. Retain a stopped rollback copy. If there is a serious fault, stop this launch server, preserve logs and ledger, then restore the known-good backup. Reconcile any Tebex deliveries after restore so an older transaction ledger cannot duplicate grants.

## Build and source

`server-tools/gradlew.bat build --no-daemon` builds the custom server module with Java 21 and the installed compile-only dependency JARs in the adjacent `mods` folder. Stop staging, copy `build/libs/mythforge-server-tools-1.0.0.jar` to `mods`, restart, and run audit/selftest. Do not run `ops/reset-copied-data.ps1` again: it would erase the newly generated world. `ops/prepare-launch.mjs` recreates base ranks and reward configuration; it overwrites per-world rank files and must not run on a production world containing earned ranks.

Website: `npm ci`, `npm run build`, `npm run check` in `website`, then commit/push tested source and generated outputs. Credential-free server source/config snapshots are kept under the website repository's `server` directory. That snapshot excludes the live world, private configuration, databases, logs, original pack ZIP, mod binaries and payment/vote secrets.
