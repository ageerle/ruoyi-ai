package org.ruoyi.utils;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * ZIP文件处理工具类
 */
public class ZipUtils {

    /**
     * 解压ZIP文件到指定目录
     *
     * @param zipData ZIP文件的字节数组
     * @param destDir 目标目录
     * @return 解压后的文件路径列表
     * @throws IOException 如果解压过程中发生错误
     */
    public static String[] unzip(byte[] zipData, String destDir) throws IOException {
        File destDirFile = new File(destDir);
        if (!destDirFile.exists()) {
            destDirFile.mkdirs();
        }

        List<String> extractedPaths = new ArrayList<>();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                String filePath = destDir + File.separator + zipEntry.getName();
                if (!zipEntry.isDirectory()) {
                    extractFile(zis, filePath);
                    extractedPaths.add(filePath);
                } else {
                    new File(filePath).mkdirs();
                }
                zis.closeEntry();
            }
        }
        return extractedPaths.toArray(new String[0]);
    }

    private static void extractFile(ZipInputStream zis, String filePath) throws IOException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath))) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = zis.read(buffer)) != -1) {
                bos.write(buffer, 0, read);
            }
        }
    }

    /**
     * 解压ZIP文件并返回文件内容的Base64编码字符串数组
     *
     * @param zipData ZIP文件的字节数组
     * @return Base64编码的文件内容数组
     * @throws IOException 如果解压过程中发生错误
     */
    public static String[] unzipForBase64(byte[] zipData) throws IOException {
        List<String> base64Contents = new ArrayList<>();
        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipData);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry zipEntry;
            while ((zipEntry = zis.getNextEntry()) != null) {
                if (!zipEntry.isDirectory()) {
                    // 读取文件内容到内存
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int read;
                    while ((read = zis.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    
                    // 将文件内容转换为Base64字符串
                    String base64Content = Base64.getEncoder().encodeToString(baos.toByteArray());
                    base64Contents.add(base64Content);
                }
                zis.closeEntry();
            }
        }
        return base64Contents.toArray(new String[0]);
    }
}