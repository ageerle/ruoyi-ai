package org.ruoyi.observability;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 跨线程事件总线
 *
 * 写入端（异步线程）：StreamingOutputWrapper / SupervisorStreamListener
 * 读取端（SSE 线程）：ChatServiceFacade.drain
 *
 * 调用链路：
 *   SSE请求 -> 创建 OutputChannel
 *           -> Supervisor.invoke() [同步阻塞调用子Agent]
 *              ├── SupervisorStreamListener -> channel.send()
 *              └── searchAgent.search()
 *                  └── StreamingOutputWrapper -> channel.send() [每个token]
 *           -> channel.complete()
 *   drain线程 -> channel.drain() -> SSE实时推送
 *
 * @author ageerle@163.com
 * @date 2025/04/10
 */
public class OutputChannel {

    private static final String DONE = "__DONE__";
    private static final Map<String, OutputChannel> REGISTRY = new ConcurrentHashMap<>();

    private final BlockingQueue<String> queue = new LinkedBlockingQueue<>(4096);
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final CountDownLatch completed = new CountDownLatch(1);

    /**
     * 创建并注册到全局注册表
     */
    public static OutputChannel create(String requestId) {
        OutputChannel ch = new OutputChannel();
        REGISTRY.put(requestId, ch);
        return ch;
    }

    /**
     * 从全局注册表移除
     */
    public static void remove(String requestId) {
        REGISTRY.remove(requestId);
    }

    /**
     * 从全局注册表获取
     */
    public static OutputChannel get(String requestId) {
        return REGISTRY.get(requestId);
    }

    /**
     * 写入：线程安全，非阻塞，队列满时丢弃
     */
public void send(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        try {
            if (!queue.offer(text, 100, TimeUnit.MILLISECONDS)) {
                System.err.println("[OutputChannel] 队列满，丢弃消息: " + truncate(text, 100));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 标记完成
     */
    public void complete() {
        queue.offer(DONE);
        completed.countDown();
    }

    /**
     * 标记错误完成
     */
    public void completeWithError(Throwable t) {
        error.set(t);
        queue.offer("\n[错误] 致命错误: " + t.getMessage());
        queue.offer(DONE);
        completed.countDown();
    }

    /**
     * 读取：阻塞迭代，配合 SSE 使用
     */
    public void drain(Consumer<String> emitter) throws InterruptedException {
        while (true) {
            String msg = queue.poll(200, TimeUnit.MILLISECONDS);
            if (msg != null) {
                if (DONE.equals(msg)) {
                    break;
                }
                emitter.accept(msg);
            } else {
                if (completed.getCount() == 0 && queue.isEmpty()) {
                    break;
                }
            }
        }
        Throwable t = error.get();
        if (t != null && !(t instanceof InterruptedException)) {
            throw new RuntimeException("Agent 执行出错", t);
        }
    }

    /**
     * 检查是否已完成
     */
    public boolean isCompleted() {
        return completed.getCount() == 0;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "null";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
