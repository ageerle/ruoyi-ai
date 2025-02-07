package org.ruoyi.common.core.manager;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.Threads;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.concurrent.ScheduledExecutorService;

/**
 * 确保应用退出时能关闭后台线程
 *
 * @author Lion Li
 */
@Slf4j
@Component
public class ShutdownManager {

    @Autowired
    @Qualifier("scheduledExecutorService")
    private ScheduledExecutorService scheduledExecutorService;

    @PreDestroy
    public void destroy() {
        shutdownAsyncManager();
    }

    /**
     * 停止异步执行任务
     */
    private void shutdownAsyncManager() {
        try {
            log.info("====关闭后台任务任务线程池====");
            Threads.shutdownAndAwaitTermination(scheduledExecutorService);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
