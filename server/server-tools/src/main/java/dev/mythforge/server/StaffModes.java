package dev.mythforge.server;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import static net.minecraft.commands.Commands.literal;

/** No player argument and no permission-level elevation. */
final class StaffModes {
    static boolean allowed(CommandSourceStack source){
        return source.getEntity() instanceof ServerPlayer player&&RoleBenefits.staff(player.getGameProfile());
    }
    static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        var root=literal("gm").requires(StaffModes::allowed);
        for(var mode:GameType.values())root.then(literal(mode.getName()).executes(context->change(context.getSource(),mode)));
        dispatcher.register(root);
    }
    static int change(CommandSourceStack source,GameType mode) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if(!allowed(source)){source.sendFailure(Component.literal("Staff only; game mode changes are self-only."));return 0;}
        var player=source.getPlayerOrException();
        player.setGameMode(mode);
        ServerTools.LOG.info("[Mythforge] {} ({}) changed their own game mode to {}",player.getGameProfile().getName(),player.getUUID(),mode.getName());
        source.sendSuccess(()->Component.literal("Your game mode is now "+mode.getName()+"."),false);
        return 1;
    }
}
