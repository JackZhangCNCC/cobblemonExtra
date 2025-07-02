package com.johnson.cobblemonextra.item;

import com.johnson.cobblemonextra.CobblemonExtra;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class CobblemonExtraItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CobblemonExtra.MOD_ID);
    
    // 动感超人面具 - 新之助专属道具，威力增加
    public static final DeferredItem<Item> ACTION_HERO_MASK = ITEMS.register("action_hero_mask",
            () -> new ActionHeroMask(new Item.Properties()));
    
    // 新之助mega石 - 简单道具注册（没有特殊功能）
    public static final DeferredItem<Item> XIAOXIN_MEGA_STONE = ITEMS.register("xiaoxinite",
            () -> new Item(new Item.Properties()));
    
    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
} 