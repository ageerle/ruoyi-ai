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
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.dto.GuestDemandSubmitReq;
import org.ruoyi.ipd.dto.GuestDemandSubmittedView;
import org.ruoyi.ipd.dto.PublicProductView;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * P4-1.1 游客需求提交模型与三路产品归属验收（页38 用例1-4；AC-REQ-01/02、AC-PROD-08、BR-REQ-03/04）。
 */
@Tag("dev")
@DisplayName("P4-1.1 游客需求提交与三路归属")
class P411AcceptanceTest {

    private RequirementMapper requirementMapper;
    private ProductMapper productMapper;
    private ProjectMemberMapper projectMemberMapper;
    private AuditLogService auditLogService;
    private GuestDemandService.GuestRateLimiter limiter;
    private GuestDemandService service;

    @BeforeEach
    void setUp() {
        requirementMapper = mock(RequirementMapper.class);
        productMapper = mock(ProductMapper.class);
        projectMemberMapper = mock(ProjectMemberMapper.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        limiter = mock(GuestDemandService.GuestRateLimiter.class);
        when(limiter.tryAcquire(anyString())).thenReturn(true);
        service = new GuestDemandService(requirementMapper, productMapper, projectMemberMapper,
            auditLogService, limiter);
        when(requirementMapper.selectCount(any())).thenReturn(0L);
    }

    private static GuestDemandSubmitReq req(Long productId, String rawModel, String website) {
        return new GuestDemandSubmitReq("深圳智控科技", "王工", "13800000000",
            productId, rawModel, "希望增加离线导出报表功能，支持按月归档", website);
    }

    @Test
    @DisplayName("用例1：选在售产品提交 → 8位查询码 + 双PM自动路由 + 审计submit")
    void submitWithProductRoutesDualPm() {
        Product p = new Product();
        p.setId(9L); p.setStatus("ACTIVE"); p.setProjectId(77L); p.setProductName("ZK-X100");
        when(productMapper.selectById(9L)).thenReturn(p);
        ProjectMember mkt = member(77L, "MARKET_PM", 501L);
        ProjectMember rd = member(77L, "RD_PM", 602L);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of(mkt, rd));

        GuestDemandSubmittedView view = service.submit(req(9L, "ZK-X100", null), "1.2.3.4", "Mozilla/5.0");

        assertTrue(GuestDemandService.QUERY_CODE_PATTERN.matcher(view.code()).matches(),
            "查询码必须匹配 ^[A-Z0-9]{8}$，实际=" + view.code());
        assertEquals("SUBMITTED", view.status());
        ArgumentCaptor<Requirement> cap = ArgumentCaptor.forClass(Requirement.class);
        verify(requirementMapper).insert(cap.capture());
        Requirement saved = cap.getValue();
        assertEquals(9L, saved.getProductId());
        assertEquals(501L, saved.getMarketPmId());
        assertEquals(602L, saved.getRdPmId());
        assertNotNull(saved.getRoutedAt(), "BR-REQ-04 路由时间必须写入");
        assertEquals("PORTAL_GUEST", saved.getSource());

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        AuditLog audit = auditCap.getValue();
        assertEquals("submit", audit.getAction());
        assertEquals("guest_demand", audit.getEntityType());
        assertEquals("route=routed", splitDetail(audit));
        assertTrue(audit.getAfterData().contains("ipHash=") && audit.getAfterData().contains("uaHash="));
        assertNotNull(audit.getIpAddress(), "审计 ipAddress 落 ipHash");
    }

    @Test
    @DisplayName("用例2：其他/未找到 → 待指派池，不路由不通知（AC-PROD-08）")
    void submitOtherGoesUnassignedPool() {
        GuestDemandSubmittedView view = service.submit(req(null, "ZZZ-999", null), "1.2.3.4", "UA");

        ArgumentCaptor<Requirement> cap = ArgumentCaptor.forClass(Requirement.class);
        verify(requirementMapper).insert(cap.capture());
        Requirement saved = cap.getValue();
        assertNull(saved.getProductId(), "其他/不确定 product_id 必须为 NULL");
        assertNull(saved.getMarketPmId());
        assertNull(saved.getRdPmId());
        assertNull(saved.getRoutedAt(), "不触发路由");
        assertEquals("ZZZ-999", saved.getRawModel());
        assertTrue(saved.getTitle().startsWith("ZZZ-999"), "title 记录原始型号输入");
        assertEquals("SUBMITTED", view.status());
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertEquals("route=unassigned", splitDetail(auditCap.getValue()));
    }

    @Test
    @DisplayName("用例4a：honeypot 非空 → 400 拒绝 + spam_rejected 审计，不落库")
    void honeypotRejected() {
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.submit(req(null, null, "http://spam.bot"), "1.2.3.4", "UA"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, ex.getErrorCode());
        verify(requirementMapper, never()).insert(any(Requirement.class));
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertEquals("spam_rejected", auditCap.getValue().getAction());
    }

    @Test
    @DisplayName("用例3：同IP第11次 → 40011 速率限制")
    void eleventhSubmitRateLimited() {
        when(limiter.tryAcquire(anyString())).thenReturn(false);
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.submit(req(null, null, null), "1.2.3.4", "UA"));
        assertEquals(ApiV1ErrorCode.RATE_LIMITED, ex.getErrorCode());
        verify(requirementMapper, never()).insert(any(Requirement.class));
    }

    @Test
    @DisplayName("40401：产品已下架（INACTIVE）拒绝")
    void inactiveProductRejected() {
        Product p = new Product();
        p.setId(9L); p.setStatus("INACTIVE");
        when(productMapper.selectById(9L)).thenReturn(p);
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.submit(req(9L, null, null), "1.2.3.4", "UA"));
        assertEquals(ApiV1ErrorCode.PRODUCT_INACTIVE, ex.getErrorCode());
    }

    @Test
    @DisplayName("50001：产品不存在拒绝")
    void missingProductRejected() {
        when(productMapper.selectById(4_044L)).thenReturn(null);
        IpdBusinessException ex = assertThrows(IpdBusinessException.class,
            () -> service.submit(req(4_044L, null, null), "1.2.3.4", "UA"));
        assertEquals(ApiV1ErrorCode.NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("服务端校验：customerName<2 与 functionalRequirement<6 拒绝（10001）")
    void fieldValidationBounds() {
        IpdBusinessException e1 = assertThrows(IpdBusinessException.class,
            () -> service.submit(new GuestDemandSubmitReq("a", "王工", null, null, null, "希望增加离线导出报表功能", null), "ip", "ua"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, e1.getErrorCode());
        IpdBusinessException e2 = assertThrows(IpdBusinessException.class,
            () -> service.submit(new GuestDemandSubmitReq("深圳智控科技", "王工", null, null, null, "太短了", null), "ip", "ua"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, e2.getErrorCode());
        // feedbackPerson 上限 64 = 列宽 submitter_name varchar(64)，65 字符拒绝防 Data too long
        IpdBusinessException e3 = assertThrows(IpdBusinessException.class,
            () -> service.submit(new GuestDemandSubmitReq("深圳智控科技", "王".repeat(65), null, null, null, "希望增加离线导出报表功能", null), "ip", "ua"));
        assertEquals(ApiV1ErrorCode.PARAM_INVALID, e3.getErrorCode());
    }

    @Test
    @DisplayName("查询码碰撞 → 查重重试后成功")
    void queryCodeCollisionRetried() {
        when(requirementMapper.selectCount(any())).thenReturn(1L, 1L, 0L);
        GuestDemandSubmittedView view = service.submit(req(null, null, null), "ip", "ua");
        assertTrue(GuestDemandService.QUERY_CODE_PATTERN.matcher(view.code()).matches());
        verify(requirementMapper, times(3)).selectCount(any());
    }

    @Test
    @DisplayName("路由边界：项目成员已全部退出 → 不写双PM（project-no-pm）")
    void exitedMembersNotRouted() {
        Product p = new Product();
        p.setId(9L); p.setStatus("ACTIVE"); p.setProjectId(77L);
        when(productMapper.selectById(9L)).thenReturn(p);
        when(projectMemberMapper.selectList(any())).thenReturn(List.of());
        service.submit(req(9L, null, null), "ip", "ua");
        ArgumentCaptor<Requirement> cap = ArgumentCaptor.forClass(Requirement.class);
        verify(requirementMapper).insert(cap.capture());
        assertNull(cap.getValue().getMarketPmId());
        assertNull(cap.getValue().getRoutedAt());
    }

    @Test
    @DisplayName("公开产品列表：listingStatus 三态派生（ON_SALE/IN_DEV/OTHER）")
    void publicProductsDeriveListingStatus() {
        Product onSale = new Product(); onSale.setId(1L); onSale.setProductName("A"); onSale.setModelCode("M1"); onSale.setStatus("ACTIVE");
        Product inDev = new Product(); inDev.setId(2L); inDev.setProductName("B"); inDev.setProjectId(5L); inDev.setStatus("ACTIVE");
        Product other = new Product(); other.setId(3L); other.setProductName("C"); other.setStatus("ACTIVE");
        when(productMapper.selectList(any())).thenReturn(List.of(onSale, inDev, other));
        List<PublicProductView> views = service.publicProducts();
        assertEquals(List.of("ON_SALE", "IN_DEV", "OTHER"),
            views.stream().map(PublicProductView::listingStatus).toList());
    }

    @Test
    @DisplayName("ipHash/uaHash：SHA-256 截断且确定性")
    void ipHashDeterministicShort() {
        String h1 = GuestDemandService.sha256Short("1.2.3.4");
        String h2 = GuestDemandService.sha256Short("1.2.3.4");
        assertEquals(h1, h2);
        assertEquals(16, h1.length());
        assertNotEquals(h1, GuestDemandService.sha256Short("1.2.3.5"));
    }

    @Test
    @DisplayName("HTTP：POST /api/v1/public/demands 200 + code；GET /api/v1/public/products 200（TS-09 形状）")
    void httpEndpoints() throws Exception {
        GuestDemandService mockService = mock(GuestDemandService.class);
        when(mockService.submit(any(), any(), any()))
            .thenReturn(new GuestDemandSubmittedView("AB12CD34", "SUBMITTED"));
        when(mockService.publicProducts()).thenReturn(List.of(
            new PublicProductView(1L, "ZK-X100", "M1", "ACTIVE", "ON_SALE")));
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new PublicPortalController(mockService)).build();

        mvc.perform(post("/api/v1/public/demands")
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Forwarded-For", "9.9.9.9")
                .header("User-Agent", "JUnit")
                .content("{\"customerName\":\"深圳智控科技\",\"feedbackPerson\":\"王工\",\"functionalRequirement\":\"希望增加离线导出报表功能\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.code").value("AB12CD34"))
            .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
        verify(mockService).submit(any(), eq("9.9.9.9"), any());

        mvc.perform(get("/api/v1/public/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].listingStatus").value("ON_SALE"));
    }

    @Test
    @DisplayName("内存限流器：第11次拒绝，窗口语义正确")
    void inMemoryRateLimiterWindow() {
        GuestDemandService.InMemoryHourRateLimiter rl = new GuestDemandService.InMemoryHourRateLimiter();
        for (int i = 1; i <= GuestDemandService.RATE_LIMIT_PER_HOUR; i++) {
            assertTrue(rl.tryAcquire("ip-a"), "第" + i + "次应放行");
        }
        assertFalse(rl.tryAcquire("ip-a"), "第11次必须拒绝");
        assertTrue(rl.tryAcquire("ip-b"), "不同IP互不影响");
    }

    private static ProjectMember member(long projectId, String role, long personId) {
        ProjectMember m = new ProjectMember();
        m.setProjectId(projectId);
        m.setRole(role);
        m.setPersonId(personId);
        m.setJoinDate(new Date());
        return m;
    }

    private static String splitDetail(AuditLog audit) {
        // detail 形如 ipHash=..;uaHash=..;route=...;productId=...——取 route 段
        for (String part : audit.getAfterData().split(";")) {
            if (part.contains("route=")) {
                return part.substring(part.indexOf("route="));
            }
        }
        return "";
    }
}
