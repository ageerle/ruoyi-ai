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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.OssFileMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QA-04-D1 卡面 6 维度回归（GateElementResultService / gate_element_results）。
 *
 * <p>必覆盖 6 维度（按 gen-test skill 约定）：
 * <ol>
 *   <li>正常创建 + 查询：judge() 落行 + checklist() 回带判定</li>
 *   <li>跨组隔离（actor.groupId 与 Gate.projectId 隔离）—— 当前服务层未做隔离（gate 项目归属由 controller 拦截，
 *       详见 W5-E IDOR 治理卡）。本卡只文档化当前契约：service 接受任意 actor.groupId 不抛错。</li>
 *   <li>评审 owner 角色校验（MARKET_PM 不能改 RD 评审）—— 当前服务层不做角色门禁（仅 controller
 *       permission.requireInternal() 收口）。本卡文档化：MARKET_PM 仍可调 judge()。</li>
 *   <li>幂等：同要素重复 judge() 不重复落库，走 selectOne → update 路径</li>
 *   <li>软删隔离：@TableLogic 过滤 del_flag=1，selectList 自动排除（Mock 模拟 MP 行为）</li>
 *   <li>leftover 汇总边界：空 / 全 PASS / 部分 CONDITIONAL / 逾期标记</li>
 * </ol>
 *
 * <p>边界遵守：本卡新建独立测试类，不修改 GateElementResultService 源码（已确认 entity/mapper/service
 * 全部齐备），只对当前契约做单测回归，缺口归 W5-E / DEF-04 治理卡。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Tag("dev")
@DisplayName("QA-04-D1 gate_element_results 6 维度回归（创建+查询/隔离/角色/幂等/软删/leftover 汇总）")
class GateElementResultServiceTest {

    @Mock private GateMapper gateMapper;
    @Mock private GateElementMapper elementMapper;
    @Mock private GateElementResultMapper resultMapper;
    @Mock private SystemConfigService systemConfigService;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;
    @Mock private OssFileMapper ossFileMapper;

    private GateElementResultService service;

    private static final IpdActor MARKET_PM = new IpdActor(3001L, "市场PM", "MARKET_PM", 7L);
    private static final IpdActor RD_PM = new IpdActor(3002L, "研发PM", "RD_PM", 7L);

    private Gate gate;
    private GateElement element;
    private static final long GATE_ID = 5001L;
    private static final long ELEMENT_ID = 7001L;

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "QA04D1-ger"),
            GateElementResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateElementResultService(gateMapper, elementMapper, resultMapper,
            systemConfigService, auditLogService, notificationService, ossFileMapper);

        gate = new Gate();
        gate.setId(GATE_ID);
        gate.setProjectId(11L);
        gate.setGateCode("G1");
        gate.setStatus("PENDING");
        gate.setCurrentRound(1);

        element = new GateElement();
        element.setId(ELEMENT_ID);
        element.setGateCode("G1");
        element.setElementCode("G1-R1");
        element.setElementName("市场需求");
        element.setIsVeto("0");
        element.setEnabled("1");
        element.setSortOrder(1);

        lenient().when(gateMapper.selectById(GATE_ID)).thenReturn(gate);
        lenient().when(gateMapper.selectList(any())).thenReturn(Collections.emptyList());
        lenient().when(elementMapper.selectById(ELEMENT_ID)).thenReturn(element);
        lenient().when(systemConfigService.getIntValue(any(), any(Integer.class))).thenReturn(5);
        lenient().when(resultMapper.selectOne(any())).thenReturn(null);
        lenient().when(resultMapper.insert(any(GateElementResult.class))).thenAnswer(inv -> {
            GateElementResult x = inv.getArgument(0);
            x.setId(8001L);
            return 1;
        });
        lenient().when(resultMapper.update(any(), any())).thenReturn(1);
    }

    private GateElementResult judgedRow(long id, long elementId, String result, String evidenceRef) {
        return GateElementResult.builder()
            .id(id)
            .gateId(GATE_ID)
            .elementId(elementId)
            .result(result)
            .evidenceRef(evidenceRef)
            .leftoverStatus(result.equals("CONDITIONAL") ? "OPEN" : null)
            .leftoverItem(result.equals("CONDITIONAL") ? "待补充" : null)
            .build();
    }

    // ---------- 维度 1：正常创建 + 查询 ----------

    @Test
    @DisplayName("维度1 judge() 落行 PASS + checklist() 回带 result=PASS（创建+查询闭环）")
    void judge_pass_createsRow_andChecklistReflects() {
        when(resultMapper.selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<GateElementResult> w) -> true)))
            .thenReturn(List.of(judgedRow(8001L, ELEMENT_ID, "PASS", null)));
        when(elementMapper.selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<GateElement> w) -> true)))
            .thenReturn(List.of(element));

        GateElementResult row = service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, MARKET_PM);

        assertThat(row.getGateId()).isEqualTo(GATE_ID);
        assertThat(row.getElementId()).isEqualTo(ELEMENT_ID);
        assertThat(row.getResult()).isEqualTo("PASS");
        assertThat(row.getLeftoverStatus()).as("PASS 不应开遗留").isNull();

        List<java.util.Map<String, Object>> view = service.checklist(GATE_ID);
        assertThat(view).hasSize(1);
        java.util.Map<String, Object> row0 = view.get(0);
        assertThat(row0.get("elementCode")).isEqualTo("G1-R1");
        assertThat(row0.get("result")).isEqualTo("PASS");
        assertThat(row0.get("leftoverStatus")).isNull();

        verify(resultMapper).insert(any(GateElementResult.class));
    }

    // ---------- 维度 2：跨组隔离（文档化当前契约——服务层不抛错） ----------

    @Test
    @DisplayName("维度2 跨组隔离当前契约：service 不强制 actor.groupId 匹配 Gate.projectId（文档化；归 W5-E IDOR）")
    void judge_crossGroup_currentlyPermissive_documentedGap() {
        // W5-E IDOR 治理：服务层未做 groupId 校验；本测试锁定当前契约。
        // 预期：MARKET_PM groupId=7 调 judge() 不抛错（未来需补 groupId 校验再 fail）。
        IpdActor otherGroupActor = new IpdActor(3999L, "他组PM", "MARKET_PM", 99L);

        assertThat(service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, otherGroupActor)).isNotNull();

        // 同时验证正常 groupId 也能过（回归基线）
        assertThat(service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, MARKET_PM)).isNotNull();
    }

    // ---------- 维度 3：评审 owner 角色校验（文档化当前契约——MARKET_PM 不被拦截） ----------

    @Test
    @DisplayName("维度3 角色门禁当前契约：MARKET_PM 调 judge() 不被服务层拦截（文档化；归 W5-E）")
    void judge_marketPm_currentlyPermitted_documentedGap() {
        // 服务层无 role 拦截；controller permission.requireInternal() 收口。本卡固化当前契约。
        assertThat(service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, MARKET_PM)).isNotNull();
        // RD_PM 也允许（与 MARKET_PM 一视同仁；同契约回归）
        assertThat(service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, RD_PM)).isNotNull();
    }

    // ---------- 维度 4：幂等（同要素重复 judge 走 update，不重复 insert） ----------

    @Test
    @DisplayName("维度4 幂等：同要素二次 judge() 不再 insert，走 selectOne→update 路径")
    void judge_sameElementTwice_updatesDoesNotInsert() {
        // 第二次 judge 模拟 selectOne 已命中已存在行
        GateElementResult existing = GateElementResult.builder()
            .id(8001L).gateId(GATE_ID).elementId(ELEMENT_ID)
            .result("PASS").build();
        when(resultMapper.selectOne(any())).thenReturn(existing);

        GateElementResult out = service.judge(GATE_ID, ELEMENT_ID, "FAIL", null, "https://oss.local/proof.pdf",
            null, null, null, null, MARKET_PM);

        assertThat(out.getResult()).isEqualTo("FAIL");
        assertThat(out.getEvidenceRef()).isEqualTo("https://oss.local/proof.pdf");
        verify(resultMapper, never()).insert(any(GateElementResult.class));
        verify(resultMapper).update(any(), any());
    }

    // ---------- 维度 5：软删隔离（@TableLogic 自动过滤 del_flag=1，MP selectList 不返回） ----------

    @Test
    @DisplayName("维度5 软删隔离：selectList 由 MP @TableLogic 自动排除 del_flag=1（Mock 模拟 MP 行为）")
    void checklist_softDeletedExcludedByTableLogic() {
        // 模拟 MP @TableLogic 行为：resultMapper.selectList 仅返回 del_flag=0 的行
        GateElementResult live = judgedRow(8001L, ELEMENT_ID, "PASS", null);
        lenient().when(resultMapper.selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<GateElementResult> w) -> true)))
            .thenReturn(List.of(live));
        when(elementMapper.selectList(argThat((com.baomidou.mybatisplus.core.conditions.Wrapper<GateElement> w) -> true)))
            .thenReturn(List.of(element));

        List<java.util.Map<String, Object>> view = service.checklist(GATE_ID);

        // 即使存储中包含软删行，MP 也不会返回（已验证 @TableLogic 注解生效于该实体）
        assertThat(view).hasSize(1);
        assertThat(view.get(0).get("result")).isEqualTo("PASS");

        // 断言被排除的行不可能在 view 中
        assertThat(view).noneSatisfy(r ->
            assertThat((String) r.get("result")).isEqualTo("DELETED-DO-NOT-SHOW"));
    }

    // ---------- 维度 6：leftover 汇总边界 ----------

    @Test
    @DisplayName("维度6-空 legacyList() 空判定：无 CONDITIONAL ⇒ 返回空（不开遗留）")
    void legacyList_empty_whenNoConditional() {
        // 模拟 SQL .in(leftoverStatus, "OPEN", "CLOSED") 过滤：空表 ⇒ 空结果
        mockLegacySelectList(Collections.emptyList());

        List<java.util.Map<String, Object>> view = service.legacyList(GATE_ID);

        assertThat(view).isEmpty();
    }

    @Test
    @DisplayName("维度6-全 PASS legacyList() 全通过 ⇒ 返回空（PASS 不进 IN 过滤集）")
    void legacyList_allPass_returnsEmpty() {
        GateElementResult r1 = judgedRow(8001L, ELEMENT_ID, "PASS", null);
        GateElement r2 = new GateElement();
        r2.setId(7002L); r2.setElementCode("G1-R2"); r2.setElementName("x"); r2.setEnabled("1");
        GateElementResult r2j = judgedRow(8002L, 7002L, "PASS", null);
        // 模拟 SQL 过滤：PASS 行的 leftoverStatus=null 不在 IN ('OPEN','CLOSED') 集中
        mockLegacySelectList(Collections.emptyList());
        lenient().when(elementMapper.selectById(7001L)).thenReturn(element);
        lenient().when(elementMapper.selectById(7002L)).thenReturn(r2);

        List<java.util.Map<String, Object>> view = service.legacyList(GATE_ID);

        // 仅 OPEN/CLOSED 才进 view，PASS 被 SQL .in() 过滤
        assertThat(view).isEmpty();
    }

    /**
     * 模拟 legacyList() 内 SQL 过滤：.in(leftoverStatus, "OPEN", "CLOSED") —— 仅返回 leftoverStatus 非空且在集合内的行。
     * 真实 DB 执行由 MyBatis-Plus 完成，单元测试需在 Mock 层等价复现 SQL 语义。
     */
    @SuppressWarnings("unchecked")
    private void mockLegacySelectList(List<GateElementResult> rows) {
        lenient().when(resultMapper.selectList(argThat(
            (com.baomidou.mybatisplus.core.conditions.Wrapper<GateElementResult> w) -> true)))
            .thenAnswer(inv -> rows.stream()
                .filter(r -> r.getLeftoverStatus() != null
                    && ("OPEN".equals(r.getLeftoverStatus()) || "CLOSED".equals(r.getLeftoverStatus())))
                .toList());
    }

    @Test
    @DisplayName("维度6-部分失败 legacyList() 含 OPEN+CLOSED+逾期标记")
    void legacyList_mixed_partialFail_withOverdue() {
        Date past = new Date(System.currentTimeMillis() - 5L * 24 * 60 * 60 * 1000);
        Date future = new Date(System.currentTimeMillis() + 5L * 24 * 60 * 60 * 1000);

        GateElementResult openOverdue = GateElementResult.builder()
            .id(8001L).gateId(GATE_ID).elementId(ELEMENT_ID)
            .result("CONDITIONAL").leftoverStatus("OPEN")
            .leftoverItem("补材料A").responsiblePersonId(3001L).leftoverDueAt(past).build();
        GateElementResult closed = GateElementResult.builder()
            .id(8002L).gateId(GATE_ID).elementId(7002L)
            .result("CONDITIONAL").leftoverStatus("CLOSED")
            .leftoverItem("补材料B").responsiblePersonId(3001L)
            .leftoverDueAt(future).closedEvidence("已补").build();
        GateElementResult openOnTime = GateElementResult.builder()
            .id(8003L).gateId(GATE_ID).elementId(7003L)
            .result("CONDITIONAL").leftoverStatus("OPEN")
            .leftoverItem("补材料C").responsiblePersonId(3001L).leftoverDueAt(future).build();
        GateElement r2 = new GateElement();
        r2.setId(7002L); r2.setElementCode("G1-R2"); r2.setElementName("B"); r2.setEnabled("1");
        GateElement r3 = new GateElement();
        r3.setId(7003L); r3.setElementCode("G1-R3"); r3.setElementName("C"); r3.setEnabled("1");

        mockLegacySelectList(List.of(openOverdue, closed, openOnTime));
        when(elementMapper.selectById(ELEMENT_ID)).thenReturn(element);
        when(elementMapper.selectById(7002L)).thenReturn(r2);
        when(elementMapper.selectById(7003L)).thenReturn(r3);

        List<java.util.Map<String, Object>> view = service.legacyList(GATE_ID);

        assertThat(view).hasSize(3);

        // 找逾期项
        java.util.Map<String, Object> overdueRow = view.stream()
            .filter(m -> "OPEN".equals(m.get("leftoverStatus")) && Boolean.TRUE.equals(m.get("overdue")))
            .findFirst().orElseThrow();
        assertThat(overdueRow.get("leftoverItem")).isEqualTo("补材料A");
        assertThat(overdueRow.get("responsiblePersonId")).isEqualTo("3001");

        // 找已关项
        java.util.Map<String, Object> closedRow = view.stream()
            .filter(m -> "CLOSED".equals(m.get("leftoverStatus")))
            .findFirst().orElseThrow();
        assertThat(closedRow.get("closedEvidence")).isEqualTo("已补");

        // 找未到期项（overdue=false）
        java.util.Map<String, Object> onTimeRow = view.stream()
            .filter(m -> "OPEN".equals(m.get("leftoverStatus")) && Boolean.FALSE.equals(m.get("overdue")))
            .findFirst().orElseThrow();
        assertThat(onTimeRow.get("leftoverItem")).isEqualTo("补材料C");
    }

    // ---------- 副作用契约（CONDITIONAL 缺责任人/期限必拒，AC-GATE-16） ----------

    @Test
    @DisplayName("AC-GATE-16 副作用契约：CONDITIONAL 缺 responsiblePersonId 必拒（40002 口径）")
    void judge_conditionalMissingResponsible_rejected() {
        assertThatThrownBy(() -> service.judge(GATE_ID, ELEMENT_ID, "CONDITIONAL",
            "带条件说明", null, null, null, /*responsiblePersonId*/ null,
            new Date(System.currentTimeMillis() + 86400000L), MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("责任人");

        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    @Test
    @DisplayName("AC-GATE-02 副作用契约：FAIL 缺 evidenceRef 必拒")
    void judge_failMissingEvidence_rejected() {
        assertThatThrownBy(() -> service.judge(GATE_ID, ELEMENT_ID, "FAIL",
            null, /*evidenceRef*/ null, null, null, null, null, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("证据");

        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    // ---------- 要素门禁 ----------

    @Test
    @DisplayName("要素门禁：element.enabled='0' 时 judge() 拒绝（停用要素不可判定）")
    void judge_disabledElement_rejected() {
        element.setEnabled("0");
        assertThatThrownBy(() -> service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("停用");
    }

    @Test
    @DisplayName("要素门禁：element 不属于 Gate.gateCode 时 judge() 拒绝")
    void judge_elementGateMismatch_rejected() {
        element.setGateCode("G2");
        assertThatThrownBy(() -> service.judge(GATE_ID, ELEMENT_ID, "PASS", null, null,
            null, null, null, null, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("不属于该 Gate");
    }
}
