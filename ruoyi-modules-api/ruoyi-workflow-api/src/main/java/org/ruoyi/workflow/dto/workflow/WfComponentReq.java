package org.ruoyi.workflow.dto.workflow;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
public class WfComponentReq {
    private String uuid;
    @NotBlank(message = "标题不能为空")
    private String name;
    @NotBlank(message = "标题不能为空")
    private String title;
    private String remark;
    private Boolean isEnable;
    private Integer displayOrder;
}
