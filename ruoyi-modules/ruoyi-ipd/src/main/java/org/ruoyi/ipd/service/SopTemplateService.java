package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.SopTemplate;
import org.ruoyi.ipd.domain.SopTemplateInstance;
import org.ruoyi.ipd.mapper.SopTemplateInstanceMapper;
import org.ruoyi.ipd.mapper.SopTemplateMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdIdorGuard;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SOP 模板版本管理与实例快照（P1-3.3）
 * <p>BR-IPD-SOP-01：同 {@code templateCode} 下版本号自增，每次发布新版本时关闭旧 PUBLISHED
 * （effectiveTo=now + status=ARCHIVED），保持线性版本轨迹。
 * <p>BR-IPD-SOP-02：模板一经发布（含 DRAFT 上线）即不可物理删除；走 DeletionRequestService +
 * DeleteAuditService 审核流程（与其它 P0-6.2 表对齐）。
 * <p>BR-IPD-SOP-03：实例化时序列化当前动作目录（ActionCatalog）+ 责任矩阵 + 默认阶段截止日期
 * 到 {@link SopTemplateInstance#getSnapshotJson()}；实例与模板版本解耦——后续模板迭代不影响在跑实例。
 * <p>权限：发布/归档仅 SUPER_ADMIN；实例化仅 MARKET_PM/RD_PM/GROUP_LEADER。
 *
 * @author ruoyi-ai
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SopTemplateService {

    private final SopTemplateMapper sopTemplateMapper;
    private final SopTemplateInstanceMapper sopTemplateInstanceMapper;
    private final AuditLogService auditLogService;

    /** 私有 Jackson 实例：序列化嵌套 JSON（meta/actionList/responsibilityMatrix/phaseDeadlineMap）。 */
    private static final ObjectMapper JSON = new ObjectMapper();

    // ========== 模板版本管理（SUPER_ADMIN only） ==========

    /**
     * 发布新版本（SUPER_ADMIN only，BR-IPD-SOP-01）。
     * <p>同 templateCode 下版本号自增 1（新模板从 1 起）；
     * 旧 PUBLISHED 模板自动 ARCHIVED + effectiveTo=now。
     *
     * @param template 模板入参（templateCode/templateName/description/category 必填，id/version 忽略）
     * @param actor    操作人（必须 SUPER_ADMIN）
     * @return 新模板 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long publishTemplate(SopTemplate template, IpdActor actor) {
        IpdIdorGuard.requireSuperAdmin(actor);
        if (template == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "模板不能为空");
        }
        if (template.getTemplateCode() == null || template.getTemplateCode().isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "templateCode 不能为空");
        }
        if (template.getTemplateName() == null || template.getTemplateName().isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "templateName 不能为空");
        }
        String category = template.getCategory();
        if (category == null
            || !(SopTemplate.Category.DEEP_MGMT.equals(category)
                || SopTemplate.Category.LIGHT_MGMT.equals(category)
                || SopTemplate.Category.MIXED.equals(category))) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "category 非法（仅 DEEP_MGMT|LIGHT_MGMT|MIXED）");
        }

        Date now = new Date();
        // 关闭同 templateCode 下的旧 PUBLISHED
        List<SopTemplate> existingPublished = sopTemplateMapper.selectList(
            Wrappers.<SopTemplate>lambdaQuery()
                .eq(SopTemplate::getTemplateCode, template.getTemplateCode())
                .eq(SopTemplate::getStatus, SopTemplate.Status.PUBLISHED)
                .eq(SopTemplate::getDelFlag, "0"));
        long nextVersion = 1L;
        for (SopTemplate old : existingPublished) {
            old.setStatus(SopTemplate.Status.ARCHIVED);
            old.setEffectiveTo(now);
            sopTemplateMapper.updateById(old);
            nextVersion = Math.max(nextVersion, old.getVersion() + 1);
            auditLogService.append(AuditLog.builder()
                .operatorName(actorName(actor)).operatorRole(actor.role())
                .action("ARCHIVE").entityType("SOP_TEMPLATE").entityId(old.getId())
                .afterData(AuditEventData.json("status", "ARCHIVED", "effectiveTo", now.getTime()))
                .reason("P1-3.3 SOP 模板新版本发布，旧版本自动归档")
                .build());
        }

        // 写入新 PUBLISHED
        SopTemplate fresh = SopTemplate.builder()
            .templateCode(template.getTemplateCode())
            .templateName(template.getTemplateName())
            .description(template.getDescription())
            .version(nextVersion)
            .effectiveFrom(now)
            .effectiveTo(null)
            .status(SopTemplate.Status.PUBLISHED)
            .category(category)
            .createdBy(actor.id() == null ? null : actor.id().toString())
            .tenantId(template.getTenantId())
            .delFlag("0")
            .build();
        sopTemplateMapper.insert(fresh);
        if (fresh.getId() == null || fresh.getId() <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "SOP 模板写入失败：未生成主键");
        }
        auditLogService.append(AuditLog.builder()
            .operatorName(actorName(actor)).operatorRole(actor.role())
            .action("PUBLISH").entityType("SOP_TEMPLATE").entityId(fresh.getId())
            .afterData(AuditEventData.json(
                "version", nextVersion,
                "status", "PUBLISHED",
                "category", category))
            .reason("P1-3.3 SOP 模板新版本发布")
            .build());
        return fresh.getId();
    }

    /**
     * 列出模板（可选 category / status 过滤）。
     */
    @Transactional(readOnly = true)
    public List<SopTemplate> listTemplates(String category, String status) {
        LambdaQueryWrapper<SopTemplate> q = Wrappers.<SopTemplate>lambdaQuery()
            .eq(SopTemplate::getDelFlag, "0");
        if (category != null && !category.isBlank()) {
            q.eq(SopTemplate::getCategory, category);
        }
        if (status != null && !status.isBlank()) {
            q.eq(SopTemplate::getStatus, status);
        }
        return sopTemplateMapper.selectList(q.orderByDesc(SopTemplate::getVersion));
    }

    /**
     * 取当前生效模板（PUBLISHED + effectiveTo IS NULL，同 templateCode 下最新一条）。
     * 不存在时抛 IpdBusinessException(NOT_FOUND)。
     */
    @Transactional(readOnly = true)
    public SopTemplate getActiveTemplate(String templateCode) {
        if (templateCode == null || templateCode.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "templateCode 不能为空");
        }
        List<SopTemplate> actives = sopTemplateMapper.selectList(
            Wrappers.<SopTemplate>lambdaQuery()
                .eq(SopTemplate::getTemplateCode, templateCode)
                .eq(SopTemplate::getStatus, SopTemplate.Status.PUBLISHED)
                .isNull(SopTemplate::getEffectiveTo)
                .eq(SopTemplate::getDelFlag, "0")
                .orderByDesc(SopTemplate::getVersion)
                .last("LIMIT 1"));
        if (actives.isEmpty() || actives.get(0) == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND,
                "未找到当前生效的 SOP 模板: " + templateCode);
        }
        return actives.get(0);
    }

    /**
     * 获取模板（任意状态）。service 层内部用法；Controller 暴露时建议只暴露 PUBLISHED。
     */
    @Transactional(readOnly = true)
    public SopTemplate getById(Long id) {
        if (id == null || id <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "id 非法");
        }
        SopTemplate t = sopTemplateMapper.selectById(id);
        if (t == null || "1".equals(t.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "SOP 模板不存在: " + id);
        }
        return t;
    }

    // ========== 实例快照（MARKET_PM/RD_PM/GROUP_LEADER） ==========

    /**
     * 实例化模板（MARKET_PM/RD_PM/GROUP_LEADER，BR-IPD-SOP-03）。
     * <p>同项目同 templateId 下旧 ACTIVE 实例自动 SUPERSEDED；新实例写入 snapshotJson
     * （包含动作列表/责任矩阵/阶段截止日期，序列化当前 ActionCatalog 全集）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplateInstance instantiate(Long templateId, Long projectId, IpdActor actor) {
        IpdIdorGuard.requireAuthenticated(actor);
        if (actor.role() == null
            || !(actor.role().equals("MARKET_PM")
                || actor.role().equals("RD_PM")
                || actor.role().equals("GROUP_LEADER")
                || actor.role().equals("SUPER_ADMIN"))) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN,
                "实例化 SOP 模板仅 MARKET_PM/RD_PM/GROUP_LEADER 可操作");
        }
        if (templateId == null || templateId <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "templateId 非法");
        }
        if (projectId == null || projectId <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "projectId 非法");
        }

        SopTemplate template = getById(templateId);
        if (!SopTemplate.Status.PUBLISHED.equals(template.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "仅 PUBLISHED 模板可实例化，当前状态=" + template.getStatus());
        }

        // 关闭同项目同 templateId 旧 ACTIVE 实例
        List<SopTemplateInstance> existingActives = sopTemplateInstanceMapper.selectList(
            Wrappers.<SopTemplateInstance>lambdaQuery()
                .eq(SopTemplateInstance::getProjectId, projectId)
                .eq(SopTemplateInstance::getTemplateId, templateId)
                .eq(SopTemplateInstance::getStatus, SopTemplateInstance.Status.ACTIVE)
                .eq(SopTemplateInstance::getDelFlag, "0"));
        for (SopTemplateInstance old : existingActives) {
            supersedeInstanceInternal(old, actor);
        }

        // 计算新实例版本（同项目同 templateId 下自增 1）
        long maxVersion = 0L;
        List<SopTemplateInstance> allByTemplateAndProject = sopTemplateInstanceMapper.selectList(
            Wrappers.<SopTemplateInstance>lambdaQuery()
                .eq(SopTemplateInstance::getProjectId, projectId)
                .eq(SopTemplateInstance::getTemplateId, templateId)
                .eq(SopTemplateInstance::getDelFlag, "0"));
        for (SopTemplateInstance i : allByTemplateAndProject) {
            if (i.getInstanceVersion() != null && i.getInstanceVersion() > maxVersion) {
                maxVersion = i.getInstanceVersion();
            }
        }
        long nextInstanceVersion = maxVersion + 1;

        Date now = new Date();
        String snapshotJson = buildSnapshotJson(template);

        SopTemplateInstance fresh = SopTemplateInstance.builder()
            .templateId(templateId)
            .instanceVersion(nextInstanceVersion)
            .projectId(projectId)
            .snapshotJson(snapshotJson)
            .instantiatedAt(now)
            .instantiatedBy(actor.id() == null ? null : actor.id().toString())
            .status(SopTemplateInstance.Status.ACTIVE)
            .tenantId(template.getTenantId())
            .delFlag("0")
            .build();
        sopTemplateInstanceMapper.insert(fresh);
        if (fresh.getId() == null || fresh.getId() <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR, "SOP 实例写入失败：未生成主键");
        }
        auditLogService.append(AuditLog.builder()
            .operatorName(actorName(actor)).operatorRole(actor.role())
            .action("INSTANTIATE").entityType("SOP_TEMPLATE_INSTANCE").entityId(fresh.getId())
            .afterData(AuditEventData.json(
                "templateId", templateId,
                "projectId", projectId,
                "instanceVersion", nextInstanceVersion))
            .reason("P1-3.3 SOP 模板实例化快照")
            .build());
        return fresh;
    }

    /**
     * 按项目列出实例（@Transactional readOnly）。
     */
    @Transactional(readOnly = true)
    public List<SopTemplateInstance> listInstancesByProject(Long projectId) {
        if (projectId == null || projectId <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "projectId 非法");
        }
        return sopTemplateInstanceMapper.selectList(
            Wrappers.<SopTemplateInstance>lambdaQuery()
                .eq(SopTemplateInstance::getProjectId, projectId)
                .eq(SopTemplateInstance::getDelFlag, "0")
                .orderByDesc(SopTemplateInstance::getInstanceVersion));
    }

    /**
     * 手动标记旧实例 SUPERSEDED（SUPER_ADMIN 运维豁免）。
     * <p>正常实例化流程自动触发；本方法用于模板强制升级或运维介入。
     */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplateInstance supersedeInstance(Long instanceId, IpdActor actor) {
        IpdIdorGuard.requireSuperAdmin(actor);
        if (instanceId == null || instanceId <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "instanceId 非法");
        }
        SopTemplateInstance inst = sopTemplateInstanceMapper.selectById(instanceId);
        if (inst == null || "1".equals(inst.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "SOP 实例不存在: " + instanceId);
        }
        if (SopTemplateInstance.Status.SUPERSEDED.equals(inst.getStatus())
            || SopTemplateInstance.Status.ARCHIVED.equals(inst.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "实例已终态，不可再次 SUPERSEDED: status=" + inst.getStatus());
        }
        supersedeInstanceInternal(inst, actor);
        return sopTemplateInstanceMapper.selectById(instanceId);
    }

    /**
     * 内部：标记实例为 SUPERSEDED 并写审计（无权限校验，调用方已校验）。
     */
    private void supersedeInstanceInternal(SopTemplateInstance inst, IpdActor actor) {
        String beforeStatus = inst.getStatus();
        inst.setStatus(SopTemplateInstance.Status.SUPERSEDED);
        sopTemplateInstanceMapper.updateById(inst);
        auditLogService.append(AuditLog.builder()
            .operatorName(actorName(actor)).operatorRole(actor == null ? "SYSTEM" : actor.role())
            .action("SUPERSEDE").entityType("SOP_TEMPLATE_INSTANCE").entityId(inst.getId())
            .beforeData(AuditEventData.json("status", beforeStatus))
            .afterData(AuditEventData.json("status", "SUPERSEDED"))
            .reason("P1-3.3 SOP 实例被新版本替换")
            .build());
    }

    // ========== helpers ==========

    /**
     * 序列化快照 JSON（BR-IPD-SOP-03）：meta + actionList + responsibilityMatrix + phaseDeadlineMap。
     * <p>使用私有 ObjectMapper 序列化嵌套结构，避免手工拼接出现非法 JSON。
     * ActionCatalog 按 category 过滤：DEEP_MGMT 仅 DEEP、LIGHT_MGMT 仅 LIGHT、MIXED 全留。
     */
    private static String buildSnapshotJson(SopTemplate template) {
        // 按 category 过滤
        String filterDepth = null;
        if (SopTemplate.Category.DEEP_MGMT.equals(template.getCategory())) {
            filterDepth = "DEEP";
        } else if (SopTemplate.Category.LIGHT_MGMT.equals(template.getCategory())) {
            filterDepth = "LIGHT";
        }

        List<Map<String, String>> actionList = new ArrayList<>();
        Map<String, String> responsibilityMatrix = new LinkedHashMap<>();
        for (ActionDef def : ActionCatalog.ALL) {
            if (filterDepth != null && !filterDepth.equals(def.depth())) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("code", def.code());
            row.put("name", def.name());
            row.put("stage", def.stage());
            row.put("ownerRole", def.ownerRole());
            row.put("depth", def.depth());
            actionList.add(row);
            responsibilityMatrix.put(def.code(), def.ownerRole());
        }

        Map<String, Integer> phaseDeadlineMap = new LinkedHashMap<>();
        phaseDeadlineMap.put("CONCEPT", 30);
        phaseDeadlineMap.put("PLAN", 60);
        phaseDeadlineMap.put("DEV", 120);
        phaseDeadlineMap.put("VALID", 90);
        phaseDeadlineMap.put("LAUNCH", 30);
        phaseDeadlineMap.put("LIFECYCLE", 180);

        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("templateCode", template.getTemplateCode());
        meta.put("templateName", template.getTemplateName());
        meta.put("version", template.getVersion());
        meta.put("category", template.getCategory());
        meta.put("actionCount", actionList.size());
        meta.put("phaseDeadlineMap", phaseDeadlineMap);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("meta", meta);
        root.put("actionList", actionList);
        root.put("responsibilityMatrix", responsibilityMatrix);
        root.put("phaseDeadlineMap", phaseDeadlineMap);

        try {
            return JSON.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR,
                "SOP 快照 JSON 序列化失败: " + e.getMessage());
        }
    }

    private static String actorName(IpdActor actor) {
        if (actor == null) return "SYSTEM";
        return actor.name() == null ? (actor.id() == null ? "SYSTEM" : actor.id().toString()) : actor.name();
    }
}