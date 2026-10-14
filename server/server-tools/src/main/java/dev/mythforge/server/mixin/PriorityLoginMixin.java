package dev.mythforge.server.mixin;
import dev.mythforge.server.PriorityLogin;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.net.SocketAddress;

@Mixin(PlayerList.class)
public abstract class PriorityLoginMixin {
    @Inject(method="canPlayerLogin",at=@At("RETURN"),cancellable=true)
    private void mythforge$reservedCapacity(SocketAddress address,GameProfile profile,CallbackInfoReturnable<Component> ci) {
        if(PriorityLogin.allow(profile,ci.getReturnValue()))ci.setReturnValue(null);
    }
}
