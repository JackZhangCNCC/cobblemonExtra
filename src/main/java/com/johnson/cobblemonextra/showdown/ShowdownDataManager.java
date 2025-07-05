package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.CobblemonExtraConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Showdown数据管理器
 * 负责统一管理所有Showdown相关的数据注入和文件操作
 * 采用智能注入模式，避免覆盖其他模组的文件
 */
public class ShowdownDataManager {
    
    public static final ArrayList<String> SHOWDOWN_FILES = new ArrayList<>(
        List.of("abilities.js", "conditions.js", "items.js", "moves.js", "pokedex.js", "scripts.js", "tags.js", "learnsets.js"));
    
    /**
     * 获取Showdown数据目录路径
     */
    public static String getShowdownFolder() {
        return FMLPaths.GAMEDIR.get().toString() + "/showdown/data/mods/cobblemon/";
    }

    /**
     * 主要方法，现在是直接将资源文件复制到目标位置。
     */
    public static void injectShowdown() {
        CobblemonExtra.LOGGER.info("CobblemonExtra开始直接写入Showdown文件...");
        String showdownFolder = getShowdownFolder();

        try {
            Files.createDirectories(Paths.get(showdownFolder));
        } catch (IOException e) {
            CobblemonExtra.LOGGER.error("FATAL: 无法创建Showdown目录: {}", showdownFolder, e);
            return;
        }

        int successCount = 0;
        
        for (String fileName : SHOWDOWN_FILES) {
            try {
                Path targetPath = Paths.get(showdownFolder, fileName);
                String resourcePath = "/showdown/" + fileName;

                String content = readResource(resourcePath);

                if (content == null || content.isEmpty()) {
                    CobblemonExtra.LOGGER.warn("资源文件 '{}' 为空或未找到，跳过。", resourcePath);
                    continue;
                }

                // 对items.js进行特殊处理，替换配置值
                if ("items.js".equals(fileName)) {
                    double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
                    content = content.replaceAll(
                        "ACTION_HERO_MASK_POWER_CONFIG", 
                        String.valueOf(powerMultiplier)
                    );
                    CobblemonExtra.LOGGER.info("已为'items.js'应用威力倍数配置: {}", powerMultiplier);
                }

                Files.writeString(targetPath, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                successCount++;
                CobblemonExtra.LOGGER.info("成功写入Showdown文件: {}", fileName);

            } catch (Exception e) {
                CobblemonExtra.LOGGER.error("写入Showdown文件 '{}' 时发生严重错误。", fileName, e);
            }

        }
        
        CobblemonExtra.LOGGER.info("CobblemonExtra Showdown文件写入完成！成功写入 {} 个文件。", successCount);
    }

    private static String readResource(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        try (InputStream stream = ShowdownDataManager.class.getResourceAsStream(path)) {
            if (stream == null) return null;
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            CobblemonExtra.LOGGER.error("读取资源文件时出错: {}", path, e);
            return null;
        }
    }
}