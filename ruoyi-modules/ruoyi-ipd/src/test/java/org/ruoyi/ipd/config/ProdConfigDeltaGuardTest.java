package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.qa.GuardSourceUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1（owner 2026-09-05 指令项3）：application-prod.yml 四项覆盖的守门测试。
 *
 * <p>owner 列的 4 项在 prod 里的真实形态并不相同（本测试即按实测事实写成断言）：
 * <ol>
 *   <li>{@code demo.enabled=false}：prod 无该段，靠父 application.yml 继承（父已 false，见 R8-P0-2）；</li>
 *   <li>{@code sa-token} 密钥占位：同上，父为 {@code ${SA_TOKEN_JWT_SECRET_KEY:}}，由
 *       {@link CredentialLiteralGuardTest} 第 4 条守住 prod 侧不得写默认值；</li>
 *   <li>{@code springdoc} 关闭：<b>父为 true 且 prod 无覆盖 → 唯一真缺项</b>，交付为
 *       {@code docs/ipd-系统说明/验收/application-prod-owner-item3-delta.patch}，由用户/CI 手动 apply
 *       （sensitive-field-guard 阻断智能体直写 application-prod.yml）；</li>
 *   <li>actuator 收窄：prod 无 management 段，靠父继承到 {@code health,info,metrics} + {@code WHEN_AUTHORIZED}，
 *       正向断言在 {@link ActuatorNarrowTest}，本处只守「prod 不得反向放宽」。</li>
 *   <li><b>SEC-NEW-MED-3（第 5 条）</b>：prod 数据源凭证必须是 {@code ${SPRING_DATASOURCE_*:}} env 占位，
 *       不得回滚为 root/root 字面量——2026-09-06 16:00 曾发生「patch 应用后被工作区回滚」事故，此条即回滚守卫；</li>
 *   <li><b>SEC-NEW-MED-3（第 6 条）</b>：MED-3 归档 patch 声称已应用，其新增语义行必须真实落盘于
 *       application-prod.yml（防「归档在、落盘无」的双重态）；归档件被清理后交由第 5 条独立把关。</li>
 * </ol>
 *
 * <p>第 4 条断言（patch 头部 + hunk 上下文自校验）是本轮踩过的坑：用 {@code git diff --no-index} 加
 * {@code sed} 改写路径时把 git 的 {@code a/}、{@code b/} 前缀里的斜杠一起吃掉，产出
 * {@code --- aruoyi-admin/...} 这种永远 apply 不上的畸形头。既然交付物是 patch 而不是代码，
 * 「能不能 apply」本身就是交付质量的一部分，不能只靠人眼。
 *
 * <p>注意：本类只针对<b>自己的</b> delta patch 立「落地后须归档」的规矩。兄弟会话的
 * {@code application-prod-batch3.patch} 已被 76888bbf 消费、现 {@code git apply --check} rc=1，
 * 若把规则泛化成扫描所有 prod patch 会立刻因他人交付物变红，属 owner 裁决范围，不在此越权。
 *
 * <p>设计要点：自适应 git root（Surefire working dir = 模块根；IDE = 项目根），同 {@code ActuatorNarrowTest} 惯例。
 */
@Tag("dev")
@DisplayName("P1 项3: application-prod.yml 四项覆盖守卫")
class ProdConfigDeltaGuardTest {

    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path APP_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml");
    private static final Path PROD_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-prod.yml");
    private static final Path DELTA_PATCH =
        REPO_ROOT.resolve("docs/ipd-系统说明/验收/application-prod-owner-item3-delta.patch");
    private static final Path MED3_PATCH_ARCHIVE =
        REPO_ROOT.resolve("docs/ipd-系统说明/验收/application-prod-med3-delta.patch.applied-20260905");

    private static final String PROD_YML_PATH = "ruoyi-admin/src/main/resources/application-prod.yml";

    private static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    @Test
    @DisplayName("1) 父基线 demo.enabled=false（项3#1 的继承前提，父被翻回 true 即破功）")
    void parentDemoDisabledSoProdInheritsSafe() throws IOException {
        String enabled = flagValueOrNull(Files.readString(APP_YML), "demo:", 3);
        assertThat(enabled).as("父 application.yml 的 demo.enabled 键必须存在（被改名/删除即失守）").isNotNull();
        assertThat(enabled).as("父 demo.enabled 必须是 false，prod 才有安全继承可言").isEqualTo("false");
    }

    @Test
    @DisplayName("2) prod 不得放宽 actuator（无 management 段=继承收窄；出现则禁 '*' / ALWAYS）")
    void prodDoesNotWidenActuator() throws IOException {
        // 2026-09-08 AM-GUARD：剔 # 注释行后再断言（与同文件第 5 条同口径，
        // 注释掉的示例段不是生效配置，不得误伤；正向断言也不能被注释文字满足）
        String prod = GuardSourceUtils.stripYamlComments(Files.readString(PROD_YML));
        assertThat(prod).as("prod 不得把 actuator 端点重新放宽为 '*'").doesNotContain("include: '*'");
        assertThat(prod).as("prod 不得把 health show-details 放宽为 ALWAYS")
            .doesNotContain("show-details: ALWAYS");
        // 反向确认继承前提仍然成立：父基线确实是收窄集（正向细粒度断言见 ActuatorNarrowTest）
        String parent = GuardSourceUtils.stripYamlComments(Files.readString(APP_YML));
        assertThat(parent).contains("include: health,info,metrics");
    }

    @Test
    @DisplayName("3) 项3#3 springdoc 关闭：prod 已覆盖为 false，或缺口必须已有登记在案的 delta patch")
    void prodSpringdocGapMustBeTracked() throws IOException {
        // prod 目前根本没有 api-docs 段（值靠父 true 继承）→ 取不到即为「缺口尚未闭合」，不能当成断言异常
        boolean prodDisablesApiDocs = "false".equals(flagValueOrNull(Files.readString(PROD_YML), "api-docs:", 4));
        if (prodDisablesApiDocs) {
            // 已落地：唯一允许的收尾形态是 patch 不再以「待应用」姿态躺在交付目录里
            assertThat(Files.exists(DELTA_PATCH))
                .as("prod 已含 springdoc.api-docs.enabled=false，delta patch 应删除或改名归档，"
                    + "否则会误导下一个执行方重复 apply（batch3.patch 即前车之鉴）").isFalse();
        } else {
            assertThat(Files.exists(DELTA_PATCH))
                .as("prod 既没关 springdoc、也没有可交付的 patch → 缺口失控（父 true 会一路带到生产）")
                .isTrue();
        }
    }

    @Test
    @DisplayName("4) delta patch 自校验：头部路径合法 + 每个 hunk 上下文与目标文件逐行对得上")
    void deltaPatchIsActuallyApplicable() throws IOException {
        String prod = Files.readString(PROD_YML);
        boolean alreadyApplied = "false".equals(flagValueOrNull(prod, "api-docs:", 4));
        if (alreadyApplied || !Files.exists(DELTA_PATCH)) {
            return; // 交给第 3 条判定，避免同一事实两处红
        }
        List<String> patch = Files.readAllLines(DELTA_PATCH);

        // 4.1 头部三件套必须存在，且路径与仓库真实文件一致（防 sed 粘连出 aruoyi-admin/... 这类畸形头）
        String gitHeader = patch.stream().filter(l -> l.startsWith("diff --git ")).findFirst()
            .orElseThrow(() -> new AssertionError("patch 缺少 'diff --git' 头"));
        assertThat(gitHeader.split(" "))
            .as("diff --git 头必须是 a/<路径> b/<路径> 两段").hasSizeGreaterThanOrEqualTo(4);
        assertThat(gitHeader).contains("a/" + PROD_YML_PATH).contains("b/" + PROD_YML_PATH);
        assertThat(patch).anyMatch(l -> l.equals("--- a/" + PROD_YML_PATH));
        assertThat(patch).anyMatch(l -> l.equals("+++ b/" + PROD_YML_PATH));
        assertThat(Files.exists(REPO_ROOT.resolve(PROD_YML_PATH))).as("目标文件必须存在").isTrue();

        // 4.2 纯 Java 复算 git apply --check 的核心判据：删除行/上下文行必须与目标文件当前内容完全一致
        List<String> target = Files.readAllLines(REPO_ROOT.resolve(PROD_YML_PATH));
        List<String> mismatched = new ArrayList<>();
        int i = 0;
        while (i < patch.size()) {
            String line = patch.get(i);
            if (!line.startsWith("@@")) {
                i++;
                continue;
            }
            int[] span = parseOldRange(line);
            int cursor = span[0] - 1;
            int consumed = 0;
            i++;
            while (i < patch.size()
                && !patch.get(i).startsWith("@@") && !patch.get(i).startsWith("diff --git ")) {
                String body = patch.get(i);
                if (body.startsWith("+")) {
                    // 纯新增行不参与目标文件比对
                } else if (body.startsWith("\\") || body.isEmpty()) {
                    // "\ No newline at end of file" 等元信息
                } else {
                    String expected = body.substring(1);
                    String actual = cursor < target.size() ? target.get(cursor) : "<文件已结束>";
                    if (!expected.equals(actual)) {
                        mismatched.add("目标第 " + (cursor + 1) + " 行应为 [" + expected + "] 实为 [" + actual + "]");
                    }
                    cursor++;
                    consumed++;
                }
                i++;
            }
            assertThat(consumed)
                .as("hunk %s 声明的旧行数与实际比对数须一致", line).isEqualTo(span[1]);
        }
        assertThat(mismatched)
            .as("patch 若与目标文件当前内容错位，git apply 必失败（等价于 batch3.patch 的 rc=1 现场）")
            .isEmpty();
    }

    @Test
    @DisplayName("5) SEC-NEW-MED-3：prod 数据源凭证必须为 env 占位，禁止回滚 root/root 字面量")
    void prodDatasourceCredentialsMustBeEnvPlaceholders() throws IOException {
        String prod = Files.readString(PROD_YML);
        // 只查有效行：注释掉的 agent 段示例（L71-72）不是生效配置，不得误伤（否则红的是噪音不是契约）
        String active = prod.lines()
            .map(String::trim)
            .filter(l -> !l.isEmpty() && !l.startsWith("#"))
            .collect(Collectors.joining("\n"));
        assertThat(active).as("prod 有效行不得再出现 username: root 字面量（2026-09-06 16:00 回滚事故的守卫）")
            .doesNotContain("username: root");
        assertThat(active).as("prod 有效行不得再出现 password: root 字面量").doesNotContain("password: root");
        assertThat(prod).as("username 必须是 SPRING_DATASOURCE_USERNAME env 占位（无默认=未注入即启动失败）")
            .contains("${SPRING_DATASOURCE_USERNAME:}");
        assertThat(prod).as("password 必须是 SPRING_DATASOURCE_PASSWORD env 占位")
            .contains("${SPRING_DATASOURCE_PASSWORD:}");
    }

    @Test
    @DisplayName("6) SEC-NEW-MED-3：归档 patch 声称已应用，其新增语义行必须真实落盘")
    void med3ArchivePatchMustBeReflectedOnDisk() throws IOException {
        if (!Files.exists(MED3_PATCH_ARCHIVE)) {
            return; // 归档件清理后由第 5 条独立把关，避免同一事实两处红
        }
        String prod = Files.readString(PROD_YML);
        List<String> added = Files.readAllLines(MED3_PATCH_ARCHIVE).stream()
            .filter(l -> l.startsWith("+") && !l.startsWith("+++"))
            .map(l -> l.substring(1).trim())
            .filter(l -> l.contains("SPRING_DATASOURCE_") || l.contains("SEC-NEW-MED-3"))
            .toList();
        assertThat(added)
            .as("归档 patch 必须含 SEC-NEW-MED-3 语义新增行（文件被截断/换内容即失守）").isNotEmpty();
        for (String line : added) {
            assertThat(prod).as("patch 声称新增的行必须已落盘: %s", line).contains(line);
        }
    }

    /** 解析 "@@ -284,3 +284,21 @@" 的旧起始行与旧行数（省略 ",count" 时按 1 计）。 */
    private static int[] parseOldRange(String hunkHeader) {
        String body = hunkHeader.substring(hunkHeader.indexOf('-') + 1);
        String oldSide = body.substring(0, body.indexOf(' '));
        String[] parts = oldSide.split(",");
        int start = Integer.parseInt(parts[0]);
        int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
        return new int[] {start, count};
    }

    /**
     * 取 {@code anchor:} 行之后 window 行内首个非注释 {@code enabled: <值>}（锚点与键都是缩进无关的 trim 比较）。
     * 锚点或键不存在时返回 null —— 调用方按「该覆盖不存在」判定；「不存在」在 prod 侧是常态（靠父继承），
     * 只有在被当作前提的父 yml 上才需要额外断言非空。
     */
    private static String flagValueOrNull(String yml, String anchor, int window) {
        String[] lines = yml.split("\\R", -1);
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].trim().equals(anchor)) {
                continue;
            }
            for (int j = i + 1; j <= Math.min(i + window, lines.length - 1); j++) {
                String t = lines[j].trim();
                if (t.isEmpty() || t.startsWith("#")) {
                    continue;
                }
                if (t.startsWith("enabled:")) {
                    return t.substring("enabled:".length()).trim();
                }
            }
            return null;
        }
        return null;
    }
}
