package org.ruoyi.ipd.seed;

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
 */
public final class MarketCodeResolver {

    private static final Map<String, String> ALIASES = new LinkedHashMap<>();

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
     * 解析目标市场 JSON 数组为去重国别码列表（仅已知映射或纯 ASCII 大写码）。
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
            } else if (isAsciiCountryCode(token)) {
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
            if (ALIASES.containsKey(upper) || isAsciiCountryCode(token)) {
                continue;
            }
            unknown.add(token);
        }
        return unknown;
    }

    /** 纯 ASCII 大写/小写字母、长度≤8，视为已是国别码（如 CB/IEC）。 */
    private static boolean isAsciiCountryCode(String token) {
        if (token == null || token.isEmpty() || token.length() > 8) {
            return false;
        }
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (!((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z'))) {
                return false;
            }
        }
        return true;
    }

    /** 从 JSON 数组字符串拆 token。 */
    static List<String> tokens(String targetMarketsJson) {
        List<String> list = new ArrayList<>();
        if (targetMarketsJson == null || targetMarketsJson.isBlank()) {
            return list;
        }
        String compact = targetMarketsJson.trim().replaceAll("[\\[\\]\"]", "");
        if (compact.isBlank()) {
            return list;
        }
        for (String part : compact.split(",")) {
            String t = part.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list;
    }
}
