package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

/**
 * P4-1.2 游客需求补登/撤回请求（AC-REQ-04/04b；页39）。
 * <p>受理前可补登 functionalRequirement / contact；受理后这两个字段被锁定，
 * 仅可通过评论端点追加（保留原值不可改）。
 *
 * @param action            SUPPLEMENT | WITHDRAW
 * @param functionalRequirement 补登内容（≤4000，仅 SUPPLEMENT 时使用）
 * @param contact           补登联系方式（≤128，仅 SUPPLEMENT 时使用）
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GuestDemandUpdateReq(
    String action,
    @Size(max = 4000) String functionalRequirement,
    @Size(max = 128) String contact
) {
    public static final String ACTION_SUPPLEMENT = "SUPPLEMENT";
    public static final String ACTION_WITHDRAW = "WITHDRAW";
}
