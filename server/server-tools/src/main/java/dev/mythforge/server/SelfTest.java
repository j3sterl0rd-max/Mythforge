package dev.mythforge.server;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbranks.api.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;
import java.nio.file.*;
import java.util.*;

final class SelfTest {
    // FakePlayer suppresses menu requests. Capture the server menu without simulating a client.
    static final class TestPlayer extends FakePlayer {
        TestPlayer(net.minecraft.server.level.ServerLevel level,GameProfile profile) {super(level,profile);}
        @Override public java.util.OptionalInt openMenu(net.minecraft.world.MenuProvider provider,java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> writer) {
            containerMenu=provider.createMenu(1,getInventory(),this);
            return containerMenu==null?java.util.OptionalInt.empty():java.util.OptionalInt.of(1);
        }
    }
    static void check(boolean condition,String message,List<String> passed) {if(!condition)throw new IllegalStateException(message);passed.add(message);}
    static void run() {
        List<String> passed=new ArrayList<>(),failures=new ArrayList<>();
        var original=ServerTools.state;var test=new State();test.path=Path.of("ops/private/selftest-state.json");
        UUID uuid=UUID.fromString("adc8b684-672a-4fd3-9b2b-401e80f2a6b8");
        var p=new TestPlayer(ServerTools.server.overworld(),new GameProfile(uuid,"MF_Test"));
        String earned="earned_"+uuid.toString().replace("-","");
        try {
            ServerTools.state=test;
            var data=test.player(uuid);data.activeSeconds=500*3600L;data.delivered.add(500);data.paidDelivered=3;
            check(Payments.purchase(uuid.toString().replace("-",""),"ember","selftest_ember"),"Accepts Tebex's compact Java UUID",passed);
            check(Payments.purchase(uuid.toString(),"runeforged","selftest_runeforged"),"Upgrade entitlement is recorded",passed);
            check(Payments.purchase(uuid.toString(),"runeforged","selftest_runeforged")&&test.purchases.size()==2,"Replayed purchase does not duplicate transactions",passed);
            ServerTools.reconcile(p);
            check(FTBRanksAPI.getPermissionValue(p,"ftbessentials.home.max").asInteger().orElse(0)==9,"Purchased home bonus adds to 500-hour natural allowance",passed);
            check(FTBRanksAPI.getPermissionValue(p,"mythforge.pwarp.max").asInteger().orElse(0)==8,"Purchased warp bonus adds to natural allowance",passed);
            check(FTBRanksAPI.getPermissionValue(p,"ftbchunks.max_claimed").asInteger().orElse(0)==592,"Paid claim bonus is added once to recurring earnings",passed);
            check(FTBRanksAPI.getPermissionValue(p,"ftbchunks.max_force_loaded").asInteger().orElse(0)==16,"Force-load entitlement combines base, earned and current rank",passed);
            check(dev.ftb.mods.ftbchunks.integration.PermissionsHelper.getMaxClaimedChunks(p,64)==592,"FTB Chunks consumes earned and paid claim limits through the integration bridge",passed);
            check(dev.ftb.mods.ftbchunks.integration.PermissionsHelper.getMaxForceLoadedChunks(p,2)==16,"FTB Chunks consumes the force-load limit through the integration bridge",passed);
            check(dev.ftb.mods.ftblibrary.integration.permissions.PermissionHelper.INSTANCE.getProvider().getIntegerPermission(p,"ftbessentials.home.max",1)==9,"The shared FTB provider exposes additive home limits to Essentials",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.give").asBooleanOrFalse(),"Supporters cannot use /give",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.enderchest.target").asBooleanOrFalse(),"Supporters cannot open another player's ender chest",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.ftbquests.editing_mode").asBooleanOrFalse(),"Players cannot edit quests",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.ftbteams.force-add").asBooleanOrFalse(),"Players cannot force team membership",passed);
            check(Payments.revoke("selftest_runeforged")&&test.paid(uuid)==1,"Refund downgrades to the remaining paid entitlement",passed);
            ServerTools.reconcile(p);
            check(FTBRanksAPI.getPermissionValue(p,"ftbessentials.home.max").asInteger().orElse(0)==7&&FTBRanksAPI.getPermissionValue(p,"mythforge.pwarp.max").asInteger().orElse(0)==6,"Refund preserves natural allowances plus remaining rank bonuses",passed);
            check(data.activeSeconds==500*3600L&&data.delivered.contains(500)&&data.paidDelivered==3,"Refund preserves earned time and one-time delivery receipts",passed);
            Payments.revoke("selftest_ember");
            check(Payments.revoke("selftest_early_refund") && Payments.purchase(uuid.toString(),"mythic","selftest_early_refund") && test.paid(uuid)==0,"Refund before purchase creates a durable revocation tombstone",passed);
            data.activeSeconds=29*3600L;ServerTools.reconcile(p);
            check(!com.alessandro.astages.api.util.AStagesUtils.hasStage(com.alessandro.astages.api.holder.AHolder.player(uuid),PublishedServices.MACHINERY),"AStages machinery stage stays locked before 30 active hours",passed);
            var press=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("create:mechanical_press")));
            var machineRule=com.alessandro.astages.engine.ARestrictionManager.ITEM_INSTANCE.getRestriction(com.alessandro.astages.api.holder.AHolder.player(uuid),press);
            check(machineRule!=null && !machineRule.get(com.alessandro.astages.engine.store.Attributes.BLOCK_PLACING) && !machineRule.get(com.alessandro.astages.engine.store.Attributes.BLOCK_INTERACTIONS),"AStages consumer rejects placement and interaction before unlock",passed);
            check(machineRule.get(com.alessandro.astages.engine.store.Attributes.PICKUP) && machineRule.get(com.alessandro.astages.engine.store.Attributes.STORING_IN_INVENTORY) && machineRule.get(com.alessandro.astages.engine.store.Attributes.BLOCK_BREAKING),"AStages consumer permits pickup, inventory and mining before unlock",passed);
            for(String decoration:List.of("create:copycat_panel","create:cut_granite","create_connected:copycat_stairs")) {
                var id=ResourceLocation.parse(decoration);var block=BuiltInRegistries.BLOCK.get(id);
                check(!ServerTools.machine(block.defaultBlockState()) && com.alessandro.astages.engine.ARestrictionManager.ITEM_INSTANCE.getRestriction(com.alessandro.astages.api.holder.AHolder.player(uuid),new ItemStack(block))==null,"Decoration stays placeable before machinery unlock: "+decoration,passed);
            }
            var previousHeld=p.getMainHandItem();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,press);
            var interaction=new net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock(p,net.minecraft.world.InteractionHand.MAIN_HAND,p.blockPosition(),new net.minecraft.world.phys.BlockHitResult(p.position(),net.minecraft.core.Direction.UP,p.blockPosition(),false));
            com.alessandro.astages.infrastructure.hook.restriction.ItemServerEvents.onItemUsed(interaction);
            check(!interaction.isCanceled(),"AStages delegates shared fake-player automation to the claim-owner guard",passed);
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,previousHeld);
            automation(test,passed);
            data.activeSeconds=30*3600L;ServerTools.reconcile(p);
            check(com.alessandro.astages.api.util.AStagesUtils.hasStage(com.alessandro.astages.api.holder.AHolder.player(uuid),PublishedServices.MACHINERY),"AStages grants machinery access at 30 active hours",passed);
            check(com.alessandro.astages.engine.ARestrictionManager.ITEM_INSTANCE.getRestriction(com.alessandro.astages.api.holder.AHolder.player(uuid),press)==null,"AStages consumer removes the machinery restriction after earned unlock",passed);
            var afkManager=dk.magnusjensen.simpleafk.AFKManager.getInstance();afkManager.addPlayer(p);
            var afkPlayer=afkManager.getPlayer(uuid);afkPlayer.toggleAfkStatus();
            check(PublishedServices.afk(p),"Manual SimpleAFK status reaches the reward activity filter",passed);
            afkPlayer.removeAfkStatus();check(!PublishedServices.afk(p),"Resuming activity clears the shared AFK status",passed);afkManager.removePlayer(uuid);
            check(PublishedServices.restrictedMachines>0,"AStages has actual functional machinery restrictions registered",passed);
            check(CrateTable.table.getTotalWeight(false)==10000 && CrateTable.rows.size()==ServerTools.config.getAsJsonArray("crate").size(),"FTB Quests owns the exact configured crate table and weights",passed);
            var random=net.minecraft.util.RandomSource.create(20261014L);
            for(int draw=0;draw<10000;draw++)if(CrateTable.draw(random)==null)throw new IllegalStateException("Unknown FTB crate selection");
            passed.add("Ten thousand FTB crate draws resolve to valid configured rewards");
            test.purchases.put("selftest_travel",new State.Purchase(uuid.toString(),"runeforged"));
            travel(p,passed);
            test.purchases.remove("selftest_travel");
            commandUnlocks(p,data,passed);
            var slots=top.theillusivec4.curios.api.CuriosApi.getPlayerSlots(ServerTools.server.overworld()).keySet();
            Files.writeString(Path.of("ops/evidence/equipment-slots.json"),State.JSON.toJson(slots));
            check(slots.containsAll(List.of("hands","necklace","trinket")),"Reward equipment slot IDs exist for players",passed);
            Rewards.audit();check(Rewards.crateValid,"All crate and kit registry IDs exist",passed);
            var disperser=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("relics:experience_disperser")));
            Relics.max(p,disperser);passed.add("Experience Disperser reaches validated maximum rank, level and stats");
            for(String id:Rewards.relicChoices()) {try {Relics.max(p,new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id))));}catch(Exception ex){failures.add("Maxed relic "+id+": "+ex.getClass().getSimpleName()+" "+ex.getMessage());}}
        } catch(Exception ex) {failures.add(ex.getMessage());}
        finally {
            dk.magnusjensen.simpleafk.AFKManager.getInstance().removePlayer(uuid);
            com.alessandro.astages.api.util.AStagesUtils.removeStage(com.alessandro.astages.api.holder.AHolder.player(uuid),PublishedServices.MACHINERY,false,false,false);
            for(String id:List.of("ember","artisan","runeforged","mythic"))FTBRanksAPI.manager().getRank(id).ifPresent(r->r.remove(p.getGameProfile()));
            if(FTBRanksAPI.manager().getRank(earned).isPresent())FTBRanksAPI.manager().deleteRank(earned);
            ServerTools.state=original;
        }
        try {Files.writeString(Path.of("ops/evidence/selftest.json"),State.JSON.toJson(Map.of("passed",passed,"failures",failures,"success",failures.isEmpty())));}catch(Exception ex){throw new RuntimeException(ex);}
        ServerTools.LOG.info("[Mythforge] Runtime self-test: {} passed, {} failed. See ops/evidence/selftest.json",passed.size(),failures.size());
    }
    static void automation(State test,List<String> passed) {
        check(!ServerTools.automationUnlocked(ServerTools.server.overworld(),new net.minecraft.core.BlockPos(500000,64,500000)),"Automated machinery cannot bypass progression in unclaimed terrain",passed);
        var claim=dev.ftb.mods.ftbchunks.api.FTBChunksAPI.api().getManager().getAllClaimedChunks().stream().findFirst().orElseThrow();
        var owner=claim.getTeamData().getTeam().getOwner();var data=test.player(owner);
        var level=ServerTools.server.getLevel(claim.getPos().dimension());var target=new net.minecraft.core.BlockPos(claim.getPos().x()*16+8,64,claim.getPos().z()*16+8);
        data.activeSeconds=29*3600L;
        check(!ServerTools.automationUnlocked(level,target),"Claim-owner automation remains locked before 30 active hours",passed);
        data.activeSeconds=30*3600L;
        check(ServerTools.automationUnlocked(level,target),"Claim-owner automation unlocks with earned machinery access",passed);
        data.activeSeconds=0;test.purchases.put("selftest_automation",new State.Purchase(owner.toString(),"ember"));
        check(ServerTools.automationUnlocked(level,target),"Claim-owner automation accepts the eligible paid entitlement",passed);
        test.purchases.remove("selftest_automation");
        check(!ServerTools.automationUnlocked(level,target),"Removing the unearned paid entitlement relocks automation",passed);
    }
    static void travel(net.minecraft.server.level.ServerPlayer p,List<String> passed) throws Exception {
        var timings=new java.util.LinkedHashMap<String,Object>();
        for(var key:List.of(net.minecraft.world.level.Level.OVERWORLD,net.minecraft.world.level.Level.NETHER,net.minecraft.world.level.Level.END)) {
            var level=ServerTools.server.getLevel(key);long started=System.nanoTime();
            var actor=new TestPlayer(level,p.getGameProfile());
            FTBTravel.search(actor,level,0).whenComplete((position,error)->ServerTools.server.execute(()->{
                timings.put(key.location().toString(),Map.of("milliseconds",(System.nanoTime()-started)/1000000,"safe",error==null && position!=null && Travel.safe(actor,level,position),"error",error==null?"":error.toString()));
                try {Files.writeString(Path.of("ops/evidence/rtp-provider-test.json"),State.JSON.toJson(timings));}catch(Exception failure){ServerTools.LOG.error("Cannot write RTP test",failure);}
            }));
        }
    }
    static void commandUnlocks(net.minecraft.server.level.ServerPlayer p,State.PlayerData data,List<String> passed) throws Exception {
        var dispatcher=ServerTools.server.getCommands().getDispatcher();
        var source=p.createCommandSourceStack();
        var held=p.getMainHandItem().copy();int xp=p.experienceLevel;long day=p.serverLevel().getDayTime();
        try {
            data.activeSeconds=149*3600L;
            var menu=p.containerMenu;dispatcher.execute("grindstone",source);
            check(p.containerMenu==menu,"Grindstone stays locked before 150 active hours",passed);
            data.activeSeconds=150*3600L;dispatcher.execute("grindstone",source);
            check(p.containerMenu instanceof net.minecraft.world.inventory.GrindstoneMenu,"Grindstone opens at 150 hours without a paid rank",passed);p.closeContainer();
            var sword=new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD);
            sword.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Verification sword"));
            p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,sword);
            data.activeSeconds=299*3600L;ServerTools.reconcile(p);dispatcher.execute("rename &aEarned name",source);
            check(sword.getHoverName().getString().equals("Verification sword")&&!FTBRanksAPI.getPermissionValue(p,"command.anvil").asBooleanOrFalse(),"Naming and portable workbenches remain locked before 300 hours",passed);
            data.activeSeconds=300*3600L;ServerTools.reconcile(p);dispatcher.execute("rename &aEarned name",source);
            check(sword.getHoverName().getString().equals("Earned name")&&FTBRanksAPI.getPermissionValue(p,"command.anvil").asBooleanOrFalse()&&FTBRanksAPI.getPermissionValue(p,"command.smithing").asBooleanOrFalse()&&FTBRanksAPI.getPermissionValue(p,"command.disposal").asBooleanOrFalse(),"Naming and portable utility permissions unlock at 300 hours",passed);
            data.activeSeconds=399*3600L;dispatcher.execute("lore Earned lore",source);dispatcher.execute("joinmessage Earned greeting",source);dispatcher.execute("worldtime 777",source);
            check(sword.getOrDefault(net.minecraft.core.component.DataComponents.LORE,net.minecraft.world.item.component.ItemLore.EMPTY).lines().isEmpty()&&data.joinMessage.isBlank()&&p.serverLevel().getDayTime()==day,"Lore, join messages and world time remain locked before 400 hours",passed);
            data.activeSeconds=400*3600L;dispatcher.execute("lore Earned lore",source);dispatcher.execute("joinmessage Earned greeting",source);dispatcher.execute("worldtime 777",source);
            check(sword.get(net.minecraft.core.component.DataComponents.LORE).lines().getFirst().getString().equals("Earned lore")&&data.joinMessage.equals("Earned greeting")&&p.serverLevel().getDayTime()==777,"Lore, join messages and world time unlock at 400 hours",passed);
            sword.setDamageValue(201);p.experienceLevel=10;data.activeSeconds=499*3600L;dispatcher.execute("repair",source);
            check(sword.getDamageValue()==201&&p.experienceLevel==10,"Repair cannot bypass the 500-hour threshold",passed);
            data.activeSeconds=500*3600L;dispatcher.execute("repair",source);
            check(sword.getDamageValue()==0&&p.experienceLevel==7&&sword.getHoverName().getString().equals("Earned name")&&sword.has(net.minecraft.core.component.DataComponents.LORE),"Earned repair charges XP and preserves item name and lore",passed);
        } finally {p.closeContainer();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,held);p.experienceLevel=xp;p.serverLevel().setDayTime(day);}
    }
}
