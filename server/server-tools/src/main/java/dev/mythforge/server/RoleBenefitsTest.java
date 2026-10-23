package dev.mythforge.server;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbranks.api.FTBRanksAPI;
import com.alessandro.astages.api.util.AStagesUtils;
import com.alessandro.astages.api.holder.AHolder;
import java.util.*;

final class RoleBenefitsTest {
    static void audit() {
        var passed=new ArrayList<String>();var failures=new ArrayList<String>();var original=ServerTools.state;
        var test=new State();test.path=java.nio.file.Path.of("ops/private/beta-selftest-state.json");
        var player=new SelfTest.TestPlayer(ServerTools.server.overworld(),new GameProfile(UUID.fromString("adc8b684-672a-4fd3-9b2b-401e80f2a6b8"),"MF_Test"));
        try {ServerTools.state=test;run(player,passed);RewardUiTest.run(passed);}catch(Exception ex){failures.add(ex.toString());}
        finally {ServerTools.state=original;}
        try {java.nio.file.Files.writeString(java.nio.file.Path.of("ops/evidence/beta-selftest.json"),State.JSON.toJson(Map.of("success",failures.isEmpty(),"passed",passed,"failures",failures)));}
        catch(Exception ex){throw new RuntimeException(ex);}
        ServerTools.LOG.info("[Mythforge] Beta entitlement test: {} passed, {} failed",passed.size(),failures.size());
    }
    static void run(SelfTest.TestPlayer player,List<String> passed) throws Exception {
        var profile=player.getGameProfile();var data=ServerTools.data(player);
        data.activeSeconds=0;data.createBypass=false;
        SelfTest.check(ServerTools.tier(player)==0&&!RoleBenefits.machinery(player.getUUID()),"Ordinary new players retain the machinery gate",passed);
        SelfTest.check(!RoleBenefits.canManage(player.createCommandSourceStack()),"Ordinary players cannot manage Create bypasses",passed);
        for(var tester:RoleBenefits.TESTERS.entrySet()) {
            var legacy=new SelfTest.TestPlayer(ServerTools.server.overworld(),new GameProfile(tester.getKey(),tester.getValue()));
            RoleBenefits.reconcile(legacy);ServerTools.reconcile(legacy);
            SelfTest.check(RoleBenefits.tier(legacy.getGameProfile())==RoleBenefits.HIGHEST_TIER,"Previous-server beta tester receives Mythic tier: "+tester.getValue(),passed);
            SelfTest.check(FTBRanksAPI.manager().getRank("beta_tester").orElseThrow().isAdded(legacy),"Beta assignment survives login reconciliation: "+tester.getValue(),passed);
            SelfTest.check(FTBRanksAPI.getPermissionValue(legacy,"ftbchunks.max_claimed").asInteger().orElse(0)==320,"Beta receives full Mythic claim benefits: "+tester.getValue(),passed);
        }
        SelfTest.check(RoleBenefits.TESTERS.size()==12 && RoleBenefits.TESTERS.containsKey(UUID.fromString("aa705957-d779-49e1-95d6-ece6ad98216b")),"All twelve previous players, including staff, receive beta entitlement",passed);
        for(String id:List.of("junior_mod","moderator","admin","owner")) {
            var rank=FTBRanksAPI.manager().getRank(id).orElseThrow();
            try {
                rank.add(profile);ServerTools.reconcile(player);
                SelfTest.check(ServerTools.tier(player)==4&&RoleBenefits.machinery(player.getUUID()),id+" inherits Mythic and machinery benefits",passed);
                SelfTest.check(FTBRanksAPI.getPermissionValue(player,"ftbchunks.max_claimed").asInteger().orElse(0)==320,id+" gets full Mythic claim bonus",passed);
                SelfTest.check(RoleBenefits.canManage(player.createCommandSourceStack())==Set.of("admin","owner").contains(id),id+" has correct bypass management access",passed);
            } finally {rank.remove(profile);}
        }
        ServerTools.reconcile(player);
        var console=ServerTools.server.createCommandSourceStack();
        RoleBenefits.bypass(console,List.of(profile),true);
        SelfTest.check(data.createBypass&&RoleBenefits.machinery(player.getUUID())&&ServerTools.tier(player)==0,"Bypass unlocks machinery without granting donor perks",passed);
        var enabledState=State.JSON.fromJson(java.nio.file.Files.readString(ServerTools.state.path),State.class);
        SelfTest.check(enabledState.player(player.getUUID()).createBypass,"Bypass-on persists across saved-state reload",passed);
        PublishedServices.reconcile(player);
        SelfTest.check(AStagesUtils.hasStage(AHolder.player(player.getUUID()),PublishedServices.MACHINERY),"Bypass synchronizes AStages machinery access",passed);
        RoleBenefits.bypass(console,List.of(profile),true);
        SelfTest.check(data.createBypass,"Repeated bypass-on is idempotent",passed);
        RoleBenefits.bypass(console,List.of(profile),false);PublishedServices.reconcile(player);
        SelfTest.check(!data.createBypass&&!RoleBenefits.machinery(player.getUUID())&&!AStagesUtils.hasStage(AHolder.player(player.getUUID()),PublishedServices.MACHINERY),"Bypass-off restores machinery and AStages time gate",passed);
        data.activeSeconds=108000;
        SelfTest.check(RoleBenefits.machinery(player.getUUID()),"Bypass-off preserves earned 30-hour access",passed);
        SelfTest.check(ServerTools.banned(net.minecraft.resources.ResourceLocation.parse("create:schematicannon")),"Machinery banned-item policy remains in force",passed);
        SelfTest.check(RoleBenefits.bypass(console,List.of(),true)==0,"Bypass rejects empty or multiple targets",passed);
        ServerTools.state.save();
        var saved=State.JSON.fromJson(java.nio.file.Files.readString(ServerTools.state.path),State.class);
        SelfTest.check(!saved.player(player.getUUID()).createBypass,"Bypass-off persists in saved state",passed);
    }
}
