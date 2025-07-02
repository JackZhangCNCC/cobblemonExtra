package com.johnson.cobblemonextra.config;

import com.johnson.cobblemonextra.CobblemonExtra;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

/**
 * CobblemonExtra模组配置
 * 管理动感超人面具等道具的效果配置
 */
public class CobblemonExtraConfig {
    
    public static class Client {
        public final ModConfigSpec.DoubleValue actionHeroMaskPowerMultiplier;
        
        Client(ModConfigSpec.Builder builder) {
            builder.comment("动感超人面具配置")
                   .comment("Action Hero Mask Configuration")
                   .push("action_hero_mask");
            
            actionHeroMaskPowerMultiplier = builder
                .comment("动感超人面具的威力倍数")
                .comment("新之助携带动感超人面具时，所有招式威力的倍数")
                .comment("范围：1.0 - 10.0，默认值：1.5")
                .comment("Power multiplier for Action Hero Mask")
                .comment("When Xiaoxin holds Action Hero Mask, all move power is multiplied by this value")
                .comment("Range: 1.0 - 10.0, Default: 1.5")
                .defineInRange("power_multiplier", 1.5, 1.0, 10.0);
            
            builder.pop();
        }
    }
    
    public static final ModConfigSpec CLIENT_SPEC;
    public static final Client CLIENT;
    
    static {
        final Pair<Client, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(Client::new);
        CLIENT_SPEC = specPair.getRight();
        CLIENT = specPair.getLeft();
    }
    
    /**
     * 获取动感超人面具的威力倍数
     * 包含范围验证，超出范围时返回默认值
     */
    public static double getActionHeroMaskPowerMultiplier() {
        double value = CLIENT.actionHeroMaskPowerMultiplier.get();
        
        // 验证范围
        if (value < 1.0 || value > 10.0) {
            CobblemonExtra.LOGGER.warn("动感超人面具威力倍数超出范围: {}，重置为默认值 1.5", value);
            // 重置为默认值
            CLIENT.actionHeroMaskPowerMultiplier.set(1.5);
            return 1.5;
        }
        
        // 精确到小数点后一位
        return Math.round(value * 10.0) / 10.0;
    }
    
    /**
     * 验证并修复配置值
     * 在模组启动时调用
     */
    public static void validateAndFixConfig() {
        double originalValue = CLIENT.actionHeroMaskPowerMultiplier.get();
        double correctedValue = getActionHeroMaskPowerMultiplier();
        
        if (originalValue != correctedValue) {
            CobblemonExtra.LOGGER.info("配置值已修正：动感超人面具威力倍数从 {} 修正为 {}", originalValue, correctedValue);
            CLIENT.actionHeroMaskPowerMultiplier.set(correctedValue);
        } else {
            CobblemonExtra.LOGGER.info("动感超人面具威力倍数配置：{}", correctedValue);
        }
    }
} 