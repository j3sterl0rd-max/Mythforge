package dev.mythforge.server;
import com.google.gson.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.*;
import java.nio.file.*;
import java.util.*;
public final class Voting {
    static JsonArray sites() {
        try {return JsonParser.parseString(Files.readString(Path.of("config/mythforge-voting-sites.json"))).getAsJsonArray();}
        catch(Exception error){return new JsonArray();}
    }
    static final Map<String,Long> reminders=new HashMap<>();
    static boolean ready(ServerPlayer p,JsonObject site) {
        String key=p.getGameProfile().getName().toLowerCase(Locale.ROOT)+":"+site.get("service").getAsString().toLowerCase(Locale.ROOT);
        return System.currentTimeMillis()-ServerTools.state.voteTimes.getOrDefault(key,0L)>=site.get("cooldownHours").getAsLong()*3600000L;
    }
    static void remind(ServerPlayer p) {
        long now=System.currentTimeMillis();String id=p.getUUID().toString();
        if(now-reminders.getOrDefault(id,0L)<3600000)return;
        long available=sites().asList().stream().map(JsonElement::getAsJsonObject).filter(s->ready(p,s)).count();
        if(available>0){ServerTools.tell(p,"Voting is available on "+available+" site(s). Use /vote for links and earn crate keys.");reminders.put(id,now);}
    }
    static void show(ServerPlayer p) {
        var sites=sites();if(sites.isEmpty()){ServerTools.tell(p,"Voting listings have not been connected yet. Existing keys remain available through /crate.");return;}
        for(var value:sites) {
            var s=value.getAsJsonObject();String url=s.get("url").getAsString();if(!url.startsWith("https://"))continue;
            p.sendSystemMessage(Component.literal("[Vote] "+s.get("name").getAsString()+" - "+(ready(p,s)?"Available":"Waiting for the site's cooldown"))
                .withStyle(style->style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL,url))));
        }
        ServerTools.tell(p,"The voting site decides final eligibility. Each verified vote awards one key.");
    }
    public static void received(Object vote) {
        if(ServerTools.server==null)return;
        try {
            String username=(String)vote.getClass().getMethod("getUsername").invoke(vote);
            String service=(String)vote.getClass().getMethod("getServiceName").invoke(vote);
            ServerTools.server.execute(()->{if(ServerTools.state!=null){ServerTools.state.voteTimes.put(username.toLowerCase(Locale.ROOT)+":"+service.toLowerCase(Locale.ROOT),System.currentTimeMillis());ServerTools.state.save();}});
        } catch(ReflectiveOperationException error){ServerTools.LOG.error("Cannot record vote reminder cooldown",error);}
    }
}
