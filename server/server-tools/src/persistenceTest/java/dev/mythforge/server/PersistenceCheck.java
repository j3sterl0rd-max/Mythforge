package dev.mythforge.server;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.UUID;

/** Exercise ledger writes without starting Minecraft or touching production state. */
public final class PersistenceCheck {
    public static void main(String[] arguments) throws Exception {
        var directory=Files.createTempDirectory("mythforge-ledger-check-");
        var state=new State();state.path=directory.resolve("state.json");
        var id=UUID.fromString("00000000-0000-0000-0000-000000000001");
        state.player(id).keys=1;state.save();
        require(Files.exists(state.path),"initial save creates ledger");
        var sentinel=FileTime.fromMillis(1000000);
        Files.setLastModifiedTime(state.path,sentinel);state.save();
        require(Files.getLastModifiedTime(state.path).equals(sentinel),"unchanged saves do not rewrite ledger");
        state.player(id).keys=2;state.save();
        var saved=State.JSON.fromJson(Files.readString(state.path),State.class);
        require(saved.player(id).keys==2,"changed state is immediately persisted");
        var previous=State.JSON.fromJson(Files.readString(directory.resolve("state.json.previous")),State.class);
        require(previous.player(id).keys==1,"previous ledger retains earlier state");
        require(!Files.exists(directory.resolve("state.json.tmp")),"atomic replacement leaves no pending temporary file");
        // All three paths are created exclusively in the temporary test directory.
        Files.delete(directory.resolve("state.json.previous"));Files.delete(state.path);Files.delete(directory);
        System.out.println("Persistence checks: 5 passed.");
    }
    private static void require(boolean condition,String label){if(!condition)throw new AssertionError(label);}
}
