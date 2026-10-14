package dev.mythforge.server;
import journeymap.common.network.model.Location;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerPlayer;

public final class Waypoints {
    public static boolean allowed(ServerPlayer p) {return ServerTools.state!=null&&(ServerTools.hours(p)>=150||ServerTools.tier(p)>=3);}
    public static boolean request(ServerPlayer p,Location location) {
        if(!allowed(p)||ServerTools.combat(p))return false;
        var level=ServerTools.server.getLevel(ResourceKey.create(Registries.DIMENSION,ResourceLocation.parse(location.getDim())));
        var pos=BlockPos.containing(location.getX(),location.getY(),location.getZ());
        String key=location.getDim()+":"+(pos.getX()>>4)+":"+(pos.getZ()>>4);
        if(level==null||!ServerTools.data(p).visited.contains(key)||!Travel.permitted(p,level)) {ServerTools.tell(p,"Waypoint destination must be safe, permitted, and personally visited.");return false;}
        if(!ServerTools.cooldown(p,"waypoint",120))return false;
        Travel.queue(p,level,pos);return true;
    }
}
