package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.Date;

/**
 * CODE-01：新建项目请求白名单（API-02）。
 * 服务端权威字段（code/currentStage/status/source/lifecycleStatus/mainGroupId/tenantId/delFlag/id）
 * 一律不收——code 由 nextCode 生成，阶段/状态由状态机迁移，source 由 create 默认 NEW。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProjectCreateReq(
    String name,
    Long productId,
    String templateType,
    String targetMarkets,
    String level,
    BigDecimal levelCoefficient,
    String levelCoefficientReason,
    BigDecimal targetSalesAmount,
    Integer targetChannelCount,
    Integer targetNps,
    Integer targetSceneCount,
    Date launchDate) {

    public org.ruoyi.ipd.domain.Project toEntity() {
        return org.ruoyi.ipd.domain.Project.builder()
            .name(name).productId(productId).templateType(templateType)
            .targetMarkets(targetMarkets).level(level).levelCoefficient(levelCoefficient)
            .levelCoefficientReason(levelCoefficientReason)
            .targetSalesAmount(targetSalesAmount).targetChannelCount(targetChannelCount)
            .targetNps(targetNps).targetSceneCount(targetSceneCount).launchDate(launchDate)
            .build();
    }
}
