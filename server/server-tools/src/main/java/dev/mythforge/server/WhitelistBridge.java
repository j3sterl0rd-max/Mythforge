package dev.mythforge.server;

import com.google.gson.*;
import com.mojang.authlib.GameProfile;
import com.sun.net.httpserver.HttpServer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.UserWhiteListEntry;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.UUID;
import java.util.concurrent.*;

/** Narrow loopback-only whitelist API. No RCON or arbitrary command execution. */
final class WhitelistBridge {
    private static HttpServer listener;
    private static ExecutorService executor;
    static void start(MinecraftServer server) {
        Path tokenFile=Path.of("ops/private/whitelist/bridge-token.txt");
        if(!Files.exists(tokenFile))return;
        try {
            String secret=Files.readString(tokenFile).strip();
            if(secret.length()<48)throw new IllegalStateException("Whitelist bridge token is too short");
            listener=HttpServer.create(new InetSocketAddress("127.0.0.1",18766),16);
            executor=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"mythforge-whitelist");t.setDaemon(true);return t;});
            listener.setExecutor(executor);
            listener.createContext("/whitelist",exchange->{
                String response="{\"ok\":false}";
                try {
                    String auth=exchange.getRequestHeaders().getFirst("Authorization");
                    if(!"POST".equals(exchange.getRequestMethod())||!exchange.getRequestURI().toString().equals("/whitelist")||auth==null||
                       !MessageDigest.isEqual(auth.getBytes(StandardCharsets.UTF_8),("Bearer "+secret).getBytes(StandardCharsets.UTF_8)))throw new IllegalArgumentException();
                    byte[] input=exchange.getRequestBody().readNBytes(2049);
                    if(input.length>2048)throw new IllegalArgumentException();
                    JsonObject task=JsonParser.parseString(new String(input,StandardCharsets.UTF_8)).getAsJsonObject();
                    String id=task.get("uuid").getAsString(), name=task.get("name").getAsString(), operation=task.get("operation").getAsString();
                    if(!id.matches("[a-f0-9]{32}")||!name.matches("[A-Za-z0-9_]{3,16}")||!(operation.equals("ADD")||operation.equals("CHECK")))throw new IllegalArgumentException();
                    UUID uuid=UUID.fromString(id.replaceFirst("(........)(....)(....)(....)(............)","$1-$2-$3-$4-$5"));
                    var completed=new CompletableFuture<JsonObject>();
                    server.execute(()->{
                        try {
                            if(!server.usesAuthentication()||!server.getPlayerList().isUsingWhitelist())throw new IllegalStateException("Authentication and whitelist must be enabled");
                            GameProfile profile=new GameProfile(uuid,name);
                            if(server.getPlayerList().getBans().isBanned(profile)){
                                var denied=new JsonObject();denied.addProperty("error","LOCAL_BAN");completed.complete(denied);return;
                            }
                            if(operation.equals("ADD"))server.getPlayerList().getWhiteList().add(new UserWhiteListEntry(profile));
                            boolean present=server.getPlayerList().getWhiteList().isWhiteListed(profile);
                            // The provider's in-memory entry alone is insufficient if persistence failed.
                            if(present){
                                var disk=JsonParser.parseString(Files.readString(Path.of("whitelist.json"))).getAsJsonArray();
                                present=false;
                                for(var item:disk)if(item.getAsJsonObject().get("uuid").getAsString().equals(uuid.toString())){present=true;break;}
                            }
                            var result=new JsonObject();result.addProperty("uuid",id);result.addProperty("present",present);result.addProperty("verified",present);completed.complete(result);
                        }catch(Exception error){completed.completeExceptionally(error);}
                    });
                    response=completed.get(15,TimeUnit.SECONDS).toString();
                }catch(Exception error){ /* Trusted connector retries; do not expose local details. */ }
                byte[] bytes=response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type","application/json");
                exchange.getResponseHeaders().set("Cache-Control","no-store");
                exchange.sendResponseHeaders(200,bytes.length);
                try(var output=exchange.getResponseBody()){output.write(bytes);}
                exchange.close();
            });
            listener.start();
            ServerTools.LOG.info("[Mythforge] Whitelist bridge listening on loopback port 18766");
        }catch(Exception error){stop();throw new IllegalStateException("Whitelist bridge could not start",error);}
    }
    static void stop(){if(listener!=null){listener.stop(0);listener=null;}if(executor!=null){executor.shutdownNow();executor=null;}}
}
