package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * IPD 业务参数历史版本（ROOT-R1 P0-5 配套）
 * <p>每次 {@link #update} 在事务内闭合当前开区间行 + 追加新行，溯源用。
 */
@TableName(value = "ipd_business_config_versions", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IpdBusinessConfigVersion extends BaseEntity {

    @TableLogic @TableField("del_flag")
    private String delFlag;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 业务参数 ID（关联 ipd_business_config.id） */
    @TableField("config_id")
    private Long configId;

    @TableField("config_key")
    private String configKey;

    /** 历史参数值 */
    @TableField("config_value")
    private String configValue;

    /** 历史版本号 */
    @TableField("version")
    private Integer version;

    /** 当时是否启用 */
    @TableField("enabled")
    private Integer enabled;

    /** 生效起点（[QA-04-E-FIX] 2026-09-06：用 import Date 替换 java.util.Date FQN，匹配 QA-04 类型契约）。 */
    @TableField("effective_from")
    private Date effectiveFrom;

    /** 生效终点（null=当前生效）。 */
    @TableField("effective_to")
    private Date effectiveTo;

    @TableField("tenant_id")
    private String tenantId;
}
