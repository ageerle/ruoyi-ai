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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P2-5.1 Gate 提交与逐项判定附件快照验收（AC-GATE-02/15/19/20/1a~1d）。
 *
 * <p>正反例口径（激活 gate_element_results 死表，QA-04-D1）：
 * <ul>
 *   <li>要素清单：按 Gate 展示且含否决标记与当前判定</li>
 *   <li>AC-GATE-02：FAIL 判定缺证据附件 ⇒ 拒绝</li>
 *   <li>CONDITIONAL 缺说明 ⇒ 拒绝</li>
 *   <li>AC-GATE-1a：5 家一手验证 ⇒ G1-1 PASS 放行</li>
 *   <li>AC-GATE-1b：3 家且无书面意向 ⇒ 拒绝（提示需 ≥5 家或 ≥1 份意向）</li>
 *   <li>AC-GATE-1c：1 家书面意向（无一手验证）⇒ PASS（替代路径）</li>
 *   <li>AC-GATE-1d：阈值参数改 3 ⇒ 3 家即 PASS（可配置即时生效）</li>
 *   <li>AC-GATE-15/19/20：否决项 FAIL ⇒ 提交阻断；未全判 ⇒ 提交阻断并列缺失</li>
 *   <li>提交成功 ⇒ 冻结要素快照 + startedAt 置位 + 审计 GATE_SUBMIT</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P251AcceptanceTest {

    @Mock
    private GateMapper gateMapper;
    @Mock
    private GateElementMapper elementMapper;
    @Mock
    private GateElementResultMapper resultMapper;
    @Mock
    private SystemConfigService systemConfigService;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private NotificationService notificationService;

    private GateElementResultService service;

    private static final IpdActor PM = new IpdActor(301L, "评审PM", "MARKET_PM", 7L);

    private Gate gate;
    private GateElement vetoElement;   // G1-2 否决项（四项基准值，AC-GATE-20）
    private GateElement g1Customer;    // G1-1 客户一手验证（AC-GATE-1a~1d）
    private GateElement normalElement; // G1-3 普通要素

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P251-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P251-ger"), GateElementResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateElementResultService(gateMapper, elementMapper, resultMapper,
            systemConfigService, auditLogService, notificationService);
        gate = new Gate();
        gate.setId(501L);
        gate.setProjectId(11L);
        gate.setGateCode("G1");
        gate.setStatus("PENDING");
        gate.setCurrentRound(1);
        vetoElement = element(601L, "G1", "G1-2", "1");
        g1Customer = element(602L, "G1", "G1-1", "0");
        normalElement = element(603L, "G1", "G1-3", "0");
        lenient().when(gateMapper.selectById(501L)).thenReturn(gate);
        lenient().when(elementMapper.selectList(any())).thenReturn(List.of(vetoElement, g1Customer, normalElement));
        lenient().when(gateMapper.selectList(any())).thenReturn(List.of());
        lenient().when(resultMapper.selectList(any())).thenReturn(List.of());
        lenient().when(systemConfigService.getIntValue("gate.g1.minCustomerVerifications", 5)).thenReturn(5);
    }

    private GateElement element(Long id, String gateCode, String code, String isVeto) {
        GateElement e = new GateElement();
        e.setId(id);
        e.setGateCode(gateCode);
        e.setElementCode(code);
        e.setElementName("要素" + code);
        e.setPassStandard("标准" + code);
        e.setIsVeto(isVeto);
        e.setSortOrder(1);
        return e;
    }

    @Test
    @DisplayName("要素清单：按 Gate 展示，含否决标记；未判定 result=null")
    void checklist_showsElementsWithVetoFlag() {
        List<java.util.Map<String, Object>> view = service.checklist(501L);

        assertThat(view).hasSize(3);
        assertThat(view.get(0)).containsEntry("elementCode", "G1-2").containsEntry("isVeto", true);
        assertThat(view.get(1)).containsEntry("elementCode", "G1-1").containsEntry("isVeto", false);
        assertThat(view.get(0)).containsEntry("result", null);
    }

    @Test
    @DisplayName("AC-GATE-02 反例：FAIL 判定缺证据附件 ⇒ 拒绝")
    void judge_failWithoutEvidence_rejected() {
        when(elementMapper.selectById(603L)).thenReturn(normalElement);

        assertThatThrownBy(() -> service.judge(501L, 603L, "FAIL", null, null, null, null, null, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须附证据");
        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("提交后快照冻结：startedAt 置位期间改判拒绝（P2-5.2 复核建议补）")
    void judge_afterSubmitFrozen_rejected() {
        gate.setStartedAt(new Date()); // 守卫在要素查询前即拒，无需要素 stub

        assertThatThrownBy(() -> service.judge(501L, 603L, "PASS", null, null, null, null, null, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("冻结快照");
        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("CONDITIONAL 缺说明 ⇒ 拒绝")
    void judge_conditionalWithoutNote_rejected() {
        when(elementMapper.selectById(603L)).thenReturn(normalElement);

        assertThatThrownBy(() -> service.judge(501L, 603L, "CONDITIONAL", " ", null, null, null, null, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("必须填写说明");
    }

    @Test
    @DisplayName("AC-GATE-1a 正例：5 家一手验证 ⇒ G1-1 判 PASS 放行")
    void judge_g1Customer_5verifications_pass() {
        when(elementMapper.selectById(602L)).thenReturn(g1Customer);
        when(resultMapper.selectOne(any())).thenReturn(null);

        GateElementResult row = service.judge(501L, 602L, "PASS", null, null, 5, 0, null, null, PM);

        assertThat(row.getResult()).isEqualTo("PASS");
        verify(resultMapper).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("AC-GATE-1b 反例：3 家一手验证且无书面意向 ⇒ 拒绝（默认阈值 5）")
    void judge_g1Customer_3verifications_rejected() {
        when(elementMapper.selectById(602L)).thenReturn(g1Customer);

        assertThatThrownBy(() -> service.judge(501L, 602L, "PASS", null, null, 3, 0, null, null, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("≥5 家一手验证");
    }

    @Test
    @DisplayName("AC-GATE-1c 替代路径：1 份书面意向（无一手验证）⇒ PASS")
    void judge_g1Customer_writtenIntent_pass() {
        when(elementMapper.selectById(602L)).thenReturn(g1Customer);
        when(resultMapper.selectOne(any())).thenReturn(null);

        GateElementResult row = service.judge(501L, 602L, "PASS", null, null, 0, 1, null, null, PM);

        assertThat(row.getResult()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("AC-GATE-1d：阈值参数改为 3 ⇒ 3 家一手验证即 PASS（可配置即时生效）")
    void judge_g1Customer_configuredThreshold3_pass() {
        when(elementMapper.selectById(602L)).thenReturn(g1Customer);
        when(resultMapper.selectOne(any())).thenReturn(null);
        when(systemConfigService.getIntValue("gate.g1.minCustomerVerifications", 5)).thenReturn(3);

        GateElementResult row = service.judge(501L, 602L, "PASS", null, null, 3, 0, null, null, PM);

        assertThat(row.getResult()).isEqualTo("PASS");
    }

    @Test
    @DisplayName("提交阻断：适用要素未全判 ⇒ 拒绝并列缺失项")
    void submit_missingJudgements_rejected() {
        assertThatThrownBy(() -> service.submit(501L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("尚未判定")
            .hasMessageContaining("G1-1");
        verify(gateMapper, never()).updateById(any(Gate.class));
    }

    @Test
    @DisplayName("AC-GATE-15/19/20：否决项 FAIL ⇒ 提交阻断")
    void submit_vetoFail_blocked() {
        withJudged(judged(601L, "FAIL", "否决证据.pdf"),   // 否决项 FAIL（有证据）
                   judged(602L, "PASS", null),
                   judged(603L, "PASS", null));

        assertThatThrownBy(() -> service.submit(501L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("命中否决项")
            .hasMessageContaining("G1-2");
    }

    @Test
    @DisplayName("AC-GATE-02：FAIL 行缺证据（历史脏数据）⇒ 提交阻断")
    void submit_failRowMissingEvidence_blocked() {
        withJudged(judged(601L, "PASS", null),
                   judged(602L, "PASS", null),
                   judged(603L, "FAIL", null));  // 普通要素 FAIL 无证据

        assertThatThrownBy(() -> service.submit(501L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("缺证据");
    }

    @Test
    @DisplayName("提交成功：全要素已判且无否决 FAIL ⇒ 冻结快照 + startedAt + 审计 GATE_SUBMIT")
    void submit_success_freezesSnapshot() {
        withJudged(judged(601L, "PASS", null),
                   judged(602L, "PASS", null),
                   judged(603L, "CONDITIONAL", null));

        Gate submitted = service.submit(501L, PM);

        assertThat(submitted.getStartedAt()).isNotNull();
        assertThat(submitted.getElementSnapshot()).contains("G1-1").contains("G1-3").contains("CONDITIONAL");
        verify(gateMapper).updateById(gate);
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("重复提交：已提交（startedAt 非空）⇒ 拒绝")
    void submit_twice_rejected() {
        gate.setStartedAt(new java.util.Date());

        assertThatThrownBy(() -> service.submit(501L, PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("等待签署");
    }

    @Test
    @DisplayName("改判幂等：同要素再次判定更新既有行（不产生第二行）")
    void judge_updatesExistingRow() {
        when(elementMapper.selectById(603L)).thenReturn(normalElement);
        GateElementResult existing = judged(603L, "CONDITIONAL", null);
        existing.setId(9001L);
        existing.setGateId(501L);
        when(resultMapper.selectOne(any())).thenReturn(existing);

        GateElementResult row = service.judge(501L, 603L, "PASS", null, null, null, null, null, null, PM);

        assertThat(row.getId()).isEqualTo(9001L);
        assertThat(row.getResult()).isEqualTo("PASS");
        // P2-5.3 起改判统一 LambdaUpdateWrapper 显式 set（含遗留清空），不再 updateById
        verify(resultMapper).update(any(), any());
        verify(resultMapper, never()).updateById(any(GateElementResult.class));
        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    private GateElementResult judged(Long elementId, String result, String evidence) {
        GateElementResult r = GateElementResult.builder()
            .gateId(501L).elementId(elementId).result(result)
            .conditionNote("CONDITIONAL".equals(result) ? "整改中" : null)
            .evidenceRef(evidence)
            .build();
        return r;
    }

    private void withJudged(GateElementResult... rows) {
        when(resultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(rows));
    }
}
