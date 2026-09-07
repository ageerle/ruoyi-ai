package org.ruoyi.ipd.vo;

import org.ruoyi.ipd.domain.GateElement;

import java.util.Date;

/**
 * View: GateElement 的对外暴露视图，移除内部字段（delFlag / createBy / updateBy / createDept / params）。
 * 对应接口：/api/v1/gate-elements
 */
public record GateElementVO(
    Long id,
    String gateCode,
    String elementCode,
    String elementName,
    String passStandard,
    String isVeto,
    Integer sortOrder,
    String enabled,
    String status,
    Integer version,
    String vetoDualRequired,
    String thresholdJson,
    Date signDueAt,
    Integer signExtensionCount,
    Date createTime,
    Date updateTime
) {
    public static GateElementVO from(GateElement e) {
        return new GateElementVO(
            e.getId(),
            e.getGateCode(),
            e.getElementCode(),
            e.getElementName(),
            e.getPassStandard(),
            e.getIsVeto(),
            e.getSortOrder(),
            e.getEnabled(),
            e.getStatus(),
            e.getVersion(),
            e.getVetoDualRequired(),
            e.getThresholdJson(),
            e.getSignDueAt(),
            e.getSignExtensionCount(),
            e.getCreateTime(),
            e.getUpdateTime()
        );
    }
}