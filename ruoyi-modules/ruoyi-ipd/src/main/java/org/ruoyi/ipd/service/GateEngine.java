package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 阶段门禁引擎（BR-IPD-06，主 Prompt v3 L560 唯一权威口径）：
 * 当前阶段阻断性动作未完成 → 禁止跳转下一阶段。
 *
 * 必做集按项目等级裁剪（分级规则后台可配置）：
 * - S 级：全部阻断动作（38 个，is_blocking=1 全集）
 * - A 级：超管配置 gate.a_level_block_codes（逗号分隔 code）；v3 原文「约 25」未逐项定死 → 不造无源数据，
 *   未配置时从严回落 S 全集（注释即契约，配置即生效）
 * - B 级：仅 Gate 关联动作（目录 gate 非空 5 项）+ P10/V02 认证 + C12 生物特征合规（v3 原文；
 *   「约 14」为约数，按规则语义实现）
 */
@Service
@RequiredArgsConstructor
public class GateEngine {

    /** B 级特例三动作（v3 L560：仅 Gate 关联动作 + P10/V02 认证 + C12 生物特征合规） */
    private static final Set<String> B_LEVEL_EXTRA = Set.of("P10", "V02", "C12");

    /** A 级必做集超管配置键 */
    public static final String A_LEVEL_CONFIG_KEY = "gate.a_level_block_codes";

    private final StageActionMapper stageActionMapper;
    private final SystemConfigService systemConfigService;

    /** 校验当前阶段：未完成的必做动作非空即拒绝，异常消息携带逐项清单（前端直接展示） */
    public void check(Project project, String currentStage) {
        List<StageAction> actions = stageActionMapper.selectList(
            new LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, project.getId()));
        Set<String> required = requiredCodes(project.getLevel());
        String stagePrefix = currentStage.substring(0, 1);
        List<String> unfinished = actions.stream()
            .filter(a -> required.contains(a.getActionCode()))
            .filter(a -> !"DONE".equals(a.getStatus()) && !"NA".equals(a.getStatus()))
            .map(a -> a.getActionCode() + " " + a.getActionName())
            .collect(Collectors.toList());
        if (!unfinished.isEmpty()) {
            throw new ServiceException("阶段门禁（BR-IPD-06）：阻断性动作未完成，禁止进入下一阶段 —— "
                + String.join("；", unfinished));
        }
    }

    /** 按项目等级解析必做动作编码集（纯函数可测） */
    public Set<String> requiredCodes(String projectLevel) {
        String lv = projectLevel == null ? "S" : projectLevel;
        return switch (lv) {
            case "B" -> bLevelCodes();
            case "A" -> {
                String conf = systemConfigService.getValue(A_LEVEL_CONFIG_KEY, "");
                yield conf.isBlank() ? sLevelCodes()
                    : new LinkedHashSet<>(Arrays.asList(conf.split(",")));
            }
            default -> sLevelCodes();
        };
    }

    private Set<String> sLevelCodes() {
        return ActionCatalog.ALL.stream()
            .filter(ActionDef::blocking)
            .map(ActionDef::code)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> bLevelCodes() {
        Set<String> codes = ActionCatalog.ALL.stream()
            .filter(d -> d.gate() != null && !d.gate().isBlank())
            .map(d -> d.code())
            .collect(Collectors.toCollection(LinkedHashSet::new));
        codes.addAll(B_LEVEL_EXTRA);
        return codes;
    }
}