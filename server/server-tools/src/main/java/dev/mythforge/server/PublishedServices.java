package dev.mythforge.server;

import com.alessandro.astages.api.constant.AEventPhase;
import com.alessandro.astages.api.event.AddStageEvent;
import com.alessandro.astages.api.event.AddRestrictionEvent;
import com.alessandro.astages.api.holder.AHolder;
import com.alessandro.astages.api.util.AStagesUtils;
import com.alessandro.astages.api.util.ARestrictionUtils;
import dk.magnusjensen.simpleafk.AFKManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.EntityBlock;
import java.nio.file.Files;
import java.nio.file.Path;
import com.google.gson.JsonParser;

/** Entitlement/activity adapters; published mods own restrictions and AFK presentation. */
final class PublishedServices {
    static final String MACHINERY="mythforge_machinery";
    static int restrictedMachines;
    static void stages(AddStageEvent event) {
        if(event.getEventPhase()==AEventPhase.RELOAD_STARTED)
            AStagesUtils.customizeStage(MACHINERY,"Machinery access");
    }
    static void restrictions(AddRestrictionEvent event) {
        if(event.getEventPhase()!=AEventPhase.RELOAD_STARTED)return;
        try {
            var config=JsonParser.parseString(Files.readString(Path.of("config/mythforge-server.json"))).getAsJsonObject();
            var extra=config.getAsJsonArray("functionalBlocks").asList().stream().map(v->v.getAsString()).toList();
            restrictedMachines=0;
            for(var block:BuiltInRegistries.BLOCK) {
                var id=BuiltInRegistries.BLOCK.getKey(block);
                if(ServerTools.decorative(id) || !ServerTools.machineryNamespace(id.getNamespace()) || !(block instanceof EntityBlock || extra.contains(id.toString())) || block.asItem()==Items.AIR)continue;
                ARestrictionUtils.addRestrictionForItem("mythforge_"+id.getNamespace()+"_"+id.getPath(),MACHINERY,block.asItem())
                    .allowPickup().allowInventoryStorage().allowContainerStorage().allowMining()
                    .allowAttack().allowEquip().allowLeftClick().showInRecipeViewer().globalShowName()
                    .disableBlockInteraction();
                restrictedMachines++;
            }
            ServerTools.LOG.info("[Mythforge] AStages protects {} functional machinery items; crafting, mining and trading remain available",restrictedMachines);
        } catch(Exception error) {throw new IllegalStateException("Cannot initialize machinery restrictions",error);}
    }
    static void reconcile(ServerPlayer player) {
        var holder=AHolder.player(player.getUUID());
        boolean allowed=ServerTools.hours(player)>=30 || ServerTools.tier(player)>0;
        if(allowed && !AStagesUtils.hasStage(holder,MACHINERY)) AStagesUtils.addStage(holder,MACHINERY,false,false,false);
        else if(!allowed && AStagesUtils.hasStage(holder,MACHINERY)) AStagesUtils.removeStage(holder,MACHINERY,false,false,false);
    }
    static boolean afk(ServerPlayer player) {
        var tracked=AFKManager.getInstance().getPlayer(player.getUUID());
        return tracked!=null && tracked.isAfk();
    }
    static void kick(ServerPlayer player) {
        var tracked=AFKManager.getInstance().getPlayer(player.getUUID());
        if(tracked!=null)tracked.kickPlayer();
    }
}
