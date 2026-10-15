package dev.mythforge.server.mixin;

import dev.mythforge.server.FTBTravel;
import dev.ftb.mods.ftbessentials.commands.groups.TeleportingCommands;
import dev.ftb.mods.ftblibrary.snbt.config.IntValue;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TeleportingCommands.class)
public abstract class FTBRtpPreparationMixin {
    @Redirect(method="findBlockPos",at=@At(value="INVOKE",target="Lnet/minecraft/util/RandomSource;nextDouble()D"),require=2)
    private static double mythforge$preparedColumn(RandomSource random) {
        var prepared=FTBTravel.PREPARED.get();return prepared==null?random.nextDouble():prepared.next();
    }
    @Redirect(method="findBlockPos",at=@At(value="INVOKE",target="Ldev/ftb/mods/ftblibrary/snbt/config/IntValue;get()Ljava/lang/Object;",remap=false),remap=false)
    private static Object mythforge$onePreparedAttempt(IntValue value) {
        return FTBTravel.PREPARED.get()==null?value.get():Integer.valueOf(1);
    }
}
