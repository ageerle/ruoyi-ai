package org.ruoyi.ipd.domain;

/**
 * 单个动作定义（动作清单 v3 目录行）
 * valueFields: 该动作要求的数值登记字段（如 FAR,FRR / CERT_NO,CERT_DATE），空=无数值要求
 * applicable: ALL | HW(硬) | SW(软) | SOL(解) | OVERSEAS(海外) | BIOCV(BioCV类)
 */
public record ActionDef(
    String code,
    String name,
    String stage,
    String ownerRole,
    String depth,
    boolean blocking,
    String applicable,
    String valueFields,
    boolean bioFeature,
    String gate) {
}