package dev.mythforge.server;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
final class PvpSettings {
    static void attack(LivingIncomingDamageEvent event) {
        if(ServerTools.state==null)return;
        if(event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof ServerPlayer attacker && victim!=attacker
            && (!ServerTools.data(victim).pvp || !ServerTools.data(attacker).pvp))event.setCanceled(true);
    }
}
