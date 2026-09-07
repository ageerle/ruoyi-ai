package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Batch-5 #1（2026-09-07）Hikari 偏离追认守门测试。
 *
 * <p>背景：prod Hikari 实际字节为 {@code maxPoolSize=80 + connectionTimeout=5000 + p6spy=false}，
 * 已由 SEC-HIGH-1 (R9-BC-COST) BCrypt cost=10 同步路径覆盖，超原 Wave3 spec 60。本测试守
 * 「prod 池字节不再被回滚」+「父基线（application.yml）maxPoolSize=20 安全兜底」+「dev 仍升 80」
 * +「prod p6spy 显式 false 与父基线对齐」4 个事实。
 *
 * <p>解析方式：snakeyaml 直接读 yml 文件（不需 Spring context）。snakeyaml 已在 Spring Boot
 * 默认依赖中，无需新加依赖。
 *
 * <p>路径自发现：与 {@link ProdConfigDeltaGuardTest} 同模式（Surefire working dir = 模块根；
 * IDE = 项目根），向上找 {@code .git} 标记。
 *
 * <p>注意：本类只断言 yml 字节，不启动 Spring；连接池真实行为由集成测试覆盖。
 */
@Tag("dev")
@DisplayName("Batch-5 #1: prod Hikari maxPoolSize=80 + connectionTimeout=5000 + p6spy=false 偏离追认守门")
class ProdHikariConfigTest {

    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path APP_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml");
    private static final Path APP_PROD_YML =
        REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-prod.yml");

    private static final String PROD_HIKARI_MAX =
        "spring.datasource.dynamic.hikari.maxPoolSize";
    private static final String PROD_HIKARI_CONN_TIMEOUT =
        "spring.datasource.dynamic.hikari.connectionTimeout";
    private static final String PROD_P6SPY = "spring.datasource.dynamic.p6spy";
    private static final String PARENT_HIKARI_MAX = "spring.datasource.dynamic.hikari.maxPoolSize";

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

    /**
     * snakeyaml 直接读 yml 文件——不需 Spring context（避免 @SpringBootTest 启动开销与依赖）。
     * 多文档 yml（--- 分隔）合并所有文档的根 map 为一个查找根；个别 doc 为 null（注释开头）
     * 直接跳过。浅合并：子 map 顶层键若重复则后 doc 覆盖前 doc，符合 yml 后覆盖前的语义。
     */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> loadYaml(Path path) throws IOException {
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        try (InputStream in = Files.newInputStream(path)) {
            for (Object doc : new Yaml().loadAll(in)) {
                if (doc instanceof Map) {
                    merged.putAll((Map<String, Object>) doc);
                }
            }
        }
        assertThat(merged).as("yaml 至少须有一个非空文档: %s", path).isNotEmpty();
        return merged;
    }

    /** 按点分路径在嵌套 map 中取值；任一段缺失返回 null。 */
    @SuppressWarnings("unchecked")
    private static Object deepGet(Map<String, Object> root, String dottedPath) {
        String[] parts = dottedPath.split("\\.");
        Object cur = root;
        for (String p : parts) {
            if (!(cur instanceof Map)) {
                return null;
            }
            cur = ((Map<String, Object>) cur).get(p);
            if (cur == null) {
                return null;
            }
        }
        return cur;
    }

    @Test
    @DisplayName("1) prod dynamic.hikari.maxPoolSize = 80（dynamic-datasource 顶层 hikari 作所有数据源默认值；超原 spec 60）")
    void prodMasterMaxPoolSizeIs80() throws IOException {
        Map<String, Object> prod = loadYaml(APP_PROD_YML);
        Object val = deepGet(prod, PROD_HIKARI_MAX);
        assertThat(val)
            .as("prod 必须显式覆盖 dynamic.hikari.maxPoolSize（落盘字节而非注释声明）: %s", PROD_HIKARI_MAX)
            .isNotNull()
            .isInstanceOf(Number.class);
        assertThat(((Number) val).intValue())
            .as("prod dynamic.hikari.maxPoolSize 必须为 80（追认实际值；超 Wave3 spec 60）")
            .isEqualTo(80);
    }

    @Test
    @DisplayName("2) prod dynamic.hikari.connectionTimeout = 5000（int ms，与父基线/dev 对齐）")
    void prodMasterConnectionTimeoutIs5000() throws IOException {
        Map<String, Object> prod = loadYaml(APP_PROD_YML);
        Object val = deepGet(prod, PROD_HIKARI_CONN_TIMEOUT);
        assertThat(val)
            .as("prod 必须显式覆盖 dynamic.hikari.connectionTimeout: %s", PROD_HIKARI_CONN_TIMEOUT)
            .isNotNull()
            .isInstanceOf(Number.class);
        assertThat(((Number) val).intValue())
            .as("prod dynamic.hikari.connectionTimeout 必须为 5000（30s → 5s 快速失败，防高并发排队雪崩）")
            .isEqualTo(5000);
    }

    @Test
    @DisplayName("3) prod p6spy = false（与父基线对齐；防 SQL 日志落盘与敏感 SQL 暴露）")
    void prodP6spyIsFalse() throws IOException {
        Map<String, Object> prod = loadYaml(APP_PROD_YML);
        Object val = deepGet(prod, PROD_P6SPY);
        assertThat(val)
            .as("prod 必须显式覆盖 p6spy=false（与父基线对齐；SEC-NEW-MED-2 守卫）: %s", PROD_P6SPY)
            .isNotNull()
            .isInstanceOf(Boolean.class);
        assertThat((Boolean) val)
            .as("prod p6spy 必须为 false（性能损耗 + 敏感 SQL 暴露）")
            .isFalse();
    }

    @Test
    @DisplayName("4) 父基线 application.yml hikari.maxPoolSize = 20（安全兜底；dev/prod 均 override 80）")
    void parentBaselineMaxPoolSizeIs20() throws IOException {
        Map<String, Object> parent = loadYaml(APP_YML);
        Object val = deepGet(parent, PARENT_HIKARI_MAX);
        assertThat(val)
            .as("父基线必须显式设置 dynamic.hikari.maxPoolSize（被删即破兜底）: %s", PARENT_HIKARI_MAX)
            .isNotNull()
            .isInstanceOf(Number.class);
        assertThat(((Number) val).intValue())
            .as("父基线 hikari.maxPoolSize 必须为 20（HikariCP 默认 10 并发>10 即排队雪崩）")
            .isEqualTo(20);
    }
}
