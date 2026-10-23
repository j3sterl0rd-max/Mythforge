# Galosphere oasis generation fix (server tools 1.0.10)

Galosphere 1.5.5 queries the live world's structure manager inside the oasis
placement loop. A captured generation worker remained blocked in that lookup
through all 20 JFR thread dumps while the server repeatedly retried chunk unloads.

`GalosphereOasisMixin` wraps only that shrine lookup. During generation it uses
`StructureManager.forWorldGenRegion`, so chunk reads use the supplied generation
region. MixinExtras `@Share` stores the result in a local reference belonging to
one `place` invocation. All remaining iterations reuse it. The original lookup
position and shrine exclusion remain intact; manual placement retains its normal
live-world manager. There is no global cache or shared mutable placement state.

Oases remain enabled. No client update or Galosphere JAR modification is needed.
The patch requires the targeted invocation in the installed 1.5.5 class;
review this compatibility patch when upgrading Galosphere. Other server-tools
features are byte-identical to 1.0.9.

## Verification

Build with Java 21, from `server-tools`:

```powershell
./gradlew.bat --offline build oasisIntegrationJar
```

From the server root, run:

```powershell
python ops/test-galosphere-oasis.py
```

The runner uses NeoForge 21.1.263, the installed Galosphere 1.5.5, and the exact
compiled production mixin in a small test mod. It creates an isolated fixed
pink-salt biome world, binds only 127.0.0.1:25589, and disables RCON. It does not
load production configuration, other production mods, or the production world.
The test server exits itself after the assertions and saves all dimensions.

Tests assert that generation lookups use WorldGenRegion, each attempt performs
at most one real shrine lookup, separate attempts do not share results, manual
placement works, and a valid shrine result prevents oasis placement. A synthetic
valid structure result isolates the shrine-exclusion assertion; generation also
executes real structure lookups. Both positive and negative chunk coordinates
are generated. Test instrumentation is excluded from the production JAR.

The initial successful run recorded 166 attempts, 166 checks, 161 generation
checks, zero repeated lookups, and a clean final save. This fixes the recorded
oasis lookup; production-wide TPS recovery must still be measured after startup.

## Installation and rollback

While the production world is closed, replace the older server-tools JAR with
`build/libs/mythforge-server-tools-1.0.10.jar`. Keep only one server-tools JAR in
`mods`. Preserve the old JAR outside that folder for rollback. Production's
original 1.0.9 JAR is preserved in `ops/audit/oasis-fix-20261023/`.

Do not install the optional oasis-disable datapack: this permanent code fix keeps
oasis generation. Leave chunk pregeneration paused until ordinary gameplay and
unloading have been checked after startup.

Upstream context: https://github.com/0rc1nus/Galosphere-Main/issues/239
