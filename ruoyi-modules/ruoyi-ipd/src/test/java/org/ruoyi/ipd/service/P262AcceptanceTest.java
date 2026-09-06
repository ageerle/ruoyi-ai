package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
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
import org.ruoyi.ipd.domain.Requirement;
import org.ruoyi.ipd.domain.RequirementChange;
import org.ruoyi.ipd.mapper.RequirementChangeMapper;
import org.ruoyi.ipd.mapper.RequirementMapper;
import org.ruoyi.ipd.security.IpdActor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-6.2 需求变更双签回写与阶段门禁验收测试
 * AC: AC-GATE-11（未闭环拒绝跳阶）, AC-GATE-12（双签通过回写需求池）, AC-KPI-14（变更率自动统计）
 * BR: BR-GATE-07
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P262AcceptanceTest {

    @Mock private RequirementChangeMapper requirementChangeMapper;
    @Mock private RequirementMapper requirementMapper;
    @Mock private AuditLogService auditLogService;

    @InjectMocks private RequirementChangeService requirementChangeService;

    private static final Long MARKET_PM = 300L;
    private static final Long RD_PM = 200L;
    private static final Long CREATOR = 350L;
    private static final Long PROJECT_ID = 100L;
    private static final Long REQUIREMENT_ID = 500L;

    private static final IpdActor MARKET = new IpdActor(MARKET_PM, "市场经理", "MARKET_PM", 10L);
    private static final IpdActor RD = new IpdActor(RD_PM, "研发经理", "RD_PM", 10L);
    private static final IpdActor CREATOR_ACTOR = new IpdActor(CREATOR, "提交人", "MARKET_PM", 10L);

    @BeforeAll
    static void initTableInfo() {
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p262-rc"),
            RequirementChange.class);
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(
            new MapperBuilderAssistant(new MybatisConfiguration(), "ipd-p262-req"),
            Requirement.class);
    }

    private RequirementChange existingPending(String signatures) {
        RequirementChange rc = RequirementChange.builder()
            .id(50L)
            .requirementId(REQUIREMENT_ID)
            .projectId(PROJECT_ID)
            .changeType("SCOPE_EXPAND")
            .reason("范围扩大")
            .beforeSnapshot("{\"scope\":\"A\"}")
            .afterSnapshot("{\"scope\":\"B\"}")
            .status(RequirementChangeService.STATUS_PENDING_SIGN)
            .signatures(signatures)
            .build();
        rc.setCreateBy(CREATOR);
        return rc;
    }

    @Test
    @DisplayName("sign_双APPROVE_回写需求池状态为ADOPTED")
    void sign_dualApprove_writeBack() {
        RequirementChange change = existingPending("MARKET_PM=APPROVE");
        Requirement requirement = Requirement.builder()
            .id(REQUIREMENT_ID).projectId(PROJECT_ID).status("SUBMITTED").build();
        when(requirementChangeMapper.selectById(50L)).thenReturn(change);
        when(requirementMapper.selectById(REQUIREMENT_ID)).thenReturn(requirement);

        RequirementChange out = requirementChangeService.sign(50L, "APPROVE", "ok", RD);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_APPROVED);
        // 回写：requirement status -> ADOPTED
        ArgumentCaptor<Requirement> reqCap = ArgumentCaptor.forClass(Requirement.class);
        verify(requirementMapper, times(1)).updateById(reqCap.capture());
        assertThat(reqCap.getValue().getStatus()).isEqualTo("ADOPTED");
    }

    @Test
    @DisplayName("sign_任一REJECT_不触发需求池回写")
    void sign_reject_noWriteBack() {
        RequirementChange change = existingPending("MARKET_PM=APPROVE");
        when(requirementChangeMapper.selectById(51L)).thenReturn(change);

        RequirementChange out = requirementChangeService.sign(51L, "REJECT", "成本过高", RD);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_REJECTED);
        // REJECT 不应回写
        verify(requirementMapper, never()).updateById(any(Requirement.class));
    }

    @Test
    @DisplayName("sign_双APPROVE_但需求不存在_不抛异常_仅审计留痕")
    void sign_dualApprove_requirementMissing() {
        RequirementChange change = existingPending("MARKET_PM=APPROVE");
        change.setId(52L);
        when(requirementChangeMapper.selectById(52L)).thenReturn(change);
        when(requirementMapper.selectById(REQUIREMENT_ID)).thenReturn(null);

        // 不应抛，回写失败有审计兜底
        RequirementChange out = requirementChangeService.sign(52L, "APPROVE", "ok", RD);

        assertThat(out.getStatus()).isEqualTo(RequirementChangeService.STATUS_APPROVED);
        verify(requirementMapper, never()).updateById(any(Requirement.class));
    }

    @Test
    @DisplayName("countOpenByProject_返回未闭环数_供阶段门禁拦截")
    void countOpen_returnsN() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(3L);
        assertThat(requirementChangeService.countOpenByProject(PROJECT_ID)).isEqualTo(3);
    }

    @Test
    @DisplayName("countOpenByProject_projectId为null返回0_不抛")
    void countOpen_nullSafe() {
        assertThat(requirementChangeService.countOpenByProject(null)).isEqualTo(0);
    }

    @Test
    @DisplayName("kpiChangeRate_变更率_自动统计无需手工填")
    void kpi_changeRate() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(3L);
        when(requirementMapper.selectCount(any())).thenReturn(10L);
        // 3/10 = 0.3
        assertThat(requirementChangeService.kpiChangeRate()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("kpiChangeRate_无需求总数时返回0")
    void kpi_zeroRequirements() {
        when(requirementMapper.selectCount(any())).thenReturn(0L);
        assertThat(requirementChangeService.kpiChangeRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("kpiChangeRate_无变更单时返回0")
    void kpi_zeroChanges() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(0L);
        when(requirementMapper.selectCount(any())).thenReturn(20L);
        assertThat(requirementChangeService.kpiChangeRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("kpiChangeRate_四舍五入到4位小数")
    void kpi_rounding() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(1L);
        when(requirementMapper.selectCount(any())).thenReturn(3L);
        // 1/3 = 0.3333...
        assertThat(requirementChangeService.kpiChangeRate()).isEqualTo(0.3333);
    }

    @Test
    @DisplayName("hasOpenChange_countOpen_两接口语义一致")
    void hasOpen_countOpen_consistent() {
        when(requirementChangeMapper.selectCount(any())).thenReturn(2L);
        assertThat(requirementChangeService.hasOpenChange(PROJECT_ID)).isTrue();
        assertThat(requirementChangeService.countOpenByProject(PROJECT_ID)).isEqualTo(2);
    }
}
