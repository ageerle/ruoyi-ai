package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.BidInvitation;
import org.ruoyi.ipd.dto.CreateBidInvitationRequest;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-3.1 一对一邀标 + 公开招标 DTO 字段补充验收测试（AC-TEAM-01/02；BR-TEAM-03）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>ONE_TO_ONE：targetPersonId 必填；缺失报 PARAM_INVALID</li>
 *   <li>PUBLIC：targetPersonId 禁止；传值报 PARAM_INVALID</li>
 *   <li>mode 非法值报 PARAM_INVALID</li>
 *   <li>expireAt 必须为未来；过去时间报 PARAM_INVALID</li>
 *   <li>PUBLIC + requiredLevel/slaDays 写入 content 扩展</li>
 *   <li>审计写入 CREATE_P231 动作</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-3.1 招标单校验型创建")
class P231BidInvitationCreateAcceptanceTest {

    @Mock BidInvitationMapper bidInvitationMapper;
    @Mock AuditLogService auditLogService;

    @InjectMocks BidP231Validator validator;

    private IpdActor operator;

    @BeforeEach
    void setUp() {
        operator = new IpdActor(100L, "Alice-PM", "MARKET_PM", 10L);
    }

    private CreateBidInvitationRequest baseRequest() {
        CreateBidInvitationRequest req = new CreateBidInvitationRequest();
        req.setProjectId(1000L);
        req.setTitle("Test Bid");
        req.setContent("Base content");
        req.setExpireAt(new Date(System.currentTimeMillis() + 86_400_000L)); // +1d
        return req;
    }

    @Test
    @DisplayName("ONE_TO_ONE：targetPersonId 必填，缺失抛 PARAM_INVALID")
    void oneToOne_requiresTargetPersonId() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(null);

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("ONE_TO_ONE：完整参数 → 写入 OPEN + targetPersonId")
    void oneToOne_persists() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);

        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenAnswer(inv -> {
            BidInvitation arg = inv.getArgument(0);
            arg.setId(500L);
            return 1;
        });

        BidInvitation result = validator.createValidated(req, operator);

        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getMode()).isEqualTo("ONE_TO_ONE");
        assertThat(result.getTargetPersonId()).isEqualTo(200L);
        assertThat(result.getCreateBy()).isEqualTo(operator.id());
    }

    @Test
    @DisplayName("PUBLIC：targetPersonId 禁止（传值抛 PARAM_INVALID）")
    void public_forbidsTargetPersonId() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("PUBLIC");
        req.setTargetPersonId(200L);

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("PUBLIC：requiredLevel + slaDays 写入 content 扩展字段")
    void public_writesExtensionToContent() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("PUBLIC");
        req.setRequiredLevel("L3");
        req.setSlaDays(7);

        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenAnswer(inv -> {
            BidInvitation arg = inv.getArgument(0);
            arg.setId(501L);
            return 1;
        });

        BidInvitation result = validator.createValidated(req, operator);

        assertThat(result.getContent()).contains("<!--ipd-ext:");
        assertThat(result.getContent()).contains("\"requiredLevel\":\"L3\"");
        assertThat(result.getContent()).contains("\"slaDays\":7");
    }

    @Test
    @DisplayName("PUBLIC：无扩展字段时 content 不附加扩展片段")
    void public_noExtension_keepsContentClean() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("PUBLIC");

        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenAnswer(inv -> {
            BidInvitation arg = inv.getArgument(0);
            arg.setId(502L);
            return 1;
        });

        BidInvitation result = validator.createValidated(req, operator);

        assertThat(result.getContent()).isEqualTo("Base content");
        assertThat(result.getContent()).doesNotContain("<!--ipd-ext:");
    }

    @Test
    @DisplayName("mode 非法值（既非 ONE_TO_ONE 也非 PUBLIC）抛 PARAM_INVALID")
    void invalidMode_throws() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("HYBRID");

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("expireAt 必须为未来（过去时间抛 PARAM_INVALID）")
    void expireAt_mustBeFuture() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);
        req.setExpireAt(new Date(System.currentTimeMillis() - 60_000L)); // -1min

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("请求体为 null 抛 PARAM_INVALID")
    void nullRequest_throws() {
        assertThatThrownBy(() -> validator.createValidated(null, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
    }

    @Test
    @DisplayName("审计写入 CREATE_P231 动作 + operatorId")
    void auditLogWritten() {
        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);

        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenAnswer(inv -> {
            BidInvitation arg = inv.getArgument(0);
            arg.setId(503L);
            return 1;
        });

        validator.createValidated(req, operator);

        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(audit.capture());
        assertThat(audit.getValue().getAction()).isEqualTo("CREATE_P231");
        assertThat(audit.getValue().getEntityType()).isEqualTo("bid_invitations");
        assertThat(audit.getValue().getOperatorId()).isEqualTo(operator.id());
        assertThat(audit.getValue().getAfterData()).contains("mode=ONE_TO_ONE");
    }
}
