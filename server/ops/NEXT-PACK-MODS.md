# Mods for the next Mythforge pack update

Maintained list, updated 2026-10-14. Target NeoForge 1.21.1. Activated services and excluded alternatives are recorded below. Do not add Fabric/Paper artifacts to this NeoForge pack.

## Confirmed additions

| Mod | Version | Client pack | Server pack | Reason/status |
| --- | --- | --- | --- | --- |
| [Fiw AntiCheat](https://modrinth.com/mod/fiw-anticheat) by Fi3w0 | 2.1.0, NeoForge 1.21.1 | Required companion | Installed | Published client pack lacks it. Preserve author attribution/license. Test legitimate pack clients before release; no anti-xray |
| [FTB XMod Compat](https://www.curseforge.com/minecraft/mc-mods/ftb-xmod-compat) | 21.1.12 | Include for supported cross-mod client features; omission must be tested | Added during audit | Required bridge for FTB Ranks values reaching FTB consumers. Dependencies match the installed server. Startup selected FTB Ranks; all 28 runtime checks passed, including consuming-provider checks |

| [AStages](https://modrinth.com/mod/astages) | 2.5.3-1.21.1 | Required | Installed and active | 414 functional machinery restrictions; paid or 30-hour synchronization |
| [SimpleAFK](https://modrinth.com/mod/simpleafk) | 1.5.2 | Not required | Installed and active | AFK status/sleep handling; shared activity adapter owns kicking |

## Server-pack additions already installed

Players do not need these administrative services just to join. Some mods also support optional client/singleplayer use; that is different from a mandatory client dependency.

| Mod | Installed version | Action for next exported server pack |
| --- | --- | --- |
| FTB Ranks | 2101.1.5 | Include alongside existing FTB Library, Essentials, Chunks and Teams |
| Official Tebex NeoForge | 2.4.6 for 1.21.1 | Include binary only; supply secrets separately on host |
| Modded Votifier | 1.0.3 | Include; generate/preserve private vote keys on host, not in public pack |
| AdvancedBackups | 3.7.1 | Include settings; exclude backup archives and private snapshots |
| Neruina | 3.3.3 | Include; keep existing Configurable 3.5.2 dependency |
| GriefLogger | 1.2.10 | Include; no real player database in public archive |
| GriefLogger Rollback Addon / GLRA | 1.0.0 | Include with web API disabled and no secrets |
| Mythforge Server Tools | 1.0.0 release build | Server-only. Published service integration and pack-specific rules; do not add to players' client mods |

Existing spark, Chunky, FTB Quests, Relics, Curios, JourneyMap, KubeJS and their pack dependencies should remain. This list is not a request to add duplicates or update unrelated gameplay mods.

## Alternatives not selected

| Candidate | Matching release found | Sides | Decision needed |
| --- | --- | --- | --- |
| ProgressiveStages | 3.0.6 | Client + server | Alternative to AStages, not an additional simultaneous stage engine |
| RarityChests | Publisher lists NeoForge 1.21.1 | Client + server | Physical vote chest/keys, exact loot weights and custom reward adapters; select/pin version only if approved |
| Teleport Commands | 1.3.5 | Server only | Only if existing FTB travel cannot preserve needed behavior; avoid duplicate command ownership |
| ExtraRTP | 1.5.0 | Server only | Not recommended as a drop-in: safety, radius and protection behavior differ |
| Playtime Rewards (neo-playtime-rewards) | 1.0.2 | Server only | Not recommended as a drop-in: inspected tick counter includes AFK and does not replace durable reward mail |

## Excluded or unnecessary

- Paper/Bukkit plugins such as standard EssentialsX/Grim/queue/crate plugins cannot be loaded directly by this NeoForge server. A hybrid-server migration is not approved.
- Essential Commands and Modular Loot Crates returned no matching NeoForge 1.21.1 release in the publisher API query. Do not treat a nearby Minecraft version or Fabric release as compatible.
- AntiAfk client mods automate activity; they are not server anti-AFK detectors. AFK farm stand-in mods are not a substitute for the requested reward filtering.
- No new client mod is needed for the existing command-based `/crate`, built-in vanilla menus, XP repair or reward mailbox.

## Release checklist for this list

Before an export, pin exact loader/game version and dependencies, obtain binaries from publishers, retain licenses/attribution, test a fresh ordinary client, and verify every server-only mod can be absent from that client. Export server configuration without worlds, logs, player data, databases, payment secrets or vote private keys. Move candidates to confirmed only after approval and runtime validation. Record replacements/removals and migration steps in MOD-AUDIT.md.
