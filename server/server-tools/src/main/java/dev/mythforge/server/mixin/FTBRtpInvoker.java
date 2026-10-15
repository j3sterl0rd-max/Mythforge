package dev.mythforge.server.mixin;

import dev.ftb.mods.ftbessentials.commands.groups.TeleportingCommands;
import dev.ftb.mods.ftbessentials.util.TeleportPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Installed FTB Essentials has no public cross-dimension search entry point. */
@Mixin(TeleportingCommands.class)
public interface FTBRtpInvoker {
    @Invoker(value="findBlockPos",remap=false)
    static TeleportPos mythforge$find(ServerLevel level,ServerPlayer player,int min,int max) {throw new AssertionError();}
}
