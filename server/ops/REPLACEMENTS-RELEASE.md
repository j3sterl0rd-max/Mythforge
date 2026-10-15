# Activated Mythforge services

Release date: 2026-10-14. Minecraft 1.21.1, NeoForge 21.1.253.

## Installed and active

- SimpleAFK 1.5.2: `/afk`, AFK name/tab presentation and sleep exclusion. Five idle minutes marks AFK. Mythforge's active-time adapter owns the active-time 15-minute kick policy and invokes SimpleAFK's kick implementation. SimpleAFK's independent kick timer is disabled; vanilla's 15-minute idle timeout remains a fallback. A compatibility shim skips its invalid config read during NeoForge config unloading, fixing an observed shutdown exception. Manual AFK pauses earnings. Repeated-action/movement filtering remains because the published status mod alone does not reject reward-farming macros.
- AStages 2.5.3-1.21.1: 414 functional machinery item restrictions generated from the installed registries and configured additional blocks. Placement and use are locked; pickup, mining, inventory/container storage and recipe visibility remain allowed. The adapter synchronizes the machinery stage from 30 active hours or the eligible supporter entitlement and removes only unearned access after revocation. Decorative blocks are not blanket-restricted. Automated fake actors delegate to the target claim owner's earned/paid eligibility rather than acquiring a persistent stage for a shared fake UUID. Existing guards retain banned machinery and pack-specific command checks.
- FTB Quests 2101.1.36: its published `RewardTable.generateWeightedRandomRewards` selects the 56 crate outcomes with the configured total weight of 10,000. The private runtime table is not a team-completion quest. Keys, complex Relics items and overflow mail remain per-player adapters. No new crate client mod is needed.
- FTB Essentials 2101.1.10: its installed destination-search engine now serves `/rtp`, `/rtp nether` and `/rtp end`. A pinned Mixin invoker supplies the missing cross-dimension entry point. Mythforge checks combat, dimension rank, claims, headroom, fluids, dangerous blocks and Nether roof, then applies the five-second movement-cancellable warmup and checks safety again. No extra teleport suite was installed. Nether RTP is also earned at 300 active hours and End RTP at 400 hours; purchased ranks grant earlier access.
- FTB XMod Compat 21.1.12: connects FTB Ranks to actual FTB permission consumers. Consumer-level allowance tests are included.
- Official Tebex NeoForge 2.4.6: owns authentication, polling and command execution. Mythforge stores entitlements only. Refund-before-purchase now persists a tombstone so a delayed purchase cannot resurrect a revoked transaction.

An unadapted FTB RTP search triggered the copied server watchdog during new terrain generation. The installed adapter now prepares the sampled column and its surrounding chunks through Minecraft's asynchronous pipeline, then runs one native FTB attempt against that prepared terrain. It retries up to 32 prepared candidates and keeps native access on the server thread. Searches remain bounded to 1,500 blocks in Overworld/Nether and 2,500 in End. Consult `ops/evidence/rtp-provider-test.json` for measured search durations. Pregeneration remains intact.

## Tebex configuration

All four packages are enabled in the creator panel. Prices remain Ember $8, Artisan $16, Runeforged $32 and Mythic $64 USD. The saved cumulative category credits the previous tier: Ember to Artisan costs the $8 difference, before any applicable tax/currency effects. Do not change Artisan's base price to $8.

The store was submitted successfully and is pending Tebex review. Tebex displays up to two working days. Its disabled-account status blocks even the official manual-payment test; this was observed in the UI and the API. Therefore actual checkout/delivery/refund certification is pending account enablement, not complete. No money was spent.

After account enablement, use Payments -> Create a payment for a disposable, valid Minecraft test account. Issue Ember and confirm the official mod delivers its transaction, then Artisan and confirm highest-rank additive bonuses and only newly acquired kit items. Test revocation. Use a private Store Test Mode checkout to verify the actual $8 upgrade basket; switch Test Mode off immediately afterward. Do not make a real payment for testing. Remove test grants before allowing that account onto production. Follow the official [package testing instructions](https://docs.tebex.io/creators/tebex-control-panel/how-to-create-packages/how-to-test-a-package).

## Late earned utility access

Public-warp icons unlock at 300 active hours, alongside Nether RTP. `/pwarp` opens a read-only, paginated vanilla menu showing those actual icons; clicking visits through the normal protected command. Display items cannot be withdrawn. Warp-icon and relic-choice commands use Minecraft's namespaced resource-ID parser. End RTP unlocks at 400 hours. Reserved-capacity login unlocks at 500 hours and only overrides the server-full rejection; whitelist/authentication failures remain enforced. Repair remains at 500 hours. These schedules appear in-game, not on public website pages.

## Client release additions

Required client additions are AStages 2.5.3 and Fiw AntiCheat 2.1.0. Include FTB XMod Compat 21.1.12 for supported cross-mod integration. The exact downloaded AStages and Fiw files are in `client-addons`; publisher release metadata is in `ops/audit/activation-releases.json`. SimpleAFK, Tebex and Mythforge Server Tools are server-only.

Install these into the actual Mythforge 1.2.4 client instance before connecting to this copy. Launch with Java 21 and the matching NeoForge. The currently published pack lacks these additions. Publishing its replacement requires access to the pack publishing account. Do not claim client acceptance testing based on a fake-player test.

## Remaining external launch work

The original live server remains untouched. This copy stays on 127.0.0.1:25566 while that server occupies 25565. Actual public cutover requires stopping the original normally, selecting this copy as the host, and applying router/firewall/DNS settings. `LAUNCH-README.md` contains the concrete cutover, vote-listing, Discord account-linking and owner verification instructions.

Tebex review, real-client/second-account acceptance testing, public vote-site ownership callbacks, Discord role linking and pack publication are not certified by local registry tests. The pack also emits inherited recipe/loot compatibility errors, recorded in `ops/evidence/pack-warning-summary.json`; the server boots, but that does not prove every pack recipe works. These limits prevent an honest claim of 100% public launch readiness.
