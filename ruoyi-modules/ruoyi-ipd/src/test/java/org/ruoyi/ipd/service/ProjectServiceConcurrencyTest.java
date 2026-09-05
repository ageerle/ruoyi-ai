package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * PERF-01 并发原子取号单测
 *
 * <p>回归 nextCode() 在 50 并发线程下的取号唯一性。
 * 修复前：selectList(likeRight) + 内存求 max + insert，无锁 → 50 并发必重复（RED 已验证）。
 * 修复后：方法级 synchronized（单测无 AOP）+ 生产 @Lock4j 跨 JVM + DB UNIQUE KEY uk_projects_code。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProjectServiceConcurrencyTest {

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private GateEngine gateEngine;
    @Mock
    private ProjectBootstrapService projectBootstrapService;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectMapper, productMapper, auditLogService, gateEngine, projectBootstrapService);
    }

    @Test
    @DisplayName("nextCode 并发 50 线程：返回的编码互不相同（synchronized 锁保证原子取号）")
    void nextCode_concurrent50_shouldReturnDistinctCodes() throws Exception {
        // 模拟 DB 当前最大编码序列的"实时"读：selectList 返回 sharedMax 当前值
        AtomicInteger sharedMax = new AtomicInteger(0);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenAnswer(inv -> {
                int cur = sharedMax.get();
                if (cur == 0) {
                    return List.<Project>of();
                }
                Project p = new Project();
                p.setCode("PRJ-2026-" + String.format("%03d", cur));
                return List.of(p);
            });

        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        Set<String> codes = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    String code = service.nextCode();
                    if (!codes.add(code)) {
                        errors.incrementAndGet();
                    }
                    sharedMax.incrementAndGet();
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(finished).as("50 threads should finish in 30s").isTrue();
        assertThat(errors.get()).as("no duplicate codes, no errors").isZero();
        assertThat(codes).as("all 50 codes distinct").hasSize(n);
    }
}
