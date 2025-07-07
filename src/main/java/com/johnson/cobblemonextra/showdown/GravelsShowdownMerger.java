package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.CobblemonExtraConfig;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Gravels Extended Battles Showdown 合并器
 * 负责合并 gravels_extended_battles 和 CobblemonExtra 的 Showdown 内容
 */
public class GravelsShowdownMerger {
    
    // gravels_extended_battles 的文件列表
    public static final List<String> GRAVELS_FILES = List.of(
        "abilities.js", "conditions.js", "items.js", "moves.js", "pokedex.js", "scripts.js", "tags.js"
    );
    
    // 我们的文件列表
    public static final List<String> OUR_FILES = List.of(
        "abilities.js", "conditions.js", "items.js", "moves.js", "pokedex.js", "scripts.js", "tags.js", "learnsets.js"
    );
    

    
    /**
     * 合并 JavaScript 内容
     * 使用简单而可靠的字符串替换方法
     */
    public static String mergeJavaScriptContent(String ourContent, String gravelsContent, String fileName) {
        try {
            // 自动检测对象名称
            String objectName = detectObjectName(ourContent);
            if (objectName == null) {
                // 如果自动检测失败，尝试使用硬编码的备用方案
                objectName = getObjectName(fileName);
            }

            if (objectName == null) {
                CobblemonExtra.LOGGER.warn("无法确定对象名，使用我们的内容覆盖: {}", fileName);
                return ourContent;
            }

            // 使用简单而可靠的合并方法
            return simpleMergeJavaScriptFiles(ourContent, gravelsContent, objectName, fileName);

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("合并 JavaScript 内容时发生错误: {}", fileName, e);
            return ourContent; // 失败时返回我们的原始内容
        }
    }
    


    /**
     * 简单直接的 JavaScript 文件合并
     * 策略：在gravels文件的对象结束前插入我们的招式内容
     */
    private static String simpleMergeJavaScriptFiles(String ourContent, String gravelsContent, String objectName, String fileName) {
        try {
            // 提取我们的纯对象定义（不包含const ObjectName = {和};）
            String ourObjectDefinitions = extractPureObjectDefinitions(ourContent, objectName);
            if (ourObjectDefinitions == null || ourObjectDefinitions.trim().isEmpty()) {
                CobblemonExtra.LOGGER.warn("无法提取我们的{}定义: {}", objectName, fileName);
                return gravelsContent;
            }

            // 在gravels文件中找到 }; 的位置（对象结束）
            int insertPos = gravelsContent.lastIndexOf("};");
            if (insertPos == -1) {
                CobblemonExtra.LOGGER.warn("无法找到对象结束位置: {}", fileName);
                return gravelsContent;
            }

            // 构建合并后的内容
            StringBuilder result = new StringBuilder();

            // 添加gravels内容到 }; 之前
            String beforeInsert = gravelsContent.substring(0, insertPos);

            // 简单直接：检查对象是否为空（避免耗时的大括号计数）
            boolean isEmpty = isObjectReallyEmpty(gravelsContent, objectName);

            if (isEmpty) {
                // 空对象（只有一层大括号），直接在 { 后添加内容
                result.append(beforeInsert);
                result.append("\n  ").append(ourObjectDefinitions);
                result.append("\n").append(gravelsContent.substring(insertPos));
                CobblemonExtra.LOGGER.info("检测到空{}对象，直接添加内容", objectName);
            } else {
                // 有内容的对象，在最后一个 } 前添加逗号和内容
                String trimmedBefore = beforeInsert.trim();
                if (trimmedBefore.endsWith("}")) {
                    // 最后一个对象没有逗号，需要添加逗号
                    result.append(beforeInsert.substring(0, beforeInsert.lastIndexOf("}")));
                    result.append("},\n");
                } else {
                    // 已经有逗号或其他情况
                    result.append(beforeInsert).append(",\n");
                }
                result.append(ourObjectDefinitions);
                result.append("\n").append(gravelsContent.substring(insertPos));
                CobblemonExtra.LOGGER.info("检测到有内容的{}对象，添加逗号", objectName);
            }

            CobblemonExtra.LOGGER.info("✅ 成功将我们的{}插入到gravels的{}对象中: {}", objectName, objectName, fileName);
            return result.toString();

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("合并失败: {}", fileName, e);
            return gravelsContent;
        }
    }

    /**
     * 提取纯对象定义（不包含const ObjectName = {和};）
     */
    private static String extractPureObjectDefinitions(String content, String objectName) {
        try {
            // 找到 const ObjectName = { 的位置
            String startPattern = "const " + objectName + " = {";
            int startPos = content.indexOf(startPattern);
            if (startPos == -1) {
                CobblemonExtra.LOGGER.warn("未找到对象定义: {}", objectName);
                return null;
            }

            // 跳过开头的 {
            startPos += startPattern.length();

            // 找到最后的 }; 位置
            int endPos = content.lastIndexOf("};");
            if (endPos == -1 || endPos <= startPos) {
                CobblemonExtra.LOGGER.warn("未找到对象结束: {}", objectName);
                return null;
            }

            // 提取中间的内容
            String objectDefinitions = content.substring(startPos, endPos).trim();

            // 清理开头和结尾的换行符
            if (objectDefinitions.startsWith("\n")) {
                objectDefinitions = objectDefinitions.substring(1);
            }
            if (objectDefinitions.endsWith("\n")) {
                objectDefinitions = objectDefinitions.substring(0, objectDefinitions.length() - 1);
            }

            CobblemonExtra.LOGGER.debug("成功提取{}对象定义，长度: {}", objectName, objectDefinitions.length());
            return objectDefinitions;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("提取{}对象定义失败", objectName, e);
            return null;
        }
    }







    /**
     * 快速检查对象是否真的为空（避免耗时的字符遍历）
     */
    private static boolean isObjectReallyEmpty(String content, String objectName) {
        try {
            // 直接查找空对象模式
            String emptyObjectPattern = "const " + objectName + " = {};";
            boolean hasEmptyPattern = content.contains(emptyObjectPattern);

            if (hasEmptyPattern) {
                CobblemonExtra.LOGGER.info("检测到{}对象为空（找到空对象模式）", objectName);
                return true;
            }

            // 如果没有找到空对象模式，再检查是否包含冒号
            String objectPattern = "const " + objectName + " = {";
            int objectStart = content.indexOf(objectPattern);
            if (objectStart == -1) {
                return false;
            }

            int contentStart = objectStart + objectPattern.length();
            int contentEnd = content.lastIndexOf("};");
            if (contentEnd == -1 || contentEnd <= contentStart) {
                return false;
            }

            String objectContent = content.substring(contentStart, contentEnd).trim();
            boolean isEmpty = !objectContent.contains(":");

            CobblemonExtra.LOGGER.info("快速检查{}对象是否为空: {} (内容长度: {}, 包含冒号: {})",
                objectName, isEmpty, objectContent.length(), objectContent.contains(":"));

            return isEmpty;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("快速检查{}对象时发生错误", objectName, e);
            return false;
        }
    }



    /**
     * 自动检测JavaScript文件中的对象名称
     * 通过解析 "const ObjectName = {" 模式来自动识别
     */
    private static String detectObjectName(String content) {
        try {
            // 查找 const ObjectName = { 模式
            Pattern pattern = Pattern.compile("const\\s+(\\w+)\\s*=\\s*\\{");
            Matcher matcher = pattern.matcher(content);

            if (matcher.find()) {
                String objectName = matcher.group(1);
                CobblemonExtra.LOGGER.debug("自动检测到对象名称: {}", objectName);
                return objectName;
            }

            CobblemonExtra.LOGGER.warn("无法自动检测对象名称，内容预览: {}",
                content.length() > 100 ? content.substring(0, 100) + "..." : content);
            return null;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("检测对象名称时发生错误", e);
            return null;
        }
    }

    /**
     * 获取对象名称（保留作为备用，但优先使用自动检测）
     */
    private static String getObjectName(String fileName) {
        return switch (fileName) {
            case "moves.js" -> "Moves";
            case "items.js" -> "Items";
            case "abilities.js" -> "Abilities";
            case "conditions.js" -> "Conditions";
            case "pokedex.js" -> "Pokedex";
            case "learnsets.js" -> "Learnsets";
            case "tags.js" -> "Tags";
            case "scripts.js" -> "Scripts";
            default -> null;
        };
    }
    

    
    /**
     * 读取我们的资源文件
     */
    private static String readOurResource(String fileName) {
        try (InputStream stream = GravelsShowdownMerger.class.getResourceAsStream("/showdown/" + fileName)) {
            if (stream == null) return null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("读取我们的资源文件时出错: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * 读取 gravels_extended_battles 的资源文件
     */
    private static String readGravelsResource(String fileName) {
        try {
            // 尝试通过不同的类加载器和路径读取
            ClassLoader[] classLoaders = {
                Thread.currentThread().getContextClassLoader(),
                GravelsShowdownMerger.class.getClassLoader(),
                ClassLoader.getSystemClassLoader()
            };

            // 可能的路径
            String[] possiblePaths = {
                "/drai/dev/gravelsextendedbattles/" + fileName,
                "/" + fileName,
                "drai/dev/gravelsextendedbattles/" + fileName,
                fileName
            };

            for (ClassLoader classLoader : classLoaders) {
                for (String path : possiblePaths) {
                    try (InputStream stream = classLoader.getResourceAsStream(path)) {
                        if (stream != null) {
                            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                            CobblemonExtra.LOGGER.info("✅ 成功读取 gravels_extended_battles 的 {} (路径: {}, 类加载器: {}), 大小: {} 字节",
                                fileName, path, classLoader.getClass().getSimpleName(), content.length());
                            return content;
                        }
                    } catch (Exception e) {
                        // 静默忽略，继续尝试下一个
                    }
                }
            }

            // 尝试通过 gravels_extended_battles 的类直接读取
            try {
                Class<?> gravelsClass = Class.forName("drai.dev.gravelsextendedbattles.GravelsExtendedBattles");
                for (String path : possiblePaths) {
                    try (InputStream stream = gravelsClass.getResourceAsStream(path)) {
                        if (stream != null) {
                            String content = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                            CobblemonExtra.LOGGER.info("✅ 通过 GravelsExtendedBattles 类成功读取 {} (路径: {}), 大小: {} 字节",
                                fileName, path, content.length());
                            return content;
                        }
                    } catch (Exception e) {
                        // 静默忽略
                    }
                }
            } catch (ClassNotFoundException e) {
                CobblemonExtra.LOGGER.warn("❌ 找不到 GravelsExtendedBattles 类: {}", e.getMessage());
            }

            CobblemonExtra.LOGGER.warn("❌ 无法在任何路径找到 gravels_extended_battles 的资源文件: {}", fileName);
            return null;

        } catch (Exception e) {
            CobblemonExtra.LOGGER.warn("❌ 读取 gravels_extended_battles 资源文件时发生错误: {}", fileName, e);
            return null;
        }
    }
    
    /**
     * 应用 items.js 配置
     */
    private static String applyItemsConfig(String content) {
        try {
            double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
            return content.replaceAll("ACTION_HERO_MASK_POWER_CONFIG", String.valueOf(powerMultiplier));
        } catch (Exception e) {
            CobblemonExtra.LOGGER.warn("配置尚未初始化，使用默认值 1.5 for items.js");
            return content.replaceAll("ACTION_HERO_MASK_POWER_CONFIG", "1.5");
        }
    }
    
    /**
     * 验证 moves.js 文件
     */
    private static void validateMovesFile(Path movesPath) {
        try {
            String content = Files.readString(movesPath);
            boolean hasActionbeam = content.contains("actionbeam");
            boolean hasShakingbutt = content.contains("shakingbutt");

            CobblemonExtra.LOGGER.info("moves.js 验证结果:");
            CobblemonExtra.LOGGER.info("  - actionbeam: {}", hasActionbeam ? "✅ 存在" : "❌ 缺失");
            CobblemonExtra.LOGGER.info("  - shakingbutt: {}", hasShakingbutt ? "✅ 存在" : "❌ 缺失");
            CobblemonExtra.LOGGER.info("  - 文件大小: {} 字节", content.length());

            if (!hasActionbeam || !hasShakingbutt) {
                CobblemonExtra.LOGGER.error("❌ 关键招式缺失！这可能导致 learnset 错误！");
            }

        } catch (Exception e) {
            CobblemonExtra.LOGGER.error("验证 moves.js 时发生错误", e);
        }
    }

    /**
     * 获取 Showdown 文件夹路径
     */
    private static String getShowdownFolder() {
        return "./showdown/data/mods/cobblemon/";
    }


}
