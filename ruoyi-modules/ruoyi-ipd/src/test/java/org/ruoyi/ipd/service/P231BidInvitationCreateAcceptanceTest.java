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
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.dto.CreateBidInvitationRequest;
import org.ruoyi.ipd.mapper.BidInvitationMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
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
 *   <li>HIGH authorization：project 不存在 → NOT_FOUND；跨组 MARKET_PM → FORBIDDEN；SUPER_ADMIN 跨组放行</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-3.1 招标单校验型创建")
class P231BidInvitationCreateAcceptanceTest {

    @Mock BidInvitationMapper bidInvitationMapper;
    @Mock AuditLogService auditLogService;
    /** HIGH authorization fix：project 可见性校验 */
    @Mock ProjectMapper projectMapper;
    @Mock IpdPermission ipdPermission;

    @InjectMocks BidP231Validator validator;

    private IpdActor operator;

    @BeforeEach
    void setUp() {
        operator = new IpdActor(100L, "Alice-PM", "MARKET_PM", 10L);
        // 默认：项目 1000L 存在且 mainGroupId=10L（与 operator.groupId 匹配）；既有 9 测走这条
        lenient().when(projectMapper.selectById(anyLong())).thenAnswer(inv -> {
            Project p = new Project();
            p.setId(inv.getArgument(0));
            p.setMainGroupId(10L);
            return p;
        });
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

    // ============ HIGH authorization fix（10~12） ============

    @Test
    @DisplayName("HIGH authorization：projectId 不存在 → NOT_FOUND")
    void projectNotFound_throwsNotFound() {
        when(projectMapper.selectById(9999L)).thenReturn(null);

        CreateBidInvitationRequest req = baseRequest();
        req.setProjectId(9999L);
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("HIGH authorization：MARKET_PM 跨组（actor.groupId≠project.mainGroupId）→ FORBIDDEN")
    void marketPm_crossGroup_forbidden() {
        // 项目 1000L 属于 group 99；operator 在 group 10
        when(projectMapper.selectById(1000L)).thenAnswer(inv -> {
            Project p = new Project();
            p.setId(1000L);
            p.setMainGroupId(99L);
            return p;
        });

        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);

        assertThatThrownBy(() -> validator.createValidated(req, operator))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("HIGH authorization：SUPER_ADMIN 跨组放行（无视 mainGroupId 不匹配）")
    void superAdmin_crossGroup_allowed() {
        // 项目属于 group 99；super admin 在 group 0
        when(projectMapper.selectById(1000L)).thenAnswer(inv -> {
            Project p = new Project();
            p.setId(1000L);
            p.setMainGroupId(99L);
            return p;
        });

        IpdActor superAdmin = new IpdActor(1L, "Root", "SUPER_ADMIN", 0L);

        CreateBidInvitationRequest req = baseRequest();
        req.setMode("ONE_TO_ONE");
        req.setTargetPersonId(200L);

        when(bidInvitationMapper.insert(any(BidInvitation.class))).thenAnswer(inv -> {
            BidInvitation arg = inv.getArgument(0);
            arg.setId(600L);
            return 1;
        });

        BidInvitation result = validator.createValidated(req, superAdmin);
        assertThat(result.getId()).isEqualTo(600L);
        assertThat(result.getCreateBy()).isEqualTo(1L);
    }
}
