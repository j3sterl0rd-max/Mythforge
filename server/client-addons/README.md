# Updated client export available

Import `../client-release/Mythforge-Beyond-the-Horizon-1.2.5.zip` as a new CurseForge profile. It includes/references the additions below and updates JEI to 19.53.0.425 to satisfy XMod Compat. Follow `../ops/START-HERE.md`. Manual upgrades must replace the old JEI, not keep both. The ZIP has been checked for integrity and preservation; actual client login is still pending.

# Client additions for the launch copy

Use the Mythforge 1.2.4 client instance, Minecraft 1.21.1 / NeoForge 21.1.253 / Java 21.

Add these exact publisher JARs to its mods directory:
- astages-2.5.3-1.21.1.jar: required stage protocol, from the AStages publisher. License: All Rights Reserved; use publisher references in public pack exports.
- ftb-xmod-compat-neoforge-21.1.12.jar: FTB cross-mod integration. Preserve publisher notices.

Keep existing FTB Library, Quests, Chunks, Teams, KubeJS and other dependencies. Do not add server-only Tebex, SimpleAFK, Mythforge Server Tools, backups or loggers to the client.

Connect to 127.0.0.1:25566 from this computer after the account is whitelisted in the server console. The original server address remains unchanged.

These are actual downloaded releases, not placeholder files. Server startup and registry tests passed. A real client login, UI/recipe compatibility and second-player protection tests still need a Minecraft client session.
