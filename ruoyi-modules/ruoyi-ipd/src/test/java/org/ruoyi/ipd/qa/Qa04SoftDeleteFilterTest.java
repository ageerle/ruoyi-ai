package org.ruoyi.ipd.qa;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.service.AuditLogService;
import org.ruoyi.ipd.service.StageActionService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * QA-04 验收线 2：stage_actions/deliverables 查询链软删过滤回归（BR-IPD-03/05，G-02 禁物理 DELETE）。
 *
 * 断言方式：捕获 MyBatis-Plus LambdaQueryWrapper，经 getSqlSegment() 还原 WHERE 片段，
 * 验证 del_flag='0' 确实进入查询链。TableInfo 预初始化以支撑 lambda 列解析。
 *
 * 现状说明（见 QA-04 报告 DEF-04）：stage_actions 自身查询链无 delFlag 过滤——
 * 实体未映射 del_flag 且当前无软删入口，属纵深防御缺口，不在本测试判定范围。
 */
@Tag("dev")
class Qa04SoftDeleteFilterTest {

    private static final MybatisConfiguration MP_CONFIG = new MybatisConfiguration();

    private StageActionMapper stageActionMapper;
    private DeliverableMapper deliverableMapper;
    private ProjectStageMapper projectStageMapper;
    private ProjectMapper projectMapper;
    private AuditLogService auditLogService;
    private StageActionService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(MP_CONFIG, "");
        for (Class<?> entity : List.of(StageAction.class, Deliverable.class, ProjectStage.class, Project.class)) {
            TableInfoHelper.initTableInfo(assistant, entity);
        }
    }

    @BeforeEach
    void setUp() {
        stageActionMapper = Mockito.mock(StageActionMapper.class);
        deliverableMapper = Mockito.mock(DeliverableMapper.class);
        projectStageMapper = Mockito.mock(ProjectStageMapper.class);
        projectMapper = Mockito.mock(ProjectMapper.class);
        auditLogService = Mockito.mock(AuditLogService.class);
        service = new StageActionService(stageActionMapper, deliverableMapper, auditLogService,
            projectStageMapper, projectMapper);
    }

    /** 构造 DEEP 动作（含 version=0，映射真实实体）。 */
    private static StageAction deepAction(Long id, Long projectId) {
        StageAction a = StageAction.builder()
            .id(id).projectId(projectId).stageId(2000L)
            .actionCode("C01").actionName("市场调研")
            .ownerRole("MARKET_PM").depth("DEEP")
            .status("NOT_STARTED").isBlocking("1").isBioFeature("0")
            .build();
        a.setVersion(0);
        return a;
    }

    private static Project activeProject(Long id) {
        Project p = Project.builder().id(id).name("QA-04").templateType("HARDWARE")
            .level("A").currentStage("CONCEPT").status("ACTIVE").build();
        p.setDelFlag("0");
        return p;
    }

    @Test
    @SuppressWarnings("unchecked")
    void deepDoneRequiresUndeletedDeliverableInQueryChain() {
        StageAction a = deepAction(881001L, 881100L);
        when(stageActionMapper.selectById(881001L)).thenReturn(a);
        when(deliverableMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
        when(projectMapper.selectById(881100L)).thenReturn(activeProject(881100L));
        when(stageActionMapper.updateById(any(StageAction.class))).thenReturn(1);

        service.transit(881001L, "DONE", "回归", "42");

        ArgumentCaptor<LambdaQueryWrapper<Deliverable>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        Mockito.verify(deliverableMapper).selectCount(captor.capture());
        LambdaQueryWrapper<Deliverable> w = captor.getValue();
        assertThat(w.getSqlSegment()).contains("del_flag");
        assertThat(w.getParamNameValuePairs().values()).contains("0");
    }

    @Test
    @SuppressWarnings("unchecked")
    void deepDoneRejectedWithoutDeliverable() {
        StageAction deep = deepAction(1L, 2L);
        when(stageActionMapper.selectById(1L)).thenReturn(deep);
        when(deliverableMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectById(2L)).thenReturn(activeProject(2L));

        assertThatThrownBy(() -> service.transit(1L, "DONE", "回归", "42"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-03");
    }

    @Test
    @SuppressWarnings("unchecked")
    void conceptStageResolutionFiltersSoftDeletedStages() {
        StageAction bio = deepAction(881001L, 881100L);
        bio.setActionCode("D01");
        when(stageActionMapper.selectById(881001L)).thenReturn(bio);
        // 涉生物判定 1 次、C12 存在性判定 1 次
        when(stageActionMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(1L)
            .thenReturn(0L);
        ProjectStage stage = ProjectStage.builder().id(881200L).projectId(881100L).stageCode("CONCEPT").build();
        when(projectStageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(stage));
        when(projectMapper.selectById(881100L)).thenReturn(activeProject(881100L));
        // mock insert 无主键回填，显式回填以通过 C12 补挂的主键断言
        when(stageActionMapper.insert(any(StageAction.class))).thenAnswer(inv -> {
            ((StageAction) inv.getArgument(0)).setId(881300L);
            return 1;
        });

        int mounted = service.ensureBioComplianceMount(881100L);

        assertThat(mounted).isEqualTo(1);
        ArgumentCaptor<LambdaQueryWrapper<ProjectStage>> captor =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        Mockito.verify(projectStageMapper).selectList(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("del_flag");
    }

    @Test
    void transitRejectedWhenProjectSoftDeleted() {
        Project deleted = activeProject(881100L);
        deleted.setDelFlag("1");
        when(stageActionMapper.selectById(881001L)).thenReturn(deepAction(881001L, 881100L));
        when(projectMapper.selectById(881100L)).thenReturn(deleted);

        assertThatThrownBy(() -> service.transit(881001L, "IN_PROGRESS", "回归", "42"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("项目不存在");
    }
}
