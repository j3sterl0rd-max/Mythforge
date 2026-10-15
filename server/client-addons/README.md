# Client additions for the launch copy

Use the Mythforge 1.2.4 client instance, Minecraft 1.21.1 / NeoForge 21.1.253 / Java 21.

Add these exact publisher JARs to its mods directory:
- astages-2.5.3-1.21.1.jar: required stage protocol, from the AStages publisher. License: All Rights Reserved; use publisher references in public pack exports.
- fiw-anticheat-neoforge-1.21.1-2.1.0.jar: required verification companion by Fi3w0. Preserve its attribution and license notices.
- ftb-xmod-compat-neoforge-21.1.12.jar: FTB cross-mod integration. Preserve publisher notices.

Keep existing FTB Library, Quests, Chunks, Teams, KubeJS and other dependencies. Do not add server-only Tebex, SimpleAFK, Mythforge Server Tools, backups or loggers to the client.

Connect to 127.0.0.1:25566 from this computer after the account is whitelisted in the server console. The original server address remains unchanged.

These are actual downloaded releases, not placeholder files. Server startup and registry tests passed. A real client login, UI/recipe compatibility and second-player protection tests still need a Minecraft client session.
