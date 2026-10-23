package dev.mythforge.server;

import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import java.util.*;

final class CommandPolicyTest {
    static void run(SelfTest.TestPlayer player,List<String> passed) throws Exception {
        var dispatcher=ServerTools.server.getCommands().getDispatcher();
        var source=player.createCommandSourceStack();
        var profile=player.getGameProfile();
        var mode=player.gameMode.getGameModeForPlayer();
        try {
            for(String role:List.of("player","junior_mod","moderator","admin","owner")){
                var rank=FTBRanksAPI.manager().getRank(role).orElseThrow();
                if(!role.equals("player"))rank.add(profile);
                try {
                    ServerTools.reconcile(player);
                    int level=CommandPolicy.level(player);
                    SelfTest.check(dispatcher.getRoot().getChild("back").canUse(source),role+" can use /back",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("kick").canUse(source)==(level>=1),role+" has correct kick access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("ban").canUse(source)==(level>=2),role+" has correct ban access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("spark").canUse(source)==(level>=3),role+" has correct profiler access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("cmdtest").canUse(source)==(level==4),role+" has correct maintenance command access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("execute").canUse(source)==(level==4)&&dispatcher.getRoot().getChild("function").canUse(source)==(level==4),role+" has correct nested command access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("ftbquests").getChild("editing_mode").canUse(source)==(level>=3),role+" has correct quest editing access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("afk").getChild("bypass-list").canUse(source)==(level>=3),role+" has correct AFK bypass access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("afk").getChild("toggle").getChildren().stream().allMatch(node->node.canUse(source)==(level>=3)),role+" cannot toggle another player's AFK unless Admin",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("guideme").canUse(source)==(level>=3),role+" has correct targeted guide/item tool access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("listhomes").getChildren().stream().allMatch(node->node.canUse(source)==(level>=2)),role+" has correct other-player home-list access",passed);
                    SelfTest.check(dispatcher.getRoot().getChild("gamemode").canUse(source)==(level>=1)&&dispatcher.getRoot().getChild("staffmode").canUse(source)==(level>=1),role+" has correct self-only game-mode access",passed);
                    if(level>=1){
                        var parsed=dispatcher.parse("gamemode creative @a",source);
                        SelfTest.check(parsed.getReader().canRead()==(level<4),role+" has correct targeted game-mode access",passed);
                        dispatcher.execute("staffmode creative",source);
                        SelfTest.check(player.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.CREATIVE,role+" can change own game mode",passed);
                        dispatcher.execute("gamemode survival",source);
                        SelfTest.check(player.gameMode.getGameModeForPlayer()==net.minecraft.world.level.GameType.SURVIVAL,role+" native /gamemode changes self",passed);
                        SelfTest.check(dispatcher.parse("gm creative @a",source).getReader().canRead(),role+" self-only alias rejects a target",passed);
                    }
                } finally {if(!role.equals("player"))rank.remove(profile);}
            }
        }finally {player.setGameMode(mode);ServerTools.reconcile(player);}
    }
}
