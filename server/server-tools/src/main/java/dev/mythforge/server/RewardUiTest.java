package dev.mythforge.server;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import java.util.List;

final class RewardUiTest {
 static void run(List<String> passed)throws Exception {
  var p=new SelfTest.TestPlayer(ServerTools.server.overworld(),new com.mojang.authlib.GameProfile(java.util.UUID.fromString("77770000-1234-4567-9876-123456789abc"),"RewardUiTest"));
  ServerTools.reconcile(p);
  for(String command:List.of("trashcan","trash","disposal"))SelfTest.check(FTBRanksAPI.getPermissionValue(p,"command."+command).asBooleanOrFalse(),"Zero-hour non-donor can use disposal: "+command,passed);
  SelfTest.check(ServerTools.server.getCommands().getDispatcher().getRoot().getChild("trashcan").canUse(p.createCommandSourceStack()),"Inventory sidebar /trashcan command is available",passed);
  var spec=new com.google.gson.JsonObject();spec.addProperty("id","minecraft:paper");spec.addProperty("count",1);spec.addProperty("custom","relic_choice");
  var voucher=Rewards.stack(p,spec);p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,voucher);
  RelicChoiceMenu.open(p,0);SelfTest.check(p.containerMenu instanceof RelicChoiceMenu,"Voucher opens vanilla-compatible choice menu",passed);
  var menu=(RelicChoiceMenu)p.containerMenu;String choice=menu.choices.getFirst();var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(choice));int before=p.getInventory().countItem(item);
  menu.clicked(0,0,ClickType.QUICK_MOVE,p);menu.clicked(0,0,ClickType.THROW,p);menu.clicked(0,0,ClickType.SWAP,p);
  SelfTest.check(voucher.getCount()==1 && p.getInventory().countItem(item)==before,"Menu shift-click, drop and hotbar swap cannot steal display relics",passed);
  menu.clicked(0,0,ClickType.PICKUP,p);
  SelfTest.check(voucher.isEmpty() && p.getInventory().countItem(item)==before && ServerTools.data(p).pending.size()==1,"Choice consumes a voucher and queues the relic without automatic delivery",passed);
  String grant=com.google.gson.JsonParser.parseString(ServerTools.data(p).pending.getFirst()).getAsJsonObject().get("grant").getAsString();
  RewardMenu.open(p,0,true);var rewards=(RewardMenu)p.containerMenu;
  rewards.clicked(0,0,ClickType.QUICK_MOVE,p);rewards.clicked(0,0,ClickType.THROW,p);rewards.clicked(0,0,ClickType.SWAP,p);
  SelfTest.check(ServerTools.data(p).pending.size()==1&&p.getInventory().countItem(item)==before,"Reward menu rejects shift-click, drop and hotbar theft",passed);
  rewards.clicked(0,0,ClickType.PICKUP,p);
  SelfTest.check(p.getInventory().countItem(item)==before+1&&ServerTools.data(p).pending.isEmpty(),"Explicit click claims one earned reward",passed);
  Rewards.claim(p,grant);SelfTest.check(p.getInventory().countItem(item)==before+1,"Replayed grant cannot duplicate the reward",passed);
  menu.clicked(0,0,ClickType.PICKUP,p);SelfTest.check(p.getInventory().countItem(item)==before+1,"Replayed menu click cannot duplicate rewards",passed);
  SelfTest.check(ServerTools.data(p).pending.isEmpty(),"Chosen relic is delivered without leaving duplicate mail",passed);
  p.closeContainer();
 }
}
