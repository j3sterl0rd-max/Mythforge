package dev.mythforge.server;

import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.*;
import java.nio.file.*;
import java.util.*;

final class Rewards {
    static List<String> relicChoices() {return BuiltInRegistries.ITEM.stream().filter(item->item instanceof it.hurts.sskirillss.relics.api.relics.IRelicItem).map(item->BuiltInRegistries.ITEM.getKey(item).toString()).filter(id->!ServerTools.banned(ResourceLocation.parse(id))).sorted().toList();}
    static boolean crateValid;
    static List<String> errors=new ArrayList<>();
    static void audit() {
        errors.clear();int sum=0;Map<String,Integer> totals=new LinkedHashMap<>();
        for(var value:ServerTools.config.getAsJsonArray("crate")) {
            var row=value.getAsJsonObject();int weight=row.get("weight").getAsInt();sum+=weight;
            totals.merge(row.get("tier").getAsString(),weight,Integer::sum);validate(row.getAsJsonArray("items"));
        }
        if(sum!=10000)errors.add("Crate weight is "+sum+", expected 10000");
        for(var entry:ServerTools.config.getAsJsonObject("milestones").entrySet())validate(entry.getValue().getAsJsonArray());
        for(var entry:ServerTools.config.getAsJsonObject("paidItems").entrySet())validate(entry.getValue().getAsJsonArray());
        crateValid=errors.isEmpty();
        try { Files.writeString(Path.of("ops/evidence/reward-audit.json"),State.JSON.toJson(Map.of("totalWeight",sum,"tiers",totals,"valid",crateValid,"errors",errors))); }
        catch(Exception ex) {throw new RuntimeException(ex);}
        if(!crateValid)ServerTools.LOG.error("[Mythforge] Rewards disabled until registry audit is clean: {}",errors);
    }
    static void validate(JsonArray items) {
        for(var item:items) {
            var spec=item.getAsJsonObject();
            if(spec.has("pool")) {
                var pool=ServerTools.config.getAsJsonObject("pools").getAsJsonArray(spec.get("pool").getAsString());
                if(pool==null||pool.isEmpty()) {errors.add("Empty pool "+spec.get("pool"));continue;}
                for(var id:pool) validateId(id.getAsString());
            } else if(spec.has("id")) validateId(spec.get("id").getAsString());
        }
    }
    static void validateId(String id) {
        if(!BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)))errors.add("Unknown item "+id);
    }
    static ItemStack stack(ServerPlayer p,JsonObject spec) {
        var item=new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.get("id").getAsString())),spec.get("count").getAsInt());
        if(spec.has("custom")) {
            String kind=spec.get("custom").getAsString();var tag=new CompoundTag();tag.putString("mythforge_consumable",kind);
            item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
            item.set(DataComponents.CUSTOM_NAME,Component.literal(kind.equals("relic_choice")?"Runeforged Relic Choice Voucher":kind.equals("relic_level")?"Relic Level-Up Consumable":kind.equals("flight_60")?"60-Minute Creative Flight Potion":"10-Minute Creative Flight Potion"));
            item.set(DataComponents.LORE,new ItemLore(List.of(Component.literal("Right-click to use. Server-managed; no effect when no eligible target exists."))));
        }
        if(spec.has("maxed")&&spec.get("maxed").getAsBoolean())Relics.max(p,item);
        return item;
    }
    static void enqueue(ServerPlayer p,JsonArray items) {
        for(var raw:items) {
            var spec=raw.getAsJsonObject().deepCopy();
            if(spec.has("keys")) { ServerTools.data(p).keys+=spec.get("keys").getAsInt();continue; }
            if(spec.has("relicChoice")) {var voucher=new JsonObject();voucher.addProperty("id","minecraft:paper");voucher.addProperty("count",1);voucher.addProperty("custom","relic_choice");spec=voucher;}
            if(spec.has("pool")) {
                var pool=ServerTools.config.getAsJsonObject("pools").getAsJsonArray(spec.remove("pool").getAsString());
                spec.addProperty("id",pool.get(ServerTools.RANDOM.nextInt(pool.size())).getAsString());
            }
            var item=BuiltInRegistries.ITEM.get(ResourceLocation.parse(spec.get("id").getAsString()));
            int remaining=spec.get("count").getAsInt();int max=new ItemStack(item).getMaxStackSize();
            while(remaining>0) {
                var part=spec.deepCopy();int count=Math.min(max,remaining);part.addProperty("count",count);part.addProperty("grant",UUID.randomUUID().toString());
                ServerTools.data(p).pending.add(State.JSON.toJson(part));remaining-=count;
            }
        }
    }
    static void deliver(ServerPlayer p) {
        if(!crateValid) return;
        var d=ServerTools.data(p);
        for(var entry:ServerTools.config.getAsJsonObject("milestones").entrySet()) {
            int hours=Integer.parseInt(entry.getKey());
            if(ServerTools.hours(p)>=hours&&d.delivered.add(hours)) { enqueue(p,entry.getValue().getAsJsonArray());ServerTools.tell(p,"Milestone reached: "+hours+" active hours."); }
        }
        for(int paid=d.paidDelivered+1;paid<=ServerTools.tier(p);paid++) {
            enqueue(p,ServerTools.config.getAsJsonObject("paidItems").getAsJsonArray(String.valueOf(paid)));d.paidDelivered=paid;
        }
        ServerTools.state.save();Relics.slots(p);mail(p);
    }
    static void mail(ServerPlayer p) {
        var d=ServerTools.data(p);int delivered=0;
        var receipt=p.getPersistentData().getCompound("mythforge_delivery_receipts");
        var completed=new ArrayList<String>();
        for(String pending:d.pending) {
            var spec=JsonParser.parseString(pending).getAsJsonObject();String grant=spec.get("grant").getAsString();
            if(receipt.getBoolean(grant)) {completed.add(pending);continue;}
            if(p.getInventory().getFreeSlot()<0)break;
            var item=stack(p,spec);
            if(p.getInventory().add(item)) {receipt.putBoolean(grant,true);completed.add(pending);delivered++;}
            else throw new IllegalStateException("Reward insertion failed despite reserved inventory space");
        }
        p.getPersistentData().put("mythforge_delivery_receipts",receipt);
        if(!completed.isEmpty()) {
            // Inventory and receipt live in the same atomic player save. Replay after a crash checks that receipt.
            ServerTools.server.getPlayerList().saveAll();
            d.pending.removeAll(completed);ServerTools.state.save();
        }
        if(delivered>0) {p.inventoryMenu.broadcastChanges();ServerTools.tell(p,"Delivered "+delivered+" reward stacks.");}
        if(!d.pending.isEmpty())ServerTools.tell(p,"Inventory full: "+d.pending.size()+" stacks remain in /mail.");
    }
    static void open(ServerPlayer p) {
        if(!crateValid) {ServerTools.tell(p,"Crate temporarily disabled pending registry verification.");return;}
        var d=ServerTools.data(p);if(d.keys<1) {ServerTools.tell(p,"No keys. Vote for the server to earn one.");return;}
        int roll=ServerTools.RANDOM.nextInt(10000);
        for(var raw:ServerTools.config.getAsJsonArray("crate")) {
            var row=raw.getAsJsonObject();roll-=row.get("weight").getAsInt();
            if(roll<0) {
                d.keys--;enqueue(p,row.getAsJsonArray("items"));ServerTools.state.save();
                ServerTools.tell(p,"Crate reward: "+row.get("name").getAsString()+" ["+row.get("tier").getAsString()+"]");mail(p);return;
            }
        }
        throw new IllegalStateException("Crate selection escaped configured weight");
    }
}
