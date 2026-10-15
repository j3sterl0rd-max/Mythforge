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
        var entitlement=new State.Purchase(id.toString(),rank);
        entitlement.revoked=ServerTools.state.revokedTransactions.contains(txn);
        ServerTools.state.purchases.put(txn,entitlement);ServerTools.state.save();
        var p=ServerTools.server.getPlayerList().getPlayer(id);
        if(p!=null) {ServerTools.reconcile(p);Rewards.deliver(p);}
        return true;
    }
    static boolean revoke(String txn) {
        if(!txn.matches("[A-Za-z0-9_-]{1,100}"))return false;
        ServerTools.state.revokedTransactions.add(txn);
        var purchase=ServerTools.state.purchases.get(txn);if(purchase==null){ServerTools.state.save();return true;}
        purchase.revoked=true;ServerTools.state.save();
        var p=ServerTools.server.getPlayerList().getPlayer(uuid(purchase.uuid));if(p!=null)ServerTools.reconcile(p);
        return true;
    }
}
