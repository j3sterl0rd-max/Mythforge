package dev.mythforge.server;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.*;
import dev.ftb.mods.ftbranks.api.*;
import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.*;
import java.util.*;
import static net.minecraft.commands.Commands.*;

final class CommandsSetup {
    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(literal("rewards").executes(c->{var p=c.getSource().getPlayerOrException();ServerTools.tell(p,"Active playtime: "+(ServerTools.data(p).activeSeconds/60)+" minutes; keys: "+ServerTools.data(p).keys+". Rewards arrive once; /mail claims pending items. Command unlocks: /grindstone at 150h; /rename, /anvil, /smithing, /disposal, /rtp nether and public-warp icons at 300h; /lore, /joinmessage world controls and /rtp end at 400h; /repair and priority login at 500h.");return 1;}));
        d.register(literal("mail").executes(c->{Rewards.mail(c.getSource().getPlayerOrException());return 1;}));
        d.register(literal("relic").executes(c->{var p=c.getSource().getPlayerOrException();ServerTools.tell(p,"Hold your Runeforged Relic Choice Voucher, then /relic choose <registry-id>. Eligible choices: "+String.join(", ",Rewards.relicChoices()));return 1;})
            .then(literal("choose").then(argument("item",ResourceLocationArgument.id()).executes(c->{var p=c.getSource().getPlayerOrException();var hand=p.getMainHandItem();var custom=hand.get(DataComponents.CUSTOM_DATA);String id=ResourceLocationArgument.getId(c,"item").toString();if(custom==null||!custom.copyTag().getString("mythforge_consumable").equals("relic_choice")||!Rewards.relicChoices().contains(id))return 0;var reward=new com.google.gson.JsonObject();reward.addProperty("id",id);reward.addProperty("count",1);reward.addProperty("maxed",true);Rewards.stack(p,reward);var rewards=new com.google.gson.JsonArray();rewards.add(reward);hand.shrink(1);Rewards.enqueue(p,rewards);ServerTools.state.save();ServerTools.server.getPlayerList().saveAll();Rewards.mail(p);return 1;}))));
        d.register(literal("crate").executes(c->{Rewards.open(c.getSource().getPlayerOrException());return 1;}));
        d.register(literal("vote").executes(c->{ServerTools.tell(c.getSource().getPlayerOrException(),"Voting links will be shared in Discord when listings open. Every verified vote gives one key; offline votes are queued.");return 1;}));
        d.register(literal("rtp").executes(c->{Travel.rtp(c.getSource().getPlayerOrException(),"overworld");return 1;})
            .then(literal("nether").executes(c->{Travel.rtp(c.getSource().getPlayerOrException(),"the_nether");return 1;}))
            .then(literal("end").executes(c->{Travel.rtp(c.getSource().getPlayerOrException(),"the_end");return 1;})));
        d.register(literal("ascend").executes(c->{Travel.ascend(c.getSource().getPlayerOrException());return 1;}));
        d.register(literal("feed").executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,120,3)&&!ServerTools.combat(p)&&ServerTools.cooldown(p,"feed",300)){p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(10);}return 1;}));
        d.register(literal("heal").executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,175,4)&&!ServerTools.combat(p)&&!ServerTools.bossEncounter(p)&&ServerTools.cooldown(p,"heal",600)){p.setHealth(p.getMaxHealth());p.clearFire();}return 1;}));
        d.register(literal("fly").executes(c->{var p=c.getSource().getPlayerOrException();var s=ServerTools.sessions.get(p.getUUID());if(s.customFlight)ServerTools.revokeFlight(p,s);else if(!ServerTools.combat(p)&&ServerTools.flightAllowed(p)){ServerTools.grantFlight(p,s);ServerTools.tell(p,"Flight enabled in your team's claims. Combat and leaving the claim end it.");}else ServerTools.tell(p,"Flight needs 200 active hours or Runeforged, and your team's claim.");return 1;}));
        d.register(literal("repair").executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,500,3))return 0;var item=p.getMainHandItem();if(!item.isDamageableItem()||item.getDamageValue()==0)return 0;int cost=Math.max(1,(int)Math.ceil(item.getDamageValue()/100.0));if(p.experienceLevel<cost){ServerTools.tell(p,"Repair costs "+cost+" XP levels.");return 0;}p.giveExperienceLevels(-cost);item.setDamageValue(0);ServerTools.tell(p,"Repaired for "+cost+" XP levels; all other components preserved.");return 1;}));
        d.register(literal("grindstone").executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,150,2))p.openMenu(new SimpleMenuProvider((id,inv,player)->new GrindstoneMenu(id,inv),Component.literal("Grindstone")));return 1;}));
        for(String effect:List.of("speed","haste","night_vision","water_breathing")) {
            d.register(literal("effects").then(literal(effect).executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,175,4)&&!ServerTools.combat(p)&&ServerTools.cooldown(p,"effects",900)){var holder=BuiltInRegistries.MOB_EFFECT.getHolder(ResourceLocation.parse("minecraft:"+effect)).orElseThrow();p.addEffect(new MobEffectInstance(holder,6000,0,false,true));}return 1;})));
        }
        d.register(literal("pwarp").executes(c->{WarpMenu.open(c.getSource().getPlayerOrException(),0);return 1;})
            .then(literal("create").then(argument("name",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();var name=StringArgumentType.getString(c,"name").toLowerCase(Locale.ROOT);if(!name.matches("[a-z0-9_-]{1,24}"))return 0;var count=ServerTools.state.warps.values().stream().filter(w->w.owner.equals(p.getUUID().toString())).count();int max=FTBRanksAPI.getPermissionValue(p,"mythforge.pwarp.max").asInteger().orElse(0);var existing=ServerTools.state.warps.get(name);if(existing!=null&&!existing.owner.equals(p.getUUID().toString()))return 0;if(existing==null&&count>=max){ServerTools.tell(p,"Public warp allowance: "+max);return 0;}var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(p.level().dimension(),p.chunkPosition()));if(claim==null||!claim.getTeamData().isTeamMember(p.getUUID())||!Travel.safe(p,p.serverLevel(),p.blockPosition())){ServerTools.tell(p,"Create warps on safe ground in your team's claim.");return 0;}ServerTools.state.warps.put(name,new State.Warp(p.getUUID().toString(),p.level().dimension().location().toString(),p.getX(),p.getY(),p.getZ()));ServerTools.state.save();return 1;})))
            .then(literal("delete").then(argument("name",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();var name=StringArgumentType.getString(c,"name");var w=ServerTools.state.warps.get(name);if(w!=null&&w.owner.equals(p.getUUID().toString())){ServerTools.state.warps.remove(name);ServerTools.state.save();return 1;}return 0;})))
            .then(literal("visit").then(argument("name",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();var w=ServerTools.state.warps.get(StringArgumentType.getString(c,"name"));if(w==null||ServerTools.combat(p))return 0;var level=ServerTools.server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(w.dimension)));if(level!=null&&publicSafe(p,level,BlockPos.containing(w.x,w.y,w.z),w.owner)&&ServerTools.cooldown(p,"pwarp",60))Travel.queue(p,level,BlockPos.containing(w.x,w.y,w.z));return 1;})))
            .then(literal("icon").then(argument("name",StringArgumentType.word()).then(argument("item",ResourceLocationArgument.id()).executes(c->{var p=c.getSource().getPlayerOrException();var w=ServerTools.state.warps.get(StringArgumentType.getString(c,"name"));String icon=ResourceLocationArgument.getId(c,"item").toString();if(w!=null&&w.owner.equals(p.getUUID().toString())&&(ServerTools.hours(p)>=300||ServerTools.tier(p)>=2)&&ResourceLocation.tryParse(icon)!=null&&BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(icon))){w.icon=icon;ServerTools.state.save();return 1;}return 0;})))));
        d.register(literal("waypoint").then(argument("x",IntegerArgumentType.integer(-30000,30000)).then(argument("y",IntegerArgumentType.integer(-64,319)).then(argument("z",IntegerArgumentType.integer(-30000,30000)).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,150,3)||ServerTools.combat(p))return 0;var pos=new BlockPos(IntegerArgumentType.getInteger(c,"x"),IntegerArgumentType.getInteger(c,"y"),IntegerArgumentType.getInteger(c,"z"));String key=p.level().dimension().location()+":"+(pos.getX()>>4)+":"+(pos.getZ()>>4);if(!ServerTools.data(p).visited.contains(key)){ServerTools.tell(p,"You must have visited this chunk yourself.");return 0;}if(ServerTools.cooldown(p,"waypoint",120))Travel.queue(p,p.serverLevel(),pos);return 1;})))));
        d.register(literal("prefix").then(argument("text",StringArgumentType.greedyString()).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,250,4))return 0;String text=clean(StringArgumentType.getString(c,"text"),20);ServerTools.data(p).prefix=text;p.refreshDisplayName();ServerTools.tell(p,"Prefix saved: "+text);ServerTools.state.save();return 1;})));
        d.register(literal("chatcolor").then(argument("color",StringArgumentType.word()).executes(c->{var p=c.getSource().getPlayerOrException();var color=net.minecraft.ChatFormatting.getByName(StringArgumentType.getString(c,"color"));if(!eligible(p,250,4)||color==null||!color.isColor())return 0;ServerTools.data(p).chatColor=color.getName();ServerTools.state.save();p.refreshDisplayName();return 1;})));
        d.register(literal("ptime").then(literal("reset").executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,300,5))return 0;ServerTools.data(p).personalTime=-1;PersonalView.send(p);ServerTools.state.save();return 1;})).then(argument("time",IntegerArgumentType.integer(0,23999)).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,300,5))return 0;ServerTools.data(p).personalTime=IntegerArgumentType.getInteger(c,"time");PersonalView.send(p);ServerTools.state.save();return 1;})));
        for(String weather:List.of("clear","rain","reset"))d.register(literal("pweather").then(literal(weather).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,300,5))return 0;ServerTools.data(p).personalWeather=weather;PersonalView.send(p);ServerTools.state.save();return 1;})));
        for(String particle:List.of("off","happy_villager","end_rod","enchant"))d.register(literal("particles").then(literal(particle).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,400,5))return 0;ServerTools.data(p).particle=particle;ServerTools.state.save();return 1;})));
        d.register(literal("rename").then(argument("text",StringArgumentType.greedyString()).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,300,1))return 0;p.getMainHandItem().set(DataComponents.CUSTOM_NAME,colored(clean(StringArgumentType.getString(c,"text"),48)));return 1;})));
        d.register(literal("lore").then(argument("text",StringArgumentType.greedyString()).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,400,4))return 0;p.getMainHandItem().set(DataComponents.LORE,new ItemLore(List.of(Component.literal(clean(StringArgumentType.getString(c,"text"),100)))));return 1;})));
        d.register(literal("joinmessage").then(argument("text",StringArgumentType.greedyString()).executes(c->{var p=c.getSource().getPlayerOrException();if(!eligible(p,400,4))return 0;ServerTools.data(p).joinMessage=clean(StringArgumentType.getString(c,"text"),80);ServerTools.state.save();return 1;})));
        d.register(literal("worldtime").then(argument("time",IntegerArgumentType.integer(0,23999)).executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,400,4)&&ServerTools.cooldown(p,"worldtime",1800))p.serverLevel().setDayTime(IntegerArgumentType.getInteger(c,"time"));return 1;})));
        for(String weather:List.of("clear","rain","thunder")) d.register(literal("worldweather").then(literal(weather).executes(c->{var p=c.getSource().getPlayerOrException();if(eligible(p,400,4)&&ServerTools.cooldown(p,"worldweather",1800))p.serverLevel().setWeatherParameters(weather.equals("clear")?6000:0,6000,!weather.equals("clear"),weather.equals("thunder"));return 1;})));
        var management=literal("mythforge").requires(s->s.hasPermission(4) && s.getEntity()==null)
            .then(literal("audit").executes(c->{ServerTools.audit();c.getSource().sendSuccess(()->Component.literal("Audit saved under ops/evidence."),false);return 1;}))
            .then(literal("selftest").executes(c->{SelfTest.run();return 1;}))
            .then(literal("vote").then(argument("player",EntityArgument.player()).executes(c->{var p=EntityArgument.getPlayer(c,"player");ServerTools.data(p).keys++;ServerTools.state.save();ServerTools.tell(p,"Verified vote received: +1 crate key.");return 1;})))
            .then(literal("purchase").then(argument("uuid",StringArgumentType.word()).then(argument("rank",StringArgumentType.word()).then(argument("transaction",StringArgumentType.word()).executes(c->{return Payments.purchase(StringArgumentType.getString(c,"uuid"),StringArgumentType.getString(c,"rank"),StringArgumentType.getString(c,"transaction"))?1:0;})))))
            .then(literal("revoke").then(argument("transaction",StringArgumentType.word()).executes(c->{return Payments.revoke(StringArgumentType.getString(c,"transaction"))?1:0;})))
            .then(literal("settime").then(argument("player",EntityArgument.player()).then(argument("hours",IntegerArgumentType.integer(0,10000)).executes(c->{var p=EntityArgument.getPlayer(c,"player");ServerTools.data(p).activeSeconds=IntegerArgumentType.getInteger(c,"hours")*3600L;ServerTools.reconcile(p);Rewards.deliver(p);ServerTools.state.save();return 1;}))));
        d.register(management);
    }
    static Component colored(String text) {
        var result=Component.empty();var color=net.minecraft.ChatFormatting.WHITE;var chunk=new StringBuilder();
        for(int i=0;i<text.length();i++) {
            if(text.charAt(i)=='&' && i+1<text.length()) {var next=net.minecraft.ChatFormatting.getByCode(text.charAt(i+1));if(next!=null&&next.isColor()){result.append(Component.literal(chunk.toString()).withStyle(color));chunk.setLength(0);color=next;i++;continue;}}
            chunk.append(text.charAt(i));
        }
        return result.append(Component.literal(chunk.toString()).withStyle(color));
    }
    static String clean(String input,int max) { return input.replaceAll("[\\p{Cntrl}§]","").substring(0,Math.min(input.replaceAll("[\\p{Cntrl}§]","").length(),max)); }
    static boolean eligible(ServerPlayer p,long hours,int paid) { if(ServerTools.hours(p)>=hours||ServerTools.tier(p)>=paid)return true;ServerTools.tell(p,"This feature has not unlocked yet.");return false; }
    static boolean publicSafe(ServerPlayer p,ServerLevel level,BlockPos pos,String owner) {
        // Public warp consent is stored by its owner, but terrain and dimension restrictions still apply.
        var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),new ChunkPos(pos)));
        if(claim==null||!claim.getTeamData().isTeamMember(UUID.fromString(owner)))return false;
        return Travel.permitted(p,level)&&(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)||Travel.safe(p,level,pos));
    }
}
