package dev.mythforge.server;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.ItemLore;
import java.util.List;

/** Vanilla read-only menu: custom warp icons are visible and click through the normal visit command. */
final class WarpMenu extends ChestMenu {
    final List<String> names;
    final int page;
    WarpMenu(int id,Inventory inventory,int requestedPage) {
        super(MenuType.GENERIC_9x6,id,inventory,new SimpleContainer(54),6);
        names=ServerTools.state.warps.keySet().stream().sorted().toList();
        page=Math.max(0,Math.min(requestedPage,Math.max(0,(names.size()-1)/45)));
        for(int slot=0;slot<45 && page*45+slot<names.size();slot++) {
            String name=names.get(page*45+slot);var warp=ServerTools.state.warps.get(name);
            var key=ResourceLocation.tryParse(warp.icon);
            Item icon=key==null?Items.ENDER_PEARL:BuiltInRegistries.ITEM.get(key);
            if(icon==Items.AIR)icon=Items.ENDER_PEARL;
            var stack=new ItemStack(icon);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));
            stack.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(warp.dimension),Component.literal("Click to visit. Movement or combat cancels travel."))));
            getContainer().setItem(slot,stack);
        }
        if(names.isEmpty())getContainer().setItem(22,label(Items.MAP,"No public warps yet"));
        if(page>0)getContainer().setItem(45,label(Items.ARROW,"Previous page"));
        getContainer().setItem(49,label(Items.BARRIER,"Close"));
        var help=label(Items.PAPER,"Manage your public warps");
        help.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("/pwarp create <name>"),Component.literal("/pwarp delete <name>"),Component.literal("/pwarp icon <name> <item>"))));
        getContainer().setItem(50,help);
        if((page+1)*45<names.size())getContainer().setItem(53,label(Items.ARROW,"Next page"));
    }
    static ItemStack label(Item item,String name) {var stack=new ItemStack(item);stack.set(DataComponents.CUSTOM_NAME,Component.literal(name));return stack;}
    static void open(ServerPlayer player,int page) {player.openMenu(new SimpleMenuProvider((id,inventory,actor)->new WarpMenu(id,inventory,page),Component.literal("Public warps")));}
    @Override public boolean stillValid(Player player) {return true;}
    @Override public ItemStack quickMoveStack(Player player,int slot) {return ItemStack.EMPTY;}
    @Override public void clicked(int slot,int button,ClickType type,Player player) {
        if(!(player instanceof ServerPlayer actor))return;
        if(type!=ClickType.PICKUP || button!=0){broadcastFullState();return;}
        if(slot==49){actor.closeContainer();return;}
        if(slot==45 && page>0){open(actor,page-1);return;}
        if(slot==53 && (page+1)*45<names.size()){open(actor,page+1);return;}
        if(slot>=0 && slot<45 && page*45+slot<names.size()) {
            String name=names.get(page*45+slot);actor.closeContainer();
            actor.server.getCommands().performPrefixedCommand(actor.createCommandSourceStack(),"pwarp visit "+name);return;
        }
        broadcastFullState();
    }
}
