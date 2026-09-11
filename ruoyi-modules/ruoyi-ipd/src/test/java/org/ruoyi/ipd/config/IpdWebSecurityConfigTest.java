package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-NEW-MED-2（hidden P0 #2）：/api/v1/public/** 公开端点拦截豁免验证（W5-C 复核）。
 * <p>覆盖 4 维度：
 * <ol>
 *   <li>登录校验拦截器 excludePathPatterns 含 /api/v1/public/**（即匿名 GET /api/v1/public/products 不被 401）</li>
 *   <li>注解鉴权拦截器（SaInterceptor）同样 exclude /api/v1/public/**（防止 type=ipd 注解意外拦到公开端点）</li>
 *   <li>排除列表同时保留 /api/v1/auth/login（认证入口匿名）</li>
 *   <li>addPathPatterns 仍锁定 /api/v1/**（不是 /** 过度宽松）；业务路径 /api/v1/projects 不在 excludePathPatterns 中</li>
 * </ol>
 * <p>设计要点：参考 ActuatorNarrowTest 静态扫描源码策略，避免依赖 Spring ApplicationContext 启动；
 * 提取 excludePathPatterns(...) 方法调用的字符串字面量集合做断言，自适应定位 git root。
 */
@Tag("dev")
@DisplayName("SEC-NEW-MED-2: /api/v1/public/** 拦截豁免验证")
class IpdWebSecurityConfigTest {

    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path CONFIG_JAVA = REPO_ROOT.resolve(
        "ruoyi-modules/ruoyi-ipd/src/main/java/org/ruoyi/ipd/config/IpdWebSecurityConfig.java");

    /** 匹配 .excludePathPatterns("a", "b", ...) 中所有字符串字面量 */
    private static final Pattern EXCLUDE_CALL = Pattern.compile(
        "\\.excludePathPatterns\\s*\\(([^)]*)\\)");

    @Test
    @DisplayName("1) 源码存在 + 至少 2 处 excludePathPatterns 调用")
    void sourceHasAtLeastTwoExcludePathPatterns() throws IOException {
        assertThat(Files.exists(CONFIG_JAVA))
            .as("IpdWebSecurityConfig.java 存在: " + CONFIG_JAVA.toAbsolutePath()).isTrue();
        String content = Files.readString(CONFIG_JAVA);
        int count = countMatches(content, "\\.excludePathPatterns\\s*\\(");
        assertThat(count)
            .as("两个拦截器都应各自调用 .excludePathPatterns(...)")
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("2) /api/v1/public/** 在全部 excludePathPatterns 调用中至少出现 2 次（两个拦截器都豁免）")
    void publicPathExcludedInBothInterceptors() throws IOException {
        String content = Files.readString(CONFIG_JAVA);
        int occurrences = countMatches(content, "\"/api/v1/public/\\*\\*\"");
        assertThat(occurrences)
            .as("登录校验拦截器 + 注解鉴权拦截器都必须 exclude /api/v1/public/**")
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("3) /api/v1/auth/login 仍在 excludePathPatterns（认证入口匿名不破）")
    void loginStillExcluded() throws IOException {
        String content = Files.readString(CONFIG_JAVA);
        int occurrences = countMatches(content, "\"/api/v1/auth/login\"");
        assertThat(occurrences)
            .as("每个拦截器都应保留 /api/v1/auth/login 排除")
            .isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("4) addPathPatterns 仍锁定 /api/v1/**；业务路径 /api/v1/projects 不被过度放行")
    void businessPathsNotOverExcluded() throws IOException {
        String content = Files.readString(CONFIG_JAVA);
        // 锁定 /api/v1/** —— 防止有人误改成 /** 误伤全局
        assertThat(countMatches(content, "\\.addPathPatterns\\s*\\(\\s*\"/api/v1/\\*\\*\"\\s*\\)"))
            .as("每个拦截器 addPathPatterns 必须仍是 /api/v1/**")
            .isGreaterThanOrEqualTo(2);
        // 业务路径必须 NOT 出现在任何一个 excludePathPatterns(...) 调用里
        Matcher m = EXCLUDE_CALL.matcher(content);
        boolean leaked = false;
        while (m.find()) {
            String args = m.group(1);
            if (args.contains("/api/v1/projects")
                || args.contains("/api/v1/kpi")
                || args.contains("/api/v1/products")
                || args.contains("/api/v1/bonus-pool")) {
                leaked = true;
                break;
            }
        }
        assertThat(leaked).as("业务路径不应出现在 excludePathPatterns 中").isFalse();
    }

    private static int countMatches(String content, String regex) {
        Matcher m = Pattern.compile(regex).matcher(content);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    /** 自适应向上找 .git 目录；找不到则返回 cwd */
    private static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) return current;
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }
}
