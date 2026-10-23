package dev.mythforge.server;
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
import java.util.List;

/** Vanilla container protocol: choices are display-only; redemption revalidates the held voucher. */
final class RelicChoiceMenu extends ChestMenu {
 final List<String> choices;final int page;
 RelicChoiceMenu(int id,Inventory inventory,int requested) {
  super(MenuType.GENERIC_9x6,id,inventory,new SimpleContainer(54),6);
  choices=Rewards.relicChoices();page=Math.max(0,Math.min(requested,Math.max(0,(choices.size()-1)/45)));
  for(int i=0;i<45 && page*45+i<choices.size();i++) {
   String key=choices.get(page*45+i);var icon=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(key)));
   icon.set(DataComponents.LORE,new ItemLore(List.of(Component.literal(key),Component.literal("Choose this maxed relic. Consumes one voucher."))));
   getContainer().setItem(i,icon);
  }
  if(page>0)getContainer().setItem(45,WarpMenu.label(Items.ARROW,"Previous page"));
  getContainer().setItem(49,WarpMenu.label(Items.BARRIER,"Cancel"));
  if((page+1)*45<choices.size())getContainer().setItem(53,WarpMenu.label(Items.ARROW,"Next page"));
 }
 static boolean voucher(ServerPlayer player) {
  var custom=player.getMainHandItem().get(DataComponents.CUSTOM_DATA);
  return !player.getMainHandItem().isEmpty() && custom!=null && custom.copyTag().getString("mythforge_consumable").equals("relic_choice");
 }
 static void open(ServerPlayer player,int page) {
  if(!voucher(player)){ServerTools.tell(player,"Hold your Relic Choice Voucher in your main hand.");return;}
  player.openMenu(new SimpleMenuProvider((id,inventory,actor)->new RelicChoiceMenu(id,inventory,page),Component.literal("Choose your maxed relic")));
 }
 @Override public boolean stillValid(Player player){return player instanceof ServerPlayer p && voucher(p);}
 @Override public ItemStack quickMoveStack(Player player,int slot){return ItemStack.EMPTY;}
 @Override public void clicked(int slot,int button,ClickType type,Player player) {
  if(!(player instanceof ServerPlayer actor))return;
  if(type!=ClickType.PICKUP || button!=0){broadcastFullState();return;}
  if(!voucher(actor)||slot==49){actor.closeContainer();return;}
  if(slot==45 && page>0){open(actor,page-1);return;}
  if(slot==53 && (page+1)*45<choices.size()){open(actor,page+1);return;}
  if(slot>=0 && slot<45 && page*45+slot<choices.size()) {
   String key=choices.get(page*45+slot);actor.closeContainer();
   Rewards.choose(actor,key);return;
  }
  broadcastFullState();
 }
}
