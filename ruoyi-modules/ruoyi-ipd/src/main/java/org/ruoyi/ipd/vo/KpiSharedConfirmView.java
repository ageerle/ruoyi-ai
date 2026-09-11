package org.ruoyi.ipd.vo;

import java.math.BigDecimal;

/**
 * P3-1.2-BACKEND 共担 KPI 双组长确认视图（看板卡 56d97bb0 DTO 契约）。
 *
 * <p>字段对齐卡片：id / period / projectId / projectName / personId / personName /
 * metricCode / metricName / weight / deadlineAt / status（PENDING|CONFIRMED|OVERDUE）/
 * firstConfirmedBy / firstConfirmedAt / secondConfirmedBy / secondConfirmedAt；
 * 另补 confirmedByMe 便于前端渲染「待我确认 / 我已确认」。
 *
 * <p>与 /api/v1 包络一致：字符串 ID + 时间 ISO 文本，防前端 BigInt 截断。
 */
public record KpiSharedConfirmView(
    String id,
    String period,
    String projectId,
    String projectName,
    String personId,
    String personName,
    String metricCode,
    String metricName,
    BigDecimal weight,
    String deadlineAt,
    String status,
    String firstConfirmedBy,
    String firstConfirmedAt,
    String secondConfirmedBy,
    String secondConfirmedAt,
    boolean confirmedByMe
) { }
