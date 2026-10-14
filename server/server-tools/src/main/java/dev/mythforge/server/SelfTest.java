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
            check(!FTBRanksAPI.getPermissionValue(p,"command.give").asBooleanOrFalse(),"Supporters cannot use /give",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.enderchest.target").asBooleanOrFalse(),"Supporters cannot open another player's ender chest",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.ftbquests.editing_mode").asBooleanOrFalse(),"Players cannot edit quests",passed);
            check(!FTBRanksAPI.getPermissionValue(p,"command.ftbteams.force-add").asBooleanOrFalse(),"Players cannot force team membership",passed);
            check(Payments.revoke("selftest_runeforged")&&test.paid(uuid)==1,"Refund downgrades to the remaining paid entitlement",passed);
            ServerTools.reconcile(p);
            check(FTBRanksAPI.getPermissionValue(p,"ftbessentials.home.max").asInteger().orElse(0)==7&&FTBRanksAPI.getPermissionValue(p,"mythforge.pwarp.max").asInteger().orElse(0)==6,"Refund preserves natural allowances plus remaining rank bonuses",passed);
            check(data.activeSeconds==500*3600L&&data.delivered.contains(500)&&data.paidDelivered==3,"Refund preserves earned time and one-time delivery receipts",passed);
            Payments.revoke("selftest_ember");
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
            for(String id:List.of("ember","artisan","runeforged","mythic"))FTBRanksAPI.manager().getRank(id).ifPresent(r->r.remove(p.getGameProfile()));
            if(FTBRanksAPI.manager().getRank(earned).isPresent())FTBRanksAPI.manager().deleteRank(earned);
            ServerTools.state=original;
        }
        try {Files.writeString(Path.of("ops/evidence/selftest.json"),State.JSON.toJson(Map.of("passed",passed,"failures",failures,"success",failures.isEmpty())));}catch(Exception ex){throw new RuntimeException(ex);}
        ServerTools.LOG.info("[Mythforge] Runtime self-test: {} passed, {} failed. See ops/evidence/selftest.json",passed.size(),failures.size());
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
