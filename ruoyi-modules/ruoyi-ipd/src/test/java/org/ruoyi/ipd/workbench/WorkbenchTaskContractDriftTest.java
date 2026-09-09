package org.ruoyi.ipd.workbench;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.service.WorkbenchService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * taskType 数据源契约登记一致性测试（B 批次机制 1，2026-09-08 立）。
 *
 * <p>防「写入侧与查询侧语义不对偶」病根的登记面漂移：
 * <ul>
 *   <li>契约登记文件（docs/ipd-系统说明/workbench-tasktype-契约登记.yaml）的 17 类键集
 *       必须与 {@link WorkbenchService#ALL_TASK_TYPES}（spec 页03:165 权威枚举）完全一致</li>
 *   <li>已实现类的五项必需契约字段（aggregator/table/write_timing/anchor/pending_expr）必须登记齐</li>
 *   <li>已实现类必须有聚合器源文件，且源文件内含 {@code return "<taskType>"} 字面量</li>
 *   <li>关键状态常量（包私有防漂移常量）与登记文本交联——常量值必须出现在登记文件中</li>
 * </ul>
 *
 * <p>登记文件刻意用零依赖的轻量行扫描解析（顶层键 + 两格缩进字段），不引 snakeyaml——
 * 本仓 IPD 模块 pom 未声明该依赖，不为测试新增编译期依赖。新增一类任务 = 先登记契约再写聚合器，
 * 本测试即门禁。
 */
@Tag("dev")
class WorkbenchTaskContractDriftTest {

    private static final String YAML_REL = "../../docs/ipd-系统说明/workbench-tasktype-契约登记.yaml";
    private static final String YAML_REL_FROM_ROOT = "docs/ipd-系统说明/workbench-tasktype-契约登记.yaml";
    private static final String YAML_ABS = "/Users/mac/Documents/ruoyi-ai/docs/ipd-系统说明/workbench-tasktype-契约登记.yaml";
    private static final String AGGREGATOR_DIR = "src/main/java/org/ruoyi/ipd/workbench/";

    private static String yamlText;
    private static Map<String, Map<String, String>> contract; // taskType -> field -> value

    @BeforeAll
    static void loadContract() throws IOException {
        Path path = Path.of(YAML_REL);
        if (!Files.isRegularFile(path)) {
            path = Path.of(YAML_REL_FROM_ROOT);
        }
        if (!Files.isRegularFile(path)) {
            path = Path.of(YAML_ABS);
        }
        assertThat(Files.isRegularFile(path))
            .as("契约登记文件必须存在（B 批次机制 1）: 尝试过 %s / %s / %s", YAML_REL, YAML_REL_FROM_ROOT, YAML_ABS)
            .isTrue();
        yamlText = Files.readString(path);
        contract = parseTopLevelEntries(yamlText);
    }

    /** 轻量行扫描：顶层键（无缩进 + 冒号结尾）与两格缩进字段。注释行/分隔线跳过。 */
    private static Map<String, Map<String, String>> parseTopLevelEntries(String text) {
        Map<String, Map<String, String>> out = new LinkedHashMap<>();
        String current = null;
        for (String rawLine : text.split("\n", -1)) {
            String line = rawLine.replaceFirst("#.*$", "").trim();
            if (line.isEmpty()) {
                continue;
            }
            if (!rawLine.startsWith(" ") && line.endsWith(":")) {
                current = line.substring(0, line.length() - 1);
                out.putIfAbsent(current, new LinkedHashMap<>());
            } else if (current != null && rawLine.startsWith("  ") && !rawLine.startsWith("   ")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    out.get(current).put(line.substring(0, colon).trim(),
                        line.substring(colon + 1).trim());
                }
            }
        }
        out.remove("meta");
        return out;
    }

    @Test
    @DisplayName("契约键集 == WorkbenchService.ALL_TASK_TYPES（spec 页03:165 权威 17 类，双向零差）")
    void contractKeysMatchAllTaskTypes() throws Exception {
        Field field = WorkbenchService.class.getDeclaredField("ALL_TASK_TYPES");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> all = (List<String>) field.get(null);
        assertThat(all).hasSize(17);
        Set<String> codeSide = Set.copyOf(all);
        Set<String> yamlSide = contract.keySet();
        assertThat(yamlSide).as("登记文件多出的键").containsExactlyInAnyOrderElementsOf(codeSide);
        assertThat(codeSide).as("代码枚举多出的键").containsExactlyInAnyOrderElementsOf(yamlSide);
    }

    @Test
    @DisplayName("已实现恰 9 类；每类五项必需契约字段登记齐")
    void implementedEntriesHaveRequiredFields() {
        Map<String, Map<String, String>> implemented = contract.entrySet().stream()
            .filter(e -> "true".equals(e.getValue().get("implemented")))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        assertThat(implemented).as("B 批次立卡时已实现 9 类").hasSize(9);
        for (Map.Entry<String, Map<String, String>> e : implemented.entrySet()) {
            for (String required : List.of("aggregator", "table", "write_timing", "anchor", "pending_expr")) {
                assertThat(e.getValue().get(required))
                    .as("taskType=%s 缺契约字段 %s（先补登记）", e.getKey(), required)
                    .isNotNull()
                    .isNotEmpty();
            }
            assertThat(List.of("ALLOCATE", "SUBMIT"))
                .as("taskType=%s write_timing 值域 ALLOCATE|SUBMIT", e.getKey())
                .contains(e.getValue().get("write_timing"));
        }
    }

    @Test
    @DisplayName("已实现类的聚合器源文件存在且含 return \"<taskType>\" 字面量")
    void implementedAggregatorsExistWithLiteral() throws IOException {
        for (Map.Entry<String, Map<String, String>> e : contract.entrySet()) {
            if (!"true".equals(e.getValue().get("implemented"))) {
                continue;
            }
            Path src = Path.of(AGGREGATOR_DIR + e.getValue().get("aggregator") + ".java");
            if (!Files.isRegularFile(src)) {
                src = Path.of("/Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-ipd/" + AGGREGATOR_DIR
                    + e.getValue().get("aggregator") + ".java");
            }
            assertThat(Files.isRegularFile(src))
                .as("taskType=%s 登记聚合器源文件必须存在: %s", e.getKey(), src)
                .isTrue();
            assertThat(Files.readString(src))
                .as("taskType=%s 聚合器必须返回登记的字面量", e.getKey())
                .contains("return \"" + e.getKey() + "\"");
        }
    }

    @Test
    @DisplayName("关键状态常量与登记文本交联（防常量值与登记漂移）")
    void keyConstantsAppearInContract() {
        // 同包包私有防漂移常量（各聚合器自带，与配套 Service 同值）——值必须出现在登记文本中
        Map<String, String> constants = Map.of(
            "KeyGateAggregator.GATE_PENDING", KeyGateAggregator.GATE_PENDING,
            "KeyGateArbitrationAggregator.GATE_REJECTED", KeyGateArbitrationAggregator.GATE_REJECTED,
            "KeyGateArbitrationAggregator.ST_ACTIVE_PROJECT", KeyGateArbitrationAggregator.ST_ACTIVE_PROJECT,
            "KpiFillAggregator.KF_EDITING", KpiFillAggregator.KF_EDITING,
            "HandoverAggregator.HS_DRAFT", HandoverAggregator.HS_DRAFT,
            "HandoverAggregator.HS_CONFIRMED", HandoverAggregator.HS_CONFIRMED,
            "CloseoutAggregator.TYPE_SELF", CloseoutAggregator.TYPE_SELF,
            "CloseoutAggregator.TYPE_LEADER", CloseoutAggregator.TYPE_LEADER);
        for (Map.Entry<String, String> e : constants.entrySet()) {
            assertThat(e.getValue()).as("%s 值", e.getKey()).isNotEmpty();
            assertThat(yamlText)
                .as("%s=%s 必须出现在契约登记文本中（改常量须同步登记）", e.getKey(), e.getValue())
                .contains(e.getValue());
        }
    }
}
