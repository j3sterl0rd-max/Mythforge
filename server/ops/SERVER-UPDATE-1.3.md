# Mythforge server 1.3 - October 24, 2026

Installed from the operator-provided 1.3 CurseForge export and matching instance at E:/ModdedMC/Instances/Mythforge - Beyond the Horizon (1). Minecraft 1.21.1 and NeoForge 21.1.263 are unchanged.

16 shared mod JARs were updated, including Mythforge Fixes 0.2.5. Client-only additions remain in the client pack. Server-only TCPShield, CraftControl, Tebex, Votifier, staff/rank, AFK, logging and Simple Backups integrations are retained.

Server Tools 1.0.12 is compiled against the new FTB Quests and Relics versions. The duplicated Galosphere and Rapier mixins are no longer activated by Server Tools: Core 0.2.5 owns them. A required Core 0.2.5 dependency prevents accidentally deploying that arrangement with the older core.

The published Core 0.2.5 supports the existing automatic-ship-backup disable flag natively. The old modified 0.2.2 JAR was replaced with the unmodified 0.2.5 JAR. JVM verification confirmed disabled automatic ensure/request/tick calls and successful ship-load readiness with the flag present. Normal ship saves and Simple Backups remain enabled.

32 pack override files were updated. Server exceptions retained deliberately: five-second FTB teleport warmups, quiet Sable sublevel saves, full Moderator/Administrator display names, NPC RTP permissions, Votifier endpoint/token/reward command and all server-only scripts. Backup .bak files from the client export were excluded. Existing worlds, player/rank/claim state, purchases, reward balances, staff assignments, credentials, public hostname and JVM launch settings were preserved.

Verified configuration backups for BOTH builds and the previous changed JARs are under ops/updates/pack-1.3/before. The backup contains private server configuration and must not be published. installed.json in the same update folder lists exact JAR hashes and override changes.

Build and five ledger-persistence checks passed. Full-pack isolated startup passed; 26 server-tools runtime checks and 13 spawn-protection checks passed. KubeJS resource reload completed with 0 failed recipes. The isolated server shut down cleanly before production launch.
