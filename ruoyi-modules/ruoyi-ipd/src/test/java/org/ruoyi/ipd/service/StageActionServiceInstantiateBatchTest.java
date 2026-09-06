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
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-03：StageActionService.instantiate 批量插入单测
 *
 * <p>修复前：循环 N 次 selectCount + N 次 insert（69 动作 = 138 IO）
 * <br>修复后：1 次 selectList 取已存在 codes + 1 次 insertBatch（69 动作 = 2 IO）
 *
 * <p>验证：用 CONCEPT 阶段 12 个动作实例化全新项目，断言：
 * <ul>
 *   <li>selectList 仅 1 次</li>
 *   <li>insertBatch 仅 1 次（12 条 StageAction 一次批量插入）</li>
 *   <li>selectCount 与单条 insert 完全不调</li>
 *   <li>返回值 = 12</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class StageActionServiceInstantiateBatchTest {

    @Mock
    private StageActionMapper stageActionMapper;
    @Mock
    private DeliverableMapper deliverableMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private org.ruoyi.ipd.mapper.ProjectStageMapper projectStageMapper;
    @Mock
    private org.ruoyi.ipd.mapper.ProjectMapper projectMapper;

    private StageActionService service;

    @BeforeEach
    void setUp() {
        service = new StageActionService(stageActionMapper, deliverableMapper, auditLogService,
            projectStageMapper, projectMapper);
    }

    @Test
    @DisplayName("instantiate CONCEPT 阶段（12 动作）全新项目：1 次 selectList + 1 次 insertBatch")
    void instantiate_concept12_freshProject_usesBatch() {
        // 项目下还没有任何动作
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        lenient().when(stageActionMapper.insertBatch(any(java.util.Collection.class), any(Integer.class))).thenReturn(true);

        int created = service.instantiate(1L, 10L, "CONCEPT");

        // 12 个 CONCEPT 动作全部新建
        assertThat(created).isEqualTo(12);

        // 必须 1 次 selectList（取已存在 codes）—— 关键优化点
        verify(stageActionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        // 必须 1 次 insertBatch（12 条 StageAction 一起插入）—— 关键优化点
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<StageAction>> cap = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(stageActionMapper, times(1)).insertBatch(cap.capture(), any(Integer.class));
        assertThat(cap.getValue()).as("batch must carry 12 actions").hasSize(12);

        // 必须不调 selectCount 和单条 insert（关键：避免 N+1）
        verify(stageActionMapper, never()).selectCount(any(LambdaQueryWrapper.class));
        verify(stageActionMapper, never()).insert(any(StageAction.class));
    }

    @Test
    @DisplayName("instantiate CONCEPT 项目已存在部分动作：剩余批量插入")
    void instantiate_concept12_partialExists_skipsAndBatchRest() {
        // 项目下已有 3 个 CONCEPT 动作（C01/C02/C03）
        StageAction existing1 = StageAction.builder().id(100L).projectId(1L).actionCode("C01").build();
        StageAction existing2 = StageAction.builder().id(101L).projectId(1L).actionCode("C02").build();
        StageAction existing3 = StageAction.builder().id(102L).projectId(1L).actionCode("C03").build();
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(existing1, existing2, existing3));
        lenient().when(stageActionMapper.insertBatch(any(java.util.Collection.class), any(Integer.class))).thenReturn(true);

        int created = service.instantiate(1L, 10L, "CONCEPT");

        // 已存在 3 个，新建 12-3=9 个
        assertThat(created).isEqualTo(9);

        verify(stageActionMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Collection<StageAction>> cap = ArgumentCaptor.forClass(java.util.Collection.class);
        verify(stageActionMapper, times(1)).insertBatch(cap.capture(), any(Integer.class));
        assertThat((java.util.Collection<StageAction>) cap.getValue()).as("batch carries only the 9 new actions, not existing 3")
            .hasSize(9)
            .noneMatch(a -> a.getActionCode().equals("C01") || a.getActionCode().equals("C02") || a.getActionCode().equals("C03"));
    }

    @Test
    @DisplayName("instantiate CONCEPT 项目已全部存在：0 新建 + 0 insertBatch")
    void instantiate_concept12_allExists_noInsert() {
        // 项目下已有全部 12 个 CONCEPT 动作
        List<StageAction> all12 = ActionCatalog.byStage("CONCEPT").stream()
            .map(def -> StageAction.builder().projectId(1L).actionCode(def.code()).build())
            .toList();
        when(stageActionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(all12);

        int created = service.instantiate(1L, 10L, "CONCEPT");

        assertThat(created).isZero();
        verify(stageActionMapper, never()).insertBatch(any(java.util.Collection.class), any(Integer.class));
    }
}
