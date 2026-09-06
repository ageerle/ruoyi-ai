package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Person;
import org.ruoyi.ipd.domain.ProductGroup;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectMember;
import org.ruoyi.ipd.domain.ProjectScore;
import org.ruoyi.ipd.domain.ProjectScoreRecord;
import org.ruoyi.ipd.domain.SystemConfigVersion;
import org.ruoyi.ipd.dto.ProjectScoreSubmitReq;
import org.ruoyi.ipd.mapper.PersonMapper;
import org.ruoyi.ipd.mapper.ProductGroupMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.mapper.ProjectMemberMapper;
import org.ruoyi.ipd.mapper.ProjectScoreMapper;
import org.ruoyi.ipd.mapper.ProjectScoreRecordMapper;
import org.ruoyi.ipd.mapper.SystemConfigVersionMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.vo.ProjectScoreView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 项目绩效归档与规则快照（P3-2.2）。
 *
 * <p>每个评分组件独立追加，{@link ProjectScoreRecord} 只允许插入，不提供更新评分通道。
 * 结算取三个组件共同存在的最高 versionNo，规则 JSON 与版本号随记录固化。
 */
@Service
@RequiredArgsConstructor
public class ProjectScoreArchiveService {

    private static final String DEFAULT_RULE_JSON =
        "{\"self\":0.2,\"marketLeader\":0.4,\"rdLeader\":0.4}";
    private static final Set<String> COMPONENTS = Set.of("SELF", "MARKET_LEADER", "RD_LEADER");
    private static final Set<String> TARGET_ROLES = Set.of("MARKET_PM", "RD_PM");

    private final ProjectScoreMapper projectScoreMapper;
    private final ProjectScoreRecordMapper recordMapper;
    private final ProjectMapper projectMapper;
    private final ProjectMemberMapper memberMapper;
    private final PersonMapper personMapper;
    private final ProductGroupMapper groupMapper;
    private final IpdPermission permission;
    private final SystemConfigService configService;
    private final SystemConfigVersionMapper versionMapper;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    /** 独立提交一个评分组件；ARCHIVED 状态后再次提交只能产生新版本。 */
    @Transactional(rollbackFor = Exception.class)
    public ProjectScoreView submit(ProjectScoreSubmitReq request) {
        IpdActor actor = permission.requireInternal();
        requireRequest(request);
        Project project = requireProject(request.projectId());
        ProjectMember target = requireTargetMember(request.projectId(), request.personId());
        String targetRole = target.getRole();
        Person targetPerson = requirePerson(target.getPersonId());
        requireAuthor(actor, request, target, targetPerson);
        validateComponentWithBase(targetRole, request.componentType(), request.score());

        RuleSnapshot rule = currentRule();
        ProjectScoreRecord previous = recordMapper.selectOne(
            Wrappers.<ProjectScoreRecord>lambdaQuery()
                .eq(ProjectScoreRecord::getProjectId, request.projectId())
                .eq(ProjectScoreRecord::getPersonId, request.personId())
                .eq(ProjectScoreRecord::getComponentType, request.componentType())
                .eq(ProjectScoreRecord::getStatus, "ARCHIVED")
                .orderByDesc(ProjectScoreRecord::getVersionNo)
                .last("LIMIT 1 FOR UPDATE"));
        int nextVersion = previous == null || previous.getVersionNo() == null
            ? 1 : previous.getVersionNo() + 1;
        Date now = new Date();
        ProjectScoreRecord row = ProjectScoreRecord.builder()
            .projectId(request.projectId())
            .personId(request.personId())
            .pmRole(targetRole)
            .componentType(request.componentType())
            .score(request.score().setScale(2, java.math.RoundingMode.HALF_UP))
            .versionNo(nextVersion)
            .ruleVersion(rule.version())
            .ruleSnapshot(rule.json())
            .status("ARCHIVED")
            .authorId(actor.id())
            .authorRole(actor.role())
            .reason(trimToNull(request.reason()))
            .submittedAt(now)
            .build();
        permission.bindCreateAudit(row, actor);
        recordMapper.insert(row);
        auditLogService.append(AuditLog.builder()
            .operatorId(actor.id())
            .operatorName(actor.name())
            .action("PROJECT_SCORE_ARCHIVE")
            .entityType("project_score_records")
            .entityId(row.getId())
            .reason("component=" + request.componentType() + " version=" + nextVersion)
            .afterData(AuditEventData.json(
                "projectId", request.projectId(), "personId", request.personId(),
                "pmRole", targetRole, "componentType", request.componentType(),
                "score", row.getScore(), "versionNo", nextVersion,
                "ruleVersion", rule.version(), "ruleSnapshot", rule.json()))
            .createTime(now)
            .build());
        BigDecimal self = "SELF".equals(row.getComponentType()) ? row.getScore() : null;
        BigDecimal market = "MARKET_LEADER".equals(row.getComponentType()) ? row.getScore() : null;
        BigDecimal rd = "RD_LEADER".equals(row.getComponentType()) ? row.getScore() : null;
        return new ProjectScoreView(row.getProjectId(), row.getPersonId(), row.getPmRole(),
            row.getVersionNo(), row.getRuleVersion(), self, market, rd, null, false);
    }

    /** 按最高共同版本重算结算视图；不读取当前规则。 */
    public ProjectScoreView settleVersion(Long projectId, Long personId) {
        ProjectScoreView result = view(projectId, personId);
        if (!result.settled()) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "项目评分三组件尚未全部归档，不能结算");
        }
        return result;
    }

    /** 只读当前共同版本；历史重算调用同一方法，结果可重复。 */
    public ProjectScoreView view(Long projectId, Long personId) {
        requireProjectAndTarget(projectId, personId);
        List<ProjectScoreRecord> records = recordMapper.selectList(
            Wrappers.<ProjectScoreRecord>lambdaQuery()
                .eq(ProjectScoreRecord::getProjectId, projectId)
                .eq(ProjectScoreRecord::getPersonId, personId)
                .eq(ProjectScoreRecord::getStatus, "ARCHIVED")
                .orderByDesc(ProjectScoreRecord::getVersionNo)
                .orderByAsc(ProjectScoreRecord::getComponentType));
        if (records.isEmpty()) {
            return new ProjectScoreView(projectId, personId, targetRole(projectId, personId),
                0, 0, null, null, null, null, false);
        }
        Map<Integer, ComponentVersion> grouped = new LinkedHashMap<>();
        for (ProjectScoreRecord row : records) {
            if (row.getVersionNo() == null || row.getComponentType() == null) {
                continue;
            }
            ComponentVersion version = grouped.computeIfAbsent(row.getVersionNo(), ignored -> new ComponentVersion());
            version.values.put(row.getComponentType(), row.getScore());
            if (row.getRuleSnapshot() != null) {
                version.ruleJson = row.getRuleSnapshot();
                version.ruleVersion = row.getRuleVersion();
            }
        }
        Integer chosenVersion = grouped.keySet().stream()
            .filter(version -> COMPONENTS.stream().allMatch(grouped.get(version).values::containsKey))
            .max(Comparator.naturalOrder()).orElse(null);
        if (chosenVersion == null) {
            ProjectScoreRecord latest = records.get(0);
            Map<String, BigDecimal> latestValues = new LinkedHashMap<>();
            for (ProjectScoreRecord row : records) {
                if (row.getComponentType() != null) {
                    latestValues.put(row.getComponentType(), row.getScore());
                }
            }
            return new ProjectScoreView(projectId, personId, latest.getPmRole(), latest.getVersionNo(),
                latest.getRuleVersion() == null ? 1 : latest.getRuleVersion(),
                latestValues.get("SELF"), latestValues.get("MARKET_LEADER"),
                latestValues.get("RD_LEADER"), null, false);
        }
        ComponentVersion chosen = grouped.get(chosenVersion);
        JsonNode json = parseRule(chosen.ruleJson == null || chosen.ruleJson.isBlank()
            ? DEFAULT_RULE_JSON : chosen.ruleJson);
        BigDecimal selfWeight = decimal(json, "self");
        BigDecimal marketWeight = decimal(json, "marketLeader");
        BigDecimal rdWeight = decimal(json, "rdLeader");
        ProjectScoreService.validateWeights(List.of(selfWeight, marketWeight, rdWeight));
        BigDecimal weighted = chosen.values.get("SELF").multiply(selfWeight)
            .add(chosen.values.get("MARKET_LEADER").multiply(marketWeight))
            .add(chosen.values.get("RD_LEADER").multiply(rdWeight))
            .setScale(2, java.math.RoundingMode.HALF_UP);
        return new ProjectScoreView(projectId, personId, targetRole(projectId, personId), chosenVersion,
            chosen.ruleVersion == null ? 1 : chosen.ruleVersion,
            chosen.values.get("SELF"), chosen.values.get("MARKET_LEADER"),
            chosen.values.get("RD_LEADER"), weighted, true);
    }

    private void requireAuthor(IpdActor actor, ProjectScoreSubmitReq request,
                                ProjectMember target, Person targetPerson) {
        if (actor == null) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "不能代替其他会话人评分");
        }
        String component = request.componentType();
        if ("SELF".equals(component)) {
            if (!Objects.equals(actor.id(), target.getPersonId())
                || !target.getRole().equals(actor.role())) {
                throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "自评必须由被评 PM 本人提交");
            }
            return;
        }
        if (!Set.of("GROUP_LEADER", "SUPER_ADMIN").contains(actor.role())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "不存在评审上级/技术委员会角色");
        }
        if ("SUPER_ADMIN".equals(actor.role())) {
            return;
        }
        ProductGroup group = groupMapper.selectById(targetPerson.getGroupId());
        if (group == null || !Objects.equals(group.getLeaderPersonId(), actor.id())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "组长必须是被评 PM 所在产品组 leader");
        }
    }

    private void validateComponentWithBase(String targetRole, String component, BigDecimal score) {
        ProjectScore draft = ProjectScore.builder()
            .pmRole(targetRole)
            .selfScore("SELF".equals(component) ? score : BigDecimal.ZERO)
            .marketLeaderScore("MARKET_LEADER".equals(component) ? score : BigDecimal.ZERO)
            .rdLeaderScore("RD_LEADER".equals(component) ? score : BigDecimal.ZERO)
            .build();
        new ProjectScoreService().draft(draft);
    }

    private RuleSnapshot currentRule() {
        String ruleJson = configService.getValue("kpi.reviewWeights", DEFAULT_RULE_JSON);
        if (ruleJson == null || ruleJson.isBlank()) {
            ruleJson = DEFAULT_RULE_JSON;
        }
        JsonNode parsed = parseRule(ruleJson);
        ProjectScoreService.validateWeights(List.of(
            decimal(parsed, "self"), decimal(parsed, "marketLeader"), decimal(parsed, "rdLeader")));
        SystemConfigVersion latest = versionMapper.selectOne(
            Wrappers.<SystemConfigVersion>lambdaQuery()
                .eq(SystemConfigVersion::getConfigKey, "kpi.reviewWeights")
                .orderByDesc(SystemConfigVersion::getVersion)
                .last("LIMIT 1"));
        int version = latest == null || latest.getVersion() == null ? 1 : latest.getVersion() + 1;
        return new RuleSnapshot(version, ruleJson);
    }

    private JsonNode parseRule(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            if (node == null || !node.isObject()) {
                throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "绩效规则快照不是 JSON 对象");
            }
            return node;
        } catch (JsonProcessingException | IpdBusinessException e) {
            if (e instanceof IpdBusinessException business) {
                throw business;
            }
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "绩效规则快照 JSON 非法");
        }
    }

    private BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "绩效规则缺少字段: " + field);
        }
        return value.decimalValue();
    }

    private Project requireProject(Long projectId) {
        Project project = projectId == null ? null : projectMapper.selectById(projectId);
        if (project == null || "1".equals(project.getDelFlag())) {
            throw new IpdBusinessException(ApiV1ErrorCode.NOT_FOUND, "项目不存在");
        }
        return project;
    }

    private ProjectMember requireTargetMember(Long projectId, Long personId) {
        List<ProjectMember> list = memberMapper.selectList(
            Wrappers.<ProjectMember>lambdaQuery()
                .eq(ProjectMember::getProjectId, projectId)
                .eq(ProjectMember::getPersonId, personId)
                .isNull(ProjectMember::getExitDate));
        if (list.size() != 1) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT,
                "项目必须有且仅有一个在职 MARKET_PM/RD_PM 被评人");
        }
        ProjectMember member = list.get(0);
        if (!TARGET_ROLES.contains(member.getRole())) {
            throw new IpdBusinessException(ApiV1ErrorCode.FORBIDDEN, "被评人必须为双 PM");
        }
        return member;
    }

    private Person requirePerson(Long personId) {
        Person person = personId == null ? null : personMapper.selectById(personId);
        if (person == null || !TARGET_ROLES.contains(person.getPersonType())) {
            throw new IpdBusinessException(ApiV1ErrorCode.STATE_CONFLICT, "被评人人员角色不合法");
        }
        return person;
    }

    private void requireProjectAndTarget(Long projectId, Long personId) {
        requireProject(projectId);
        requireTargetMember(projectId, personId);
    }

    private String targetRole(Long projectId, Long personId) {
        return requireTargetMember(projectId, personId).getRole();
    }

    private void requireRequest(ProjectScoreSubmitReq request) {
        if (request == null || request.componentType() == null
            || !COMPONENTS.contains(request.componentType().toUpperCase())) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID,
                "componentType 必须为 SELF|MARKET_LEADER|RD_LEADER");
        }
        if (request.score() == null || request.score().compareTo(BigDecimal.ZERO) < 0
            || request.score().compareTo(new BigDecimal("100")) > 0) {
            throw new IpdBusinessException(ApiV1ErrorCode.PARAM_INVALID, "评分必须在 [0,100]");
        }
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record RuleSnapshot(int version, String json) { }
    private static final class ComponentVersion {
        private final Map<String, BigDecimal> values = new LinkedHashMap<>();
        private String ruleJson;
        private Integer ruleVersion;
    }
}
