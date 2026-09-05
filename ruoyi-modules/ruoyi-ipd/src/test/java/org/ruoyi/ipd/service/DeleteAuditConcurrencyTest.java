package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.service.executor.ProjectSoftDeleteExecutor;

import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-6.2 并发审批单测：模拟两个超管同时通过同一 DeletionRequest；
 * 由于状态机 + updateById 影响行数，第二次调用必然失败并回滚。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DeleteAuditConcurrencyTest {

    @Mock private DeletionRequestMapper deletionRequestMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private ProjectMapper projectMapper;

    @Test
    @DisplayName("P0-6.2.C1 两个超管并发 approveAndExecute：恰好 1 成功 + 1 抛异常")
    void concurrentApprovalSingleWinner() throws InterruptedException {
        DeleteAuditService service = new DeleteAuditService(deletionRequestMapper, auditLogService, List.of(
            new ProjectSoftDeleteExecutor(projectMapper)
        ));

        DeletionRequest req = pending();
        when(deletionRequestMapper.selectById(10L)).thenReturn(req);
        AtomicInteger updateCount = new AtomicInteger(0);
        when(deletionRequestMapper.updateById(any(DeletionRequest.class))).thenAnswer(inv -> {
            int n = updateCount.incrementAndGet();
            return n == 1 ? 1 : 0;
        });
        Project project = Project.builder().id(100L).code("PRJ").delFlag("0").build();
        when(projectMapper.selectById(100L)).thenReturn(project);
        when(projectMapper.updateById(any(Project.class))).thenReturn(1);

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(2);
        AtomicInteger success = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        ExecutorService exec = Executors.newFixedThreadPool(2);
        for (int i = 0; i < 2; i++) {
            exec.submit(() -> {
                try {
                    start.await();
                    service.approveAndExecute(10L, 99L);
                    success.incrementAndGet();
                } catch (ServiceException e) {
                    failed.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();
        exec.shutdownNow();

        assertThat(success.get()).isEqualTo(1);
        assertThat(failed.get()).isEqualTo(1);
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    private DeletionRequest pending() {
        return DeletionRequest.builder()
            .id(10L).entityType("projects").entityId(100L).reason("并发测试")
            .requesterId(1L).leaderId(2L).leaderDecision("APPROVE").leaderDecidedAt(new Date())
            .adminDueAt(new Date(System.currentTimeMillis() + 86_400_000))
            .status(DeletionRequestService.ST_ADMIN_REVIEW)
            .build();
    }
}