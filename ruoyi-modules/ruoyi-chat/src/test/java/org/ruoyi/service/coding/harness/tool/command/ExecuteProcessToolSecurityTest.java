package org.ruoyi.service.coding.harness.tool.command;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ruoyi.service.coding.harness.tool.builtin.RunContext;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * R-NEW-S-2：execute_process 攻击样本拒绝测试。
 *
 * <p>覆盖三层守卫——
 * <ol>
 *   <li><b>argv 守卫</b>（{@code CommandValidation.validateArgv} + 重复 executable 检查 + inline 解释器源码拒绝）：
 *       null/empty/超限 argv、{@code node -e} / {@code python -c} inline 源码</li>
 *   <li><b>executable 守卫</b>（{@code ExecutablePolicy.authorize}）：
 *       shell 解释器、绝对路径不在白名单、未白名单名</li>
 *   <li><b>cwd 守卫</b>（{@code CommandWorkspaceGuard.cwd}）：
 *       绝对路径、父级遍历（{@code ..}）、非法字符</li>
 * </ol>
 *
 * <p>Dev profile 通过 {@code coding.harness.tools.execute-process.enabled=true} 显式 opt-in；
 * 默认禁用——见 {@code DefaultHarnessToolRuntimeFactory} 与
 * {@code application.yml#coding.harness.tools.execute-process.enabled}。本测试聚焦
 * {@link ExecuteProcessTool} 对象本身的安全语义，不测试工厂 enable/disable（属 Spring 配置层）。
 *
 * <p>所有用例均在 {@link CommandToolException} 抛出点之前终止——不依赖真实进程启动。
 */
@Tag("dev")
@DisplayName("S-2 execute_process 攻击样本拒绝——argv/executable/cwd 三层守卫")
class ExecuteProcessToolSecurityTest {

    @TempDir
    Path workspace;

    private ExecuteProcessTool newTool() {
        RunContext ctx = RunContext.forWorkspace(workspace);
        return new ExecuteProcessTool(ctx);
    }

    // ---------- argv 守卫 ----------

    /**
     * EP-1：{@code argv=null} ⇒ validateArgv 拒绝（{@code ARGV_INVALID}）。
     */
    @Test
    @DisplayName("EP-1 argv=null 拒绝")
    void argv_null_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("git", null, ".", null, null))
            .isInstanceOf(CommandToolException.class);
    }

    /**
     * EP-2：argv 中单个 entry 是空字符串（{@code requireText} 拒绝 blank）⇒ INVALID_COMMAND_ARGUMENT。
     * 注：空数组本身合法（CommandValidation.validateArgv 明确说"use an empty array for no arguments"），
     * 这里测试的是 entry 内容的空字符串拦截。
     */
    @Test
    @DisplayName("EP-2 argv 单 entry 是空字符串拒绝")
    void argv_blankEntry_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("git", List.of("status", ""), ".", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "INVALID_COMMAND_ARGUMENT");
    }

    /**
     * EP-3：argv 中包含 257 个 entry（&gt; maxArgvEntries=256）⇒ 拒绝。
     */
    @Test
    @DisplayName("EP-3 argv 超 256 entries 拒绝")
    void argv_tooMany_denied() {
        ExecuteProcessTool tool = newTool();
        List<String> argv = new java.util.ArrayList<>();
        for (int i = 0; i < 257; i++) argv.add("arg" + i);
        assertThatThrownBy(() -> tool.executeProcess("git", argv, ".", null, null))
            .isInstanceOf(CommandToolException.class);
    }

    /**
     * EP-4：argv 中单个 entry 超过 16384 字符（maxArgumentChars=16*1024）⇒ 拒绝。
     */
    @Test
    @DisplayName("EP-4 argv 单 entry 超 16384 chars 拒绝")
    void argv_tooLongEntry_denied() {
        ExecuteProcessTool tool = newTool();
        String huge = "x".repeat(16 * 1024 + 1);
        assertThatThrownBy(() -> tool.executeProcess("git", List.of("status", huge), ".", null, null))
            .isInstanceOf(CommandToolException.class);
    }

    // ---------- executable 守卫（白名单） ----------

    /**
     * EP-5：executable="bash" ⇒ SHELL_INTERPRETER_DENIED（shell 解释器永不允许）。
     */
    @Test
    @DisplayName("EP-5 bash/sh 解释器拒绝 SHELL_INTERPRETER_DENIED")
    void executable_bash_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("bash", List.of("-c", "echo hi"), ".", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "SHELL_INTERPRETER_DENIED");
    }

    /**
     * EP-6：executable="powershell"（Windows 解释器在 macOS/Linux 也被拒）⇒ SHELL_INTERPRETER_DENIED。
     */
    @Test
    @DisplayName("EP-6 powershell 解释器拒绝 SHELL_INTERPRETER_DENIED")
    void executable_powershell_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("powershell", List.of("-Command", "exit 0"), ".", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "SHELL_INTERPRETER_DENIED");
    }

    /**
     * EP-7：executable="curl"（不在白名单）⇒ EXECUTABLE_NOT_ALLOWLISTED。
     */
    @Test
    @DisplayName("EP-7 curl 未白名单拒绝 EXECUTABLE_NOT_ALLOWLISTED")
    void executable_curl_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("curl", List.of("https://example.com"), ".", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "EXECUTABLE_NOT_ALLOWLISTED");
    }

    /**
     * EP-8：executable="/bin/rm"（绝对路径但不在白名单）⇒ EXECUTABLE_NOT_ALLOWLISTED。
     */
    @Test
    @DisplayName("EP-8 /bin/rm 绝对路径未白名单拒绝")
    void executable_absolutePath_notAllowlisted_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("/bin/rm", List.of("-rf", "/tmp/x"), ".", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "EXECUTABLE_NOT_ALLOWLISTED");
    }

    // ---------- inline 解释器源码拒绝 ----------

    /**
     * EP-9：{@code node -e 'malicious'} inline 解释器源码 ⇒ 拒绝（executeProcess 不允许 inlineProbe）。
     * 仅 {@code executeInlineProbe} 通道允许 inline；普通 {@code executeProcess} 一律拒。
     */
    @Test
    @DisplayName("EP-9 node -e inline 解释器源码拒绝")
    void executable_node_dashE_inline_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("node", List.of("-e", "require('child_process').execSync('rm -rf /')"), ".", null, null))
            .isInstanceOf(CommandToolException.class);
    }

    /**
     * EP-10：{@code python -c 'import os; os.system("...")'} ⇒ 拒绝。
     */
    @Test
    @DisplayName("EP-10 python -c inline 解释器源码拒绝")
    void executable_python_dashC_inline_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("python", List.of("-c", "import os; os.system('rm -rf /')"), ".", null, null))
            .isInstanceOf(CommandToolException.class);
    }

    // ---------- cwd 守卫 ----------

    /**
     * EP-11：{@code cwd="/etc"}（绝对路径）⇒ CWD_OUTSIDE_WORKSPACE。
     * workspaceGuard 先于 authorize 触达：cwd=null/blank/"." 走 root；其它先入 argv/exe 校验。
     * 但本用例 argv/exe 已合法（git status）→ 触达 cwd 守卫。
     */
    @Test
    @DisplayName("EP-11 cwd 绝对路径 /etc 拒绝 CWD_OUTSIDE_WORKSPACE")
    void cwd_absolutePath_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("git", List.of("status"), "/etc", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "CWD_OUTSIDE_WORKSPACE");
    }

    /**
     * EP-12：{@code cwd="../etc"}（父级遍历）⇒ CWD_OUTSIDE_WORKSPACE。
     */
    @Test
    @DisplayName("EP-12 cwd 父级遍历 ../etc 拒绝 CWD_OUTSIDE_WORKSPACE")
    void cwd_parentTraversal_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("git", List.of("status"), "../etc", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "CWD_OUTSIDE_WORKSPACE");
    }

    /**
     * EP-13：{@code cwd="sub/../../etc"}（混合路径含父级遍历）⇒ CWD_OUTSIDE_WORKSPACE。
     */
    @Test
    @DisplayName("EP-13 cwd 混合路径含父级遍历拒绝")
    void cwd_mixedParentTraversal_denied() {
        ExecuteProcessTool tool = newTool();
        assertThatThrownBy(() -> tool.executeProcess("git", List.of("status"), "sub/../../etc", null, null))
            .isInstanceOf(CommandToolException.class)
            .hasFieldOrPropertyWithValue("code", "CWD_OUTSIDE_WORKSPACE");
    }

    /**
     * EP-14：argv 中含 shell 元字符——{@code ; cat /etc/passwd} 必须字面传递，不被解析为 shell。
     * 不期望抛出异常（应到达 ProcessBuilder.start）；但本测试聚焦"不被路径解析为目录"——
     * cwd=null 走 root；argv 含分号是合法字符串。但 ProcessBuilder.start 会真起 git，
     * 用一个不存在的 git 子命令让它快速退出。
     */
    @Test
    @DisplayName("EP-14 argv 含 shell 元字符字面传递不解析")
    void argv_shellMetachars_literal_noShellParse() {
        // 注：本用例仅断言 argv 元字符不被 CommandWorkspaceGuard/ExecutablePolicy 拦截
        // ——即使后续真启动 git shell 也不会执行注入。argv 元字符是普通字符串，git args 不解析。
        ExecuteProcessTool tool = newTool();
        // 调用入口不抛 argv/exe/cwd 守卫错——可能因 PATH 没有 git 抛 EXECUTABLE_NOT_FOUND
        // 这是正确语义：未授权的程序找不到；非元字符解析路径
        try {
            tool.executeProcess("git", List.of("status", "; cat /etc/passwd #"), ".", null, null);
        } catch (CommandToolException ex) {
            // 只可能是 EXECUTABLE_NOT_FOUND（PATH 无 git）或 PROCESS_SLOT_TIMEOUT
            // 不应是 ARGV_INVALID 或 SHELL_INTERPRETER_DENIED
            String code = ex.code();
            org.assertj.core.api.Assertions.assertThat(code)
                .as("shell 元字符必须字面传递，不应被解析为 ARGV_INVALID/SHELL_INTERPRETER_DENIED")
                .isIn("EXECUTABLE_NOT_FOUND", "EXECUTABLE_NOT_ALLOWLISTED", "PROCESS_SLOT_TIMEOUT",
                    "INVALID_EXECUTABLE");
        }
    }
}