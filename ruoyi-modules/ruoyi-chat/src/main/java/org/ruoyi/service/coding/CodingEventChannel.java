package org.ruoyi.service.coding;

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
public class CodingEventChannel {

    /** DONE 哨兵，drain 遇到即退出 */
    private static final CodingSseEvent DONE = new CodingSseEvent("__done__", null, null, null, null);

    private final BlockingQueue<CodingSseEvent> queue = new LinkedBlockingQueue<>(4096);
    private final AtomicReference<Throwable> error = new AtomicReference<>();
    private final CountDownLatch completed = new CountDownLatch(1);

    /**
     * 写入一个事件：线程安全，队列满时 100ms 超时丢弃。
     */
    public void send(CodingSseEvent event) {
        if (event == null) {
            return;
        }
        try {
            if (!queue.offer(event, 100, TimeUnit.MILLISECONDS)) {
                // 队列满，丢弃但不中断流程
                System.err.println("[CodingEventChannel] 队列满，丢弃事件: " + event.eventType());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 标记正常完成。
     */
    public void complete() {
        queue.offer(DONE);
        completed.countDown();
    }

    /**
     * 标记错误完成，附带一条 error 事件。
     */
    public void completeWithError(Throwable t) {
        error.set(t);
        if (t != null && t.getMessage() != null) {
            queue.offer(CodingSseEvent.error(t.getMessage()));
        }
        queue.offer(DONE);
        completed.countDown();
    }

    /**
     * 阻塞读取事件并逐个回调；遇 DONE 退出。
     *
     * @param emitter 事件消费回调
     */
    public void drain(Consumer<CodingSseEvent> emitter) throws InterruptedException {
        while (true) {
            CodingSseEvent msg = queue.poll(200, TimeUnit.MILLISECONDS);
            if (msg != null) {
                if (DONE == msg || "__done__".equals(msg.eventType())) {
                    break;
                }
                emitter.accept(msg);
            } else if (completed.getCount() == 0 && queue.isEmpty()) {
                break;
            }
        }
    }

    public boolean isCompleted() {
        return completed.getCount() == 0;
    }
}
