package dev.mythforge.server;
import java.nio.file.*;
import com.google.gson.JsonParser;
final class RtpUnlocks {
    static final int NETHER_HOURS;
    static final int END_HOURS;
    static {
        int nether=40,end=80;
        Path file=Path.of("config/mythforge-rtp.json");
        if(Files.exists(file)) {
            try {
                var json=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                nether=json.get("nether_active_hours").getAsInt();
                end=json.get("end_active_hours").getAsInt();
                if(nether<=0 || end<=0)throw new IllegalArgumentException("RTP hours must be positive");
            } catch(Exception error) {throw new IllegalStateException("Invalid config/mythforge-rtp.json",error);}
        }
        NETHER_HOURS=nether;END_HOURS=end;
    }
    static boolean allowed(String dimension,long hours,int tier) {
        return dimension.equals("overworld")
            || dimension.equals("the_nether") && (hours>=NETHER_HOURS || tier>=2)
            || dimension.equals("the_end") && (hours>=END_HOURS || tier>=3);
    }
}
