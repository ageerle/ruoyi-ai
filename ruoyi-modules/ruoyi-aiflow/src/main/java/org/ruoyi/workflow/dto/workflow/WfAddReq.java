package org.ruoyi.workflow.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class WfAddReq {

    @NotBlank
    private String title;

    private String remark;

    private Boolean isPublic;
}
