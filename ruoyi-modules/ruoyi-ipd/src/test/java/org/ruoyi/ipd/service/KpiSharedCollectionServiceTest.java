package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.KpiRecord;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * W4-Security IDOR 修复（件 1）+ W4-E §1.5 listSharedKpis 单测。
 *
 * <p>覆盖 5 维度：
 * <ol>
 *   <li>无 actor（null） → UNAUTHORIZED</li>
 *   <li>项目不存在 → FORBIDDEN</li>
 *   <li>actor 越项目（非成员） → FORBIDDEN（MEDIUM IDOR 核心修复）</li>
 *   <li>actor 跨租户 → FORBIDDEN（多租户 + project 一致性）</li>
 *   <li>actor 正常在职成员 → 返回 KpiRecord 列表（含 revision 排序）</li>
 *   <li>（额外覆盖）SUPER_ADMIN 跨项目访问豁免 → 返回列表</li>
 * </ol>
 *
 * <p>件 1.7 必查：4 旧测例已由 SharedKpiControllerTest 覆盖（已升级到 3-arg 签名）；
 * 本类负责 service 层 IDOR 鉴权契约的 5 测例。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class KpiSharedCollectionServiceTest {

    @Mock private KpiRecordMapper kpiRecordMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper projectMemberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper productGroupMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemConfigService systemConfigService;
    @Mock private NotificationService notificationService;

    private KpiSharedCollectionService service;

    private static final Long PROJECT_ID = 201L;
    private static final Long ACTOR_PERSON_ID = 1001L;
    private static final Long ACTOR_GROUP_ID = 10L;
    private static final String PERIOD = "2026-09";
    private static final String TENANT_ID = "tenant-A";

    @BeforeEach
    void setUp() {
        service = new KpiSharedCollectionService(
            kpiRecordMapper, projectMapper, projectMemberMapper, personMapper,
            null, auditLogService, productGroupMapper, systemConfigService,
            notificationService);
    }

    /* ====================== 1. actor 缺失 ====================== */

    @Test
    @DisplayName("[W4-SEC-1] actor 为 null → UNAUTHORIZED（不查 mapper）")
    void listSharedKpis_nullActor_unauthorized() {
        assertThatThrownBy(() -> service.listSharedKpis(null, PROJECT_ID, PERIOD))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);

        // 防御性兜底：未通过 actor 校验前，DB 一律不查
        verify(projectMapper, never()).selectById(any());
        verify(projectMemberMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(kpiRecordMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("[W4-SEC-1b] actor.id 为 null → UNAUTHORIZED")
    void listSharedKpis_actorIdNull_unauthorized() {
        IpdActor actor = new IpdActor(null, "匿名", "MARKET_PM", ACTOR_GROUP_ID);

        assertThatThrownBy(() -> service.listSharedKpis(actor, PROJECT_ID, PERIOD))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.UNAUTHORIZED);
    }

    /* ====================== 2. 项目不存在 ====================== */

    @Test
    @DisplayName("[W4-SEC-2] projectMapper 返回 null → FORBIDDEN（不泄漏存在性）")
    void listSharedKpis_projectNotFound_forbidden() {
        IpdActor actor = new IpdActor(ACTOR_PERSON_ID, "市场PM", "MARKET_PM", ACTOR_GROUP_ID);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.listSharedKpis(actor, PROJECT_ID, PERIOD))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        verify(projectMemberMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(kpiRecordMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    /* ====================== 3. 越项目（非成员）→ MEDIUM IDOR 核心修复 ====================== */

    @Test
    @DisplayName("[W4-SEC-3] actor 不是项目在职成员 → FORBIDDEN（MEDIUM IDOR 修复）")
    void listSharedKpis_notMember_forbidden() {
        IpdActor actor = new IpdActor(ACTOR_PERSON_ID, "市场PM", "MARKET_PM", ACTOR_GROUP_ID);
        Project project = projectWithTenant(TENANT_ID);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        // 无任何匹配成员（既不是 MARKET_PM 也不是 RD_PM，也不是主组组长）
        when(projectMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        assertThatThrownBy(() -> service.listSharedKpis(actor, PROJECT_ID, PERIOD))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        // 越权用户被挡后不再查 KpiRecord
        verify(kpiRecordMapper, never()).selectList(any(LambdaQueryWrapper.class));
    }

    /* ====================== 4. 跨租户 ====================== */

    @Test
    @DisplayName("[W4-SEC-4] requireTenantMatch 跨租户 → FORBIDDEN（多租户拦截器仅做 tenant_id 过滤，此处补 guard）")
    void requireTenantMatch_crossTenant_forbidden() {
        Project project = projectWithTenant("tenant-B");
        // 当前会话租户为 tenant-A（与项目 tenant-B 不同）
        assertThatThrownBy(() -> KpiSharedCollectionService.requireTenantMatch("tenant-A", project))
            .isInstanceOf(IpdBusinessException.class)
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("[W4-SEC-4b] requireTenantMatch 同租户放行")
    void requireTenantMatch_sameTenant_pass() {
        Project project = projectWithTenant(TENANT_ID);
        KpiSharedCollectionService.requireTenantMatch(TENANT_ID, project);
        // 不抛即通过
    }

    @Test
    @DisplayName("[W4-SEC-4c] requireTenantMatch currentTenant=null 放行（租户隔离未启用）")
    void requireTenantMatch_nullTenant_pass() {
        Project project = projectWithTenant("any-tenant");
        KpiSharedCollectionService.requireTenantMatch(null, project);
        // 不抛即通过（与多租户拦截器行为一致）
    }

    /* ====================== 5. 正常在职成员 ====================== */

    @Test
    @DisplayName("[W4-SEC-5] actor 正常在职成员 → 返回 KpiRecord 列表（revision DESC）")
    void listSharedKpis_member_returnsRecords() {
        IpdActor actor = new IpdActor(ACTOR_PERSON_ID, "市场PM", "MARKET_PM", ACTOR_GROUP_ID);
        Project project = projectWithTenant(TENANT_ID);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(projectMemberMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L); // 是成员
        KpiRecord rev2 = buildRecord(11L, PROJECT_ID, 2, "85.50");
        KpiRecord rev1 = buildRecord(13L, PROJECT_ID, 1, "75.00");
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(rev2, rev1));

        List<KpiRecord> result = service.listSharedKpis(actor, PROJECT_ID, PERIOD);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRevision()).isEqualTo(2);
        assertThat(result.get(1).getRevision()).isEqualTo(1);
    }

    /* ====================== 6. SUPER_ADMIN 跨项目访问豁免 ====================== */

    @Test
    @DisplayName("[W4-SEC-6] SUPER_ADMIN 不做 ProjectMember 鉴权（运维观察需要）")
    void listSharedKpis_superAdminBypassMemberCheck() {
        IpdActor actor = new IpdActor(999L, "超管", "SUPER_ADMIN", null);
        Project project = projectWithTenant(TENANT_ID);
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(project);
        when(kpiRecordMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(buildRecord(11L, PROJECT_ID, 1, "90.00")));

        List<KpiRecord> result = service.listSharedKpis(actor, PROJECT_ID, PERIOD);

        assertThat(result).hasSize(1);
        // SUPER_ADMIN 不走 ProjectMember 鉴权
        verify(projectMemberMapper, never()).selectCount(any(LambdaQueryWrapper.class));
    }

    /* ====================== 测试工具 ====================== */

    private Project projectWithTenant(String tenantId) {
        Project p = new Project();
        p.setId(PROJECT_ID);
        p.setTenantId(tenantId);
        p.setStatus("ACTIVE");
        return p;
    }

    private KpiRecord buildRecord(Long id, Long projectId, int revision, String score) {
        KpiRecord r = new KpiRecord();
        r.setId(id);
        r.setProjectId(projectId);
        r.setPersonId(1000L + id);
        r.setKpiType("SHARED");
        r.setPeriod(PERIOD);
        r.setComprehensiveScore(new java.math.BigDecimal(score));
        r.setStatus("FINALIZED");
        r.setRevision(revision);
        r.setSegment("FULL_SHARED");
        return r;
    }
}
