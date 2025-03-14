package org.ruoyi.common.core.utils.file;

import cn.hutool.core.io.FileUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

/**
 * 文件处理工具类
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class FileUtils extends FileUtil {

    private static final String FILE_EXTENTION_SPLIT = ".";

    /**
     * 下载文件名重新编码
     *
     * @param response     响应对象
     * @param realFileName 真实文件名
     */
    public static void setAttachmentResponseHeader(HttpServletResponse response, String realFileName) {
        String percentEncodedFileName = percentEncode(realFileName);
        String contentDispositionValue = "attachment; filename=%s;filename*=utf-8''%s".formatted(percentEncodedFileName, percentEncodedFileName);
        response.addHeader("Access-Control-Expose-Headers", "Content-Disposition,download-filename");
        response.setHeader("Content-disposition", contentDispositionValue);
        response.setHeader("download-filename", percentEncodedFileName);
    }

    /**
     * 百分号编码工具方法
     *
     * @param s 需要百分号编码的字符串
     * @return 百分号编码后的字符串
     */
    public static String percentEncode(String s) {
        String encode = URLEncoder.encode(s, StandardCharsets.UTF_8);
        return encode.replaceAll("\\+", "%20");
    }

    /**
     * 检查文件扩展名是否符合要求
     *
     * @param file
     * @return
     */
    public static boolean isValidFileExtention(MultipartFile file, String[] ALLOWED_EXTENSIONS) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        final String filename = file.getOriginalFilename();
        if (StringUtils.isBlank(filename) || !filename.contains(FILE_EXTENTION_SPLIT)) {
            return false;
        }
        // 获取文件后缀
        String fileExtension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return Arrays.asList(ALLOWED_EXTENSIONS).contains(fileExtension);
    }

    /**
     * 获取安全的文件路径
     *
     * @param originalFilename 原始文件名
     * @param secureFilePath   安全路径
     * @return 安全文件路径
     */
    public static String getSecureFilePathForUpload(final String originalFilename, final String secureFilePath) {
        String extension = originalFilename.substring(originalFilename.lastIndexOf(FILE_EXTENTION_SPLIT));
        String newFileName = UUID.randomUUID() + extension;

        return secureFilePath + newFileName; // 预定义安全路径
    }
}
