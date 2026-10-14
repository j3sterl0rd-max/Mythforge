package dev.mythforge.server;

import java.util.*;
final class Payments {
    static UUID uuid(String input) {
        if(input.matches("[A-Fa-f0-9]{32}"))input=input.replaceFirst("(.{8})(.{4})(.{4})(.{4})(.{12})","$1-$2-$3-$4-$5");
        return UUID.fromString(input);
    }
    static boolean purchase(String player,String rank,String txn) {
        UUID id=uuid(player);
        if(!List.of("ember","artisan","runeforged","mythic").contains(rank)||!txn.matches("[A-Za-z0-9_-]{1,100}"))return false;
        var old=ServerTools.state.purchases.get(txn);
        if(old!=null) {
            if(!old.uuid.equals(id.toString())||!old.rank.equals(rank))throw new IllegalStateException("Transaction identity conflict");
            return true;
        }
        ServerTools.state.purchases.put(txn,new State.Purchase(id.toString(),rank));ServerTools.state.save();
        var p=ServerTools.server.getPlayerList().getPlayer(id);
        if(p!=null) {ServerTools.reconcile(p);Rewards.deliver(p);}
        return true;
    }
    static boolean revoke(String txn) {
        var purchase=ServerTools.state.purchases.get(txn);if(purchase==null)return false;
        purchase.revoked=true;ServerTools.state.save();
        var p=ServerTools.server.getPlayerList().getPlayer(uuid(purchase.uuid));if(p!=null)ServerTools.reconcile(p);
        return true;
    }
}
