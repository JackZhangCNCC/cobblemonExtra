package com.johnson.cobblemonextra.item;

import com.johnson.cobblemonextra.CobblemonExtra;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class CobblemonExtraCreativeTab {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = 
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CobblemonExtra.MOD_ID);

    public static final Supplier<CreativeModeTab> COBBLEMON_EXTRA_TAB = CREATIVE_MODE_TABS.register("cobblemonextra_tab", () -> 
        CreativeModeTab.builder()
            .icon(() -> new ItemStack(CobblemonExtraItems.ACTION_HERO_MASK.get()))
            .title(Component.translatable("itemGroup.cobblemonextra"))
            .displayItems((parameters, output) -> {
                // 添加我们模组的所有物品
                output.accept(CobblemonExtraItems.ACTION_HERO_MASK.get());
                output.accept(CobblemonExtraItems.XIAOXIN_MEGA_STONE.get());
            })
            .build()
    );

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
} 