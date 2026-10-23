package dev.mythforge.server.mixin;
import dev.mythforge.server.Voting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(targets="com.uberswe.votifier.Votifier",remap=false)
public abstract class VoteReminderMixin {
    @Inject(method="onVoteReceived",at=@At("HEAD"),remap=false)
    private void mythforge$recordVote(@Coerce Object vote,CallbackInfo ci){Voting.received(vote);}
}
