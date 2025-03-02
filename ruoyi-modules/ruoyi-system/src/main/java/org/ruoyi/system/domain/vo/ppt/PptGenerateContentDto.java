package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * 生成大纲内容参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptGenerateContentDto {
    // 模板ID（非必填）
    private String templateId;

    // 大纲 markdown 文本
    private String outlineMarkdown;

    // 异步生成PPT（这里必须为 true 才会流式生成）
    private boolean asyncGenPptx = false;

    // 用户要求
    private String prompt;

    // 文件数据url，调用解析文件内容接口返回
    private String dataUrl;
}
