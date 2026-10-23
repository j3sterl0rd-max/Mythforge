package dev.mythforge.server;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PriorityLogin {
    public static boolean allow(GameProfile profile,Component rejection) {
        return ServerTools.state!=null&&rejection!=null&&rejection.getContents() instanceof TranslatableContents message
            &&message.getKey().equals("multiplayer.disconnect.server_full")&&(RoleBenefits.tier(profile)>=2 || ServerTools.state.players.getOrDefault(profile.getId().toString(),new State.PlayerData()).activeSeconds>=500*3600L)
            &&ServerTools.server.getPlayerList().getPlayerCount()<ServerTools.server.getMaxPlayers()+2;
    }
}
