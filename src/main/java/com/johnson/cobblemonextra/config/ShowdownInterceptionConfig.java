package com.johnson.cobblemonextra.config;

import com.johnson.cobblemonextra.CobblemonExtra;
import java.util.*;

/**
 * Showdown拦截配置
 * 管理要拦截的mod和它们的优先级
 */
public class ShowdownInterceptionConfig {
    
    /**
     * 已知的Showdown注入mod配置
     */
    public static final Map<String, ModInterceptionInfo> KNOWN_MODS = new HashMap<>();
    
    static {
        // gravels_extended_battles
        KNOWN_MODS.put("gravels_extended_battles", new ModInterceptionInfo(
            "gravels_extended_battles",
            "drai.dev.gravelsextendedbattles.showdown.ShowdownFileManager",
            "injectShowdown",
            500,
            Arrays.asList("/drai/dev/gravelsextendedbattles/", "/gravelsextendedbattles/"),
            true
        ));
        
        // mega_showdown (如果需要拦截)
        KNOWN_MODS.put("mega_showdown", new ModInterceptionInfo(
            "mega_showdown",
            "com.example.megashowdown.ShowdownManager", // 示例类名
            "injectShowdown",
            400,
            Arrays.asList("/megashowdown/", "/showdown/"),
            false // 默认不启用，因为可能不需要拦截
        ));
        
        // 可以在这里添加更多mod
        // KNOWN_MODS.put("other_mod", new ModInterceptionInfo(...));
    }
    
    /**
     * 检查是否应该拦截指定的mod
     */
    public static boolean shouldInterceptMod(String modId) {
        ModInterceptionInfo info = KNOWN_MODS.get(modId);
        return info != null && info.isEnabled();
    }
    
    /**
     * 获取mod的优先级
     */
    public static int getModPriority(String modId) {
        ModInterceptionInfo info = KNOWN_MODS.get(modId);
        return info != null ? info.getPriority() : 100; // 默认优先级
    }
    
    /**
     * 获取mod的资源路径
     */
    @SuppressWarnings("unused")
    public static List<String> getModResourcePaths(String modId) {
        ModInterceptionInfo info = KNOWN_MODS.get(modId);
        return info != null ? info.getResourcePaths() : Collections.emptyList();
    }
    
    /**
     * 获取所有启用的mod
     */
    public static Set<String> getEnabledMods() {
        Set<String> enabled = new HashSet<>();
        for (Map.Entry<String, ModInterceptionInfo> entry : KNOWN_MODS.entrySet()) {
            if (entry.getValue().isEnabled()) {
                enabled.add(entry.getKey());
            }
        }
        return enabled;
    }
    
    /**
     * 启用或禁用mod拦截
     */
    public static void setModEnabled(String modId, boolean enabled) {
        ModInterceptionInfo info = KNOWN_MODS.get(modId);
        if (info != null) {
            info.setEnabled(enabled);
            CobblemonExtra.LOGGER.info("🔧 {} 拦截已{}", modId, enabled ? "启用" : "禁用");
        }
    }
    
    /**
     * 添加新的mod配置
     */
    public static void addModConfig(String modId, String targetClass, String targetMethod, 
                                   int priority, List<String> resourcePaths, boolean enabled) {
        KNOWN_MODS.put(modId, new ModInterceptionInfo(
            modId, targetClass, targetMethod, priority, resourcePaths, enabled));
        CobblemonExtra.LOGGER.info("🔧 添加新的mod拦截配置: {}", modId);
    }
    
    /**
     * Mod拦截信息类
     */
    public static class ModInterceptionInfo {
        private final String modId;
        private final String targetClass;
        private final String targetMethod;
        private final int priority;
        private final List<String> resourcePaths;
        private boolean enabled;
        
        public ModInterceptionInfo(String modId, String targetClass, String targetMethod,
                                 int priority, List<String> resourcePaths, boolean enabled) {
            this.modId = modId;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
            this.priority = priority;
            this.resourcePaths = new ArrayList<>(resourcePaths);
            this.enabled = enabled;
        }
        
        // Getters
        public String getModId() { return modId; }
        public String getTargetClass() { return targetClass; }
        public String getTargetMethod() { return targetMethod; }
        public int getPriority() { return priority; }
        public List<String> getResourcePaths() { return new ArrayList<>(resourcePaths); }
        public boolean isEnabled() { return enabled; }
        
        // Setter
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        @Override
        public String toString() {
            return String.format("ModInterceptionInfo{modId='%s', targetClass='%s', " +
                "targetMethod='%s', priority=%d, enabled=%s}", 
                modId, targetClass, targetMethod, priority, enabled);
        }
    }
    
    /**
     * 打印当前配置
     */
    public static void printCurrentConfig() {
        CobblemonExtra.LOGGER.info("📋 当前Showdown拦截配置:");
        for (Map.Entry<String, ModInterceptionInfo> entry : KNOWN_MODS.entrySet()) {
            ModInterceptionInfo info = entry.getValue();
            CobblemonExtra.LOGGER.info("  {} - {} (优先级: {}, 状态: {})", 
                info.getModId(), 
                info.isEnabled() ? "✅ 启用" : "❌ 禁用",
                info.getPriority(),
                info.getTargetClass());
        }
    }
}

/*
 * 使用说明：
 * 
 * 1. 添加新mod拦截：
 *    ShowdownInterceptionConfig.addModConfig(
 *        "new_mod_id",
 *        "com.example.newmod.ShowdownManager",
 *        "injectShowdown",
 *        300,
 *        Arrays.asList("/newmod/", "/showdown/"),
 *        true
 *    );
 * 
 * 2. 启用/禁用mod拦截：
 *    ShowdownInterceptionConfig.setModEnabled("gravels_extended_battles", false);
 * 
 * 3. 检查是否应该拦截：
 *    if (ShowdownInterceptionConfig.shouldInterceptMod("some_mod")) {
 *        // 执行拦截逻辑
 *    }
 * 
 * 4. 获取mod优先级：
 *    int priority = ShowdownInterceptionConfig.getModPriority("some_mod");
 * 
 * 5. 打印当前配置：
 *    ShowdownInterceptionConfig.printCurrentConfig();
 */
