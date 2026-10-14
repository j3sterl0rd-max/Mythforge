package dev.mythforge.server.mixin;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.concurrent.ConcurrentLinkedQueue;

/** A collider rebuild must never re-enter the Rust engine while its simulation step is active. */
@Mixin(targets="dev.ryanhcode.sable.physics.impl.rapier.RapierPhysicsPipeline")
public abstract class RapierReentrancyMixin {
    @Unique private volatile boolean mythforge$stepping;
    @Unique private final ConcurrentLinkedQueue<Runnable> mythforge$deferred=new ConcurrentLinkedQueue<>();
    @Shadow(remap=false) public abstract void handleBlockChange(SectionPos section,LevelChunkSection chunk,int x,int y,int z,BlockState before,BlockState after);
    @Inject(method="physicsTick(D)V",at=@At("HEAD"),remap=false)
    private void mythforge$begin(double delta,CallbackInfo ci) {mythforge$stepping=true;}
    @Inject(method="physicsTick(D)V",at=@At("RETURN"),remap=false)
    private void mythforge$finish(double delta,CallbackInfo ci) {
        mythforge$stepping=false;
        Runnable update;while((update=mythforge$deferred.poll())!=null)update.run();
    }
    @Inject(method="handleBlockChange",at=@At("HEAD"),cancellable=true,remap=false)
    private void mythforge$deferCollider(SectionPos section,LevelChunkSection chunk,int x,int y,int z,BlockState before,BlockState after,CallbackInfo ci) {
        if(mythforge$stepping) {mythforge$deferred.add(()->handleBlockChange(section,chunk,x,y,z,before,after));ci.cancel();}
    }
}
