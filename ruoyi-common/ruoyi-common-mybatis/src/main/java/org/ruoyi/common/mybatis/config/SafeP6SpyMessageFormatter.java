package org.ruoyi.common.mybatis.config;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;

import java.util.regex.Pattern;

/**
 * P6Spy formatter that deliberately ignores the bound SQL argument.
 *
 * <p>P6Spy supplies both the JDBC prepared statement and a reconstructed statement with
 * parameter values. The latter can contain passwords, API keys and tokens, so this formatter
 * records only a bounded, literal-free structural form of the prepared statement.</p>
 */
public final class SafeP6SpyMessageFormatter implements MessageFormattingStrategy {

    private static final int MAX_SQL_LENGTH = 4096;
    private static final Pattern SQL_STRING_LITERAL = Pattern.compile("'(?:''|\\\\.|[^'\\\\])*'");
    private static final Pattern SQL_DOLLAR_QUOTED_LITERAL = Pattern.compile(
        "\\$[A-Za-z0-9_]*\\$.*?\\$[A-Za-z0-9_]*\\$", Pattern.DOTALL);
    private static final Pattern SECRET_SHAPED_TOKEN = Pattern.compile(
        "(?i)\\b(?:sk[-_][A-Za-z0-9._~+/-]{8,}|AKIA[A-Z0-9]{12,})\\b");
    private static final Pattern SQL_LINE_COMMENT = Pattern.compile("--[^\\r\\n]*");
    private static final Pattern SQL_BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category,
                                String prepared, String sql, String url) {
        String statement = structuralPreparedSql(prepared);
        return "%s | %d ms | %s | connection %d | %s".formatted(
            safeLabel(now), elapsed, safeLabel(category), connectionId, statement);
    }

    static String structuralPreparedSql(String prepared) {
        if (prepared == null || prepared.isBlank()) {
            return "[prepared-sql-unavailable]";
        }
        String safe = SQL_BLOCK_COMMENT.matcher(prepared).replaceAll(" ");
        safe = SQL_LINE_COMMENT.matcher(safe).replaceAll(" ");
        safe = SQL_DOLLAR_QUOTED_LITERAL.matcher(safe).replaceAll("?");
        safe = SQL_STRING_LITERAL.matcher(safe).replaceAll("'?'");
        safe = SECRET_SHAPED_TOKEN.matcher(safe).replaceAll("[REDACTED]");
        safe = WHITESPACE.matcher(safe).replaceAll(" ").trim();
        if (safe.length() > MAX_SQL_LENGTH) {
            return safe.substring(0, MAX_SQL_LENGTH) + "...(truncated)";
        }
        return safe;
    }

    private static String safeLabel(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\r\\n|]", "_");
    }
}
