package org.ruoyi.mcp.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.Disposable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


@Component
public class McpProcessSSEManager {

    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();
    private final Map<String, ProcessInfo> processInfos = new ConcurrentHashMap<>();
    private final Map<String, WebClient> sseClients = new ConcurrentHashMap<>();
    private final Map<String, Disposable> sseSubscriptions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private McpSSEToolInvoker mcpToolInvoker;

    /**
     * 启动 MCP 服务器进程（SSE 模式）
     */
    public boolean startMcpServer(String serverName, String command, List<String> args, Map<String, String> env) {
        try {
            System.out.println("准备启动 MCP 服务器 (SSE 模式): " + serverName);

            // 如果已经运行，先停止
            if (isMcpServerRunning(serverName)) {
                stopMcpServer(serverName);
            }

            // 构建命令
            List<String> commandList = buildCommandList(command, args);

            // 创建 ProcessBuilder
            ProcessBuilder processBuilder = new ProcessBuilder(commandList);
            processBuilder.redirectErrorStream(true);

            // 设置工作目录
            String workingDir = System.getProperty("user.dir");
            processBuilder.directory(new File(workingDir));

            // 打印调试信息
            System.out.println("=== ProcessBuilder 调试信息 ===");
            System.out.println("完整命令列表: " + commandList);
            System.out.println("================================");

            // 执行命令
            Process process = processBuilder.start();
            runningProcesses.put(serverName, process);

            ProcessInfo processInfo = new ProcessInfo();
            processInfo.setStartTime(System.currentTimeMillis());
            processInfo.setPid(getProcessId(process));
            processInfos.put(serverName, processInfo);

            // 启动日志读取线程
            ExecutorService executorService = Executors.newCachedThreadPool();
            executorService.submit(() -> readProcessOutput(serverName, process));

            // 等待进程启动
            Thread.sleep(3000);
            boolean isAlive = process.isAlive();

            if (isAlive) {
                System.out.println("✓ MCP 服务器 [" + serverName + "] 启动成功");
                // 初始化 SSE 连接
                initializeSseConnection(serverName);
            } else {
                System.err.println("✗ MCP 服务器 [" + serverName + "] 启动失败");
                readErrorOutput(process);
            }

            return isAlive;

        } catch (Exception e) {
            System.err.println("✗ 启动 MCP 服务器 [" + serverName + "] 失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String getProcessId(Process process) {
        try {
            return String.valueOf(process.pid());
        } catch (Exception e) {
            return "unknown";
        }
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
     * 初始化 SSE 连接
     */
    private void initializeSseConnection(String serverName) {
        try {
            // 创建 WebClient 用于 SSE 连接
            WebClient webClient = WebClient.builder()
                    .baseUrl("http://localhost:3000") // 假设默认端口 3000
                    .build();

            sseClients.put(serverName, webClient);

            // 建立 SSE 连接
            String sseUrl = "/sse/" + serverName; // SSE 端点

            Disposable subscription = webClient.get()
                    .uri(sseUrl)
                    .accept(MediaType.TEXT_EVENT_STREAM)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .subscribe(
                            event -> handleSseEvent(serverName, event),
                            error -> System.err.println("SSE 连接错误 [" + serverName + "]: " + error.getMessage()),
                            () -> System.out.println("SSE 连接完成 [" + serverName + "]")
                    );

            sseSubscriptions.put(serverName, subscription);
            System.out.println("✓ SSE 连接建立成功 [" + serverName + "]");

        } catch (Exception e) {
            System.err.println("✗ 建立 SSE 连接失败 [" + serverName + "]: " + e.getMessage());
        }
    }

    /**
     * 处理 SSE 事件
     */
    private void handleSseEvent(String serverName, String event) {
        try {
            System.out.println("收到来自 [" + serverName + "] 的 SSE 事件: " + event);

            // 解析 SSE 事件
            if (event.startsWith("data: ")) {
                String jsonData = event.substring(6); // 移除 "data: " 前缀
                Map<String, Object> message = objectMapper.readValue(jsonData, Map.class);

                // 处理不同类型的事件
                String type = (String) message.get("type");
                if ("tool_response".equals(type)) {
                    mcpToolInvoker.handleSseResponse(serverName, message);
                } else if ("tool_error".equals(type)) {
                    mcpToolInvoker.handleSseError(serverName, message);
                } else if ("progress".equals(type)) {
                    handleProgressEvent(serverName, message);
                } else {
                    System.out.println("[" + serverName + "] 未知事件类型: " + type);
                }
            }

        } catch (Exception e) {
            System.err.println("处理 SSE 事件失败 [" + serverName + "]: " + e.getMessage());
        }
    }

    /**
     * 处理进度事件
     */
    private void handleProgressEvent(String serverName, Map<String, Object> message) {
        Object progress = message.get("progress");
        Object messageText = message.get("message");
        System.out.println("[" + serverName + "] 进度: " + progress + " - " + messageText);
    }


    /**
     * 构建命令列表
     */
    private List<String> buildCommandList(String command, List<String> args) {
        List<String> commandList = new ArrayList<>();

        if (isWindows() && "npx".equalsIgnoreCase(command)) {
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
     * 停止 MCP 服务器进程
     */
    public boolean stopMcpServer(String serverName) {
        // 停止 SSE 连接
        Disposable subscription = sseSubscriptions.remove(serverName);
        if (subscription != null && !subscription.isDisposed()) {
            subscription.dispose();
        }

        sseClients.remove(serverName);

        // 停止进程
        Process process = runningProcesses.remove(serverName);
        ProcessInfo processInfo = processInfos.remove(serverName);

        if (process != null && process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(10, TimeUnit.SECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
                System.out.println("MCP 服务器 [" + serverName + "] 已停止");
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
     * 检查 MCP 服务器是否运行
     */
    public boolean isMcpServerRunning(String serverName) {
        Process process = runningProcesses.get(serverName);
        return process != null && process.isAlive();
    }

    /**
     * 进程信息类
     */
    public static class ProcessInfo {
        private String pid;
        private long startTime;

        public String getPid() {
            return pid;
        }

        public void setPid(String pid) {
            this.pid = pid;
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(long startTime) {
            this.startTime = startTime;
        }

        public long getUptime() {
            return System.currentTimeMillis() - startTime;
        }
    }
}
