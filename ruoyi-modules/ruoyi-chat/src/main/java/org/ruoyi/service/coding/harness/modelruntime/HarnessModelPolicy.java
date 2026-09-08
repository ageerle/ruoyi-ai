package org.ruoyi.service.coding.harness.modelruntime;

import java.util.Locale;
import java.util.List;
import java.util.Set;

import org.ruoyi.service.coding.harness.model.HarnessTaskClass;

/** Deterministic, zero-token routing hints for the configured DeepSeek coding models. */
public final class HarnessModelPolicy {

    public static final int POLICY_VERSION = 2;
    public static final String AUTO = "deepseek-auto";
    public static final String FLASH = "deepseek-v4-flash";
    public static final String PRO = "deepseek-v4-pro";
    public static final String VISION = "deepseek-v4-flash-vision-exp";

    /** 字节 Doubao-Seed-Evolving 模型（精确匹配，不参与 DeepSeek 自动路由）。 */
    public static final String DOUBAO_SEED_EVOLVING = "doubao-seed-evolving";
    public static final String DOUBAO_SEED_EVOLVING_QUALIFIED =
        "bytedance/doubao-seed-evolving";

    private static final List<String> AUTOMATIC_CANDIDATES = List.of(FLASH, PRO, VISION);

    private static final Set<String> COMPLEX_MARKERS = Set.of(
        "架构", "重构", "迁移", "安全", "并发", "多租户", "分布式", "跨仓库",
        "多表", "联表", "事务", "权限", "三端", "全栈", "数据库迁移", "端到端",
        "多模块", "跨模块",
        "architecture", "refactor", "migration", "security", "concurrency",
        "distributed", "multi-tenant", "cross-repository", "multi-table", "join table",
        "transaction", "authorization", "permissions", "database migration", "end-to-end",
        "full-stack", "multi-module", "cross-module");

    private static final Set<String> BACKEND_MARKERS = Set.of(
        "后端", "服务端", "接口", "backend", "server", " api ");
    private static final Set<String> FRONTEND_MARKERS = Set.of(
        "前端", "页面", "管理端", "用户端", "frontend", "page", " ui ");
    private static final Set<String> DATA_MARKERS = Set.of(
        "数据库", "数据表", "建表", "sql", "database", "schema", " table ");

    private HarnessModelPolicy() { }

    public static boolean isAutomatic(String model) {
        return AUTO.equals(normalizeModel(model));
    }

    /**
     * 是否为字节 Doubao-Seed-Evolving 模型。精确支持 doubao-seed-evolving 与
     * bytedance/doubao-seed-evolving 两个 Model ID；Doubao 会话固定模型，永不
     * 回退到 DeepSeek 自动路由。
     */
    public static boolean isDoubao(String model) {
        String normalized = normalizeModel(model);
        return DOUBAO_SEED_EVOLVING.equals(normalized)
            || DOUBAO_SEED_EVOLVING_QUALIFIED.equals(normalized);
    }

    /** 模型是否支持图片输入（Doubao-Seed-Evolving 原生支持多模态图片理解）。 */
    public static boolean supportsImages(String model) {
        return isDoubao(model) || VISION.equals(normalizeModel(model))
            || isAutomatic(model);
    }

    /** 模型是否支持 Doubao 七档思考等级选择。 */
    public static boolean supportsThinkingLevel(String model) {
        return isDoubao(model);
    }

    /** 模型是否支持 Doubao 图片精度 low/high/xhigh 选择。 */
    public static boolean supportsImageDetail(String model) {
        return isDoubao(model);
    }

    public static List<String> automaticCandidates() {
        return AUTOMATIC_CANDIDATES;
    }

    public static HarnessTaskClass classify(String requirement, boolean hasImages) {
        if (hasImages) {
            return HarnessTaskClass.VISION;
        }
        String normalized = requirement == null ? "" : requirement.toLowerCase(Locale.ROOT);
        return normalized.length() > 1_500
            || COMPLEX_MARKERS.stream().anyMatch(normalized::contains)
            || matchedLayerCount(" " + normalized + " ") >= 2
            ? HarnessTaskClass.COMPLEX : HarnessTaskClass.SIMPLE;
    }

    private static int matchedLayerCount(String requirement) {
        int count = 0;
        count += containsAny(requirement, BACKEND_MARKERS) ? 1 : 0;
        count += containsAny(requirement, FRONTEND_MARKERS) ? 1 : 0;
        count += containsAny(requirement, DATA_MARKERS) ? 1 : 0;
        return count;
    }

    private static boolean containsAny(String requirement, Set<String> markers) {
        return markers.stream().anyMatch(requirement::contains);
    }

    public static String selectAutomaticModel(HarnessTaskClass taskClass) {
        if (taskClass == null) {
            throw new IllegalArgumentException("taskClass is required");
        }
        return switch (taskClass) {
            case VISION -> VISION;
            case COMPLEX -> PRO;
            case SIMPLE -> FLASH;
        };
    }

    public static boolean enableThinking(String model, String requirement) {
        return enableThinking(model, classify(requirement, false));
    }

    public static boolean enableThinking(String model, HarnessTaskClass taskClass) {
        // Doubao 默认开启深度思考；none 关闭由会话思考等级在请求构造处决定。
        if (isDoubao(model)) {
            return true;
        }
        return PRO.equals(normalizeModel(model)) && taskClass == HarnessTaskClass.COMPLEX;
    }

    public static String normalizeModel(String model) {
        String normalized = model == null ? "" : model.strip();
        if (normalized.isEmpty() || normalized.length() > 200) {
            throw new IllegalArgumentException("model is required and must not exceed 200 characters");
        }
        return normalized;
    }
}
