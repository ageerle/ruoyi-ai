package org.ruoyi.ipd.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AcceptanceMatrixValidationTest
 *
 * ROOT-R4 P0-8 落地：单测侧守住 acceptance-matrix.json 的 4 个核心约束。
 * 覆盖 AC 范围：acceptance-matrix.json 自身的 schema 合规（不直接对应某条 AC-INC-*，
 * 而是作为「矩阵存在性 + 字段合规」的 meta-AC，对应 ROOT-R4 P0-8 交付物清单第 5 项）。
 *
 * 4 维度硬断言：
 *   1) matrix 文件存在 + JSON 合法 + schema 文件存在
 *   2) 必填字段非空：ac_id / category / title / docRef / status / owner
 *   3) ac_id 唯一 + 格式合规（^AC-(INC|EXT|MIN)-\d+[a-z]?$）
 *   4) status=covered 的行 unitTestClass 必须存在且 linkedCommits ≥ 1
 *
 * 关联工件：
 *   - docs/ipd-系统说明/治理/acceptance-matrix.json（SSOT）
 *   - docs/ipd-系统说明/治理/acceptance-matrix.schema.json
 *   - .claude/helpers/acceptance-matrix-validate.cjs（Node 端双校验）
 *   - .github/workflows/docs-link-check.yml（CI 钩子）
 */
@Tag("dev")
class AcceptanceMatrixValidationTest {

    private static final String MATRIX_REL = "docs/ipd-系统说明/治理/acceptance-matrix.json";
    private static final String SCHEMA_REL = "docs/ipd-系统说明/治理/acceptance-matrix.schema.json";
    private static final Pattern AC_ID_PATTERN = Pattern.compile("^AC-(INC|EXT|MIN|AUTH|AUD|ENV|GATE|GLB|CFG|PROD)-\\d+[a-z]?$");

    private File locateRepoRoot() {
        // src/test/java → ruoyi-modules/ruoyi-ipd/src/test/java
        // 向上 5 级到 ruoyi-ai 仓库根
        File f = new File("").getAbsoluteFile();
        for (int i = 0; i < 6 && f != null; i++) {
            if (new File(f, "pom.xml").exists() && new File(f, "ruoyi-modules").exists()) {
                return f;
            }
            f = f.getParentFile();
        }
        throw new IllegalStateException("无法定位仓库根（未找到 pom.xml + ruoyi-modules）");
    }

    @Test
    @DisplayName("AC-AM-VAL-1: acceptance-matrix.json + schema 文件存在且 JSON 合法")
    void matrixFilesExistAndParse() throws Exception {
        File root = locateRepoRoot();
        File matrix = new File(root, MATRIX_REL);
        File schema = new File(root, SCHEMA_REL);

        assertThat(matrix).as("acceptance-matrix.json 必须存在（ROOT-R4 P0-8 交付物 1）").exists();
        assertThat(schema).as("acceptance-matrix.schema.json 必须存在（ROOT-R4 P0-8 交付物 2）").exists();

        ObjectMapper om = new ObjectMapper();
        JsonNode root1 = om.readTree(matrix);
        JsonNode root2 = om.readTree(schema);
        assertThat(root1.isObject()).isTrue();
        assertThat(root2.isObject()).isTrue();
        assertThat(root1.has("version")).as("必须含 version 字段").isTrue();
        assertThat(root1.has("updated_at")).as("必须含 updated_at 字段").isTrue();
        assertThat(root1.has("rows")).as("必须含 rows 数组").isTrue();
        assertThat(root1.get("rows").isArray()).isTrue();
    }

    @Test
    @DisplayName("AC-AM-VAL-2: rows[].ac_id 必填且唯一 + 格式合规")
    void acIdUniqueAndFormat() throws Exception {
        File root = locateRepoRoot();
        JsonNode rows = new ObjectMapper().readTree(new File(root, MATRIX_REL)).get("rows");
        assertThat(rows.size()).as("样板 ≥ 5 条（owner 决策后批量导入至 237）").isGreaterThanOrEqualTo(5);

        Set<String> seen = new HashSet<>();
        for (JsonNode row : rows) {
            String acId = row.path("ac_id").asText(null);
            assertThat(acId).as("ac_id 必填非空").isNotBlank();
            assertThat(AC_ID_PATTERN.matcher(acId).matches())
                .as("ac_id 必须符合 ^AC-(INC|EXT|MIN)-\\d+[a-z]?$，实际: " + acId)
                .isTrue();
            assertThat(seen.add(acId))
                .as("ac_id 唯一性被破坏: " + acId)
                .isTrue();

            // category 必须与 ac_id 前缀一致
            String category = row.path("category").asText(null);
            String prefix = acId.split("-")[1];
            assertThat(category)
                .as("category=" + category + " 必须等于 ac_id 前缀 " + prefix + "（行 " + acId + "）")
                .isEqualTo(prefix);
        }
    }

    @Test
    @DisplayName("AC-AM-VAL-3: 必填字段 ac_id/category/title/docRef/status/owner 非空")
    void requiredFieldsNonEmpty() throws Exception {
        File root = locateRepoRoot();
        JsonNode rows = new ObjectMapper().readTree(new File(root, MATRIX_REL)).get("rows");
        for (JsonNode row : rows) {
            String acId = row.path("ac_id").asText("<missing>");
            for (String f : new String[]{"ac_id", "category", "title", "docRef", "status", "owner"}) {
                String v = row.path(f).asText(null);
                assertThat(v)
                    .as("行 " + acId + " 字段 " + f + " 必填非空")
                    .isNotBlank();
            }
            String status = row.path("status").asText("");
            assertThat(status).as("status 枚举: " + acId).isIn(
                "covered", "partial", "blocked", "manual", "deprecated"
            );
            String owner = row.path("owner").asText("");
            assertThat(owner).as("owner 枚举: " + acId).isIn(
                "pm", "rd", "qa", "ops", "owner"
            );
        }
    }

    @Test
    @DisplayName("AC-AM-VAL-4: status=covered 的行 unitTestClass 非空 + linkedCommits ≥ 1")
    void coveredRowsHaveEvidence() throws Exception {
        File root = locateRepoRoot();
        JsonNode rows = new ObjectMapper().readTree(new File(root, MATRIX_REL)).get("rows");
        int coveredCount = 0;
        for (JsonNode row : rows) {
            String status = row.path("status").asText("");
            if (!"covered".equals(status)) continue;
            coveredCount++;
            String acId = row.path("ac_id").asText("<missing>");
            String unitTest = row.path("unitTestClass").asText(null);
            assertThat(unitTest)
                .as("status=covered 的行 unitTestClass 必填: " + acId)
                .isNotBlank();
            // unitTestClass FQN → 物理路径
            String relPath = unitTest.replace('.', '/') + ".java";
            File testFile = new File(root, "ruoyi-modules/ruoyi-ipd/src/test/java/" + relPath);
            assertThat(testFile)
                .as("unitTestClass 对应 .java 文件必须存在: " + acId + " → " + unitTest)
                .exists();

            JsonNode commits = row.path("linkedCommits");
            assertThat(commits.isArray())
                .as("linkedCommits 必须是数组: " + acId)
                .isTrue();
            assertThat(commits.size())
                .as("status=covered 需 ≥1 个 commit 证据: " + acId)
                .isGreaterThanOrEqualTo(1);
            for (JsonNode c : commits) {
                String commit = c.asText("");
                assertThat(commit)
                    .as("commit 格式 7-40 位十六进制: " + acId + " → " + commit)
                    .matches("[0-9a-f]{7,40}");
            }
        }
        // 当前样板至少有 1 条 covered（owner 决策后增至更多）
        assertThat(coveredCount)
            .as("样板中至少含 1 条 covered 行")
            .isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("AC-AM-VAL-5: 样板进度 + owner 决策项已登记")
    void sampleProgressAndOwnerDecisionsRegistered() throws Exception {
        File root = locateRepoRoot();
        JsonNode matrix = new ObjectMapper().readTree(new File(root, MATRIX_REL));
        JsonNode rows = matrix.get("rows");
        JsonNode ownerDecisions = matrix.path("owner_decisions_needed");

        assertThat(rows.size())
            .as("样板 ≤ 237（避免 4 文件清单外的扩展）")
            .isLessThanOrEqualTo(237);
        assertThat(ownerDecisions.path("open_count").asInt())
            .as("owner 决策项 open_count ≥ 1（OD-AM-01 ~ 04 必备）")
            .isGreaterThanOrEqualTo(1);
        JsonNode items = ownerDecisions.path("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items.size()).isGreaterThanOrEqualTo(1);

        // 校验每条决策项必含 id/topic/blocker
        Iterator<JsonNode> it = items.elements();
        while (it.hasNext()) {
            JsonNode d = it.next();
            for (String f : new String[]{"id", "topic", "blocker"}) {
                assertThat(d.path(f).asText(null))
                    .as("owner 决策项字段 " + f + " 必填")
                    .isNotBlank();
            }
        }
    }
}
