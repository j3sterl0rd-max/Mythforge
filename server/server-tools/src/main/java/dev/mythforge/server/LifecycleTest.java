package dev.mythforge.server;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

/** Regression checks run against the installed server runtime by /mythforge roleaudit. */
final class LifecycleTest {
    static void run(ServerPlayer p,List<String> passed) {
        var mode=p.gameMode.getGameModeForPlayer();
        boolean mayfly=p.getAbilities().mayfly,flying=p.getAbilities().flying;
        float health=p.getHealth();
        double x=p.getX(),y=p.getY(),z=p.getZ();
        try {
            p.setGameMode(GameType.SURVIVAL);
            p.getAbilities().mayfly=false;p.getAbilities().flying=false;
            var session=new ServerTools.Session(p);
            ServerTools.grantFlight(p,session);
            SelfTest.check(p.getAbilities().mayfly&&p.getPersistentData().contains("mythforge_flight",10),"Temporary flight persists its ownership marker",passed);
            ServerTools.grantFlight(p,session);
            SelfTest.check(!session.priorMayfly,"Repeated flight grants preserve the original permission",passed);
            var reconnect=new SelfTest.TestPlayer(p.serverLevel(),p.getGameProfile());
            reconnect.getPersistentData().merge(p.getPersistentData().copy());
            reconnect.getAbilities().mayfly=true;reconnect.getAbilities().flying=true;
            ServerTools.restoreFlight(reconnect);
            SelfTest.check(!reconnect.getAbilities().mayfly&&!reconnect.getAbilities().flying&&!reconnect.getPersistentData().contains("mythforge_flight"),"Reconnect clears saved temporary flight even without an in-memory session",passed);
            ServerTools.revokeFlight(p,session);
            SelfTest.check(!p.getAbilities().mayfly&&!session.customFlight&&!p.getPersistentData().contains("mythforge_flight"),"Logout/shutdown cleanup removes only the temporary flight grant",passed);

            p.getAbilities().mayfly=true;
            ServerTools.grantFlight(p,session);ServerTools.revokeFlight(p,session);
            SelfTest.check(p.getAbilities().mayfly,"An existing independent flight permission survives cleanup",passed);
            p.setGameMode(GameType.CREATIVE);
            ServerTools.grantFlight(p,session);p.setGameMode(GameType.SURVIVAL);ServerTools.revokeFlight(p,session);
            SelfTest.check(!p.getAbilities().mayfly,"Creative-mode flight is not restored as a survival permission",passed);
            p.setGameMode(GameType.CREATIVE);ServerTools.grantFlight(p,session);ServerTools.revokeFlight(p,session);
            SelfTest.check(p.getAbilities().mayfly,"Cleanup retains flight while the player remains creative",passed);

            p.setGameMode(GameType.SURVIVAL);p.setHealth(20);p.setPos(x,y,z);
            var pending=new ServerTools.PendingTeleport(p.serverLevel(),BlockPos.ZERO,5,x,y,z,false,p.serverLevel().dimension());
            SelfTest.check(pending.validSource(p),"Stationary living player retains the pending teleport",passed);
            p.setPos(x+1,y,z);
            SelfTest.check(!pending.validSource(p),"Movement cancels the pending teleport",passed);
            p.setPos(x,y,z);p.setHealth(0);
            SelfTest.check(!pending.validSource(p),"Death cancels the pending teleport",passed);
            p.setHealth(20);
            var other=new SelfTest.TestPlayer(ServerTools.server.getLevel(Level.NETHER),p.getGameProfile());
            other.setPos(x,y,z);
            SelfTest.check(!pending.validSource(other),"Changing dimension cancels a teleport even at identical coordinates",passed);
        } finally {
            p.getPersistentData().remove("mythforge_flight");
            p.setGameMode(mode);p.setPos(x,y,z);p.setHealth(health);
            p.getAbilities().mayfly=mayfly;p.getAbilities().flying=flying;
        }
    }
}
