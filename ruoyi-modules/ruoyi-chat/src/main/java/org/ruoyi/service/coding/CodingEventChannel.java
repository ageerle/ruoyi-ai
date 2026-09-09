package org.ruoyi.service.coding;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * 编程能力跨线程事件通道。
 *
 * <p>结构抄自 {@code OutputChannel}，但队列元素是结构化 {@link CodingSseEvent} 而非 String。
 * OutputChannel 的 {@code send(String)} 塞 JSON 字符串再让 drain 端解析是反模式；
 * 这里直接传结构化对象，drain 时再由 Service 层序列化。
 *
 * <p>调用链路：
 * <pre>
 *   异步线程（工具执行、LLM 回调）-> channel.send(event)
 *   drain 线程                       -> channel.drain(emitter::send)
 * </pre>
 *
 * @author ageerle
 */
@Slf4j
public class CodingEventChannel {

    static final int QUEUE_CAPACITY = 4096;
    static final String SAFE_ERROR_MESSAGE = "编程任务执行失败，请稍后重试";

    private final BlockingQueue<CodingSseEvent> queue = new LinkedBlockingQueue<>(QUEUE_CAPACITY);
    private final AtomicReference<CodingSseEvent> terminalError = new AtomicReference<>();
    private final CountDownLatch completed = new CountDownLatch(1);
    private final Object lifecycleLock = new Object();
    private boolean accepting = true;

    /**
     * 写入一个事件：线程安全，队列满时 100ms 超时丢弃。
     */
    public void send(CodingSseEvent event) {
        if (event == null) {
            return;
        }
        // error 是终态，不进入有界数据队列；这样即使队列已满也不会丢失唯一错误事件。
        if ("error".equals(event.eventType())) {
            finish(CodingSseEvent.error(SAFE_ERROR_MESSAGE));
            return;
        }
        synchronized (lifecycleLock) {
            if (!accepting) {
                return;
            }
            try {
                if (!queue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                    log.warn("coding_event_channel status=DROPPED reason=QUEUE_FULL");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 标记正常完成。
     */
    public void complete() {
        finish(null);
    }

    /**
     * 标记错误完成，附带一条 error 事件。
     */
    public void completeWithError(Throwable ignored) {
        finish(CodingSseEvent.error(SAFE_ERROR_MESSAGE));
    }

    private void finish(CodingSseEvent safeTerminalError) {
        synchronized (lifecycleLock) {
            if (!accepting) {
                return;
            }
            accepting = false;
            if (safeTerminalError != null) {
                terminalError.set(safeTerminalError);
                // A failure terminal is more important than stale progress. Dropping the backlog
                // lets the drain thread deliver the error before the service closes its emitter.
                queue.clear();
            }
        }
        completed.countDown();
    }

    /**
     * 阻塞读取事件并逐个回调；遇 DONE 退出。
     *
     * @param emitter 事件消费回调
     */
    public void drain(Consumer<CodingSseEvent> emitter) throws InterruptedException {
        while (true) {
            CodingSseEvent pendingTerminalError = terminalError.getAndSet(null);
            if (pendingTerminalError != null) {
                emitter.accept(pendingTerminalError);
                return;
            }
            CodingSseEvent msg = queue.poll(200, TimeUnit.MILLISECONDS);
            if (msg != null) {
                emitter.accept(msg);
            } else if (completed.getCount() == 0 && queue.isEmpty()) {
                break;
            }
        }
        CodingSseEvent safeTerminalError = terminalError.getAndSet(null);
        if (safeTerminalError != null) {
            emitter.accept(safeTerminalError);
        }
    }

    public boolean isCompleted() {
        return completed.getCount() == 0;
    }
}
