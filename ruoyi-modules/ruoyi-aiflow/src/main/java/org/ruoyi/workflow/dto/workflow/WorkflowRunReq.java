package org.ruoyi.workflow.dto.workflow;

import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

import java.util.List;

@Data
public class WorkflowRunReq {
    private List<ObjectNode> inputs;
    private String uuid;


}
