package org.ruoyi.ipd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.security.IpdActor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * PersonSyncService 六道防线（commit d3deb74a / SEC-PersonSync-07）场景级回归。
 *
 * <p>与 {@code P223PersonSyncRetryAcceptanceTest} 互补：本类以端到端场景断言
 * 副作用零重复、审计链可解析、脱敏边界、终态重入风暴与非法输入零副作用。
 *
 * <ul>
 *   <li>复合幂等键 (operatorId, groupId, idempotencyKey)：同键重放不重复插入/不重复审计</li>
 *   <li>AuditEventData.json：SUBMIT/MANUAL_RETRY/BATCH_RETRY 全链合法 JSON（DEF-6 应用层护栏）</li>
 *   <li>maskKey：16 字符边界透传、17 字符截断、null 键字面量安全</li>
 *   <li>synchronized(job)：终态任务并发重试风暴不复活、attempts 不膨胀</li>
 *   <li>processor 未配置：Person 表写入旁路彻底关闭</li>
 * </ul>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PersonSyncService 六道防线场景回归（幂等副作用/审计JSON/脱敏边界/重入风暴/旁路关闭）")
class PersonSyncSecurityScenarioTest {

    @Mock
    private PersonMapper personMapper;
    @Mock
    private AuditLogService auditLogService;

    private PersonSyncService service;

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final IpdActor ADMIN = new IpdActor(1L, "Admin", "SUPER_ADMIN", null);
    private static final IpdActor LEADER = new IpdActor(2L, "Leader", "GROUP_LEADER", 10L);

    @BeforeEach
    void setUp() {
        service = new PersonSyncService(personMapper, auditLogService);
    }

    private static PersonSyncService.SyncProcessor okProcessor() {
        return job -> { };
    }

    private static PersonSyncService.SyncProcessor failProcessor(PersonSyncService.FailureKind kind) {
        return job -> { throw new PersonSyncService.SyncFailureException(kind, kind.name() + " 注入失败"); };
    }

    private List<AuditLog> capturedAudits() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(captor.capture());
        return captor.getAllValues();
    }

    private List<AuditLog> auditsOfAction(String action) {
        return capturedAudits().stream()
            .filter(l -> action.equals(l.getAction()))
            .collect(Collectors.toList());
    }

    private static JsonNode parse(String payload) {
        try {
            return JSON.readTree(payload);
        } catch (Exception e) {
            throw new AssertionError("审计载荷不是合法 JSON: " + payload, e);
        }
    }

    @Test
    @DisplayName("同 (operator, group, key) 二次 submit：返原 jobId、processor 只跑一次、SUBMIT 审计只一条")
    void submit_sameCompositeKeyReplay_noDuplicateSideEffects() {
        AtomicInteger processorRuns = new AtomicInteger(0);
        service.setProcessor(job -> processorRuns.incrementAndGet());

        PersonSyncService.SyncJob first = service.submit("E-DUP", "dup-key-0001", LEADER);
        PersonSyncService.SyncJob second = service.submit("E-DUP", "dup-key-0001", LEADER);

        assertThat(second.jobId).as("幂等重放返回原 jobId").isEqualTo(first.jobId);
        assertThat(processorRuns.get()).as("processor 只执行一次（不重复插入人员）").isEqualTo(1);
        assertThat(service.listAll()).as("任务表不新增重复任务").hasSize(1);
        assertThat(auditsOfAction("SUBMIT")).as("SUBMIT 审计不重复落库").hasSize(1);
    }

    @Test
    @DisplayName("PERMANENT 失败到手动重试全链：SUBMIT/MANUAL_RETRY 审计逐条可解析且字段完整")
    void auditLifecycle_permanentFailToManualRetry_parsableJsonChain() throws Exception {
        service.setProcessor(failProcessor(PersonSyncService.FailureKind.PERMANENT));

        PersonSyncService.SyncJob job = service.submit("E-CHAIN", "chain-key", ADMIN);
        assertThat(job.status).isEqualTo(PersonSyncService.JobStatus.FAILED);

        service.setProcessor(okProcessor());
        service.retry(job.jobId, ADMIN);

        List<AuditLog> audits = capturedAudits();
        assertThat(audits).extracting(AuditLog::getAction)
            .as("生命周期审计动作齐全")
            .contains("SUBMIT", "MANUAL_RETRY");

        for (AuditLog audit : audits) {
            JsonNode node = parse(audit.getAfterData());
            assertThat(node.isObject()).as("载荷是 JSON 对象").isTrue();
        }
        JsonNode submitNode = parse(auditsOfAction("SUBMIT").get(0).getAfterData());
        assertThat(submitNode.path("employeeNo").asText()).isEqualTo("E-CHAIN");
        assertThat(submitNode.path("jobId").asText()).isEqualTo(job.jobId);
        JsonNode retryNode = parse(auditsOfAction("MANUAL_RETRY").get(0).getAfterData());
        assertThat(retryNode.path("jobId").asText()).as("MANUAL_RETRY 载荷可追溯到 jobId").isEqualTo(job.jobId);
    }

    @Test
    @DisplayName("maskKey 边界：16 字符明文透传、17 字符截断加省略号、null 键写字面量不抛")
    void maskKey_boundary_keepsSixteenChars_masksSeventeen_nullSafe() {
        service.setProcessor(okProcessor());

        String exactly16 = "ABCDEFGHIJKLMNOP";
        String overflow17 = "ABCDEFGHIJKLMNOPQ";
        service.submit("E-K16", exactly16, ADMIN);
        service.submit("E-K17", overflow17, ADMIN);
        service.submit("E-KNULL", null, ADMIN);

        List<AuditLog> submits = auditsOfAction("SUBMIT");
        assertThat(submits).hasSize(3);

        String payload16 = submits.stream()
            .filter(l -> l.getAfterData().contains("E-K16")).findFirst().orElseThrow().getAfterData();
        assertThat(parse(payload16).path("idempotencyKey").asText())
            .as("恰好 16 字符按语义原样透传（不误伤短键）")
            .isEqualTo(exactly16);

        String payload17 = submits.stream()
            .filter(l -> l.getAfterData().contains("E-K17")).findFirst().orElseThrow().getAfterData();
        JsonNode masked = parse(payload17);
        assertThat(masked.path("idempotencyKey").asText())
            .as("17 字符只保留前 16 字符加省略号")
            .isEqualTo(exactly16 + "...");
        assertThat(payload17)
            .as("审计载荷不得出现完整明文长键")
            .doesNotContain(overflow17);

        String payloadNull = submits.stream()
            .filter(l -> l.getAfterData().contains("E-KNULL")).findFirst().orElseThrow().getAfterData();
        assertThat(parse(payloadNull).path("idempotencyKey").asText())
            .as("null 键以字面量 null 入审计且不抛异常")
            .isEqualTo("null");
    }

    @Test
    @DisplayName("终态任务并发重试风暴（8 线程混打 retryAll 与手动 retry）：attempts 不膨胀、无 MANUAL_RETRY 新增")
    void retryStorm_onExhaustedJob_neverRevivesOrInflates() throws Exception {
        service.setProcessor(failProcessor(PersonSyncService.FailureKind.PERMANENT));
        PersonSyncService.SyncJob job = service.submit("E-STORM", "storm-key", ADMIN);
        service.retry(job.jobId, ADMIN);
        service.retry(job.jobId, ADMIN);
        assertThat(job.attempts).as("前置：任务已耗尽 3 次").isEqualTo(3);
        assertThat(job.status).isEqualTo(PersonSyncService.JobStatus.FAILED);

        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger stateConflicts = new AtomicInteger(0);
        AtomicInteger stormSkipped = new AtomicInteger(0);
        for (int i = 0; i < threads; i++) {
            boolean batch = i % 2 == 0;
            new Thread(() -> {
                try {
                    start.await();
                    if (batch) {
                        PersonSyncService.BatchRetryResult r = service.retryAll(ADMIN);
                        stormSkipped.addAndGet(r.skipped());
                    } else {
                        try {
                            service.retry(job.jobId, ADMIN);
                        } catch (IpdBusinessException e) {
                            if (e.getErrorCode() == ApiV1ErrorCode.STATE_CONFLICT) {
                                stateConflicts.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }).start();
        }
        start.countDown();
        done.await();

        assertThat(stateConflicts.get())
            .as("4 个手动 retry 全部被终态守卫拒绝").isEqualTo(threads / 2);
        assertThat(stormSkipped.get())
            .as("4 个 retryAll 全部跳过已耗尽任务").isEqualTo(threads / 2);
        assertThat(job.attempts).as("风暴后 attempts 仍为 3（synchronized(job) 与守卫双重生效）").isEqualTo(3);
        assertThat(job.status).as("终态不被并发复活").isEqualTo(PersonSyncService.JobStatus.FAILED);
        assertThat(auditsOfAction("MANUAL_RETRY"))
            .as("风暴不产生新 MANUAL_RETRY 审计（仅前置合法重试的 2 条）")
            .hasSize(2);
    }

    @Test
    @DisplayName("processor 未配置时 submit 拒绝且 Person 表零写入（defaultProcess 旁路彻底关闭）")
    void submit_withoutProcessor_blocksPersonInsertBypass() {
        PersonSyncService fresh = new PersonSyncService(personMapper, auditLogService);
        // 注意：fresh 不调用 setProcessor，模拟生产漏配 HR 适配器 bean

        assertThatThrownBy(() -> fresh.submit("E-BYPASS", "bypass-key", ADMIN))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("processor");

        verify(personMapper, never()).insert(any(Person.class));
        verify(personMapper, never()).updateById(any(Person.class));
        verify(personMapper, never()).selectOne(any());
    }

    @Test
    @DisplayName("非法输入拒绝：未知 jobId 手动重试抛 NOT_FOUND 且零审计；SUCCESS 任务重试抛 STATE_CONFLICT")
    void invalidInputs_rejectedWithZeroSideEffects() {
        service.setProcessor(okProcessor());

        assertThatThrownBy(() -> service.retry("sync-not-exist", ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        assertThatThrownBy(() -> service.get("sync-not-exist"))
            .isInstanceOf(IpdBusinessException.class)
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.NOT_FOUND);
        verify(auditLogService, never()).append(any());

        PersonSyncService.SyncJob ok = service.submit("E-OK", "ok-key", ADMIN);
        assertThat(ok.status).isEqualTo(PersonSyncService.JobStatus.SUCCESS);
        assertThatThrownBy(() -> service.retry(ok.jobId, ADMIN))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("仅 FAILED 任务可手动重试")
            .extracting(e -> ((IpdBusinessException) e).getErrorCode())
            .isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);
        // 仅成功 submit 的 1 条 SUBMIT 审计；两条拒绝路径均未追加审计
        verify(auditLogService, times(1)).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("retryAll 混合批次：统计与 BATCH_RETRY 审计 JSON 数值一致（retried=2/succeeded=1/failed=1/skipped=2）")
    void retryAll_mixedBatch_statsAgreeWithAuditPayload() throws Exception {
        service.setProcessor(okProcessor());
        service.submit("E-J1", "k1", ADMIN);                                    // SUCCESS → skip
        service.setProcessor(failProcessor(PersonSyncService.FailureKind.PERMANENT));
        PersonSyncService.SyncJob exhausted = service.submit("E-J2", "k2", ADMIN); // PERMANENT → FAILED attempts=1
        service.submit("E-J3", "k3", ADMIN);                                    // FAILED attempts=1 → 可重试
        service.submit("E-J4", "k4", ADMIN);                                    // FAILED attempts=1 → 重试仍失败
        service.retry(exhausted.jobId, ADMIN);                                  // attempts=2
        service.retry(exhausted.jobId, ADMIN);                                  // attempts=3 → skip（耗尽）

        // E-J3 重试成功；E-J4 重试仍 PERMANENT 失败
        service.setProcessor(job -> {
            if ("E-J4".equals(job.employeeNo)) {
                throw new PersonSyncService.SyncFailureException(PersonSyncService.FailureKind.PERMANENT, "J4 仍失败");
            }
        });

        PersonSyncService.BatchRetryResult result = service.retryAll(ADMIN);

        assertThat(result.retried()).isEqualTo(2);
        assertThat(result.succeeded()).isEqualTo(1);
        assertThat(result.failed()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(captor.capture());
        AuditLog batchAudit = captor.getAllValues().stream()
            .filter(l -> "BATCH_RETRY".equals(l.getAction()))
            .reduce((a, b) -> b).orElseThrow();
        JsonNode node = JSON.readTree(batchAudit.getAfterData());
        assertThat(node.path("retried").asInt()).as("审计统计与返回值一致").isEqualTo(result.retried());
        assertThat(node.path("succeeded").asInt()).isEqualTo(result.succeeded());
        assertThat(node.path("failed").asInt()).isEqualTo(result.failed());
        assertThat(node.path("skipped").asInt()).isEqualTo(result.skipped());
        assertThat(batchAudit.getOperatorId()).as("审计列 operatorId 来自会话身份").isEqualTo(ADMIN.id());
    }
}
