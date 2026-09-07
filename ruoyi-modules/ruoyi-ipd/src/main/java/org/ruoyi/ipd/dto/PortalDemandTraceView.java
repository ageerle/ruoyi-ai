package org.ruoyi.ipd.dto;

import java.util.List;

/**
 * P4-1.2 / CONSISTENCY-2 页39 游客需求脱敏进度视图（GET /api/v1/public/demands/{code}）。
 * <p>规格 batch-04 §4：code, status, customerName(脱敏), timeline, attachments,
 * canSupplement, canWithdraw, withdrawDeadlineAt；不暴露内部 ID / 人员 / 联系方式 / 需求原文。
 * <p>契约以前端 ruoyi-ipd-web api/ipd/portal.ts 的 PortalDemandTrace 为准，字段名一一对应
 * （record 组件名即 JSON 键）。
 * <p>时间字段一律 String（UTC ISO-8601 'Z' 秒级），与 {@code ApiV1Response.timestamp} 同构：
 * 全局 Jackson JavaTimeModule 会把 {@code Instant/Date} 序列化为 epoch millis，字段级
 * 注解无法覆盖 module 路径（P0-4.1 惯例），故 Service 层先行格式化。
 */
public record PortalDemandTraceView(
    String code,
    String status,
    /** 脱敏：保留首字符，其余以 * 替换（「深圳智控科技」→「深*****」），不泄露其余原文。 */
    String customerName,
    /** 受理前（SUBMITTED）且提交未超 24h 才为 true（规格 batch-04 §2 24h 撤回按钮）。 */
    boolean canWithdraw,
    /** 受理前（SUBMITTED）为 true；受理后原文锁定仅可评论（BR-REQ-03a）。 */
    boolean canSupplement,
    /** 撤回截止 = 提交 + 24h；仅受理前非空，受理后 null。 */
    String withdrawDeadlineAt,
    /** 状态推进节点（stage 取 8 态大写词表，occurredAt/memo 可空）。 */
    List<TimelineEntry> timeline,
    /** 附件脱敏视图（仅文件名与大小）；当前游客提交通道无附件，恒空列表占位。 */
    List<AttachmentEntry> attachments
) {

    /**
     * 时间线节点。stage 必填且为 8 态大写词表（SUBMITTED/ACCEPTED/EVALUATING/SCHEDULED/
     * PROCESSING/CLOSED/ARCHIVED）——与前端 STATUS_TEXT 词表键一致；规格 batch-04 §5 的
     * 小写形态在前端无对应文案键，输出大写（契约以前端为准）。
     */
    public record TimelineEntry(String stage, String memo, String occurredAt) {
    }

    /** 附件脱敏项：仅文件名与大小（页39 附件清单），不含下载地址。 */
    public record AttachmentEntry(String fileName, Long fileSize) {
    }
}
