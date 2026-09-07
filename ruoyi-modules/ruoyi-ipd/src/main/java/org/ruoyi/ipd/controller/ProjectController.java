package org.ruoyi.ipd.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectCertItem;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.dto.LegacyImportReq;
import org.ruoyi.ipd.dto.LegacyImportResult;
import org.ruoyi.ipd.dto.LegacyImportRowResult;
import org.ruoyi.ipd.dto.ProjectCertListView;
import org.ruoyi.ipd.dto.ProjectCertManualReq;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermissionCode;
import org.ruoyi.ipd.security.IpdAuthSession;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.GateEngine;
import org.ruoyi.ipd.service.LaunchDateChangeService;
import org.ruoyi.ipd.service.GateCreationService;
import org.ruoyi.ipd.domain.GateReview;
import org.ruoyi.ipd.service.LegacyImportService;
import org.ruoyi.ipd.service.ProjectCertService;
import org.ruoyi.ipd.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * 项目接口 /api/v1/projects（TS-09 统一响应 code=0）
 */
@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final GateEngine gateEngine;
    private final ProjectCertService projectCertService;
    private final LegacyImportService legacyImportService;
    private final LaunchDateChangeService launchDateChangeService;
    private final GateCreationService gateCreationService;
    private final IpdPermission ipdPermission;

    /**
     * 查询项目列表（P1-9.2：含 scenarioDaysRemaining + critical 派生字段）。
     * 需 ipd:project:list 权限
     */
    @GetMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<org.ruoyi.ipd.dto.ProjectListItemView>> list(@RequestParam(required = false) String keyword) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.listWithScenario(keyword));
    }

    /** 查询项目详情，需 ipd:project:query 权限 */
    @GetMapping("/{id}")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> get(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.getById(id));
    }

    /**
     * P1-5.2：本阶段门禁必做集可解释清单（逐项原因 + 配置版本）。
     *
     * @param id    项目 ID
     * @param stage 可选阶段；缺省用项目 currentStage
     * @return 清单视图
     */
    @GetMapping("/{id}/gate-checklist")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<GateChecklistView> gateChecklist(@PathVariable Long id,
                                                          @RequestParam(required = false) String stage) {
        ipdPermission.requireInternal();
        Project project = projectService.getById(id);
        return ApiV1Response.ok(gateEngine.explainChecklist(project, stage));
    }

    /** 创建项目，需 ipd:project:add 权限 */
    @PostMapping
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> create(@RequestBody org.ruoyi.ipd.dto.ProjectCreateReq req) {
        // CODE-01：白名单 DTO，code/currentStage/status/source 由服务端定，客户端不可注入
        IpdActor actor = ipdPermission.requireProjectCreator();
        return ApiV1Response.ok(projectService.create(req.toEntity(), actor.id()));
    }

    /** 变更项目状态，需 ipd:project:edit 权限 */
    @PostMapping("/{id}/status")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> changeStatus(@PathVariable Long id, @RequestParam String target) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.changeStatus(id, target, actor.id(), actor.groupId(), actor.role()));
    }

    /** 推进项目阶段，需 ipd:project:edit 权限 */
    @PostMapping("/{id}/advance-stage")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> advanceStage(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectService.advanceStage(id, actor.id(), actor.groupId(), actor.role()));
    }

    /**
     * P1-2.2：DRAFT 期内更新四基准；立项后锁定。
     *
     * @param id  项目
     * @param req 四基准白名单
     * @return 更新后项目
     */
    @PostMapping("/{id}/baselines")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> updateBaselines(@PathVariable Long id,
                                                  @RequestBody BaselinePatchReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        Project patch = Project.builder()
            .targetSalesAmount(req.targetSalesAmount())
            .targetChannelCount(req.targetChannelCount())
            .targetNps(req.targetNps())
            .targetSceneCount(req.targetSceneCount())
            .build();
        return ApiV1Response.ok(projectService.updateBaselines(id, patch, actor.id(), actor.groupId(), actor.role()));
    }

    /** 四基准补丁。 */
    public record BaselinePatchReq(
        java.math.BigDecimal targetSalesAmount,
        Integer targetChannelCount,
        Integer targetNps,
        Integer targetSceneCount) {
    }

    /**
     * P1-9.1：存量单条导入（超管）；历史缺失标记不阻断后续。
     *
     * @param req 白名单
     * @return 导入结果
     */
    @PostMapping("/legacy-import")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<LegacyImportResult> legacyImport(@RequestBody LegacyImportReq req) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(legacyImportService.importOne(req, actor.id()));
    }

    /**
     * P1-9.1：存量批量导入；错误行隔离，不回滚已成功行。
     *
     * @param rows 行列表
     * @return 逐行结果
     */
    @PostMapping("/legacy-import/batch")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_CREATE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<List<LegacyImportRowResult>> legacyImportBatch(@RequestBody List<LegacyImportReq> rows) {
        IpdActor actor = ipdPermission.requireAdmin();
        return ApiV1Response.ok(legacyImportService.importBatch(rows, actor.id()));
    }

    /**
     * P1 / §5.1 HIGH-1.1：L08 上市日期初次录入（独立端点）——
     * 仅 DRAFT|CONFIRMED|TEAMING|ACTIVE 状态可调；写 INITIAL_LAUNCH_DATE 审计。
     * launch_date 已存在则拒绝（走双签流程）。
     */
    @PostMapping("/{id}/launch-date")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Project> recordLaunchDate(
            @PathVariable Long id,
            @RequestBody LaunchDateRecordReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        Date date = Date.from(req.launchDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        return ApiV1Response.ok(launchDateChangeService.initialRecord(id, date, req.reason(), actor.id()));
    }

    /** L08 上市日期初次录入入参（独立于双签流程）。 */
    public record LaunchDateRecordReq(
            @NotNull @JsonFormat(pattern = "yyyy-MM-dd") LocalDate launchDate,
            @NotBlank @Size(max = 500) String reason) {
    }

    /**
     * P1 / §5.3 HIGH-1.1：G3 评审自动创建入口 —— 独立端点 POST /api/v1/projects/{id}/gates?gateCode=。
     * 每 14 天最多创建 1 次；写 GATE_AUTO_CREATE 审计。
     */
    @PostMapping("/{id}/gates")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<GateReview> autoCreateGate(
            @PathVariable Long id,
            @RequestParam String gateCode) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(gateCreationService.autoCreateGate(id, gateCode, actor.id()));
    }

    /**
     * P1-7.1：项目认证清单（含未知市场提示）。
     *
     * @param id 项目 ID
     * @return 清单视图
     */
    @GetMapping("/{id}/cert-items")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_QUERY, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProjectCertListView> listCertItems(@PathVariable Long id) {
        ipdPermission.requireInternal();
        return ApiV1Response.ok(projectCertService.listView(id));
    }

    /**
     * P1-7.1：按当前目标市场重新带出模板项（只增不重置 DONE）。
     *
     * @param id 项目
     * @return 新增条数
     */
    @PostMapping("/{id}/cert-items/sync")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<Integer> syncCertItems(@PathVariable Long id) {
        IpdActor actor = ipdPermission.requireInternal();
        Project project = projectService.getById(id);
        return ApiV1Response.ok(projectCertService.syncFromProject(project, actor.id()));
    }

    /**
     * AC-PROD-12：手工补充认证项。
     *
     * @param id  项目
     * @param req 白名单
     * @return 新建项
     */
    @PostMapping("/{id}/cert-items")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProjectCertItem> addCertItem(@PathVariable Long id,
                                                      @RequestBody ProjectCertManualReq req) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectCertService.addManual(id, req, actor.id()));
    }

    /**
     * 更新项目认证项状态（DONE 后不被 sync 重置）。
     *
     * @param id     项目
     * @param itemId 清单项
     * @param target 目标状态
     * @return 更新后项
     */
    @PostMapping("/{id}/cert-items/{itemId}/status")
    @SaCheckPermission(value = IpdPermissionCode.OPERATION_MODULE_PROJECT_STATUS_CHANGE, type = IpdAuthSession.LOGIN_TYPE)
    public ApiV1Response<ProjectCertItem> changeCertStatus(@PathVariable Long id,
                                                           @PathVariable Long itemId,
                                                           @RequestParam String target) {
        IpdActor actor = ipdPermission.requireInternal();
        return ApiV1Response.ok(projectCertService.changeStatus(id, itemId, target, actor.id()));
    }
}
