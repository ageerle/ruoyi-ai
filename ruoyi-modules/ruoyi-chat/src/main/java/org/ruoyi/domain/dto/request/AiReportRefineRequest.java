package org.ruoyi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiReportRefineRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "编辑提示词不能为空")
    private String prompt;

    @NotBlank(message = "当前报表 HTML 不能为空")
    private String html;

    private String dataContext;
}
