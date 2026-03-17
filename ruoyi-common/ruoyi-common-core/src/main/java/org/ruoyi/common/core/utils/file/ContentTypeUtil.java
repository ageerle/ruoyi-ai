package org.ruoyi.common.core.utils.file;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author: xiaoen
 * @Description: Content-Type 映射工具
 * @Date: Created in 18:50 2026/3/17
 */
public class ContentTypeUtil {

    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

    static {
        // 文本文件
        CONTENT_TYPE_MAP.put(".txt", "text/plain; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".html", "text/html; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".htm", "text/html; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".css", "text/css; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".js", "application/javascript; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".json", "application/json; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".xml", "application/xml; charset=UTF-8");
        CONTENT_TYPE_MAP.put(".csv", "text/csv; charset=UTF-8");

        // 图片文件
        CONTENT_TYPE_MAP.put(".jpg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".jpeg", "image/jpeg");
        CONTENT_TYPE_MAP.put(".png", "image/png");
        CONTENT_TYPE_MAP.put(".gif", "image/gif");
        CONTENT_TYPE_MAP.put(".bmp", "image/bmp");
        CONTENT_TYPE_MAP.put(".webp", "image/webp");
        CONTENT_TYPE_MAP.put(".svg", "image/svg+xml");

        // 应用文件
        CONTENT_TYPE_MAP.put(".pdf", "application/pdf");
        CONTENT_TYPE_MAP.put(".doc", "application/msword");
        CONTENT_TYPE_MAP.put(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        CONTENT_TYPE_MAP.put(".xls", "application/vnd.ms-excel");
        CONTENT_TYPE_MAP.put(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        CONTENT_TYPE_MAP.put(".ppt", "application/vnd.ms-powerpoint");
        CONTENT_TYPE_MAP.put(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");

        // 其他
        CONTENT_TYPE_MAP.put(".mp3", "audio/mpeg");
        CONTENT_TYPE_MAP.put(".mp4", "video/mp4");
        CONTENT_TYPE_MAP.put(".zip", "application/zip");
        CONTENT_TYPE_MAP.put(".rar", "application/x-rar-compressed");
    }

    /**
     * 根据后缀返回对应的 content-type
     * @param suffix
     * @param defaultContentType
     * @return
     */
    public static String getContentType(String suffix, String defaultContentType) {
        String contentType = CONTENT_TYPE_MAP.get(suffix.toLowerCase());
        return contentType != null ? contentType : defaultContentType;
    }
}
