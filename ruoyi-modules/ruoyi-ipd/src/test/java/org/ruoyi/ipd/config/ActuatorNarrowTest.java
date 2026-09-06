package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-NEW-MED-1：actuator 端点暴露收窄验证（Round 9 / R9-ACTUATOR-NARROW）。
 * <p>覆盖 4 维度：
 * <ol>
 *   <li>父 application.yml include 收窄到 health,info,metrics（不再是 '*'）</li>
 *   <li>父 application.yml show-details 改 WHEN_AUTHORIZED（不再是 ALWAYS）</li>
 *   <li>dev application-dev.yml include 显式覆盖 '*'（本地调试便利）</li>
 *   <li>dev application-dev.yml show-details 显式 ALWAYS（调试便利）</li>
 * </ol>
 * <p>设计要点：静态 grep yml 文件验证配置层加固，避免依赖 Spring ApplicationContext 启动；
 * 自适应定位 git root（向上找 .git 目录），兼容 maven-surefire-plugin 默认 working dir（ruoyi-modules/ruoyi-ipd/）
 * 和 IDE 单跑 working dir（项目根 ruoyi-ai/）。
 */
@Tag("dev")
@DisplayName("SEC-NEW-MED-1: actuator 收窄验证")
class ActuatorNarrowTest {

    /** 自适应定位 git root（向上递归找 .git 目录） */
    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path APP_YML = REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml");
    private static final Path DEV_YML = REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-dev.yml");

    /** 自适应向上找 .git 目录；找不到则返回 cwd */
    private static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) return current;
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    @Test
    @DisplayName("1) 父 application.yml include 已收窄到 health,info,metrics（不再 '*'）")
    void parentIncludeNarrowed() throws IOException {
        assertThat(Files.exists(APP_YML)).as("父 yml 存在: " + APP_YML.toAbsolutePath()).isTrue();
        String content = Files.readString(APP_YML);
        // 父基线 include 必须是 health,info,metrics（不是 '*'）
        assertThat(content).contains("include: health,info,metrics");
        // 父基线不允许再含 include: '*'
        assertThat(content).doesNotContain("include: '*'");
    }

    @Test
    @DisplayName("2) 父 application.yml show-details 已改 WHEN_AUTHORIZED")
    void parentShowDetailsWhenAuthorized() throws IOException {
        String content = Files.readString(APP_YML);
        assertThat(content).contains("show-details: WHEN_AUTHORIZED");
    }

    @Test
    @DisplayName("3) dev application-dev.yml include 显式覆盖 '*'（本地调试便利）")
    void devIncludeWildcard() throws IOException {
        assertThat(Files.exists(DEV_YML)).as("dev yml 存在: " + DEV_YML.toAbsolutePath()).isTrue();
        String content = Files.readString(DEV_YML);
        // dev 段应含 include: '*' 显式覆盖（保留本地调试所有端点访问能力）
        assertThat(content).contains("include: '*'");
    }

    @Test
    @DisplayName("4) dev application-dev.yml show-details 显式 ALWAYS（调试便利）")
    void devShowDetailsAlways() throws IOException {
        String content = Files.readString(DEV_YML);
        assertThat(content).contains("show-details: ALWAYS");
    }
}
