package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * 分页查询 PPT 模板查询参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptTemplateQueryDto {
    // 分页
    private Integer page;
    // 每页大小（最大不超过100）
    private Integer size;
    // 模型筛选参数
    private PptTemplateFilterDto filters;
}
