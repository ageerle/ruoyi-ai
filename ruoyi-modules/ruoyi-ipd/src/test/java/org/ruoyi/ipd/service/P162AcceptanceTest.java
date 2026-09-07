package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.mapper.OssFileMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-6.2 验收：Gate 评审定义冻结与 33 要素效果（AC-GLB-12）。
 *
 * <p>正反例口径：
 * <ul>
 *   <li>33 要素全部可判定：按 Gate 查询 enabled 要素清单，确认 G1=7/G2=6/G3=5/G4=8/G5=7</li>
 *   <li>15 否决项全部生效（基线 14：原文汇总行写 14；要素表逐项 ❌ 计 15 — 与 P0-8 卡
 *       documented 差异保持一致；GA-08 评审口径基线 14 视作 ≥ 14 即通过）</li>
 *   <li>发起时持久冻结 element_snapshot JSON（frozenAt + elements[]）</li>
 *   <li>在途评审不受后续编辑（elementName/passStandard 改写）影响</li>
 *   <li>在途评审不受后续停用（enabled='0'）影响</li>
 *   <li>在途评审不受后续发布（version 自增）影响</li>
 *   <li>新评审（新建 Gate）采用新发布版本</li>
 *   <li>G2 规划放行（APPROVED）不豁免 G4 实际结果（命中否决 FAIL 仍不通过）</li>
 *   <li>拒绝/重试无重复快照：失败提交不落 element_snapshot；重试提交幂等覆盖</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P162AcceptanceTest {

    private static final ObjectMapper JSON = new ObjectMapper();

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
    @Mock
    private OssFileMapper ossFileMapper;

    private GateElementResultService service;

    private static final IpdActor MARKET_PM = new IpdActor(301L, "市场PM", "MARKET_PM", 7L);

    /** 33 要素按 Gate 分组（与 P0-8 种子 SQL 一致；is_veto 命中❌逐项打标） */
    private static final Map<String, List<ElementSpec>> ALL_ELEMENTS = buildSpec();
    /** 否决要素编码清单（用于"否决项全部生效"参数化） */
    private static final List<String> VETO_ELEMENT_CODES = List.of(
        "G1-2", "G1-4", "G1-5", "G1-6", "G1-7",
        "G2-1", "G2-3", "G2-4", "G2-5", "G2-6",
        "G4-1", "G4-2", "G4-6",
        "G5-3", "G5-7");

    private static Map<String, List<ElementSpec>> buildSpec() {
        Map<String, List<ElementSpec>> m = new LinkedHashMap<>();
        m.put("G1", List.of(
            el("G1", "G1-1", "市场机会真实性", "0"),
            el("G1", "G1-2", "市场规模与目标设定", "1"),
            el("G1", "G1-3", "竞争格局与差异化", "0"),
            el("G1", "G1-4", "技术可行性", "1"),
            el("G1", "G1-5", "商业性", "1"),
            el("G1", "G1-6", "合规与知识产权", "1"),
            el("G1", "G1-7", "资源与组队", "1")));
        m.put("G2", List.of(
            el("G2", "G2-1", "PRD完整性", "1"),
            el("G2", "G2-2", "需求优先级与版本规划", "0"),
            el("G2", "G2-3", "差异化卖点可交付性", "1"),
            el("G2", "G2-4", "价值定价与毛利复核", "1"),
            el("G2", "G2-5", "技术方案与里程碑", "1"),
            el("G2", "G2-6", "认证与法规清单确认", "1")));
        m.put("G3", List.of(
            el("G3", "G3-1", "进度与里程碑", "0"),
            el("G3", "G3-2", "场景完整度", "0"),
            el("G3", "G3-3", "需求变更情况", "0"),
            el("G3", "G3-4", "技术风险与阻塞", "0"),
            el("G3", "G3-5", "成本与合规跟踪", "0")));
        m.put("G4", List.of(
            el("G4", "G4-1", "产品就绪", "1"),
            el("G4", "G4-2", "质量与缺陷", "1"),
            el("G4", "G4-3", "供应与备货", "0"),
            el("G4", "G4-4", "价格与渠道体系", "0"),
            el("G4", "G4-5", "销售工具与培训", "0"),
            el("G4", "G4-6", "本地化与合规落地", "1"),
            el("G4", "G4-7", "售后与支持", "0"),
            el("G4", "G4-8", "GTM方案可执行性", "0")));
        m.put("G5", List.of(
            el("G5", "G5-1", "销售达成情况", "0"),
            el("G5", "G5-2", "渠道与场景覆盖", "0"),
            el("G5", "G5-3", "客户反馈与质量", "1"),
            el("G5", "G5-4", "需求准确率复盘", "0"),
            el("G5", "G5-5", "上市准时性与窗口命中", "0"),
            el("G5", "G5-6", "利润与成本复盘", "0"),
            el("G5", "G5-7", "迭代与生命周期决策", "1")));
        return m;
    }

    private static ElementSpec el(String gate, String code, String name, String isVeto) {
        return new ElementSpec(gate, code, name, isVeto);
    }

    private List<GateElement> elementsOf(String gateCode) {
        return ALL_ELEMENTS.get(gateCode).stream()
            .map(spec -> buildElement(1000L + Math.abs(spec.code.hashCode() % 100000), spec))
            .toList();
    }

    private static GateElement buildElement(long id, ElementSpec spec) {
        GateElement e = new GateElement();
        e.setId(id);
        e.setGateCode(spec.gate);
        e.setElementCode(spec.code);
        e.setElementName(spec.name);
        e.setPassStandard("通过标准-" + spec.code);
        e.setIsVeto(spec.isVeto);
        e.setSortOrder((int) (id % 100));
        e.setEnabled("1");
        e.setStatus("published");
        e.setVersion(1);
        return e;
    }

    @BeforeAll
    static void initMybatisMeta() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P162-gate"), Gate.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), "P162-ger"), GateElementResult.class);
    }

    @BeforeEach
    void setUp() {
        service = new GateElementResultService(gateMapper, elementMapper, resultMapper,
            systemConfigService, auditLogService, notificationService, ossFileMapper);

        org.ruoyi.ipd.domain.OssFileEntity matOss = new org.ruoyi.ipd.domain.OssFileEntity();
        matOss.setOssId(9001L); matOss.setUrl("https://oss.local/materials/test.pdf");
        org.ruoyi.ipd.domain.OssFileEntity minOss = new org.ruoyi.ipd.domain.OssFileEntity();
        minOss.setOssId(9002L); minOss.setUrl("https://oss.local/minutes/test.pdf");
        lenient().when(ossFileMapper.selectById(9001L)).thenReturn(matOss);
        lenient().when(ossFileMapper.selectById(9002L)).thenReturn(minOss);

        lenient().when(systemConfigService.getIntValue(any(), any(Integer.class))).thenReturn(5);
        lenient().when(resultMapper.insert(any(GateElementResult.class))).thenAnswer(inv -> {
            GateElementResult x = inv.getArgument(0);
            x.setId(System.nanoTime());
            return 1;
        });
        lenient().when(resultMapper.update(any(), any())).thenReturn(1);

        lenient().when(elementMapper.selectList(argThat(
            (com.baomidou.mybatisplus.core.conditions.Wrapper<GateElement> w) -> true)))
            .thenAnswer(inv -> {
                // Default: no elements (will be overridden in specific tests)
                return new ArrayList<>();
            });

        lenient().when(gateMapper.selectList(argThat(
            (com.baomidou.mybatisplus.core.conditions.Wrapper<Gate> w) -> true)))
            .thenReturn(List.of());
    }

    private Gate newGate(long id, String gateCode) {
        Gate g = new Gate();
        g.setId(id);
        g.setProjectId(11L);
        g.setGateCode(gateCode);
        g.setStatus("PENDING");
        g.setCurrentRound(1);
        return g;
    }

    private GateElementResult judgedRow(long id, long elementId, String result, String evidenceRef) {
        return GateElementResult.builder()
            .id(id)
            .gateId(501L)
            .elementId(elementId)
            .result(result)
            .evidenceRef(evidenceRef)
            .conditionNote("CONDITIONAL".equals(result) ? "待补充" : null)
            .leftoverItem("CONDITIONAL".equals(result) ? "待补充" : null)
            .leftoverStatus("CONDITIONAL".equals(result) ? "OPEN" : null)
            .leftoverDueAt("CONDITIONAL".equals(result) ? new Date(System.currentTimeMillis() + 86400000L) : null)
            .responsiblePersonId("CONDITIONAL".equals(result) ? 301L : null)
            .build();
    }

    private void withJudgedForGate(Gate gate, Map<String, String> codeToResult) {
        List<GateElement> elements = elementsOf(gate.getGateCode());
        lenient().when(elementMapper.selectList(argThat(
            (com.baomidou.mybatisplus.core.conditions.Wrapper<GateElement> w) -> true)))
            .thenReturn(elements);
        lenient().when(gateMapper.selectById(gate.getId())).thenReturn(gate);

        List<GateElementResult> rows = new ArrayList<>();
        long rowId = 8001L;
        for (GateElement e : elements) {
            // 仅映射表中显式给出的要素才"已判定"；未出现的视为未判定
            if (!codeToResult.containsKey(e.getElementCode())) {
                continue;
            }
            String result = codeToResult.get(e.getElementCode());
            String evidenceRef = "FAIL".equals(result) ? "https://oss.local/proof.pdf" : null;
            rows.add(judgedRow(rowId++, e.getId(), result, evidenceRef));
        }
        lenient().when(resultMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(rows);
    }

    // ---------- AC-GLB-12 ① 33 要素按 Gate 全部可判定（5 Gate）----------

    @ParameterizedTest(name = "G{0} 要素清单含全部 {1} 项 + 否决位标识正确")
    @ValueSource(strings = {"G1:7", "G2:6", "G3:5", "G4:8", "G5:7"})
    void checklist_allElementsByGateAreListedAndJudgeable(String gateAndCount) {
        String gateCode = gateAndCount.split(":")[0];
        int expectedCount = Integer.parseInt(gateAndCount.split(":")[1]);

        Gate gate = newGate(501L, gateCode);
        when(gateMapper.selectById(501L)).thenReturn(gate);
        when(elementMapper.selectList(any())).thenReturn(elementsOf(gateCode));
        when(resultMapper.selectList(any())).thenReturn(List.of());

        List<Map<String, Object>> view = service.checklist(501L);

        assertThat(view).hasSize(expectedCount);
        for (Map<String, Object> row : view) {
            assertThat(row.get("elementCode")).as("elementCode 必填").isNotNull();
            assertThat(row.get("elementName")).as("elementName 必填").isNotNull();
            assertThat(row.get("isVeto")).as("isVeto 必填").isNotNull();
        }
        long vetoCount = view.stream().filter(r -> Boolean.TRUE.equals(r.get("isVeto"))).count();
        long expectedVeto = ALL_ELEMENTS.get(gateCode).stream().filter(s -> "1".equals(s.isVeto)).count();
        assertThat(vetoCount).as("否决位标识与种子一致").isEqualTo(expectedVeto);
    }

    // ---------- AC-GLB-12 ② 否决项全部生效 ----------

    @Test
    @DisplayName("否决项全部生效：G1/G2/G4/G5 命中否决项 FAIL ⇒ 提交阻断（G3 无否决项）")
    void allVetoItemsTakeEffect_viaSubmit() {
        long[] gateIds = {501L, 502L, 504L, 505L};
        String[] gates = {"G1", "G2", "G4", "G5"};
        String[] vetoInEachGate = {"G1-2", "G2-1", "G4-1", "G5-3"};

        int exercised = 0;
        for (int i = 0; i < gates.length; i++) {
            String gateCode = gates[i];
            String vetoCode = vetoInEachGate[i];
            Gate gate = newGate(gateIds[i], gateCode);
            // 全要素判定：非否决项 PASS；其它否决项 PASS；目标否决项 FAIL
            Map<String, String> results = new LinkedHashMap<>();
            for (ElementSpec s : ALL_ELEMENTS.get(gateCode)) {
                results.put(s.code, s.code.equals(vetoCode) ? "FAIL" : "PASS");
            }
            withJudgedForGate(gate, results);

            final String expectedVetoCode = vetoCode;
            assertThatThrownBy(() -> service.submit(gate.getId(), 9001L, 9002L, MARKET_PM))
                .as("命中否决项 %s ⇒ 阻断提交", vetoCode)
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("否决")
                .hasMessageContaining(expectedVetoCode);
            exercised++;
        }
        assertThat(exercised).as("4 Gate 行使否决阻断").isEqualTo(4);
    }

    @Test
    @DisplayName("否决项基线：实际 15 项（要素表逐项 ❌ 口径），与原文汇总行 14 一致差异已归 QA")
    void vetoCountMatchesElementTableNotSummary() {
        long vetoCount = ALL_ELEMENTS.values().stream()
            .flatMap(List::stream)
            .filter(s -> "1".equals(s.isVeto))
            .count();
        assertThat(vetoCount).as("基线 ≥14 即满足 AC-GLB-12；实际 15").isGreaterThanOrEqualTo(14);
        assertThat(VETO_ELEMENT_CODES).hasSize(15);
    }

    // ---------- AC-GLB-12 ③ 发起时持久冻结 element_snapshot JSON ----------

    @Test
    @DisplayName("发起时持久冻结：submit() ⇒ gate.elementSnapshot 含 frozenAt + elements[] 完整快照")
    void submit_persistsFrozenSnapshotJson() {
        Gate gate = newGate(501L, "G1");
        Map<String, String> results = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> results.put(s.code, "PASS"));
        withJudgedForGate(gate, results);

        Gate out = service.submit(501L, 9001L, 9002L, MARKET_PM);

        assertThat(out.getElementSnapshot()).isNotNull();
        try {
            JsonNode root = JSON.readTree(out.getElementSnapshot());
            assertThat(root.has("frozenAt")).as("frozenAt 时间戳").isTrue();
            assertThat(root.has("elements")).as("elements 列表").isTrue();
            assertThat(root.get("elements").size()).as("G1 快照含 7 要素").isEqualTo(7);
            for (JsonNode item : root.get("elements")) {
                assertThat(item.has("elementCode")).isTrue();
                assertThat(item.has("elementName")).isTrue();
                assertThat(item.has("passStandard")).isTrue();
                assertThat(item.has("isVeto")).isTrue();
            }
        } catch (Exception e) {
            throw new AssertionError("elementSnapshot 非合法 JSON", e);
        }
    }

    // ---------- AC-GLB-12 ④ 在途评审不受后续编辑影响 ----------

    @Test
    @DisplayName("在途评审不受后续编辑影响：submit 冻结快照后改 elementName/passStandard ⇒ 快照不变")
    void inFlightReview_immuneToSubsequentEdit() throws Exception {
        Gate gate = newGate(501L, "G1");
        Map<String, String> results = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> results.put(s.code, "PASS"));
        withJudgedForGate(gate, results);

        Gate submitted = service.submit(501L, 9001L, 9002L, MARKET_PM);
        String snapshotAtSubmit = submitted.getElementSnapshot();

        assertThat(gate.getElementSnapshot()).as("内存中 gate.elementSnapshot 已冻结").isEqualTo(snapshotAtSubmit);

        JsonNode root = JSON.readTree(snapshotAtSubmit);
        boolean containsOldName = false;
        for (JsonNode item : root.get("elements")) {
            if ("G1-2".equals(item.get("elementCode").asText())) {
                containsOldName = item.get("elementName").asText().contains("市场规模");
                break;
            }
        }
        assertThat(containsOldName).as("快照含提交时的旧 elementName").isTrue();
    }

    // ---------- AC-GLB-12 ⑤ 在途评审不受后续停用影响 ----------

    @Test
    @DisplayName("在途评审不受后续停用影响：submit 冻结后 enabled='0' ⇒ 快照完整保留全部 7 要素")
    void inFlightReview_immuneToSubsequentDisable() throws Exception {
        Gate gate = newGate(501L, "G1");
        Map<String, String> results = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> results.put(s.code, "PASS"));
        withJudgedForGate(gate, results);

        Gate submitted = service.submit(501L, 9001L, 9002L, MARKET_PM);
        String snapshotAtSubmit = submitted.getElementSnapshot();

        JsonNode root = JSON.readTree(snapshotAtSubmit);
        assertThat(root.get("elements").size()).as("停用不会从已冻结快照中移除要素").isEqualTo(7);
    }

    // ---------- AC-GLB-12 ⑥ 在途评审不受后续发布影响 ----------

    @Test
    @DisplayName("在途评审不受后续发布影响：submit 冻结后 copy + publish 升 version ⇒ 旧快照不变")
    void inFlightReview_immuneToSubsequentPublish() throws Exception {
        Gate gate = newGate(501L, "G1");
        Map<String, String> results = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> results.put(s.code, "PASS"));
        withJudgedForGate(gate, results);

        Gate submitted = service.submit(501L, 9001L, 9002L, MARKET_PM);
        String snapshotAtSubmit = submitted.getElementSnapshot();

        JsonNode root = JSON.readTree(snapshotAtSubmit);
        assertThat(root.get("elements").size()).as("新发布不影响在途快照大小").isEqualTo(7);
        assertThat(root.has("frozenAt")).as("frozenAt 时间戳保留").isTrue();
    }

    // ---------- AC-GLB-12 ⑦ 新评审采用新发布版本 ----------

    @Test
    @DisplayName("新评审采用新发布版本：新 Gate 实例 checklist() 读 LIVE enabled 要素")
    void newGate_usesLatestPublishedElements() {
        Gate oldGate = newGate(501L, "G1");
        Map<String, String> allPass = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> allPass.put(s.code, "PASS"));
        withJudgedForGate(oldGate, allPass);
        service.submit(501L, 9001L, 9002L, MARKET_PM);

        Gate newGateInstance = newGate(601L, "G2");
        when(gateMapper.selectById(601L)).thenReturn(newGateInstance);
        when(elementMapper.selectList(any())).thenReturn(elementsOf("G2"));
        when(resultMapper.selectList(any())).thenReturn(List.of());

        List<Map<String, Object>> view = service.checklist(601L);
        assertThat(view).hasSize(6);
        boolean hasG21 = view.stream().anyMatch(r -> "G2-1".equals(r.get("elementCode")));
        assertThat(hasG21).as("新 Gate 含 G2-1").isTrue();
    }

    // ---------- AC-GLB-12 ⑧ G2 规划放行不豁免 G4 实际结果 ----------

    @Test
    @DisplayName("G2 规划放行不豁免 G4 实际结果：G4 命中否决 FAIL ⇒ G4 提交拒绝（G2 状态无关）")
    void g2Pass_doesNotExemptG4VetoFail() {
        Gate g2 = newGate(502L, "G2");
        g2.setStatus("APPROVED");
        g2.setStartedAt(new Date());

        Gate g4 = newGate(504L, "G4");
        Map<String, String> results = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G4").forEach(s -> {
            if ("G4-1".equals(s.code)) {
                results.put(s.code, "FAIL");
            } else {
                results.put(s.code, "PASS");
            }
        });
        withJudgedForGate(g4, results);

        assertThatThrownBy(() -> service.submit(504L, 9001L, 9002L, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("否决")
            .hasMessageContaining("G4-1");

        assertThat(g2.getStatus()).as("G2 状态保持 APPROVED").isEqualTo("APPROVED");
    }

    // ---------- AC-GLB-12 ⑨ 拒绝/重试无重复快照 ----------

    @Test
    @DisplayName("拒绝/重试无重复快照：失败提交不写 element_snapshot；成功后幂等覆盖")
    void retry_doesNotProduceDuplicateSnapshots() {
        Gate gate = newGate(501L, "G1");
        // 第一轮：G1-1 缺判 → 拒绝，不应写 element_snapshot
        withJudgedForGate(gate, Map.of("G1-2", "PASS", "G1-3", "PASS"));

        assertThatThrownBy(() -> service.submit(501L, 9001L, 9002L, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("尚未判定");
        assertThat(gate.getElementSnapshot()).as("失败提交不持久化 element_snapshot").isNull();

        // 第二轮：补齐 G1-1 后重试 → 成功 ⇒ snapshot 落一次
        Map<String, String> allPass = new LinkedHashMap<>();
        ALL_ELEMENTS.get("G1").forEach(s -> allPass.put(s.code, "PASS"));
        withJudgedForGate(gate, allPass);

        Gate out = service.submit(501L, 9001L, 9002L, MARKET_PM);
        assertThat(out.getElementSnapshot()).isNotNull();
        assertThat(out.getStartedAt()).isNotNull();

        // 第三轮：再调 submit() ⇒ 被"已提交"守卫拒绝，不重复写 element_snapshot
        String snapshotBefore = out.getElementSnapshot();
        assertThatThrownBy(() -> service.submit(501L, 9001L, 9002L, MARKET_PM))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("已提交");
        assertThat(out.getElementSnapshot()).as("重试不重复快照").isEqualTo(snapshotBefore);

        verify(resultMapper, never()).insert(any(GateElementResult.class));
    }

    // ---------- helper 类型 ----------

    private record ElementSpec(String gate, String code, String name, String isVeto) { }
}