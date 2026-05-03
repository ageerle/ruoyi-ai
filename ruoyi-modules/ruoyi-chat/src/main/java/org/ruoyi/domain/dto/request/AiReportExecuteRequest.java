package org.ruoyi.domain.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiReportExecuteRequest {

    @NotBlank(message = "模型不能为空")
    private String model;

    @NotBlank(message = "报表标题不能为空")
    private String title;

    @NotBlank(message = "报表摘要不能为空")
    private String summary;

    @NotBlank(message = "SQL 不能为空")
    private String sql;
}
