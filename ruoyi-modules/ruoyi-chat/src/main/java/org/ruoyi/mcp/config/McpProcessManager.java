package org.ruoyi.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.mcp.service.McpInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class McpProcessManager {

    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<String, McpServerProcess> mcpServerProcesses = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, BufferedWriter> processWriters = new ConcurrentHashMap<>();
    private final Map<String, BufferedReader> processReaders = new ConcurrentHashMap<>();

    @Autowired
    private McpInfoService mcpInfoService;

    /**
     * 启动 MCP 服务器进程（支持环境变量）
     */
    public boolean startMcpServer(String serverName, McpServerConfig serverConfig) {
        try {
            log.info("启动MCP服务器进程: {}", serverName);

            ProcessBuilder processBuilder = new ProcessBuilder();

            // 构建命令
            List<String> commandList = buildCommandListWithFullPaths(serverConfig.getCommand(), serverConfig.getArgs());


            processBuilder.command(commandList);

            // 设置工作目录
            if (serverConfig.getWorkingDirectory() != null) {
                processBuilder.directory(new File(serverConfig.getWorkingDirectory()));
            } else {
                processBuilder.directory(new File(System.getProperty("user.dir")));
            }

            // 设置环境变量
            if (serverConfig.getEnv() != null) {
                processBuilder.environment().putAll(serverConfig.getEnv());
            }
            // ===== 关键：在 start 之前打印完整的调试信息 =====
            System.out.println("=== ProcessBuilder 调试信息 ===");
            System.out.println("完整命令列表: " + commandList);
            System.out.println("命令字符串: " + String.join(" ", commandList));
            System.out.println("工作目录: " + processBuilder.directory());
            System.out.println("================================");
            //https://www.modelscope.cn/mcp/servers/@worryzyy/howtocook-mcp

            // 启动进程
            Process process = processBuilder.start();
            // 获取输入输出流
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            processWriters.put(serverName, writer);
            processReaders.put(serverName, reader);

            // 存储进程引用
            McpServerProcess serverProcess = new McpServerProcess(serverName, process, serverConfig);
            mcpServerProcesses.put(serverName, serverProcess);
            // 启动日志读取线程
            executorService.submit(() -> readProcessOutput(serverName, process));
            // 启动 MCP 通信监听线程
            executorService.submit(() -> listenMcpMessages(serverName, reader));

            // 更新服务器状态
            mcpInfoService.enableTool(serverName);
            boolean isAlive = process.isAlive();

            if (isAlive) {
                log.info("成功启动MCP服务器: {} 命令: {}", serverName, commandList);
            } else {
                System.err.println("✗ MCP server [" + serverName + "] failed to start");
                // 读取错误输出
                readErrorOutput(process);
            }
            return true;

        } catch (IOException e) {
            log.error("启动MCP服务器进程失败: " + serverName, e);

            // 更新服务器状态为禁用
            //mcpInfoService.disableTool(serverName);

            throw new RuntimeException("Failed to start MCP server process: " + e.getMessage(), e);

        }

    }

    /**
     * 发送 MCP 消息
     */
    public boolean sendMcpMessage(String serverName, Map<String, Object> message) {
        try {
            BufferedWriter writer = processWriters.get(serverName);
            if (writer == null) {
                System.err.println("未找到服务器 [" + serverName + "] 的输出流");
                return false;
            }

            String jsonMessage = objectMapper.writeValueAsString(message);
            System.out.println("发送消息到 [" + serverName + "]: " + jsonMessage);

            writer.write(jsonMessage);
            writer.newLine();
            writer.flush();

            return true;
        } catch (Exception e) {
            System.err.println("发送消息到 [" + serverName + "] 失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * 监听 MCP 消息
     */
    private void listenMcpMessages(String serverName, BufferedReader reader) {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                try {
                    // 解析收到的 JSON 消息
                    Map<String, Object> message = objectMapper.readValue(line, Map.class);
                    System.out.println("收到来自 [" + serverName + "] 的消息: " + message);

                    // 处理不同类型的 MCP 消息
                    handleMessage(serverName, message);

                } catch (Exception e) {
                    System.err.println("解析消息失败: " + line + ", 错误: " + e.getMessage());
                    // 如果不是 JSON，当作普通日志输出
                    System.out.println("[" + serverName + "] 日志: " + line);
                }
            }
        } catch (IOException e) {
            if (isMcpServerRunning(serverName)) {
                System.err.println("监听 [" + serverName + "] 消息时出错: " + e.getMessage());
            }
        }
    }


    /**
     * 处理 MCP 消息（更新版本）
     */
    private void handleMessage(String serverName, Map<String, Object> message) {
        String type = (String) message.get("type");
        if (type == null) return;

        switch (type) {
            case "ready":
                System.out.println("MCP 服务器 [" + serverName + "] 准备就绪");
                break;
            case "response":
                System.out.println("MCP 服务器 [" + serverName + "] 响应: " + message.get("data"));
                break;
            case "error":
                System.err.println("MCP 服务器 [" + serverName + "] 错误: " + message.get("message"));
                break;
            default:
                System.out.println("MCP 服务器 [" + serverName + "] 未知消息类型: " + type);
                break;
        }
    }

    /**
     * 构建命令列表
     */
    private List<String> buildCommandListWithFullPaths(String command, List<String> args) {
        List<String> commandList = new ArrayList<>();

        if (isWindows() && "npx".equalsIgnoreCase(command)) {
            // 在 Windows 上使用 cmd.exe 包装以确保兼容性
            commandList.add("cmd.exe");
            commandList.add("/c");
            commandList.add("npx");
            commandList.addAll(args);
        } else {
            commandList.add(command);
            commandList.addAll(args);
        }

        return commandList;
    }

    /**
     * 检查是否为 Windows 系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }


    /**
     * 读取错误输出
     */
    private void readErrorOutput(Process process) {
        try {
            InputStream errorStream = process.getErrorStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
            String line;
            while ((line = reader.readLine()) != null) {
                System.err.println("ERROR: " + line);
            }
        } catch (Exception e) {
            System.err.println("Failed to read error output: " + e.getMessage());
        }
    }

    /**
     * 停止 MCP 服务器进程
     */
    public boolean stopMcpServer(String serverName) {
        Process process = runningProcesses.remove(serverName);
        BufferedWriter writer = processWriters.remove(serverName);
        BufferedReader reader = processReaders.remove(serverName);
        try {
            if (writer != null) {
                writer.close();
            }
            if (reader != null) {
                reader.close();
            }
        } catch (IOException e) {
            System.err.println("关闭流时出错: " + e.getMessage());
        }
        // 更新服务器状态为禁用
        mcpInfoService.disableTool(serverName);

        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(1, TimeUnit.SECONDS);
                }
                System.out.println("MCP server [" + serverName + "] stopped");
                return true;
            } catch (InterruptedException e) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    /**
     * 重启 MCP 服务器进程
     */
    public boolean restartMcpServer(String serverName, String command, List<String> args, Map<String, String> env) {
        stopMcpServer(serverName);
        McpServerConfig mcpServerConfig = new McpServerConfig();
        mcpServerConfig.setCommand(command);
        mcpServerConfig.setArgs(args);
        mcpServerConfig.setEnv(env);
        return startMcpServer(serverName, mcpServerConfig);
    }

    /**
     * 检查 MCP 服务器是否运行
     */
    public boolean isMcpServerRunning(String serverName) {
        Process process = runningProcesses.get(serverName);
        return process != null && process.isAlive();
    }

    /**
     * 获取所有运行中的 MCP 服务器
     */
    public Set<String> getRunningMcpServers() {
        Set<String> running = new HashSet<>();
        for (Map.Entry<String, Process> entry : runningProcesses.entrySet()) {
            if (entry.getValue().isAlive()) {
                running.add(entry.getKey());
            }
        }
        return running;
    }

    /**
     * 获取进程信息
     */
    public McpServerProcess getProcessInfo(String serverName) {
        return mcpServerProcesses.get(serverName);
    }

    private void readProcessOutput(String serverName, Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null && process.isAlive()) {
                System.out.println("[" + serverName + "] " + line);
            }
        } catch (IOException e) {
            System.err.println("Error reading output from " + serverName + ": " + e.getMessage());
        }
    }

    private String getProcessId(Process process) {
        try {
            // Java 9+ 可以直接获取 PID
            return String.valueOf(process.pid());
        } catch (Exception e) {
            // Java 8 兼容处理
            return "unknown";
        }
    }

    /**
     * MCP服务器进程信息
     */
    public static class McpServerProcess {
        private final String name;
        private final Process process;
        private final McpServerConfig config;
        private final LocalDateTime startTime;

        public McpServerProcess(String name, Process process, McpServerConfig config) {
            this.name = name;
            this.process = process;
            this.config = config;
            this.startTime = LocalDateTime.now();
        }

        // Getters
        public String getName() {
            return name;
        }

        public Process getProcess() {
            return process;
        }

        public McpServerConfig getConfig() {
            return config;
        }

        public LocalDateTime getStartTime() {
            return startTime;
        }
    }
}
