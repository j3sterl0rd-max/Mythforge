package dev.mythforge.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import java.nio.file.*;
import java.util.*;

final class State {
    static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    Map<String, PlayerData> players = new LinkedHashMap<>();
    Map<String, Purchase> purchases = new LinkedHashMap<>();
    Set<String> revokedTransactions = new HashSet<>();
    Map<String, Warp> warps = new LinkedHashMap<>();
    transient Path path;
    static final class PlayerData {
        long activeSeconds;
        int keys;
        int paidDelivered;
        long flightUntil;
        Set<Integer> delivered = new HashSet<>();
        Set<String> visited = new HashSet<>();
        Map<String, Long> cooldowns = new HashMap<>();
        List<String> pending = new ArrayList<>();
        String name = "", prefix = "", joinMessage = "";
        int personalTime=-1;
        String personalWeather="reset",chatColor="white",particle="off";
    }
    static final class Purchase {
        String uuid, rank;
        boolean revoked;
        Purchase(String uuid, String rank) { this.uuid=uuid; this.rank=rank; }
    }
    static final class Warp {
        String owner, dimension, icon = "minecraft:ender_pearl";
        double x,y,z;
        Warp(String owner, String dimension, double x, double y, double z) { this.owner=owner;this.dimension=dimension;this.x=x;this.y=y;this.z=z; }
    }
    PlayerData player(UUID uuid) { return players.computeIfAbsent(uuid.toString(), k -> new PlayerData()); }
    int paid(UUID uuid) {
        return purchases.values().stream().filter(p -> !p.revoked && p.uuid.equals(uuid.toString()))
            .mapToInt(p -> List.of("ember","artisan","runeforged","mythic").indexOf(p.rank)+1).max().orElse(0);
    }
    static State load(MinecraftServer server) {
        Path path=server.getWorldPath(LevelResource.ROOT).resolve("mythforge/state.json");
        try {
            State state=Files.exists(path) ? JSON.fromJson(Files.readString(path), State.class) : new State();
            if(state==null) throw new IllegalStateException("Empty state");
            state.path=path;
            return state;
        } catch(Exception ex) { throw new IllegalStateException("Mythforge state cannot be read; refusing to reset entitlements",ex); }
    }
    void save() {
        try {
            Files.createDirectories(path.getParent());
            Path temp=path.resolveSibling("state.json.tmp");
            Files.writeString(temp, JSON.toJson(this));
            if(Files.exists(path)) Files.copy(path,path.resolveSibling("state.json.previous"),StandardCopyOption.REPLACE_EXISTING);
            try { Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException ex) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        } catch(Exception ex) { throw new IllegalStateException("Mythforge state cannot be saved",ex); }
    }
}
