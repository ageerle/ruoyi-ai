package org.ruoyi.agent.tool;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;

/** SQL syntax and table checks are an additional guard, not a replacement for a read-only DB account. */
final class AgentSqlValidator {
    private AgentSqlValidator() {}

    static void validate(String sql, List<String> allowedTables) {
        // Reject comments conservatively, including MySQL executable comments that a generic parser ignores.
        if (sql.contains("/*") || sql.contains("--") || sql.contains("#")) {
            throw new IllegalArgumentException("Error: SQL comments are not supported");
        }
        Set<String> allowed = allowedTables.stream().map(String::trim)
            .map(name -> name.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        try {
            var statements = CCJSqlParserUtil.parseStatements(sql).getStatements();
            if (statements.size() != 1) {
                throw new IllegalArgumentException("Error: Only a single SELECT is supported");
            }
            Statement statement = statements.get(0);
            if (!(statement instanceof Select select) || select.getWithItemsList() != null) {
                throw new IllegalArgumentException("Error: Only a single SELECT without WITH is supported");
            }
            Set<String> tables = new TablesNamesFinder<Void>() {
                @Override
                public <S> Void visit(Table table, S context) {
                    String name = table.getFullyQualifiedName().replace("`", "");
                    // No cross-database references, even when the unqualified name is allowed.
                    if (!name.matches("[a-zA-Z0-9_]+")
                        || !allowed.contains(name.toLowerCase(Locale.ROOT))) {
                        throw new IllegalArgumentException("Error: SQL references a table outside AGENT_ALLOWED_TABLES");
                    }
                    return super.visit(table, context);
                }

                @Override
                public <S> Void visit(PlainSelect select, S context) {
                    if (select.getIntoTables() != null || select.getIntoTempTable() != null
                        || select.getForMode() != null || select.getForClause() != null
                        || select.getWithItemsList() != null) {
                        throw new IllegalArgumentException("Error: SELECT INTO, locking and WITH are not supported");
                    }
                    return super.visit(select, context);
                }

                @Override
                public <S> Void visit(Function function, S context) {
                    String name = function.getName().toUpperCase(Locale.ROOT);
                    if (name.contains(".") || Set.of("LOAD_FILE", "SLEEP", "BENCHMARK", "GET_LOCK",
                        "RELEASE_LOCK", "RELEASE_ALL_LOCKS").contains(name)) {
                        throw new IllegalArgumentException("Error: SQL function is not allowed");
                    }
                    return super.visit(function, context);
                }
            }.getTables(statement);
            if (tables.isEmpty()) {
                throw new IllegalArgumentException("Error: SELECT must reference an allowed table");
            }
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("Error: Invalid or unsupported SELECT SQL");
        }
    }
}
