package dev.mythforge.oasistest.mixin;
import dev.mythforge.oasistest.OasisIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(StructureManager.class)
public class StructureTraceMixin {
    @Shadow @Final private LevelAccessor level;
    @Inject(method="getStructureAt",at=@At("HEAD"),cancellable=true)
    private void check(BlockPos pos, Structure structure, CallbackInfoReturnable<StructureStart> ci) {
        OasisIntegration.check(level);
        if (OasisIntegration.forceShrine && !OasisIntegration.CALLS.get().isEmpty()) ci.setReturnValue(OasisIntegration.SHRINE);
    }
}
