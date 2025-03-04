package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * 生成PPT参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptGeneratePptxDto {
    // 模板ID（非必填）
    private String templateId;

    // 是否返回PPT数据结构
    private boolean pptxProperty;

    // 大纲内容markdown
    private String outlineContentMarkdown;

    // 备注（PPT页面备注，非必填，数组 ["内容页面一备注", "内容页面二备注"]）
    private String notes;
}
