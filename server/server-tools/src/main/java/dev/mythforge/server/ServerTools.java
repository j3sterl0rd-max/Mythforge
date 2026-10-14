package dev.mythforge.server;

import com.google.gson.*;
import com.mojang.brigadier.arguments.*;
import dev.ftb.mods.ftbranks.api.*;
import dev.ftb.mods.ftbchunks.api.*;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.*;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.*;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.effect.*;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.*;
import net.neoforged.neoforge.event.entity.living.*;
import net.neoforged.neoforge.event.entity.player.*;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.*;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import java.nio.file.*;
import java.util.*;

@Mod("mythforge_server")
public final class ServerTools {
    static final Logger LOG=LogUtils.getLogger();
    static State state;
    static JsonObject config;
    static MinecraftServer server;
    static final Map<UUID, Session> sessions=new HashMap<>();
    static final Map<UUID, PendingTeleport> teleports=new HashMap<>();
    static long seconds;
    static final Random RANDOM=new java.security.SecureRandom();
    static final class Session {
        long lastAction=seconds-301, lastMovement=seconds-91, combatUntil, lastCredit=seconds;
        String lastSignal="";
        int repetition;
        double x,y,z;
        float yaw,pitch;
        boolean customFlight, priorMayfly;
        Session(ServerPlayer p) { x=p.getX();y=p.getY();z=p.getZ();yaw=p.getYRot();pitch=p.getXRot(); }
    }
    record PendingTeleport(ServerLevel level, BlockPos pos, long due, double x,double y,double z) {}
    public ServerTools() {
        var bus=NeoForge.EVENT_BUS;
        bus.addListener(this::started); bus.addListener(this::stopping); bus.addListener(this::tick);
        bus.addListener(this::commands); bus.addListener(this::login); bus.addListener(this::logout);
        bus.addListener(this::place); bus.addListener(this::breakBlock); bus.addListener(this::interact);
        bus.addListener(this::useItem); bus.addListener(this::damage); bus.addListener(this::crafted);
        bus.addListener(this::commandGuard);
        bus.addListener(this::formatName); bus.addListener(this::chat);
    }
    void started(ServerStartedEvent e) {
        server=e.getServer(); state=State.load(server);
        try {
            config=JsonParser.parseString(Files.readString(Path.of("config/mythforge-server.json"))).getAsJsonObject();
            List<String> items=BuiltInRegistries.ITEM.keySet().stream().map(Object::toString).sorted().toList();
            Files.createDirectories(Path.of("ops/evidence"));
            Files.writeString(Path.of("ops/evidence/item-registry.json"),State.JSON.toJson(items));
            List<String> machines=BuiltInRegistries.BLOCK.stream().filter(b -> {
                var id=BuiltInRegistries.BLOCK.getKey(b);
                return machineryNamespace(id.getNamespace()) && b instanceof EntityBlock;
            }).map(b->BuiltInRegistries.BLOCK.getKey(b).toString()).sorted().toList();
            Files.writeString(Path.of("ops/evidence/functional-machinery.json"),State.JSON.toJson(machines));
            audit();
            state.save();
            LOG.info("[Mythforge] Active playtime, paid ledger, crates and travel ready; {} items registered",items.size());
        } catch(Exception ex) { throw new IllegalStateException("Mythforge launch configuration invalid",ex); }
    }
    void stopping(ServerStoppingEvent e) { if(state!=null) state.save(); }
    void formatName(PlayerEvent.NameFormat e) {
        if(state==null||!(e.getEntity() instanceof ServerPlayer p))return;
        var d=data(p);var name=e.getDisplayname().copy();
        var color=net.minecraft.ChatFormatting.getByName(d.chatColor);
        if((hours(p)>=250||tier(p)>=4) && color!=null)name.withStyle(color);
        String prefix=(hours(p)>=500?"[Prestige] ":"")+d.prefix;
        if(!prefix.isBlank())e.setDisplayname(Component.literal(prefix+" ").append(name));else e.setDisplayname(name);
    }
    void chat(ServerChatEvent e) {
        if(state==null)return;
        var p=e.getPlayer();var color=net.minecraft.ChatFormatting.getByName(data(p).chatColor);
        if((hours(p)>=250||tier(p)>=4)&&color!=null)e.setMessage(e.getMessage().copy().withStyle(color));
    }
    static State.PlayerData data(ServerPlayer p) { return state.player(p.getUUID()); }
    static long hours(ServerPlayer p) { return data(p).activeSeconds/3600; }
    static int tier(ServerPlayer p) { return state.paid(p.getUUID()); }
    static void tell(ServerPlayer p,String msg) { p.sendSystemMessage(Component.literal("[Mythforge] "+msg)); }
    static void signal(ServerPlayer p,String signal) {
        if(p instanceof FakePlayer || state==null) return;
        var s=sessions.computeIfAbsent(p.getUUID(),k->new Session(p));
        if(signal.equals(s.lastSignal)) s.repetition++; else { s.lastSignal=signal; s.repetition=0; }
        if(s.repetition<20) s.lastAction=seconds;
    }
    void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||state==null) return;
        sessions.put(p.getUUID(),new Session(p)); data(p).name=p.getGameProfile().getName();
        reconcile(p); deliver(p); tell(p,"Welcome. /rewards, /vote, /crate, /rtp, /pwarp and /back are available.");
        if(!data(p).joinMessage.isBlank()) server.getPlayerList().broadcastSystemMessage(Component.literal(p.getGameProfile().getName()+": "+data(p).joinMessage),false);
        state.save();
    }
    void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        sessions.remove(e.getEntity().getUUID()); teleports.remove(e.getEntity().getUUID());
        if(state!=null) state.save();
    }
    void tick(ServerTickEvent.Post e) {
        if(state==null || e.getServer().getTickCount()%20!=0) return;
        seconds++;
        for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            Session s=sessions.computeIfAbsent(p.getUUID(),k->new Session(p));
            var d=data(p);
            double distance=Math.pow(p.getX()-s.x,2)+Math.pow(p.getY()-s.y,2)+Math.pow(p.getZ()-s.z,2);
            boolean intentional=distance>0.25 && (Math.abs(p.getYRot()-s.yaw)>2 || Math.abs(p.getXRot()-s.pitch)>2);
            if(intentional && seconds-s.lastAction<240) s.lastMovement=seconds;
            s.x=p.getX();s.y=p.getY();s.z=p.getZ();s.yaw=p.getYRot();s.pitch=p.getXRot();
            // Movement alone buys at most five minutes. Repeated identical block actions stop extending the window.
            if(!p.isSpectator() && !p.isDeadOrDying() && seconds-s.lastAction<300 && (seconds-s.lastMovement<90 || seconds-s.lastAction<90)) {
                d.activeSeconds++; s.lastCredit=seconds;
                if(d.activeSeconds%60==0) { reconcile(p); deliver(p); }
            }
            if(seconds-s.lastCredit==600) tell(p,"Active time is paused. Movement loops and repeated identical actions do not earn rewards.");
            if(seconds-s.lastCredit>=900 && !p.hasPermissions(4)) p.connection.disconnect(Component.literal("AFK for 15 minutes. Rejoin when ready to play."));
            if(seconds%10==0 && intentional && !p.isPassenger()) {
                if(d.visited.add(p.level().dimension().location()+":"+(p.blockPosition().getX()>>4)+":"+(p.blockPosition().getZ()>>4))) s.lastAction=seconds;
            }
            long now=System.currentTimeMillis();
            if(seconds%3==0&&hours(p)>=400&&!d.particle.equals("off")) {
                var type=BuiltInRegistries.PARTICLE_TYPE.get(ResourceLocation.parse("minecraft:"+d.particle));
                if(type instanceof net.minecraft.core.particles.SimpleParticleType simple)p.serverLevel().sendParticles(p,simple,false,p.getX(),p.getY()+1,p.getZ(),2,0.25,0.4,0.25,0.01);
            }
            if(s.customFlight && (s.combatUntil>seconds || !flightAllowed(p))) revokeFlight(p,s);
            if(d.flightUntil>0 && d.flightUntil<=now) { d.flightUntil=0; revokeFlight(p,s); }
            if(d.flightUntil>now && !s.customFlight && s.combatUntil<=seconds) { grantFlight(p,s); p.onUpdateAbilities(); }
            var tp=teleports.get(p.getUUID());
            if(tp!=null) {
                if(s.combatUntil>seconds || Math.pow(p.getX()-tp.x,2)+Math.pow(p.getY()-tp.y,2)+Math.pow(p.getZ()-tp.z,2)>0.25) {
                    teleports.remove(p.getUUID());tell(p,"Teleport cancelled by movement or combat.");
                } else if(seconds>=tp.due) {
                    teleports.remove(p.getUUID());
                    if(Travel.safe(p,tp.level,tp.pos)) p.teleportTo(tp.level,tp.pos.getX()+0.5,tp.pos.getY(),tp.pos.getZ()+0.5,p.getYRot(),p.getXRot());
                    else tell(p,"Destination became unsafe. Teleport cancelled.");
                }
            }
        }
        if(seconds%60==0) state.save();
    }
    static boolean combat(ServerPlayer p) { var s=sessions.get(p.getUUID());return s!=null&&s.combatUntil>seconds; }
    static boolean bossEncounter(ServerPlayer p) {
        return !p.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,p.getBoundingBox().inflate(32),e->e!=p && e.isAlive() && !(e instanceof Player) && e.getMaxHealth()>=150).isEmpty();
    }
    void damage(LivingDamageEvent.Post e) {
        if(e.getEntity() instanceof ServerPlayer p) { var s=sessions.get(p.getUUID());if(s!=null)s.combatUntil=seconds+30; }
        if(e.getSource().getEntity() instanceof ServerPlayer p) { var s=sessions.get(p.getUUID());if(s!=null)s.combatUntil=seconds+30; signal(p,"combat:"+e.getEntity().getType().toString()); }
    }
    void crafted(PlayerEvent.ItemCraftedEvent e) { if(e.getEntity() instanceof ServerPlayer p) signal(p,"craft:"+BuiltInRegistries.ITEM.getKey(e.getCrafting().getItem())); }
    static boolean machineryNamespace(String ns) { return ns.equals("create") || ns.equals("simulated") || ns.equals("sable") || ns.equals("aeronautics") || ns.startsWith("create_") || ns.equals("createaddition") || ns.equals("createthrusters"); }
    static boolean banned(ResourceLocation id) { return id.toString().equals("create:schematicannon") || id.getNamespace().equals("buildinggadgets2") || id.getPath().contains("schematicannon"); }
    static boolean machine(BlockState block) {
        var id=BuiltInRegistries.BLOCK.getKey(block.getBlock());
        return machineryNamespace(id.getNamespace()) && (block.getBlock() instanceof EntityBlock || config.getAsJsonArray("functionalBlocks").asList().stream().anyMatch(v->v.getAsString().equals(id.toString())));
    }
    static boolean unlocked(ServerPlayer p) {
        if(p instanceof FakePlayer) {
            // Fake actors may use machinery only inside a claim whose owner has earned access.
            var chunk=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(p.level().dimension(),p.chunkPosition()));
            if(chunk==null) return false;
            UUID owner=chunk.getTeamData().getTeam().getOwner();
            return state.player(owner).activeSeconds>=108000 || state.paid(owner)>0;
        }
        return hours(p)>=30 || tier(p)>0;
    }
    void place(BlockEvent.EntityPlaceEvent e) {
        if(e.getLevel().isClientSide() || state==null) return;
        var id=BuiltInRegistries.BLOCK.getKey(e.getPlacedBlock().getBlock());
        boolean deny=banned(id);
        if(machine(e.getPlacedBlock())) {
            if(e.getEntity() instanceof ServerPlayer p) deny|=!unlocked(p);
            else {
                var level=(ServerLevel)e.getLevel();
                var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),new ChunkPos(e.getPos())));
                UUID owner=claim==null?null:claim.getTeamData().getTeam().getOwner();
                deny|=owner==null || (state.player(owner).activeSeconds<108000 && state.paid(owner)==0);
            }
        }
        if(deny) { e.setCanceled(true); if(e.getEntity() instanceof ServerPlayer p && !(p instanceof FakePlayer)) tell(p,"Functional machinery needs 30 active hours or a supporter rank. Banned machinery stays banned."); }
        else if(e.getEntity() instanceof ServerPlayer p) signal(p,"place:"+e.getPos());
    }
    void breakBlock(BlockEvent.BreakEvent e) { if(e.getPlayer() instanceof ServerPlayer p) signal(p,"break:"+e.getPos()); }
    void interact(PlayerInteractEvent.RightClickBlock e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||state==null) return;
        var block=p.level().getBlockState(e.getPos());
        if(banned(BuiltInRegistries.BLOCK.getKey(block.getBlock())) || (machine(block)&&!unlocked(p))) { e.setCanceled(true); return; }
        if(banned(BuiltInRegistries.ITEM.getKey(e.getItemStack().getItem()))) { e.setCanceled(true); return; }
        signal(p,"use:"+e.getPos());
    }
    void useItem(PlayerInteractEvent.RightClickItem e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||state==null) return;
        var item=e.getItemStack(); var id=BuiltInRegistries.ITEM.getKey(item.getItem());
        if(banned(id)) { e.setCanceled(true); return; }
        if(!unlocked(p) && (id.getPath().contains("contraption") || id.getPath().contains("toolgun") || id.getPath().contains("assembly"))) { e.setCanceled(true); return; }
        var custom=item.get(DataComponents.CUSTOM_DATA);
        if(custom==null) return;
        String kind=custom.copyTag().getString("mythforge_consumable");
        if(kind.startsWith("flight_")) {
            e.setCanceled(true);
            if(combat(p)) { tell(p,"Flight consumables are disabled in combat."); return; }
            int minutes=kind.equals("flight_60")?60:10;
            data(p).flightUntil=Math.max(System.currentTimeMillis(),data(p).flightUntil)+minutes*60000L;
            if(!p.isCreative()) item.shrink(1);
            state.save(); tell(p,"Timed flight added: "+minutes+" minutes.");
        } else if(kind.equals("relic_level")) {
            e.setCanceled(true);
            int upgraded=Relics.levelEquipped(p);
            if(upgraded>0) { if(!p.isCreative())item.shrink(1);tell(p,"Leveled "+upgraded+" equipped relics."); }
            else tell(p,"No equipped non-max-level Relics items qualify. The consumable was kept.");
        }
    }
    static boolean flightAllowed(ServerPlayer p) {
        if(data(p).flightUntil>System.currentTimeMillis()) return true;
        if(hours(p)<200 && tier(p)<3) return false;
        var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(p.level().dimension(),p.chunkPosition()));
        return claim!=null && claim.getTeamData().isTeamMember(p.getUUID());
    }
    static void grantFlight(ServerPlayer p,Session s) {
        if(!s.customFlight)s.priorMayfly=p.getAbilities().mayfly;
        s.customFlight=true;p.getAbilities().mayfly=true;p.onUpdateAbilities();
    }
    static void revokeFlight(ServerPlayer p,Session s) {
        if(!s.customFlight)return;
        s.customFlight=false;
        if(!p.isCreative()&&!p.isSpectator()) { p.getAbilities().flying=s.priorMayfly && p.getAbilities().flying;p.getAbilities().mayfly=s.priorMayfly;p.onUpdateAbilities(); p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING,200,0,false,false)); }
    }
    static boolean cooldown(ServerPlayer p,String key,int sec) {
        var d=data(p);long now=System.currentTimeMillis();long until=d.cooldowns.getOrDefault(key,0L);
        if(until>now) { tell(p,"Wait "+((until-now+999)/1000)+" seconds.");return false; }
        d.cooldowns.put(key,now+sec*1000L);return true;
    }
    static void setNode(Rank r,String node,Object value) { r.setPermission(node,PermissionValue.parse(String.valueOf(value))); }
    static void reconcile(ServerPlayer p) {
        Rank rank=FTBRanksAPI.manager().getRank("earned_"+p.getUUID().toString().replace("-","")).orElseGet(()->FTBRanksAPI.manager().createRank("earned_"+p.getUUID().toString().replace("-",""),50,false));
        rank.add(p.getGameProfile());
        long h=hours(p);int paid=tier(p);
        int homes=(h>=500?5:h>=200?4:h>=70?3:h>=10?2:1)+(paid==0?0:paid+1);
        int warps=(h>=500?5:h>=400?4:h>=250?3:h>=100?2:h>=40?1:0)+new int[]{0,1,2,3,5}[paid];
        setNode(rank,"ftbessentials.home.max",homes);
        setNode(rank,"mythforge.pwarp.max",warps);
        setNode(rank,"ftbchunks.max_claimed",64+8*(h/10)+new int[]{0,32,64,128,256}[paid]);
        setNode(rank,"ftbchunks.max_force_loaded",2+h/50+new int[]{0,1,2,4,6}[paid]);
        for(String cmd:List.of("craft","crafting")) setNode(rank,"command."+cmd,h>=30||paid>=1);
        setNode(rank,"command.stonecutter",h>=20||paid>=1);
        setNode(rank,"command.nick",h>=40||paid>=1);
        setNode(rank,"command.nickname",h>=40||paid>=1);
        for(String cmd:List.of("echest","enderchest")) setNode(rank,"command."+cmd,h>=90||paid>=2);
        for(String cmd:List.of("anvil","smithing")) setNode(rank,"command."+cmd,h>=300||paid>=2);
        for(String cmd:List.of("trashcan","trash","disposal")) setNode(rank,"command."+cmd,h>=300||paid>=1);
        setNode(rank,"command.hat",h>=400);
        for(String id:List.of("ember","artisan","runeforged","mythic")) {
            var paidRank=FTBRanksAPI.manager().getRank(id).orElseThrow();
            if(paid>0 && id.equals(List.of("ember","artisan","runeforged","mythic").get(paid-1)))paidRank.add(p.getGameProfile());else paidRank.remove(p.getGameProfile());
        }
        // Explicitly avoid raw /tp, /time, /weather, /effect, /give or cross-player utility grants.
    }
    static void deliver(ServerPlayer p) { Rewards.deliver(p); }
    static void audit() { Rewards.audit(); }
    void commands(RegisterCommandsEvent e) { CommandsSetup.register(e.getDispatcher()); }
    void commandGuard(CommandEvent e) {
        var source=e.getParseResults().getContext().getSource();
        if(!(source.getEntity() instanceof ServerPlayer p)||state==null) return;
        String input=e.getParseResults().getReader().getString().trim();
        String[] words=input.split("\\s+");String root=words[0].replaceFirst("^[^:]+:","");
        if(Set.of("home","back","spawn","warp","tpa","tpaccept","tpahere","rtp","ascend","waypoint").contains(root) && combat(p)) { e.setCanceled(true);tell(p,"Travel is disabled for 30 seconds after combat."); }
        if(root.equals("ftbchunks") && words.length>1 && !Set.of("claim","unclaim","unclaim_all","load","unload","unload_all","info","map","block_color","waypoint").contains(words[1]) && !p.hasPermissions(4)) { e.setCanceled(true); }
        if((root.equals("create")||root.equals("sable")||root.equals("aeronautics")) && !unlocked(p)) { e.setCanceled(true);tell(p,"Machinery access unlocks at 30 active hours."); }
    }
}
