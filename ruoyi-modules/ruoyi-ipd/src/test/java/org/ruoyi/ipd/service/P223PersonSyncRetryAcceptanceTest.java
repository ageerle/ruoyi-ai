package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.service.PersonSyncService.FailureKind;
import org.ruoyi.ipd.service.PersonSyncService.JobStatus;
import org.ruoyi.ipd.service.PersonSyncService.SyncJob;
import org.ruoyi.ipd.service.PersonSyncService.SyncProcessor;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * P2-2.3 同步任务重试与异常项回补 + P2-2.1 幂等键重放保护验收测试。
 *
 * <p>覆盖：
 * <ul>
 *   <li>submit: 正常同步 → SUCCESS / TRANSIENT 失败 → RETRYING 排程下次 / PERMANENT → FAILED</li>
 *   <li>retry: FAILED → 重试 / SUCCESS 重试拒绝 / 超 maxAttempts 拒绝</li>
 *   <li>retryAll: 批量重试 PENDING/FAILED，跳过 SUCCESS/RETRYING/已耗尽</li>
 *   <li>幂等键重放：同 idempotencyKey 重放返原 jobId，不创建新任务</li>
 *   <li>audit append: SUBMIT/MANUAL_RETRY/BATCH_RETRY 三类动作</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@DisplayName("P2-2.3 + P2-2.1 同步任务重试与幂等")
class P223PersonSyncRetryAcceptanceTest {

    @Mock PersonMapper personMapper;
    @Mock AuditLogService auditLogService;

    @InjectMocks PersonSyncService service;

    private IpdActor admin;

    @BeforeEach
    void setUp() {
        admin = new IpdActor(1L, "Admin", "SUPER_ADMIN", null);
        // Audit 静默接受
    }

    /** 构造一个必成功的处理器。 */
    private SyncProcessor okProcessor() {
        return job -> { /* no-op */ };
    }

    /** 构造一个必失败的处理器（TRANSIENT）。 */
    private SyncProcessor transientFailProcessor(String msg) {
        return job -> { throw new PersonSyncService.SyncFailureException(FailureKind.TRANSIENT, msg); };
    }

    /** 构造一个必失败的处理器（PERMANENT）。 */
    private SyncProcessor permanentFailProcessor(String msg) {
        return job -> { throw new PersonSyncService.SyncFailureException(FailureKind.PERMANENT, msg); };
    }

    /** 构造一个前 N 次失败后成功的处理器。 */
    private SyncProcessor flakyProcessor(int failTimes, FailureKind kind, String msg) {
        AtomicInteger counter = new AtomicInteger(0);
        return job -> {
            int n = counter.incrementAndGet();
            if (n <= failTimes) {
                throw new PersonSyncService.SyncFailureException(kind, msg + " (attempt " + n + ")");
            }
            // 第 N+1 次成功
        };
    }

    @Test
    @DisplayName("submit: 正常同步 → SUCCESS + attempts=1")
    void submit_success() {
        service.setProcessor(okProcessor());
        var job = service.submit("E001", "key-1", admin);

        assertThat(job.status).isEqualTo(JobStatus.SUCCESS);
        assertThat(job.attempts).isEqualTo(1);
        assertThat(job.failureKind).isNull();
        verify(auditLogService, atLeastOnce()).append(any());
    }

    @Test
    @DisplayName("submit: PERMANENT 失败 → FAILED 不重试 + failureKind=PERMANENT")
    void submit_permanentFailure_marksFailed() {
        service.setProcessor(permanentFailProcessor("data validation failed"));
        var job = service.submit("E002", "key-2", admin);

        assertThat(job.status).isEqualTo(JobStatus.FAILED);
        assertThat(job.failureKind).isEqualTo(FailureKind.PERMANENT);
        assertThat(job.attempts).isEqualTo(1);
        assertThat(job.nextRetryAt).isNull();
    }

    @Test
    @DisplayName("submit: TRANSIENT 失败 + 未耗尽 → RETRYING + 排程 nextRetryAt")
    void submit_transientFailure_marksRetrying() {
        service.setProcessor(transientFailProcessor("network timeout"));
        var job = service.submit("E003", "key-3", admin);

        assertThat(job.status).isEqualTo(JobStatus.RETRYING);
        assertThat(job.failureKind).isEqualTo(FailureKind.TRANSIENT);
        assertThat(job.nextRetryAt).isNotNull();
    }

    @Test
    @DisplayName("P2-2.1 幂等键：同 idempotencyKey 重放返原 jobId 不创建新任务")
    void idempotencyKey_replayReturnsSameJob() {
        service.setProcessor(okProcessor());
        String key = "idem-" + UUID.randomUUID();

        var job1 = service.submit("E010", key, admin);
        int sizeBefore = service.listAll().size();
        var job2 = service.submit("E010", key, admin); // 同 key 重放
        int sizeAfter = service.listAll().size();

        assertThat(job2.jobId).isEqualTo(job1.jobId);
        assertThat(sizeAfter).isEqualTo(sizeBefore);
    }

    @Test
    @DisplayName("retry: FAILED 任务可手动重试（成功后 status=SUCCESS）")
    void retry_failedJob_canSucceedOnRetry() {
        // 第一次 PERMANENT 失败 → FAILED；第二次改用 okProcessor
        service.setProcessor(permanentFailProcessor("first attempt failed"));
        var job = service.submit("E020", "k20", admin);
        assertThat(job.status).isEqualTo(JobStatus.FAILED);

        service.setProcessor(okProcessor());
        var retried = service.retry(job.jobId, admin);

        assertThat(retried.status).isEqualTo(JobStatus.SUCCESS);
        assertThat(retried.attempts).isEqualTo(2);
    }

    @Test
    @DisplayName("retry: SUCCESS 任务拒绝重试（仅 FAILED 可重试）")
    void retry_rejectsNonFailedStatus() {
        service.setProcessor(okProcessor());
        var job = service.submit("E030", "k30", admin);
        assertThat(job.status).isEqualTo(JobStatus.SUCCESS);

        assertThatThrownBy(() -> service.retry(job.jobId, admin))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
    }

    @Test
    @DisplayName("retryAll: 批量重试 PENDING/FAILED；跳过 SUCCESS/已耗尽")
    void retryAll_processesPendingAndFailed() {
        // 创建 4 个任务：1 SUCCESS, 1 FAILED (PERMANENT), 1 RETRYING, 1 FAILED (PERMANENT)
        // 通过切换 processor 创建不同状态
        service.setProcessor(okProcessor());
        var j1 = service.submit("E-A", "ka", admin);
        assertThat(j1.status).isEqualTo(JobStatus.SUCCESS);

        service.setProcessor(permanentFailProcessor("bad data"));
        var j2 = service.submit("E-B", "kb", admin);
        assertThat(j2.status).isEqualTo(JobStatus.FAILED);

        service.setProcessor(transientFailProcessor("timeout"));
        var j3 = service.submit("E-C", "kc", admin);
        assertThat(j3.status).isEqualTo(JobStatus.RETRYING);

        service.setProcessor(permanentFailProcessor("another bad data"));
        var j4 = service.submit("E-D", "kd", admin);
        assertThat(j4.status).isEqualTo(JobStatus.FAILED);

        // 切换为 ok processor 让 retryAll 重试 j2/j4 成功
        service.setProcessor(okProcessor());
        var result = service.retryAll(admin);

        // 重试数 = 2（j2 和 j4，j3 是 RETRYING 跳过），成功 2，失败 0，跳过 2（j1 SUCCESS + j3 RETRYING）
        assertThat(result.retried()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(2);
        assertThat(result.failed()).isZero();
        assertThat(result.skipped()).isEqualTo(2);
    }

    @Test
    @DisplayName("retryAll: 已耗尽任务跳过（attempts >= maxAttempts）")
    void retryAll_skipsExhaustedJobs() {
        // 第一次失败后 attempts=1；连续失败直到 attempts=maxAttempts=3 ⇒ FAILED
        service.setProcessor(permanentFailProcessor("always fails"));
        var job = service.submit("E-Z", "kz", admin);

        // 手动 retry 两次把 attempts 推到 3
        service.retry(job.jobId, admin);
        service.retry(job.jobId, admin);
        // 此时 attempts == maxAttempts，状态 FAILED
        assertThat(service.get(job.jobId).attempts).isEqualTo(3);

        // 第三次 retry 应被拒绝（超 maxAttempts）
        assertThatThrownBy(() -> service.retry(job.jobId, admin))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("最大重试次数");

        // retryAll 跳过（attempts >= maxAttempts）
        var result = service.retryAll(admin);
        assertThat(result.retried()).isZero();
        assertThat(result.skipped()).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("listAbnormal: 仅返 FAILED 状态任务")
    void listAbnormal_returnsOnlyFailed() {
        service.setProcessor(okProcessor());
        service.submit("E-OK", "kok", admin);
        service.setProcessor(permanentFailProcessor("bad"));
        service.submit("E-FAIL", "kfail", admin);

        var abnormal = service.listAbnormal();
        assertThat(abnormal).hasSize(1);
        assertThat(abnormal.get(0).status).isEqualTo(JobStatus.FAILED);
        assertThat(abnormal.get(0).employeeNo).isEqualTo("E-FAIL");
    }

    @Test
    @DisplayName("get: 不存在的 jobId 抛 NOT_FOUND")
    void get_notFound_throws() {
        assertThatThrownBy(() -> service.get("sync-xxx-999"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("submit: 处理器抛非 SyncFailureException 异常透传（不分类、不计入任务失败）")
    void submit_propagatesRuntimeException() {
        service.setProcessor(job -> { throw new RuntimeException("unexpected"); });
        assertThatThrownBy(() -> service.submit("E-X", "kx", admin))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("unexpected");
    }

    @Test
    @DisplayName("P2-2.1 幂等键隔离：不同 key 创建独立任务（不互相影响）")
    void differentKeys_createSeparateJobs() {
        service.setProcessor(okProcessor());
        var j1 = service.submit("E-100", "key-100", admin);
        var j2 = service.submit("E-100", "key-200", admin); // 不同 key 即使 employeeNo 相同也独立

        assertThat(j1.jobId).isNotEqualTo(j2.jobId);
        assertThat(service.listAll()).hasSize(2);
    }
}
