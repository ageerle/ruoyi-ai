package org.ruoyi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiReportGenerateRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "报表需求不能为空")
    private String prompt;

    private Integer maxRows;
}
