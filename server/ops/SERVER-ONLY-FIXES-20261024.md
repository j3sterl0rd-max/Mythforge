# Server-only fixes - October 24, 2026

All changes run on the dedicated server or its host. The banned-item client tooltip script was deliberately left to the pack maintainer. No client mod update is required for this release.

## Installed and active

- Server Tools 1.0.11 saves reward recipients through the actual protected PlayerList.save API via a server-side mixin invoker. It preserves inventory/receipt-before-ledger-clear ordering while avoiding saving unrelated online players. Redundant unchanged ledger writes are skipped; changed entitlements/rewards still save immediately using atomic replacement and the previous copy.
- Built against production NeoForge 21.1.263. Explicit Sable/Votifier/FTB dependency bounds prevent silently using unreviewed integration versions.
- Maintenance and launch scripts now select the configured bind endpoint, check Java ownership and matching Minecraft/RCON ownership, and ignore Tailscale listeners on different addresses. Wildcard conflicts are still rejected. Launch was verified using the repaired scheduled launcher with Tailscale left running.
- Local status uses authenticated RCON. --public checks play.mythforge.win through TCPShield. Both passed after deployment; no public RCON or firewall changes were needed.
- Whitelist startup no longer pipes native stderr through Windows PowerShell with ErrorActionPreference=Stop. A controlled test reproduced NativeCommandError in the old wrapper. The new wrapper uses direct process-level log redirection, a singleton mutex and safe configuration validation. The task now runs with no execution-time limit, explicit per-process execution policy and its existing retry policy. connector-health.json records successful polling; private configuration remains in ops/private/whitelist. Real queue processing is active. A read-only authenticated local CHECK passed; no synthetic account was added and no test application/email was submitted.
- Removed retired spawn-construction helpers that accessed private chunk ticket maps and LimitedChunks internals. Active protection, arrival handling and validation commands remain intact.
- Pregeneration now reuses one authenticated RCON connection, reconnects after failures, waits through extended server outages and does not mistake intentional player pauses for generation stalls. The approved plan runs under Mythforge-Pregeneration, a logon task with single-instance behavior, failure retries and no time limit. It still pauses for players/slow ticks, respects the stop marker and stops under the disk-space threshold. Create ops/pregeneration-stop.txt to stop it intentionally.

## Verification

- Build succeeded against NeoForge 21.1.263.
- Isolated full-pack server: 26 update regression checks passed (rewards, replay protection, PvP and lifecycle).
- Independently read the recipient's actual saved NBT: inventory and delivery receipt both persisted.
- Isolated spawn protection: 13 checks passed after construction-code removal.
- Ledger persistence: five checks passed, including unchanged-write suppression, immediate changed-state persistence and previous-copy retention.
- Endpoint selection: five regression checks passed, including Tailscale-only stopped-server and wildcard conflicts.
- Pregeneration: six offline regression checks passed, including connection reuse, reauthentication, player pause, real stall behavior and completion handoff. The supervisor now allows time for Chunky to publish its completion log and accepts a final count ahead of its saved checkpoint. The 5,000-block overworld run finished with 393,129 chunks; its completion was verified in the archived log and the remaining approved queue resumed.
- Production restarted cleanly with no players online. Server Tools 1.0.11 and CraftControl loaded; local/public health checks passed. Whitelist task remained Running with fresh successful polling timestamps.
- The interrupted backup from the earlier restart lacked a ZIP directory; it was preserved with a .zip.incomplete extension so it is not mistaken for a usable archive. Existing completed backups were retained.

## Recovery/source locations

Local originals and previous JAR are preserved under ops/audit/server-only-fixes-20261024/before. Deployment hash is in deployment.json alongside the test evidence. Restore JARs only while the world is closed; keep exactly one Server Tools JAR in mods.

The source-only whitelist launcher and connector in server/whitelist deploy to ops/private/whitelist on the server host. They require the existing private configuration/token files, which are intentionally not included here. The Windows tasks are host configuration; this update did not install client software or distribute credentials.
