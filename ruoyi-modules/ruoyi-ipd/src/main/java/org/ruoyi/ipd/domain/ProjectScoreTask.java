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

import java.util.Date;

/** 上市 30/90 日项目绩效待办（P3-2.3）。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_score_tasks", autoResultMap = true)
public class ProjectScoreTask extends BaseEntity implements SoftDeletable {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;
    private Long personId;
    /** SELF_SCORING|LEADER_REVIEW。 */
    private String targetType;
    private Date dueAt;
    /** 记录创建时使用的上市日期；日期更正是更新而非新增。 */
    private Date launchDateSnapshot;
    /** PENDING|DONE|CANCELLED。 */
    private String status;
    private String actionUrl;

    @TableLogic
    @TableField("del_flag")
    private String delFlag;

    public void setDelFlag(String flag) {
        this.delFlag = flag;
    }
}
