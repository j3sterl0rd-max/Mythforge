package dev.mythforge.server;

import com.google.gson.JsonObject;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.loot.RewardTable;
import dev.ftb.mods.ftbquests.quest.loot.WeightedReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.util.RandomSource;
import java.util.IdentityHashMap;
import java.util.Map;

/** FTB owns weighted selection; keys and complex item delivery stay per player. */
final class CrateTable {
    static RewardTable table;
    static final Map<WeightedReward,JsonObject> rows=new IdentityHashMap<>();
    static void initialize() {
        var file=ServerQuestFile.getInstance().orElseThrow();
        table=new RewardTable(0x4d59465400000001L,file,"Mythforge vote rewards");
        rows.clear();long index=0;
        for(var value:ServerTools.config.getAsJsonArray("crate")) {
            var row=value.getAsJsonObject();
            var reward=new ItemReward(0x4d59465400000100L+index++,table.getFakeQuest(),new ItemStack(Items.PAPER));
            var weighted=new WeightedReward(reward,row.get("weight").getAsInt());
            table.addReward(weighted);rows.put(weighted,row);
        }
        if(table.getTotalWeight(false)!=10000)throw new IllegalStateException("FTB crate weight mismatch");
    }
    static JsonObject draw(RandomSource random) {
        // Retry the provider's zero-float boundary without consuming a key.
        for(int attempt=0;attempt<8;attempt++) {
            var rewards=table.generateWeightedRandomRewards(random,1,false);
            if(!rewards.isEmpty())return rows.get(rewards.iterator().next());
        }
        throw new IllegalStateException("FTB reward selector returned no reward");
    }
}
