package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.dto.CreateBidInvitationRequest;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.BidP231Validator;
import org.springframework.web.bind.annotation.*;

/**
 * P2-3.1 招标单校验型创建端点（AC-TEAM-01/02；BR-TEAM-03）。
 *
 * <p>独立 controller 原因：BidController 已被其他会话 in-progress 修改（OPS-09 单写入者约束），
 * 本卡以独立 controller 方式追加新入口 {@code POST /api/v1/bid-invitations/p231-create}。
 * 既有 {@code POST /api/v1/bid-invitations} 保持不变。
 */
@RestController
@RequestMapping("/api/v1/bid-invitations")
@RequiredArgsConstructor
public class BidP231Controller {

    private final BidP231Validator bidP231Validator;
    private final IpdPermission permission;

    /**
     * P2-3.1 校验型创建招标单。
     * <p>权限：内部用户（requireInternal）。
     * <p>字段校验：
     * <ul>
     *   <li>projectId/mode/title/expireAt 必填（mode ∈ ONE_TO_ONE/PUBLIC）</li>
     *   <li>ONE_TO_ONE：targetPersonId 必填</li>
     *   <li>PUBLIC：targetPersonId 禁止；requiredLevel(L1..L5)、slaDays(1..90) 可选</li>
     * </ul>
     */
    @PostMapping("/p231-create")
    public ApiV1Response<BidInvitation> createValidated(@Valid @RequestBody CreateBidInvitationRequest req) {
        IpdActor operator = permission.requireInternal();
        return ApiV1Response.ok(bidP231Validator.createValidated(req, operator));
    }
}
