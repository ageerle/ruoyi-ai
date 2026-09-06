package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目绩效评分不可变归档记录（P3-2.2）。
 * 评分按组件独立追加，历史结算只读同一 versionNo 的三个组件。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_score_records", autoResultMap = true)
public class ProjectScoreRecord extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /** 项目 ID。 */
    private Long projectId;

    /** 被评 PM 人员 ID。 */
    private Long personId;

    /** MARKET_PM|RD_PM。 */
    private String pmRole;

    /** SELF|MARKET_LEADER|RD_LEADER。 */
    private String componentType;

    /** 0~100 评分。 */
    private BigDecimal score;

    /** 逻辑评分版本；更正不覆盖，只能递增。 */
    private Integer versionNo;

    /** kpi.reviewWeights 规则版本。 */
    private Integer ruleVersion;

    /** 当时生效规则的不可变 JSON 快照。 */
    private String ruleSnapshot;

    /** DRAFT|ARCHIVED；归档行禁止更新评分。 */
    private String status;

    /** 提交人 ID。 */
    private Long authorId;

    /** 提交人角色 MARKET_PM|RD_PM|GROUP_LEADER|SUPER_ADMIN。 */
    private String authorRole;

    /** 更正/说明，不覆盖历史。 */
    private String reason;

    /** 提交时间。 */
    private Date submittedAt;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
