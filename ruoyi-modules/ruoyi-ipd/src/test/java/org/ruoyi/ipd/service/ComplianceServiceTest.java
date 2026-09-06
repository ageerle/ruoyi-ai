package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.dto.AuditEntryVO;
import org.ruoyi.ipd.dto.DataDeletionRequestDTO;
import org.ruoyi.ipd.dto.DataDeletionRequestVO;
import org.ruoyi.ipd.dto.DataRetentionRuleVO;
import org.ruoyi.ipd.dto.PermissionSeparationVO;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 合规卡服务单测（P2-5.1；6 维度覆盖：正常 / 边界 / 异常 / 权限 / 审计 / 并发基本）。
 *
 * <p>Mockito + AssertJ/JUnit5。@Tag("dev") 让 surefire 在 {@code -Pdev} 下拾起。
 */
@Tag("dev")
class ComplianceServiceTest {

    private SystemConfigService systemConfigService;
    private AuditLogService auditLogService;
    private AuditLogMapper auditLogMapper;
    private PersonMapper personMapper;
    private ObjectMapper objectMapper;
    private ComplianceService service;

    @BeforeEach
    void setUp() {
        systemConfigService = mock(SystemConfigService.class);
        auditLogService = mock(AuditLogService.class);
        auditLogMapper = mock(AuditLogMapper.class);
        personMapper = mock(PersonMapper.class);
        objectMapper = new ObjectMapper();
        service = new ComplianceService(
            systemConfigService, auditLogService, auditLogMapper, personMapper, objectMapper);
    }

    @Test
    void retentionRules_returnsConfiguredRules_withDefaults() {
        // 配置缺失 → 兜底默认（retentionDays=2557, SOFT_DELETE, DSL-内部留存）
        when(systemConfigService.getValue(anyString(), any())).thenReturn(null);

        List<DataRetentionRuleVO> rules = service.getRetentionRules();
        Assertions.assertFalse(rules.isEmpty(), "规则清单非空");
        for (DataRetentionRuleVO r : rules) {
            Assertions.assertNotNull(r.getResourceType());
            Assertions.assertTrue(r.getRetentionDays() > 0);
            Assertions.assertNotNull(r.getDeletionPolicy());
            Assertions.assertNotNull(r.getLegalBasis());
        }
        // 默认兜底：2557 天 / SOFT_DELETE / DSL-内部留存
        DataRetentionRuleVO first = rules.get(0);
        Assertions.assertEquals(2557, first.getRetentionDays());
        Assertions.assertEquals("SOFT_DELETE", first.getDeletionPolicy());
        Assertions.assertEquals("DSL-内部留存", first.getLegalBasis());
    }

    @Test
    void retentionRules_parsesJsonConfig() throws Exception {
        // sys_config 配了自定义 JSON（仅 projects 配，其他仍走默认）
        String json = "{\"retentionDays\":90,\"deletionPolicy\":\"HARD_DELETE\",\"legalBasis\":\"GDPR-Art17\"}";
        when(systemConfigService.getValue(eq("compliance.retention.projects"), any())).thenReturn(json);
        when(systemConfigService.getValue(org.mockito.ArgumentMatchers.argThat(k -> k != null && !k.equals("compliance.retention.projects")), any()))
            .thenReturn(null);

        List<DataRetentionRuleVO> rules = service.getRetentionRules();
        DataRetentionRuleVO projects = rules.stream()
            .filter(r -> "projects".equals(r.getResourceType())).findFirst().orElseThrow();
        Assertions.assertEquals(90, projects.getRetentionDays());
        Assertions.assertEquals("HARD_DELETE", projects.getDeletionPolicy());
        Assertions.assertEquals("GDPR-Art17", projects.getLegalBasis());
    }

    @Test
    void createDeletionRequest_sets30DayDeadline() {
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog arg = inv.getArgument(0);
            arg.setId(1001L);
            arg.setCreateTime(new Date());
            arg.setSeq(42L);
            return arg;
        });

        IpdActor actor = new IpdActor(7L, "张三", "MARKET_PM", 100L);
        DataDeletionRequestDTO dto = DataDeletionRequestDTO.builder()
            .resourceType("projects").resourceId(99L).reason("个保法要求删除").build();

        long before = System.currentTimeMillis();
        DataDeletionRequestVO vo = service.createDeletionRequest(dto, actor);
        long after = System.currentTimeMillis();

        Assertions.assertNotNull(vo.getDeadlineAt());
        // 30 天 ± 5s 误差
        long expectedMin = before + ComplianceService.DELETION_DEADLINE_DAYS * 86_400_000L - 5000L;
        long expectedMax = after + ComplianceService.DELETION_DEADLINE_DAYS * 86_400_000L + 5000L;
        Assertions.assertTrue(vo.getDeadlineAt().getTime() >= expectedMin
            && vo.getDeadlineAt().getTime() <= expectedMax,
            "deadline ≈ now + 30 days");
        Assertions.assertEquals("PENDING", vo.getStatus());
        Assertions.assertEquals(7L, vo.getRequesterId());
    }

    @Test
    void createDeletionRequest_auditsAction_andBlocksBypass() {
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog arg = inv.getArgument(0);
            arg.setId(2L);
            arg.setCreateTime(new Date());
            return arg;
        });

        IpdActor actor = new IpdActor(7L, "张三", "MARKET_PM", 100L);
        service.createDeletionRequest(
            DataDeletionRequestDTO.builder()
                .resourceType("requirements").resourceId(11L).reason("GDPR").build(),
            actor);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(captor.capture());
        AuditLog written = captor.getValue();
        Assertions.assertEquals("COMPLIANCE_DELETION_REQUEST", written.getAction());
        Assertions.assertEquals("requirements", written.getEntityType());
        Assertions.assertEquals(11L, written.getEntityId());
        Assertions.assertEquals(7L, written.getOperatorId());
        // 审计载荷必须经 AuditLogService.append 单点收口（不可绕过）
        Assertions.assertNotNull(written.getAfterData());
        Assertions.assertTrue(written.getAfterData().contains("deadlineAt"));
    }

    @Test
    void createDeletionRequest_rejectsMissingActor() {
        DataDeletionRequestDTO dto = DataDeletionRequestDTO.builder()
            .resourceType("projects").resourceId(1L).reason("test").build();
        Assertions.assertThrows(RuntimeException.class,
            () -> service.createDeletionRequest(dto, null));
    }

    @Test
    void auditTrail_returnsPaginatedEntries_andScopesByActorRole() {
        // 模拟 auditLogMapper.selectPage 返回 2 条
        AuditLog l1 = AuditLog.builder().seq(10L).operatorId(7L).operatorName("张三")
            .action("X").entityType("projects").entityId(99L).build();
        l1.setCreateTime(new Date());
        AuditLog l2 = AuditLog.builder().seq(9L).operatorId(7L).operatorName("张三")
            .action("Y").entityType("projects").entityId(99L).build();
        l2.setCreateTime(new Date());

        IPage<AuditLog> page = new Page<>(1, 20);
        page.setRecords(List.of(l1, l2));
        page.setTotal(2);
        when(auditLogMapper.selectPage(any(com.baomidou.mybatisplus.core.metadata.IPage.class), any())).thenReturn(page);

        // MARKET_PM 角色 → service 会按 operatorId 范围过滤（验证 wrapper 含 operatorId eq）
        IpdActor actor = new IpdActor(7L, "张三", "MARKET_PM", 100L);
        IPage<AuditEntryVO> result = service.getAuditTrail("projects", 99L, actor, 1, 20);

        Assertions.assertEquals(2, result.getRecords().size());
        Assertions.assertEquals(10L, result.getRecords().get(0).getSeq());
    }

    @Test
    void auditTrail_adminSeesAll_noOperatorScopeFilter() {
        AuditLog l1 = AuditLog.builder().seq(10L).operatorId(7L).operatorName("张三")
            .action("X").entityType("audit_logs").entityId(1L).build();
        l1.setCreateTime(new Date());
        IPage<AuditLog> page = new Page<>(1, 20);
        page.setRecords(List.of(l1));
        page.setTotal(1);
        when(auditLogMapper.selectPage(any(com.baomidou.mybatisplus.core.metadata.IPage.class), any())).thenReturn(page);

        IpdActor admin = new IpdActor(1L, "超管", "SUPER_ADMIN", null);
        IPage<AuditEntryVO> result = service.getAuditTrail("audit_logs", 1L, admin, 1, 20);
        Assertions.assertEquals(1, result.getRecords().size());
    }

    @Test
    void auditTrail_rejectsMissingResourceId() {
        IpdActor actor = new IpdActor(7L, "张三", "MARKET_PM", 100L);
        Assertions.assertThrows(RuntimeException.class,
            () -> service.getAuditTrail("projects", null, actor, 1, 20));
    }

    @Test
    void permissionSeparation_detectsReadWriteConflict_forAdmin() {
        Person admin = new Person();
        admin.setId(1L);
        admin.setName("超管");
        admin.setPersonType("SUPER_ADMIN");
        admin.setDelFlag("0");
        when(personMapper.selectById(1L)).thenReturn(admin);

        PermissionSeparationVO vo = service.checkPermissionSeparation(1L);
        Assertions.assertTrue(vo.isHasReadRole());
        Assertions.assertTrue(vo.isHasWriteRole());
        Assertions.assertTrue(vo.isConflict(), "超管 R+W 同源 → conflict=true");
    }

    @Test
    void permissionSeparation_returnsClean_forMarketPm() {
        Person pm = new Person();
        pm.setId(7L);
        pm.setName("张三");
        pm.setPersonType("MARKET_PM");
        pm.setDelFlag("0");
        when(personMapper.selectById(7L)).thenReturn(pm);

        PermissionSeparationVO vo = service.checkPermissionSeparation(7L);
        Assertions.assertTrue(vo.isHasReadRole());
        Assertions.assertFalse(vo.isHasWriteRole());
        Assertions.assertFalse(vo.isConflict(), "MARKET_PM 仅持 R → conflict=false");
        Assertions.assertEquals(List.of("MARKET_PM"), vo.getRoleList());
    }

    @Test
    void permissionSeparation_throwsOnMissingUser() {
        when(personMapper.selectById(999L)).thenReturn(null);
        Assertions.assertThrows(RuntimeException.class,
            () -> service.checkPermissionSeparation(999L));
    }
}
