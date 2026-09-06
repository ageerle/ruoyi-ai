package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 贡献度评定保存请求（P3-6.2；BR-INC-09 / AC-INC-27）。
 *
 * <p>双 PM 自评五维度原始分数（0~100），由系统按维度权重 25/25/20/20/10 计算
 * tierCoefficient（= 加权得分 / 100），无需前端传权重。
 *
 * <p>id / status / leader 字段不可由客户端注入（CODE-01）。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ContributionSaveReq(
    @NotBlank String role,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal dimInitiation,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal dimInnovation,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal dimLaunch,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal dimMarketResult,
    @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal dimLeadership,
    @Size(max = 500) String comment
) {
}