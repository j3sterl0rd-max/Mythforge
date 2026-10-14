package dev.mythforge.server;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.server.level.ServerPlayer;

public final class PersonalView {
    public static Packet<?> rewrite(ServerPlayer p,Packet<?> packet) {
        if(ServerTools.state==null)return packet;
        var data=ServerTools.data(p);
        if(packet instanceof ClientboundSetTimePacket time && data.personalTime>=0) return new ClientboundSetTimePacket(time.getGameTime(),data.personalTime,false);
        if(packet instanceof ClientboundGameEventPacket event && !data.personalWeather.equals("reset")) {
            boolean rain=data.personalWeather.equals("rain");var type=event.getEvent();
            if(type==ClientboundGameEventPacket.START_RAINING||type==ClientboundGameEventPacket.STOP_RAINING)return new ClientboundGameEventPacket(rain?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0);
            if(type==ClientboundGameEventPacket.RAIN_LEVEL_CHANGE)return new ClientboundGameEventPacket(type,rain?1:0);
            if(type==ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE)return new ClientboundGameEventPacket(type,0);
        }
        return packet;
    }
    public static void send(ServerPlayer p) {
        p.connection.send(new ClientboundSetTimePacket(p.serverLevel().getGameTime(),p.serverLevel().getDayTime(),true));
        p.connection.send(new ClientboundGameEventPacket(p.serverLevel().isRaining()?ClientboundGameEventPacket.START_RAINING:ClientboundGameEventPacket.STOP_RAINING,0));
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.RAIN_LEVEL_CHANGE,p.serverLevel().getRainLevel(1)));
        p.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.THUNDER_LEVEL_CHANGE,p.serverLevel().getThunderLevel(1)));
    }
}
