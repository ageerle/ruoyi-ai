package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import org.ruoyi.ipd.domain.CertTemplate;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectCertItem;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.ProjectCertItemMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.ruoyi.ipd.seed.MarketCodeResolver;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-11.1：新硬件项目到阶段推进契约锁——门禁拒/过、SA 认证、BioCV 数值、失败后恢复。
 * 真库 HTTP 闭环见 docs/ipd-系统说明/验收/P1-11.1-* 与 .codex/ipd-dev/runtime/evidence-p1111.json。
 *
 * <p>命名辨析（防误判重复）：P1111 = P1-11.1 三段编号，主题是 GateEngine B 级 CONCEPT 门禁
 * 与 MarketCodeResolver 在「新硬件项目→阶段推进」链路中的<b>整合契约</b>；
 * 同包的 {@link P171AcceptanceTest} 才是 P1-7.1 国别认证模板的纯 resolver 单元测试。
 * 二者测试目标、断言维度、被测服务均不同，非重复用例。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P1111AcceptanceTest {

    @Mock private StageActionMapper actionMapper;
    @Mock private DeliverableMapper deliverableMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectStageMapper projectStageMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private SystemConfigService configService;
    @Mock private ProjectCertItemMapper certItemMapper;
    @Mock private CertTemplateService certTemplateService;

    private GateEngine gateEngine;
    private StageActionService stageActionService;
    private ProjectCertService projectCertService;

    @BeforeAll
    static void initMeta() {
        // ProjectCertService 内联构造 LambdaQueryWrapper<ProjectCertItem>，纯 Mockito 环境需预初始化
        // TableInfo 元数据（lambda cache），否则运行时抛 "can not find lambda cache for this entity"。
        // 模式对齐同包 P062AcceptanceTest.initMeta()。
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "P1111-test");
        TableInfoHelper.initTableInfo(assistant, ProjectCertItem.class);
    }

    @BeforeEach
    void setUp() {
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(projectMapper.selectById(any())).thenReturn(
            Project.builder().id(1111L).status("ACTIVE").delFlag("0").level("B").currentStage("CONCEPT").build());
        gateEngine = new GateEngine(actionMapper, configService);
        stageActionService = new StageActionService(actionMapper, deliverableMapper, auditLogService,
            projectStageMapper, projectMapper);
        projectCertService = new ProjectCertService(certItemMapper, projectMapper, certTemplateService, auditLogService);
    }

    @Test
    @DisplayName("B 级 CONCEPT 门禁必做仅 C11∩C12")
    void bConceptRequiredIsC11C12() {
        Set<String> codes = gateEngine.requiredCodesForStage("B", "CONCEPT");
        assertThat(codes).containsExactly("C11", "C12");
        assertThat(ActionCatalog.B_LEVEL_BLOCKING_CODES).containsAll(codes);
    }

    @Test
    @DisplayName("门禁拒绝：C12 未 DONE 时 check 抛 BR-IPD-06")
    void gateRejectsWhenC12Open() {
        Project p = Project.builder().id(1111L).level("B").currentStage("CONCEPT").build();
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            StageAction.builder().actionCode("C11").actionName("Charter").status("DONE").build(),
            StageAction.builder().actionCode("C12").actionName("合规").status("IN_PROGRESS").build()));
        assertThatThrownBy(() -> gateEngine.check(p, "CONCEPT"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("BR-IPD-06")
            .hasMessageContaining("C12");
    }

    @Test
    @DisplayName("门禁通过：C11/C12 均 DONE 后可过 CONCEPT")
    void gatePassesWhenBlockersDone() {
        Project p = Project.builder().id(1111L).level("B").currentStage("CONCEPT").build();
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            StageAction.builder().actionCode("C11").actionName("Charter").status("DONE").build(),
            StageAction.builder().actionCode("C12").actionName("合规").status("DONE").build()));
        gateEngine.check(p, "CONCEPT");
        GateChecklistView view = gateEngine.explainChecklist(p, "CONCEPT");
        assertThat(view.items()).allMatch(i -> i.ok());
    }

    @Test
    @DisplayName("SA 市场别名→SABER/SASO 必做落库契约")
    void saMarketSyncsSaber() {
        assertThat(MarketCodeResolver.knownCodes("[\"沙特\"]")).containsExactly("SA");
        Project p = Project.builder().id(20L).targetMarkets("[\"沙特\"]").delFlag("0").build();
        when(certTemplateService.resolve(any())).thenReturn(List.of(
            CertTemplate.builder().id(1L).countryCode("SA").countryName("沙特阿拉伯")
                .certName("SABER/SASO").isMandatory("1").build()));
        // R8-P0-6 批量契约：sync 走一次性 selectList 判重 + insertBatch，不再逐条 selectCount/insert
        when(certItemMapper.selectList(any())).thenReturn(List.of());
        assertThat(projectCertService.syncFromProject(p, 9L)).isEqualTo(1);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProjectCertItem>> cap = ArgumentCaptor.forClass(List.class);
        verify(certItemMapper).insertBatch(cap.capture(), eq(200));
        assertThat(cap.getValue()).extracting(ProjectCertItem::getCertName).containsExactly("SABER/SASO");
    }

    @Test
    @DisplayName("BioCV：D11 无 FAR/FRR → DONE 拒绝；成对登记后可通过校验")
    void biocvFarRequiredThenRecover() {
        StageAction a = StageAction.builder()
            .id(1L).projectId(1111L).stageId(10L).actionCode("D11").actionName("算法评测")
            .ownerRole("RD_PM").depth("LIGHT").status("IN_PROGRESS")
            .isBlocking("1").isBioFeature("1").version(0)
            .actualDoneAt(new Date())
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        lenient().when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);

        assertThatThrownBy(() -> stageActionService.transit(1L, "DONE", null, "op"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("FAR/FRR");

        a.setFarValue(new BigDecimal("0.0001"));
        a.setFrrValue(new BigDecimal("0.01"));
        a.setAlgoType("FACE");
        StageAction done = stageActionService.transit(1L, "DONE", null, "op");
        assertThat(done.getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("失败恢复剧本：清单可解释 ok=false → 完成后 ok=true")
    void checklistRecoverNarrative() {
        Project p = Project.builder().id(1111L).level("B").currentStage("CONCEPT").build();
        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            StageAction.builder().actionCode("C11").actionName("Charter").status("NOT_STARTED").build(),
            StageAction.builder().actionCode("C12").actionName("合规").status("NOT_STARTED").build()));
        GateChecklistView before = gateEngine.explainChecklist(p, "CONCEPT");
        assertThat(before.items()).extracting(i -> i.ok()).containsOnly(false);

        when(actionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(
            StageAction.builder().actionCode("C11").actionName("Charter").status("DONE").build(),
            StageAction.builder().actionCode("C12").actionName("合规").status("DONE").build()));
        GateChecklistView after = gateEngine.explainChecklist(p, "CONCEPT");
        assertThat(after.items()).extracting(i -> i.ok()).containsOnly(true);
    }
}
