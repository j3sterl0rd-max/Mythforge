package dev.mythforge.server.mixin;

import dev.mythforge.server.PersonalView;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(ServerCommonPacketListenerImpl.class)
public abstract class PersonalViewMixin {
    @ModifyVariable(method="send(Lnet/minecraft/network/protocol/Packet;)V",at=@At("HEAD"),argsOnly=true)
    private Packet<?> mythforge$personalView(Packet<?> packet) {
        if((Object)this instanceof ServerGamePacketListenerImpl connection)return PersonalView.rewrite(connection.player,packet);
        return packet;
    }
}
