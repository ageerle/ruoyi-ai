package org.ruoyi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

import java.net.InetSocketAddress;
import java.net.ServerSocket;

/**
 * 启动程序
 *
 * @author Lion Li
 */
@SpringBootApplication
public class RuoYiAIApplication {

    public static void main(String[] args) {
        killPortProcess(6039);
        SpringApplication application = new SpringApplication(RuoYiAIApplication.class);
        application.setApplicationStartup(new BufferingApplicationStartup(2048));
        application.run(args);
        System.out.println("(♥◠‿◠)ﾉﾞ     RuoYi-AI启动成功   ლ(´ڡ`ლ)");
    }

    /**
     * 检查并终止占用指定端口的进程
     *
     * @param port 端口号
     */
    private static void killPortProcess(int port) {
        try {
            if (!isPortInUse(port)) {
                return;
            }
            System.out.println("端口 " + port + " 已被占用，正在查找并终止进程...");

            ProcessBuilder pb = new ProcessBuilder("netstat", "-ano");
            Process process = pb.start();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(":" + port + " ") && line.contains("LISTENING")) {
                    String[] parts = line.trim().split("\\s+");
                    String pid = parts[parts.length - 1];
                    System.out.println("找到占用端口 " + port + " 的进程 PID: " + pid + "，正在终止...");

                    ProcessBuilder killPb = new ProcessBuilder("taskkill", "/F", "/PID", pid);
                    Process killProcess = killPb.start();
                    int exitCode = killProcess.waitFor();
                    if (exitCode == 0) {
                        System.out.println("进程 " + pid + " 已成功终止");
                    } else {
                        System.out.println("终止进程 " + pid + " 失败，exitCode: " + exitCode);
                    }
                    break;
                }
            }

            // 等待一小段时间确保端口释放
            Thread.sleep(500);
        } catch (Exception e) {
            System.out.println("检查/终止端口进程时发生异常: " + e.getMessage());
        }
    }

    /**
     * 检查端口是否被占用
     */
    private static boolean isPortInUse(int port) {
        try (ServerSocket socket = new ServerSocket()) {
            socket.bind(new InetSocketAddress(port));
            return false;
        } catch (Exception e) {
            return true;
        }
    }

}
