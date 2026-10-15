package dev.mythforge.server.mixin;

import dk.magnusjensen.simpleafk.NeoforgeSimpleAFK;
import net.neoforged.fml.event.config.ModConfigEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** SimpleAFK 1.5.2 reads its detached config during unloading; loading/reloading still run normally. */
@Mixin(NeoforgeSimpleAFK.class)
public abstract class SimpleAfkConfigMixin {
    @Inject(method="onConfigUpdates",at=@At("HEAD"),cancellable=true,remap=false)
    private void mythforge$skipUnloadedConfig(ModConfigEvent event,CallbackInfo callback) {
        if(event instanceof ModConfigEvent.Unloading)callback.cancel();
    }
}
