package com.johnson.cobblemonextra;

import com.johnson.cobblemonextra.config.CobblemonExtraConfig;
import com.johnson.cobblemonextra.item.CobblemonExtraItems;
import com.johnson.cobblemonextra.item.CobblemonExtraCreativeTab;
import com.johnson.cobblemonextra.showdown.ShowdownDataManager;
import com.johnson.cobblemonextra.showdown.ShowdownInterceptionManager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(CobblemonExtra.MOD_ID)
public class CobblemonExtra {
    public static final String MOD_ID = "cobblemonextra";
    public static final Logger LOGGER = LoggerFactory.getLogger("CobblemonExtra");
    
    public CobblemonExtra(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("CobblemonExtra开始初始化...");

        // 初始化通用Showdown拦截系统
        LOGGER.info("🚀 初始化通用Showdown拦截系统...");
        try {
            ShowdownInterceptionManager.initialize();
            LOGGER.info("✅ 通用Showdown拦截系统初始化成功！");
        } catch (Exception e) {
            LOGGER.error("❌ 通用Showdown拦截系统初始化失败", e);
        }

        // 重新启用早期创建，为智能追加提供基础
        LOGGER.info("正在进行早期Showdown文件创建，为智能追加提供基础...");
        try {
            ShowdownDataManager.injectShowdown();
            LOGGER.info("早期Showdown文件创建成功！");
        } catch (Exception e) {
            LOGGER.error("早期Showdown文件创建失败，将在后续事件中重试", e);
        }
        
        // 注册配置
        modContainer.registerConfig(ModConfig.Type.CLIENT, CobblemonExtraConfig.CLIENT_SPEC);
        
        // 注册到模组事件总线
        modEventBus.addListener(this::commonSetup);
        
        // 注册道具
        CobblemonExtraItems.register(modEventBus);
        
        // 注册创造栏
        CobblemonExtraCreativeTab.register(modEventBus);
        
        LOGGER.info("CobblemonExtra初始化完成！");
    }
    
    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            LOGGER.info("CobblemonExtra通用设置开始...");
            
            // 验证和修复配置
            CobblemonExtraConfig.validateAndFixConfig();
            
            // 注册动感超人面具的held item效果
            registerHeldItemEffects();
            
            LOGGER.info("CobblemonExtra通用设置完成！");
        });
    }
    
    /**
     * 注册携带道具效果
     */
    private void registerHeldItemEffects() {
        try {
            LOGGER.info("注册动感超人面具的held item效果...");
            
            // 尝试通过反射注册动感超人面具到Showdown ID的映射
            try {
                Class<?> heldItemManagerClass = Class.forName("com.cobblemon.mod.common.pokemon.helditem.CobblemonHeldItemManager");
                Object heldItemManagerInstance = heldItemManagerClass.getField("INSTANCE").get(null);
                
                // 获取Item类
                Class<?> itemClass = Class.forName("net.minecraft.world.item.Item");
                
                // 获取registerRemap方法
                java.lang.reflect.Method registerRemapMethod = heldItemManagerClass.getMethod("registerRemap",
                    itemClass, String.class);

                // 获取ACTION_HERO_MASK道具
                Class<?> itemsClass = Class.forName("com.johnson.cobblemonextra.item.CobblemonExtraItems");
                java.lang.reflect.Field actionHeroMaskField = itemsClass.getField("ACTION_HERO_MASK");
                Object actionHeroMaskDeferred = actionHeroMaskField.get(null);
                java.lang.reflect.Method getMethod = actionHeroMaskDeferred.getClass().getMethod("get");
                Object actionHeroMaskItem = getMethod.invoke(actionHeroMaskDeferred);

                // 注册映射：action_hero_mask -> actionheromask
                registerRemapMethod.invoke(heldItemManagerInstance,
                    actionHeroMaskItem,
                    "actionheromask");
                
                LOGGER.info("动感超人面具映射注册成功：action_hero_mask -> actionheromask");
                
            } catch (ClassNotFoundException e) {
                LOGGER.warn("未找到Cobblemon或Minecraft类，跳过映射注册: {}", e.getMessage());
            } catch (Exception e) {
                LOGGER.warn("注册held item映射时出错: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            LOGGER.warn("注册held item效果时出错: {}", e.getMessage());
        }
    }
} 