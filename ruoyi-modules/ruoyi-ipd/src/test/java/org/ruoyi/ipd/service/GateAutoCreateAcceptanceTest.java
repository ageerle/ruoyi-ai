package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.GateReviewMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1 / §5.3 HIGH-1.1：G3 评审自动创建入口验收测试。
 *
 * <p>边界：
 * <ol>
 *   <li>合法 gateCode（G1/G2/G3/G4/G5）⇒ 创建成功 + 写 GATE_AUTO_CREATE 审计</li>
 *   <li>非法 gateCode（XYZ/空/null）⇒ 拒</li>
 *   <li>ARCHIVED/SUSPENDED 项目 ⇒ 拒</li>
 *   <li>最近 14 天已创建同 gateCode ⇒ 拒</li>
 *   <li>项目不存在 ⇒ 拒</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class GateAutoCreateAcceptanceTest {

    @Mock private GateReviewMapper gateReviewMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;

    private GateCreationService service;

    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, GateReview.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateCreationService(gateReviewMapper, projectMapper, auditLogService);
    }

    private Project activeProject(long id) {
        return Project.builder().id(id).name("GATE 测试 " + id).status("ACTIVE").mainGroupId(70L).delFlag("0").build();
    }

    @Test
    @DisplayName("AC#1 G3 合法创建 ⇒ 成功 + 审计")
    void createG3_succeeds() {
        when(projectMapper.selectById(700L)).thenReturn(activeProject(700L));
        when(gateReviewMapper.selectCount(any())).thenReturn(0L);
        GateReview r = service.autoCreateGate(700L, "G3", 999L);
        assertThat(r.getGateCode()).isEqualTo("G3");
        assertThat(r.getProjectId()).isEqualTo(700L);
        assertThat(r.getRound()).isEqualTo(1);
        // R11 / A4 修复:决策字段必须 NULL（待决语义），不得预设哨兵值破坏 KeyGateAggregator 锚点。
        // 死路源头:GateCreationService L72 原 .decision("PENDING")，不在 GateReview.decision 值域（APPROVE|REJECT|ABSTAIN），
        // 导致 KeyGateAggregator 的 .isNull(GateReview::getDecision) 永不可命中，工作台 key_gate 卡恒空。
        assertThat(r.getDecision()).as("A4 修复:决策字段保持 NULL").isNull();
        verify(gateReviewMapper, times(1)).insert(any(GateReview.class));
        verify(auditLogService, times(1)).append(any());
    }

    @Test
    @DisplayName("AC#2 非法 gateCode ⇒ 拒")
    void invalidGateCode_rejected() {
        assertThatThrownBy(() -> service.autoCreateGate(700L, "XYZ", 999L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("gateCode");
    }

    @Test
    @DisplayName("AC#3 ARCHIVED 项目 ⇒ 拒")
    void archivedProject_rejected() {
        Project p = activeProject(700L);
        p.setStatus("ARCHIVED");
        when(projectMapper.selectById(700L)).thenReturn(p);
        assertThatThrownBy(() -> service.autoCreateGate(700L, "G3", 999L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("归档");
    }

    @Test
    @DisplayName("AC#4 14 天冷却：最近 14 天已有同 gateCode ⇒ 拒")
    void cooldown_rejected() {
        when(projectMapper.selectById(700L)).thenReturn(activeProject(700L));
        when(gateReviewMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> service.autoCreateGate(700L, "G3", 999L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("14");
    }

    @Test
    @DisplayName("AC#5 项目不存在 ⇒ 拒")
    void missingProject_rejected() {
        when(projectMapper.selectById(999L)).thenReturn(null);
        assertThatThrownBy(() -> service.autoCreateGate(999L, "G3", 999L))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("AC#6 全部 5 gateCode 合法（G1/G2/G3/G4/G5）")
    void allGateCodes_accepted() {
        for (String code : GateCreationService.ALLOWED_GATE_CODES) {
            when(projectMapper.selectById(700L)).thenReturn(activeProject(700L));
            when(gateReviewMapper.selectCount(any())).thenReturn(0L);
            GateReview r = service.autoCreateGate(700L, code, 999L);
            assertThat(r.getGateCode()).isEqualTo(code);
        }
    }
}