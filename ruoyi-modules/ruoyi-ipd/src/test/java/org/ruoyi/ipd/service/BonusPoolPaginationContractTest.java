package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.BonusPool;
import org.ruoyi.ipd.mapper.BonusPoolMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-P0-2 验收测试：BonusPoolService.pageByProject 物理分页契约。
 *
 * <p>核心契约：
 * <ul>
 *   <li>{@code projectId} 必填：null 即抛 IpdBusinessException(PARAM_INVALID)，不进 mapper</li>
 *   <li>{@code pageSize} 上限 200（硬约束，防 DoS 滥用）—— Service 层二次防御</li>
 *   <li>{@code pageNo} 下限 1（负数兜底到 1）</li>
 *   <li>排序与软删语义与 {@code listByProject} 完全一致（{@code calculatedAt} DESC）</li>
 *   <li>旧 {@code listByProject} 仍走 {@code selectList}（兼容回归 web-antd 旧调用方）</li>
 * </ul>
 *
 * <p>本测试与 {@link BonusPoolServiceTest}（既有 16 测试）分离，按 BidResponsePaginationTest
 * 风格独立成类，契约边界更清晰；不影响既有测试。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("PERF-P0-2 BonusPoolService.pageByProject 物理分页契约")
class BonusPoolPaginationContractTest {

    @Mock private BonusPoolMapper bonusPoolMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private org.ruoyi.ipd.service.StateMachineGuard stateMachineGuard;

    private BonusPoolService service;

    @BeforeEach
    void setUp() {
        service = new BonusPoolService(bonusPoolMapper, projectMapper);
        service.setAuditLogService(auditLogService);
        service.setStateMachineGuard(stateMachineGuard);
    }

    /** 一条奖金池记录（最小必要字段）。 */
    private BonusPool row(Long id, Long projectId) {
        BonusPool p = new BonusPool();
        p.setId(id);
        p.setProjectId(projectId);
        return p;
    }

    /**
     * BN-1：默认 3 行数据，selectPage 返 IPage(records=3行, total=3)。
     * 验证 pageByProject 走物理分页，records 与 wrapper 过滤一致。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-1 正常分页 → IPage 含 total=3 + records 顺序按 calculatedAt DESC")
    void pageByProject_normal_returnsIPage() {
        Page<BonusPool> inputPage = new Page<>(1, 20);
        when(bonusPoolMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(inputPage.setRecords(List.of(row(101L, 200L), row(102L, 200L), row(103L, 200L)))
                .setTotal(3L));

        IPage<BonusPool> result = service.pageByProject(200L, inputPage);

        assertThat(result.getRecords()).hasSize(3);
        assertThat(result.getTotal()).isEqualTo(3L);
        assertThat(result.getCurrent()).isEqualTo(1L);
        assertThat(result.getSize()).isEqualTo(20L);
    }

    /**
     * BN-2：大项目 1000 行 → records 仅当前页 20 条，pages=50。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-2 大数据量 1000+ 行 → records 仅当前页 20 条，total=1000")
    void pageByProject_largeDataset_returnsSinglePageOf20() {
        List<BonusPool> records = new ArrayList<>();
        for (long i = 1; i <= 20; i++) records.add(row(i, 200L));
        Page<BonusPool> inputPage = new Page<>(1, 20);
        when(bonusPoolMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenReturn(inputPage.setRecords(records).setTotal(1000L));

        IPage<BonusPool> result = service.pageByProject(200L, inputPage);

        assertThat(result.getRecords()).hasSize(20);
        assertThat(result.getTotal()).isEqualTo(1000L);
        assertThat(result.getPages()).isEqualTo(50L); // 1000 / 20
    }

    /**
     * BN-3：projectId=null 必须立刻抛 IpdBusinessException(PARAM_INVALID)，
     * 不进 mapper.selectPage（避免 NPE/IllegalArgumentException 在 DB 层才暴露）。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-3 projectId=null → IpdBusinessException(PARAM_INVALID)，不进 mapper")
    void pageByProject_nullProjectId_throwsParamInvalid() {
        assertThatThrownBy(() -> service.pageByProject(null, new Page<>(1, 20)))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("projectId 不能为空")
            .extracting("errorCode").isEqualTo(ApiV1ErrorCode.PARAM_INVALID);
        // 防 mapper 被调：null 进 wrapper 会变成 IS NULL 全表扫
        verify(bonusPoolMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
    }

    /**
     * BN-4：pageSize=500 超限 → Service 层 Math.min 兜底到 200（即使 Controller 漏防）。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-4 pageSize=500 超限 → Service 兜底到 200")
    void pageByProject_oversizedPageSize_cappedToMax() {
        Page<BonusPool> inputPage = new Page<>(1, 500);
        Page<BonusPool> capturedPage = new Page<>(1, 20);
        when(bonusPoolMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenAnswer(inv -> {
                Page<BonusPool> p = inv.getArgument(0);
                capturedPage.setCurrent(p.getCurrent());
                capturedPage.setSize(p.getSize());
                return capturedPage.setRecords(Collections.emptyList()).setTotal(0L);
            });

        service.pageByProject(200L, inputPage);

        ArgumentCaptor<Page<BonusPool>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bonusPoolMapper, times(1)).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(200L);
    }

    /**
     * BN-5：pageNo=-1 → Service 层 Math.max 兜底到 1。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-5 pageNo=-1 负数 → Service 兜底到 1")
    void pageByProject_negativePageNo_cappedToMin() {
        Page<BonusPool> inputPage = new Page<>(-1, 20);
        Page<BonusPool> capturedPage = new Page<>(1, 20);
        when(bonusPoolMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenAnswer(inv -> {
                Page<BonusPool> p = inv.getArgument(0);
                capturedPage.setCurrent(p.getCurrent());
                return capturedPage.setRecords(Collections.emptyList()).setTotal(0L);
            });

        service.pageByProject(200L, inputPage);

        ArgumentCaptor<Page<BonusPool>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bonusPoolMapper, times(1)).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
    }

    /**
     * BN-6：page 入参 null（直接调用场景）→ Service 兜底默认 Page(1, 20)。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-6 page 入参 null → Service 兜底默认 Page(1,20)，不抛 NPE")
    void pageByProject_nullPage_defaultsToDefaultSize() {
        Page<BonusPool> capturedPage = new Page<>(1, 20);
        when(bonusPoolMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
            .thenAnswer(inv -> {
                Page<BonusPool> p = inv.getArgument(0);
                capturedPage.setCurrent(p.getCurrent());
                capturedPage.setSize(p.getSize());
                return capturedPage.setRecords(Collections.emptyList()).setTotal(0L);
            });

        IPage<BonusPool> result = service.pageByProject(200L, null);

        assertThat(result).isNotNull();
        ArgumentCaptor<Page<BonusPool>> pageCaptor = ArgumentCaptor.forClass(Page.class);
        verify(bonusPoolMapper, times(1)).selectPage(pageCaptor.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCaptor.getValue().getCurrent()).isEqualTo(1L);
        assertThat(pageCaptor.getValue().getSize()).isEqualTo(20L);
    }

    /**
     * BN-7（兼容回归）：listByProject 仍走 selectList，不走 selectPage。
     */
    @Test
    @DisplayName("PERF-P0-2 BN-7（兼容回归）listByProject 仍走 selectList，不走 selectPage")
    void listByProject_stillUsesSelectList_compatRegression() {
        when(bonusPoolMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());

        List<BonusPool> result = service.listByProject(200L);

        assertThat(result).isEmpty();
        verify(bonusPoolMapper, never()).selectPage(any(Page.class), any(LambdaQueryWrapper.class));
        verify(bonusPoolMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }
}
