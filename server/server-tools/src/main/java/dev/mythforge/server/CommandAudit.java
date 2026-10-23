package dev.mythforge.server;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import java.nio.file.*;
import java.util.*;

final class CommandAudit {
    static void write() {
        var original=ServerTools.state;
        var temporary=new State();temporary.path=Path.of("ops/private/command-audit-state.json");
        var uuid=UUID.fromString("c8eace22-bb00-4aa0-9000-000000000019");
        var player=new SelfTest.TestPlayer(ServerTools.server.overworld(),new GameProfile(uuid,"MF_CommandAudit"));
        Map<String,Map<String,Object>> rows=new TreeMap<>();
        try {
            ServerTools.state=temporary;
            for(String role:List.of("player","junior_mod","moderator","admin")) {
                var rank=FTBRanksAPI.manager().getRank(role).orElseThrow();
                if(!role.equals("player"))rank.add(player.getGameProfile());
                try {
                    ServerTools.reconcile(player);
                    for(var node:ServerTools.server.getCommands().getDispatcher().getRoot().getChildren()) {
                        var row=rows.computeIfAbsent(node.getName(),key->new LinkedHashMap<>());
                        row.put(role,node.canUse(player.createCommandSourceStack()));
                        row.put("usage",node.getUsageText());
                        row.put("subcommands",node.getChildren().stream().map(CommandNode::getUsageText).toList());
                        branches(node,node.getName(),role,player.createCommandSourceStack(),true,rows);
                    }
                } finally {if(!role.equals("player"))rank.remove(player.getGameProfile());}
            }
            Files.writeString(Path.of("ops/evidence/command-access.json"),State.JSON.toJson(rows));
            for(String role:List.of("moderator","admin")) {
                var lines=new ArrayList<String>();lines.add("MYTHFORGE - "+role.toUpperCase(Locale.ROOT)+" COMMAND ACCESS");
                lines.add("Generated from the loaded command dispatcher and role permissions. Angle brackets are required arguments.");
                lines.add("Includes inherited player commands. Reward commands still check playtime/rank when run. Self-only restrictions still apply.");
                rows.forEach((path,row)->{if(Boolean.TRUE.equals(row.get(role)) && Boolean.TRUE.equals(row.get("executable")))lines.add("/"+path);});
                Files.write(Path.of("ops/"+role.toUpperCase(Locale.ROOT)+"-COMMANDS.txt"),lines);
            }
        } catch(Exception error){throw new IllegalStateException("Command access audit failed",error);}
        finally {
            FTBRanksAPI.manager().deleteRank("earned_"+uuid.toString().replace("-",""));
            com.alessandro.astages.api.util.AStagesUtils.removeStage(com.alessandro.astages.api.holder.AHolder.player(uuid),PublishedServices.MACHINERY,false,false,false);
            ServerTools.state=original;
        }
    }
    static void branches(CommandNode<CommandSourceStack> node,String path,String role,CommandSourceStack source,boolean parent,Map<String,Map<String,Object>> rows) {
        boolean allowed=parent&&node.canUse(source);
        var row=rows.computeIfAbsent(path,key->new LinkedHashMap<>());
        row.put(role,allowed);row.put("executable",node.getCommand()!=null);
        for(var child:node.getChildren())branches(child,path+" "+child.getUsageText(),role,source,allowed,rows);
    }
}
