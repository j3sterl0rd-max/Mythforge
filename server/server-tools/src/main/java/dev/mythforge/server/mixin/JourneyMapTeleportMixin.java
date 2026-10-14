package dev.mythforge.server.mixin;

import dev.mythforge.server.Waypoints;
import journeymap.common.util.JourneyMapTeleport;
import journeymap.common.network.model.Location;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(JourneyMapTeleport.class)
public abstract class JourneyMapTeleportMixin {
    @Inject(method="hasVanillaTeleportPrivilege",at=@At("HEAD"),cancellable=true,remap=false)
    private void mythforge$earnedPermission(ServerPlayer p,CallbackInfoReturnable<Boolean> ci) {ci.setReturnValue(Waypoints.allowed(p));}
    @Inject(method="attemptTeleport",at=@At("HEAD"),cancellable=true,remap=false)
    private void mythforge$safeWaypoint(Entity entity,Location location,CallbackInfoReturnable<Boolean> ci) {
        if(entity instanceof ServerPlayer p)ci.setReturnValue(Waypoints.request(p,location));
    }
}
