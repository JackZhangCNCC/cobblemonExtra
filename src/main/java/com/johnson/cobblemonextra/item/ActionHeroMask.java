package com.johnson.cobblemonextra.item;

import com.johnson.cobblemonextra.config.CobblemonExtraConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 这个文件是动感超人面具的物品类，用于新之助的专属道具
 * 动感超人面具 - 新之助专属道具
 * 效果：新之助携带后，所有招式威力增加
 */
public class ActionHeroMask extends Item {
    
    public ActionHeroMask(Properties properties) {
        super(properties);
    }
    
    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
        
        // 获取当前配置的威力倍数
        double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
        int powerPercentage = (int) Math.round((powerMultiplier - 1.0) * 100);
        
        // 动态显示威力提升百分比
        tooltipComponents.add(Component.translatable("item.cobblemonextra.action_hero_mask.tooltip", powerPercentage)
                .withStyle(ChatFormatting.YELLOW));
        
        // 显示当前倍数（仅在调试模式下）
        if (tooltipFlag.isAdvanced()) {
            tooltipComponents.add(Component.literal("威力倍数: " + powerMultiplier + "x")
                    .withStyle(ChatFormatting.GRAY));
        }
    }
} 