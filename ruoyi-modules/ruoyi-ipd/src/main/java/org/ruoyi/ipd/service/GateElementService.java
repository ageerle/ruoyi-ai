package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.AuditLogMapper;
import org.ruoyi.ipd.mapper.GateElementMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * Gate 评审要素管理（P1-6 补口 + P1-6.1 版本生命周期）。
 *
 * <p>生命周期（页47 draft/publish/archive）：新建即 {@code draft}（enabled 强制 '0'，
 * 对业务/列表不可见）→ publish 转正（enabled 置 '1'、version 递增）→ archive 归档
 * （终态，enabled 置 '0'，仅可 copy 复活）。已发布（及归档）要素的定义编辑一律
 * {@link ApiV1ErrorCode#STATE_CONFLICT}（409）——改定义请 copy 出新草稿再发布；
 * enabled 启停是管理操作不受 409 限制（停用列表可管理，G-02 禁删仅停用）。
 *
 * <p>校验（本卡纳入）：gate/否决/启用/双否决枚举、编码唯一（服务层预检 + DB
 * {@code uk_gate_element_code} 唯一索引兜底并发）、名称/标准长度、thresholdJson
 * 必须是键非空、值均为整数的 JSON 对象。审计：CREATE/UPDATE/PUBLISH/ARCHIVE/
 * COPY/DISABLE/REVERT 全部携带字段级 before/after 快照（DEF-1/DEF-6 JSON 契约），
 * revert 即从该快照恢复。
 */
@Service
@RequiredArgsConstructor
public class GateElementService {

    private static final Set<String> GATES = Set.of("G1", "G2", "G3", "G4", "G5");
    private static final Set<String> FLAGS = Set.of("0", "1");

    /** 生命周期三态（页47） */
    public static final String STATUS_DRAFT = "draft";
    public static final String STATUS_PUBLISHED = "published";
    public static final String STATUS_ARCHIVED = "archived";

    /** gate_review_elements.element_code varchar(16) */
    private static final int CODE_MAX = 16;
    /** gate_review_elements.element_name varchar(128) */
    private static final int NAME_MAX = 128;
    /** pass_standard text：utf8mb4 下最大 21845 字符 */
    private static final int STANDARD_MAX = 21845;
    /** threshold_json varchar(512) */
    private static final int THRESHOLD_MAX = 512;

    private static final ObjectMapper JSON = new ObjectMapper();

    private final GateElementMapper gateElementMapper;
    private final AuditLogService auditLogService;
    private final AuditLogMapper auditLogMapper;

    /** 业务/超管列表：仅启用要素；草稿与归档天然不可见（enabled='0'）。 */
    public List<GateElement> listByGate(String gateCode) {
        if (gateCode != null && !gateCode.isBlank() && !GATES.contains(gateCode)) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        return gateElementMapper.selectList(new LambdaQueryWrapper<GateElement>()
            .eq(gateCode != null && !gateCode.isBlank(), GateElement::getGateCode, gateCode)
            .eq(GateElement::getEnabled, "1")
            .orderByAsc(GateElement::getSortOrder)
            .orderByAsc(GateElement::getId));
    }

    /**
     * 新建要素：一律落为草稿（status=draft/version=0/enabled='0'），publish 后才对业务可见。
     * 请求里的 enabled 仅作旧客户端兼容字段，不采纳。
     *
     * <p>SEC-REV-GATE-ELEMENT-01：审计字段绑定 actor —— createBy/updateBy 取 actor.id()，
     * createTime/updateTime 清空交由 MyBatis-Plus MetaObjectHandler 填充服务端权威时间。
     */
    public GateElement create(GateElement e, IpdActor actor) {
        if (e == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        validateDefinition(e);
        requireUniqueCode(e.getElementCode());
        e.setElementCode(e.getElementCode().trim());
        e.setElementName(e.getElementName().trim());
        if (e.getIsVeto() == null) {
            e.setIsVeto("0");
        }
        if (e.getVetoDualRequired() == null) {
            e.setVetoDualRequired("0");
        }
        if (e.getThresholdJson() != null && e.getThresholdJson().isBlank()) {
            e.setThresholdJson(null);
        }
        if (e.getSortOrder() == null) {
            e.setSortOrder(0);
        }
        e.setEnabled("0");
        e.setStatus(STATUS_DRAFT);
        e.setVersion(0);
        // Bug#7 中危：审计字段绑 actor —— 防止客户端透传 createBy/updateBy 伪造身份
        e.setCreateBy(actor.id());
        e.setUpdateBy(actor.id());
        e.setCreateDept(null);
        e.setCreateTime(null);
        e.setUpdateTime(null);
        if (gateElementMapper.insert(e) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "CREATE", e, null,
            e.getGateCode() + "/" + e.getElementCode() + " 新建草稿");
        return e;
    }

    /**
     * 编辑定义（白名单合并）。仅草稿可改定义；已发布/归档携定义字段即 409；
     * 仅 enabled 启停的补丁放行已发布行（停用/恢复管理路径）。
     */
    public GateElement update(GateElement patch, IpdActor actor) {
        if (patch == null || patch.getId() == null || patch.getId() <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        GateElement exist = gateElementMapper.selectById(patch.getId());
        if (exist == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (patch.getElementCode() != null && !patch.getElementCode().equals(exist.getElementCode())) {
            throw conflict("要素编码不可修改: " + exist.getElementCode());
        }
        if (patch.getGateCode() != null && !patch.getGateCode().equals(exist.getGateCode())) {
            throw conflict("所属 Gate 不可修改: " + exist.getGateCode());
        }
        boolean hasDefinitionChange = patch.getElementName() != null || patch.getPassStandard() != null
            || patch.getIsVeto() != null || patch.getSortOrder() != null
            || patch.getVetoDualRequired() != null || patch.getThresholdJson() != null;
        if (STATUS_ARCHIVED.equals(exist.getStatus())) {
            throw conflict("已归档要素不可编辑，请复制新草稿: " + exist.getElementCode());
        }
        if (hasDefinitionChange && STATUS_PUBLISHED.equals(exist.getStatus())) {
            throw conflict("已发布要素不可编辑(409)，请复制新草稿再发布: " + exist.getElementCode());
        }
        if (patch.getEnabled() != null && STATUS_DRAFT.equals(exist.getStatus())) {
            throw conflict("草稿尚未发布，启停无意义，请走 publish: " + exist.getElementCode());
        }
        GateElement before = snapshotCopy(exist);
        // 在副本上合并+校验：校验失败时不得污染读出的原定义（fail 前 zero-mutation）
        GateElement merged = snapshotCopy(exist);
        if (patch.getElementName() != null) {
            merged.setElementName(patch.getElementName());
        }
        if (patch.getPassStandard() != null) {
            merged.setPassStandard(patch.getPassStandard());
        }
        if (patch.getIsVeto() != null) {
            merged.setIsVeto(patch.getIsVeto());
        }
        if (patch.getSortOrder() != null) {
            merged.setSortOrder(patch.getSortOrder());
        }
        if (patch.getVetoDualRequired() != null) {
            merged.setVetoDualRequired(patch.getVetoDualRequired());
        }
        if (patch.getThresholdJson() != null) {
            merged.setThresholdJson(patch.getThresholdJson().isBlank() ? null : patch.getThresholdJson());
        }
        if (patch.getEnabled() != null) {
            merged.setEnabled(patch.getEnabled());
        }
        validateDefinition(merged);
        // Bug#7 中危：审计字段绑 actor —— updateBy 取 actor.id()，updateTime 清空交由 MetaObjectHandler 填服务端权威时间
        merged.setUpdateBy(actor.id());
        merged.setUpdateTime(null);
        if (gateElementMapper.updateById(merged) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "UPDATE", merged, before,
            merged.getGateCode() + "/" + merged.getElementCode() + " 编辑");
        return merged;
    }

    /** 停用（禁删：在途 gate_element_results 引用，G-02 证据链）。发布/草稿态均可停用。 */
    public GateElement disable(Long id, IpdActor actor) {
        requireId(id);
        GateElement exist = requireExisting(id);
        if (STATUS_ARCHIVED.equals(exist.getStatus())) {
            throw conflict("已归档要素无需停用: " + exist.getElementCode());
        }
        GateElement before = snapshotCopy(exist);
        exist.setEnabled("0");
        // Bug#7 中危：审计字段绑 actor
        exist.setUpdateBy(actor.id());
        exist.setUpdateTime(null);
        if (gateElementMapper.updateById(exist) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "DISABLE", exist, before,
            exist.getElementCode() + " disabled");
        return exist;
    }

    /** 发布：draft → published，enabled 置 '1'，发布版本递增。 */
    public GateElement publish(Long id, IpdActor actor) {
        requireId(id);
        GateElement exist = requireExisting(id);
        if (!STATUS_DRAFT.equals(exist.getStatus())) {
            throw conflict("仅草稿可发布，当前状态: " + exist.getStatus());
        }
        GateElement before = snapshotCopy(exist);
        exist.setStatus(STATUS_PUBLISHED);
        exist.setEnabled("1");
        exist.setVersion(exist.getVersion() == null ? 1 : exist.getVersion() + 1);
        // Bug#7 中危：审计字段绑 actor
        exist.setUpdateBy(actor.id());
        exist.setUpdateTime(null);
        if (gateElementMapper.updateById(exist) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "PUBLISH", exist, before,
            exist.getElementCode() + " publish v" + exist.getVersion());
        return exist;
    }

    /** 归档（终态）：draft/published → archived，enabled 置 '0'；复活请用 copy。 */
    public GateElement archive(Long id, IpdActor actor) {
        requireId(id);
        GateElement exist = requireExisting(id);
        if (STATUS_ARCHIVED.equals(exist.getStatus())) {
            throw conflict("已是归档态: " + exist.getElementCode());
        }
        GateElement before = snapshotCopy(exist);
        exist.setStatus(STATUS_ARCHIVED);
        exist.setEnabled("0");
        // Bug#7 中危：审计字段绑 actor
        exist.setUpdateBy(actor.id());
        exist.setUpdateTime(null);
        if (gateElementMapper.updateById(exist) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "ARCHIVE", exist, before,
            exist.getElementCode() + " archive");
        return exist;
    }

    /**
     * 复制：以任一状态要素为蓝本克隆出新草稿（新编码必填且唯一），定义字段全量拷贝，
     * status=draft/version=0/enabled='0'。归档要素借此复活修改后重新发布。
     */
    public GateElement copy(Long id, String newElementCode, IpdActor actor) {
        requireId(id);
        GateElement source = requireExisting(id);
        if (newElementCode == null || newElementCode.isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        GateElement clone = GateElement.builder()
            .gateCode(source.getGateCode())
            .elementCode(newElementCode.trim())
            .elementName(source.getElementName())
            .passStandard(source.getPassStandard())
            .isVeto(source.getIsVeto() == null ? "0" : source.getIsVeto())
            .sortOrder(source.getSortOrder() == null ? 0 : source.getSortOrder())
            .vetoDualRequired(source.getVetoDualRequired() == null ? "0" : source.getVetoDualRequired())
            .thresholdJson(source.getThresholdJson())
            .build();
        validateDefinition(clone);
        requireUniqueCode(clone.getElementCode());
        clone.setEnabled("0");
        clone.setStatus(STATUS_DRAFT);
        clone.setVersion(0);
        // Bug#7 中危：审计字段绑 actor —— 新行 createBy/updateBy 取 actor.id()
        clone.setCreateBy(actor.id());
        clone.setUpdateBy(actor.id());
        clone.setCreateDept(null);
        clone.setCreateTime(null);
        clone.setUpdateTime(null);
        if (gateElementMapper.insert(clone) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "COPY", clone, null,
            source.getElementCode() + " → " + clone.getElementCode() + " 复制新草稿");
        return clone;
    }

    /**
     * 历史恢复：仅草稿可回滚；从指定审计行的 before_data 定义快照恢复字段。
     * 已发布要素请走 copy（对已发布直接回滚同属定义编辑，一并 409）。
     */
    public GateElement revert(Long id, Long auditLogId, IpdActor actor) {
        requireId(id);
        if (auditLogId == null || auditLogId <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        GateElement exist = requireExisting(id);
        if (!STATUS_DRAFT.equals(exist.getStatus())) {
            throw conflict("仅草稿可回滚，当前状态: " + exist.getStatus());
        }
        AuditLog history = auditLogMapper.selectById(auditLogId);
        if (history == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        if (!"GATE_ELEMENT".equals(history.getEntityType())
            || !id.equals(history.getEntityId())
            || history.getBeforeData() == null || history.getBeforeData().isBlank()) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        GateElement restored = applyHistorySnapshot(exist, history.getBeforeData());
        // Bug#7 中危：审计字段绑 actor
        exist.setUpdateBy(actor.id());
        exist.setUpdateTime(null);
        if (gateElementMapper.updateById(exist) != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.INTERNAL_ERROR);
        }
        audit(actor, "REVERT", exist, restored,
            exist.getElementCode() + " revert@audit#" + auditLogId);
        return exist;
    }

    private GateElement applyHistorySnapshot(GateElement exist, String beforeData) {
        JsonNode node;
        try {
            node = JSON.readTree(beforeData);
        } catch (Exception e) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        if (!node.isObject() || !node.hasNonNull("elementCode")) {
            // 早期审计行仅含 {"detail":...} 摘要，无定义快照，不可回滚
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
        GateElement before = snapshotCopy(exist);
        if (node.hasNonNull("elementName")) {
            exist.setElementName(node.get("elementName").asText());
        }
        if (node.has("passStandard")) {
            exist.setPassStandard(node.get("passStandard").isNull() ? null : node.get("passStandard").asText());
        }
        if (node.hasNonNull("isVeto")) {
            exist.setIsVeto(node.get("isVeto").asText());
        }
        if (node.hasNonNull("sortOrder")) {
            exist.setSortOrder(node.get("sortOrder").asInt());
        }
        if (node.hasNonNull("vetoDualRequired")) {
            exist.setVetoDualRequired(node.get("vetoDualRequired").asText());
        }
        if (node.has("thresholdJson")) {
            exist.setThresholdJson(node.get("thresholdJson").isNull() ? null : node.get("thresholdJson").asText());
        }
        try {
            validateDefinition(exist);
        } catch (IpdBusinessException e) {
            // 快照恢复后校验失败则回滚内存态，保持读改写幂等
            exist.setElementName(before.getElementName());
            exist.setPassStandard(before.getPassStandard());
            exist.setIsVeto(before.getIsVeto());
            exist.setSortOrder(before.getSortOrder());
            exist.setVetoDualRequired(before.getVetoDualRequired());
            exist.setThresholdJson(before.getThresholdJson());
            throw e;
        }
        return before;
    }

    /** 定义字段统一校验（create/update/copy/revert 共用），失败即 PARAM_INVALID。 */
    private void validateDefinition(GateElement e) {
        if (e.getGateCode() == null || !GATES.contains(e.getGateCode())) {
            throw invalid("gateCode 必须为 G1..G5: " + e.getGateCode());
        }
        String code = e.getElementCode();
        if (code == null || code.isBlank() || code.trim().length() > CODE_MAX) {
            throw invalid("要素编码必填且 ≤" + CODE_MAX + " 字符");
        }
        if (e.getElementCode() != null) {
            e.setElementCode(code.trim());
        }
        String name = e.getElementName();
        if (name == null || name.isBlank() || name.trim().length() > NAME_MAX) {
            throw invalid("要素名必填且 ≤" + NAME_MAX + " 字符");
        }
        if (e.getElementName() != null) {
            e.setElementName(name.trim());
        }
        if (e.getPassStandard() != null && e.getPassStandard().length() > STANDARD_MAX) {
            throw invalid("通过标准超长（≤" + STANDARD_MAX + " 字符）");
        }
        if (e.getIsVeto() != null && !FLAGS.contains(e.getIsVeto())) {
            throw invalid("isVeto 必须为 '0' 或 '1'");
        }
        if (e.getEnabled() != null && !FLAGS.contains(e.getEnabled())) {
            throw invalid("enabled 必须为 '0' 或 '1'");
        }
        if (e.getVetoDualRequired() != null && !FLAGS.contains(e.getVetoDualRequired())) {
            throw invalid("vetoDualRequired 必须为 '0' 或 '1'");
        }
        if ("1".equals(e.getVetoDualRequired()) && !"1".equals(e.getIsVeto())) {
            throw invalid("vetoDualRequired='1' 仅适用于否决项（isVeto='1'）");
        }
        validateThresholdJson(e);
        if (e.getSortOrder() != null && (e.getSortOrder() < 0 || e.getSortOrder() > 9999)) {
            throw invalid("sortOrder 超出范围 0..9999");
        }
        if (e.getStatus() != null
            && !Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_ARCHIVED).contains(e.getStatus())) {
            throw invalid("status 非法: " + e.getStatus());
        }
    }

    /** thresholdJson：空视为 null；否则必须是键非空、值均为整数的 JSON 对象且 ≤512 字符。 */
    private void validateThresholdJson(GateElement e) {
        String raw = e.getThresholdJson();
        if (raw == null || raw.isBlank()) {
            return;
        }
        if (raw.length() > THRESHOLD_MAX) {
            throw invalid("thresholdJson 超长（≤" + THRESHOLD_MAX + " 字符）");
        }
        JsonNode node;
        try {
            node = JSON.readTree(raw);
        } catch (Exception ex) {
            throw invalid("thresholdJson 不是合法 JSON");
        }
        if (!node.isObject() || node.isEmpty()) {
            throw invalid("thresholdJson 必须为非空 JSON 对象");
        }
        node.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (key == null || key.isBlank()) {
                throw invalid("thresholdJson 键不可为空");
            }
            if (!value.isNumber() || !value.isIntegralNumber()) {
                throw invalid("thresholdJson 值必须为整数: " + key);
            }
        });
    }

    private void requireUniqueCode(String elementCode) {
        Long dup = gateElementMapper.selectCount(new LambdaQueryWrapper<GateElement>()
            .eq(GateElement::getElementCode, elementCode));
        if (dup != null && dup > 0) {
            throw conflict("要素编码已存在: " + elementCode);
        }
    }

    private GateElement requireExisting(Long id) {
        GateElement exist = gateElementMapper.selectById(id);
        if (exist == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND);
        }
        return exist;
    }

    private void requireId(Long id) {
        if (id == null || id <= 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID);
        }
    }

    private static IpdBusinessException invalid(String message) {
        return new IpdBusinessException(message);
    }

    private static IpdBusinessException conflict(String message) {
        return new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, message);
    }

    /** 字段级审计快照（显式枚举字段，绝不整体序列化实体——AuditEventData 契约）。 */
    private static GateElement snapshotCopy(GateElement e) {
        return GateElement.builder()
            .id(e.getId()).gateCode(e.getGateCode()).elementCode(e.getElementCode())
            .elementName(e.getElementName()).passStandard(e.getPassStandard())
            .isVeto(e.getIsVeto()).sortOrder(e.getSortOrder()).enabled(e.getEnabled())
            .status(e.getStatus()).version(e.getVersion())
            .vetoDualRequired(e.getVetoDualRequired()).thresholdJson(e.getThresholdJson())
            .build();
    }

    private static Object[] snapshotPairs(GateElement e) {
        return new Object[]{
            "gateCode", e.getGateCode(), "elementCode", e.getElementCode(),
            "elementName", e.getElementName(), "passStandard", e.getPassStandard(),
            "isVeto", e.getIsVeto(), "sortOrder", e.getSortOrder(),
            "enabled", e.getEnabled(), "status", e.getStatus(),
            "version", e.getVersion(), "vetoDualRequired", e.getVetoDualRequired(),
            "thresholdJson", e.getThresholdJson()};
    }

    private void audit(IpdActor actor, String action, GateElement after, GateElement before, String reason) {
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .operatorRole(actor.role())
            .action(action)
            .entityType("GATE_ELEMENT")
            .entityId(after.getId())
            .reason(reason)
            .beforeData(before == null ? null : AuditEventData.json(snapshotPairs(before)))
            .afterData(AuditEventData.json(snapshotPairs(after)))
            .build());
    }
}
