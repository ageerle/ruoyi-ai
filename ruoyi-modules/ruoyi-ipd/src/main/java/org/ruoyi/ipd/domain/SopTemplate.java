package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

/**
 * IPD SOP 模板（标准作业程序+版本；v3 TS-05）
 * 绑定深管 42 动作，超管可维护多版本
 * 删除走 DeletionRequestService + DeleteAuditService（P0-6.2，软删除）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "sop_templates", autoResultMap = true)
public class SopTemplate extends BaseEntity implements SoftDeletable {

    /**
     * 主键（雪花算法）
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 绑定动作编号（深管 42 动作）
     */
    private String actionCode;

    /**
     * SOP 标题
     */
    private String title;

    /**
     * SOP 富文本内容（mediumtext）
     */
    private String content;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 状态 DRAFT|PUBLISHED|ARCHIVED
     */
    private String status;

    /**
     * 软删除标志（0正常 1已删）
     */
    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    /**
     * 显式覆盖 Lombok @Accessors(chain=true) 的链式 setter
     */
    public void setDelFlag(String flag) { this.delFlag = flag; }
}
