package dev.mythforge.server.mixin;
import dev.ftb.mods.ftbessentials.util.WarmupCooldownTeleporter;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.*;
@Mixin(value=WarmupCooldownTeleporter.class,remap=false)
public abstract class TeleportWarmupMixin {
    @Shadow @Final private static Map<UUID, ?> WARMUPS;
    @Shadow @Final private static Map<UUID, ?> pendingAdditions;
    @Inject(method="cancelWarmup",at=@At("HEAD"),remap=false)
    private static void mythforge$cancelBeforeFirstTick(ServerPlayer player,CallbackInfo ci){pendingAdditions.remove(player.getUUID());}
    @ModifyConstant(method="tickWarmups",constant=@Constant(doubleValue=0.25),remap=false)
    private static double mythforge$stayStill(double original){return 0.0001;}
}
