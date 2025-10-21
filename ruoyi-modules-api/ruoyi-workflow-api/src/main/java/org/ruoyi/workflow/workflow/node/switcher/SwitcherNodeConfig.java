package org.ruoyi.workflow.workflow.node.switcher;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class SwitcherNodeConfig {

    private List<SwitcherCase> cases;
    @JsonProperty("default_target_node_uuid")
    private String defaultTargetNodeUuid;
}
