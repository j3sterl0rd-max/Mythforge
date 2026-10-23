package dev.mythforge.server.mixin;
import dev.mythforge.server.CommandPolicy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Owner ranks receive vanilla command permission level four without persistent OP changes. */
@Mixin(CommandSourceStack.class)
public abstract class OwnerCommandPermissionMixin {
    @Inject(method="hasPermission",at=@At("HEAD"),cancellable=true)
    private void mythforge$ownerCommands(int level,CallbackInfoReturnable<Boolean> ci){
        var source=(CommandSourceStack)(Object)this;
        if(level<=4&&source.getEntity() instanceof ServerPlayer player&&CommandPolicy.owner(player))ci.setReturnValue(true);
    }
}
