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
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.dto.LegacyImportReq;
import org.ruoyi.ipd.dto.LegacyImportResult;
import org.ruoyi.ipd.dto.LegacyImportRowResult;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.springframework.beans.factory.ObjectProvider;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-9.1：AC-PROD-04 / BR-PROD-03 — LEGACY 导入、历史缺失、不伪造 DONE、批量错误隔离。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P191AcceptanceTest {

    @Mock private ProjectService projectService;
    @Mock private ProjectMapper projectMapper;
    @Mock private StageActionMapper stageActionMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ObjectProvider<LegacyImportService> self;
    @Mock private SystemConfigService configService;

    private LegacyImportService legacyImportService;
    private GateEngine gateEngine;

    @BeforeEach
    void setUp() {
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(self.getIfAvailable()).thenReturn(null);
        legacyImportService = new LegacyImportService(
            projectService, projectMapper, stageActionMapper, auditLogService, self);
        gateEngine = new GateEngine(stageActionMapper, configService);
    }

    private LegacyImportReq baseReq(String stage) {
        return new LegacyImportReq(
            "存量导入项目甲", 100L, "HARDWARE", "[\"CN\"]", "B",
            new BigDecimal("0.8"), "B级", new BigDecimal("1000000"),
            10, 50, 5, 900001L, new Date(), stage, true,
            Map.of("C11", "会议纪要-2024"));
    }

    @Test
    @DisplayName("阶段别名 develop/verify 归一")
    void stageAliases() {
        assertThat(LegacyImportService.normalizeStage("develop")).isEqualTo("DEV");
        assertThat(LegacyImportService.normalizeStage("verify")).isEqualTo("VALID");
        assertThatThrownBy(() -> LegacyImportService.normalizeStage("nope"))
            .isInstanceOf(ServiceException.class);
    }

    @Test
    @DisplayName("缺少历史缺失声明或生效日 → 拒绝")
    void rejectWithoutAckOrDate() {
        LegacyImportReq noAck = new LegacyImportReq(
            "存量导入项目乙", 100L, "HARDWARE", "[\"CN\"]", "B",
            new BigDecimal("0.8"), "B", new BigDecimal("1"), 1, 1, 1, 900001L,
            new Date(), "DEV", false, null);
        assertThatThrownBy(() -> legacyImportService.importOne(noAck, 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("missingHistoryAck");

        LegacyImportReq noDate = new LegacyImportReq(
            "存量导入项目丙", 100L, "HARDWARE", "[\"CN\"]", "B",
            new BigDecimal("0.8"), "B", new BigDecimal("1"), 1, 1, 1, 900001L,
            null, "DEV", true, null);
        assertThatThrownBy(() -> legacyImportService.importOne(noDate, 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("legacyEffectiveAt");
    }

    @Test
    @DisplayName("AC-PROD-04：导入后过往动作标历史缺失且 status 仍非 DONE")
    void markHistoricalMissingWithoutForgingDone() {
        when(projectMapper.selectCount(any())).thenReturn(0L);
        Project created = Project.builder().id(50L).name("存量导入项目甲").source("NEW")
            .currentStage("CONCEPT").status("DRAFT").level("B").build();
        when(projectService.create(any(Project.class), anyLong())).thenAnswer(inv -> {
            Project p = inv.getArgument(0);
            assertThat(p.getSource()).isEqualTo("LEGACY");
            return created;
        });
        when(stageActionMapper.selectList(any())).thenReturn(List.of(
            StageAction.builder().id(1L).projectId(50L).actionCode("C11").actionName("Charter")
                .status("NOT_STARTED").build(),
            StageAction.builder().id(2L).projectId(50L).actionCode("P12").actionName("计划Gate")
                .status("NOT_STARTED").build(),
            StageAction.builder().id(3L).projectId(50L).actionCode("D05").actionName("开发Gate")
                .status("NOT_STARTED").build()));
        when(stageActionMapper.updateById(any(StageAction.class))).thenReturn(1);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);
        when(projectService.getById(50L)).thenAnswer(inv -> {
            Project p = Project.builder().id(50L).name("存量导入项目甲").source("LEGACY")
                .currentStage("DEV").declaredStage("DEV").missingHistoryAck("1")
                .catchupStatus("IN_PROGRESS").level("B").build();
            return p;
        });

        LegacyImportResult result = legacyImportService.importOne(baseReq("develop"), 9L);
        assertThat(result.markedCodes()).contains("C11", "P12").doesNotContain("D05");
        ArgumentCaptor<StageAction> cap = ArgumentCaptor.forClass(StageAction.class);
        verify(stageActionMapper, org.mockito.Mockito.atLeastOnce()).updateById(cap.capture());
        assertThat(cap.getAllValues()).allMatch(a ->
            LegacyImportService.HISTORY_MISSING.equals(a.getHistoryMark())
                && !"DONE".equals(a.getStatus()));
        assertThat(cap.getAllValues().stream().anyMatch(a ->
            a.getRemark() != null && a.getRemark().contains("会议纪要"))).isTrue();

        ArgumentCaptor<AuditLog> auditCap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(auditCap.capture());
        assertThat(auditCap.getValue().getAction()).isEqualTo("PROJECT_LEGACY_IMPORT");
        assertThat(auditCap.getValue().getAfterData()).contains("markedCodes");
        assertThat(auditCap.getValue().getAfterData()).contains("C11");
    }

    @Test
    @DisplayName("历史缺失视同门禁满足，不阻断推进校验")
    void historicalMissingDoesNotBlockGate() {
        Project p = Project.builder().id(50L).level("B").currentStage("CONCEPT").build();
        when(stageActionMapper.selectList(any())).thenReturn(List.of(
            StageAction.builder().actionCode("C11").actionName("Charter").status("NOT_STARTED")
                .historyMark(LegacyImportService.HISTORY_MISSING).build(),
            StageAction.builder().actionCode("C12").actionName("合规").status("NOT_STARTED")
                .historyMark(LegacyImportService.HISTORY_MISSING).build()));
        gateEngine.check(p, "CONCEPT");
        GateChecklistView view = gateEngine.explainChecklist(p, "CONCEPT");
        assertThat(view.items()).allMatch(i -> i.ok());
        assertThat(view.items().get(0).reason()).contains("历史缺失");
    }

    @Test
    @DisplayName("批量：错误行隔离，成功行保留")
    void batchIsolatesErrors() {
        when(projectMapper.selectCount(any())).thenReturn(0L);
        when(projectService.create(any(Project.class), anyLong())).thenAnswer(inv -> {
            Project in = inv.getArgument(0);
            return Project.builder().id(88L).name(in.getName()).source("LEGACY")
                .currentStage("CONCEPT").build();
        });
        when(stageActionMapper.selectList(any())).thenReturn(List.of());
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);
        when(projectService.getById(88L)).thenReturn(Project.builder().id(88L).name("存量导入项目甲")
            .declaredStage("PLAN").currentStage("PLAN").build());

        LegacyImportReq bad = new LegacyImportReq(
            "坏", 100L, "HARDWARE", "[\"CN\"]", "B",
            new BigDecimal("0.8"), "B", new BigDecimal("1"), 1, 1, 1, 900001L,
            new Date(), "PLAN", true, null);
        List<LegacyImportRowResult> rows = legacyImportService.importBatch(
            List.of(baseReq("PLAN"), bad), 9L);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).ok()).isTrue();
        assertThat(rows.get(0).projectId()).isEqualTo(88L);
        assertThat(rows.get(1).ok()).isFalse();
        assertThat(rows.get(1).error()).contains("至少 4");
    }

    @Test
    @DisplayName("名称重复拒绝")
    void rejectDuplicateName() {
        when(projectMapper.selectCount(any())).thenReturn(1L);
        assertThatThrownBy(() -> legacyImportService.importOne(baseReq("PLAN"), 9L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已存在");
    }
}
