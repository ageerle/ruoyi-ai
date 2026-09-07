package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.SopTemplate;
import org.ruoyi.ipd.domain.SopTemplateInstance;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.SopTemplateInstanceMapper;
import org.ruoyi.ipd.mapper.SopTemplateMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.dao.DuplicateKeyException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * P1-3.3 SOP 模板版本管理与实例快照（BR-IPD-SOP-01/02/03）：
 * 模板版本自增、旧 PUBLISHED 自动 ARCHIVED、实例化快照不可变、权限守卫。
 */
@Tag("dev")
class SopTemplateServiceTest {

    private SopTemplateMapper templateMapper;
    private SopTemplateInstanceMapper instanceMapper;
    private AuditLogService auditLogService;
    private ProjectMemberMapper projectMemberMapper;
    private ProjectMapper projectMapper;
    private SopTemplateService service;

    private static final IpdActor ADMIN = new IpdActor(1L, "admin", "SUPER_ADMIN", 100L);
    private static final IpdActor MARKET_PM = new IpdActor(2L, "market", "MARKET_PM", 100L);
    private static final IpdActor RD_PM = new IpdActor(3L, "rd", "RD_PM", 100L);
    private static final IpdActor GUEST = new IpdActor(4L, "guest", "GUEST", 100L);
    private static final IpdActor OUTSIDER = new IpdActor(99L, "outsider", "MARKET_PM", 200L);

    @BeforeEach
    void setUp() {
        templateMapper = mock(SopTemplateMapper.class);
        instanceMapper = mock(SopTemplateInstanceMapper.class);
        auditLogService = mock(AuditLogService.class);
        projectMemberMapper = mock(ProjectMemberMapper.class);
        projectMapper = mock(ProjectMapper.class);
        when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        when(templateMapper.insert(any(SopTemplate.class))).thenAnswer(inv -> {
            SopTemplate t = inv.getArgument(0);
            if (t.getId() == null) t.setId(System.nanoTime());
            return 1;
        });
        when(instanceMapper.insert(any(SopTemplateInstance.class))).thenAnswer(inv -> {
            SopTemplateInstance i = inv.getArgument(0);
            if (i.getId() == null) i.setId(System.nanoTime());
            return 1;
        });
        when(instanceMapper.updateById(any(SopTemplateInstance.class))).thenReturn(1);
        // 默认：项目存在 + 当前 actor 是项目成员（让现有测试无须额外 stub 即可通过新守卫）
        when(projectMapper.selectById(anyLong())).thenReturn(stubProject());
        when(projectMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        service = new SopTemplateService(
            templateMapper, instanceMapper, auditLogService, projectMemberMapper, projectMapper);
    }

    /** 项目 stub——tenantId=null 让 IpdIdorGuard.currentTenantId() 容错跳过跨租户校验 */
    private static Project stubProject() {
        Project p = new Project();
        p.setId(100L);
        p.setTenantId(null);
        return p;
    }

    // ========== 模板版本管理 ==========

    @Test
    @DisplayName("publishTemplate 新模板版本号从 1 起")
    void publishTemplateFirstVersion() {
        // 没有同 templateCode 旧 PUBLISHED
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        SopTemplate input = SopTemplate.builder()
            .templateCode("SOP-CONCEPT-DEEP")
            .templateName("深管概念阶段模板")
            .category(SopTemplate.Category.DEEP_MGMT)
            .build();
        Long id = service.publishTemplate(input, ADMIN);
        assertThat(id).isNotNull();
        assertThat(id).isPositive();
        // 验证 selectList 被调用过
        org.mockito.Mockito.verify(templateMapper).insert(any(SopTemplate.class));
    }

    @Test
    @DisplayName("publishTemplate 旧 PUBLISHED 自动 ARCHIVED + 版本号 +1")
    void publishTemplateAutoArchive() {
        // 同 templateCode 下旧 PUBLISHED=3
        SopTemplate old = SopTemplate.builder()
            .id(100L).templateCode("SOP-PLAN-LIGHT").templateName("老")
            .version(3L).status(SopTemplate.Status.PUBLISHED).delFlag("0").build();
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(old));
        SopTemplate input = SopTemplate.builder()
            .templateCode("SOP-PLAN-LIGHT").templateName("新").category(SopTemplate.Category.LIGHT_MGMT).build();
        service.publishTemplate(input, ADMIN);
        // 旧 status=ARCHIVED + effectiveTo 已设
        assertThat(old.getStatus()).isEqualTo(SopTemplate.Status.ARCHIVED);
        assertThat(old.getEffectiveTo()).isNotNull();
        // 新 insert 被调用，新版本号=4
        org.mockito.Mockito.verify(templateMapper).insert(org.mockito.Mockito.argThat((SopTemplate t) ->
            t.getVersion() != null && t.getVersion() == 4L
                && SopTemplate.Status.PUBLISHED.equals(t.getStatus())
                && SopTemplate.Category.LIGHT_MGMT.equals(t.getCategory())));
        // 至少 2 条审计（ARCHIVE 旧 + PUBLISH 新）
        org.mockito.Mockito.verify(auditLogService, org.mockito.Mockito.atLeast(2)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("publishTemplate 同 templateCode 跨版本保持线性递增")
    void publishTemplateVersionMonotonic() {
        SopTemplate oldA = SopTemplate.builder()
            .id(100L).templateCode("SOP-MIXED").version(5L)
            .status(SopTemplate.Status.PUBLISHED).delFlag("0").build();
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(oldA));
        service.publishTemplate(SopTemplate.builder()
            .templateCode("SOP-MIXED").templateName("x").category(SopTemplate.Category.MIXED).build(), ADMIN);
        org.mockito.Mockito.verify(templateMapper).insert(org.mockito.Mockito.argThat((SopTemplate t) ->
            t.getVersion() != null && t.getVersion() == 6L));
    }

    // ========== getActiveTemplate ==========

    @Test
    @DisplayName("getActiveTemplate 不存在时抛 IpdBusinessException(NOT_FOUND)")
    void getActiveTemplateNotFound() {
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        assertThatThrownBy(() -> service.getActiveTemplate("SOP-UNKNOWN"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("getActiveTemplate 取最新一条 PUBLISHED + effectiveTo=null")
    void getActiveTemplateReturnsFirst() {
        SopTemplate active = SopTemplate.builder()
            .id(50L).templateCode("SOP-DEV-LIGHT").version(2L)
            .status(SopTemplate.Status.PUBLISHED).delFlag("0").build();
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(active));
        SopTemplate out = service.getActiveTemplate("SOP-DEV-LIGHT");
        assertThat(out.getId()).isEqualTo(50L);
        assertThat(out.getStatus()).isEqualTo(SopTemplate.Status.PUBLISHED);
    }

    // ========== instantiate ==========

    @Test
    @DisplayName("instantiate snapshotJson 包含 meta/actionList/responsibilityMatrix/phaseDeadlineMap")
    void instantiateSnapshotFields() {
        SopTemplate t = SopTemplate.builder()
            .id(10L).templateCode("SOP-CONCEPT-DEEP").templateName("深管概念")
            .version(1L).status(SopTemplate.Status.PUBLISHED)
            .category(SopTemplate.Category.DEEP_MGMT).delFlag("0").build();
        when(templateMapper.selectById(10L)).thenReturn(t);
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        SopTemplateInstance inst = service.instantiate(10L, 200L, MARKET_PM);
        assertThat(inst.getStatus()).isEqualTo(SopTemplateInstance.Status.ACTIVE);
        assertThat(inst.getInstanceVersion()).isEqualTo(1L);
        assertThat(inst.getSnapshotJson()).isNotBlank();
        // 关键字段校验
        assertThat(inst.getSnapshotJson()).contains("\"meta\"");
        assertThat(inst.getSnapshotJson()).contains("\"actionList\"");
        assertThat(inst.getSnapshotJson()).contains("\"responsibilityMatrix\"");
        assertThat(inst.getSnapshotJson()).contains("\"phaseDeadlineMap\"");
        assertThat(inst.getSnapshotJson()).contains("\"templateCode\":\"SOP-CONCEPT-DEEP\"");
        assertThat(inst.getSnapshotJson()).contains("\"category\":\"DEEP_MGMT\"");
        // DEEP_MGMT 应只含 DEEP 动作
        assertThat(inst.getSnapshotJson()).contains("\"depth\":\"DEEP\"");
        // 实例化时间非空
        assertThat(inst.getInstantiatedAt()).isNotNull();
        assertThat(inst.getInstantiatedBy()).isEqualTo("2");
    }

    @Test
    @DisplayName("instantiate 不可改快照：返回的 snapshotJson 是构造期固化的字面量")
    void instantiateImmutableSnapshot() {
        SopTemplate t = SopTemplate.builder()
            .id(11L).templateCode("SOP-LAUNCH-MIXED").templateName("混合上市")
            .version(1L).status(SopTemplate.Status.PUBLISHED)
            .category(SopTemplate.Category.MIXED).delFlag("0").build();
        when(templateMapper.selectById(11L)).thenReturn(t);
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        SopTemplateInstance inst = service.instantiate(11L, 201L, MARKET_PM);
        // 校验 snapshotJson 是 String + 非空，且后续修改入参不影响已生成的快照
        String before = inst.getSnapshotJson();
        t.setTemplateName("故意改名");
        assertThat(inst.getSnapshotJson()).isEqualTo(before);
        assertThat(inst.getSnapshotJson()).contains("混合上市");
    }

    @Test
    @DisplayName("supersedeInstance 旧 ACTIVE 实例自动 SUPERSEDED；新实例 ACTIVE")
    void supersedeOnNewInstantiate() {
        SopTemplate t = SopTemplate.builder()
            .id(20L).templateCode("SOP-VALID-DEEP").version(1L)
            .status(SopTemplate.Status.PUBLISHED).category(SopTemplate.Category.DEEP_MGMT).delFlag("0").build();
        SopTemplateInstance old = SopTemplateInstance.builder()
            .id(900L).templateId(20L).projectId(300L).instanceVersion(1L)
            .status(SopTemplateInstance.Status.ACTIVE).delFlag("0").build();
        when(templateMapper.selectById(20L)).thenReturn(t);
        // 第一次 selectList（查同 templateId+projectId 的 ACTIVE）= 1 条；第二次（查全部实例）= 1 条
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(old))    // 旧 ACTIVE
            .thenReturn(List.of(old));   // 同 templateId+projectId 全部
        SopTemplateInstance fresh = service.instantiate(20L, 300L, MARKET_PM);
        assertThat(fresh.getStatus()).isEqualTo(SopTemplateInstance.Status.ACTIVE);
        assertThat(fresh.getInstanceVersion()).isEqualTo(2L); // 1+1
        // 旧实例被 updateById 标 SUPERSEDED
        assertThat(old.getStatus()).isEqualTo(SopTemplateInstance.Status.SUPERSEDED);
        org.mockito.Mockito.verify(instanceMapper).updateById(org.mockito.Mockito.argThat(
            (SopTemplateInstance i) -> i.getId() != null && i.getId() == 900L
                && SopTemplateInstance.Status.SUPERSEDED.equals(i.getStatus())));
    }

    // ========== 权限 ==========

    @Test
    @DisplayName("publishTemplate 仅超管可调用：MARKET_PM 拒绝")
    void publishTemplateRequireAdmin() {
        assertThatThrownBy(() -> service.publishTemplate(
            SopTemplate.builder().templateCode("X").templateName("X").category("DEEP_MGMT").build(),
            MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("instantiate MARKET_PM 可调用")
    void instantiateAllowedForMarketPm() {
        SopTemplate t = SopTemplate.builder()
            .id(30L).templateCode("SOP-MKT").version(1L)
            .status(SopTemplate.Status.PUBLISHED).category(SopTemplate.Category.MIXED).delFlag("0").build();
        when(templateMapper.selectById(30L)).thenReturn(t);
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        SopTemplateInstance inst = service.instantiate(30L, 500L, MARKET_PM);
        assertThat(inst.getStatus()).isEqualTo(SopTemplateInstance.Status.ACTIVE);
    }

    @Test
    @DisplayName("instantiate RD_PM 可调用")
    void instantiateAllowedForRdPm() {
        SopTemplate t = SopTemplate.builder()
            .id(31L).templateCode("SOP-RD").version(1L)
            .status(SopTemplate.Status.PUBLISHED).category(SopTemplate.Category.DEEP_MGMT).delFlag("0").build();
        when(templateMapper.selectById(31L)).thenReturn(t);
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        SopTemplateInstance inst = service.instantiate(31L, 501L, RD_PM);
        assertThat(inst.getStatus()).isEqualTo(SopTemplateInstance.Status.ACTIVE);
    }

    @Test
    @DisplayName("instantiate GUEST 角色拒绝（仅 MARKET_PM/RD_PM/GROUP_LEADER/SUPER_ADMIN）")
    void instantiateRejectedForGuest() {
        SopTemplate t = SopTemplate.builder()
            .id(32L).templateCode("SOP-X").version(1L)
            .status(SopTemplate.Status.PUBLISHED).category(SopTemplate.Category.MIXED).delFlag("0").build();
        when(templateMapper.selectById(32L)).thenReturn(t);
        assertThatThrownBy(() -> service.instantiate(32L, 502L, GUEST))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    // ========== [P1-3.3-IDOR-FIX] post-commit security review 4 项修复 ==========

    @Test
    @DisplayName("[P1-3.3-IDOR-FIX-1] instantiate 非项目成员 → FORBIDDEN（项目成员守卫）")
    void instantiateNonProjectMemberForbidden() {
        // override 默认 mock：actor 不是项目成员
        when(projectMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        SopTemplate t = SopTemplate.builder()
            .id(40L).templateCode("SOP-IDOR").version(1L)
            .status(SopTemplate.Status.PUBLISHED).category(SopTemplate.Category.MIXED).delFlag("0").build();
        when(templateMapper.selectById(40L)).thenReturn(t);
        assertThatThrownBy(() -> service.instantiate(40L, 600L, MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("[P1-3.3-IDOR-FIX-2] listInstancesByProject 非项目成员 → FORBIDDEN")
    void listInstancesByProjectNonProjectMemberForbidden() {
        when(projectMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        assertThatThrownBy(() -> service.listInstancesByProject(700L, MARKET_PM))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("[P1-3.3-IDOR-FIX-3] publishTemplate 同 templateCode 并发冲突 → STATE_CONFLICT")
    void publishTemplateConcurrentDuplicateKey() {
        when(templateMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        when(templateMapper.insert(any(SopTemplate.class)))
            .thenThrow(new DuplicateKeyException("uk_sop_template_code_published"));
        assertThatThrownBy(() -> service.publishTemplate(
            SopTemplate.builder()
                .templateCode("SOP-RACE").templateName("race")
                .category(SopTemplate.Category.DEEP_MGMT).build(),
            ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(ex -> ((IpdBusinessException) ex).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("[P1-3.3-IDOR-FIX-4] instantiate MARKET_PM + 在项目成员中 + PUBLISHED 模板 → 完整流程成功")
    void instantiateFullFlowSuccess() {
        SopTemplate t = SopTemplate.builder()
            .id(50L).templateCode("SOP-FULL").templateName("full flow")
            .version(2L).status(SopTemplate.Status.PUBLISHED)
            .category(SopTemplate.Category.MIXED).delFlag("0").tenantId(null).build();
        when(templateMapper.selectById(50L)).thenReturn(t);
        when(instanceMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(new ArrayList<>());
        // 默认 mock 已含项目成员关系，直接走完整路径
        SopTemplateInstance inst = service.instantiate(50L, 800L, MARKET_PM);
        assertThat(inst.getStatus()).isEqualTo(SopTemplateInstance.Status.ACTIVE);
        assertThat(inst.getTemplateId()).isEqualTo(50L);
        assertThat(inst.getProjectId()).isEqualTo(800L);
        assertThat(inst.getInstanceVersion()).isEqualTo(1L);
        assertThat(inst.getInstantiatedBy()).isEqualTo("2");
        assertThat(inst.getSnapshotJson()).contains("\"templateCode\":\"SOP-FULL\"");
    }
}