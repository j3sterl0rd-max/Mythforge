package dev.mythforge.server.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

/** Keep oasis shrine checks in the generation region, once per placement attempt. */
@Pseudo
@Mixin(targets = "net.orcinus.galosphere.world.gen.features.OasisFeature", remap = false)
public abstract class GalosphereOasisMixin {
    @WrapOperation(method = "place", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/level/StructureManager;getStructureAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/levelgen/structure/Structure;)Lnet/minecraft/world/level/levelgen/structure/StructureStart;"), require = 1)
    private StructureStart mythforge$checkShrineInGenerationRegion(
        StructureManager manager, BlockPos origin, Structure shrine,
        Operation<StructureStart> original,
        FeaturePlaceContext<NoneFeatureConfiguration> context,
        @Share("mythforge$oasisShrine") LocalRef<StructureStart> cached) {
        StructureStart result = cached.get();
        if (result == null) {
            if (context.level() instanceof WorldGenRegion region) {
                manager = manager.forWorldGenRegion(region);
            }
            result = original.call(manager, origin, shrine);
            cached.set(result);
        }
        return result;
    }
}
