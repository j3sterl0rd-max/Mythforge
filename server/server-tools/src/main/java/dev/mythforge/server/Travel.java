package dev.mythforge.server;

import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import java.util.*;

final class Travel {
    static final Set<UUID> searching=new HashSet<>();
    record Reservation(ServerLevel level,ChunkPos chunk,long id) {}
    static final Map<UUID,Reservation> reservations=new HashMap<>();
    static void release(ServerPlayer player) {
        var reservation=reservations.remove(player.getUUID());
        if(reservation!=null)reservation.level.getChunkSource().removeRegionTicket(FTBTravel.TICKET,reservation.chunk,0,reservation.id);
    }
    static boolean permitted(ServerPlayer p,ServerLevel level) {
        String dim=level.dimension().location().toString();
        return dim.equals("minecraft:overworld") || p.level().dimension().equals(level.dimension()) || ServerTools.data(p).visited.stream().anyMatch(v->v.startsWith(dim+":")) || dim.equals("minecraft:the_nether")&&canRtp(p,"the_nether") || dim.equals("minecraft:the_end")&&canRtp(p,"the_end");
    }
    static boolean canRtp(ServerPlayer p,String dimension) {
        return dimension.equals("overworld") || dimension.equals("the_nether") && (ServerTools.hours(p)>=300 || ServerTools.tier(p)>=2) || dimension.equals("the_end") && (ServerTools.hours(p)>=400 || ServerTools.tier(p)>=3);
    }
    static boolean safe(ServerPlayer p,ServerLevel level,BlockPos pos) {
        if(!permitted(p,level)||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunk(pos.getX()>>4,pos.getZ()>>4)) return false;
        var below=level.getBlockState(pos.below());
        if(!below.isCollisionShapeFullBlock(level,pos.below()) || !below.getFluidState().isEmpty() || below.is(Blocks.MAGMA_BLOCK)||below.is(Blocks.CAMPFIRE)||below.is(Blocks.CACTUS)||below.is(Blocks.BEDROCK)) return false;
        for(int i=0;i<2;i++) {
            var at=pos.above(i);var block=level.getBlockState(at);
            if(!block.getCollisionShape(level,at).isEmpty()||!block.getFluidState().isEmpty()||block.is(Blocks.FIRE)||block.is(Blocks.SOUL_FIRE)||block.is(Blocks.NETHER_PORTAL)||block.is(Blocks.END_PORTAL)) return false;
        }
        var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),new ChunkPos(pos)));
        if(claim!=null&&!claim.getTeamData().isTeamMember(p.getUUID())) {
            boolean consent=ServerTools.state.warps.values().stream().anyMatch(w->w.dimension.equals(level.dimension().location().toString()) && BlockPos.containing(w.x,w.y,w.z).equals(pos) && claim.getTeamData().isTeamMember(UUID.fromString(w.owner)));
            if(!consent)return false;
        }
        return level.noCollision(p,new AABB(pos.getX()+0.2,pos.getY(),pos.getZ()+0.2,pos.getX()+0.8,pos.getY()+1.8,pos.getZ()+0.8));
    }
    static void queue(ServerPlayer p,ServerLevel level,BlockPos pos) {
        if(!permitted(p,level)||ServerTools.combat(p))return;
        release(p);ServerTools.teleports.remove(p.getUUID());
        var reservation=new Reservation(level,new ChunkPos(pos),FTBTravel.IDS.incrementAndGet());
        reservations.put(p.getUUID(),reservation);
        level.getChunkSource().addRegionTicket(FTBTravel.TICKET,reservation.chunk,0,reservation.id);
        FTBTravel.prepare(level,pos.getX()>>4,pos.getZ()>>4).whenComplete((chunk,error)->ServerTools.server.execute(()->{
            if(reservations.get(p.getUUID())!=reservation)return;
            if(p.hasDisconnected()){release(p);return;}
            if(error!=null||ServerTools.combat(p)||!safe(p,level,pos)){release(p);ServerTools.tell(p,"Destination is unsafe or unavailable. Teleport cancelled.");return;}
            ServerTools.teleports.put(p.getUUID(),new ServerTools.PendingTeleport(level,pos,ServerTools.seconds+5,p.getX(),p.getY(),p.getZ()));
            ServerTools.tell(p,"Teleport in 5 seconds. Stay still and out of combat.");
        }));
    }
    static void rtp(ServerPlayer p,String dimension) {
        var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("minecraft:"+dimension));
        var level=ServerTools.server.getLevel(key);
        if(level==null || !canRtp(p,dimension)) { ServerTools.tell(p,"Nether RTP needs Artisan or 300 active hours; End RTP needs Runeforged or 400 active hours.");return; }
        if(ServerTools.combat(p)||searching.contains(p.getUUID())||ServerTools.teleports.containsKey(p.getUUID())) { ServerTools.tell(p,"Already searching, teleporting, or in combat.");return; }
        if(!ServerTools.cooldown(p,"rtp",600)) return;
        searching.add(p.getUUID()); ServerTools.tell(p,"Finding safe, unclaimed terrain...");
        search(p,level,0);
    }
    static void search(ServerPlayer p,ServerLevel level,int attempt) {
        FTBTravel.search(p,level,attempt).whenComplete((pos,error)->ServerTools.server.execute(()->{
            searching.remove(p.getUUID());
            if(p.hasDisconnected())return;
            if(error!=null || pos==null){ServerTools.data(p).cooldowns.remove("rtp");ServerTools.tell(p,"No safe destination found. Try again.");if(error!=null)ServerTools.LOG.error("FTB RTP preparation failed",error);return;}
            queue(p,level,pos);
        }));
    }
    static void ascend(ServerPlayer p) {
        if(ServerTools.hours(p)<100&&ServerTools.tier(p)<2) { ServerTools.tell(p,"Ascend needs 100 active hours or Artisan.");return; }
        if(ServerTools.combat(p)||!ServerTools.cooldown(p,"ascend",120)) return;
        var level=p.serverLevel();int max=level.dimension()==Level.NETHER?120:level.getMaxBuildHeight()-3;
        for(int y=p.blockPosition().getY()+2;y<max;y++) {
            var pos=new BlockPos(p.blockPosition().getX(),y,p.blockPosition().getZ());
            if(safe(p,level,pos)) { queue(p,level,pos);return; }
        }
        ServerTools.tell(p,"No safe space above you.");
    }
}
