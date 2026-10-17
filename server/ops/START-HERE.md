# Mythforge: your remaining setup checklist

Updated October 16, 2026. Work through this in order. Check each box only after its result passes. The launch server remains private; the old test server must keep its current address until you deliberately switch.

## 1. Import the updated client

- [ ] Open CurseForge, select Minecraft, and choose Import (usually beside Create Custom Profile).
- [ ] Select `C:\Mythforge Server\client-release\Mythforge-Beyond-the-Horizon-1.2.5.zip`. Import as a new profile so your old instance and saves remain available.
- [ ] Let CurseForge finish downloading the referenced mods. If it asks for a manual download, follow the publisher link it provides; do not substitute a Fabric or Forge file.
- [ ] Check the profile uses Minecraft **1.21.1**, **NeoForge 21.1.253**, and **Java 21**. Start with the export's existing 9 GB memory recommendation; close unnecessary programs while both servers run.
- [ ] In the profile's mod list, confirm **AStages 2.5.3-1.21.1**, **Fiw AntiCheat 2.1.0**, **FTB XMod Compat 21.1.12**, and **JEI 19.53.0.425** (the publisher marks this required compatible build as beta). AStages and XMod Compat download through the manifest. Fiw is already included in overrides: do not add it twice.
- [ ] Launch and reach the main menu. If it fails, save that profile's `logs/latest.log` and crash report and send their paths to me. Do not delete random mods to get past the error.

The export preserves 1,633 original archive entries byte for byte, all original mod pins except JEI, and all original artwork/scripts/configuration. JEI was raised from 19.51.0.418 to the minimum required by XMod Compat. Fiw AntiCheat is by [Fi3w0](https://modrinth.com/mod/fiw-anticheat); its original license and attribution are included. Archive integrity checks passed. A real client launch and join are still unverified.

## 2. Join the private launch copy

- [ ] If this copy is already running, use its existing console. Do not start a second process. Otherwise open PowerShell and run:

```powershell
Set-Location -LiteralPath 'C:\Mythforge Server'
powershell -NoProfile -ExecutionPolicy Bypass -File .\ops\start-staging.ps1
```

- [ ] Wait for the server's `Done` message. In **this copy's console**, type `whitelist add YOUR_JAVA_NAME` (replace the name; no leading slash).
- [ ] In Minecraft choose Multiplayer, Add Server, address **127.0.0.1:25566**. This address works only on this computer. Stay connected at least two minutes to verify the Fiw handshake does not disconnect you.
- [ ] Open quests, JEI, inventory, map and a normal recipe. Walk around, place ordinary blocks, claim a chunk, use a home and `/back`. Record any missing recipes, visual failures or disconnects.

If the server closes during boot, preserve its crash report. If memory is exhausted while the original server and client run, arrange a maintenance window to stop the old server normally before testing; do not kill an unknown Java process.

## 3. Run the short acceptance test

Use a normal, non-OP test player. For protection checks you need a second real Java account. Both accounts must be whitelisted. The automated 60-check backend test passed previously; these checks cover real client behavior it cannot establish.

- [ ] Before players join, run `mythforge audit` and `mythforge selftest` in the launch console. `ops\evidence\selftest.json` must show zero failures. Do not run selftest with players online.
- [ ] Default player: confirm `/give`, `/op` and quest editing are denied; ordinary quests, homes and claims work.
- [ ] Player A claims land. Player B, outside A's team, must not break/place/open containers there. Test an actual automation contraption as well as hand interactions. A locked player must not operate gated machinery through another player or an unclaimed automation setup.
- [ ] On safe ground test `/rtp`, move during its warmup, and confirm cancellation. Repeat after taking damage. Check `/rtp nether` and `/rtp end` are denied below their paid/earned requirements.
- [ ] For temporary paid access, obtain your Java UUID from the launch server's `usercache.json`. Replace `UUID` below with that value, then use these console commands:

```text
mythforge purchase UUID artisan acceptance_artisan_20261016
```

Test Artisan Nether RTP and the added home/claim allowance, then:

```text
mythforge purchase UUID runeforged acceptance_runeforged_20261016
```

Test End RTP. Remove both test purchases afterward:

```text
mythforge revoke acceptance_runeforged_20261016
mythforge revoke acceptance_artisan_20261016
```

These purchases deliver real test kits/items and record receipts. Use a disposable tester account; revoking removes privileges but does not reclaim delivered items. Do not reuse those transaction IDs for another person. Do not set your real earned hours forward just to test: that grants real milestone rewards.

- [ ] Check `/pwarp` opens its menu and items cannot be removed from that menu. Test chosen icons with an eligible tester.
- [ ] Check a maxed relic reward and extra Curios equipment slots on the actual client. Backend registry checks cannot certify the displayed UI.
- [ ] Stand idle for 15 minutes; confirm the AFK kick. Repeat with a simple repeating movement/click pattern. Confirm active play earns time while idle/repeating activity does not. Report bypasses for tuning; no system guarantees detection of every macro.
- [ ] During play run `spark profiler start --timeout 120` in the console. Keep its resulting report link and note any repeated freezes or broken recipes. The inherited pack has unresolved recipe/loot warnings; successful startup alone does not clear them.

For the full test matrix and troubleshooting, see [LAUNCH-README.md](LAUNCH-README.md).

## 4. Publish the client after the join test passes

- [ ] Sign into your existing CurseForge project owner account. Upload the new client ZIP as version **1.2.5**, Minecraft **1.21.1**, loader **NeoForge**.
- [ ] Use `client-release\CHANGELOG.txt` for the changelog. Keep the visible Fi3w0 credit and link. The bundled Fiw binary is unmodified; its license is included. If CurseForge requests third-party review, supply the publisher/license links rather than removing notices.
- [ ] Wait for approval, then install the published release into a fresh profile and join once more. Share the approved release with testers. The local ZIP being built does not mean it has been uploaded or approved.

## 5. Finish the external accounts

**Voting:** create listings for your final address on Minecraft-MP, MinecraftServers.org, Planet Minecraft, Minecraft-Server-List, TopG and TopModdedServers. Use Votifier TCP **8193**. For v2 read the token privately from `config\votifier\config.json`; for v1 use only `config\votifier\public.pem`. Never share `private.pem`. Save each voting URL. Internet vote tests need the final reachable listener after cutover. Test online and offline votes and confirm exactly one key arrives. Voting stays off the website as requested.

**Discord:** automatic purchased-rank roles are not connected. Create the four supporter roles without staff permissions, then choose an account-linking integration that verifies Minecraft UUID ownership. Its bot role should sit above only the roles it manages. Test delivery and removal. Until that is configured, grant/revoke manually against verified receipts and game identity. I still need your Discord server/integration access to implement this part.

**Tebex:** review was submitted October 14; the last verified state was pending review. Sign into the creator dashboard and check the current status. Complete any owner verification requests. Once enabled, use its free test/manual-payment facility and check delivery, restart persistence, refund removal, and Ember-to-Artisan charging only the **$8 difference**. Do not spend money or activate a paid plan to test. Tell me when the account is enabled so I can finish the official queue roundtrip and website purchase links. The website must stay closed to purchases until delivery is verified.

## 6. Protect the new world

- [ ] Stop this launch copy using `stop` and wait for Java to exit. Then run `ops\backup-settings.ps1` from PowerShell. This archive contains secrets: keep it private.
- [ ] Copy the latest complete world backup and settings archive to a second device or private off-site storage. Local backups alone do not cover disk failure.
- [ ] The full ZIP restore and restored boot passed. Still test the AdvancedBackups differential-chain restore using its bundled restore tool into a separate directory. Preserve the complete chain. Follow the recovery section of LAUNCH-README; never restore over a running world.

Do not run `reset-copied-data.ps1` again. The old data was already cleared; it would erase the fresh generated world. Do not run `prepare-launch.mjs` over earned player ranks.

## 7. Switch servers only when ready

- [ ] Finish the tests above, resolve serious pack faults, and arrange maintenance.
- [ ] Stop the original test server normally and verify its port is released. Keep its directory as a stopped rollback copy.
- [ ] With both servers stopped, run `ops\start-production.ps1`. It refuses to use an occupied game port. Read the production cutover section in LAUNCH-README before proceeding.
- [ ] Point your router/firewall and DNS at this computer: game TCP **25565**, votes TCP **8193**. Change Votifier's localhost bind to the intended public listening interface. Keep workshop **18765** private, authentication enabled, and RCON disabled.
- [ ] Have someone outside your home network join using the public address. Test each voting site's callback and confirm backups and Tebex still work.
- [ ] Keep the whitelist until controlled tests pass. Spawn construction remains a separate task as requested. Ask me to update the website when you want “Server coming soon!” changed.

## 8. Activate website applications (when back at your computer)

- [ ] Open PowerShell and run `npx wrangler login`. Complete the official Cloudflare authorization for the account owning mythforge.win. Tell Codex when signed in so the remaining setup can be connected and tested.
- [ ] Check the verified destination inbox behind admin@mythforge.win in Cloudflare Email Routing. The preferred alert method sends only to that destination using Cloudflare's free exception. Do not upgrade Workers or buy an email plan. General outbound email is paid.
- [ ] Follow [WHITELIST-SETUP.md](WHITELIST-SETUP.md). Detailed private deployment instructions are in `ops/private/whitelist/README.md`; keep them private.
- [ ] Complete the remaining integrations and a real application/email/server-confirmation test before enabling public submissions. The `/apply/` page stays closed until those checks pass.

## What to send me if something fails

Send the failed step number, Java player name, exact time, screenshot of the error if useful, and paths to the client/server `latest.log` or crash report. Keep payment keys, vote tokens, private backups and account passwords out of messages and public reports.
