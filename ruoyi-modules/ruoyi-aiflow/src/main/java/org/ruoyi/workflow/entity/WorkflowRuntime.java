package org.ruoyi.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_workflow_runtime", autoResultMap = true)
@Schema(title = "工作流运行时 | Workflow runtime")
public class WorkflowRuntime extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("uuid")
    private String uuid;

    @TableField("user_id")
    private Long userId;

    @TableField("workflow_id")
    private Long workflowId;

    @TableField(value = "input")
    private String input;

    @TableField(value = "output")
    private String output;

    @TableField("status")
    private Integer status;

    @TableField("status_remark")
    private String statusRemark;
}
