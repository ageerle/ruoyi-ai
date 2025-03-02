package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * 生成PPT大纲参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptGenerateOutlineDto {
    // 是否流式生成（默认流式）
    private boolean stream = true;
    // 篇幅长度：short/medium/long, 默认 medium, 分别对应: 10-15页/20-30页/25-35页
    private String length = "medium";
    // 语言: zh/zh-Hant/en/ja/ko/ar/de/fr/it/pt/es/ru
    private String lang;
    // 用户要求（小于50字）
    private String prompt;
    // 方式一：通过主题创建 主题（与dataUrl可同时存在）
    private String subject;
    // 方式二：通过文件内容创建 文件数据url，通过解析文件内容接口返回（与subject可同时存在）
    private String dataUrl;
}
