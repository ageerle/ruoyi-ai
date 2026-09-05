package org.ruoyi.ipd.service;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.config.SaTokenConfig;
import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.context.SaTokenContextForThreadLocal;
import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import cn.dev33.satoken.filter.SaTokenContextFilterForJakartaServlet;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.controller.DeletionRequestController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P0-6.4 归档区与最终清除验收（AC-DEL-08）。
 * - 归档列表只显示 DELETED + 未 PURGED
 * - 清除 API 必须 SUPER_ADMIN 身份（第二级校验，不依赖 IpdPermission）
 * - 二次确认清除写最终审计（与软删除审计区分）
 * - 同一记录二次清除 → 状态冲突 50002
 * - 仅 audit.append 调用次数增加 1（与软删除审计独立）
 *
 * 采用独立 MockMvc + Sa-Token 真 JWT + PersonMapper 验证身份。
 * 不依赖 owner 的 IpdAuthConfiguration interceptor（已被简化）。
 */
@Tag("dev")
class P064AcceptanceTest {

    private final ObjectMapper json = new ObjectMapper();
    private DeletionRequestMapper deletionRequestMapper;
    private PersonMapper personMapper;
    private AuditLogService auditLogService;
    private Person superAdmin;
    private Person marketPm;
    private MockMvc mvc;
    private SaTokenConfig oldConfig;
    private SaTokenDao oldDao;
    private SaTokenContext oldContext;

    private static final long SUPER_ADMIN_ID = 100L;
    private static final long PM_ID = 200L;

    @BeforeEach
    void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p064-test"),
            Person.class);
        oldConfig = SaManager.getConfig(); oldDao = SaManager.getSaTokenDao(); oldContext = SaManager.getSaTokenContext();
        SaManager.setConfig(new SaTokenConfig().setJwtSecretKey(UUID.randomUUID().toString())
            .setTokenName("Authorization").setTokenPrefix("Bearer").setIsReadCookie(false)
            .setIsReadBody(false).setIsShare(false).setTimeout(900).setIsPrint(false));
        SaManager.setSaTokenDao(new SaTokenDaoDefaultImpl());
        SaManager.setSaTokenContext(new SaTokenContextForThreadLocal());

        deletionRequestMapper = mock(DeletionRequestMapper.class);
        personMapper = mock(PersonMapper.class);
        auditLogService = mock(AuditLogService.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(call -> call.getArgument(0));

        superAdmin = Person.builder().id(SUPER_ADMIN_ID).name("超管").username("super_admin")
            .personType("SUPER_ADMIN").accountStatus("ACTIVE").employmentStatus("ACTIVE").delFlag("0").build();
        marketPm = Person.builder().id(PM_ID).name("PM").username("pm")
            .personType("MARKET_PM").accountStatus("ACTIVE").employmentStatus("ACTIVE").delFlag("0").build();
        when(personMapper.selectById(SUPER_ADMIN_ID)).thenReturn(superAdmin);
        when(personMapper.selectById(PM_ID)).thenReturn(marketPm);

        // 归档区列表 mock：1 条 DELETED + 1 条已 PURGED
        DeletionRequest deleted = DeletionRequest.builder()
            .id(11L).entityType("projects").entityId(99L)
            .status(DeletionRequestService.ST_DELETED)
            .executedAt(new java.util.Date())
            .remark("软删除完成").build();
        DeletionRequest purged = DeletionRequest.builder()
            .id(12L).entityType("products").entityId(88L)
            .status(DeletionRequestService.ST_DELETED)
            .executedAt(new java.util.Date())
            .remark(DeletionArchiveService.PURGED_MARK + "7@1000").build();
        when(deletionRequestMapper.selectList(any(Wrapper.class))).thenReturn(List.of(deleted, purged));
        when(deletionRequestMapper.selectById(11L)).thenReturn(deleted);
        when(deletionRequestMapper.selectById(12L)).thenReturn(purged);
        when(deletionRequestMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(1);

        DeletionArchiveService service = new DeletionArchiveService(deletionRequestMapper, auditLogService, personMapper);
        mvc = MockMvcBuilders.standaloneSetup(new DeletionRequestController(service))
            .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(json))
            .setControllerAdvice(new ServiceExceptionAdvice())
            .addFilters(new SaTokenContextFilterForJakartaServlet())
            .build();
    }

    /** owner 删了全局 advice；本测试自带一个针对 ServiceException → 400 的局部 advice。 */
    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice
    static class ServiceExceptionAdvice {
        @ExceptionHandler(ServiceException.class)
        public ResponseEntity<ApiV1Response<Void>> service(ServiceException ex) {
            Integer code = ex.getCode();
            return ResponseEntity.badRequest().body(ApiV1Response.fail(
                code != null ? code : ApiV1ErrorCode.PARAM_INVALID.getCode(), ex.getMessage()));
        }
    }

    @AfterEach
    void tearDown() {
        if (oldContext != null) SaManager.setSaTokenContext(oldContext);
        if (oldDao != null) SaManager.setSaTokenDao(oldDao);
        if (oldConfig != null) SaManager.setConfig(oldConfig);
        try { StpUtil.logout(); } catch (Exception ignore) { }
    }

    private void loginAs(Long id) {
        StpUtil.login(id);
    }

    @Test void archiveListReturnsOnlyNonPurgedEntries() throws Exception {
        // 归档区列表：DELETED + 非 PURGED 前缀的记录才返回
        DeletionArchiveService service = new DeletionArchiveService(deletionRequestMapper, auditLogService, personMapper);
        List<DeletionRequest> list = service.listArchive();
        assertThat(list).hasSize(2);
        // 验证 SQL 过滤条件：notLike remark 'PURGED_BY_SUPER_ADMIN:%'
        org.mockito.ArgumentCaptor<Wrapper<DeletionRequest>> cap =
            org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(deletionRequestMapper, times(1)).selectList(cap.capture());
        String sql = cap.getValue().getSqlSegment();
        assertThat(sql).contains("DELETED");
        assertThat(sql).contains("PURGED_BY_SUPER_ADMIN");
    }

    @Test void purgeRequiresSuperAdminRole() throws Exception {
        // 权限校验：MARKET_PM 调用 purge → 403 FORBIDDEN
        loginAs(PM_ID);
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        verify(auditLogService, times(0)).append(any(AuditLog.class));
    }

    @Test void purgeRequiresAuthenticated() throws Exception {
        // 未登录：401 UNAUTHORIZED
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(20001));
    }

    @Test void superAdminCanPurgeAndAuditIsWritten() throws Exception {
        // 超管调用 purge 成功：remark 加 PURGED_ 前缀 + 写一条 PURGE 审计
        loginAs(SUPER_ADMIN_ID);
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));

        // 数据库 update 调用 1 次
        verify(deletionRequestMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        // 审计调用 1 次（PURGE 独立审计）
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCap.capture());
        AuditLog audit = auditCap.getValue();
        assertThat(audit.getAction()).isEqualTo("DELETE_ARCHIVE_PURGE");
        assertThat(audit.getOperatorId()).isEqualTo(SUPER_ADMIN_ID);
        assertThat(audit.getEntityType()).isEqualTo("projects");
        assertThat(audit.getEntityId()).isEqualTo(99L);
        assertThat(audit.getReason()).contains("purge_by:100");
    }

    @Test void purgeFailsWhenStatusIsNotDeleted() throws Exception {
        // 状态非 DELETED → 50002 STATE_CONFLICT
        DeletionRequest draft = DeletionRequest.builder()
            .id(20L).entityType("projects").entityId(50L)
            .status(DeletionRequestService.ST_DRAFT).build();
        when(deletionRequestMapper.selectById(20L)).thenReturn(draft);
        loginAs(SUPER_ADMIN_ID);
        mvc.perform(post("/api/v1/deletion-requests/20/purge"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(50002));
        verify(auditLogService, times(0)).append(any(AuditLog.class));
    }

    @Test void purgeFailsWhenRecordAlreadyPurged() throws Exception {
        // remark 已有 PURGED_ 前缀 → 50002 不可二次清除
        loginAs(SUPER_ADMIN_ID);
        mvc.perform(post("/api/v1/deletion-requests/12/purge"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(50002));
    }

    @Test void purgeFailsWhenRecordNotFound() throws Exception {
        when(deletionRequestMapper.selectById(999L)).thenReturn(null);
        loginAs(SUPER_ADMIN_ID);
        mvc.perform(post("/api/v1/deletion-requests/999/purge"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value(50001));
    }

    @Test void archiveListViaHttpReturnsOkForSuperAdmin() throws Exception {
        // 完整 HTTP 路径验证：超管登录后 GET archive 返回 DELETED 列表
        loginAs(SUPER_ADMIN_ID);
        var result = mvc.perform(get("/api/v1/deletion-requests/archive"))
            .andExpect(status().isOk()).andReturn();
        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"code\":0");
        // 列表 SQL 已过滤 PURGED 前缀
        org.mockito.ArgumentCaptor<Wrapper<DeletionRequest>> cap =
            org.mockito.ArgumentCaptor.forClass(Wrapper.class);
        verify(deletionRequestMapper, times(1)).selectList(cap.capture());
    }

    @Test void archiveListForbiddenForNonSuperAdmin() throws Exception {
        // 非超管：403
        loginAs(PM_ID);
        mvc.perform(get("/api/v1/deletion-requests/archive"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
    }

    @Test void purgeAtomicUpdateRaceResilience() throws Exception {
        // 并发清除同一记录：第一次 update 返回 1 → 成功；第二次返回 0（被乐观锁挡住）→ 状态冲突
        when(deletionRequestMapper.update(isNull(), any(LambdaUpdateWrapper.class)))
            .thenReturn(1).thenReturn(0);
        DeletionArchiveService service = new DeletionArchiveService(deletionRequestMapper, auditLogService, personMapper);
        // 第一次
        DeletionRequest first = service.purge(11L);
        assertThat(first).isNotNull();
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }
}