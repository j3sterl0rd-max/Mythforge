package dev.mythforge.server;

import com.mojang.authlib.GameProfile;
import dev.ftb.mods.ftbranks.api.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Honorary entitlements share the paid benefit path without fabricating purchases. */
final class RoleBenefits {
    static final int HIGHEST_TIER=4;
    static final Set<String> STAFF=Set.of("junior_mod","moderator","admin","owner");
    // Fixed cohort: every genuine player save on the previous server at migration.
    static final Map<UUID,String> TESTERS=Map.ofEntries(
        Map.entry(UUID.fromString("049f2b50-8c1a-4c0a-b8b8-dfbd7aabec6f"),"EnderHayes18"),
        Map.entry(UUID.fromString("04fc914c-0584-43ff-af3c-28ee0c05a6b5"),"tony332211"),
        Map.entry(UUID.fromString("1bb4e05a-10a1-46f3-a1a9-c810ad745409"),"iDonnyboy23"),
        Map.entry(UUID.fromString("3b054a0b-7088-4b00-9262-0fbc22a6ef36"),"Procyonine"),
        Map.entry(UUID.fromString("3fab6d20-f7c2-4d23-ad65-4503348ace93"),"Slenana"),
        Map.entry(UUID.fromString("48f2f005-71ce-4cfb-bf46-1e750def5ceb"),"Regranaas47"),
        Map.entry(UUID.fromString("6acd1d1e-b9d5-4dcd-90d1-fa2d4fd659fd"),"Yakuzco"),
        Map.entry(UUID.fromString("81622cdc-6dc3-4518-b6e0-ecbbc5934a54"),"Irenoir"),
        Map.entry(UUID.fromString("aa705957-d779-49e1-95d6-ece6ad98216b"),"kaididit"),
        Map.entry(UUID.fromString("c56f9c59-4c83-4e80-a4ba-2613b7a76974"),"SealedHorror"),
        Map.entry(UUID.fromString("d14db3c0-198b-41e4-a7d0-a6d800d31fa4"),"Winterberry99"),
        Map.entry(UUID.fromString("ed2473cd-addc-4de9-9f3c-a5fed26e09e7"),"Saku1849"));
    static boolean staff(GameProfile profile) {
        var online=ServerTools.server.getPlayerList().getPlayer(profile.getId());
        return ServerTools.server.getPlayerList().isOp(profile)
            || FTBRanksAPI.manager().getAddedRanks(profile).stream().anyMatch(r->STAFF.contains(r.getId()))
            || online!=null && FTBRanksAPI.manager().getRanks(online).stream().anyMatch(r->STAFF.contains(r.getId()));
    }
    static int tier(GameProfile profile) {
        return TESTERS.containsKey(profile.getId()) || staff(profile)?HIGHEST_TIER:ServerTools.state.paid(profile.getId());
    }
    static boolean machinery(UUID id) {
        var data=ServerTools.state.player(id);
        return data.createBypass || data.activeSeconds>=108000 || tier(new GameProfile(id,""))>0;
    }
    static void initialize() {
        // Owner is valid for explicit UUID assignment as well as existing operators.
        var ownerRank=FTBRanksAPI.manager().getRank("owner").orElseThrow();
        ownerRank.setCondition(new dev.ftb.mods.ftbranks.api.RankCondition.Simple() {
            public String getType(){return "default";}
            public boolean isDefaultCondition(){return true;}
            public boolean isRankActive(ServerPlayer player){
                return ownerRank.isAdded(player)||ServerTools.server.getPlayerList().isOp(player.getGameProfile());
            }
        });

        for(var role:FTBRanksAPI.manager().getAllRanks())if(!role.getId().startsWith("earned_")){PrefixPolicy.reserve(role.getId());PrefixPolicy.reserve(role.getName());}
        // Operator-designated staff, keyed by Minecraft UUID rather than mutable names.
        // Procyonine's UUID was verified with Mojang's public profile API.
        FTBRanksAPI.manager().getRank("owner").orElseThrow().add(new GameProfile(UUID.fromString("3b054a0b-7088-4b00-9262-0fbc22a6ef36"),"Procyonine"));
        var kaididit=new GameProfile(UUID.fromString("aa705957-d779-49e1-95d6-ece6ad98216b"),"kaididit");
        FTBRanksAPI.manager().getRank("moderator").ifPresent(rank->rank.remove(kaididit));
        FTBRanksAPI.manager().getRank("admin").orElseThrow().add(kaididit);
        var rank=FTBRanksAPI.manager().getRank("beta_tester").orElseGet(()->FTBRanksAPI.manager().createRank("beta_tester","Beta Tester",150));
        ServerTools.setNode(rank,"ftbranks.name_format","&b[Beta Tester] &f{name}&r");
        for(var entry:TESTERS.entrySet()) {
            var profile=new GameProfile(entry.getKey(),entry.getValue());
            rank.add(profile);
        }
    }
    static void reconcile(ServerPlayer player) {
        FTBRanksAPI.manager().getRank("beta_tester").ifPresent(rank->{
            if(TESTERS.containsKey(player.getUUID()))rank.add(player.getGameProfile());
            else rank.remove(player.getGameProfile());
        });
    }
    static boolean canManage(CommandSourceStack source) {
        if(source.hasPermission(3))return true;
        return source.getEntity() instanceof ServerPlayer player
            && (FTBRanksAPI.manager().getAddedRanks(player.getGameProfile()).stream().anyMatch(r->Set.of("admin","owner").contains(r.getId()))
                || FTBRanksAPI.manager().getRanks(player).stream().anyMatch(r->Set.of("admin","owner").contains(r.getId())));
    }
    static int bypass(CommandSourceStack source,Collection<GameProfile> profiles,Boolean enabled) {
        if(profiles.size()!=1){source.sendFailure(Component.literal("Choose exactly one player."));return 0;}
        var profile=profiles.iterator().next();
        var data=ServerTools.state.player(profile.getId());
        if(enabled!=null) {
            data.createBypass=enabled;ServerTools.state.save();
            var online=ServerTools.server.getPlayerList().getPlayer(profile.getId());
            if(online!=null)PublishedServices.reconcile(online);
            ServerTools.LOG.info("[Mythforge] {} set Create time bypass for {} ({}) to {}",source.getTextName(),profile.getName(),profile.getId(),enabled);
        }
        source.sendSuccess(()->Component.literal("Create time bypass for "+profile.getName()+": "+(data.createBypass?"on":"off")+". Banned items remain banned; other unlocks still apply when bypass is off."),false);
        return 1;
    }
}
