package com.johnson.cobblemonextra.showdown;

import com.johnson.cobblemonextra.CobblemonExtra;
import com.johnson.cobblemonextra.config.CobblemonExtraConfig;
import net.neoforged.fml.loading.FMLPaths;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;

/**
 * Showdown数据管理器
 * 完全参考gravels_extended_battles模组的ShowdownFileManager架构
 * 负责统一管理所有Showdown相关的数据注入和文件操作
 */
public class ShowdownDataManager {
    
    public static final ArrayList<String> SHOWDOWN_FILES = new ArrayList<>(
        List.of("abilities.js", "conditions.js", "items.js", "moves.js", "pokedex.js", "scripts.js", "tags.js"));
    
    /**
     * 获取Showdown数据目录路径
     * 完全参考gravels_extended_battles的ShowdownFolderLocatorImpl实现
     */
    public static String getShowdownFolder() {
        return FMLPaths.GAMEDIR.get().toString() + "/showdown/data/mods/cobblemon/";
    }
    
    /**
     * 计算输入流的MD5哈希值
     */
    private static String calculateMD5(InputStream inputStream) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[4096];
        int readBytes;
        
        while ((readBytes = inputStream.read(buffer)) > 0) {
            md.update(buffer, 0, readBytes);
        }
        
        byte[] digest = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
    
    /**
     * 计算文件的MD5哈希值
     */
    private static String calculateFileMD5(File file) throws Exception {
        if (!file.exists()) {
            return null;
        }
        
        try (FileInputStream fis = new FileInputStream(file)) {
            return calculateMD5(fis);
        }
    }
    
    /**
     * 计算资源文件的MD5哈希值
     */
    private static String calculateResourceMD5(String resourceName) throws Exception {
        try (InputStream stream = CobblemonExtra.class.getResourceAsStream("/" + resourceName)) {
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            return calculateMD5(stream);
        }
    }
    
    /**
     * 检查是否需要更新文件（通过MD5比较）
     */
    private static boolean needsUpdate(String targetFilePath, String resourceName) {
        try {
            File targetFile = new File(targetFilePath);
            
            // 如果目标文件不存在，需要更新
            if (!targetFile.exists()) {
                CobblemonExtra.LOGGER.debug("文件不存在，需要创建: " + resourceName);
                return true;
            }
            
            // 计算现有文件和资源文件的MD5
            String existingMD5 = calculateFileMD5(targetFile);
            String resourceMD5 = calculateResourceMD5(resourceName);
            
            // 比较MD5
            boolean different = !resourceMD5.equals(existingMD5);
            if (different) {
                CobblemonExtra.LOGGER.info("文件内容不同，需要更新: " + resourceName);
                CobblemonExtra.LOGGER.debug("现有文件MD5: " + existingMD5);
                CobblemonExtra.LOGGER.debug("资源文件MD5: " + resourceMD5);
            } else {
                CobblemonExtra.LOGGER.debug("文件内容相同，跳过更新: " + resourceName);
            }
            
            return different;
            
        } catch (Exception e) {
            CobblemonExtra.LOGGER.warn("MD5比较失败，强制更新: " + resourceName, e);
            return true; // 出错时选择安全的做法：强制更新
        }
    }
    
    /**
     * 导出资源文件到指定位置（智能更新版本）
     * 只有当文件内容不同时才进行替换
     */
    public static boolean exportResourceSmart(String showdownFolder, String resourceName) throws Exception {
        String targetFilePath = showdownFolder + resourceName;
        
        // 特殊处理items.js，需要动态替换威力倍数
        if ("items.js".equals(resourceName)) {
            return exportItemsJsWithConfig(showdownFolder, resourceName);
        }
        
        // 检查是否需要更新
        if (!needsUpdate(targetFilePath, resourceName)) {
            return false; // 不需要更新
        }
        
        // 创建目录
        Files.createDirectories(Paths.get(showdownFolder));
        
        // 导出文件
        try (InputStream stream = CobblemonExtra.class.getResourceAsStream("/" + resourceName);
             OutputStream resStreamOut = new FileOutputStream(targetFilePath)) {
            
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        }
        
        return true; // 已更新
    }
    
    /**
     * 特殊处理items.js文件，根据配置动态替换威力倍数
     */
    private static boolean exportItemsJsWithConfig(String showdownFolder, String resourceName) throws Exception {
        String targetFilePath = showdownFolder + resourceName;
        
        // 读取模板内容
        String content;
        try (InputStream stream = CobblemonExtra.class.getResourceAsStream("/" + resourceName)) {
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            content = new String(stream.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        }
        
        // 获取配置的威力倍数
        double powerMultiplier = CobblemonExtraConfig.getActionHeroMaskPowerMultiplier();
        
        // 🔥 精确替换：只替换动感超人面具的特定配置标记
        String updatedContent = content.replaceAll(
            "ACTION_HERO_MASK_POWER_CONFIG", 
            String.valueOf(powerMultiplier)
        );
        
        // 检查是否需要更新（比较处理后的内容）
        File targetFile = new File(targetFilePath);
        if (targetFile.exists()) {
            String existingContent = Files.readString(Paths.get(targetFilePath), java.nio.charset.StandardCharsets.UTF_8);
            if (existingContent.equals(updatedContent)) {
                CobblemonExtra.LOGGER.debug("items.js内容相同，跳过更新 (威力倍数: {})", powerMultiplier);
                return false; // 不需要更新
            }
        }
        
        // 创建目录
        Files.createDirectories(Paths.get(showdownFolder));
        
        // 写入更新后的内容
        Files.writeString(Paths.get(targetFilePath), updatedContent, java.nio.charset.StandardCharsets.UTF_8);
        
        CobblemonExtra.LOGGER.info("已更新items.js，威力倍数设置为: {}", powerMultiplier);
        return true; // 已更新
    }
    
    /**
     * 导出资源文件到指定位置（兼容旧版本，总是覆盖）
     */
    public static void exportResource(String showdownFolder, String resourceName) throws Exception {
        String targetFilePath = showdownFolder + resourceName;
        Files.createDirectories(Paths.get(showdownFolder));
        
        try (InputStream stream = CobblemonExtra.class.getResourceAsStream("/" + resourceName);
             OutputStream resStreamOut = new FileOutputStream(targetFilePath)) {
            
            if (stream == null) {
                throw new Exception("Cannot get resource \"" + resourceName + "\" from Jar file.");
            }
            
            byte[] buffer = new byte[4096];
            int readBytes;
            while ((readBytes = stream.read(buffer)) > 0) {
                resStreamOut.write(buffer, 0, readBytes);
            }
        }
    }
    
    /**
     * 主要注入方法，使用智能更新逻辑
     */
    public static void injectShowdown() {
        CobblemonExtra.LOGGER.info("CobblemonExtra开始检查Showdown数据...");
        
        String showdownFolder = getShowdownFolder();
        int updatedCount = 0;
        int skippedCount = 0;
        
        // 智能导出所有基础Showdown文件
        for (String fileName : SHOWDOWN_FILES) {
            try {
                boolean updated = exportResourceSmart(showdownFolder, fileName);
                if (updated) {
                    CobblemonExtra.LOGGER.info("成功导出: " + fileName);
                    updatedCount++;
                } else {
                    CobblemonExtra.LOGGER.debug("跳过未更改的文件: " + fileName);
                    skippedCount++;
                }
            } catch (Exception e) {
                CobblemonExtra.LOGGER.error("导出文件失败: " + fileName, e);
                throw new RuntimeException(e);
            }
        }
        
        CobblemonExtra.LOGGER.info("CobblemonExtra Showdown数据检查完成！更新了 " + updatedCount + " 个文件，跳过了 " + skippedCount + " 个未更改的文件。");
    }
    
    /**
     * 强制重新导出所有文件（忽略MD5比较）
     */
    public static void forceInjectShowdown() {
        CobblemonExtra.LOGGER.info("CobblemonExtra强制重新导出所有Showdown数据...");
        
        String showdownFolder = getShowdownFolder();
        
        // 强制导出所有基础Showdown文件
        for (String fileName : SHOWDOWN_FILES) {
            try {
                exportResource(showdownFolder, fileName);
                CobblemonExtra.LOGGER.info("强制导出: " + fileName);
            } catch (Exception e) {
                CobblemonExtra.LOGGER.error("导出文件失败: " + fileName, e);
                throw new RuntimeException(e);
            }
        }
        
        CobblemonExtra.LOGGER.info("CobblemonExtra Showdown数据强制导出完成！");
    }
    
    /**
     * 初始化所有Showdown数据
     * 保留旧方法以便兼容，但现在由injectShowdown()替代
     */
    @Deprecated
    public static void initializeShowdownData() {
        injectShowdown();
    }
}