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
        return true; // Access no longer depends on previously visiting a dimension.
    }
    static boolean canRtp(ServerPlayer p,String dimension) {
        return RtpUnlocks.allowed(dimension,ServerTools.hours(p),ServerTools.tier(p));
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
    static boolean destinationAllowed(ServerPlayer p,ServerLevel level,BlockPos pos,boolean requireSafe) {
        // Explicit destinations do not require a safe-ground or visited-dimension check.
        if(!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)||!level.hasChunk(pos.getX()>>4,pos.getZ()>>4))return false;
        var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),new ChunkPos(pos)));
        if(claim==null||claim.getTeamData().isTeamMember(p.getUUID()))return true;
        return ServerTools.state.warps.values().stream().anyMatch(w->w.dimension.equals(level.dimension().location().toString()) && BlockPos.containing(w.x,w.y,w.z).equals(pos) && claim.getTeamData().isTeamMember(UUID.fromString(w.owner)));
    }
    static void queue(ServerPlayer p,ServerLevel level,BlockPos pos) {
        queue(p,level,pos,true);
    }
    static void queue(ServerPlayer p,ServerLevel level,BlockPos pos,boolean requireSafe) {
        if(!level.isInWorldBounds(pos)||!level.getWorldBorder().isWithinBounds(pos)){ServerTools.tell(p,"Destination is outside world bounds.");return;}
        release(p);ServerTools.teleports.remove(p.getUUID());
        var origin=p.serverLevel().dimension();
        var reservation=new Reservation(level,new ChunkPos(pos),FTBTravel.IDS.incrementAndGet());
        reservations.put(p.getUUID(),reservation);
        level.getChunkSource().addRegionTicket(FTBTravel.TICKET,reservation.chunk,0,reservation.id);
        FTBTravel.prepare(level,pos.getX()>>4,pos.getZ()>>4).whenComplete((chunk,error)->ServerTools.server.execute(()->{
            if(reservations.get(p.getUUID())!=reservation)return;
            if(p.hasDisconnected()||p.isDeadOrDying()||!p.serverLevel().dimension().equals(origin)){release(p);return;}
            if(error!=null||!destinationAllowed(p,level,pos,requireSafe)){release(p);ServerTools.tell(p,requireSafe?"Destination is unsafe or unavailable. Teleport cancelled.":"Waypoint destination is unavailable or claim access is denied.");return;}
            ServerTools.teleports.put(p.getUUID(),new ServerTools.PendingTeleport(level,pos,ServerTools.seconds+5,p.getX(),p.getY(),p.getZ(),requireSafe,origin));
            ServerTools.tell(p,"Teleport in 5 seconds. Stay still and avoid taking damage.");
        }));
    }
    static void rtp(ServerPlayer p,String dimension) {
        var key=ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse("minecraft:"+dimension));
        var level=ServerTools.server.getLevel(key);
        if(level==null || !canRtp(p,dimension)) { ServerTools.tell(p,"Nether RTP needs Artisan or "+RtpUnlocks.NETHER_HOURS+" active hours; End RTP needs Runeforged or "+RtpUnlocks.END_HOURS+" active hours.");return; }
        if(searching.contains(p.getUUID())||ServerTools.teleports.containsKey(p.getUUID())) { ServerTools.tell(p,"Already searching or teleporting.");return; }
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
        if(!CommandPolicy.owner(p)&&ServerTools.hours(p)<100&&ServerTools.tier(p)<2) { ServerTools.tell(p,"Ascend needs 100 active hours or Artisan.");return; }
        var level=p.serverLevel();int max=level.dimension()==Level.NETHER?120:level.getMaxBuildHeight()-3;
        for(int y=p.blockPosition().getY()+2;y<max;y++) {
            var pos=new BlockPos(p.blockPosition().getX(),y,p.blockPosition().getZ());
            if(safe(p,level,pos)) {
                release(p);ServerTools.teleports.remove(p.getUUID());
                queue(p,level,pos,false);return;
            }
        }
        ServerTools.tell(p,"No safe space above you.");
    }
}
