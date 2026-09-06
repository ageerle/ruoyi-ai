package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * P1-8.2：Z 别名归一、算法分类合法、AC-IPD-17/18 FAR·FRR、合规 C12 入阻断集。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P182AcceptanceTest {

    @Mock private StageActionMapper actionMapper;
    @Mock private DeliverableMapper deliverableMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectStageMapper projectStageMapper;
    @Mock private ProjectMapper projectMapper;

    private StageActionService service;

    @BeforeEach
    void setUp() {
        lenient().when(projectMapper.selectById(any())).thenReturn(
            Project.builder().id(100L).status("ACTIVE").delFlag("0").level("B").build());
        lenient().when(auditLogService.append(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));
        service = new StageActionService(actionMapper, deliverableMapper, auditLogService,
            projectStageMapper, projectMapper);
    }

    private StageAction seed(String code, String depth) {
        StageAction a = StageAction.builder()
            .id(1L).projectId(100L).stageId(10L).actionCode(code).actionName("t")
            .ownerRole("MARKET_PM").depth(depth).status("IN_PROGRESS")
            .isBlocking("1").isBioFeature("1").version(0)
            .build();
        when(actionMapper.selectById(1L)).thenReturn(a);
        lenient().when(actionMapper.updateById(any(StageAction.class))).thenReturn(1);
        return a;
    }

    @Test
    @DisplayName("五对 Z 别名 byCode 归一为权威码")
    void fiveAliasesResolve() {
        assertThat(ActionCatalog.byCode("Z01").code()).isEqualTo("D11");
        assertThat(ActionCatalog.byCode("Z02").code()).isEqualTo("V10");
        assertThat(ActionCatalog.byCode("Z03").code()).isEqualTo("C12");
        assertThat(ActionCatalog.byCode("Z04").code()).isEqualTo("V11");
        assertThat(ActionCatalog.byCode("Z05").code()).isEqualTo("V12");
        assertThat(ActionCatalog.ALIASES).hasSize(5);
    }

    @Test
    @DisplayName("AC-IPD-17：D11/Z01 仅日期无 FAR/FRR → DONE 拒绝")
    void acIpd17RejectDoneWithoutFar() {
        StageAction a = seed("Z01", "LIGHT");
        a.setActualDoneAt(new Date());
        assertThatThrownBy(() -> service.transit(1L, "DONE", "x", "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("FAR/FRR");
        assertThat(a.getActionCode()).isEqualTo("D11");
    }

    @Test
    @DisplayName("AC-IPD-18：FAR=0.0001 FRR=0.01 保存成功并可 DONE")
    void acIpd18FarFrrPersist() {
        StageAction a = seed("Z01", "LIGHT");
        Date day = new Date(1_700_000_000_000L);
        service.recordFields(1L, day, new BigDecimal("0.0001"), new BigDecimal("0.01"),
            null, null, "FACE", "9");
        assertThat(a.getActionCode()).isEqualTo("D11");
        assertThat(a.getFarValue()).isEqualByComparingTo("0.0001");
        assertThat(a.getFrrValue()).isEqualByComparingTo("0.01");
        assertThat(a.getAlgoType()).isEqualTo("FACE");
        assertThat(service.transit(1L, "DONE", "ok", "9").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("算法分类非法拒绝；合法白名单归一大写")
    void algoTypeValidation() {
        StageAction a = seed("D11", "LIGHT");
        assertThatThrownBy(() -> service.recordFields(1L, null, null, null, null, null, "IRIS", "9"))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("算法分类非法");
        service.recordFields(1L, null, null, null, null, null, "fingerprint", "9");
        assertThat(a.getAlgoType()).isEqualTo("FINGERPRINT");
    }

    @Test
    @DisplayName("V02 证书字段保存重读语义（实例字段保留）")
    void v02CertPersist() {
        StageAction a = seed("V02", "LIGHT");
        Date passed = new Date(1_700_200_000_000L);
        service.recordFields(1L, new Date(), null, null, "ANATEL-99", passed, null, "9");
        assertThat(a.getCertNo()).isEqualTo("ANATEL-99");
        assertThat(a.getCertPassedAt()).isNotNull();
        assertThat(service.transit(1L, "DONE", "ok", "9").getStatus()).isEqualTo("DONE");
    }

    @Test
    @DisplayName("合规 C12（含 Z03）在 B 级阻断集，缺失阻断 Gate")
    void c12InBlockingSet() {
        assertThat(ActionCatalog.B_LEVEL_BLOCKING_CODES).contains("C12");
        assertThat(ActionCatalog.resolveCode("Z03")).isEqualTo("C12");
        SystemConfigService configs = org.mockito.Mockito.mock(SystemConfigService.class);
        GateEngine engine = new GateEngine(actionMapper, configs);
        when(actionMapper.selectList(any())).thenReturn(List.of(
            StageAction.builder().actionCode("C12").status("NOT_STARTED").build()));
        Project p = Project.builder().id(100L).level("B").currentStage("CONCEPT").delFlag("0").build();
        assertThatThrownBy(() -> engine.check(p, "CONCEPT"))
            .isInstanceOf(ServiceException.class);
    }
}
