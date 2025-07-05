package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.CobblemonExtraConfig;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Showdown内容合并器
 * 负责将CobblemonExtra的内容与其他模组的内容智能合并
 * 实现和谐共存而不是简单覆盖
 */
public class ShowdownContentMerger {
    
    // 支持的文件类型及其主对象名称
    private static final Map<String, String> FILE_OBJECT_MAP = new HashMap<>();
    
    static {
        FILE_OBJECT_MAP.put("moves.js", "Moves");
        FILE_OBJECT_MAP.put("items.js", "Items");
        FILE_OBJECT_MAP.put("abilities.js", "Abilities");
        FILE_OBJECT_MAP.put("conditions.js", "Conditions");
        FILE_OBJECT_MAP.put("pokedex.js", "Pokedex");
        FILE_OBJECT_MAP.put("learnsets.js", "Learnsets");
        FILE_OBJECT_MAP.put("tags.js", "Tags");
        // scripts.js 特殊处理，因为它可能包含脚本逻辑
    }
    
    /**
     * 智能合并指定文件的内容
     * @param fileName 文件名
     * @param targetPath 目标路径
     * @return 是否成功合并
     */
    public static boolean mergeContent(String fileName, Path targetPath) {
        try {
            CobblemonExtra.LOGGER.info("开始智能合并文件: {}", fileName);
            
            // 对于 scripts.js，使用简单追加策略
            if ("scripts.js".equals(fileName)) {
                return handleScriptsFile(targetPath);
            }
            
            // 获取对象名称
            String objectName = FILE_OBJECT_MAP.get(fileName);
            if (objectName == null) {
                CobblemonExtra.LOGGER.warn("不支持的文件类型: {}", fileName);
                return false;
            }
            
            // 读取我们的内容
            String ourContent = readOurContent(fileName);
            if (ourContent == null) {
                CobblemonExtra.LOGGER.warn("无法读取我们的文件内容: {}", fileName);
                return false;
            }
            
            // 读取现有内容
            String existingContent = "";
            if (Files.exists(targetPath)) {
                existingContent = Files.readString(targetPath, StandardCharsets.UTF_8);
                CobblemonExtra.LOGGER.info("检测到现有文件，准备合并内容");
            } else {
                CobblemonExtra.LOGGER.info("目标文件不存在，将创建新文件");
            }
            
            // 执行智能合并
            String mergedContent = mergeJavaScriptObjects(ourContent, existingContent, objectName, fileName);
            
            if (mergedContent != null) {
                // 写入合并后的内容
                Files.writeString(targetPath, mergedContent, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                CobblemonExtra.LOGGER.info("成功合并文件: {}", fileName);
                return true;
            } else {
                CobblemonExtra.LOGGER.error("合并失败，回退到覆盖模式: {}", fileName);
                // 回退到覆盖模式
                Files.writeString(targetPath, ourContent, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                return true;
            }
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("合并文件 '{}' 时发生错误", fileName, e);
            return false;
        }
    }
    
    /**
     * 合并JavaScript对象内容
     */
    private static String mergeJavaScriptObjects(String ourContent, String existingContent, String objectName, String fileName) {
        try {
            // 提取我们的对象内容
            String ourObjectContent = extractObjectContent(ourContent, objectName);
            if (ourObjectContent == null) {
                CobblemonExtra.LOGGER.warn("无法从我们的文件中提取 {} 对象", objectName);
                return null;
            }
            
            String baseContent;
            String existingObjectContent = "";
            
            if (existingContent.isEmpty()) {
                // 如果没有现有内容，使用我们的内容作为基础
                baseContent = ourContent;
            } else {
                // 提取现有对象内容
                existingObjectContent = extractObjectContent(existingContent, objectName);
                baseContent = existingContent;
            }
            
            // 合并对象内容
            String mergedObjectContent = combineObjectContents(existingObjectContent, ourObjectContent, fileName);
            
            // 替换对象定义
            String result = replaceObjectContent(baseContent, objectName, mergedObjectContent);
            
            CobblemonExtra.LOGGER.info("成功合并 {} 对象，原有 {} 项，新增 {} 项", 
                objectName, 
                countObjectProperties(existingObjectContent),
                countObjectProperties(ourObjectContent));
            
            return result;
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("合并JavaScript对象时发生错误", e);
            return null;
        }
    }
    
    /**
     * 提取JavaScript对象的内容
     */
    private static String extractObjectContent(String content, String objectName) {
        // 查找对象定义的开始位置
        Pattern startPattern = Pattern.compile("const\\s+" + objectName + "\\s*=\\s*\\{");
        Matcher startMatcher = startPattern.matcher(content);
        
        if (!startMatcher.find()) {
            return null;
        }
        
        int startPos = startMatcher.end() - 1; // 定位到开始的 {
        int braceCount = 0;
        int contentStart = startPos + 1;
        int contentEnd = -1;
        
        // 从开始位置扫描，计算花括号平衡
        for (int i = startPos; i < content.length(); i++) {
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
            CobblemonExtra.LOGGER.warn("无法找到 {} 对象的结束位置", objectName);
            return null;
        }
        
        return content.substring(contentStart, contentEnd).trim();
    }
    
    /**
     * 合并两个对象的内容
     */
    private static String combineObjectContents(String existing, String our, String fileName) {
        // 对于items.js，处理配置替换
        if ("items.js".equals(fileName)) {
            double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
            our = our.replaceAll("ACTION_HERO_MASK_POWER_CONFIG", String.valueOf(powerMultiplier));
        }
        
        // 解析现有属性名
        Set<String> existingProperties = extractPropertyNames(existing);
        
        // 构建合并后的内容
        StringBuilder combined = new StringBuilder();
        
        // 添加现有内容
        if (!existing.isEmpty()) {
            combined.append(existing);
        }
        
        // 添加我们的新属性（只添加不重复的）
        if (!our.isEmpty()) {
            StringBuilder newContent = new StringBuilder();
            
            // 按行解析我们的内容，只添加新属性
            String[] ourLines = our.split("\n");
            boolean inProperty = false;
            String currentProperty = "";
            StringBuilder currentPropertyContent = new StringBuilder();
            
            for (String line : ourLines) {
                String trimmedLine = line.trim();
                
                // 检查是否是属性开始
                if (trimmedLine.matches("\\w+\\s*:\\s*\\{.*")) {
                    // 保存上一个属性
                    if (inProperty && !currentProperty.isEmpty()) {
                        if (!existingProperties.contains(currentProperty)) {
                            if (newContent.length() > 0) {
                                newContent.append(",\n");
                            }
                            newContent.append(currentPropertyContent.toString());
                        }
                    }
                    
                    // 开始新属性
                    currentProperty = trimmedLine.split("\\s*:")[0].trim();
                    currentPropertyContent = new StringBuilder(line);
                    inProperty = true;
                } else if (inProperty) {
                    // 继续当前属性
                    currentPropertyContent.append("\n").append(line);
                } else {
                    // 独立行（不属于任何属性）
                    currentPropertyContent.append(line);
                }
            }
            
            // 保存最后一个属性
            if (inProperty && !currentProperty.isEmpty()) {
                if (!existingProperties.contains(currentProperty)) {
                    if (newContent.length() > 0) {
                        newContent.append(",\n");
                    }
                    newContent.append(currentPropertyContent.toString());
                }
            }
            
            // 添加新内容
            if (newContent.length() > 0) {
                if (combined.length() > 0 && !combined.toString().trim().endsWith(",")) {
                    combined.append(",\n");
                }
                combined.append(newContent.toString());
            }
        }
        
        return combined.toString();
    }
    
    /**
     * 提取对象内容中的属性名
     */
    private static Set<String> extractPropertyNames(String objectContent) {
        Set<String> properties = new HashSet<>();
        if (objectContent == null || objectContent.trim().isEmpty()) {
            return properties;
        }
        
        // 匹配属性名（格式：属性名: {）
        Pattern pattern = Pattern.compile("(\\w+)\\s*:\\s*\\{");
        Matcher matcher = pattern.matcher(objectContent);
        
        while (matcher.find()) {
            properties.add(matcher.group(1));
        }
        
        return properties;
    }
    
    /**
     * 替换文件中的对象内容
     */
    private static String replaceObjectContent(String content, String objectName, String newContent) {
        // 查找对象定义的开始位置
        Pattern startPattern = Pattern.compile("(const\\s+" + objectName + "\\s*=\\s*)\\{");
        Matcher startMatcher = startPattern.matcher(content);
        
        if (!startMatcher.find()) {
            CobblemonExtra.LOGGER.warn("无法找到 {} 对象定义进行替换", objectName);
            return content;
        }
        
        int prefixEnd = startMatcher.end() - 1; // { 之前的位置
        String prefix = startMatcher.group(1);
        int braceStart = prefixEnd;
        int braceCount = 0;
        int objectEnd = -1;
        
        // 从开始位置扫描，找到对象结束位置
        for (int i = braceStart; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0) {
                    objectEnd = i + 1; // 包含结束的 }
                    break;
                }
            }
        }
        
        if (objectEnd == -1) {
            CobblemonExtra.LOGGER.warn("无法找到 {} 对象的结束位置进行替换", objectName);
            return content;
        }
        
        // 查找对象后的内容（通常是分号和其他代码）
        String suffix = "";
        if (objectEnd < content.length()) {
            // 简单地取剩余所有内容作为后缀
            suffix = content.substring(objectEnd);
        }
        
        // 构建新内容
        String formattedContent = newContent.isEmpty() ? "" : "\n  " + newContent.replaceAll("\n", "\n  ") + "\n";
        String replacement = prefix + "{" + formattedContent + "}" + suffix;
        
        // 替换原内容
        return content.substring(0, startMatcher.start()) + replacement;
    }
    
    /**
     * 统计对象属性数量
     */
    private static int countObjectProperties(String objectContent) {
        if (objectContent == null || objectContent.trim().isEmpty()) {
            return 0;
        }
        // 简单统计：计算逗号后跟随属性名的数量
        Pattern pattern = Pattern.compile("\\w+\\s*:");
        Matcher matcher = pattern.matcher(objectContent);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * 处理scripts.js文件（特殊处理）
     */
    private static boolean handleScriptsFile(Path targetPath) {
        try {
            String ourContent = readOurContent("scripts.js");
            if (ourContent == null) return false;
            
            if (Files.exists(targetPath)) {
                // 对于scripts.js，简单地使用我们的内容（因为通常是空的或者结构简单）
                Files.writeString(targetPath, ourContent, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                CobblemonExtra.LOGGER.info("Scripts文件使用覆盖策略（内容通常为空或简单）");
            } else {
                Files.writeString(targetPath, ourContent, StandardCharsets.UTF_8, 
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            CobblemonExtra.LOGGER.error("处理scripts.js文件时发生错误", e);
            return false;
        }
    }
    
    /**
     * 读取我们的资源文件内容
     */
    private static String readOurContent(String fileName) {
        String resourcePath = "/showdown/" + fileName;
        try (InputStream stream = ShowdownContentMerger.class.getResourceAsStream(resourcePath)) {
            if (stream == null) return null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CobblemonExtra.LOGGER.error("读取资源文件时出错: {}", resourcePath, e);
            return null;
        }
    }
} 