package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectScoreRecord;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.dto.ProjectScoreSubmitReq;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.mapper.ProjectScoreRecordMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.vo.ProjectScoreView;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** P3-2.2 绩效归档、规则版本与更正留痕验收。 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P322AcceptanceTest {

    @Mock private ProjectScoreMapper projectScoreMapper;
    @Mock private ProjectScoreRecordMapper recordMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ProjectMemberMapper memberMapper;
    @Mock private PersonMapper personMapper;
    @Mock private ProductGroupMapper groupMapper;
    @Mock private IpdPermission permission;
    @Mock private AuditLogService auditLogService;
    @Mock private SystemConfigService configService;
    @Mock private SystemConfigVersionMapper versionMapper;

    private ProjectScoreArchiveService service;

    @BeforeEach
    void setUp() {
        service = new ProjectScoreArchiveService(
            projectScoreMapper, recordMapper, projectMapper, memberMapper, personMapper,
            groupMapper, permission, configService, versionMapper, auditLogService,
            new com.fasterxml.jackson.databind.ObjectMapper());
    }

    @Test
    @DisplayName("AC-KPI-16c：双 PM 自评与两名组长分别归档")
    void submitScore_appendsImmutableComponent() {
        projectAndMembers();
        when(configService.getValue("kpi.reviewWeights", "{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}"))
            .thenReturn("{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}");
        when(versionMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(permission.requireInternal()).thenReturn(
            new IpdActor(101L, "市场PM", "MARKET_PM", 11L));

        ProjectScoreView view = service.submit(new ProjectScoreSubmitReq(
            1L, 101L, "SELF", new BigDecimal("80"), "首次自评"));

        assertThat(view.selfScore()).isEqualByComparingTo("80.00");
        assertThat(view.versionNo()).isEqualTo(1);
        assertThat(view.ruleVersion()).isEqualTo(1);
        ArgumentCaptor<ProjectScoreRecord> captor = ArgumentCaptor.forClass(ProjectScoreRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo("ARCHIVED");
        assertThat(captor.getValue().getScore()).isEqualByComparingTo("80.00");
        assertThat(captor.getValue().getAuthorId()).isEqualTo(101L);
        verify(auditLogService).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("组长必须是被评 PM 所在产品组 leader，不能跨组")
    void submitScore_leaderIdentityRequired() {
        projectAndMembers();
        IpdActor outsider = new IpdActor(999L, "外组组长", "GROUP_LEADER", 99L);
        when(permission.requireInternal()).thenReturn(outsider);

        assertThatThrownBy(() -> service.submit(new ProjectScoreSubmitReq(
            1L, 101L, "MARKET_LEADER", new BigDecimal("90"), "组长评定")))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("产品组");
        verify(recordMapper, never()).insert(any(ProjectScoreRecord.class));
    }

    @Test
    @DisplayName("更正产生新版本，ARCHIVED 记录不 update 覆盖")
    void submitScore_correctionAppendsVersion() {
        projectAndMembers();
        when(configService.getValue("kpi.reviewWeights", "{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}"))
            .thenReturn("{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}");
        when(versionMapper.selectOne(any())).thenReturn(null);
        when(recordMapper.selectOne(any(LambdaQueryWrapper.class)))
            .thenReturn(ProjectScoreRecord.builder().versionNo(1).build())
            .thenReturn(ProjectScoreRecord.builder().versionNo(2).build());
        when(permission.requireInternal()).thenReturn(
            new IpdActor(101L, "市场PM", "MARKET_PM", 11L));
        ProjectScoreSubmitReq first = new ProjectScoreSubmitReq(1L, 101L, "SELF", new BigDecimal("80"), null);
        ProjectScoreSubmitReq second = new ProjectScoreSubmitReq(1L, 101L, "SELF", new BigDecimal("85"), "更正自评");

        service.submit(first);
        service.submit(second);

        ArgumentCaptor<ProjectScoreRecord> captor = ArgumentCaptor.forClass(ProjectScoreRecord.class);
        verify(recordMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
        assertThat(captor.getAllValues().get(0).getVersionNo()).isEqualTo(2);
        assertThat(captor.getAllValues().get(1).getVersionNo()).isEqualTo(3);
        assertThat(captor.getAllValues()).allMatch(row -> "ARCHIVED".equals(row.getStatus()));
        verify(recordMapper, never()).update(any(ProjectScoreRecord.class), any());
    }

    @Test
    @DisplayName("结算只引用一个共同 versionNo，历史重算结果不漂移")
    void settlement_usesCommonImmutableVersion() {
        projectAndMembers();
        when(recordMapper.selectList(any())).thenReturn(List.of(
            score(1L, 1, "SELF", "80"),
            score(2L, 1, "MARKET_LEADER", "90"),
            score(3L, 1, "RD_LEADER", "85"),
            score(4L, 2, "SELF", "99"),
            score(5L, 2, "MARKET_LEADER", "90"),
            score(6L, 2, "RD_LEADER", "85")));

        ProjectScoreView first = service.settleVersion(1L, 101L);
        ProjectScoreView second = service.settleVersion(1L, 101L);

        assertThat(first.versionNo()).isEqualTo(2);
        assertThat(first.ruleVersion()).isEqualTo(1);
        assertThat(first.weightedScore()).isEqualByComparingTo("89.80");
        assertThat(second).isEqualTo(first);
    }

    @Test
    @DisplayName("组长评定时角色合法且保存规则快照")
    void leaderScore_recordsRuleSnapshot() {
        projectAndMembers();
        when(configService.getValue("kpi.reviewWeights", "{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}"))
            .thenReturn("{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}");
        when(versionMapper.selectOne(any())).thenReturn(
            SystemConfigVersion.builder().version(7).configValue("{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}").build());
        when(recordMapper.selectOne(any())).thenReturn(null);
        when(permission.requireInternal()).thenReturn(
            new IpdActor(901L, "市场组组长", "GROUP_LEADER", 11L));

        service.submit(new ProjectScoreSubmitReq(1L, 101L, "MARKET_LEADER", new BigDecimal("90"), null));

        ArgumentCaptor<ProjectScoreRecord> captor = ArgumentCaptor.forClass(ProjectScoreRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getAuthorRole()).isEqualTo("GROUP_LEADER");
        assertThat(captor.getValue().getRuleVersion()).isEqualTo(8);
        assertThat(captor.getValue().getRuleSnapshot()).contains("marketLeader");
    }

    private void projectAndMembers() {
        lenient().when(projectMapper.selectById(1L)).thenReturn(Project.builder()
            .id(1L).mainGroupId(11L).status("ACTIVE").build());
        lenient().when(memberMapper.selectList(any())).thenReturn(List.of(
            ProjectMember.builder().projectId(1L).personId(101L).role("MARKET_PM").build()));
        lenient().when(personMapper.selectById(101L)).thenReturn(Person.builder()
            .id(101L).personType("MARKET_PM").groupId(11L).build());
        lenient().when(groupMapper.selectById(11L)).thenReturn(ProductGroup.builder()
            .id(11L).leaderPersonId(901L).build());
    }

    private static ProjectScoreRecord score(Long id, int version, String component, String value) {
        return ProjectScoreRecord.builder()
            .id(id).projectId(1L).personId(101L).pmRole("MARKET_PM")
            .componentType(component).score(new BigDecimal(value)).versionNo(version)
            .ruleVersion(1).status("ARCHIVED").build();
    }
}
