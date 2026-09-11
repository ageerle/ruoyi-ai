package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.controller.PublicPortalController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.dto.PortalDemandTraceView;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P4-1.2 / CONSISTENCY-2：页39 GET /api/v1/public/demands/{code} 脱敏进度端点验收
 * （AC-REQ-04；BR-REQ-03 受理前撤回/补充；规格 batch-04 §4 脱敏视图）。
 * <p>契约以前端 ruoyi-ipd-web api/ipd/portal.ts 的 PortalDemandTrace 为准，字段一一对应：
 * code/status/customerName(末位脱敏)/canWithdraw/canSupplement/withdrawDeadlineAt/timeline/attachments。
 * <p>安全反例三同码：查无此码、格式非法码、已撤回（CONSISTENCY-2 裁决：不泄露存在性差异）。
 */
@Tag("dev")
@DisplayName("P4-1.2 游客查询码脱敏进度查询（页39 / CONSISTENCY-2）")
class PortalDemandTraceTest {

    private static final String ISO_PATTERN = "^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z$";

    private RequirementMapper requirementMapper;
    private AuditLogService auditLogService;
    private GuestDemandService.GuestRateLimiter limiter;
    private GuestDemandService service;

    @BeforeEach
    void setUp() {
        requirementMapper = mock(RequirementMapper.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        limiter = mock(GuestDemandService.GuestRateLimiter.class);
        when(limiter.tryAcquire(anyString())).thenReturn(true);
        service = new GuestDemandService(requirementMapper, mock(ProductMapper.class),
            mock(ProjectMemberMapper.class), auditLogService, limiter);
    }

    private static Requirement demand(String status, long createMillisAgo, Date routedAt, Date acceptedAt) {
        Requirement r = new Requirement();
        r.setId(101L);
        r.setQueryCode("AB12CD34");
        r.setStatus(status);
        r.setSource("PORTAL_GUEST");
        r.setCustomerName("深圳智控科技");
        r.setSubmitterName("王工");
        r.setContact("13800000000");
        r.setContent("希望增加离线导出报表功能，支持按月归档");
        r.setCreateTime(new Date(System.currentTimeMillis() - createMillisAgo));
        r.setRoutedAt(routedAt);
        r.setAcceptedAt(acceptedAt);
        r.setUpdateTime(acceptedAt);
        return r;
    }

    @Test
    @DisplayName("正例1：受理前（提交2h+已路由）→ 双开关开 + deadline=提交+24h + timeline 提交/路由两节点 + 末位脱敏")
    void traceBeforeAcceptFullContract() {
        Date routedAt = new Date(System.currentTimeMillis() - 3_600_000L);
        when(requirementMapper.selectOne(any())).thenReturn(
            demand("SUBMITTED", 2 * 3_600_000L, routedAt, null));

        PortalDemandTraceView view = service.traceByCode("AB12CD34", "1.2.3.4");

        assertEquals("AB12CD34", view.code());
        assertEquals("SUBMITTED", view.status());
        // 末位脱敏：保留首字符，其余打码，不泄露全文（规格 batch-04 §4 customerName 末位脱敏）
        assertEquals("深" + "*".repeat(5), view.customerName());
        assertFalse(view.customerName().contains("智控"), "脱敏后不得包含原文其余字符");
        assertTrue(view.canWithdraw(), "受理前且提交未超 24h 可撤回");
        assertTrue(view.canSupplement(), "受理前可补充");
        assertNotNull(view.withdrawDeadlineAt(), "withdrawDeadlineAt 仅受理前非空");
        assertTrue(view.withdrawDeadlineAt().matches(ISO_PATTERN), "UTC ISO-8601 秒级字符串");
        assertEquals(GuestDemandService.toIsoUtc(new Date(
                System.currentTimeMillis() - 2 * 3_600_000L + GuestDemandService.TRACE_WITHDRAW_HOURS * 3_600_000L)),
            view.withdrawDeadlineAt(), "deadline = 提交时间 + 24h");
        // timeline：提交节点 + 路由节点（路由是 SUBMITTED 内子事件，前端 8 态词表无 ROUTED 键）
        assertEquals(2, view.timeline().size());
        assertEquals("SUBMITTED", view.timeline().get(0).stage());
        assertNull(view.timeline().get(0).memo());
        assertEquals("SUBMITTED", view.timeline().get(1).stage());
        assertNotNull(view.timeline().get(1).memo(), "路由节点以 memo 表达");
        assertEquals(GuestDemandService.toIsoUtc(routedAt), view.timeline().get(1).occurredAt());
        // attachments：当前后端无游客附件，恒空列表但字段在（前端宽松解析）
        assertNotNull(view.attachments());
        assertTrue(view.attachments().isEmpty());

        // 规格闭环剧本 ⑤：查询写审计 action=track
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertEquals("track", auditCap.getValue().getAction());
        assertEquals("guest_demand", auditCap.getValue().getEntityType());
    }

    @Test
    @DisplayName("正例2：已受理（ACCEPTED）→ 双开关关 + deadline=null + timeline 含 ACCEPTED 节点")
    void acceptedLocksAllGuestOps() {
        Date acceptedAt = new Date(System.currentTimeMillis() - 1_800_000L);
        when(requirementMapper.selectOne(any())).thenReturn(
            demand("ACCEPTED", 6 * 3_600_000L, null, acceptedAt));

        PortalDemandTraceView view = service.traceByCode("AB12CD34", "1.2.3.4");

        assertFalse(view.canWithdraw(), "受理后锁定不可撤（BR-REQ-03）");
        assertFalse(view.canSupplement(), "受理后原文锁定仅可评论（BR-REQ-03a）");
        assertNull(view.withdrawDeadlineAt(), "受理后撤回期限无意义置 null");
        assertEquals(2, view.timeline().size());
        assertEquals("ACCEPTED", view.timeline().get(1).stage());
        assertEquals(GuestDemandService.toIsoUtc(acceptedAt), view.timeline().get(1).occurredAt());
    }

    @Test
    @DisplayName("正例3：提交超 24h 仍 SUBMITTED → 撤回窗口过期 canWithdraw=false，补充仍开")
    void withdrawWindowExpiry() {
        when(requirementMapper.selectOne(any())).thenReturn(
            demand("SUBMITTED", 25 * 3_600_000L, null, null));

        PortalDemandTraceView view = service.traceByCode("AB12CD34", "1.2.3.4");

        assertFalse(view.canWithdraw(), "超 24h 撤回窗口关闭");
        assertTrue(view.canSupplement(), "补充无时限仅受受理约束");
        assertNotNull(view.withdrawDeadlineAt(), "过期 deadline 仍返回供前端展示已过期");
    }

    @Test
    @DisplayName("反例1：查无此码 → 50001 NOT_FOUND")
    void unknownCodeNotFound() {
        when(requirementMapper.selectOne(any())).thenReturn(null);
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.traceByCode("ZZ99ZZ99", "1.2.3.4"));
        assertEquals(ApiV1ErrorCode.NOT_FOUND, ex.getErrorCode());
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("反例2：格式非法码（小写/7位/含空格）→ 与查无此码同 50001，且不触库")
    void malformedCodeSameNotFound() {
        for (String bad : new String[] {"ab12cd34", "AB12CD3", "AB1 CD34"}) {
            IpdBusinessException ex = assertThrows(IpdBusinessException.class,
                () -> service.traceByCode(bad, "1.2.3.4"));
            assertEquals(ApiV1ErrorCode.NOT_FOUND, ex.getErrorCode(), "非法码=" + bad);
        }
        verify(requirementMapper, never()).selectOne(any());
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("反例3：已撤回（WITHDRAWN）→ 与查无此码同 50001，不泄露存在性差异（CONSISTENCY-2）")
    void withdrawnSameAsUnknown() {
        when(requirementMapper.selectOne(any())).thenReturn(
            demand("WITHDRAWN", 3_600_000L, null, null));
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.traceByCode("AB12CD34", "1.2.3.4"));
        assertEquals(ApiV1ErrorCode.NOT_FOUND, ex.getErrorCode(),
            "已撤回须与查无此码同码（规格 batch-04 原定 410 会泄露码曾存在，此处按裁决收口为同码）");
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("反例4：限流触发 → 40011 RATE_LIMITED，查库前拦截")
    void traceRateLimited() {
        when(limiter.tryAcquire(anyString())).thenReturn(false);
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.traceByCode("AB12CD34", "1.2.3.4"));
        assertEquals(ApiV1ErrorCode.RATE_LIMITED, ex.getErrorCode());
        verify(requirementMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("限流键空间：查询走 trace: 前缀与提交窗口隔离，同款设施 10 次/小时")
    void traceRateKeyNamespaced() {
        when(requirementMapper.selectOne(any())).thenReturn(
            demand("SUBMITTED", 3_600_000L, null, null));
        service.traceByCode("AB12CD34", "1.2.3.4");
        ArgumentCaptor<String> keyCap = ArgumentCaptor.forClass(String.class);
        verify(limiter).tryAcquire(keyCap.capture());
        assertEquals("trace:" + GuestDemandService.sha256Short("1.2.3.4"), keyCap.getValue());
    }

    @Test
    @DisplayName("HTTP：GET /api/v1/public/demands/{code} 200 + ApiV1Response 包络 + 字段名与前端 PortalDemandTrace 一一对应")
    void httpTraceEndpointMatchesFrontendContract() throws Exception {
        GuestDemandService mockService = mock(GuestDemandService.class);
        when(mockService.traceByCode("AB12CD34", "127.0.0.1")).thenReturn(
            new PortalDemandTraceView("AB12CD34", "SUBMITTED", "深*****",
                true, true, "2026-09-06T10:00:00Z",
                List.of(new PortalDemandTraceView.TimelineEntry("SUBMITTED", null, "2026-09-06T09:00:00Z")),
                List.of(new PortalDemandTraceView.AttachmentEntry("需求截图.png", 20480L))));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicPortalController(mockService)).build();

        mvc.perform(get("/api/v1/public/demands/AB12CD34"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.code").value("AB12CD34"))
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.customerName").value("深*****"))
            .andExpect(jsonPath("$.data.canWithdraw").value(true))
            .andExpect(jsonPath("$.data.canSupplement").value(true))
            .andExpect(jsonPath("$.data.withdrawDeadlineAt").value("2026-09-06T10:00:00Z"))
            .andExpect(jsonPath("$.data.timeline[0].stage").value("SUBMITTED"))
            .andExpect(jsonPath("$.data.timeline[0].occurredAt").value("2026-09-06T09:00:00Z"))
            .andExpect(jsonPath("$.data.attachments[0].fileName").value("需求截图.png"))
            .andExpect(jsonPath("$.data.attachments[0].fileSize").value(20480));
    }
}
