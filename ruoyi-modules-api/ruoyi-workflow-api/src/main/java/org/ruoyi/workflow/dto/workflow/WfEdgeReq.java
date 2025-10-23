package org.ruoyi.workflow.dto.workflow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
public class WfEdgeReq {
    private Long id;
    @NotBlank
    private String uuid;
    @Min(1)
    private Long workflowId;
    @NotBlank
    private String sourceNodeUuid;
    private String sourceHandle;
    @NotBlank
    private String targetNodeUuid;
    /**
     * 是否新增
     */
    private Boolean isNew;
}
