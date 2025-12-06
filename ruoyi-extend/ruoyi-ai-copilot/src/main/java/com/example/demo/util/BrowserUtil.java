package com.example.demo.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * 浏览器工具类
 * 用于跨平台打开默认浏览器
 */
public class BrowserUtil {

    private static final Logger logger = LoggerFactory.getLogger(BrowserUtil.class);

    /**
     * 打开默认浏览器访问指定URL
     *
     * @param url 要访问的URL
     * @return 是否成功打开
     */
    public static boolean openBrowser(String url) {
        if (url == null || url.trim().isEmpty()) {
            logger.warn("URL is null or empty, cannot open browser");
            return false;
        }

        try {
            // 方法1: 使用Desktop API (推荐)
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(url));
                    logger.info("Successfully opened browser with URL: {}", url);
                    return true;
                }
            }

            // 方法2: 使用系统命令 (备用方案)
            return openBrowserWithCommand(url);

        } catch (IOException | URISyntaxException e) {
            logger.error("Failed to open browser with Desktop API for URL: {}", url, e);
            // 尝试备用方案
            return openBrowserWithCommand(url);
        }
    }

    /**
     * 使用系统命令打开浏览器 (备用方案)
     *
     * @param url 要访问的URL
     * @return 是否成功打开
     */
    private static boolean openBrowserWithCommand(String url) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder processBuilder;

            if (os.contains("win")) {
                // Windows
                processBuilder = new ProcessBuilder("rundll32", "url.dll,FileProtocolHandler", url);
            } else if (os.contains("mac")) {
                // macOS
                processBuilder = new ProcessBuilder("open", url);
            } else {
                // Linux/Unix
                processBuilder = new ProcessBuilder("xdg-open", url);
            }

            Process process = processBuilder.start();

            // 等待一小段时间确保命令执行
            Thread.sleep(500);

            logger.info("Successfully opened browser using system command for URL: {}", url);
            return true;

        } catch (IOException | InterruptedException e) {
            logger.error("Failed to open browser using system command for URL: {}", url, e);
            return false;
        }
    }

    /**
     * 检查URL是否有效
     *
     * @param url 要检查的URL
     * @return 是否有效
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return false;
        }

        try {
            new URI(url);
            return url.startsWith("http://") || url.startsWith("https://");
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
