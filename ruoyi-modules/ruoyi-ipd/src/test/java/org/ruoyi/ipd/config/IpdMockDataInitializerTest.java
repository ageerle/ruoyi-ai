package org.ruoyi.ipd.config;

import cn.hutool.crypto.digest.BCrypt;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SEC-HIGH-1：BCrypt cost 4→10 升级验证（Round 9 / R9-BC-COST）
 * <p>覆盖 4 维度：
 * <ol>
 *   <li>BCrypt cost=10 生成的 hash 格式正确（$2a$10$ / $2b$10$ 开头）</li>
 *   <li>cost=10 hash 能正常 verify 正确密码</li>
 *   <li>cost=10 hash 拒绝错误密码</li>
 *   <li>源码 IpdMockDataInitializer.java 已使用 gensalt(10)，不再使用 gensalt(4)</li>
 * </ol>
 * <p>设计要点：纯 BCrypt 算法层 + 源码层验证，避免依赖 Spring ApplicationRunner 启动；
 * 4 实例 × 100 并发实测在 ipd_perf 隔离库跑（详见 Wave3-Batch-1-收口文档）。
 */
@Tag("dev")
@DisplayName("SEC-HIGH-1: BCrypt cost 4→10 升级验证")
class IpdMockDataInitializerTest {

    /**
     * P1（owner 2026-09-05 指令项2）：不再引用 {@code IpdMockDataInitializer.INITIAL_PWD}（该常量已删）。
     * 验证 2/3 只测 BCrypt hash/verify 往返行为，与真实种子口令无关，因此用自备探测串即可，
     * 不必把已入仓的 QA 种子口令再复制一份到测试源码。
     */
    private static final String BCRYPT_ROUNDTRIP_PROBE = "bcrypt-roundtrip-probe-not-a-real-seed";

    /**
     * SEC-HIGH-1 验证 1：BCrypt cost=10 生成的 hash 格式正确。
     * BCrypt hash 标准格式：$2[ab]$<cost>$<22-char-base64-salt><31-char-base64-hash> = 60 字符总长
     */
    @Test
    @DisplayName("1) BCrypt cost=10 hash 格式正确（$2a$10$ 或 $2b$10$ 开头，60 字符总长）")
    void bcryptCostTenHashFormat() {
        String salt = BCrypt.gensalt(10);
        String hash = BCrypt.hashpw("test_password_123", salt);
        assertThat(hash).matches("\\$2[ab]\\$10\\$.{53}");
        assertThat(hash.length()).isEqualTo(60);
    }

    /**
     * SEC-HIGH-1 验证 2：cost=10 hash 能正常 verify 正确密码。
     * 防止 hash 函数本身被破坏（极端情况下 BCrypt 库版本兼容问题）。
     */
    @Test
    @DisplayName("2) BCrypt cost=10 hash 验证正确密码通过")
    void bcryptCostTenVerifiesCorrectPassword() {
        String hash = BCrypt.hashpw(BCRYPT_ROUNDTRIP_PROBE, BCrypt.gensalt(10));
        assertThat(BCrypt.checkpw(BCRYPT_ROUNDTRIP_PROBE, hash)).isTrue();
    }

    /**
     * SEC-HIGH-1 验证 3：cost=10 hash 拒绝错误密码。
     * 防 BCrypt verify 函数绕过（极端回归）。
     */
    @Test
    @DisplayName("3) BCrypt cost=10 hash 拒绝错误密码")
    void bcryptCostTenRejectsWrongPassword() {
        String hash = BCrypt.hashpw(BCRYPT_ROUNDTRIP_PROBE, BCrypt.gensalt(10));
        assertThat(BCrypt.checkpw("wrong_password_attempt", hash)).isFalse();
    }

    /**
     * SEC-HIGH-1 验证 4：源码 IpdMockDataInitializer.java 第 96 行已使用 gensalt(10)（不再是 gensalt(4)）。
     * 通过读相对路径源文件验证（src/main/java/org/ruoyi/ipd/config/IpdMockDataInitializer.java）。
     * 注：本测试在 IDE 单跑时 working dir 设为 ruoyi-modules/ruoyi-ipd 即可；Maven Surefire 默认 working dir 为模块根。
     */
    @Test
    @DisplayName("4) 源码 IpdMockDataInitializer.java 已升级 gensalt(10)（不再使用 gensalt(4)）")
    void sourceUsesGensaltTen() throws IOException {
        Path path = Paths.get("src/main/java/org/ruoyi/ipd/config/IpdMockDataInitializer.java");
        assertThat(Files.exists(path)).as("源码文件存在: " + path.toAbsolutePath()).isTrue();
        String content = Files.readString(path);
        assertThat(content).contains("BCrypt.gensalt(10)");
        assertThat(content).doesNotContain("BCrypt.gensalt(4)");
    }

    /**
     * SEC-HIGH-2 验证 5：源码 ensurePerson 方法的 passwordHash 调用使用 runtimeInitialPwd 注入字段，
     * 不硬编码密码常量。
     * <p>P1（项2）补充：原来「常量保留仅供单测」的说法已作废——单测改用自备探测串，
     * main 源码里不得再出现任何密码字面量（由 {@code CredentialLiteralGuardTest} 静态守住）。
     * <p>断言仅检查 .passwordHash(BCrypt.hashpw(...)) 调用行，不检查方法注释里的字面提及（文档说明需要）。
     */
    @Test
    @DisplayName("5) HIGH-2: 源码 passwordHash 调用使用 runtimeInitialPwd 注入字段（且不引用任何密码常量）")
    void sourceUsesRuntimeInitialPwdNotConstant() throws IOException {
        Path path = Paths.get("src/main/java/org/ruoyi/ipd/config/IpdMockDataInitializer.java");
        assertThat(Files.exists(path)).as("源码文件存在: " + path.toAbsolutePath()).isTrue();
        String content = Files.readString(path);
        // 定位 .passwordHash(BCrypt.hashpw(...)) 调用行（精确到调用括号闭合）
        int hashCallStart = content.indexOf(".passwordHash(BCrypt.hashpw(");
        assertThat(hashCallStart).as("应存在 .passwordHash(BCrypt.hashpw(...)) 调用行").isGreaterThan(0);
        int hashCallEnd = content.indexOf("))", hashCallStart);
        String hashCall = content.substring(hashCallStart, hashCallEnd + 2);
        // 调用行第一参数必须是注入字段 runtimeInitialPwd（不能是密码常量）
        assertThat(hashCall).as(".passwordHash 调用第一参必须是 runtimeInitialPwd").contains("BCrypt.hashpw(runtimeInitialPwd,");
        assertThat(hashCall).as(".passwordHash 调用行不应出现密码常量").doesNotContain("INITIAL_PWD");
        // P1（项2）：整个类不得再残留密码常量定义
        assertThat(content).as("main 源码不应再定义 INITIAL_PWD 常量").doesNotContain("INITIAL_PWD =");
        // 确认存在 @Value 注入字段
        assertThat(content).contains("@Value(\"${ipd.security.initial-password}\")");
        assertThat(content).contains("private String runtimeInitialPwd;");
    }
}
