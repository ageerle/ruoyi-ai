package org.ruoyi.ipd.domain;
import com.baomidou.mybatisplus.annotation.*;import lombok.*;import org.ruoyi.common.mybatis.core.domain.BaseEntity;
/**
 * IPD 系统参数（G-05：全部可配置参数，禁止硬编码）
 * 依据：开发说明书 D.0.7 全局系统参数键清单 + §8 涉钱参数
 */
@TableName(value = "system_configs", autoResultMap = true)
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SystemConfig extends BaseEntity {
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;
    /** 参数键（如 bonus.salesSource / allowance.L3 / gate.signDeadlineDays） */
    @TableField("config_key")
    private String configKey;
    @TableField("config_value")
    private String configValue;
    /** STRING|NUMBER|JSON|BOOL */
    @TableField("value_type")
    private String valueType;
    @TableField("default_value")
    private String defaultValue;
    @TableField("description")
    private String description;
    @TableField("tenant_id")
    private String tenantId;
    @TableLogic @TableField("del_flag")
    private String delFlag;
    @TableField("remark")
    private String remark;
}