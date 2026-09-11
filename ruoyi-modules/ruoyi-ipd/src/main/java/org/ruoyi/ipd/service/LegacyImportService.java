package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.LegacyImportReq;
import org.ruoyi.ipd.dto.LegacyImportResult;
import org.ruoyi.ipd.dto.LegacyImportRowResult;
import org.ruoyi.ipd.domain.LegacyImport;
import org.ruoyi.ipd.mapper.LegacyImportMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadLocalRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * P1-9.1 / BR-PROD-03：存量 LEGACY 导入。
 * 已过阶段动作标 HISTORICAL_MISSING（不伪造 DONE）；门禁视同满足；审计记录缺失标记。
 *
 * Round 8 / R8-P0-5：markPastStages 批量化（updateBatchById 替换循环 updateById）
 * Round 8 / R8-P0-7：importBatch 异常收窄（catch 仅 ServiceException 与 DataAccessException；
 *                  其它 RuntimeException 向上抛，让 Controller 决定，不再吞环境错误）
 * Round 8 / R8-P0-10：importBatch 异步化（@Async + 线程池限流）
 */
@Service
@RequiredArgsConstructor
public class LegacyImportService {

    public static final String HISTORY_MISSING = "HISTORICAL_MISSING";
    public static final String CATCHUP_IN_PROGRESS = "IN_PROGRESS";

    /** Round 8 / R8-P0-10：批量导入单次最大行数（防连接池打爆） */
    public static final int MAX_BATCH_SIZE = 500;
    /** Round 8 / R8-P0-10：并发导入最大并行度 */
    public static final int IMPORT_BATCH_PARALLELISM = 8;

    private static final List<String> STAGE_ORDER = List.of(
        "CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE");

    /** R8-PERF-12：阶段别名映射改为 static final，避免每次调用都 new LinkedHashMap */
    private static final Map<String, String> STAGE_ALIASES;
    static {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("CONCEPT", "CONCEPT");
        m.put("PLAN", "PLAN");
        m.put("DEV", "DEV");
        m.put("DEVELOP", "DEV");
        m.put("VALID", "VALID");
        m.put("VERIFY", "VALID");
        m.put("LAUNCH", "LAUNCH");
        m.put("LIFECYCLE", "LIFECYCLE");
        STAGE_ALIASES = Map.copyOf(m);
    }

    private static final Logger log = LoggerFactory.getLogger(LegacyImportService.class);

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final StageActionMapper stageActionMapper;
    private final LegacyImportMapper legacyImportMapper;
    private final AuditLogService auditLogService;
    private final ObjectProvider<LegacyImportService> self;
    /** R8-P0-10：并行导入执行器。2026-09-05 修正：必须具名 mainExecutor——上下文存在多个
     * @Primary 的 Executor 子类型 bean（aiflow mainExecutor + common-core scheduledExecutorService），
     * 裸 Executor 构造注入会 NoUniqueBeanDefinitionException（依赖根 lombok.config 的 copyableAnnotations）。 */
    @Qualifier("mainExecutor")
    private final Executor executor;

    /**
     * 单条存量导入。
     *
     * @param req        白名单请求
     * @param operatorId 超管操作人
     * @return 项目 + 标记编码列表
     */
    @Transactional(rollbackFor = Exception.class)
    public LegacyImportResult importOne(LegacyImportReq req, Long operatorId) {
        validateReq(req);
        String declared = normalizeStage(req.declaredStage());
        Project draft = Project.builder()
            .name(req.name().trim())
            .productId(req.productId())
            .templateType(req.templateType())
            .targetMarkets(req.targetMarkets())
            .level(req.level())
            .levelCoefficient(req.levelCoefficient())
            .levelCoefficientReason(req.levelCoefficientReason())
            .targetSalesAmount(req.targetSalesAmount())
            .targetChannelCount(req.targetChannelCount())
            .targetNps(req.targetNps())
            .targetSceneCount(req.targetSceneCount())
            .mainGroupId(req.mainGroupId())
            .source("LEGACY")
            .build();
        Project created = projectService.create(draft, operatorId);
        List<String> marked = markPastStages(created.getId(), declared, req.alternativeEvidence());
        created.setDeclaredStage(declared);
        created.setLegacyEffectiveAt(truncateSeconds(req.legacyEffectiveAt()));
        // P1-9.2：lastActivityAt = legacyEffectiveAt（导入即起算 14 天复核窗口）
        // 后续 scanLegacyCriticalProjects 按 (today - lastActivityAt) 计算 remaining
        created.setLastActivityAt(truncateSeconds(req.legacyEffectiveAt()));
        created.setMissingHistoryAck("1");
        created.setCatchupStatus(CATCHUP_IN_PROGRESS);
        created.setCurrentStage(declared);
        projectMapper.updateById(created);
        auditLegacy(created, marked, operatorId);
        return new LegacyImportResult(projectService.getById(created.getId()), marked);
    }

    /**
     * 批量导入：逐行独立事务语义由调用方外层决定；本方法逐行捕获业务异常不中断后续行。
     * Round 8 / R8-P0-7：异常收窄——只吞 ServiceException（业务校验）与 DataAccessException（已识别数据异常），
     * 其它 RuntimeException（连接池耗尽 / DB 挂 / OOM 等环境错误）向上抛，让 Controller/全局异常处理统一记录。
     *
     * @param rows       行列表（受 Controller @Size(max=500) 约束，Service 层再做一次防御）
     * @param operatorId 操作人
     * @return 逐行结果
     */
    /**
     * Round 8 / R8-P0-10：并行导入——按行提交 CompletableFuture，受 IMPORT_BATCH_PARALLELISM 限流。
     * 业务异常（ServiceException）和数据访问异常（DataAccessException）逐行捕获不影响其他行；
     * 其它 RuntimeException（连接池耗尽 / DB 挂 / OOM）向上抛，由 Controller 统一处理。
     */
    public List<LegacyImportRowResult> importBatch(List<LegacyImportReq> rows, Long operatorId) {
        if (rows == null || rows.isEmpty()) {
            throw new ServiceException("导入行不能为空");
        }
        if (rows.size() > MAX_BATCH_SIZE) {
            throw new ServiceException("单次导入最多 " + MAX_BATCH_SIZE + " 行（实际 " + rows.size() + " 行）");
        }
        // 2026-09-08 缺口补齐：开批次 → 逐行导入 → 关批次（写 legacy_imports 落账）
        LegacyImport batch = openBatch(rows.size(), operatorId);
        LegacyImportService proxy = self.getIfAvailable() == null ? this : self.getObject();
        List<CompletableFuture<LegacyImportRowResult>> futures = new ArrayList<>(rows.size());
        for (int i = 0; i < rows.size(); i++) {
            final int idx = i;
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    LegacyImportResult r = proxy.importOne(rows.get(idx), operatorId);
                    return new LegacyImportRowResult(idx, true, r.project().getId(), null, r.markedCodes());
                } catch (ServiceException ex) {
                    return new LegacyImportRowResult(idx, false, null, ex.getMessage(), List.of());
                } catch (DataAccessException ex) {
                    log.warn("legacy import row {} data access error: {}", idx, ex.getMessage());
                    return new LegacyImportRowResult(idx, false, null, "数据冲突: " + ex.getMostSpecificCause().getMessage(), List.of());
                }
            }, executor));
        }
        List<LegacyImportRowResult> results = futures.stream()
            .map(CompletableFuture::join)
            .collect(Collectors.toList());
        closeBatch(batch, results);
        return results;
    }

    /**
     * 2026-09-08 缺口补齐：开批次记录（IN_PROGRESS 状态，batchNo 唯一）。
     */
    private LegacyImport openBatch(int totalRows, Long operatorId) {
        LegacyImport batch = new LegacyImport();
        batch.setBatchNo("LEG-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
            + "-" + ThreadLocalRandom.current().nextInt(100, 1000));
        batch.setSourceSystem("MANUAL_IMPORT");
        batch.setImportStatus("IN_PROGRESS");
        batch.setTotalCount(totalRows);
        batch.setImportedBy(operatorId);
        batch.setStartedAt(new Date());
        legacyImportMapper.insert(batch);
        return batch;
    }

    /**
     * 2026-09-08 缺口补齐：关批次（终态：全成功 SUCCESS / 全失败 FAILED / 混合 PARTIAL）。
     */
    private void closeBatch(LegacyImport batch, List<LegacyImportRowResult> results) {
        long success = results.stream().filter(LegacyImportRowResult::ok).count();
        batch.setSuccessCount((int) success);
        batch.setErrorCount(results.size() - (int) success);
        batch.setImportStatus(success == results.size() ? "SUCCESS" : success == 0 ? "FAILED" : "PARTIAL");
        batch.setCompletedAt(new Date());
        legacyImportMapper.updateById(batch);
    }

    /**
     * 将 declared 之前各阶段的动作标为历史缺失（status 保持 NOT_STARTED，不伪造 DONE）。
     * Round 8 / R8-P0-5：批量化——从「循环每条 updateById」改造为「一次性 updateBatchById(200)」。
     *
     * @param projectId 项目
     * @param declared  申报阶段
     * @param evidence  可选替代佐证
     * @return 标记的动作码
     */
    List<String> markPastStages(Long projectId, String declared, Map<String, String> evidence) {
        List<StageAction> actions = stageActionMapper.selectList(new LambdaQueryWrapper<StageAction>()
            .eq(StageAction::getProjectId, projectId));
        if (actions.isEmpty()) {
            return List.of();
        }
        Map<String, String> ev = evidence == null ? Map.of() : evidence;
        List<StageAction> toUpdate = new ArrayList<>();
        List<String> marked = new ArrayList<>();
        for (StageAction a : actions) {
            var def = ActionCatalog.byCode(a.getActionCode());
            if (def == null) {
                continue;
            }
            if (!isStageBefore(def.stage(), declared)) {
                continue;
            }
            a.setHistoryMark(HISTORY_MISSING);
            String ref = ev.get(a.getActionCode());
            if (ref != null && !ref.isBlank()) {
                a.setRemark("历史缺失替代佐证:" + ref.trim());
            } else if (a.getRemark() == null || a.getRemark().isBlank()) {
                a.setRemark("历史缺失");
            }
            toUpdate.add(a);
            marked.add(a.getActionCode());
        }
        // R8-P0-5：单条 updateById 循环 → 一次性 updateBatchById（200/批）
        if (!toUpdate.isEmpty()) {
            stageActionMapper.updateBatchById(toUpdate, 200);
        }
        return marked;
    }

    private void validateReq(LegacyImportReq req) {
        if (req == null) {
            throw new ServiceException("导入请求不能为空");
        }
        if (req.name() == null || req.name().isBlank() || req.name().trim().length() < 4) {
            throw new ServiceException("项目名称至少 4 字");
        }
        if (req.productId() == null || req.productId() <= 0) {
            throw new ServiceException("productId 必填");
        }
        if (req.legacyEffectiveAt() == null) {
            throw new ServiceException("legacyEffectiveAt 生效日必填");
        }
        if (req.declaredStage() == null || req.declaredStage().isBlank()) {
            throw new ServiceException("declaredStage 必填");
        }
        if (!Boolean.TRUE.equals(req.missingHistoryAck())) {
            throw new ServiceException("必须确认 missingHistoryAck=true（历史缺失声明）");
        }
        normalizeStage(req.declaredStage());
        Long dupName = projectMapper.selectCount(new LambdaQueryWrapper<Project>()
            .eq(Project::getName, req.name().trim()).eq(Project::getDelFlag, "0"));
        if (dupName != null && dupName > 0) {
            throw new ServiceException("项目名称已存在: " + req.name().trim());
        }
    }

    /**
     * 归一阶段别名（页09 concept/develop/verify → 权威码）。
     * Round 8 / R8-PERF-12：static final Map（STAGE_ALIASES），避免每次调用 new LinkedHashMap。
     *
     * @param raw 原始阶段
     * @return 权威阶段码
     */
    public static String normalizeStage(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ServiceException("阶段不能为空");
        }
        String s = raw.trim().toUpperCase();
        String n = STAGE_ALIASES.get(s);
        if (n == null) {
            throw new ServiceException("未知阶段: " + raw);
        }
        return n;
    }

    static boolean isStageBefore(String stage, String declared) {
        int a = STAGE_ORDER.indexOf(stage);
        int b = STAGE_ORDER.indexOf(declared);
        return a >= 0 && b >= 0 && a < b;
    }

    private static Date truncateSeconds(Date d) {
        return new Date(Math.floorDiv(d.getTime(), 1000L) * 1000L);
    }

    private void auditLegacy(Project project, List<String> marked, Long operatorId) {
        String detail = AuditEventData.json(
            "source", "LEGACY",
            "declaredStage", project.getDeclaredStage(),
            "missingHistoryAck", true,
            "markedCodes", marked,
            "catchupStatus", CATCHUP_IN_PROGRESS);
        auditLogService.append(AuditLog.builder()
            .operatorName(String.valueOf(operatorId))
            .operatorRole("SUPER_ADMIN")
            .action("PROJECT_LEGACY_IMPORT")
            .entityType("PROJECT")
            .entityId(project.getId())
            .afterData(detail)
            .build());
    }
}
