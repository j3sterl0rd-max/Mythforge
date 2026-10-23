package dev.mythforge.oasistest.mixin;
import dev.mythforge.oasistest.OasisIntegration;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(targets="net.orcinus.galosphere.world.gen.features.OasisFeature", remap=false)
public class AttemptTraceMixin {
    @Inject(method="place",at=@At("HEAD"))
    private void begin(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> ci) { OasisIntegration.begin(); }
    @Inject(method="place",at=@At("RETURN"))
    private void end(FeaturePlaceContext<?> context, CallbackInfoReturnable<Boolean> ci) { OasisIntegration.end(); }
}
