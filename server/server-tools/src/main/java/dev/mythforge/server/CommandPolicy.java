package dev.mythforge.server;

import com.mojang.brigadier.tree.CommandNode;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;
import java.util.*;
import java.util.function.Predicate;

/** Explicit role boundaries apply even to roots registered after FTB's command scan. */
public final class CommandPolicy {
    static final Set<String> PUBLIC=Set.of("help","list","msg","tell","w","me","teammsg","tm","home","sethome","delhome","homes","listhomes","back","spawn","warp","listwarps","tpa","tpaccept","tpdeny","tpahere","rewards","pvp","crate","vote","rtp","ascend","feed","heal","fly","repair","effects","grindstone","pwarp","waypoint","prefix","rename","lore","joinmessage","worldtime","worldweather","ftbteams","ftbchunks","chunks","ftbquests","ftbultimine","kickme","rec","recording","afk","relic","chatcolor","ptime","pweather","particles","modlist","leaderboard","emi","trigger");
    static final Set<String> EARNED=Set.of("craft","crafting","stonecutter","nick","nickname","echest","enderchest","anvil","smithing","trashcan","trash","disposal","hat");
    static final Set<String> JUNIOR=Set.of("kick","mute","unmute","gamemode","staffmode","gm","grieflogger","gl");
    static final Set<String> MODERATOR=Set.of("ban","banlist","pardon","near","deathhistory");
    static final Set<String> ADMIN=Set.of("jade","open-patchouli-book","guideme","createbypass","invsee","spark","observable","ban-ip","pardon-ip","whitelist","tp","teleport","tpo","tpx","teleport_last","tp_offline","setwarp","delwarp","setworldspawn","spawnpoint","spectate","give","clear","enchant","effect","attribute","experience","xp","kill","summon","setblock","fill","clone","place","particle","playsound","stopsound","tellraw","title","say","time","weather","difficulty","worldborder","gamerule","advancement","recipe","forceload","neruina","chunky","lootr","tb","curios","accessories","fluxnetworks","ae2","sable","simulated","create","c","automated_logistics","aal","waterframes");
    public static boolean owner(ServerPlayer player){
        if(ServerTools.server==null)return false;
        var manager=FTBRanksAPI.manager();
        return manager.getAddedRanks(player.getGameProfile()).stream().anyMatch(r->r.getId().equals("owner"))
            || manager.getRanks(player).stream().anyMatch(r->r.getId().equals("owner"));
    }
    static int level(ServerPlayer player){
        if(owner(player))return 4;
        var profile=player.getGameProfile();
        if(ServerTools.server.getPlayerList().isOp(profile))return 3;
        var ranks=new HashSet<>(FTBRanksAPI.manager().getAddedRanks(profile));
        ranks.addAll(FTBRanksAPI.manager().getRanks(player));
        if(ranks.stream().anyMatch(r->Set.of("admin","owner").contains(r.getId())))return 3;
        if(ranks.stream().anyMatch(r->r.getId().equals("moderator")))return 2;
        return ranks.stream().anyMatch(r->r.getId().equals("junior_mod"))?1:0;
    }
    static int minimum(String root){
        if(PUBLIC.contains(root)||EARNED.contains(root))return 0;
        if(JUNIOR.contains(root))return 1;
        if(MODERATOR.contains(root))return 2;
        return ADMIN.contains(root)?3:4;
    }
    static int minimumPath(String root,String path){
        if(root.equals("ftbquests"))return path.isEmpty()||path.startsWith("open_book")?0:3;
        if(Set.of("ftbchunks","chunks").contains(root)&&path.startsWith("admin"))return 3;
        if(root.equals("ftbteams")&&(path.matches("^(server|force-disband|force-add|force-remove|nbtedit|teamstage|serverconfig)( .*|$)")||path.contains("settings_for")||path.contains("transfer_ownership_for")))return 3;
        if(root.equals("ftbultimine")&&(path.startsWith("serverconfig")||path.startsWith("config")))return 3;
        if(root.equals("afk")&&(path.startsWith("bypass-list")||path.startsWith("toggle ")))return 3;
        if(root.equals("recording")&&!path.isEmpty())return 3;
        if(root.equals("listhomes")&&!path.isEmpty())return 2;
        if(root.equals("chunky")&&path.startsWith("trim"))return 4;
        if(root.equals("gl")&&(path.startsWith("rollback")||path.startsWith("web")||path.startsWith("config")))return 3;
        if(root.equals("gamemode")&&Arrays.asList(path.replace("<","").replace(">","").split(" ")).contains("targets"))return 4;
        return minimum(root);
    }
    static void install(){
        var roots=ServerTools.server.getCommands().getDispatcher().getRoot().getChildren();
        for(String role:List.of("player","junior_mod","moderator","admin","owner")){
            var rank=FTBRanksAPI.manager().getRank(role).orElseThrow();
            int level=switch(role){case "junior_mod"->1;case "moderator"->2;case "admin"->3;case "owner"->4;default->0;};
            if(level==4){ServerTools.setNode(rank,"command",true);ServerTools.setNode(rank,"ftbranks.permission_level",4);}
            for(var root:roots)permissions(rank,root,root.getName(),"",level);
        }
        try {
            var field=CommandNode.class.getDeclaredField("requirement");field.setAccessible(true);
            for(var root:roots)wrap(root,root.getName(),"",field);
        }catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot enforce command role boundaries",error);}
    }
    static void permissions(dev.ftb.mods.ftbranks.api.Rank rank,CommandNode<CommandSourceStack> node,String root,String path,int level){
        // Per-player earned nodes continue to own utility unlocks.
        if(level==4||!EARNED.contains(root))ServerTools.setNode(rank,"command."+root+(path.isEmpty()?"":"."+path.replace(' ','.').replaceAll("[<>]","")),level>=minimumPath(root,path));
        for(var child:node.getChildren())permissions(rank,child,root,path.isEmpty()?child.getName():path+" "+child.getName(),level);
    }
    @SuppressWarnings("unchecked")
    static void wrap(CommandNode<CommandSourceStack> node,String root,String path,java.lang.reflect.Field field) throws IllegalAccessException {
        var original=(Predicate<CommandSourceStack>)field.get(node);
        int minimum=minimumPath(root,path);
        field.set(node,(Predicate<CommandSourceStack>)source->{
            if(!(source.getEntity() instanceof ServerPlayer player))return original.test(source);
            return owner(player)||minimum<4&&level(player)>=minimum&&original.test(source);
        });
        for(var child:node.getChildren())wrap(child,root,path.isEmpty()?child.getUsageText():path+" "+child.getUsageText(),field);
    }
    static boolean guard(CommandEvent event,ServerPlayer player,String root,String[] words){
        if(owner(player))return false;
        if(minimum(root)>level(player)){
            event.setCanceled(true);ServerTools.tell(player,minimum(root)==4?"This maintenance command is console-only.":"Your staff role does not have access to this command.");return true;
        }
        // The native /gamemode path is self-only, including for operators.
        if(root.equals("gamemode")&&words.length!=2){event.setCanceled(true);ServerTools.tell(player,"Game mode changes are self-only. Use /gamemode <mode> or /staffmode <mode>, without a target.");return true;}
        return false;
    }
}
