package dev.mythforge.oasistest;

import com.google.gson.GsonBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.structures.BuriedTreasurePieces;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Mod("mythforge_oasis_test")
public final class OasisIntegration {
    public static final ThreadLocal<ArrayDeque<Integer>> CALLS = ThreadLocal.withInitial(ArrayDeque::new);
    public static final AtomicInteger attempts = new AtomicInteger(), checks = new AtomicInteger();
    public static final AtomicInteger generationChecks = new AtomicInteger(), repeated = new AtomicInteger();
    public static final AtomicInteger manualChecks = new AtomicInteger();
    public static volatile boolean forceShrine;
    public static final StructureStart SHRINE = new StructureStart(null,
        new net.minecraft.world.level.ChunkPos(0, 0), 0,
        new PiecesContainer(List.of(new BuriedTreasurePieces.BuriedTreasurePiece(BlockPos.ZERO))));

    public OasisIntegration() { NeoForge.EVENT_BUS.addListener(OasisIntegration::run); }

    public static void begin() { attempts.incrementAndGet(); CALLS.get().push(0); }
    public static void end() {
        int n = CALLS.get().pop();
        if (n > 1) { repeated.incrementAndGet(); throw new AssertionError("Repeated shrine lookup in one attempt: " + n); }
    }
    public static void check(LevelAccessor level) {
        var stack = CALLS.get();
        if (stack.isEmpty()) return;
        checks.incrementAndGet(); stack.push(stack.pop() + 1);
        if (Thread.currentThread().getName().startsWith("Worker")) {
            if (!(level instanceof WorldGenRegion)) throw new AssertionError("Oasis requested the live level from worldgen");
            generationChecks.incrementAndGet();
        } else if (Thread.currentThread().getName().equals("Server thread")) manualChecks.incrementAndGet();
    }
    private static void run(ServerStartedEvent event) {
        var server = event.getServer();
        try {
            var level = server.overworld();
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), "gamerule spawnChunkRadius 0");
            // A fixed pink-salt biome makes normal generation exercise the actual feature.
            level.getChunk(40, 40);
            level.getChunk(-40, -40);
            var feature = level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.parse("galosphere:oasis"));
            if (feature == null) throw new AssertionError("Missing oasis feature");
            int before = manualChecks.get();
            for (int i = 0; i < 4; i++) {
                feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(i), new BlockPos(i - 2, 80, 0));
            }
            if (manualChecks.get() - before != 4) throw new AssertionError("Cache crossed placement boundaries: " + (manualChecks.get() - before));
            forceShrine = true;
            before = manualChecks.get();
            boolean placed = feature.place(level, level.getChunkSource().getGenerator(), RandomSource.create(8), new BlockPos(0, 80, 0));
            forceShrine = false;
            if (placed || manualChecks.get() != before + 1 || !SHRINE.isValid()) throw new AssertionError("Shrine exclusion changed");
            if (generationChecks.get() < 1 || repeated.get() != 0) throw new AssertionError("Generation path not exercised");
            Files.writeString(Path.of("oasis-test-result.json"), new GsonBuilder().setPrettyPrinting().create().toJson(
                Map.of("passed", true, "attempts", attempts.get(), "structureChecks", checks.get(),
                    "generationChecks", generationChecks.get(), "repeatedLookups", repeated.get(),
                    "manualPlacement", true, "shrineExclusion", true, "cacheIsolation", true)));
            System.out.println("MYTHFORGE_OASIS_TEST_PASSED");
        } catch (Throwable failure) {
            failure.printStackTrace();
            try { Files.writeString(Path.of("oasis-test-failure.txt"), failure.toString()); } catch (Exception ignored) { }
        } finally { forceShrine = false; server.halt(false); }
    }
}
