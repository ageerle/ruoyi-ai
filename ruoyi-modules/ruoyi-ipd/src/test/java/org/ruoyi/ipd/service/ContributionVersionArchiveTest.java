package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.ipd.domain.Contribution;
import org.ruoyi.ipd.domain.ContributionVersion;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ContributionMapper;
import org.ruoyi.ipd.mapper.ContributionVersionMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.common.IpdBusinessException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * BR-INC-09「归档版本可追溯」验收：APPROVE 确认归档快照、版次递增、
 * REJECT 不归档、versions 列表降序（2026-09-08 前端契约对照轮补交）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContributionVersionArchiveTest {

    @Mock private ContributionMapper contributionMapper;
    @Mock private ContributionVersionMapper versionMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission ipdPermission;
    @Mock private ProductGroupMapper productGroupMapper;

    private ContributionService service;

    private static final Long GROUP_LEADER_ID = 2001L;
    private static final Long PROJECT_ID = 500L;

    @BeforeEach
    void setUp() {
        service = new ContributionService(contributionMapper, versionMapper, projectMapper,
            productGroupMapper, auditLogService, ipdPermission);
    }

    private IpdActor leaderActor() {
        return new IpdActor(GROUP_LEADER_ID, "组长A", "GROUP_LEADER", 10L);
    }

    private Project lifecycleProject() {
        return Project.builder().id(PROJECT_ID).name("PRJ").code("PRJ-001")
            .status("LIFECYCLE").delFlag("0").build();
    }

    private Contribution submitted() {
        return Contribution.builder()
            .id(999L).projectId(PROJECT_ID).status(Contribution.ST_SUBMITTED)
            .marketShare(new BigDecimal("0.55")).rdShare(new BigDecimal("0.45"))
            .tierCoefficient(new BigDecimal("83.00"))
            .delFlag("0").build();
    }

    @Test
    @DisplayName("confirm APPROVE：已有版本 v1 时归档为 v2（versionNo 递增不覆盖）")
    void confirm_approve_incrementsVersionNo() {
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(leaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(contributionMapper.selectOne(any())).thenReturn(submitted());
        when(versionMapper.selectList(any())).thenReturn(List.of(
            ContributionVersion.builder().id(1L).projectId(PROJECT_ID).versionNo(1).build()));

        service.confirm(PROJECT_ID, "APPROVE", "二次确认");

        verify(versionMapper).insert(any(ContributionVersion.class));
        verify(versionMapper).insert(org.mockito.ArgumentMatchers
            .<ContributionVersion>argThat(cv -> cv.getVersionNo() == 2));
    }

    @Test
    @DisplayName("confirm REJECT：退回 DRAFT 且不归档（无确认即无版本）")
    void confirm_reject_doesNotArchive() {
        when(ipdPermission.requireLeaderOrAdmin()).thenReturn(leaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(contributionMapper.selectOne(any())).thenReturn(submitted());

        service.confirm(PROJECT_ID, "REJECT", "比例重议");

        verify(versionMapper, never()).insert(any(ContributionVersion.class));
    }

    @Test
    @DisplayName("listVersions：按 versionNo 降序，最新确认在前")
    void listVersions_descendingOrder() {
        when(ipdPermission.requireInternal()).thenReturn(leaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(lifecycleProject());
        when(versionMapper.selectList(any())).thenReturn(List.of(
            ContributionVersion.builder().id(2L).projectId(PROJECT_ID).versionNo(2)
                .status("CONFIRMED").marketShare(new BigDecimal("0.55")).build(),
            ContributionVersion.builder().id(1L).projectId(PROJECT_ID).versionNo(1)
                .status("CONFIRMED").marketShare(new BigDecimal("0.60")).build()));

        List<org.ruoyi.ipd.dto.ContributionVersionView> views = service.listVersions(PROJECT_ID);

        assertThat(views).hasSize(2);
        assertThat(views.get(0).versionNo()).isEqualTo(2);
        assertThat(views.get(1).versionNo()).isEqualTo(1);
        assertThat(views.get(0).status()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("listVersions：项目不存在 → NOT_FOUND")
    void listVersions_projectMissing() {
        when(ipdPermission.requireInternal()).thenReturn(leaderActor());
        when(projectMapper.selectById(PROJECT_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.listVersions(PROJECT_ID))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("项目不存在");
    }
}
