package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.SopTemplate;
import org.ruoyi.ipd.dto.SopTemplateListItem;
import org.ruoyi.ipd.dto.SopTemplateSaveReq;
import org.ruoyi.ipd.mapper.SopTemplateMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * P1-3.3 SOP 模板管理和实例版本快照（页46；BR-IPD-07）。
 * <ul>
 *   <li>版本生命周期：DRAFT --publish--> PUBLISHED --被新版本替代--> ARCHIVED；同 actionCode 至多一个
 *       PUBLISHED、至多一个 DRAFT（DB 条件 UPDATE 守卫并发）。</li>
 *   <li>AC-IPD-27：publish 只影响「此后实例化」的项目（bootstrap/instantiate 绑定当时 PUBLISHED 的
 *       sopId）；在研项目 stage_actions.sop_id 保持原值，取内容走 {@link #get}（快照语义）。</li>
 *   <li>AC-IPD-20：生物特征动作（V10/C12/D11）publish 时 content 必须含「算法公平性」与「偏见测试」。</li>
 *   <li>停用（ARCHIVED）只读不删，历史引用（sop_id）不破坏。</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class SopTemplateService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_PUBLISHED = "PUBLISHED";
    public static final String STATUS_ARCHIVED = "ARCHIVED";
    /** AC-IPD-20：生物特征 SOP 内容必备关键词。 */
    static final String KEY_ALGO_FAIRNESS = "算法公平性";
    static final String KEY_BIAS_TEST = "偏见测试";

    private final SopTemplateMapper sopTemplateMapper;
    private final AuditLogService auditLogService;

    /** 版本列表（version 倒序；不含 mediumtext 正文）。 */
    public List<SopTemplateListItem> listByAction(String actionCode) {
        requireActionCode(actionCode);
        return sopTemplateMapper.selectList(new LambdaQueryWrapper<SopTemplate>()
                .eq(SopTemplate::getActionCode, actionCode)
                .orderByDesc(SopTemplate::getVersion))
            .stream().map(SopTemplateService::toItem).toList();
    }

    /** 深管取当前生效 SOP（PUBLISHED；无则 404）。 */
    public SopTemplate currentForAction(String actionCode) {
        requireActionCode(actionCode);
        return publishedOf(actionCode);
    }

    /** 详情（含全文；在研项目按 stage_actions.sop_id 快照取历史版本也走这里）。 */
    public SopTemplate get(Long id) {
        SopTemplate t = sopTemplateMapper.selectById(id);
        if (t == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return t;
    }

    /**
     * 复制为 draft（源须 PUBLISHED/ARCHIVED）；同 actionCode 已有 DRAFT 则 409。
     * revert 历史版本同走此处，仅审计 action 不同。
     */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplate copyToDraft(Long sourceId, String operator) {
        return copyToDraft(sourceId, operator, "COPY");
    }

    /** 仅 DRAFT 可编辑；PUBLISHED/ARCHIVED 拒绝（409）。 */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplate updateDraft(Long id, SopTemplateSaveReq req, String operator) {
        SopTemplate t = requireDraft(id);
        validateSave(req);
        String before = summaryOf(t);
        int n = sopTemplateMapper.update(null, new LambdaUpdateWrapper<SopTemplate>()
            .eq(SopTemplate::getId, id).eq(SopTemplate::getStatus, STATUS_DRAFT)
            .set(SopTemplate::getTitle, req.title().trim())
            .set(SopTemplate::getContent, req.content()));
        if (n != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        SopTemplate after = sopTemplateMapper.selectById(id);
        audit(operator, "UPDATE", id, before, summaryOf(after));
        return after;
    }

    /**
     * DRAFT → PUBLISHED：AC-IPD-20 生物特征内容校验；旧 PUBLISHED 自动 ARCHIVED（历史引用不破坏）；
     * 全程条件 UPDATE 守卫并发（两个 draft 同时 publish 只有一个成功）。
     */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplate publish(Long id, String operator) {
        SopTemplate t = requireDraft(id);
        validateBioContent(t);
        String before = summaryOf(t);
        // 旧 PUBLISHED → ARCHIVED（停用不删除，既有 sop_id 引用继续可读）
        sopTemplateMapper.update(null, new LambdaUpdateWrapper<SopTemplate>()
            .eq(SopTemplate::getActionCode, t.getActionCode())
            .eq(SopTemplate::getStatus, STATUS_PUBLISHED)
            .set(SopTemplate::getStatus, STATUS_ARCHIVED));
        int n = sopTemplateMapper.update(null, new LambdaUpdateWrapper<SopTemplate>()
            .eq(SopTemplate::getId, id).eq(SopTemplate::getStatus, STATUS_DRAFT)
            .set(SopTemplate::getStatus, STATUS_PUBLISHED));
        if (n != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        SopTemplate after = sopTemplateMapper.selectById(id);
        audit(operator, "PUBLISH", id, before, summaryOf(after));
        return after;
    }

    /** 历史恢复：ARCHIVED/PUBLISHED 复制为新 DRAFT（不直接改动现行版本，走正常 publish 流程生效）。 */
    @Transactional(rollbackFor = Exception.class)
    public SopTemplate revert(Long sourceId, String operator) {
        return copyToDraft(sourceId, operator, "REVERT");
    }

    /**
     * bootstrap/instantiate 绑定用：一次查询取一批动作的当前 PUBLISHED 模板 id（无模板的动作不入 Map）。
     */
    public Map<String, Long> publishedIdByCodes(Set<String> actionCodes) {
        if (actionCodes == null || actionCodes.isEmpty()) {
            return Map.of();
        }
        return sopTemplateMapper.selectList(new LambdaQueryWrapper<SopTemplate>()
                .in(SopTemplate::getActionCode, actionCodes)
                .eq(SopTemplate::getStatus, STATUS_PUBLISHED))
            .stream().collect(Collectors.toMap(SopTemplate::getActionCode, SopTemplate::getId, (a, b) -> a));
    }

    private SopTemplate copyToDraft(Long sourceId, String operator, String auditAction) {
        SopTemplate source = get(sourceId);
        if (STATUS_DRAFT.equals(source.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        if (hasDraft(source.getActionCode())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        Integer maxVersion = sopTemplateMapper.selectList(new LambdaQueryWrapper<SopTemplate>()
                .eq(SopTemplate::getActionCode, source.getActionCode())
                .select(SopTemplate::getVersion))
            .stream().map(SopTemplate::getVersion).max(Integer::compareTo).orElse(0);
        SopTemplate draft = SopTemplate.builder()
            .actionCode(source.getActionCode())
            .title(source.getTitle())
            .content(source.getContent())
            .version(maxVersion + 1)
            .status(STATUS_DRAFT)
            .build();
        sopTemplateMapper.insert(draft);
        audit(operator, auditAction, draft.getId(), summaryOf(source), summaryOf(draft));
        return draft;
    }

    private SopTemplate publishedOf(String actionCode) {
        return sopTemplateMapper.selectList(new LambdaQueryWrapper<SopTemplate>()
                .eq(SopTemplate::getActionCode, actionCode)
                .eq(SopTemplate::getStatus, STATUS_PUBLISHED))
            .stream().findFirst()
            .orElseThrow(() -> new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND));
    }

    private boolean hasDraft(String actionCode) {
        return sopTemplateMapper.selectCount(new LambdaQueryWrapper<SopTemplate>()
            .eq(SopTemplate::getActionCode, actionCode)
            .eq(SopTemplate::getStatus, STATUS_DRAFT)) > 0;
    }

    private SopTemplate requireDraft(Long id) {
        SopTemplate t = get(id);
        if (!STATUS_DRAFT.equals(t.getStatus())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT);
        }
        return t;
    }

    /** 页46 字段校验 + AC-IPD-20：生物特征动作内容必须含算法公平性与偏见测试要求。 */
    private void validateBioContent(SopTemplate t) {
        String code = ActionCatalog.resolveCode(t.getActionCode());
        boolean bio;
        try {
            bio = ActionCatalog.byCode(code).bioFeature();
        } catch (IllegalArgumentException e) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        String content = t.getContent() == null ? "" : t.getContent();
        if (bio && (!content.contains(KEY_ALGO_FAIRNESS) || !content.contains(KEY_BIAS_TEST))) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    private static void validateSave(SopTemplateSaveReq req) {
        if (req == null || req.title() == null || req.title().trim().length() < 2
            || req.title().trim().length() > 128) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (req.content() == null || req.content().length() < 2) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    private static void requireActionCode(String actionCode) {
        if (actionCode == null || actionCode.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    /** 审计摘要：version/status/title + 正文指纹（diff 语义，不重复落 mediumtext）。 */
    private static String summaryOf(SopTemplate t) {
        return AuditEventData.json(
            "version", t.getVersion(),
            "status", t.getStatus(),
            "title", t.getTitle(),
            "contentSha", GuestDemandService.sha256Short(t.getContent()),
            "contentLen", t.getContent() == null ? 0 : t.getContent().length());
    }

    private void audit(String operator, String action, Long entityId, String before, String after) {
        auditLogService.append(AuditLog.builder()
            .operatorName(operator).operatorRole("SUPER_ADMIN")
            .action(action).entityType("SOP_TEMPLATE").entityId(entityId)
            .beforeData(before).afterData(after)
            .build());
    }

    /** 列表轻量视图。 */
    public static SopTemplateListItem toItem(SopTemplate t) {
        return new SopTemplateListItem(t.getId(), t.getActionCode(), t.getTitle(),
            t.getVersion(), t.getStatus(), t.getContent() == null ? 0 : t.getContent().length());
    }
}
