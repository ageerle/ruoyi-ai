package org.ruoyi.ipd.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Date;

/**
 * 招标单创建请求 DTO（P2-3.1；AC-TEAM-01/02；BR-TEAM-03）。
 *
 * <p>口径：
 * <ul>
 *   <li>{@link #mode} 必填，枚举 {@code ONE_TO_ONE|PUBLIC}（与 BidInvitation.mode DDL 枚举对齐）</li>
 *   <li>{@link #targetPersonId} 当 mode=ONE_TO_ONE 时必填；mode=PUBLIC 时禁止填（业务隔离）</li>
 *   <li>{@link #expireAt} 必填且必须为未来时间（避免即时过期）</li>
 *   <li>{@link #requiredLevel} 选填 L1..L5；用于公开招标时匹配应标者等级（前端页21筛选项）</li>
 *   <li>{@link #slaDays} 选填，区间 [1, 90]；用于公开招标时标定响应 SLA（供扫表提醒使用）</li>
 * </ul>
 *
 * <p>与既有 {@link org.ruoyi.ipd.domain.BidInvitation} 字段映射：mode→mode、targetPersonId→targetPersonId、
 * title→title、content→content、expireAt→expireAt、requiredLevel/slaDays 写入 content 扩展字段
 * （JSON 序列化，content 列存 "{...,\"requiredLevel\":\"L3\",\"slaDays\":7}"），
 * 避免 DDL 增量（轻量级 P2 卡，保持 DDL 零变更）。
 */
@Data
public class CreateBidInvitationRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    @Pattern(regexp = "ONE_TO_ONE|PUBLIC", message = "mode must be ONE_TO_ONE or PUBLIC")
    private String mode;

    /** ONE_TO_ONE 时必填；PUBLIC 时禁止。 */
    private Long targetPersonId;

    @NotBlank
    @Size(max = 200)
    private String title;

    @Size(max = 4000)
    private String content;

    @NotNull
    @Future(message = "expireAt must be future")
    private Date expireAt;

    /** 公开招标应标者等级门槛（L1..L5）；ONE_TO_ONE 模式忽略。 */
    @Pattern(regexp = "L[1-5]", message = "requiredLevel must be L1..L5")
    private String requiredLevel;

    /** 公开招标 SLA 天数；区间 [1, 90]；ONE_TO_ONE 模式忽略。 */
    @Size(min = 1, max = 90, message = "slaDays must be 1..90")
    private Integer slaDays;
}
