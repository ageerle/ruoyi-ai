package org.ruoyi.ipd.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * P1-7.1：目标市场 JSON → 国别码归一（中文名/别名 → ISO 风格码）。
 * 未知 token 保留原样供前端提示手工补充，不抛错。
 *
 * R8-AUTO-7 [后台审查 MEDIUM parser-differential]：用 Jackson ObjectMapper 替换正则 replaceAll。
 * 旧实现 regex `\"[\\[\\]\"]\"` 在引号内含逗号时会被错误切分（如 {@code ["SA,US","CN"]} → ["SA","US","CN"]），
 * 现在用 Jackson 标准 JSON 解析，逗号只在引号外分隔。
 */
public final class MarketCodeResolver {

    private static final Logger log = LoggerFactory.getLogger(MarketCodeResolver.class);

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** ISO 3166 alpha-2/alpha-3 白名单 + 项目自定义的非 ISO 码（CB/IEC）。 */
    private static final Set<String> ISO_CODES = Set.of(
        "CN", "SA", "AE", "BR", "IN", "KR", "JP", "US", "EU", "AU", "MX"
    );
    private static final Set<String> CUSTOM_CODES = Set.of("CB", "IEC");

    static {
        put("CN", "CN", "国内", "中国", "CHINA");
        put("SA", "SA", "沙特", "沙特阿拉伯", "SAUDI", "SAUDI ARABIA");
        put("AE", "AE", "阿联酋", "UAE");
        put("BR", "BR", "巴西", "BRAZIL");
        put("IN", "IN", "印度", "INDIA");
        put("KR", "KR", "韩国", "南韩", "KOREA");
        put("JP", "JP", "日本", "JAPAN");
        put("US", "US", "美国", "USA", "UNITED STATES");
        put("EU", "EU", "欧盟", "欧洲");
        put("AU", "AU", "澳大利亚", "AUSTRALIA");
        put("MX", "MX", "墨西哥", "MEXICO");
    }

    private MarketCodeResolver() {
    }

    private static void put(String code, String... keys) {
        for (String k : keys) {
            ALIASES.put(k.toUpperCase(Locale.ROOT), code);
        }
    }

    /**
     * 解析目标市场 JSON 数组为去重国别码列表（仅已知映射或 ISO 风格码）。
     *
     * @param targetMarketsJson 如 {@code ["SA","沙特","国内"]}
     * @return 已知码列表（有序去重）
     */
    public static List<String> knownCodes(String targetMarketsJson) {
        Set<String> out = new LinkedHashSet<>();
        for (String token : tokens(targetMarketsJson)) {
            String mapped = ALIASES.get(token.toUpperCase(Locale.ROOT));
            if (mapped != null) {
                out.add(mapped);
            } else if (isValidCountryCode(token)) {
                out.add(token.toUpperCase(Locale.ROOT));
            }
        }
        return new ArrayList<>(out);
    }

    /**
     * 无法映射到已知国别的原始 token（供「未知国家提示手工补充」）。
     *
     * @param targetMarketsJson 目标市场 JSON
     * @return 未知 token 列表
     */
    public static List<String> unknownTokens(String targetMarketsJson) {
        List<String> unknown = new ArrayList<>();
        for (String token : tokens(targetMarketsJson)) {
            String upper = token.toUpperCase(Locale.ROOT);
            if (ALIASES.containsKey(upper) || isValidCountryCode(token)) {
                continue;
            }
            unknown.add(token);
        }
        return unknown;
    }

    /**
     * R8-AUTO-7：白名单判断——只接受 ISO 3166 alpha-2/alpha-3 + 项目自定义 (CB/IEC)。
     * 拒绝任意 "as long as ASCII letters ≤8" 的攻击者输入。
     */
    private static boolean isValidCountryCode(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }
        String upper = token.toUpperCase(Locale.ROOT);
        if (ISO_CODES.contains(upper) || CUSTOM_CODES.contains(upper)) {
            return true;
        }
        // 仅允许 2-3 字符纯 ASCII 字母作为 ISO alpha-2/alpha-3 兜底
        if (upper.length() < 2 || upper.length() > 3) {
            return false;
        }
        for (int i = 0; i < upper.length(); i++) {
            char c = upper.charAt(i);
            if (!((c >= 'A' && c <= 'Z'))) {
                return false;
            }
        }
        return true;
    }

    /**
     * R8-AUTO-7：从 JSON 数组字符串拆 token（用 Jackson 解析）。
     * 旧实现 regex 替换引号/方括号导致含逗号字符串被错误切分，已替换为标准 JSON 解析。
     * 解析失败时降级为空列表（不抛错，与旧行为兼容）。
     */
    static List<String> tokens(String targetMarketsJson) {
        List<String> list = new ArrayList<>();
        if (targetMarketsJson == null || targetMarketsJson.isBlank()) {
            return list;
        }
        String trimmed = targetMarketsJson.trim();
        // 兼容裸字符串（不带 [ ] 的单值）：直接走原 split 路径
        if (!trimmed.startsWith("[")) {
            for (String part : trimmed.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) {
                    list.add(t);
                }
            }
            return list;
        }
        try {
            List<String> parsed = MAPPER.readValue(trimmed, new TypeReference<List<String>>() {});
            if (parsed != null) {
                for (String t : parsed) {
                    if (t != null && !t.isBlank()) {
                        list.add(t.trim());
                    }
                }
            }
        } catch (Exception ex) {
            log.warn("MarketCodeResolver tokens parse failed for input={}, fallback to empty", trimmed, ex);
        }
        return list;
    }
}
