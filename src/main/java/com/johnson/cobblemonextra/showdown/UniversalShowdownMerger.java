package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.ShowdownInterceptionConfig;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 通用Showdown合并器
 * 可以拦截和合并任何mod的Showdown注入操作
 */
public class UniversalShowdownMerger {
    
    // 记录已处理的mod，避免重复处理
    private static final Set<String> processedMods = ConcurrentHashMap.newKeySet();
    
    // 存储各个mod的Showdown内容
    private static final Map<String, Map<String, String>> modShowdownContent = new ConcurrentHashMap<>();
    
    // 我们的Showdown文件列表
    private static final List<String> SHOWDOWN_FILES = Arrays.asList(
        "abilities.js", "conditions.js", "items.js", "moves.js", 
        "pokedex.js", "scripts.js", "tags.js", "learnsets.js"
    );
    
    /**
     * 处理任何mod的Showdown注入
     */
    public static void handleShowdownInjection(String modId) {
        try {
            CobblemonExtra.LOGGER.info("🔄 开始处理 {} 的 Showdown 注入", modId);

            // 检查是否应该拦截这个mod
            if (!ShowdownInterceptionConfig.shouldInterceptMod(modId)) {
                CobblemonExtra.LOGGER.info("⚠️ {} 未启用拦截，跳过处理", modId);
                return;
            }

            // 检查是否已经处理过这个mod
            if (processedMods.contains(modId)) {
                CobblemonExtra.LOGGER.info("⚠️ {} 已经处理过，跳过", modId);
                return;
            }

            // 收集这个mod的Showdown内容
            collectModShowdownContent(modId);

            // 执行通用合并
            performUniversalMerge();

            // 标记为已处理
            processedMods.add(modId);

            CobblemonExtra.LOGGER.info("✅ 成功处理 {} 的 Showdown 注入", modId);

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 处理 {} 的 Showdown 注入时发生错误", modId, e);
            throw e;
        }
    }
    
    /**
     * 收集指定mod的Showdown内容
     */
    private static void collectModShowdownContent(String modId) {
        Map<String, String> modContent = new HashMap<>();
        
        for (String fileName : SHOWDOWN_FILES) {
            try {
                String content = readModShowdownFile(modId, fileName);
                if (content != null && !content.trim().isEmpty()) {
                    modContent.put(fileName, content);
                    CobblemonExtra.LOGGER.debug("📄 收集到 {} 的 {} (大小: {} 字节)", 
                        modId, fileName, content.length());
                }
            } catch (Exception e) {
                CobblemonExtra.LOGGER.debug("⚠️ 无法读取 {} 的 {}: {}", modId, fileName, e.getMessage());
            }
        }
        
        if (!modContent.isEmpty()) {
            modShowdownContent.put(modId, modContent);
            CobblemonExtra.LOGGER.info("📦 成功收集 {} 的 {} 个 Showdown 文件", modId, modContent.size());
        }
    }
    
    /**
     * 读取指定mod的Showdown文件
     */
    private static String readModShowdownFile(String modId, String fileName) {
        try {
            // 从配置中获取mod的资源路径
            List<String> configuredPaths = ShowdownInterceptionConfig.getModResourcePaths(modId);

            // 构建完整的可能路径列表
            List<String> possiblePaths = new ArrayList<>();

            // 添加配置的路径
            for (String basePath : configuredPaths) {
                possiblePaths.add(basePath + fileName);
            }

            // 添加默认的备用路径
            possiblePaths.addAll(Arrays.asList(
                "/" + modId.replace("_", "") + "/" + fileName,
                "/" + modId + "/" + fileName,
                "/drai/dev/" + modId + "/" + fileName,
                "/showdown/" + fileName,
                "/data/" + fileName
            ));

            for (String path : possiblePaths) {
                try (InputStream inputStream = UniversalShowdownMerger.class.getResourceAsStream(path)) {
                    if (inputStream != null) {
                        String content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
                        CobblemonExtra.LOGGER.debug("✅ 成功从路径 {} 读取 {} 的 {}", path, modId, fileName);
                        return content;
                    }
                }
            }

            CobblemonExtra.LOGGER.debug("⚠️ 未找到 {} 的 {} 文件", modId, fileName);
            return null;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.debug("❌ 读取 {} 的 {} 时发生错误: {}", modId, fileName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 执行通用合并
     */
    private static void performUniversalMerge() {
        try {
            CobblemonExtra.LOGGER.info("🔄 开始执行通用 Showdown 合并...");
            
            for (String fileName : SHOWDOWN_FILES) {
                mergeFileFromAllMods(fileName);
            }
            
            CobblemonExtra.LOGGER.info("✅ 通用 Showdown 合并完成！");
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 通用合并失败", e);
            throw e;
        }
    }
    
    /**
     * 合并所有mod的指定文件
     */
    private static void mergeFileFromAllMods(String fileName) {
        try {
            // 收集所有mod的这个文件内容
            List<ModFileContent> allContents = new ArrayList<>();
            
            // 添加我们的内容（最高优先级）
            String ourContent = readOurShowdownFile(fileName);
            if (ourContent != null) {
                allContents.add(new ModFileContent("cobblemonextra", ourContent, 1000));
            }
            
            // 添加其他mod的内容
            for (Map.Entry<String, Map<String, String>> modEntry : modShowdownContent.entrySet()) {
                String modId = modEntry.getKey();
                Map<String, String> modFiles = modEntry.getValue();
                
                if (modFiles.containsKey(fileName)) {
                    String content = modFiles.get(fileName);
                    int priority = getModPriority(modId);
                    allContents.add(new ModFileContent(modId, content, priority));
                }
            }
            
            if (allContents.isEmpty()) {
                CobblemonExtra.LOGGER.debug("⚠️ 没有找到任何 {} 内容，跳过", fileName);
                return;
            }
            
            // 按优先级排序（优先级高的在后面，这样会覆盖前面的）
            allContents.sort(Comparator.comparingInt(ModFileContent::getPriority));
            
            // 执行智能合并
            String mergedContent = smartMergeContents(allContents, fileName);
            
            // 写入最终文件
            writeShowdownFile(fileName, mergedContent);
            
            CobblemonExtra.LOGGER.info("✅ 成功合并 {} ({} 个mod的内容)", fileName, allContents.size());
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 合并 {} 失败", fileName, e);
        }
    }
    
    /**
     * 智能合并多个mod的内容
     */
    @SuppressWarnings("unused")
    private static String smartMergeContents(List<ModFileContent> contents, String fileName) {
        if (contents.size() == 1) {
            return contents.getFirst().getContent();
        }
        
        // 检测对象名称
        String objectName = detectObjectName(contents.getFirst().getContent());
        if (objectName == null) {
            // 如果无法检测对象名称，使用简单拼接
            CobblemonExtra.LOGGER.warn("⚠️ 无法检测 {} 的对象名称，使用简单合并", fileName);
            return simpleContentMerge(contents);
        }
        
        // 使用智能对象合并
        return smartObjectMerge(contents, objectName, fileName);
    }
    
    /**
     * 简单内容合并（当无法检测对象时使用）
     */
    @SuppressWarnings("unused")
    private static String simpleContentMerge(List<ModFileContent> contents) {
        // 使用最高优先级的内容
        ModFileContent highest = contents.getLast();
        CobblemonExtra.LOGGER.info("🔄 使用简单合并，采用 {} 的内容", highest.getModId());
        return highest.getContent();
    }
    
    /**
     * 智能对象合并
     */
    @SuppressWarnings("unused")
    private static String smartObjectMerge(List<ModFileContent> contents, String objectName, String fileName) {
        // 使用现有的合并逻辑
        String baseContent = contents.getFirst().getContent();
        
        for (int i = 1; i < contents.size(); i++) {
            ModFileContent current = contents.get(i);
            baseContent = GravelsShowdownMerger.mergeJavaScriptContent(
                current.getContent(), baseContent, fileName);
            
            CobblemonExtra.LOGGER.debug("🔄 合并 {} 到基础内容中", current.getModId());
        }
        
        return baseContent;
    }
    
    /**
     * 自动检测JavaScript文件中的对象名称
     */
    private static String detectObjectName(String content) {
        try {
            Pattern pattern = Pattern.compile("const\\s+(\\w+)\\s*=\\s*\\{");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String objectName = matcher.group(1);
                CobblemonExtra.LOGGER.debug("🔍 自动检测到对象名称: {}", objectName);
                return objectName;
            }

            return null;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.debug("❌ 检测对象名称时发生错误", e);
            return null;
        }
    }

    /**
     * 读取我们的Showdown文件
     */
    private static String readOurShowdownFile(String fileName) {
        try (InputStream inputStream = UniversalShowdownMerger.class.getResourceAsStream("/showdown/" + fileName)) {
            if (inputStream != null) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            return null;
        } catch (Exception e) {
            CobblemonExtra.LOGGER.debug("⚠️ 读取我们的 {} 失败: {}", fileName, e.getMessage());
            return null;
        }
    }

    /**
     * 获取mod的优先级
     */
    private static int getModPriority(String modId) {
        if ("cobblemonextra".equals(modId)) {
            return 1000; // 我们的优先级最高
        }

        // 从配置中获取优先级
        return ShowdownInterceptionConfig.getModPriority(modId);
    }

    /**
     * 写入Showdown文件
     */
    private static void writeShowdownFile(String fileName, String content) {
        try {
            Path outputPath = Paths.get("run/showdown/data/mods/cobblemon/" + fileName);
            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            CobblemonExtra.LOGGER.debug("✅ 成功写入: {} (大小: {} 字节)", fileName, content.length());

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("❌ 写入 {} 失败", fileName, e);
            throw new RuntimeException("写入文件失败: " + fileName, e);
        }
    }

    /**
     * 重置处理状态（用于测试或重新加载）
     */
    @SuppressWarnings("unused")
    public static void reset() {
        processedMods.clear();
        modShowdownContent.clear();
        CobblemonExtra.LOGGER.info("🔄 已重置通用Showdown合并器状态");
    }

    /**
     * 获取已处理的mod列表
     */
    public static Set<String> getProcessedMods() {
        return new HashSet<>(processedMods);
    }

    /**
     * Mod文件内容包装类
     */
    private static class ModFileContent {
        private final String modId;
        private final String content;
        private final int priority;

        public ModFileContent(String modId, String content, int priority) {
            this.modId = modId;
            this.content = content;
            this.priority = priority;
        }

        public String getModId() { return modId; }
        public String getContent() { return content; }
        public int getPriority() { return priority; }
    }
}
