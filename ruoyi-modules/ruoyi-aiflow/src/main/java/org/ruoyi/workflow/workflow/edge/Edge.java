package org.ruoyi.workflow.workflow.edge;

import lombok.Data;

import java.util.List;

@Data
public class Edge {
    private String sourceNodeUuid;
    private List<String> targetNodeUuid;
}
