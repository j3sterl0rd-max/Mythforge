package dev.mythforge.server;

import it.hurts.sskirillss.relics.api.relics.IRelicItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.api.CuriosApi;

final class Relics {
    static int levelEquipped(ServerPlayer p) {
        int result=0;
        var inventory=CuriosApi.getCuriosInventory(p).orElse(null);
        if(inventory==null)return 0;
        var items=inventory.getEquippedCurios();
        for(int i=0;i<items.getSlots();i++) {
            var stack=items.getStackInSlot(i);
            if(stack.getItem() instanceof IRelicItem relic) {
                var data=relic.getRelicData(p,stack);
                if(!data.isMaxLevel()&&data.getLevelingData().addLevel(1))result++;
            }
        }
        return result;
    }
    static void max(ServerPlayer p,ItemStack stack) {
        if(!(stack.getItem() instanceof IRelicItem relic)) throw new IllegalStateException("Not a Relics item");
        var data=relic.getRelicData(p,stack);
        var leveling=data.getLevelingData();
        leveling.setRank(leveling.getTemplate().getMaxRank());
        leveling.setLevel(data.calculateMaxLevel());
        for(var ability:data.getAbilitiesData().getAbilities().values()) {
            int max=ability.getTemplate().getInitialMaxLevel()+(int)(ability.getTemplate().getMaxLevelRankModifier()*leveling.getRank());
            ability.setLevel(max);
            ability.getResearchData().complete();
            ability.getLockData().setUnlocks(ability.getLockData().getMaxUnlocks());
            for(var stat:ability.getTemplate().getStats().keySet()) {
                var statData=ability.getStatData(stat);statData.setInitialQuality(statData.getMaxQuality());
            }
        }
        if(!data.isMaxLevel()||!data.isMaxRank())throw new IllegalStateException("Relic maximum validation failed");
    }
    static void slots(ServerPlayer p) {
        var inventory=CuriosApi.getCuriosInventory(p).orElse(null);if(inventory==null)return;
        long h=ServerTools.hours(p);
        for(String slot:new String[]{"hands","necklace","trinket"}) {
            int count=slot.equals("hands")?(h>=300?2:h>=120?1:0):slot.equals("necklace")?(h>=150?1:0):(h>=200?1:0);
            if(inventory.getStacksHandler(slot).isEmpty())continue;
            var id=ResourceLocation.parse("mythforge_server:earned_"+slot);
            inventory.removeSlotModifier(slot,id);
            if(count>0) inventory.addPermanentSlotModifier(slot,id,count,AttributeModifier.Operation.ADD_VALUE);
        }
    }
}
