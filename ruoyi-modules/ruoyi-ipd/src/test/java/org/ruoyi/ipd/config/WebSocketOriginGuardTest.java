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
 * SEC-LOW-4：WebSocket allowedOrigins 不应为 '*' 通配（Round 9 / R9-WS-ORIGIN）。
 * <p>历史背景：父 application.yml websocket.allowedOrigins='*'，即使 websocket.enabled=false
 * 当前不生效，作为 defense-in-depth 必须收窄（防未来 enabled=true 时任意源跨域访问）。
 * 本测试 4 维度：
 * <ol>
 *   <li>父 application.yml allowedOrigins 不为 '*'（应为 '' 同源默认）</li>
 *   <li>父 application.yml 含 SEC-LOW-4 注释说明</li>
 *   <li>dev application-dev.yml 显式覆盖 '*'（本地双端口调试便利）</li>
 *   <li>prod application-prod.yml 不主动放宽到 '*'（默认走父基线）</li>
 * </ol>
 * <p>设计要点：自适应 git root 定位（maven-surefire working dir = 模块根；IDE = 项目根）。
 */
@Tag("dev")
@DisplayName("SEC-LOW-4: WebSocket allowedOrigins 不为 '*' 通配")
class WebSocketOriginGuardTest {

    private static final Path REPO_ROOT = findGitRoot(Paths.get(System.getProperty("user.dir")));
    private static final Path APP_YML = REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application.yml");
    private static final Path DEV_YML = REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-dev.yml");
    private static final Path PROD_YML = REPO_ROOT.resolve("ruoyi-admin/src/main/resources/application-prod.yml");

    /** 自适应向上找 .git 目录；找不到则返回 cwd */
    private static Path findGitRoot(Path start) {
        Path current = start.toAbsolutePath().normalize();
        while (current != null) {
            if (Files.exists(current.resolve(".git"))) return current;
            current = current.getParent();
        }
        return start.toAbsolutePath().normalize();
    }

    /** 提取 yml 中指定顶层段（从段头到下一个 "--- #" 文档分隔符） */
    private static String extractSection(String content, String sectionKey) {
        int start = content.indexOf(sectionKey);
        if (start < 0) return "";
        int end = content.indexOf("\n--- #", start);
        if (end < 0) end = content.length();
        return content.substring(start, end);
    }

    @Test
    @DisplayName("1) 父 application.yml allowedOrigins 不为 '*'（应为 ''）")
    void parentAllowedOriginsNotWildcard() throws IOException {
        String content = Files.readString(APP_YML);
        // 父基线 websocket.allowedOrigins 不能是 '*'（防未来启用后任意源跨域）。
        // R15：yml 存在多个 websocket: 顶层段（IPD 通知段 destination-prefix + SEC-LOW-4 段），
        // 锚第一个段会错位（R14 修复时仅一段，兄弟会话后插入 IPD 通知段导致败），
        // 改为逐段校验全部段（任何段都不得放宽到 '*'）。
        int idx = content.indexOf("\nwebsocket:");
        assertThat(idx).as("父 yml 含 websocket 段").isNotNegative();
        while (idx >= 0) {
            String wsBlock = extractSection(content.substring(idx + 1), "websocket:");
            assertThat(wsBlock).doesNotContain("allowedOrigins: '*'");
            idx = content.indexOf("\nwebsocket:", idx + 1);
        }
        // 父基线必须显式含 allowedOrigins: ''（同源默认，不能是 '*'）
        assertThat(content).contains("allowedOrigins: ''");
    }

    @Test
    @DisplayName("2) 父 application.yml 含 SEC-LOW-4 注释")
    void parentHasSecurityComment() throws IOException {
        String content = Files.readString(APP_YML);
        // 父 yml 在 websocket 段必须含 SEC-LOW-4 注释（防漂移）
        assertThat(content).contains("SEC-LOW-4");
    }

    @Test
    @DisplayName("3) dev application-dev.yml 显式覆盖 allowedOrigins '*'")
    void devAllowedOriginsWildcard() throws IOException {
        String content = Files.readString(DEV_YML);
        // dev 段应有 websocket.allowedOrigins: '*' 显式覆盖
        int wsIdx = content.indexOf("websocket:");
        assertThat(wsIdx).as("dev yml 含 websocket 段").isPositive();
        String wsBlock = content.substring(wsIdx, Math.min(wsIdx + 200, content.length()));
        assertThat(wsBlock).contains("allowedOrigins: '*'");
    }

    @Test
    @DisplayName("4) prod application-prod.yml 不主动放宽到 '*'")
    void prodYmlNotWildcard() throws IOException {
        if (!Files.exists(PROD_YML)) {
            return; // 没有 prod profile 则跳过
        }
        String content = Files.readString(PROD_YML);
        // 如果 prod yml 含 websocket 段，必须不出现 allowedOrigins: '*'
        // （如果 prod 段没显式 websocket 配置，走父基线即可，不阻断）
        int wsIdx = content.indexOf("websocket:");
        if (wsIdx > 0) {
            String wsBlock = content.substring(wsIdx, Math.min(wsIdx + 300, content.length()));
            assertThat(wsBlock).doesNotContain("allowedOrigins: '*'");
        }
    }
}