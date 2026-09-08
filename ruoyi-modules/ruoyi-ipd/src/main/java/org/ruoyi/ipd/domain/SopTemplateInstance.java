package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * SOP 模板实例快照（P1-3.3）
 * <p>由 MARKET_PM/RD_PM 在项目立项时触发 {@link org.ruoyi.ipd.service.SopTemplateService#instantiate}：
 * 拷贝 {@link SopTemplate} 在快照点的全部字段到 {@link #snapshotJson}（动作列表/责任矩阵/截止日期等），
 * 实现「实例与模板版本解耦」——后续模板迭代不影响已在跑实例（P1-3.3 BR-IPD-SOP-03）。
 * <p>同一项目同一 templateCode 仅允许一条 ACTIVE；新版本发布时旧实例自动 SUPERSEDED
 * （{@link org.ruoyi.ipd.service.SopTemplateService#supersedeInstance}）。
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sop_template_instances", autoResultMap = true)
public class SopTemplateInstance extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 模板主键（关联 sop_templates.id） */
    private Long templateId;

    /** 实例版本号（项目级自增；与 SopTemplate.version 同源，便于追溯） */
    private Long instanceVersion;

    /** 项目主键（关联 projects.id） */
    private Long projectId;

    /**
     * 快照 JSON（不可变）：包含 actionList/responsibilityMatrix/deadlineMap 等动作目录快照。
     * 实例创建后不再随模板迭代变化（immutable）。
     */
    private String snapshotJson;

    /** 实例化时间 */
    private Date instantiatedAt;

    /** 实例化人 Person ID */
    private String instantiatedBy;

    /** 状态 ACTIVE|SUPERSEDED|ARCHIVED */
    private String status;

    /** 软删除标志（0正常 1已删） */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /** 租户ID */
    private String tenantId;

    /** 显式覆盖 Lombok @Accessors(chain=true) 链式 setter 以匹配 SoftDeletable 接口签名 */
    public void setDelFlag(String flag) { this.delFlag = flag; }

    /** 状态枚举字面量 */
    public static final class Status {
        public static final String ACTIVE = "ACTIVE";
        public static final String SUPERSEDED = "SUPERSEDED";
        public static final String ARCHIVED = "ARCHIVED";
        private Status() {}
    }
}