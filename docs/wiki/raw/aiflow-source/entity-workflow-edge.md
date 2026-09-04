---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-aiflow/src/main/java/org/ruoyi/workflow/entity/WorkflowEdge.java
collected: 2026-09-04
published: 2026-09-04
topic: aiflow-source
---

# WorkflowEdge.java

```java
package org.ruoyi.workflow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ruoyi.common.chat.entity.BaseEntity;

import java.io.Serial;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_workflow_edge")
@Schema(title = "工作流定义-边 | workflow definition edge")
public class WorkflowEdge extends BaseEntity {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableField("uuid")
    private String uuid;

    @TableField("workflow_id")
    private Long workflowId;

    @TableField("source_node_uuid")
    private String sourceNodeUuid;

    @TableField("source_handle")
    private String sourceHandle;

    @TableField("target_node_uuid")
    private String targetNodeUuid;
}

```
