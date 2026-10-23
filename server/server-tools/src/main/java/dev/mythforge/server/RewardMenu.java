package dev.mythforge.server;

import com.google.gson.JsonParser;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.*;

/** Display stacks never leave this menu. Claims revalidate the persisted grant on the server. */
final class RewardMenu extends ChestMenu {
    final int page; final boolean inbox; final List<String> entries;
    RewardMenu(int id,Inventory inv,int requested,boolean inbox) {
        super(MenuType.GENERIC_9x6,id,inv,new SimpleContainer(54),6);
        this.inbox=inbox;var player=(ServerPlayer)inv.player;var data=ServerTools.data(player);
        entries=inbox?List.copyOf(data.pending):ServerTools.config.getAsJsonObject("milestones").keySet().stream().sorted(Comparator.comparingInt(Integer::parseInt)).toList();
        page=Math.max(0,Math.min(requested,Math.max(0,(entries.size()-1)/45)));
        for(int i=0;i<45 && page*45+i<entries.size();i++) {
            String entry=entries.get(page*45+i);ItemStack icon;var lore=new ArrayList<Component>();
            if(inbox) {
                var spec=JsonParser.parseString(entry).getAsJsonObject();icon=Rewards.stack(player,spec);
                lore.add(Component.literal(spec.has("milestone")?"Earned at "+spec.get("milestone").getAsInt()+" active hours":"Unclaimed reward"));
                lore.add(Component.literal("Click to claim. Requires an empty inventory slot."));
            } else {
                int hours=Integer.parseInt(entry);boolean earned=ServerTools.hours(player)>=hours;
                icon=WarpMenu.label(earned?Items.EXPERIENCE_BOTTLE:Items.CLOCK,hours+" active hours - "+(earned?"Unlocked":"Locked"));
                for(var raw:ServerTools.config.getAsJsonObject("milestones").getAsJsonArray(entry)) {
                    var spec=raw.getAsJsonObject();
                    if(spec.has("id"))lore.add(Component.literal(spec.get("count").getAsInt()+" x ").append(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.get("id").getAsString())).getDescription()));
                }
                String unlock=unlocks(hours);if(!unlock.isEmpty())lore.add(Component.literal(unlock));
                lore.add(Component.literal(earned?"Item rewards are in Unclaimed rewards, or already claimed.":"Keep playing actively to unlock this milestone."));
            }
            icon.set(DataComponents.LORE,new ItemLore(lore));getContainer().setItem(i,icon);
        }
        if(entries.isEmpty())getContainer().setItem(22,WarpMenu.label(Items.GLASS_PANE,"No unclaimed rewards"));
        if(page>0)getContainer().setItem(45,WarpMenu.label(Items.ARROW,"Previous page"));
        getContainer().setItem(47,WarpMenu.label(Items.CHEST,"Unclaimed rewards ("+data.pending.size()+")"));
        getContainer().setItem(48,WarpMenu.label(Items.CLOCK,"Playtime: "+data.activeSeconds/3600+"h "+data.activeSeconds/60%60+"m"));
        var clock=getContainer().getItem(48);
        clock.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Only active playtime earns progress."),Component.literal("Base: 64 claim chunks and 2 force-loadable chunks."),Component.literal("+8 claim chunks every 10 active hours; +1 force-load every 50 hours."),Component.literal("Purchased allowances add to natural limits."))));
        getContainer().setItem(49,WarpMenu.label(Items.BARRIER,"Close"));
        getContainer().setItem(50,WarpMenu.label(Items.BOOK,"All playtime milestones"));
        getContainer().setItem(51,WarpMenu.label(Items.TRIPWIRE_HOOK,"Vote keys: "+data.keys+" - /vote and /crate"));
        if((page+1)*45<entries.size())getContainer().setItem(53,WarpMenu.label(Items.ARROW,"Next page"));
    }
    static String unlocks(int h) {return switch(h) {
        case 10 -> "2 natural homes"; case 20 -> "/stonecutter"; case 30 -> "Machinery access; /craft";
        case 40 -> "/nick; 1 natural public warp; Nether RTP at "+RtpUnlocks.NETHER_HOURS+"h"; case 80 -> "End RTP at "+RtpUnlocks.END_HOURS+"h"; case 70 -> "3 natural homes"; case 90 -> "/echest";
        case 100 -> "/ascend; 2 natural public warps"; case 120 -> "/feed; +1 hands equipment slot";
        case 150 -> "/grindstone; waypoint teleporting; +1 necklace slot"; case 175 -> "/heal; /effects";
        case 200 -> "4 natural homes; claim-limited /fly; +1 trinket slot"; case 250 -> "/prefix; /chatcolor; 3 natural public warps";
        case 300 -> "/rename; /anvil; /smithing; /pwarp icon; /ptime; /pweather; second bonus hands slot";
        case 400 -> "/lore; /joinmessage; /worldtime; /worldweather; /hat; /particles; 4 natural public warps";
        case 500 -> "/repair; priority login; prestige title; 5 natural homes and public warps";
        default -> "";
    };}
    static void open(ServerPlayer p,int page){open(p,page,false);}
    static void open(ServerPlayer p,int page,boolean inbox){p.openMenu(new SimpleMenuProvider((id,inv,actor)->new RewardMenu(id,inv,page,inbox),Component.literal(inbox?"Unclaimed rewards":"Playtime rewards")));}
    @Override public boolean stillValid(Player p){return p instanceof ServerPlayer && !p.isDeadOrDying();}
    @Override public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(!(p instanceof ServerPlayer actor))return;
        if(type!=ClickType.PICKUP||button!=0){broadcastFullState();return;}
        if(slot==49){actor.closeContainer();return;}
        if(slot==47){open(actor,0,true);return;}if(slot==50){open(actor,0,false);return;}
        if(slot==45&&page>0){open(actor,page-1,inbox);return;}
        if(slot==53&&(page+1)*45<entries.size()){open(actor,page+1,inbox);return;}
        if(inbox&&slot>=0&&slot<45&&page*45+slot<entries.size()){
            String grant=JsonParser.parseString(entries.get(page*45+slot)).getAsJsonObject().get("grant").getAsString();
            Rewards.claim(actor,grant);open(actor,page,true);return;
        }
        broadcastFullState();
    }
}
