package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.dto.GateChecklistItem;
import org.ruoyi.ipd.dto.GateChecklistView;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 阶段门禁引擎（BR-IPD-06，主 Prompt v3 L560 唯一权威口径）：
 * 当前阶段阻断性动作未完成 → 禁止跳转下一阶段。
 *
 * <p>P1-5.1：只判当前阶段应做集；缺失/未实例化必做动作拒绝；未来阶段不阻塞。
 * <p>P1-5.2：S/A/B 必做集配置校验与可解释清单（AC-IPD-10/11）。
 *
 * 必做集按项目等级裁剪：
 * <ul>
 *   <li>S 级：全部阻断动作（38 个）</li>
 *   <li>A 级：超管配置 gate.a_level_block_codes（trim/去重/未知码拒绝）；未配置从严回落 S</li>
 *   <li>B 级：ActionCatalog.B_LEVEL_BLOCKING_CODES（权威 10 项，不硬凑 14）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class GateEngine {

    /** A 级必做集超管配置键 */
    public static final String A_LEVEL_CONFIG_KEY = "gate.a_level_block_codes";

    private final StageActionMapper stageActionMapper;
    private final SystemConfigService systemConfigService;

    /**
     * 校验当前阶段门禁：未完成或未实例化的必做动作非空即拒绝。
     *
     * @param project      项目（含等级）
     * @param currentStage 当前阶段编码 CONCEPT|PLAN|…
     */
    public void check(Project project, String currentStage) {
        if (currentStage == null || currentStage.isBlank()) {
            throw new ServiceException("当前阶段不能为空");
        }
        Set<String> requiredHere = requiredCodesForStage(project.getLevel(), currentStage);
        if (requiredHere.isEmpty()) {
            return;
        }
        Map<String, StageAction> byCode = loadByCode(project.getId(), requiredHere);
        List<String> unfinished = new ArrayList<>();
        for (String code : requiredHere) {
            StageAction action = byCode.get(code);
            if (action == null) {
                ActionDef def = ActionCatalog.byCode(code);
                unfinished.add(code + " " + def.name() + "（未实例化）");
                continue;
            }
            if (!"DONE".equals(action.getStatus()) && !"NA".equals(action.getStatus())
                && !LegacyImportService.HISTORY_MISSING.equals(action.getHistoryMark())) {
                unfinished.add(code + " " + action.getActionName());
            }
        }
        if (!unfinished.isEmpty()) {
            throw new ServiceException("阶段门禁（BR-IPD-06）：阻断性动作未完成，禁止进入下一阶段 —— "
                + String.join("；", unfinished));
        }
    }

    /**
     * P1-5.2：返回本阶段必做清单 + 逐项原因 + 配置版本。
     *
     * @param project 项目
     * @param stage   阶段；空则用 currentStage
     * @return 可解释视图
     */
    public GateChecklistView explainChecklist(Project project, String stage) {
        if (project == null || project.getId() == null) {
            throw new ServiceException("项目不能为空");
        }
        String st = (stage == null || stage.isBlank()) ? project.getCurrentStage() : stage;
        if (st == null || st.isBlank()) {
            throw new ServiceException("阶段不能为空");
        }
        String level = project.getLevel() == null ? "S" : project.getLevel();
        String configVersion = configVersionOf(level);
        Set<String> required = requiredCodesForStage(level, st);
        Map<String, StageAction> byCode = loadByCode(project.getId(), required);
        List<GateChecklistItem> items = new ArrayList<>();
        for (String code : required) {
            ActionDef def = ActionCatalog.byCode(code);
            StageAction action = byCode.get(code);
            if (action == null) {
                items.add(new GateChecklistItem(code, def.name(), st, null, false,
                    "必做未实例化；来源=" + sourceLabel(level, code)));
                continue;
            }
            boolean ok = "DONE".equals(action.getStatus()) || "NA".equals(action.getStatus())
                || LegacyImportService.HISTORY_MISSING.equals(action.getHistoryMark());
            String reason;
            if (LegacyImportService.HISTORY_MISSING.equals(action.getHistoryMark())) {
                reason = "历史缺失（BR-PROD-03）；来源=" + sourceLabel(level, code);
            } else if (ok) {
                reason = "已满足（" + action.getStatus() + "）；来源=" + sourceLabel(level, code);
            } else {
                reason = "未完成 status=" + action.getStatus() + "；来源=" + sourceLabel(level, code);
            }
            items.add(new GateChecklistItem(code, def.name(), st, action.getStatus(), ok, reason));
        }
        return new GateChecklistView(project.getId(), level, st, configVersion, items);
    }

    /**
     * 等级必做集 ∩ 当前阶段目录动作。
     *
     * @param projectLevel 项目等级
     * @param stage        当前阶段
     * @return 本阶段应检查的编码集（保序）
     */
    public Set<String> requiredCodesForStage(String projectLevel, String stage) {
        Set<String> stageCodes = ActionCatalog.byStage(stage).stream()
            .map(ActionDef::code)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        return requiredCodes(projectLevel).stream()
            .filter(stageCodes::contains)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 按项目等级解析必做动作编码集（纯函数可测） */
    public Set<String> requiredCodes(String projectLevel) {
        String lv = projectLevel == null ? "S" : projectLevel;
        return switch (lv) {
            case "B" -> bLevelCodes();
            case "A" -> {
                String conf = systemConfigService.getValue(A_LEVEL_CONFIG_KEY, "");
                yield conf.isBlank() ? sLevelCodes() : normalizeALevelConfig(conf);
            }
            default -> sLevelCodes();
        };
    }

    /**
     * A 级配置规范化：trim、去重、别名归一；未知码拒绝。
     * 空串返回空集（调用方决定是否回落 S）。
     *
     * @param raw 逗号分隔配置
     * @return 保序编码集
     */
    public static Set<String> normalizeALevelConfig(String raw) {
        if (raw == null || raw.isBlank()) {
            return new LinkedHashSet<>();
        }
        LinkedHashSet<String> out = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            if (part == null) {
                continue;
            }
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            String code = ActionCatalog.resolveCode(token);
            try {
                ActionCatalog.byCode(code);
            } catch (IllegalArgumentException ex) {
                throw new ServiceException("A 级必做集含未知动作码: " + token);
            }
            out.add(code);
        }
        return out;
    }

    /**
     * 写配置前校验并返回规范化 CSV（供 SystemConfigService 使用）。
     *
     * @param raw 原始值
     * @return 规范化逗号串；空输入返回 ""
     */
    public static String validateAndNormalizeALevelConfigValue(String raw) {
        Set<String> codes = normalizeALevelConfig(raw);
        return String.join(",", codes);
    }

    /**
     * R8-AUTO-8 [PERF-01]：loadByCode 加 .in(actionCode, requiredCodes) 过滤。
     * 100 并发下 S 级项目 69 行全表拉取降到 ≤38 行必做集，配合 idx_sa_project_code 索引
     * （参考 Round 8 R8-PERF-08 SQL）消除 filesort 与 100×6ms 阻塞池。
     *
     * @param projectId     项目 ID
     * @param requiredCodes 本阶段必做集（resolveCode 后）；空集合返回空 Map
     * @return code → action 映射
     */
    private Map<String, StageAction> loadByCode(Long projectId, Set<String> requiredCodes) {
        if (requiredCodes == null || requiredCodes.isEmpty()) {
            return Map.of();
        }
        List<StageAction> actions = stageActionMapper.selectList(
            new LambdaQueryWrapper<StageAction>()
                .eq(StageAction::getProjectId, projectId)
                .in(StageAction::getActionCode, requiredCodes));
        Map<String, StageAction> byCode = new LinkedHashMap<>();
        for (StageAction action : actions) {
            if (action.getActionCode() != null) {
                byCode.putIfAbsent(ActionCatalog.resolveCode(action.getActionCode()), action);
            }
        }
        return byCode;
    }

    private String configVersionOf(String level) {
        return switch (level == null ? "S" : level) {
            case "A" -> {
                String conf = systemConfigService.getValue(A_LEVEL_CONFIG_KEY, "");
                if (conf.isBlank()) {
                    yield "fallback:S-blocking-38";
                }
                yield "A:" + String.join(",", normalizeALevelConfig(conf));
            }
            case "B" -> "B:catalog-" + ActionCatalog.B_LEVEL_BLOCKING_CODES.size();
            default -> "S:blocking-38";
        };
    }

    private String sourceLabel(String level, String code) {
        return switch (level == null ? "S" : level) {
            case "A" -> {
                String conf = systemConfigService.getValue(A_LEVEL_CONFIG_KEY, "");
                yield conf.isBlank() ? "S级阻断全集回落" : "超管配置 " + A_LEVEL_CONFIG_KEY;
            }
            case "B" -> ActionCatalog.B_LEVEL_BLOCKING_CODES.contains(code)
                ? "B级权威清单" : "B级权威清单";
            default -> "S级阻断动作";
        };
    }

    private Set<String> sLevelCodes() {
        return ActionCatalog.ALL.stream()
            .filter(ActionDef::blocking)
            .map(ActionDef::code)
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> bLevelCodes() {
        return new LinkedHashSet<>(ActionCatalog.B_LEVEL_BLOCKING_CODES);
    }
}
