package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.LegacyImport;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.dto.LegacyImportReq;
import org.ruoyi.ipd.dto.LegacyImportResult;
import org.ruoyi.ipd.dto.LegacyImportRowResult;
import org.ruoyi.ipd.mapper.LegacyImportMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 2026-09-08 缺口补齐回归：legacy_imports 批次记录落库。
 * 此前 LegacyImportService.importBatch 只逐行导入不写批次，
 * legacy_imports 表 0 行、批量导入不可追溯——本测试锁定批次两段写入契约。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class LegacyImportBatchRecordTest {

    @Mock
    private ProjectService projectService;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private StageActionMapper stageActionMapper;
    @Mock
    private LegacyImportMapper legacyImportMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ObjectProvider<LegacyImportService> self;

    private LegacyImportService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new LegacyImportService(
            projectService, projectMapper, stageActionMapper, legacyImportMapper,
            auditLogService, self, Runnable::run);
    }

    private LegacyImportRowResult okRow(int idx) {
        return new LegacyImportRowResult(idx, true, (long) (idx + 1), null, List.of());
    }

    private LegacyImportRowResult failRow(int idx) {
        return new LegacyImportRowResult(idx, false, null, "行失败", List.of());
    }

    @Test
    @DisplayName("2 行全成功：批次 IN_PROGRESS 写入 + SUCCESS 终态回填")
    void importBatch_allSuccess_batchTracked() {
        LegacyImportService proxy = mock(LegacyImportService.class);
        when(self.getIfAvailable()).thenReturn(proxy);
        lenient().when(self.getObject()).thenReturn(proxy);
        Project p = new Project();
        p.setId(1L);
        lenient().when(proxy.importOne(any(LegacyImportReq.class), anyLong()))
            .thenReturn(new LegacyImportResult(p, List.of()));
        // insert 与 updateById 传同一引用（closeBatch 原地改终态），捕获 insert 时的快照验 IN_PROGRESS
        List<LegacyImport> openedSnapshots = new java.util.ArrayList<>();
        lenient().when(legacyImportMapper.insert(any(LegacyImport.class))).thenAnswer(inv -> {
            LegacyImport b = inv.getArgument(0);
            LegacyImport snap = new LegacyImport();
            snap.setBatchNo(b.getBatchNo());
            snap.setImportStatus(b.getImportStatus());
            snap.setTotalCount(b.getTotalCount());
            snap.setImportedBy(b.getImportedBy());
            snap.setSourceSystem(b.getSourceSystem());
            openedSnapshots.add(snap);
            return 1;
        });

        List<LegacyImportRowResult> results = service.importBatch(rows(2), 88L);

        assertThat(results).hasSize(2);
        assertThat(openedSnapshots).hasSize(1);
        LegacyImport opened = openedSnapshots.get(0);
        assertThat(opened.getBatchNo()).startsWith("LEG-");
        assertThat(opened.getImportStatus()).isEqualTo("IN_PROGRESS");
        assertThat(opened.getTotalCount()).isEqualTo(2);
        assertThat(opened.getImportedBy()).isEqualTo(88L);
        assertThat(opened.getSourceSystem()).isEqualTo("MANUAL_IMPORT");

        ArgumentCaptor<LegacyImport> closer = ArgumentCaptor.forClass(LegacyImport.class);
        verify(legacyImportMapper).updateById(closer.capture());
        LegacyImport closed = closer.getValue();
        assertThat(closed.getImportStatus()).isEqualTo("SUCCESS");
        assertThat(closed.getSuccessCount()).isEqualTo(2);
        assertThat(closed.getErrorCount()).isZero();
        assertThat(closed.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("1 成 1 败：批次 PARTIAL，success/error 计数各自落账")
    void importBatch_partial_batchPartiallyTracked() {
        LegacyImportService proxy = mock(LegacyImportService.class);
        when(self.getIfAvailable()).thenReturn(proxy);
        lenient().when(self.getObject()).thenReturn(proxy);
        Project p = new Project();
        p.setId(1L);
        lenient().when(proxy.importOne(any(LegacyImportReq.class), anyLong()))
            .thenReturn(new LegacyImportResult(p, List.of()))
            .thenThrow(new ServiceException("行校验失败"));

        List<LegacyImportRowResult> results = service.importBatch(rows(2), 88L);

        assertThat(results).hasSize(2);
        ArgumentCaptor<LegacyImport> captor = ArgumentCaptor.forClass(LegacyImport.class);
        verify(legacyImportMapper).updateById(captor.capture());
        LegacyImport closed = captor.getValue();
        assertThat(closed.getImportStatus()).isEqualTo("PARTIAL");
        assertThat(closed.getSuccessCount()).isEqualTo(1);
        assertThat(closed.getErrorCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("空行列表 → 拒绝且不写批次")
    void importBatch_emptyRows_rejected() {
        assertThatThrownBy(() -> service.importBatch(Collections.emptyList(), 88L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("导入行不能为空");
        verify(legacyImportMapper, never()).insert(any(LegacyImport.class));
    }

    @Test
    @DisplayName("超过 500 行上限 → 拒绝且不写批次")
    void importBatch_overLimit_rejected() {
        assertThatThrownBy(() -> service.importBatch(rows(501), 88L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("500");
        verify(legacyImportMapper, never()).insert(any(LegacyImport.class));
        verify(legacyImportMapper, times(0)).updateById(any(LegacyImport.class));
    }

    private List<LegacyImportReq> rows(int n) {
        return IntStream.range(0, n)
            .mapToObj(i -> new LegacyImportReq(
                "存量导入项目" + i, 100L, "HARDWARE", "[\"CN\"]", "B",
                null, null, null, null, null, null, null,
                java.util.Date.from(java.time.LocalDate.of(2026, 1, 1)
                    .atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()),
                "DEV", null, null))
            .toList();
    }
}
