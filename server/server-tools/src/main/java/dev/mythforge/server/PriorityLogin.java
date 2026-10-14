package dev.mythforge.server;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class PriorityLogin {
    public static boolean allow(GameProfile profile,Component rejection) {
        return ServerTools.state!=null&&rejection!=null&&rejection.getContents() instanceof TranslatableContents message
            &&message.getKey().equals("multiplayer.disconnect.server_full")&&ServerTools.state.paid(profile.getId())>=2
            &&ServerTools.server.getPlayerList().getPlayerCount()<ServerTools.server.getMaxPlayers()+2;
    }
}
