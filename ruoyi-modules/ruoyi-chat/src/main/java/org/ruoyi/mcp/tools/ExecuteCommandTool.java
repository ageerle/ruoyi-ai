package org.ruoyi.mcp.tools;

import dev.langchain4j.agent.tool.Tool;
import org.ruoyi.mcp.service.core.BuiltinToolProvider;
import org.ruoyi.service.coding.CodingEventChannel;
import org.ruoyi.service.coding.CodingSseEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 命令执行工具
 *
 * <p>在会话工作目录内执行白名单命令，返回 stdout+stderr 尾部（8KB）。
 * 安全四层防御：
 * <ol>
 *   <li>命令白名单：首段必须是允许的命令名</li>
 *   <li>元字符黑名单：含 shell 元字符 {@code &|;`$<>\n} 直接拒绝（虽走 ProcessBuilder 不经 shell，仍双保险）</li>
 *   <li>工作目录锁定：ProcessBuilder.directory 锁在会话 workspace</li>
 *   <li>超时 + 输出截断：30s 超时 destroyForcibly，输出重定向临时文件只读尾部 8KB</li>
 * </ol>
 *
 * <p>实现借鉴 {@code FfmpegProcessRunner}：ProcessBuilder(List) 不走 shell 防注入，
 * redirectErrorStream+redirectOutput 到临时文件，waitFor 超时，readTail 截断。
 *
 * @author ageerle
 */
@Component
public class ExecuteCommandTool implements BuiltinToolProvider {

    public static final String DESCRIPTION = "Executes a shell command in the workspace directory. " +
        "Command must be in the allowed whitelist (npm/pnpm/yarn/git/mvn/gradle/java/javac/" +
        "python/pip/node/tsc/eslint/prettier/cat/ls/dir/echo). " +
        "Returns combined stdout+stderr tail (8KB). 30s timeout.";

    /** 允许的命令白名单（首段） */
    private static final Set<String> ALLOWED_COMMANDS = Set.of(
        "npm", "pnpm", "yarn", "git", "mvn", "gradle", "java", "javac",
        "python", "python3", "pip", "node", "tsc", "eslint", "prettier",
        "cat", "ls", "dir", "echo"
    );

    /** 禁止的 shell 元字符（防注入） */
    private static final String FORBIDDEN_CHARS = "&|;`$<>\n\r";

    /** 输出截断上限 */
    private static final int MAX_OUTPUT_BYTES = 8 * 1024;
    /** 命令超时（秒） */
    private static final long TIMEOUT_SECONDS = 30;

    private final Path rootDirectory;
    private final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(getClass());
    private final CodingEventChannel channel;

    public ExecuteCommandTool() {
        this.rootDirectory = Paths.get(System.getProperty("user.dir"), "workspace");
        this.channel = null;
    }

    /**
     * 编程能力专用构造。
     */
    public ExecuteCommandTool(Path root, CodingEventChannel channel) {
        this.rootDirectory = root.toAbsolutePath().normalize();
        this.channel = channel;
    }

    /**
     * 执行命令
     *
     * @param command 完整命令行（如 "npm install" 或 "node -v"）
     * @return 命令输出尾部，失败返回 "Error: ..."
     */
    @Tool(DESCRIPTION)
    public String executeCommand(String command) {
        if (command == null || command.trim().isEmpty()) {
            return "Error: Command cannot be empty";
        }

        // 元字符黑名单校验
        for (int i = 0; i < command.length(); i++) {
            if (FORBIDDEN_CHARS.indexOf(command.charAt(i)) >= 0) {
                return "Error: Command contains forbidden shell character: '" + command.charAt(i) + "'";
            }
        }

        // 按空白拆分（多个空格也兼容）
        List<String> parts = splitCommand(command);
        if (parts.isEmpty()) {
            return "Error: Command is empty after split";
        }

        String cmdName = parts.get(0);
        // 白名单校验，Windows 下尝试追加 .cmd/.exe/.bat 后缀
        String resolvedCmd = resolveCommand(cmdName);
        if (resolvedCmd == null) {
            return "Error: Command not in whitelist: " + cmdName +
                ". Allowed: " + ALLOWED_COMMANDS;
        }

        List<String> cmdList = new ArrayList<>(parts);
        cmdList.set(0, resolvedCmd);

        if (channel != null) {
            channel.send(CodingSseEvent.thinking("正在执行命令：" + command));
            channel.send(CodingSseEvent.of("cmd", null, command, null, "running"));
        }

        Path logFile = null;
        Process process = null;
        try {
            logFile = Files.createTempFile("coding-cmd-", ".log");
            ProcessBuilder builder = new ProcessBuilder(cmdList);
            builder.directory(rootDirectory.toFile());
            builder.redirectErrorStream(true);
            builder.redirectOutput(logFile.toFile());
            process = builder.start();

            String result;
            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                stop(process);
                String tail = readTail(logFile, MAX_OUTPUT_BYTES);
                if (channel != null) {
                    channel.send(CodingSseEvent.of("cmd", null, command,
                        "超时（" + TIMEOUT_SECONDS + "s）\n" + tail, "done"));
                }
                return "Error: command timed out after " + TIMEOUT_SECONDS + "s\n" + tail;
            }

            int exitCode = process.exitValue();
            String output = readTail(logFile, MAX_OUTPUT_BYTES);
            if (channel != null) {
                channel.send(CodingSseEvent.of("cmd", null, command,
                    output, "done"));
            }
            result = exitCode == 0 ? output : "Error: exit " + exitCode + "\n" + output;
            return result;

        } catch (InterruptedException ex) {
            if (process != null) {
                process.destroyForcibly();
            }
            Thread.currentThread().interrupt();
            return "Error: command interrupted";
        } catch (IOException ex) {
            logger.error("Error executing command: {}", command, ex);
            return "Error: " + ex.getMessage();
        } finally {
            if (logFile != null) {
                try {
                    Files.deleteIfExists(logFile);
                } catch (IOException ignored) {
                    // 临时文件清理失败不影响主流程
                }
            }
        }
    }

    /**
     * 解析命令名：白名单匹配，Windows 下追加后缀重试。
     */
    private String resolveCommand(String cmdName) {
        if (ALLOWED_COMMANDS.contains(cmdName)) {
            return cmdName;
        }
        // Windows 下 npm/pnpm 等可能是 .cmd
        if (isWindows()) {
            for (String suffix : new String[]{".cmd", ".exe", ".bat"}) {
                String candidate = cmdName + suffix;
                String baseName = stripSuffix(cmdName);
                if (ALLOWED_COMMANDS.contains(baseName)) {
                    return candidate;
                }
            }
        }
        return null;
    }

    private String stripSuffix(String name) {
        for (String suffix : new String[]{".cmd", ".exe", ".bat"}) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    private boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    /**
     * 按空白拆分命令行（不处理引号，保持简单；元字符已在上游拦截）。
     */
    private List<String> splitCommand(String command) {
        List<String> parts = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (Character.isWhitespace(c)) {
                if (cur.length() > 0) {
                    parts.add(cur.toString());
                    cur.setLength(0);
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            parts.add(cur.toString());
        }
        return parts;
    }

    private static void stop(Process process) throws InterruptedException {
        process.destroy();
        if (!process.waitFor(2, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(2, TimeUnit.SECONDS);
        }
    }

    /**
     * 读取文件尾部（抄自 FfmpegProcessRunner.readTail）。
     */
    static String readTail(Path path, int maxBytes) throws IOException {
        if (!Files.exists(path)) {
            return "";
        }
        long size = Files.size(path);
        int bytesToRead = (int) Math.min(size, maxBytes);
        ByteBuffer buffer = ByteBuffer.allocate(bytesToRead);
        try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ)) {
            channel.position(Math.max(0, size - bytesToRead));
            while (buffer.hasRemaining() && channel.read(buffer) >= 0) {
                // 读取直到尾部
            }
        }
        return new String(buffer.array(), 0, buffer.position(), StandardCharsets.UTF_8).trim();
    }

    @Override
    public String getToolName() {
        return "execute_command";
    }

    @Override
    public String getDisplayName() {
        return "执行命令";
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }
}
