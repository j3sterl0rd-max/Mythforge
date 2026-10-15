package dev.mythforge.server;

import dev.mythforge.server.mixin.FTBRtpInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import java.util.concurrent.CompletableFuture;

/** Prepare native FTB search terrain without blocking the server tick on generation. */
public final class FTBTravel {
    static final net.minecraft.server.level.TicketType<Long> TICKET=net.minecraft.server.level.TicketType.create("mythforge_travel",Long::compareTo);
    static final java.util.concurrent.atomic.AtomicLong IDS=new java.util.concurrent.atomic.AtomicLong();
    public static final class Sample {
        final double radial,angular;
        int reads;
        Sample(double radial,double angular){this.radial=radial;this.angular=angular;}
        public double next(){return reads++%2==0?radial:angular;}
    }
    public static final ThreadLocal<Sample> PREPARED=new ThreadLocal<>();
    static CompletableFuture<?> prepare(ServerLevel level,int x,int z) {
        var source=level.getChunkSource();
        // Minecraft's public API blocks when called ON the server thread. Its documented
        // other-thread branch schedules the request back to the server without managedBlock.
        return CompletableFuture.supplyAsync(()->source.getChunkFuture(x,z,ChunkStatus.FULL,true)).thenCompose(future->future);
    }
    static CompletableFuture<BlockPos> search(ServerPlayer player,ServerLevel level,int attempt) {
        if(attempt>=32 || player.hasDisconnected())return CompletableFuture.completedFuture(null);
        int radius=level.dimension()==Level.END?2500:1500;
        var sample=new Sample(ServerTools.RANDOM.nextDouble(),ServerTools.RANDOM.nextDouble());
        double distance=500+sample.radial*(radius-500),angle=sample.angular*Math.PI*2;
        int x=(int)Math.floor(Math.cos(angle)*distance),z=(int)Math.floor(Math.sin(angle)*distance);
        var chunkPos=new ChunkPos(x>>4,z>>4);
        if(!level.getWorldBorder().isWithinBounds(new BlockPos(x,64,z)) || FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),chunkPos))!=null)return search(player,level,attempt+1);
        var result=new CompletableFuture<BlockPos>();
        var chunks=new java.util.ArrayList<CompletableFuture<?>>();
        long reservation=IDS.incrementAndGet();
        var preparedChunks=new java.util.ArrayList<ChunkPos>();
        // Native Nether spiral can inspect terrain up to 16 blocks from the sampled column.
        for(int cx=chunkPos.x-1;cx<=chunkPos.x+1;cx++)for(int cz=chunkPos.z-1;cz<=chunkPos.z+1;cz++) {
            var preparedChunk=new ChunkPos(cx,cz);preparedChunks.add(preparedChunk);
            level.getChunkSource().addRegionTicket(TICKET,preparedChunk,0,reservation);
            chunks.add(prepare(level,cx,cz));
        }
        CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new)).whenComplete((ignored,error)->ServerTools.server.execute(()->{
            try {
                if(error!=null){result.completeExceptionally(error);return;}
                if(player.hasDisconnected()){result.complete(null);return;}
                PREPARED.set(sample);
                var destination=FTBRtpInvoker.mythforge$find(level,player,500,radius).getPos();
                for(int offset=0;offset>=-1;offset--) {
                    var landing=destination.above(offset);
                    var claim=FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level.dimension(),new ChunkPos(landing)));
                    if(claim==null && !(level.dimension()==Level.NETHER && landing.getY()>=121) && Travel.safe(player,level,landing)) {result.complete(landing);return;}
                }
            } catch(Exception failure){result.completeExceptionally(failure);return;}
            finally {PREPARED.remove();for(var preparedChunk:preparedChunks)level.getChunkSource().removeRegionTicket(TICKET,preparedChunk,0,reservation);}
            search(player,level,attempt+1).whenComplete((position,failure)->{if(failure==null)result.complete(position);else result.completeExceptionally(failure);});
        }));
        return result;
    }
}
