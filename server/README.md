# Mythforge server launch source

Read [the operational walkthrough](ops/LAUNCH-README.md) and [activated services](ops/REPLACEMENTS-RELEASE.md). This is the credential-free source/configuration snapshot of the fresh launch copy. It contains actual installed integrations, without the modpack binaries or world. Overlay only on a stopped compatible pack. Java 21 and the exact publisher dependencies in server-tools/build.gradle are required. Configure private secrets locally.

SimpleAFK and AStages are installed; FTB Quests owns crate selection and FTB Essentials owns RTP destination search. Pack-specific progression, protection and delivery adapters remain. The copy stays isolated from the existing live server until cutover. Tebex review and real-client/public-service verification remain external launch requirements.
