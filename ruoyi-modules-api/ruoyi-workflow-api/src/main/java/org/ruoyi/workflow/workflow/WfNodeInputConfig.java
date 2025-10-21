package org.ruoyi.workflow.workflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.ruoyi.workflow.workflow.def.WfNodeIO;
import org.ruoyi.workflow.workflow.def.WfNodeParamRef;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * 节点的输入参数配置
 */
@Validated
@Data
public class WfNodeInputConfig {

    @NotNull
    @JsonProperty("user_inputs")
    private List<WfNodeIO> userInputs;

    @NotNull
    @JsonProperty("ref_inputs")
    private List<WfNodeParamRef> refInputs;
}
