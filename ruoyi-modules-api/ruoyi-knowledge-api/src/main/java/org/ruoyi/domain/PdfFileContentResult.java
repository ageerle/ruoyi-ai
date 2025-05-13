package org.ruoyi.domain;

/**
 * 文件内容结果封装类
 */
public class PdfFileContentResult {
    private String filename;
    private String content;

    public PdfFileContentResult(String filename, String content) {
        this.filename = filename;
        this.content = content;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}