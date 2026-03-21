package org.ruoyi.workflow.dto.workflow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class WfRuntimeNodeDto {
    private Long id;
    private String uuid;
    private Long workflowRuntimeId;
    private Long nodeId;
    private ObjectNode input;
    private ObjectNode output;
    private Integer status;
}
