package dev.mythforge.server.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Persist a reward recipient and receipt without saving unrelated online players. */
@Mixin(PlayerList.class)
public interface PlayerListSaveInvoker {
    @Invoker("save")
    void mythforge$savePlayer(ServerPlayer player);
}
