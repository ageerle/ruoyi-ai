package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P1（owner 2026-09-05 指令项2）：凭证字面量守卫（QA 种子口令 + sa-token JWT 密钥）。
 *
 * <p>背景两件：
 * <ol>
 *   <li>QA 种子口令以 {@code Ipd@<6位数字>} 形态被 4 组角色账号（SUPER_ADMIN / GROUP_LEADER /
 *       MARKET_PM / RD_PM）共用，且历史上写死在 {@code IpdMockDataInitializer.INITIAL_PWD}
 *       （SEC-AUD HIGH-2 / SUPPLEMENT L158 一直未收口）。本批删除该常量，写入路径改由
 *       {@code ipd.security.initial-password} 注入。</li>
 *   <li>commit {@code a8a70ad9} 曾把 64 字符 JWT 密钥明文写进父 application.yml（git 历史永久可见，
 *       已按 O4 裁决不重写历史 → 只能声明作废）。父 yml 现值必须是 {@code ${SA_TOKEN_JWT_SECRET_KEY:}}
 *       这种「env 注入、无内联默认」形态，否则同类泄露会重演。</li>
 * </ol>
 *
 * <p>本测试刻意**不把自己变成新的明文载体**：种子口令用正则 {@code Ipd@\d{6}} 匹配，
 * 历史 JWT 密钥用「值必须是占位符形态」间接判定，全文不含任何真实凭证。
 *
 * <p>设计要点：自适应 git root（Surefire working dir = 模块根；IDE = 项目根），与
 * {@code RsaPemLiteralAbsentTest} 同一惯例。
 */
@Tag("dev")
@DisplayName("P1 项2: QA 种子口令与 JWT 密钥字面量守卫")
class CredentialLiteralGuardTest {

    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path IPD_MAIN_SRC =
        REPO_ROOT.resolve("ruoyi-modules/ruoyi-ipd/src/main/java");
    private static final Path APP_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml");
    private static final Path DEV_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-dev.yml");
    private static final Path PROD_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-prod.yml");

    /** QA 种子口令族（不写死完整串，避免测试源码自身成为新的明文载体） */
    private static final Pattern SEED_PWD = Pattern.compile("Ipd@\\d{6}");

    /** 形如 {@code static final String XXX_PWD = "字面量"} 的密码常量定义 */
    private static final Pattern PWD_CONSTANT_DEF =
        Pattern.compile("static\\s+final\\s+String\\s+\\w*(PWD|PASSWORD)\\w*\\s*=\\s*\"[^\"]+\"");

    /** 自适应向上找 .git 目录；找不到则返回 cwd */
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
    @DisplayName("1) ruoyi-ipd 的 main 源码不含 QA 种子口令字面量")
    void mainSourceHasNoSeedPassword() throws IOException {
        assertThat(Files.isDirectory(IPD_MAIN_SRC)).as("main 源码目录存在: " + IPD_MAIN_SRC).isTrue();
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(IPD_MAIN_SRC)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    if (SEED_PWD.matcher(Files.readString(p)).find()) {
                        hits.add(REPO_ROOT.relativize(p).toString());
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        assertThat(hits).as("main 源码不应再出现种子口令（改由 ipd.security.initial-password 注入）").isEmpty();
    }

    @Test
    @DisplayName("2) main 源码不定义任何密码常量（static final *PWD*/*PASSWORD* = \"字面量\"）")
    void mainSourceDefinesNoPasswordConstant() throws IOException {
        List<String> hits = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(IPD_MAIN_SRC)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    if (PWD_CONSTANT_DEF.matcher(Files.readString(p)).find()) {
                        hits.add(REPO_ROOT.relativize(p).toString());
                    }
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
            });
        }
        assertThat(hits).as("密码只能来自配置注入，不能是 main 源码常量").isEmpty();
    }

    @Test
    @DisplayName("3) 父 application.yml 的 jwt-secret-key 必须是 env 占位且无内联默认")
    void parentYmlJwtSecretIsEnvPlaceholderOnly() throws IOException {
        String value = yamlValueOf(Files.readString(APP_YML), "jwt-secret-key");
        assertThat(value).as("父 yml 必须回到 ${SA_TOKEN_JWT_SECRET_KEY:}（无默认）形态")
            .isEqualTo("${SA_TOKEN_JWT_SECRET_KEY:}");
        // a8a70ad9 那类回归：直接把 32+ 字符密钥写进 yml
        assertThat(value).doesNotContainPattern("(?i)[a-z0-9]{32,}");
    }

    @Test
    @DisplayName("4) dev 默认密钥显式标注 devOnly 前缀；prod 若配置该键必须是 env 占位")
    void devDefaultIsMarkedDevOnlyAndProdStaysPlaceholder() throws IOException {
        String dev = yamlValueOf(Files.readString(DEV_YML), "jwt-secret-key");
        assertThat(dev).as("dev 兜底值必须带 devOnly 标记，防止被当成生产密钥复用")
            .startsWith("${SA_TOKEN_JWT_SECRET_KEY:devOnly-");

        String prod = Files.readString(PROD_YML);
        if (prod.contains("jwt-secret-key")) {
            assertThat(yamlValueOf(prod, "jwt-secret-key"))
                .as("prod 只能留 env 占位，不得写任何默认值")
                .isEqualTo("${SA_TOKEN_JWT_SECRET_KEY:}");
        }
        // prod 也不得出现种子口令字面量
        assertThat(SEED_PWD.matcher(prod).find()).as("prod yml 不含种子口令").isFalse();
    }

    /** 取形如 "  key: value" 的行值（只取首个命中，足够覆盖 sa-token 段唯一键） */
    private static String yamlValueOf(String content, String key) {
        for (String line : content.split("\\R")) {
            String t = line.trim();
            if (t.startsWith("#")) {
                continue;
            }
            if (t.startsWith(key + ":")) {
                return t.substring(key.length() + 1).trim();
            }
        }
        throw new AssertionError("yml 未找到键 " + key + "（若被删除/改名，本守卫应同步复核而不是让它静默失效）");
    }
}
