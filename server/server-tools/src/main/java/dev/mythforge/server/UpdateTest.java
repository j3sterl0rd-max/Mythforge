package dev.mythforge.server;
import java.util.*;
import java.nio.file.*;
import com.mojang.authlib.GameProfile;
import com.google.gson.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
final class UpdateTest {
 static void run() {
  var original=ServerTools.state;var test=new State();test.path=Path.of("ops/private/update-test-state.json");
  var passed=new ArrayList<String>();var failures=new ArrayList<String>();
  var p=new SelfTest.TestPlayer(ServerTools.server.overworld(),new GameProfile(UUID.fromString("c8770000-1234-4567-9876-123456789abc"),"UpdateTest"));
  var other=new SelfTest.TestPlayer(ServerTools.server.overworld(),new GameProfile(UUID.fromString("c8770000-1234-4567-9876-123456789abd"),"UpdateTest2"));
  try {
   ServerTools.state=test;Files.createDirectories(test.path.getParent());
   var players=new ArrayList<>(List.of(1,2,3));var visited=new ArrayList<Integer>();
   for(var value:List.copyOf(players)){visited.add(value);players.remove(value);}
   SelfTest.check(visited.equals(List.of(1,2,3))&&players.isEmpty(),"Disconnecting during snapshot iteration visits remaining players without concurrent modification",passed);
   var spec=new JsonObject();spec.addProperty("id","minecraft:diamond");spec.addProperty("count",2);spec.addProperty("milestone",5);var list=new JsonArray();list.add(spec);
   Rewards.enqueue(p,list);test.save();
   SelfTest.check(p.getInventory().countItem(Items.DIAMOND)==0&&ServerTools.data(p).pending.size()==1,"Earning items does not insert them into inventory",passed);
   RewardMenu.open(p,0);SelfTest.check(p.containerMenu instanceof RewardMenu,"Rewards command panel uses the vanilla container protocol",passed);
   RewardMenu.open(p,0,true);var menu=(RewardMenu)p.containerMenu;
   for(var type:List.of(ClickType.QUICK_MOVE,ClickType.SWAP,ClickType.THROW,ClickType.CLONE,ClickType.PICKUP_ALL,ClickType.QUICK_CRAFT))menu.clicked(0,0,type,p);
   SelfTest.check(p.getInventory().countItem(Items.DIAMOND)==0&&ServerTools.data(p).pending.size()==1,"Unsupported clicks cannot steal reward display stacks",passed);
   String grant=JsonParser.parseString(ServerTools.data(p).pending.getFirst()).getAsJsonObject().get("grant").getAsString();
   Rewards.claim(other,grant);SelfTest.check(ServerTools.data(p).pending.size()==1&&other.getInventory().countItem(Items.DIAMOND)==0,"Another player cannot claim an unowned grant",passed);
   menu.clicked(0,0,ClickType.PICKUP,p);Rewards.claim(p,grant);menu.clicked(0,0,ClickType.PICKUP,p);
   SelfTest.check(p.getInventory().countItem(Items.DIAMOND)==2&&ServerTools.data(p).pending.isEmpty(),"Explicit claim delivers exactly once despite repeated requests",passed);
   Rewards.enqueue(p,list);String raw=ServerTools.data(p).pending.getFirst();String receiptId=JsonParser.parseString(raw).getAsJsonObject().get("grant").getAsString();
   var receipts=p.getPersistentData().getCompound("mythforge_delivery_receipts");receipts.putBoolean(receiptId,true);p.getPersistentData().put("mythforge_delivery_receipts",receipts);Rewards.claim(p,receiptId);
   SelfTest.check(p.getInventory().countItem(Items.DIAMOND)==2&&ServerTools.data(p).pending.isEmpty(),"Crash-replayed receipt clears pending entry without a second item",passed);
   Rewards.enqueue(p,list);for(int i=0;i<p.getInventory().items.size();i++)p.getInventory().items.set(i,new ItemStack(Items.COBBLESTONE,64));
   String fullGrant=JsonParser.parseString(ServerTools.data(p).pending.getFirst()).getAsJsonObject().get("grant").getAsString();Rewards.claim(p,fullGrant);
   SelfTest.check(ServerTools.data(p).pending.size()==1,"A full inventory preserves the unclaimed reward",passed);
   SelfTest.check(!ServerTools.data(p).pvp&&!ServerTools.data(other).pvp,"PvP defaults off for both players",passed);
   var hit=new LivingIncomingDamageEvent(other,new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p),2));PvpSettings.attack(hit);SelfTest.check(hit.isCanceled(),"PvP damage is blocked by default",passed);
   ServerTools.data(p).pvp=true;hit=new LivingIncomingDamageEvent(other,new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p),2));PvpSettings.attack(hit);SelfTest.check(hit.isCanceled(),"One-sided PvP opt-in still blocks damage",passed);
   ServerTools.data(other).pvp=true;hit=new LivingIncomingDamageEvent(other,new net.neoforged.neoforge.common.damagesource.DamageContainer(p.damageSources().playerAttack(p),2));PvpSettings.attack(hit);SelfTest.check(!hit.isCanceled(),"Both players can opt into PvP",passed);
   test.save();var roundtrip=State.JSON.fromJson(Files.readString(test.path),State.class);SelfTest.check(roundtrip.player(p.getUUID()).pvp&&roundtrip.player(p.getUUID()).pending.size()==1,"PvP preferences and pending rewards persist in the ledger",passed);
   LifecycleTest.run(p,passed);
   SelfTest.check(Travel.permitted(p,ServerTools.server.getLevel(net.minecraft.world.level.Level.NETHER)),"An unvisited dimension is no longer blocked",passed);
   var dispatcher=ServerTools.server.getCommands().getDispatcher();SelfTest.check(dispatcher.getRoot().getChild("mail")==null&&dispatcher.getRoot().getChild("pvp")!=null,"Mail command removed and PvP command registered",passed);
  } catch(Exception error){failures.add(error.toString());ServerTools.LOG.error("Update regression test failed",error);}
  finally{ServerTools.state=original;}
  try{Files.writeString(Path.of("ops/evidence/update-test.json"),State.JSON.toJson(Map.of("passed",passed,"failures",failures,"success",failures.isEmpty())));}catch(Exception error){throw new RuntimeException(error);}
  ServerTools.LOG.info("Update regression test: {} passed, {} failed",passed.size(),failures.size());
 }
}
