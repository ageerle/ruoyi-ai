package com.example.demo.utils;

import java.nio.file.Paths;

/**
 * 跨平台路径处理工具类
 */
public class PathUtils {
    
    /**
     * 构建跨平台兼容的绝对路径
     * @param basePath 基础路径
     * @param relativePath 相对路径部分
     * @return 跨平台兼容的绝对路径
     */
    public static String buildPath(String basePath, String... relativePath) {
        return Paths.get(basePath, relativePath).toString();
    }
    
    /**
     * 规范化路径，确保跨平台兼容
     * @param path 原始路径
     * @return 规范化后的路径
     */
    public static String normalizePath(String path) {
        return Paths.get(path).normalize().toString();
    }
    
    /**
     * 检查路径是否为绝对路径
     * @param path 要检查的路径
     * @return 是否为绝对路径
     */
    public static boolean isAbsolute(String path) {
        return Paths.get(path).isAbsolute();
    }
    
    /**
     * 获取当前工作目录
     * @return 当前工作目录的绝对路径
     */
    public static String getCurrentWorkingDirectory() {
        return System.getProperty("user.dir");
    }
    
    /**
     * 构建工作空间路径
     * @return 工作空间的绝对路径
     */
    public static String buildWorkspacePath() {
        return buildPath(getCurrentWorkingDirectory(), "workspace");
    }
}
