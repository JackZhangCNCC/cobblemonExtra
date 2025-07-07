package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.ShowdownInterceptionConfig;
import net.neoforged.fml.ModList;

/**
 * Showdown拦截管理器
 * 负责初始化和管理通用Showdown拦截系统
 */
public class ShowdownInterceptionManager {
    
    private static boolean initialized = false;
    
    /**
     * 初始化Showdown拦截系统
     */
    public static void initialize() {
        if (initialized) {
            CobblemonExtra.LOGGER.warn("⚠️ Showdown拦截系统已经初始化过了");
            return;
        }
        
        try {
            CobblemonExtra.LOGGER.info("🚀 初始化通用Showdown拦截系统...");
            
            // 检测已安装的mod并自动配置
            detectAndConfigureMods();
            
            // 打印当前配置
            ShowdownInterceptionConfig.printCurrentConfig();
            
            initialized = true;
            CobblemonExtra.LOGGER.info("✅ 通用Showdown拦截系统初始化完成！");
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 初始化Showdown拦截系统失败", e);
        }
    }
    
    /**
     * 检测已安装的mod并自动配置拦截
     */
    private static void detectAndConfigureMods() {
        CobblemonExtra.LOGGER.info("🔍 检测已安装的Showdown相关mod...");
        
        ModList modList = ModList.get();
        int detectedCount = 0;
        
        // 检查已知的mod
        for (String modId : ShowdownInterceptionConfig.KNOWN_MODS.keySet()) {
            if (modList.isLoaded(modId)) {
                ShowdownInterceptionConfig.ModInterceptionInfo info = 
                    ShowdownInterceptionConfig.KNOWN_MODS.get(modId);
                
                if (!info.isEnabled()) {
                    // 自动启用检测到的mod
                    ShowdownInterceptionConfig.setModEnabled(modId, true);
                    CobblemonExtra.LOGGER.info("🔧 自动启用 {} 的拦截", modId);
                }
                
                detectedCount++;
                CobblemonExtra.LOGGER.info("✅ 检测到支持的mod: {}", modId);
            } else {
                // 禁用未安装的mod
                ShowdownInterceptionConfig.setModEnabled(modId, false);
                CobblemonExtra.LOGGER.debug("⚠️ 未安装mod: {}", modId);
            }
        }
        
        // 检查其他可能的Showdown mod
        detectUnknownShowdownMods(modList);
        
        CobblemonExtra.LOGGER.info("📊 检测完成，找到 {} 个支持的Showdown mod", detectedCount);
    }
    
    /**
     * 检测未知的Showdown mod
     */
    private static void detectUnknownShowdownMods(ModList modList) {
        // 检查可能包含Showdown功能的mod
        String[] suspiciousKeywords = {
            "showdown", "battle", "pokemon", "cobblemon", "extended"
        };
        
        modList.getMods().forEach(modInfo -> {
            String modId = modInfo.getModId();
            String displayName = modInfo.getDisplayName().toLowerCase();
            
            // 跳过已知的mod
            if (ShowdownInterceptionConfig.KNOWN_MODS.containsKey(modId) || 
                "cobblemonextra".equals(modId) || 
                "cobblemon".equals(modId) ||
                "minecraft".equals(modId) ||
                "neoforge".equals(modId)) {
                return;
            }
            
            // 检查是否包含可疑关键词
            for (String keyword : suspiciousKeywords) {
                if (modId.contains(keyword) || displayName.contains(keyword)) {
                    CobblemonExtra.LOGGER.info("🤔 发现可能的Showdown相关mod: {} ({})", 
                        modId, modInfo.getDisplayName());
                    CobblemonExtra.LOGGER.info("   💡 如果此mod使用Showdown注入，请手动添加拦截配置");
                    break;
                }
            }
        });
    }
    
    /**
     * 添加运行时mod拦截配置
     */
    @SuppressWarnings("unused")
    public static void addRuntimeModConfig(String modId, String targetClass, String targetMethod) {
        try {
            // 使用默认配置
            ShowdownInterceptionConfig.addModConfig(
                modId,
                targetClass,
                targetMethod,
                200, // 默认优先级
                java.util.Arrays.asList("/" + modId + "/", "/showdown/"),
                true
            );
            
            CobblemonExtra.LOGGER.info("✅ 运行时添加mod拦截配置: {}", modId);
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 添加运行时mod配置失败: {}", modId, e);
        }
    }
    
    /**
     * 获取拦截统计信息
     */
    @SuppressWarnings("unused")
    public static InterceptionStats getStats() {
        return new InterceptionStats(
            ShowdownInterceptionConfig.getEnabledMods().size(),
            UniversalShowdownMerger.getProcessedMods().size(),
            ShowdownInterceptionConfig.KNOWN_MODS.size()
        );
    }
    
    /**
     * 重置拦截系统
     */
    @SuppressWarnings("unused")
    public static void reset() {
        UniversalShowdownMerger.reset();
        initialized = false;
        CobblemonExtra.LOGGER.info("🔄 Showdown拦截系统已重置");
    }
    
    /**
     * 拦截统计信息
     */
    public static class InterceptionStats {
        private final int enabledMods;
        private final int processedMods;
        private final int knownMods;
        
        public InterceptionStats(int enabledMods, int processedMods, int knownMods) {
            this.enabledMods = enabledMods;
            this.processedMods = processedMods;
            this.knownMods = knownMods;
        }
        
        @SuppressWarnings("unused")
        public int getEnabledMods() { return enabledMods; }
        @SuppressWarnings("unused")
        public int getProcessedMods() { return processedMods; }
        @SuppressWarnings("unused")
        public int getKnownMods() { return knownMods; }
        
        @Override
        public String toString() {
            return String.format("InterceptionStats{启用: %d, 已处理: %d, 已知: %d}", 
                enabledMods, processedMods, knownMods);
        }
    }
    
    /**
     * 打印详细状态
     */
    @SuppressWarnings("unused")
    public static void printDetailedStatus() {
        CobblemonExtra.LOGGER.info("📊 Showdown拦截系统状态:");
        CobblemonExtra.LOGGER.info("  初始化状态: {}", initialized ? "✅ 已初始化" : "❌ 未初始化");
        
        InterceptionStats stats = getStats();
        CobblemonExtra.LOGGER.info("  统计信息: {}", stats);
        
        CobblemonExtra.LOGGER.info("  启用的mod: {}", ShowdownInterceptionConfig.getEnabledMods());
        CobblemonExtra.LOGGER.info("  已处理的mod: {}", UniversalShowdownMerger.getProcessedMods());
    }
}

/*
 * 使用说明：
 * 
 * 1. 在mod初始化时调用：
 *    ShowdownInterceptionManager.initialize();
 * 
 * 2. 运行时添加新mod配置：
 *    ShowdownInterceptionManager.addRuntimeModConfig(
 *        "new_mod", 
 *        "com.example.NewMod", 
 *        "injectShowdown"
 *    );
 * 
 * 3. 获取统计信息：
 *    InterceptionStats stats = ShowdownInterceptionManager.getStats();
 * 
 * 4. 打印详细状态：
 *    ShowdownInterceptionManager.printDetailedStatus();
 * 
 * 5. 重置系统（用于调试）：
 *    ShowdownInterceptionManager.reset();
 */
