package org.ruoyi.ipd.dto;

/**
 * P1-5.2：门禁必做清单单项（可解释原因）。
 *
 * @param code    动作编码
 * @param name    动作名称
 * @param stage   所属阶段
 * @param status  实例状态；未实例化为 null
 * @param ok      是否已满足门禁（DONE/NA）
 * @param reason  人可读原因（配置来源 / 未完成说明）
 */
public record GateChecklistItem(
    String code,
    String name,
    String stage,
    String status,
    boolean ok,
    String reason
) {
}
