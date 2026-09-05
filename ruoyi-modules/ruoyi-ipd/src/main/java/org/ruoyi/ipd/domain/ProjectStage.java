package org.ruoyi.ipd.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.ruoyi.common.mybatis.core.domain.BaseEntity;

import java.util.Date;

/**
 * 项目六阶段实例（TS-05 project_stages；P1-3.1 在 create() 时一次性 bootstrap）
 * 阶段顺序：CONCEPT → PLAN → DEV → VALID → LAUNCH → LIFECYCLE
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@TableName(value = "project_stages", autoResultMap = true)
public class ProjectStage extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long projectId;
    private String stageCode;
    private String stageName;
    private Integer sortOrder;
    private String status;
    private Long gateId;
    private Date startedAt;
    private Date completedAt;
    private String tenantId;
    private String delFlag;
    private String remark;
}