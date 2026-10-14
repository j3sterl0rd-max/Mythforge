# Mythforge mod and integration audit

Date: 2026-10-14. Target: Minecraft 1.21.1, NeoForge 21.1.253. This is a source/configuration audit and dependency check. Candidate replacements have not been installed or certified by a real client play session. Gameplay compromises require an owner decision before migration.

## Main findings

1. Most foundational services already use published mods. The custom Mythforge module supplies progression and pack-specific restrictions, but also owns standard travel and convenience features. It should be reduced to integration code where an established implementation can preserve the required behavior.
2. A real integration omission was found: FTB XMod Compat was absent. Installed FTB Chunks reads numeric limits through FTB Library's permission provider. Its fallback provider returns defaults. Previous tests verified FTB Ranks values, not the consuming provider. XMod Compat 21.1.12 supplies the FTB Ranks provider and FTB integration hooks; its declared dependencies match the installed versions. It is now added to staging, with consumer-level checks added to the runtime audit. All 28 runtime checks passed, including the three new consuming-provider checks. Startup selected FTB Ranks as the active provider.
3. Tebex integration is official. `tebex-neoforge-1.21.1-2.4.6-0096441.jar` comes from [Tebex's v2.4.6 release](https://github.com/tebexio/Tebex-Minecraft/releases/tag/v2.4.6). The installed SHA-256 matches the saved release manifest. The official mod authenticates, polls commands and executes delivery. Our `Payments.java` only records rank entitlements, deduplicates transactions and handles revocation. It contains no HTTP client, payment collection or independent Tebex poller. The console connection was verified again during this audit; an actual Tebex test-payment/refund queue roundtrip is still required.
4. Published compatibility is not enough to justify a replacement. One compatible playtime mod counts every player tick, including idle time. Some RTP mods omit safeguards the server currently relies on. Replacing custom code with a weaker implementation would not meet the original requirements.

## Feature audit

| Feature | Current implementation | Existing alternative and recommendation | Compromise or remaining verification |
| --- | --- | --- | --- |
| Permissions, ranks, claims, teams | FTB Ranks, Chunks, Teams; custom code sets earned bonuses | Keep these existing mods; use XMod Compat as their integration bridge | Test consuming limits, not just configured nodes; real team limit/force-load behavior still needs client testing |
| Homes, back, TPA, nick, craft, stonecutter, anvil, smithing, disposal, hats | FTB Essentials, with progression-based permissions | Already uses established functionality; keep | Custom earned permissions remain a small integration layer |
| Nether/End RTP | Custom async search, safety, warmup, combat and claims guards | FTB Essentials already has RTP and warmups; investigate adapting it before adding another teleport suite | Installed native RTP searches the player's current dimension. A simple switch would lose cross-dimension `/rtp nether` and `/rtp end`; claim rejection needs its integration listener. Do not silently make that compromise |
| ExtraRTP candidate | Not installed | Verified NeoForge 1.21.1 release 1.5.0; source reviewed | The inspected service samples the world border, loads chunks synchronously before its async position work, and lacks the current claim/combat/warmup guards. Not a recommended drop-in replacement |
| Teleport Commands candidate | Not installed | NeoForge 1.21.1 version 1.3.5 exists; established teleport suite | Duplicates existing homes/back/TPA. FTB integration, per-dimension paid gates and public-warp ownership need prototype testing. Adding an entire second suite may increase maintenance |
| Player-owned public warps | Custom `/pwarp`, owner consent and safe destinations | FTB Essentials has admin-owned global warps, not this exact player-owned model | Replacing with global warps would lose player ownership/limits/icons; retain until owner chooses a simpler model or a compatible provider is proven |
| Active time and repeating allowances | Custom active-time ledger | FTB Ranks already has playtime conditions, but its installed condition uses Minecraft statistics | Vanilla/statistical playtime includes idle online time. Native conditions would weaken anti-AFK progression without an active-time adapter |
| Playtime Rewards candidate | Not installed | NeoForge 1.21.1 version 1.0.2 exists | Inspected `PlayTimeEvents` increments on every player tick; no AFK filtering in that path. Milestones are command-based. It does not replace the current activity clock, overflow mail or one-time delivery journal by itself. Not recommended as a drop-in |
| AFK status/kicking | Custom repeated-action/activity heuristics plus vanilla idle timeout | SimpleAFK 1.5.2 has a NeoForge 1.21.1 release and standard status/kick behavior | It is a good candidate for ordinary AFK status, but movement-based detection is not a complete anti-macro reward clock. Retain a thin activity filter if adopted. Requires avoiding two independent kick policies |
| Vote receipt and offline queue | Modded Votifier | Keep official protocol-compatible existing mod | Existing local v1/v2 and bad-signature tests passed. Site callbacks and login delivery still need real-account tests; receipt removal before reward persistence deserves crash testing |
| Vote crate selection/delivery | Custom weighted table, virtual keys and mailbox | FTB Quests already contains weighted/random/item/command rewards. RarityChests publishes NeoForge 1.21.1 physical chests/keys | Quest-backed delivery changes the UI/workflow and must not turn a per-player key into a shared-team reward; physical crate mods require a client-pack addition. Complex Relics/consumable rewards still need adapters and exact-weight tests. Keep current crate until one option is chosen and prototyped |
| Modular Loot Crates and Bundles | Not installed | Useful public project but no matching NeoForge 1.21.1 release returned by publisher API | Excluded for this loader/version; do not use Fabric builds through a compatibility layer merely to get crates |
| Machinery progression | Custom placement/interact/item guards | AStages 2.5.3-1.21.1 is a strong staging candidate; its API can allow pickup, inventory storage and mining separately | Requires both client and server. Must preserve crafting/trading/mining before unlock and avoid hiding decorative blocks. Sable assembly packets, contraption imports and automated writes still require specific integration testing |
| Alternative stage engine | Not installed | ProgressiveStages 3.0.6 has a matching release | Broad progression system with required client/server installation. No need to add two stage engines. Evaluate only if AStages fails the needed restrictions |
| Relic leveling, maxed items, equipment slots | Custom adapter calling Relics and Curios APIs | Keep the published Relics and Curios engines; retain only the reward adapter | Replacing the adapter with hand-written static item NBT can diverge from valid Relics maxima. Real client slot-capacity/compatibility tests remain necessary |
| Claim-limited flight, timed flight, heal/feed/effects | Custom rules around vanilla effects/abilities; native FTB admin variants disabled | Native FTB commands exist, but a guard adapter would still be needed | Dropping the wrapper can lose claim limits, combat restrictions, expiry persistence and cooldowns. Preserve those rules; do not substitute unrestricted native admin commands |
| Item naming/lore/XP repair and world controls | Small custom vanilla command wrappers | Basic behavior is small, but supported standard providers should be preferred when they preserve XP/components/permissions | Native providers do not automatically reproduce the XP-based repair rule and earned milestones. Do not replace with free repair or expose raw `/time`, `/weather`, `/give` or unrestricted NBT |
| Personal time/weather | Custom outbound packet adapter | Preserve only if a compatible established provider is verified | Protocol mixin increases maintenance; a decision to drop personal visual controls would reduce code but remove an earned perk |
| JourneyMap travel | Custom safe visited-waypoint adapter | Existing JourneyMap UI retained | Removing the adapter restores native permission semantics and may require broader teleport privileges. Preserve visited/claim/combat guards unless explicitly changed |
| Priority login | Custom two reserved overflow slots | Existing larger queue systems often target Paper/proxies | This is reserved capacity, not a true queue. A proxy changes infrastructure and can require paid hosting/setup; no such migration is approved |
| Crash containment, logs, backups, performance, pregeneration | Neruina, GriefLogger/GLRA, AdvancedBackups, spark, Chunky | Keep published mods | Configuration backup/off-host recovery and a restored-server boot remain operational tests, not reasons to create more custom mods |
| Sable/Rapier deadlock mitigation | Custom compatibility mixin | Prefer an upstream fix in the exact pack version when available | Removing it without a replacement may restore the observed native collider deadlock. This is an upstream compatibility issue, not a feature-plugin replacement |
| Anticheat | Fiw AntiCheat client verification | Keep provisionally; do not mistake it for a complete server movement/combat anticheat | Its author explicitly says a malicious custom client can lie. Pack abilities complicate speed/flight checks. Mandatory companion and client privacy/configuration need owner acceptance. Anti-xray stays excluded |

## Cases for owner decisions

No replacement below has been activated.

1. **AFK:** prototype SimpleAFK for standard status/kicks while keeping active-reward filtering, or keep the current combined implementation. Recommendation: prototype the split approach; no weaker reward filtering.
2. **RTP:** investigate an adapter around FTB's existing travel engine while preserving cross-dimension commands, or accept native current-dimension RTP. Recommendation: retain the current commands while testing the adapter; do not give up cross-dimension functionality by default.
3. **Machinery gates:** add AStages to the next client/server release and test it, or keep server-only custom event guards. Recommendation: AStages prototype, with paid/30-hour stage synchronization and no pickup/crafting lockouts.
4. **Crates:** use FTB Quests' existing UI/loot machinery, add a physical chest/key mod to both sides, or keep command crates and their mailbox. Recommendation: choose the desired in-game interaction first. Physical crate presentation needs a client update; none is necessary for `/crate`.
5. **Personal visuals/priority/waypoints:** keep the specialized adapters, or simplify individual perks to reduce custom code. These should be decided separately, not removed in one bulk migration.

## Custom-code reliability findings

These are audit findings, not claims of completed crash testing:

- A revocation for an unknown transaction returns false and stores no tombstone. Test out-of-order purchase/refund commands and reconciliation against Tebex before enabling sales.
- Reward mail has player-save receipts, but consumable/voucher consumption and ledger writes span separate saves. Test a forced crash between each step; do not claim all reward actions are atomic.
- Vote delivery removes pending votes before running the configured command. Test failure/crash recovery so a consumed queue entry cannot silently lose the key.
- Machinery guards cannot cover every mod's direct placement or custom packet. A stage engine improves coverage but is not automatically Sable-compatible.
- AFK heuristics reject simple repeated signals but can still be fooled by varied macros. No candidate reviewed justifies promising perfect anti-AFK enforcement.
- Specialized mixins need regression tests on JourneyMap, Sable/Rapier and Minecraft updates. Prefer an upstream fix or a stable API to private-method hooks.

## Prices

My recommendation is to **keep Ember $8 and Artisan $16** as permanent launch prices. This is a value judgement, not a researched market median. The $8 entry is accessible and the next cumulative upgrade should be another $8, not a second full $16 purchase. Test that actual checkout behavior. [Tebex cumulative category documentation](https://docs.tebex.io/creators/tebex-control-panel/how-to-create-packages/how-to-create-a-category).

The more important issue is the strength of the rewards. A Creative Blaze Cake, maxed relic and instant machinery access can accelerate progression substantially. Raising or lowering price does not resolve balance. Test the early-game difference and decide about individual rewards before changing prices. No price changes have been made.

Gameplay rewards being obtainable without payment does not automatically address competitive advantage. [Minecraft's usage guidelines](https://www.minecraft.net/en-us/usage-guidelines) condition gameplay entitlements on their effect on other players and competitive advantage. Review the actual pack/PvP behavior and Tebex approval; this audit does not certify compliance.

## Sources and reproducibility

- [FTB Essentials installed-version source](https://github.com/FTBTeam/FTB-Essentials/tree/v2101.1.10), plus installed JAR bytecode inspection.
- [FTB XMod Compat 1.21.1 source](https://github.com/FTBTeam/FTB-XMod-Compat/tree/1.21.1/main), particularly permission-provider registration; [official Maven artifacts](https://maven.ftb.dev/releases/dev/ftb/mods/ftb-xmod-compat-neoforge/).
- [Playtime Rewards source](https://github.com/Matt-CompSci/PlaytimeRewards), `PlayTimeEvents.java` and `PlayTimeRewards.java`.
- [SimpleAFK 1.21.1 source](https://github.com/magnushjensen/simpleafk/tree/1.21.1).
- [ExtraRTP source](https://github.com/VecooDEV/ExtraRTP), NeoForge 21 service; [Teleport Commands](https://github.com/MrSn0wy/TeleportCommands).
- [AStages 1.21 source](https://github.com/Alessandro-Casale/AStages/tree/1.21.X); [ProgressiveStages](https://modrinth.com/mod/progressivestages).
- [RarityChests publisher description](https://www.curseforge.com/minecraft/mc-mods/raritychests); [Modular Loot Crates publisher metadata](https://modrinth.com/mod/lootcrates).
- Publisher API snapshots in `ops/audit/candidates.json` and `more-candidates.json` record exact matching releases and sides. GitHub development branches may differ from published binaries; candidate-specific runtime tests are still required.
- Third-party source downloads remain local under `ops/audit-sources`; they are not redistributed in the website repository.

The running release list is [NEXT-PACK-MODS.md](NEXT-PACK-MODS.md). Update its status only after a candidate is approved and validated. Do not add every candidate merely because it appears here.
