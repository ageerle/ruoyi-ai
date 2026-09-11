package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * P4-2.2 AI 生成请求（AC-AI-02：PM 录入原始资料调用 AI）。
 * prompt = PM 录入的原始资料/生成指令；生成结果登记为版本链 v1（status=GENERATED 待审核）。
 * 校验以 service 层为准（与 CreateReq 同惯例，注解作契约文档）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiGenerateReq(@NotNull Long projectId,
                            @Size(max = 32) String docType,
                            @NotBlank @Size(max = 200) String title,
                            @NotBlank @Size(max = 30000) String prompt) {
}
