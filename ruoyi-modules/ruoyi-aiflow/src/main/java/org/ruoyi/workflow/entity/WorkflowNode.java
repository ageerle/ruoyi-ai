package org.ruoyi.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_workflow_node", autoResultMap = true)
@Schema(title = "工作流定义-节点 | workflow definition node")
public class WorkflowNode extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("uuid")
    private String uuid;

    @TableField("workflow_id")
    private Long workflowId;

    @TableField("workflow_component_id")
    private Long workflowComponentId;

    @TableField("title")
    private String title;

    @TableField("remark")
    private String remark;

    @TableField(value = "input_config")
    private String inputConfig;

    @TableField(value = "node_config")
    private String nodeConfig;

    @TableField("position_x")
    private Double positionX;

    @TableField("position_y")
    private Double positionY;
}
