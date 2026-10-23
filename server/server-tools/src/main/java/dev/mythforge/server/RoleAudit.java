package dev.mythforge.server;
import com.mojang.authlib.GameProfile;
import java.nio.file.*;
import java.util.*;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;

final class RoleAudit {
 static void run(){
  var original=ServerTools.state;var test=new State();test.path=Path.of("ops/private/roleaudit-state.json");
  var passed=new ArrayList<String>();var failures=new ArrayList<String>();
  var profile=new GameProfile(UUID.fromString("adc8b684-672a-4fd3-9b2b-401e80f2a6b8"),"MF_Test");
  var player=new SelfTest.TestPlayer(ServerTools.server.overworld(),profile);
  try{
   Files.createDirectories(Path.of("ops/private"));ServerTools.state=test;
   RoleBenefitsTest.run(player,passed);
   int expectedTier=0;
   for(String role:List.of("ember","artisan","runeforged","mythic")){
    expectedTier++;test.purchases.clear();test.purchases.put("roleaudit_paid",new State.Purchase(profile.getId().toString(),role));
    ServerTools.reconcile(player);
    SelfTest.check(ServerTools.tier(player)==expectedTier,role+" grants its configured benefit tier",passed);
    SelfTest.check(dev.ftb.mods.ftbranks.api.FTBRanksAPI.manager().getAddedRanks(profile).stream().anyMatch(r->r.getId().equals(role)),role+" activates the matching rank",passed);
    SelfTest.check(CommandPolicy.level(player)==0&&!ServerTools.server.getCommands().getDispatcher().getRoot().getChild("give").canUse(player.createCommandSourceStack()),role+" does not grant staff commands",passed);
   }
   test.purchases.clear();ServerTools.reconcile(player);
   test.player(profile.getId()).activeSeconds=500*3600L;
   CommandPolicyTest.run(player,passed);
   LifecycleTest.run(player,passed);
   var newPlayer=new ServerPlayer(ServerTools.server,ServerTools.server.overworld(),new GameProfile(UUID.fromString("f70c00ad-c7c0-4c8c-85f8-2a5631a3dab6"),"MF_NewPlayer"),ClientInformation.createDefault());
   SelfTest.check(newPlayer.serverLevel().dimension().equals(Level.OVERWORLD),"A new player starts in the new Overworld",passed);
   SelfTest.check(newPlayer.getRespawnDimension().equals(Level.OVERWORLD),"A new player's default respawn dimension is the Overworld",passed);
   var spawn=newPlayer.blockPosition();
   ServerTools.LOG.info("Role audit actual new-player spawn: {} in {}",spawn,newPlayer.serverLevel().dimension().location());
   var level=ServerTools.server.overworld();level.getChunkAt(spawn);
   SelfTest.check(level.getWorldBorder().isWithinBounds(spawn),"New-player spawn lies inside the new Overworld border",passed);
   boolean safeLanding=false;
   // This pack places a fresh player slightly above ground. Validate the entire
   // landing path, including the maximum harmless three-block drop.
   for(int drop=0;drop<=3;drop++) {
    var feet=spawn.below(drop);
    if(!level.getBlockState(feet).getCollisionShape(level,feet).isEmpty()||!level.getBlockState(feet.above()).getCollisionShape(level,feet.above()).isEmpty()||!level.getFluidState(feet).isEmpty())break;
    var floor=level.getBlockState(feet.below());
    if(floor.isCollisionShapeFullBlock(level,feet.below())&&floor.getFluidState().isEmpty()&&!floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)&&!floor.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)) {
     safeLanding=true;ServerTools.LOG.info("Role audit safe landing: {} (drop {} blocks)",feet,drop);break;
    }
   }
   SelfTest.check(safeLanding,"New-player spawn has a clear landing path with no damaging fall",passed);
  }catch(Exception error){failures.add(error.toString());ServerTools.LOG.error("Role audit failed",error);}
  finally{
   ServerTools.state=original;
   for(String role:List.of("junior_mod","moderator","admin","owner","beta_tester","ember","artisan","runeforged","mythic"))dev.ftb.mods.ftbranks.api.FTBRanksAPI.manager().getRank(role).ifPresent(r->r.remove(profile));
   String earned="earned_"+profile.getId().toString().replace("-","");
   if(dev.ftb.mods.ftbranks.api.FTBRanksAPI.manager().getRank(earned).isPresent())dev.ftb.mods.ftbranks.api.FTBRanksAPI.manager().deleteRank(earned);
  }
  try{Files.writeString(Path.of("ops/evidence/roleaudit.json"),State.JSON.toJson(Map.of("passed",passed,"failures",failures,"success",failures.isEmpty())));}catch(Exception error){throw new RuntimeException(error);}
  ServerTools.LOG.info("Role/spawn audit: {} passed, {} failed",passed.size(),failures.size());
 }
}
