package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.CobblemonExtraConfig;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Showdown文件冲突解决器
 * 负责处理与Mega Showdown等其他模组的文件冲突问题
 * 采用延迟加载策略确保我们的文件不被覆盖
 */
@EventBusSubscriber(modid = CobblemonExtra.MOD_ID)
public class ShowdownConflictResolver {
    
    private static final int DELAY_SECONDS = 5; // 延迟时间，让其他模组先完成操作
    private static boolean hasExecuted = false;
    
    static {
        CobblemonExtra.LOGGER.info("ShowdownConflictResolver类已加载，事件监听器已注册");
    }
    
    /**
     * 监听所有模组加载完成事件
     * 使用最低优先级确保在其他模组之后执行
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLoadComplete(FMLLoadCompleteEvent event) {
        CobblemonExtra.LOGGER.info("========= 收到 FMLLoadCompleteEvent 事件 =========");
        if (hasExecuted) {
            CobblemonExtra.LOGGER.info("ShowdownConflictResolver已经执行过，跳过");
            return;
        }
        hasExecuted = true;
        
        CobblemonExtra.LOGGER.info("检测到所有模组加载完成，开始处理Showdown文件冲突...");
        
        CobblemonExtra.LOGGER.info("开始基于内容的智能追加策略...");
        
        // 延迟执行，让其他模组（如 GEB、Mega Showdown 等）先完成文件操作
        CompletableFuture.delayedExecutor(DELAY_SECONDS, TimeUnit.SECONDS).execute(() -> {
            try {
                CobblemonExtra.LOGGER.info("延迟{}秒后开始检查和追加 Showdown 内容...", DELAY_SECONDS);
                intelligentAppendContent();
            } catch (Exception e) {
                CobblemonExtra.LOGGER.error("智能追加过程中发生错误", e);
            }
        });
    }
    
    /**
     * 智能追加内容的方法
     * 基于文件内容检测，自动处理所有showdown文件
     */
    private static void intelligentAppendContent() {
        try {
            CobblemonExtra.LOGGER.info("开始智能追加Showdown内容...");
            
            Path showdownDir = Paths.get("showdown/data/mods/cobblemon");
            if (!Files.exists(showdownDir)) {
                CobblemonExtra.LOGGER.info("Showdown目录不存在，创建基础文件...");
                ShowdownDataManager.injectShowdown();
                return;
            }
            
            // 动态检查所有showdown文件
            for (String fileName : ShowdownDataManager.SHOWDOWN_FILES) {
                appendIfMissingGeneric(fileName);
            }
            
            CobblemonExtra.LOGGER.info("智能追加完成！");
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("智能追加过程中发生错误", e);
        }
    }
    
    /**
     * 通用的文件内容检查和追加方法
     * 自动检测我们的资源文件是否有实际内容，如果有则进行追加检查
     */
    private static void appendIfMissingGeneric(String fileName) {
        try {
            CobblemonExtra.LOGGER.info("正在检查文件: {}", fileName);
            
            // 获取我们的原始内容
            String ourContent = getOurContent(fileName);
            if (ourContent == null || ourContent.isEmpty()) {
                CobblemonExtra.LOGGER.debug("我们的{}文件为空或不存在，跳过", fileName);
                return;
            }
            
            // 检查我们的内容是否只是空对象
            String objectName = getObjectName(fileName);
            if (isEmptyObject(ourContent, objectName)) {
                CobblemonExtra.LOGGER.debug("我们的{}文件只包含空对象，跳过", fileName);
                return;
            }
            
            CobblemonExtra.LOGGER.info("检测到{}文件包含实际内容，开始检查是否需要追加", fileName);
            
            Path filePath = Paths.get("showdown/data/mods/cobblemon/" + fileName);
            
            if (!Files.exists(filePath)) {
                CobblemonExtra.LOGGER.info("目标文件{}不存在，跳过检查", fileName);
                return;
            }
            
            String existingContent = Files.readString(filePath);
            
            // 检查是否需要追加我们的内容
            if (needsOurContent(existingContent, ourContent, objectName)) {
                CobblemonExtra.LOGGER.info("文件{}中缺少我们的内容，开始追加...", fileName);
                
                String updatedContent = appendToExistingFile(existingContent, ourContent, fileName);
                Files.write(filePath, updatedContent.getBytes());
                CobblemonExtra.LOGGER.info("成功追加内容到{}", fileName);
            } else {
                CobblemonExtra.LOGGER.info("文件{}中已包含我们的内容，无需追加", fileName);
            }
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("处理文件{}时发生错误", fileName, e);
        }
    }
    
    /**
     * 检查我们的内容是否只是空对象
     */
    private static boolean isEmptyObject(String content, String objectName) {
        try {
            // 提取对象内容
            String objectContent = extractObjectContent(content, objectName);
            if (objectContent == null) {
                return true;
            }
            
            // 检查是否只包含空白字符
            return objectContent.trim().isEmpty();
        } catch (Exception e) {
            CobblemonExtra.LOGGER.debug("检查空对象时发生错误", e);
            return true;
        }
    }
    
    /**
     * 检查目标文件是否需要我们的内容
     */
    private static boolean needsOurContent(String existingContent, String ourContent, String objectName) {
        try {
            // 提取我们的对象内容中的主要标识符
            String ourObjectContent = extractObjectContent(ourContent, objectName);
            if (ourObjectContent == null || ourObjectContent.trim().isEmpty()) {
                return false;
            }
            
            // 提取我们内容中的第一个主要标识符（属性名）
            String firstKey = extractFirstKey(ourObjectContent);
            if (firstKey == null || firstKey.isEmpty()) {
                CobblemonExtra.LOGGER.debug("无法提取主要标识符，使用简单检查");
                return !existingContent.contains(ourObjectContent.substring(0, Math.min(50, ourObjectContent.length())));
            }
            
            CobblemonExtra.LOGGER.info("使用标识符'{}'检查文件内容", firstKey);
            return !existingContent.contains(firstKey);
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("检查内容需求时发生错误", e);
            return false;
        }
    }
    
    /**
     * 从对象内容中提取第一个属性名作为标识符
     */
    private static String extractFirstKey(String objectContent) {
        try {
            // 查找第一个属性名（格式：propertyName: {）
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z_][a-zA-Z0-9_]*)\\s*:");
            java.util.regex.Matcher matcher = pattern.matcher(objectContent);
            if (matcher.find()) {
                return matcher.group(1);
            }
            return null;
        } catch (Exception e) {
            CobblemonExtra.LOGGER.debug("提取第一个属性名时发生错误", e);
            return null;
        }
    }

    /**
     * 检查文件是否包含我们的内容，如果没有则追加（保留原方法以防需要）
     */
    private static void appendIfMissing(String fileName, String identifier, String contentType) {
        try {
            Path filePath = Paths.get("showdown/data/mods/cobblemon/" + fileName);
            
            if (!Files.exists(filePath)) {
                CobblemonExtra.LOGGER.info("文件{}不存在，跳过检查", fileName);
                return;
            }
            
            String content = Files.readString(filePath);
            
            if (identifier.isEmpty() || !content.contains(identifier)) {
                CobblemonExtra.LOGGER.info("文件{}中缺少我们的{}内容，开始追加...", fileName, contentType);
                
                // 获取我们的原始内容
                String ourContent = getOurContent(fileName);
                if (ourContent != null && !ourContent.isEmpty()) {
                    String updatedContent = appendToExistingFile(content, ourContent, fileName);
                    Files.write(filePath, updatedContent.getBytes());
                    CobblemonExtra.LOGGER.info("成功追加{}内容到{}", contentType, fileName);
                } else {
                    CobblemonExtra.LOGGER.warn("无法获取我们的{}内容，跳过追加", contentType);
                }
            } else {
                CobblemonExtra.LOGGER.info("文件{}中已包含我们的{}内容，无需追加", fileName, contentType);
            }
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("处理文件{}时发生错误", fileName, e);
        }
    }
    
    /**
     * 获取我们的原始内容（包含配置替换）
     */
    private static String getOurContent(String fileName) {
        try {
            java.io.InputStream inputStream = ShowdownConflictResolver.class.getResourceAsStream("/showdown/" + fileName);
            if (inputStream == null) {
                return null;
            }
            String content = new String(inputStream.readAllBytes());
            
            // 对items.js进行特殊处理，替换配置值
            if ("items.js".equals(fileName)) {
                try {
                    double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
                    content = content.replaceAll(
                        "ACTION_HERO_MASK_POWER_CONFIG", 
                        String.valueOf(powerMultiplier)
                    );
                    CobblemonExtra.LOGGER.info("已为'items.js'应用威力倍数配置: {}", powerMultiplier);
                } catch (Exception e) {
                    CobblemonExtra.LOGGER.warn("配置尚未初始化，使用默认值 1.5 for items.js");
                    content = content.replaceAll("ACTION_HERO_MASK_POWER_CONFIG", "1.5");
                }
            }
            
            return content;
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("读取我们的{}内容时发生错误", fileName, e);
            return null;
        }
    }
    
    /**
     * 将我们的内容追加到现有文件
     */
    private static String appendToExistingFile(String existingContent, String ourContent, String fileName) {
        try {
            // 简单的追加策略：检查现有文件是否是空对象，如果是则直接替换
            String objectName = getObjectName(fileName);
            
            // 检查是否是空对象 (如 "const Moves = {}")
            if (existingContent.matches(".*const\\s+" + objectName + "\\s*=\\s*\\{\\s*\\}.*")) {
                CobblemonExtra.LOGGER.info("检测到空对象，直接替换内容");
                return existingContent.replaceAll(
                    "const\\s+" + objectName + "\\s*=\\s*\\{\\s*\\}",
                    ourContent.replaceAll("const\\s+" + objectName + "\\s*=\\s*\\{([^}]*)\\}", 
                        "const " + objectName + " = {$1}")
                );
            }
            
            // 如果不是空对象，使用简单的对象合并
            CobblemonExtra.LOGGER.info("检测到非空对象，使用简单合并策略");
            return simpleObjectMerge(existingContent, ourContent, objectName);
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("追加内容到{}时发生错误", fileName, e);
            return existingContent;
        }
    }
    
    /**
     * 根据文件名获取对象名称
     */
    private static String getObjectName(String fileName) {
        switch (fileName) {
            case "moves.js": return "Moves";
            case "items.js": return "Items";
            case "abilities.js": return "Abilities";
            case "conditions.js": return "Conditions";
            case "pokedex.js": return "Pokedex";
            case "learnsets.js": return "Learnsets";
            case "tags.js": return "Tags";
            default: return "Unknown";
        }
    }
    
    /**
     * 智能对象合并方法 - 在对象末尾正确追加内容
     */
    private static String simpleObjectMerge(String existingContent, String ourContent, String objectName) {
        try {
            // 提取我们的对象内容
            String ourObjectContent = extractObjectContent(ourContent, objectName);
            if (ourObjectContent == null) {
                CobblemonExtra.LOGGER.warn("无法从我们的内容中提取对象内容");
                return existingContent;
            }
            
            // 找到对象的结束位置，在最后一个 } 之前插入我们的内容
            // 使用更智能的方式：从后往前找最后一个 }
            int lastBraceIndex = existingContent.lastIndexOf("}");
            if (lastBraceIndex == -1) {
                CobblemonExtra.LOGGER.warn("无法找到对象结束标记");
                return existingContent;
            }
            
            // 检查对象是否为空或最后一个项目是否以逗号结尾
            String beforeLastBrace = existingContent.substring(0, lastBraceIndex);
            
            // 更可靠的空对象检测：查找对象声明到结束花括号之间是否只有空白字符
            // 先找到对象声明的开始位置
            java.util.regex.Pattern objectStartPattern = java.util.regex.Pattern.compile(
                "const\\s+" + objectName + "\\s*=\\s*\\{", 
                java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.MULTILINE
            );
            java.util.regex.Matcher startMatcher = objectStartPattern.matcher(beforeLastBrace);
            
            boolean isEmpty = false;
            if (startMatcher.find()) {
                // 找到对象声明后，检查从对象开始到最后一个花括号之间是否只有空白字符
                int objectStart = startMatcher.end(); // 对象开始位置（在 { 之后）
                String objectContent = beforeLastBrace.substring(objectStart).trim();
                isEmpty = objectContent.isEmpty();
            }
            
            CobblemonExtra.LOGGER.info("检测对象是否为空: {} (对象: {})", isEmpty, objectName);
            
            if (isEmpty) {
                // 如果对象为空，直接插入内容，不添加逗号
                String result = existingContent.substring(0, lastBraceIndex) + "\n  " + ourObjectContent + "\n" + existingContent.substring(lastBraceIndex);
                CobblemonExtra.LOGGER.info("空对象检测：不添加前置逗号");
                CobblemonExtra.LOGGER.info("成功在空对象中追加内容");
                return result;
            } else {
                // 非空对象，找到倒数第二个 '}'，替换为 '},\n  '，再拼接追加内容
                int secondLastBrace = existingContent.lastIndexOf("}", lastBraceIndex - 1);
                if (secondLastBrace == -1) {
                    CobblemonExtra.LOGGER.warn("无法找到倒数第二个对象结束标记");
                    return existingContent;
                }
                String result = existingContent.substring(0, secondLastBrace) + "},\n  " + ourObjectContent + "\n" + existingContent.substring(lastBraceIndex);
                CobblemonExtra.LOGGER.info("非空对象，倒数第二个大括号替换为 '},\\n' 并追加内容");
                return result;
            }
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("智能对象合并时发生错误", e);
            return existingContent;
        }
    }
    
    /**
     * 提取对象内容（支持嵌套花括号）
     */
    private static String extractObjectContent(String content, String objectName) {
        try {
            // 找到对象声明的开始位置
            String objectDeclaration = "const\\s+" + objectName + "\\s*=\\s*\\{";
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(objectDeclaration);
            java.util.regex.Matcher matcher = pattern.matcher(content);
            if (!matcher.find()) {
                CobblemonExtra.LOGGER.warn("无法找到对象声明: {}", objectName);
                return null;
            }
            
            int startIndex = matcher.end() - 1; // 从开始的 { 位置算起
            int braceCount = 0;
            int contentStart = startIndex + 1; // 实际内容从 { 后开始
            int contentEnd = -1;
            
            // 使用括号平衡算法找到对象结束位置
            for (int i = startIndex; i < content.length(); i++) {
                char c = content.charAt(i);
                if (c == '{') {
                    braceCount++;
                } else if (c == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        contentEnd = i;
                        break;
                    }
                }
            }
            
            if (contentEnd == -1) {
                CobblemonExtra.LOGGER.warn("无法找到对象结束位置: {}", objectName);
                return null;
            }
            
            // 提取对象内容（不包括外层花括号）
            String objectContent = content.substring(contentStart, contentEnd).trim();
            CobblemonExtra.LOGGER.info("成功提取{}对象内容，长度: {}", objectName, objectContent.length());
            
            return objectContent;
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("提取对象内容时发生错误", e);
            return null;
        }
    }
    
    /**
     * 使用智能合并策略处理Showdown内容
     */
    private static void mergeShowdownContent() {
        try {
            String showdownFolder = ShowdownDataManager.getShowdownFolder();
            Files.createDirectories(Paths.get(showdownFolder));
            
            int successCount = 0;
            for (String fileName : ShowdownDataManager.SHOWDOWN_FILES) {
                Path targetPath = Paths.get(showdownFolder, fileName);
                if (ShowdownContentMerger.mergeContent(fileName, targetPath)) {
                    successCount++;
                } else {
                    CobblemonExtra.LOGGER.warn("合并文件失败: {}", fileName);
                }
            }
            
            CobblemonExtra.LOGGER.info("CobblemonExtra Showdown内容合并完成！成功处理 {} 个文件。", successCount);
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("合并Showdown内容时发生错误", e);
        }
    }
    
    /**
     * 验证内容完整性
     * 确保我们的内容已经被正确合并
     */
    private static void verifyContentIntegrity() {
        try {
            String showdownFolder = ShowdownDataManager.getShowdownFolder();
            
            // 验证moves.js中是否包含我们的技能
            Path movesPath = Paths.get(showdownFolder, "moves.js");
            if (Files.exists(movesPath)) {
                String content = Files.readString(movesPath);
                if (content.contains("actionbeam") && content.contains("shakingbutt")) {
                    CobblemonExtra.LOGGER.info("内容完整性验证通过：检测到CobblemonExtra的技能");
                } else {
                    CobblemonExtra.LOGGER.warn("内容完整性验证警告：未在moves.js中找到我们的技能");
                }
            }
            
            // 验证items.js中是否包含我们的道具
            Path itemsPath = Paths.get(showdownFolder, "items.js");
            if (Files.exists(itemsPath)) {
                String content = Files.readString(itemsPath);
                if (content.contains("actionheromask")) {
                    CobblemonExtra.LOGGER.info("内容完整性验证通过：检测到CobblemonExtra的道具");
                } else {
                    CobblemonExtra.LOGGER.warn("内容完整性验证警告：未在items.js中找到我们的道具");
                }
            }
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("验证内容完整性时发生错误", e);
        }
    }
    
    /**
     * 提供手动重新合并的方法
     * 可用于游戏运行时的内容恢复
     */
    public static void manualReinject() {
        CobblemonExtra.LOGGER.info("执行手动重新合并...");
        mergeShowdownContent();
        verifyContentIntegrity();
    }
} 