package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.controller.DeletionRequestController;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
 * SEC-API-01：身份经 IpdPermission.requireAdmin，不再依赖 StpUtil。
 */
@Tag("dev")
class P064AcceptanceTest {

    private final ObjectMapper json = new ObjectMapper();
    private DeletionRequestMapper deletionRequestMapper;
    private AuditLogService auditLogService;
    private IpdPermission ipdPermission;
    private MockMvc mvc;
    private DeletionArchiveService service;

    private static final long SUPER_ADMIN_ID = 100L;
    private static final IpdActor ADMIN =
        new IpdActor(SUPER_ADMIN_ID, "超管", "SUPER_ADMIN", 1L);

    @BeforeEach
    void setup() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new org.apache.ibatis.builder.MapperBuilderAssistant(
                new com.baomidou.mybatisplus.core.MybatisConfiguration(), "ipd-p064-test-dr"),
            DeletionRequest.class);

        deletionRequestMapper = mock(DeletionRequestMapper.class);
        auditLogService = mock(AuditLogService.class);
        ipdPermission = mock(IpdPermission.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(call -> call.getArgument(0));
        when(ipdPermission.requireAdmin()).thenReturn(ADMIN);

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

        service = new DeletionArchiveService(deletionRequestMapper, auditLogService, ipdPermission);
        DeletionRequestService deletionRequestService = mock(DeletionRequestService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                new DeletionRequestController(service, deletionRequestService, ipdPermission))
            .setMessageConverters(new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(json))
            .setControllerAdvice(new ArchiveAdvice())
            .build();
    }

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @RestControllerAdvice
    static class ArchiveAdvice {
        @ExceptionHandler(ServiceException.class)
        public ResponseEntity<ApiV1Response<Void>> service(ServiceException ex) {
            ApiV1ErrorCode ec = ApiV1ErrorCode.fromCode(ex.getCode());
            return ResponseEntity.status(ec.getHttpStatus())
                .body(ApiV1Response.fail(ec.getCode(), ex.getMessage()));
        }

        @ExceptionHandler(IpdPermissionException.class)
        public ResponseEntity<ApiV1Response<Void>> denied(IpdPermissionException ex) {
            return ResponseEntity.status(ex.getHttpStatus())
                .body(ApiV1Response.fail(ex.getErrorCode().getCode(), ex.getErrorCode().getMessage()));
        }
    }

    /**
     * DEF-8 回归锁：归档区过滤必须 NULL 安全。
     *
     * <p>原断言只检查 {@code contains("NOT LIKE")}，属于「只锁 SQL 文本、不锁语义」的**假绿**：
     * {@code NULL NOT LIKE '%x%'} 在 SQL 三值逻辑下为 NULL，remark 默认 NULL 的行会被整条排除，
     * 归档区恒空，而本用例依旧全绿。现在额外锁住 {@code IS NULL} 与 {@code OR} 的嵌套形状。
     *
     * <p>注意：Mockito 不会真执行 SQL，本用例只是**形状锁**；「归档区真能查到 DELETED 行」的
     * 语义证据由真库脚本给出：{@code docs/ipd-系统说明/验收/P0-9.1-业务链真实验收-20260905.py}
     * 的 L5「归档区列表可见该申请」+ 真库对照 {@code SUM(remark NOT LIKE ...)} vs
     * {@code SUM(remark IS NULL OR remark NOT LIKE ...)}。
     */
    @Test
    void archiveListFilterIsNullSafeForUnpurgedRemark() {
        List<DeletionRequest> list = service.listArchive();
        assertThat(list).hasSize(2);
        ArgumentCaptor<Wrapper<DeletionRequest>> cap = ArgumentCaptor.forClass(Wrapper.class);
        verify(deletionRequestMapper, times(1)).selectList(cap.capture());
        String sql = cap.getValue().getSqlSegment();
        // DEF-8：必须同时具备 IS NULL 放行分支与 NOT LIKE 排除分支，且两者以 OR 相连
        assertThat(sql).contains("status").contains("IS NULL").contains("OR").contains("NOT LIKE");
        LambdaQueryWrapper<DeletionRequest> wrapper = (LambdaQueryWrapper<DeletionRequest>) cap.getValue();
        assertThat(wrapper.getParamNameValuePairs().values())
            .contains("DELETED", "%" + DeletionArchiveService.PURGED_MARK + "%");
    }

    @Test
    void purgeRequiresSuperAdminRole() throws Exception {
        when(ipdPermission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
        verify(auditLogService, times(0)).append(any(AuditLog.class));
    }

    @Test
    void purgeRequiresAuthenticated() throws Exception {
        when(ipdPermission.requireAdmin())
            .thenThrow(new IpdPermissionException(401, ApiV1ErrorCode.UNAUTHORIZED));
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    void superAdminCanPurgeAndAuditIsWritten() throws Exception {
        mvc.perform(post("/api/v1/deletion-requests/11/purge"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(0));
        verify(deletionRequestMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(auditCap.capture());
        AuditLog audit = auditCap.getValue();
        assertThat(audit.getAction()).isEqualTo("DELETE_ARCHIVE_PURGE");
        assertThat(audit.getOperatorId()).isEqualTo(SUPER_ADMIN_ID);
        assertThat(audit.getEntityType()).isEqualTo("projects");
        assertThat(audit.getEntityId()).isEqualTo(99L);
        assertThat(audit.getReason()).contains("purge_by:100");
    }

    @Test
    void purgeFailsWhenStatusIsNotDeleted() throws Exception {
        DeletionRequest draft = DeletionRequest.builder()
            .id(20L).entityType("projects").entityId(50L)
            .status(DeletionRequestService.ST_DRAFT).build();
        when(deletionRequestMapper.selectById(20L)).thenReturn(draft);
        mvc.perform(post("/api/v1/deletion-requests/20/purge"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(50002));
        verify(auditLogService, times(0)).append(any(AuditLog.class));
    }

    @Test
    void purgeFailsWhenRecordAlreadyPurged() throws Exception {
        mvc.perform(post("/api/v1/deletion-requests/12/purge"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value(50002));
    }

    @Test
    void purgeFailsWhenRecordNotFound() throws Exception {
        when(deletionRequestMapper.selectById(999L)).thenReturn(null);
        mvc.perform(post("/api/v1/deletion-requests/999/purge"))
            .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value(50001));
    }

    @Test
    void archiveListViaHttpReturnsOkForSuperAdmin() throws Exception {
        var result = mvc.perform(get("/api/v1/deletion-requests/archive"))
            .andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":0");
        verify(deletionRequestMapper, times(1)).selectList(any(Wrapper.class));
    }

    @Test
    void archiveListForbiddenForNonSuperAdmin() throws Exception {
        when(ipdPermission.requireAdmin())
            .thenThrow(new IpdPermissionException(403, ApiV1ErrorCode.FORBIDDEN));
        mvc.perform(get("/api/v1/deletion-requests/archive"))
            .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value(30001));
    }

    @Test
    void purgeAtomicUpdateRaceResilience() {
        when(deletionRequestMapper.update(isNull(), any(LambdaUpdateWrapper.class)))
            .thenReturn(1).thenReturn(0);
        DeletionRequest first = service.purge(11L);
        assertThat(first).isNotNull();
        verify(auditLogService, times(1)).append(any(AuditLog.class));
        assertThatThrownBy(() -> service.purge(11L)).isInstanceOf(ServiceException.class);
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }
}
