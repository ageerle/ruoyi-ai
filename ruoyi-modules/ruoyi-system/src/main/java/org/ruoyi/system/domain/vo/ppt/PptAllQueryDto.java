package org.ruoyi.system.domain.vo.ppt;

import lombok.Data;

/**
 * 查询所有PPT列表查询参数
 *
 * @author NSL
 * @since 2024/12/31
 */
@Data
public class PptAllQueryDto {
    // 分页
    private Integer page;
    // 每页大小（最大不超过100）
    private Integer size;
    // ppt id（非必填）
    private String id;
    // 第三方用户ID（非必填）
    private String uid;
    // 模板ID（非必填）
    private String templateId;
    // 创建开始时间（非必填）
    private String startDate;
    // 创建结束时间（非必填）
    private String endDate;
    // 按时间倒序返回（非必填）
    private boolean desc;
}
