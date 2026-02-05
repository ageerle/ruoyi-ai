package org.ruoyi.workflow.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Data
public class WorkflowUpdateReq {
    @NotBlank
    private String uuid;
    @Size(min = 1)
    private List<WfNodeDto> nodes;
    @NotNull
    private List<WfEdgeReq> edges;

    private List<String> deleteNodes;

    private List<String> deleteEdges;
}
