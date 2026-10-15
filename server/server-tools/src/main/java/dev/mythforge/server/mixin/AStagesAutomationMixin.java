package dev.mythforge.server.mixin;

import com.alessandro.astages.infrastructure.hook.restriction.ItemServerEvents;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Automated actors use the target claim owner's entitlement, not a shared fake UUID. */
@Mixin(ItemServerEvents.class)
public abstract class AStagesAutomationMixin {
    @Inject(method="onBlockPlaced",at=@At("HEAD"),cancellable=true,remap=false)
    private static void mythforge$claimedAutomation(BlockEvent.EntityPlaceEvent event,CallbackInfo callback) {
        if(event.getEntity() instanceof FakePlayer)callback.cancel();
    }
    @Inject(method="onItemUsed(Lnet/neoforged/neoforge/event/entity/player/PlayerInteractEvent$RightClickBlock;)V",at=@At("HEAD"),cancellable=true,remap=false)
    private static void mythforge$claimedInteraction(PlayerInteractEvent.RightClickBlock event,CallbackInfo callback) {
        if(event.getEntity() instanceof FakePlayer)callback.cancel();
    }
}
