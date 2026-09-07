package org.ruoyi.ipd.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.PersonSyncService;
import org.ruoyi.ipd.service.PersonSyncService.SyncJobView;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人员同步任务管理（P2-2.3 + P2-2.1；AC-USER-11/12；BR-USER-04）。
 *
 * <p>端点：
 * <ul>
 *   <li>{@code POST /api/v1/person-sync/jobs} 提交同步任务（idempotencyKey 幂等保护）</li>
 *   <li>{@code POST /api/v1/person-sync/jobs/{id}/retry} 单任务手动重试（FAILED only）</li>
 *   <li>{@code POST /api/v1/person-sync/jobs/retry-all} 批量回补（admin 触发）</li>
 *   <li>{@code GET /api/v1/person-sync/jobs} 列出所有任务</li>
 *   <li>{@code GET /api/v1/person-sync/jobs/abnormal} 列出异常项（FAILED only）</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/person-sync")
@RequiredArgsConstructor
public class PersonSyncController {

    private final PersonSyncService personSyncService;
    private final IpdPermission permission;

    /** 提交请求（idempotencyKey 选填；同 key 重放返原 jobId）。 */
    public record SubmitRequest(@NotBlank String employeeNo, String idempotencyKey) { }

    /** 提交响应（jobId + 当前 status）。 */
    public record SubmitResponse(String jobId, String status) { }

    /** 批量重试响应。 */
    public record BatchRetryView(int retried, int succeeded, int failed, int skipped) {
        public static BatchRetryView from(PersonSyncService.BatchRetryResult r) {
            return new BatchRetryView(r.retried(), r.succeeded(), r.failed(), r.skipped());
        }
    }

    /** 提交同步任务（P2-2.1 idempotency + P2-2.3 状态）。 */
    @PostMapping("/jobs")
    public ApiV1Response<SubmitResponse> submit(@Valid @RequestBody SubmitRequest req) {
        IpdActor operator = permission.requireLeaderOrAdmin();
        var job = personSyncService.submit(req.employeeNo(), req.idempotencyKey(), operator);
        return ApiV1Response.ok(new SubmitResponse(job.jobId, job.status.name()));
    }

    /** 单任务重试（仅 FAILED 可重试；超 maxAttempts 拒绝）。 */
    @PostMapping("/jobs/{id}/retry")
    public ApiV1Response<SyncJobView> retry(@PathVariable("id") String jobId) {
        IpdActor operator = permission.requireLeaderOrAdmin();
        var job = personSyncService.retry(jobId, operator);
        return ApiV1Response.ok(PersonSyncService.toView(job));
    }

    /** 批量回补（admin 触发；P2-2.3 核心端点）。 */
    @PostMapping("/jobs/retry-all")
    public ApiV1Response<BatchRetryView> retryAll() {
        IpdActor operator = permission.requireAdmin();
        var result = personSyncService.retryAll(operator);
        return ApiV1Response.ok(BatchRetryView.from(result));
    }

    /** 列出所有任务（admin 视图）。 */
    @GetMapping("/jobs")
    public ApiV1Response<List<SyncJobView>> listAll() {
        permission.requireAdmin();
        return ApiV1Response.ok(personSyncService.listAll().stream()
            .map(PersonSyncService::toView).toList());
    }

    /** 列出异常项（FAILED only；admin 异常项视图）。 */
    @GetMapping("/jobs/abnormal")
    public ApiV1Response<List<SyncJobView>> listAbnormal() {
        permission.requireAdmin();
        return ApiV1Response.ok(personSyncService.listAbnormal().stream()
            .map(PersonSyncService::toView).toList());
    }
}
