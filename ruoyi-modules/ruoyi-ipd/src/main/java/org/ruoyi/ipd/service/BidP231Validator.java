package org.ruoyi.ipd.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.dto.CreateBidInvitationRequest;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * P2-3.1 招标单校验型创建（P2-3.1；AC-TEAM-01/02；BR-TEAM-03）。
 *
 * <p>独立 service 原因：BidInvitationService 已被其他会话 in-progress 修改（OPS-09 单写入者约束），
 * 本卡以独立 service 方式追加校验型入口，避免抢活；既有 {@code BidInvitationService.create(BidInvitation)}
 * 保持不变供历史调用。
 *
 * <p>校验规则（与 CreateBidInvitationRequest 注解对称，服务端再保一次防绕过）：
 * <ul>
 *   <li>mode ∈ {ONE_TO_ONE, PUBLIC}；否则 PARAM_INVALID</li>
 *   <li>ONE_TO_ONE：targetPersonId 必填</li>
 *   <li>PUBLIC：targetPersonId 必须 null；requiredLevel/slaDays 可选且写扩展字段</li>
 *   <li>expireAt 必须为未来</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class BidP231Validator {

    private final BidInvitationMapper bidInvitationMapper;
    private final AuditLogService auditLogService;

    /** mode 枚举常量。 */
    public static final String MODE_ONE_TO_ONE = "ONE_TO_ONE";
    public static final String MODE_PUBLIC = "PUBLIC";

    public BidInvitation createValidated(CreateBidInvitationRequest req, IpdActor operator) {
        if (req == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "请求体不能为空");
        }
        String mode = req.getMode();
        if (!MODE_ONE_TO_ONE.equals(mode) && !MODE_PUBLIC.equals(mode)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "mode 必须为 ONE_TO_ONE 或 PUBLIC，实际: " + mode);
        }
        if (MODE_ONE_TO_ONE.equals(mode)) {
            if (req.getTargetPersonId() == null) {
                throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                    "ONE_TO_ONE 模式 targetPersonId 必填");
            }
        } else {
            if (req.getTargetPersonId() != null) {
                throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                    "PUBLIC 模式禁止指定 targetPersonId");
            }
        }
        if (req.getExpireAt() == null || !req.getExpireAt().after(new Date())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "expireAt 必须为未来时间");
        }

        BidInvitation inv = new BidInvitation();
        inv.setProjectId(req.getProjectId());
        inv.setMode(mode);
        inv.setTargetPersonId(req.getTargetPersonId());
        inv.setTitle(req.getTitle());
        if (MODE_PUBLIC.equals(mode)
            && (req.getRequiredLevel() != null || req.getSlaDays() != null)) {
            inv.setContent(appendExtension(req.getContent(), req.getRequiredLevel(), req.getSlaDays()));
        } else {
            inv.setContent(req.getContent());
        }
        inv.setExpireAt(req.getExpireAt());
        inv.setStatus("OPEN");
        inv.setCreateTime(new Date());
        inv.setCreateBy(operator.id());
        bidInvitationMapper.insert(inv);

        auditLogService.append(AuditLog.builder()
            .entityType("bid_invitations")
            .entityId(inv.getId())
            .action("CREATE_P231")
            .operatorId(operator.id())
            .beforeData(null)
            .afterData("{id=" + inv.getId() + ",projectId=" + inv.getProjectId()
                + ",mode=" + mode + ",targetPersonId=" + inv.getTargetPersonId()
                + ",expireAt=" + inv.getExpireAt()
                + (req.getRequiredLevel() == null ? "" : ",requiredLevel=" + req.getRequiredLevel())
                + (req.getSlaDays() == null ? "" : ",slaDays=" + req.getSlaDays()) + "}")
            .build());
        log.info("P2-3.1 createValidated: invitationId={} mode={} operator={}",
            inv.getId(), mode, operator.id());
        return inv;
    }

    /** 扩展字段追加到 content 末尾（隐藏 JSON 片段，前端透明）。 */
    private String appendExtension(String original, String requiredLevel, Integer slaDays) {
        StringBuilder sb = new StringBuilder(original == null ? "" : original);
        sb.append("\n<!--ipd-ext:");
        boolean needComma = false;
        if (requiredLevel != null) {
            sb.append("\"requiredLevel\":\"").append(requiredLevel).append("\"");
            needComma = true;
        }
        if (slaDays != null) {
            if (needComma) sb.append(",");
            sb.append("\"slaDays\":").append(slaDays);
        }
        sb.append("-->");
        return sb.toString();
    }
}
