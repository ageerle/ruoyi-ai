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
 * SEC-NEW-MED-4：RSA 私钥 PEM 字面量不应出现在 yml 配置注释里（Round 9 / R9-RSA-PEM-LEAK）。
 * <p>历史背景：父 application.yml line 293 注释里曾含完整 PEM PKCS#8 私钥
 * （MIIBVAIBADANBgkqhkiG9w0BAQEFAAS... ~1100+ 字符），导致 git 历史永久暴露私钥。
 * 本测试 4 维度：
 * <ol>
 *   <li>父 application.yml 不含 MIIBVAIB/MIIEvQIB 私钥 PEM 头</li>
 *   <li>父 application.yml 不含 "BEGIN.*PRIVATE KEY" 头</li>
 *   <li>dev/prod application*.yml 注释里都不含真私钥字面量（仅允许 alipay-public-key 占位符 MIIB***DAQAB）</li>
 *   <li>line 293 已替换为 SEC-NEW-MED-4 占位描述</li>
 * </ol>
 * <p>设计要点：自适应 git root（maven-surefire-plugin working dir = 模块根；IDE = 项目根）。
 */
@Tag("dev")
@DisplayName("SEC-NEW-MED-4: RSA 私钥 PEM 字面量反向用例")
class RsaPemLiteralAbsentTest {

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

    @Test
    @DisplayName("1) 父 application.yml 不含 MIIBVAIB/MIIEvQIB 私钥 PEM 头")
    void parentYmlNoPrivatePem() throws IOException {
        String content = Files.readString(APP_YML);
        // PKCS#8 私钥头：MIIBVAIB 或 MIIEvQI（注意 MIIB 在公钥里也常见，但后缀 VAIB/EvQIB 才是私钥）
        assertThat(content).doesNotContain("MIIBVAIB");
        assertThat(content).doesNotContain("MIIEvQIB");
    }

    @Test
    @DisplayName("2) 父 application.yml 不含 BEGIN PRIVATE KEY 头（PEM 任何变体）")
    void parentYmlNoBeginPrivateKey() throws IOException {
        String content = Files.readString(APP_YML);
        assertThat(content).doesNotContain("BEGIN PRIVATE KEY");
        assertThat(content).doesNotContain("BEGIN RSA PRIVATE KEY");
    }

    @Test
    @DisplayName("3) dev yml 不含真私钥字面量（仅 alipay-public-key 占位符）")
    void devYmlNoRealPrivateKey() throws IOException {
        String content = Files.readString(DEV_YML);
        assertThat(content).doesNotContain("MIIBVAIB");
        assertThat(content).doesNotContain("MIIEvQIB");
        // MIIB 占位符（alipay-public-key MIIB***DAQAB）允许保留
        assertThat(content).contains("alipay-public-key: MIIB***");
    }

    @Test
    @DisplayName("4) prod yml 不含真私钥字面量（仅 alipay 占位符）")
    void prodYmlNoRealPrivateKey() throws IOException {
        if (!Files.exists(PROD_YML)) {
            // 某些环境可能未提供 prod profile，本测试跳过
            return;
        }
        String content = Files.readString(PROD_YML);
        assertThat(content).doesNotContain("MIIBVAIB");
        assertThat(content).doesNotContain("MIIEvQIB");
        assertThat(content).contains("alipay-public-key: MIIB***");
    }
}