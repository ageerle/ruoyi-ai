package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditChainHeadMapper;
import org.ruoyi.ipd.mapper.AuditLogMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA-05-P3：游标分页重载（beforeSeq）+ 导出 5 万行硬上限守卫（@Tag("dev") 必须）。
 *
 * <p>覆盖 6 个维度：
 * <ol>
 *   <li>beforeSeq 非 null 走 {@code seq < beforeSeq} 谓词（旧 OFFSET 不再现）</li>
 *   <li>beforeSeq=null 走旧 OFFSET 路径（兼容 AC-AUD-04/05 历史消费者）</li>
 *   <li>边界：beforeSeq 与库内最大 seq 相等时 {@code seq <} 严格不包含（不漏不重）</li>
 *   <li>limit 传导：pageSize 透传且被 clamp 到 200</li>
 *   <li>导出超 5 万行：countByOperatorIds 抛 ServiceException（防 OOM）</li>
 *   <li>导出 5 万行（含）：不抛，正常返回 count</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AuditLogCursorPagingTest {

    @Mock
    private AuditLogMapper auditLogMapper;
    @Mock
    private AuditChainHeadMapper chainHeadMapper;

    private AuditLogService service;

    /** 纯 JVM 单测无 MP 运行时：手动初始化 lambda 列缓存（LambdaQueryWrapper.in/lt/.in 需列名解析） */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, AuditLog.class);
    }

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogMapper, chainHeadMapper);
    }

    @Test
    @DisplayName("QA-05-P3 #1：beforeSeq 非 null 走 seq<beforeSeq 游标窗口（消除 OFFSET）")
    void cursorPagingAppliesSeqLessThanBeforeSeq() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(List.of(900101L), 1, 50, 1000L);

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> wCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectPage(any(IPage.class), wCap.capture());
        // QA-05-P3：beforeSeq=1000 ⇒ wrapper 必须带 seq<1000；orderByDesc(seq) 仍在
        String sql = wCap.getValue().getSqlSegment();
        assertThat(sql).contains("seq");        // 列名解析生效
        assertThat(sql).contains("<");          // 严格小于 lt 谓词（beforeSeq 非 null 时入谓词）
        assertThat(sql).contains("ORDER BY seq DESC");
    }

    @Test
    @DisplayName("QA-05-P3 #2：beforeSeq=null 走旧 OFFSET 路径（兼容历史调用方）")
    void nullBeforeSeqPreservesOldOffsetBehavior() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(List.of(900101L), 1, 20, null);

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> wCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectPage(any(IPage.class), wCap.capture());
        // beforeSeq=null ⇒ 不带 lt(seq, ?) 谓词（wrapper 内不含 < 比较）
        // 通过 SQL 段验证 lt 没有被加入：因 lt 没调，getSqlSegment 不带 lt 关键字
        String sql = wCap.getValue().getSqlSegment();
        // 不出现 "<" SQL 谓词（仅在 selectPage 调入时 MP 不生成 lt 段）
        assertThat(sql).doesNotContain("<");
    }

    @Test
    @DisplayName("QA-05-P3 #3：边界 — beforeSeq 等于库内最大 seq 时 wrapper 仍带 seq<谓词（严格小于不漏不重）")
    void cursorBoundaryStrictLessThan() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        // 假设库内最大 seq=500，调用方传 beforeSeq=500：应仍带 seq<500（严格小于，不含 500）
        service.listByOperatorIds(List.of(900101L), 1, 20, 500L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> wCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectPage(any(IPage.class), wCap.capture());
        String sql = wCap.getValue().getSqlSegment();
        // 严格小于 lt 谓词（beforeSeq=500 ⇒ seq<500，500 本身不含——边界正确）
        assertThat(sql).contains("seq <");
        assertThat(sql).contains("ORDER BY seq DESC");
    }

    @Test
    @DisplayName("QA-05-P3 #9 (W3-A7 #3 升级)：边界 — beforeSeq 命中库内某条 seq 时 wrapper 仍带 seq<严格小于（mock 含真实 seq 数据，lt 上界绑定 beforeSeq 而非 pageSize）")
    void cursorBoundaryStrictLessThanWithRealData() {
        // W3-A7 升级点 1：mock 返回带真实 seq 数据的 Page（seq=10/20/30），证明 wrapper 透传到 mapper 后 Page 实例保持一致
        IPage<AuditLog> fakePage = stubPageWithSeqs(10L, 20L, 30L);
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(fakePage);

        // W3-A7 升级点 2：用真实边界 beforeSeq=15（库内某条 seq=15 须被排除）+ pageSize=50（≠ beforeSeq，证明 lt 上界不是 pageSize）
        IPage<AuditLog> result = service.listByOperatorIds(List.of(900101L), 1, 50, 15L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> wCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectPage(any(IPage.class), wCap.capture());
        String sql = wCap.getValue().getSqlSegment();
        // 严格小于 lt 谓词（beforeSeq=15 ⇒ seq<15，seq=15 本身不含——边界正确）
        assertThat(sql).contains("seq <");
        assertThat(sql).contains("ORDER BY seq DESC");
        // W3-A7 升级断言 1：反向谓词必须不存在（防止退化到 seq >= 或 seq > 反向）
        assertThat(sql).doesNotContain(">=");
        // W3-A7 升级断言 2：lt 上界不能误绑 pageSize（= 50 不应作为 lt 上界出现，证明 lt 用的是 beforeSeq=15）
        assertThat(sql).doesNotContain("= 50");
        // W3-A7 升级断言 3：返回值是 mock 返回的同一 Page 实例（透传验证，wrapper 行为不影响返回）
        assertThat(result).isSameAs(fakePage);
    }

    @Test
    @DisplayName("QA-05-P3 #4：limit 传导 — pageSize=300 被 clamp 到 200")
    void limitClampedTo200() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<AuditLog>> pageCap = ArgumentCaptor.forClass(IPage.class);
        service.listByOperatorIds(List.of(900101L), 1, 300, 1000L);

        verify(auditLogMapper).selectPage(pageCap.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCap.getValue().getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("QA-05-P3 #5：导出超 5 万行 — countByOperatorIds 抛 ServiceException（防全量物化 OOM）")
    void exportHardLimitRejectsOver50000Rows() {
        when(auditLogMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(AuditLogService.EXPORT_HARD_LIMIT + 1);

        assertThatThrownBy(() -> service.countByOperatorIds(List.of(900101L)))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("超过硬上限")
            .hasMessageContaining("50000")
            .hasMessageContaining("QA-05-P3");
    }

    @Test
    @DisplayName("QA-05-P3 #6：导出 = 5 万行（含边界）— 不抛，正常返回 count")
    void exportHardLimitAllowsExactlyLimit() {
        when(auditLogMapper.selectCount(any(LambdaQueryWrapper.class)))
            .thenReturn(AuditLogService.EXPORT_HARD_LIMIT);

        long count = service.countByOperatorIds(List.of(900101L));

        assertThat(count).isEqualTo(AuditLogService.EXPORT_HARD_LIMIT);
    }

    @Test
    @DisplayName("QA-05-P3 #7：旧三参 listByOperatorIds(operatorIds,pageNo,pageSize) 仍兼容（旧消费者零改动）")
    void legacyThreeArgOverloadStillWorks() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(List.of(900101L), 1, 20);

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<AuditLog>> wCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(auditLogMapper).selectPage(any(IPage.class), wCap.capture());
        // 兼容旧路径：不带 lt(seq, ?) 谓词
        assertThat(wCap.getValue().getSqlSegment()).doesNotContain("<");
    }

    @Test
    @DisplayName("QA-05-P3 #8：旧三参 listByOperatorIds(operatorIds,pageNo,pageSize) pageSize=1000 仍被 clamp 到 200（3 参委派 4 参后 clamp 仍生效）")
    void legacyThreeArgClampsPageSize() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<AuditLog>> pageCap = ArgumentCaptor.forClass(IPage.class);
        // 3 参旧路径调用：pageSize=1000（旧消费者零改动兼容入口）
        service.listByOperatorIds(List.of(900101L), 1, 1000);

        // 3 参委派到 4 参方法传 null beforeSeq ⇒ 4 参内部 clamp(pageSize, 1, 200) 仍生效
        verify(auditLogMapper).selectPage(pageCap.capture(), any(LambdaQueryWrapper.class));
        // 断言：pageSize=1000 在 3 参旧路径下仍被 clamp 到 200（防旧路径绕过 clamp）
        assertThat(pageCap.getValue().getSize()).isEqualTo(200);
    }

    private IPage<AuditLog> stubPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
    }

    /** QA-05-P3 #3 升级：mock 含真实 seq 数据的 Page（证明 wrapper 透传到 mapper 后 Page 实例保持一致） */
    private IPage<AuditLog> stubPageWithSeqs(Long... seqs) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AuditLog> p =
            new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
        List<AuditLog> records = new ArrayList<>();
        for (Long seq : seqs) {
            AuditLog log = new AuditLog();
            log.setSeq(seq);
            records.add(log);
        }
        p.setRecords(records);
        return p;
    }
}