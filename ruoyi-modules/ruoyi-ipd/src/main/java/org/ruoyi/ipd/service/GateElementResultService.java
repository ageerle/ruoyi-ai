package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.BusinessConfigKeys;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Gate;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.domain.GateElementResult;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.mapper.GateElementResultMapper;
import org.ruoyi.ipd.mapper.GateMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * P2-5.1 Gate 提交与逐项判定附件快照（激活 gate_element_results，QA-04-D1）。
 *
 * <ul>
 *   <li>要素展示：按 Gate 列出适用要素（含否决标记与当前判定），AC「33要素按Gate展示」</li>
 *   <li>逐项判定：PASS/CONDITIONAL/FAIL；CONDITIONAL 必填说明；FAIL 必填证据附件（AC-GATE-02）</li>
 *   <li>G1-1 量化阈值：PASS 须一手验证 ≥ gate.g1.minCustomerVerifications（默认5，可配置即时生效）
 *       或书面意向 ≥1（AC-GATE-1a/1b/1c/1d）</li>
 * <li>提交校验：全部适用要素已有判定；否决项 FAIL 阻断提交通过（AC-GATE-15/19/20）</li>
 *   <li>提交冻结：要素定义快照写入 gates.element_snapshot，后续编辑/停用不影响在 途评审</li>
 *   <li>条件遗留深闭环（P2-5.3）：CONDITIONAL 必填责任人+期限（AC-GATE-16）；关闭需 凭证且仅责任人/超管；逾期未关阻断下一 Gate 提交，且不随要素停用消除（AC-GATE-17）</li>
 * </ul>
 * 双签盲签流转归 P2-5.2；条件遗留项深闭环归 P2-5.3。
 */
@Service
@RequiredArgsConstructor
public class GateElementResultService {

    /** G1-1 客户一手验证要素（量化阈值裁决，I3 衍生参数） */
    static final String G1_CUSTOMER_ELEMENT = "G1-1";
    /** ROOT-R1 P0-7：G1-1 客户一手验证阈值（兼容旧 SystemConfig 键名） */
    static final String MIN_VERIFICATION_KEY = "gate.g1.minCustomerVerifications";

    private static final Set<String> RESULTS = Set.of("PASS", "CONDITIONAL", "FAIL");

    private final GateMapper gateMapper;
    private final GateElementMapper elementMapper;
    private final GateElementResultMapper resultMapper;
    private final SystemConfigService systemConfigService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    /** [SEC-FIX-HIGH-1.1-FOLLOWUP] 注入本地 OssFileMapper 解析 ossId → URL（IPD 模块不依赖 system 模块）。 */
    private final org.ruoyi.ipd.mapper.OssFileMapper ossFileMapper;

    /** 可注入时钟（仿 stateMachineGuard 模式；测试固定时刻消除真实时钟摇摆，生产零影响）。 */
    private java.time.Clock clock = java.time.Clock.systemDefaultZone();
    public void setClock(java.time.Clock clock) {
        this.clock = (clock == null) ? java.time.Clock.systemDefaultZone() : clock;
    }
    private Date now() { return Date.from(clock.instant()); }
    /** ROOT-R1 P0-7 字面量迁移：Gate 评审配置（G1 客户验证阈值/签署期限；B-RULE-05 配套）来源 */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private BusinessConfigService businessConfigService;

    /** 要素清单（含当前判定）：33 要素按 Gate 展示，未判定项 result=null 供前端高亮缺失。 */
    public List<Map<String, Object>> checklist(Long gateId) {
        Gate gate = requireGate(gateId);
        List<GateElement> elements = enabledElements(gate.getGateCode());
        Map<Long, GateElementResult> judged = resultMapper.selectList(new LambdaQueryWrapper<GateElementResult>()
                .eq(GateElementResult::getGateId, gateId))
            .stream().collect(Collectors.toMap(GateElementResult::getElementId, r -> r, (a, b) -> b));
        List<Map<String, Object>> view = new ArrayList<>();
        for (GateElement e : elements) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("elementId", String.valueOf(e.getId()));
            row.put("elementCode", e.getElementCode());
            row.put("elementName", e.getElementName());
            row.put("passStandard", e.getPassStandard());
            row.put("isVeto", "1".equals(e.getIsVeto()));
            row.put("sortOrder", e.getSortOrder());
            GateElementResult r = judged.get(e.getId());
            row.put("result", r == null ? null : r.getResult());
            row.put("conditionNote", r == null ? null : r.getConditionNote());
            row.put("evidenceRef", r == null ? null : r.getEvidenceRef());
            row.put("leftoverStatus", r == null ? null : r.getLeftoverStatus());
            row.put("responsiblePersonId", r == null || r.getResponsiblePersonId() == null ? null : String.valueOf(r.getResponsiblePersonId()));
            row.put("leftoverDueAt", r == null || r.getLeftoverDueAt() == null ? null : r.getLeftoverDueAt().toString());
            row.put("closedEvidence", r == null ? null : r.getClosedEvidence());
            view.add(row);
        }
        return view;
    }

    /** 逐项判定写入（同要素重复提交 = 改判更新，审计留痕）。
     * <p>AC-GATE-16：CONDITIONAL 必填说明 + 责任人 + 关闭期限（缺一拒绝，规格 40002 口径）。 */
    @Transactional(rollbackFor = Exception.class)
    public GateElementResult judge(Long gateId, Long elementId, String result, String conditionNote,
                                   String evidenceRef, Integer verifications, Integer writtenIntents,
                                   Long responsiblePersonId, Date closeDeadline,
                                   IpdActor operator) {
        Gate gate = requireGate(gateId);
        if (!"PENDING".equals(gate.getStatus())) {
            throw new ServiceException("Gate 已终态，不可再判定: " + gate.getStatus());
        }
        // 提交后快照已冻结（P2-5.2 复核建议补）：等待签署期间改判会与 element_snapshot 漂移
        if (gate.getStartedAt() != null) {
            throw new ServiceException("评审已提交并冻结快照，签署期间不可改判（重新发起归 P2-5.4）");
        }
        GateElement element = elementMapper.selectById(elementId);
        if (element == null || "0".equals(element.getEnabled())) {
            // enabled 为空视为启用（种子数据兼容）；显式停用要素不可判定
            throw new ServiceException("要素不存在或已停用: " + elementId);
        }
        if (!gate.getGateCode().equals(element.getGateCode())) {
            throw new ServiceException("要素不属于该 Gate: " + element.getElementCode());
        }
        if (!RESULTS.contains(result)) {
            throw new ServiceException("判定非法: " + result);
        }
        boolean veto = "1".equals(element.getIsVeto());
        if ("CONDITIONAL".equals(result)) {
            if (isBlank(conditionNote)) {
                throw new ServiceException("带条件通过必须填写说明: " + element.getElementCode());
            }
            // AC-GATE-16：责任人与关闭期限缺一不可（40002）
            if (responsiblePersonId == null) {
                throw new ServiceException("带条件通过必须指定责任人: " + element.getElementCode());
            }
            if (closeDeadline == null) {
                throw new ServiceException("带条件通过必须填写关闭期限: " + element.getElementCode());
            }
        }
        if ("FAIL".equals(result) && isBlank(evidenceRef)) {
            throw new ServiceException("FAIL 判定必须附证据/附件: " + element.getElementCode());
        }
        if ("PASS".equals(result) && G1_CUSTOMER_ELEMENT.equals(element.getElementCode())) {
            verifyCustomerEvidence(element, verifications, writtenIntents);
        }
        GateElementResult row = resultMapper.selectOne(new LambdaQueryWrapper<GateElementResult>()
            .eq(GateElementResult::getGateId, gateId)
            .eq(GateElementResult::getElementId, elementId)
            .last("limit 1"));
        boolean conditional = "CONDITIONAL".equals(result);
        if (row == null) {
            row = GateElementResult.builder()
                .gateId(gateId).elementId(elementId).result(result)
                .conditionNote(conditionNote).evidenceRef(evidenceRef)
                .leftoverItem(conditional ? conditionNote : null)
                .responsiblePersonId(conditional ? responsiblePersonId : null)
                .leftoverDueAt(conditional ? closeDeadline : null)
                .leftoverStatus(conditional ? "OPEN" : null)
                .build();
            resultMapper.insert(row);
        } else {
            // 改判统一显式 set（含 CONDITIONAL→PASS 清空遗留四件套）：MP updateById 默认忽略
            // null 字段，清列必须 LambdaUpdateWrapper（真库 unbind 同坑实测）
            resultMapper.update(null, new LambdaUpdateWrapper<GateElementResult>()
                .eq(GateElementResult::getId, row.getId())
                .set(GateElementResult::getResult, result)
                .set(GateElementResult::getConditionNote, conditionNote)
                .set(GateElementResult::getEvidenceRef, evidenceRef)
                .set(GateElementResult::getLeftoverItem, conditional ? conditionNote : null)
                .set(GateElementResult::getResponsiblePersonId, conditional ? responsiblePersonId : null)
                .set(GateElementResult::getLeftoverDueAt, conditional ? closeDeadline : null)
                .set(GateElementResult::getLeftoverStatus, conditional ? "OPEN" : null)
                .set(GateElementResult::getClosedEvidence, null));
            row.setResult(result);
            row.setConditionNote(conditionNote);
            row.setEvidenceRef(evidenceRef);
            row.setLeftoverItem(conditional ? conditionNote : null);
            row.setResponsiblePersonId(conditional ? responsiblePersonId : null);
            row.setLeftoverDueAt(conditional ? closeDeadline : null);
            row.setLeftoverStatus(conditional ? "OPEN" : null);
            row.setClosedEvidence(null);
        }
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id())
            .operatorName(operator.name())
            .action("GATE_ELEMENT_JUDGE")
            .entityType("gate_element_results")
            .entityId(row.getId())
            .reason(element.getElementCode() + "=" + result)
            .afterData(AuditEventData.json(
                "gateId", gateId, "elementCode", element.getElementCode(),
                "result", result, "evidenceRef", evidenceRef == null ? "" : evidenceRef))
            .createTime(now())
            .build());
        return row;
    }

    /** AC-GATE-1a~1d：G1-1 通过阈值由服务端按可配置参数裁决（≥N 家一手验证 或 ≥1 家书面意向）。
     * <p>1d：参数改为 3 后 3 家即可通过——阈值即时生效，非硬编码。 */
    private void verifyCustomerEvidence(GateElement element, Integer verifications, Integer writtenIntents) {
        int required = resolveMinCustomerVerifications();
        int actual = verifications == null ? 0 : verifications;
        int intents = writtenIntents == null ? 0 : writtenIntents;
        if (actual < required && intents < 1) {
            throw new ServiceException(String.format(
                "需 ≥%d 家一手验证，或 ≥1 家书面意向（当前 %d 家/%d 份）", required, actual, intents));
        }
    }

    /**
     * ROOT-R1 P0-7：读取 G1-1 客户一手验证阈值。优先 BusinessConfigService，回退 SystemConfigService。
     */
    private int resolveMinCustomerVerifications() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getInt(MIN_VERIFICATION_KEY);
            } catch (Exception ex) {
                // fall through
            }
        }
        return systemConfigService.getIntValue(MIN_VERIFICATION_KEY, 5);
    }

    /**
     * 提交 Gate 评审：[SEC-FIX-HIGH-1.1] 全要素已判 + 否决项未 FAIL +
     * 强制输出物（评审材料 + 会议纪要）+ 冻结要素定义快照。
     */
    @Transactional(rollbackFor = Exception.class)
    public Gate submit(Long gateId, Long materialsOssId, Long meetingMinutesOssId, IpdActor operator) {
        Gate gate = requireGate(gateId);
        if (!"PENDING".equals(gate.getStatus())) {
            throw new ServiceException("Gate 已终态，不可重复提交: " + gate.getStatus());
        }
        // [SEC-FIX-HIGH-1.1-FOLLOWUP] ossId 守卫 + 解析为 URL（防 open-redirect/SSRF）
        String materialsUrl = resolveOssUrl(materialsOssId, "评审材料");
        String meetingMinutesUrl = resolveOssUrl(meetingMinutesOssId, "会议纪要");
        if (materialsUrl == null || meetingMinutesUrl == null) {
            throw new ServiceException("OSS 文件不存在或无权访问（[SEC-FIX-HIGH-1.1-FOLLOWUP]）");
        }
        if (gate.getStartedAt() != null) {
            throw new ServiceException("已提交，等待签署（双签流转归 P2-5.2）");
        }
        List<GateElement> elements = enabledElements(gate.getGateCode());
        Map<Long, GateElementResult> judged = resultMapper.selectList(new LambdaQueryWrapper<GateElementResult>()
                .eq(GateElementResult::getGateId, gateId))
            .stream().collect(Collectors.toMap(GateElementResult::getElementId, r -> r, (a, b) -> b));
        List<String> missing = new ArrayList<>();
        List<String> vetoFails = new ArrayList<>();
        List<Map<String, Object>> snapshot = new ArrayList<>();
        for (GateElement e : elements) {
            GateElementResult r = judged.get(e.getId());
            if (r == null) {
                missing.add(e.getElementCode());
                continue;
            }
            if ("FAIL".equals(r.getResult())) {
                if (isBlank(r.getEvidenceRef())) {
                    vetoFails.add(e.getElementCode() + "(缺证据)");
                } else if ("1".equals(e.getIsVeto())) {
                    vetoFails.add(e.getElementCode());
                }
            }
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("elementCode", e.getElementCode());
            item.put("elementName", e.getElementName());
            item.put("passStandard", e.getPassStandard());
            item.put("isVeto", e.getIsVeto());
            item.put("result", r.getResult());
            item.put("conditionNote", r.getConditionNote());
            item.put("evidenceRef", r.getEvidenceRef());
            snapshot.add(item);
        }
        if (!missing.isEmpty()) {
            throw new ServiceException("以下适用要素尚未判定: " + String.join(", ", missing));
        }
        if (!vetoFails.isEmpty()) {
            throw new ServiceException("命中否决项无法提交通过: " + String.join(", ", vetoFails));
        }
        // AC-GATE-17：前序 Gate 逾期未关闭遗留 → 阻断进入下一个 Gate
        requireNoOverdueLegacy(gate);
        gate.setMaterialsUrl(materialsUrl);
        gate.setMeetingMinutesUrl(meetingMinutesUrl);
        gate.setStartedAt(now());
        // P2-5.4：签署期限与 startedAt 同步起算（BR-GATE-04；延期/弃权扫描的锚点）
        int signDays = resolveSignDeadlineDays();
        gate.setSignDueAt(new Date(gate.getStartedAt().getTime() + 24L * 60 * 60 * 1000 * signDays));
        gate.setElementSnapshot(AuditEventData.json("frozenAt", now().toString(), "elements", snapshot));
        gateMapper.updateById(gate);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id())
            .operatorName(operator.name())
            .action("GATE_SUBMIT")
            .entityType("gates")
            .entityId(gate.getId())
            .reason("gateCode=" + gate.getGateCode() + " elements=" + snapshot.size()
                + " materialsOssId=" + materialsOssId + " meetingMinutesOssId=" + meetingMinutesOssId
                + " materialsUrlLen=" + materialsUrl.length() + " meetingMinutesUrlLen=" + meetingMinutesUrl.length())
            .afterData(AuditEventData.json(
                "gateCode", gate.getGateCode(),
                "elements", snapshot.size(),
                "materialsUrl", materialsUrl,
                "meetingMinutesUrl", meetingMinutesUrl,
                "materialsUrlLen", materialsUrl.length(),
                "meetingMinutesUrlLen", meetingMinutesUrl.length()))
            .createTime(now())
            .build());
        return gate;
    }

    /**
     * [SEC-FIX-HIGH-1.1-FOLLOWUP] 解析 ossId 为 URL——杜绝任意外部 URL（防 open-redirect/SSRF）。
     * 入参校验：ossId > 0；OSS 文件不存在/无权访问返回 null 让上层抛错。
     */
    private String resolveOssUrl(Long ossId, String label) {
        if (ossId == null || ossId <= 0) {
            throw new ServiceException(label + " ossId 非法（[SEC-FIX-HIGH-1.1-FOLLOWUP]）：" + ossId);
        }
        org.ruoyi.ipd.domain.OssFileEntity oss = ossFileMapper.selectById(ossId);
        if (oss == null || oss.getUrl() == null || oss.getUrl().isBlank()) {
            return null;
        }
        String url = oss.getUrl();
        if (url.length() > 500) {
            throw new ServiceException(label + " URL 长度超 500（[SEC-FIX-HIGH-1.1-FOLLOWUP]）");
        }
        return url;
    }

    /** 遗留清单（含已关与未关）：遗留查询仅依赖本表，要素停用/删除不消除遗留（AC-GATE-17 防线）。 */
    public List<Map<String, Object>> legacyList(Long gateId) {
        Gate gate = requireGate(gateId);
        List<GateElementResult> rows = resultMapper.selectList(new LambdaQueryWrapper<GateElementResult>()
            .eq(GateElementResult::getGateId, gateId)
            // 不能用 .or() 拼状态：or 会拆掉 gate_id 过滤（and 优先级）导致跨 Gate 泄漏
            .in(GateElementResult::getLeftoverStatus, "OPEN", "CLOSED")
            .orderByAsc(GateElementResult::getId));
        List<Map<String, Object>> view = new ArrayList<>();
        for (GateElementResult r : rows) {
            GateElement element = elementMapper.selectById(r.getElementId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("resultId", String.valueOf(r.getId()));
            // 要素已停用时 code/name 退化为本表留存的遗留描述，阻断与提醒不受影响
            item.put("elementCode", element == null ? "(已删要素)" : element.getElementCode());
            item.put("elementName", element == null ? r.getLeftoverItem() : element.getElementName());
            item.put("result", r.getResult());
            item.put("leftoverItem", r.getLeftoverItem());
            item.put("responsiblePersonId", r.getResponsiblePersonId() == null ? null : String.valueOf(r.getResponsiblePersonId()));
            item.put("leftoverDueAt", r.getLeftoverDueAt() == null ? null : r.getLeftoverDueAt().toString());
            item.put("leftoverStatus", r.getLeftoverStatus());
            item.put("closedEvidence", r.getClosedEvidence());
            item.put("overdue", "OPEN".equals(r.getLeftoverStatus()) && r.getLeftoverDueAt() != null
                && r.getLeftoverDueAt().before(now()));
            view.add(item);
        }
        return view;
    }

    /** 关闭遗留项：仅责任人本人或超管；证据必填（AC「关闭需证据」）。 */
    @Transactional(rollbackFor = Exception.class)
    public GateElementResult close(Long gateId, Long resultId, String evidence, IpdActor operator) {
        Gate gate = requireGate(gateId);
        GateElementResult row = resultMapper.selectById(resultId);
        if (row == null || !gateId.equals(row.getGateId())) {
            throw new ServiceException("遗留项不存在: " + resultId);
        }
        if (!"OPEN".equals(row.getLeftoverStatus())) {
            throw new ServiceException("遗留项不在开放状态，无需关闭: " + row.getLeftoverStatus());
        }
        if (isBlank(evidence)) {
            throw new ServiceException("关闭遗留项必须附凭证/证据");
        }
        boolean responsible = operator.id() != null && operator.id().equals(row.getResponsiblePersonId());
        if (!responsible && !"SUPER_ADMIN".equals(operator.role())) {
            throw new ServiceException("仅遗留责任人或超管可关闭遗留项");
        }
        resultMapper.update(null, new LambdaUpdateWrapper<GateElementResult>()
            .eq(GateElementResult::getId, resultId)
            .set(GateElementResult::getLeftoverStatus, "CLOSED")
            .set(GateElementResult::getClosedEvidence, evidence));
        row.setLeftoverStatus("CLOSED");
        row.setClosedEvidence(evidence);
        auditLogService.append(AuditLog.builder()
            .operatorId(operator.id())
            .operatorName(operator.name())
            .action("LEGACY_CLOSE")
            .entityType("gate_element_results")
            .entityId(resultId)
            .reason("gateCode=" + gate.getGateCode())
            .afterData(AuditEventData.json(
                "gateId", gateId, "resultId", resultId, "evidence", evidence))
            .createTime(now())
            .build());
        return row;
    }

    /** 逾期扫描（AC-GATE-17 前半）：OPEN 且期限已过 → 通知责任人（publishDaily 每日去重）。
     * <p>ops cron（OPS 卡）与超管手工触发共用本方法；返回本次命中的遗留项数。 */
    @Transactional(rollbackFor = Exception.class)
    public int scanOverdue(IpdActor operator) {
        List<GateElementResult> rows = resultMapper.selectList(new LambdaQueryWrapper<GateElementResult>()
            .eq(GateElementResult::getLeftoverStatus, "OPEN")
            .isNotNull(GateElementResult::getLeftoverDueAt)
            .lt(GateElementResult::getLeftoverDueAt, now()));
        int notified = 0;
        for (GateElementResult r : rows) {
            if (r.getResponsiblePersonId() == null) {
                continue;
            }
            Gate gate = gateMapper.selectById(r.getGateId());
            String gateCode = gate == null ? "?" : gate.getGateCode();
            notificationService.publishDaily(r.getResponsiblePersonId(),
                NotificationService.Types.GATE_CONDITION_OVERDUE, NotificationService.KIND_ACTION,
                "gate_element_results", r.getId(),
                "条件遗留项已逾期",
                String.format("%s 存在逾期未关闭的条件遗留项：%s（期限 %s），关闭前不可进入下一个 Gate",
                    gateCode, r.getLeftoverItem(), r.getLeftoverDueAt()),
                "/reviews/gate/" + r.getGateId(), now());
            notified++;
        }
        if (!rows.isEmpty()) {
            auditLogService.append(AuditLog.builder()
                .operatorId(operator.id())
                .operatorName(operator.name())
                .action("LEGACY_SCAN_OVERDUE")
                .entityType("gate_element_results")
                .entityId(rows.get(0).getId())
                .reason("overdue=" + rows.size() + " notified=" + notified)
                .createTime(now())
                .build());
        }
        return rows.size();
    }

    /** AC-GATE-17 后半：同项目前序 Gate 存在逾期未关闭遗留 → 阻断本 Gate 提交。
     * <p>遗留查询不 join 要素表（停用/删要素不消除阻断）。 */
    private void requireNoOverdueLegacy(Gate gate) {
        List<Long> priorGateIds = gateMapper.selectList(new LambdaQueryWrapper<Gate>()
                .eq(Gate::getProjectId, gate.getProjectId())
                .lt(Gate::getGateCode, gate.getGateCode()))
            .stream().map(Gate::getId).collect(Collectors.toList());
        if (priorGateIds.isEmpty()) {
            return;
        }
        List<GateElementResult> overdue = resultMapper.selectList(new LambdaQueryWrapper<GateElementResult>()
            .in(GateElementResult::getGateId, priorGateIds)
            .eq(GateElementResult::getLeftoverStatus, "OPEN")
            .isNotNull(GateElementResult::getLeftoverDueAt)
            .lt(GateElementResult::getLeftoverDueAt, now()));
        if (!overdue.isEmpty()) {
            throw new ServiceException(String.format(
                "前序 Gate 存在 %d 项逾期未关闭的条件遗留（关闭后才能提交本 Gate）: 首项遗留=%s", overdue.size(), overdue.get(0).getLeftoverItem()));
        }
    }

    private Gate requireGate(Long gateId) {
        Gate gate = gateMapper.selectById(gateId);
        if (gate == null) {
            throw new ServiceException("Gate 不存在: " + gateId);
        }
        return gate;
    }

    /**
     * ROOT-R1 P0-7：读取 Gate 签署期限天数。优先 BusinessConfigService.GATE_SIGN_DEADLINE_DAYS，回退 SystemConfig。
     */
    private int resolveSignDeadlineDays() {
        if (businessConfigService != null) {
            try {
                return businessConfigService.getInt(BusinessConfigKeys.GATE_SIGN_DEADLINE_DAYS);
            } catch (Exception ex) {
                // fall through
            }
        }
        return systemConfigService.getIntValue(
            org.ruoyi.ipd.service.GateReviewService.SIGN_DEADLINE_KEY, 3);
    }

    private List<GateElement> enabledElements(String gateCode) {
        return elementMapper.selectList(new LambdaQueryWrapper<GateElement>()
                .eq(GateElement::getGateCode, gateCode)
                .orderByAsc(GateElement::getSortOrder))
            .stream()
            .filter(e -> !"0".equals(e.getEnabled()))
            .collect(Collectors.toList());
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }
}
