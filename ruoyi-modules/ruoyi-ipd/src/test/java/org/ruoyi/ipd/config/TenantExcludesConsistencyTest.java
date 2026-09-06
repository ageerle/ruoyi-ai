package org.ruoyi.ipd.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class TenantExcludesConsistencyTest {

    private static final Path APP_YML = Path.of("ruoyi-admin/src/main/resources/application.yml");
    private static final Path SQL_DIR = Path.of("docs/script/sql/update");

    private Set<String> ddlTables() throws IOException {
        Set<String> result = new HashSet<>();
        try (Stream<Path> files = Files.walk(SQL_DIR)) {
            files.filter(Files::isRegularFile)
                 .filter(p -> p.toString().endsWith(".sql"))
                 .forEach(p -> {
                    try {
                        String content = Files.readString(p);
                        for (String line : content.split("\\n")) {
                            String trim = line.trim();
                            if (trim.startsWith("CREATE TABLE")) {
                                String cleaned = trim.replaceAll("^CREATE TABLE\\s+", "");
                                cleaned = cleaned.replaceAll("IF NOT EXISTS\\s+", "");
                                String name = cleaned.replaceAll("[` (].*", "").toLowerCase();
                                if (!name.isEmpty() && !name.startsWith("sys_")
                                    && !name.startsWith("flow_") && !name.startsWith("trace_")
                                    && !name.startsWith("snail")) {
                                    result.add(name);
                                }
                            }
                        }
                    } catch (IOException ignored) {}
                 });
        }
        return result;
    }

    private Set<String> excludedTables() throws IOException {
        String content = Files.readString(APP_YML);
        int tenantStart = content.indexOf("# 多租户配置");
        if (tenantStart < 0) return Set.of();
        int nextTop = content.indexOf("\\n# ", tenantStart);
        if (nextTop < 0) nextTop = content.length();
        String section = content.substring(tenantStart, nextTop);
        Set<String> result = new HashSet<>();
        for (String line : section.split("\\n")) {
            String trim = line.trim();
            if (trim.startsWith("- ") && !trim.startsWith("- /") && !trim.startsWith("- group:")) {
                String table = trim.substring(2).trim().toLowerCase();
                if (!table.isEmpty() && !table.startsWith("sys_")
                    && !table.startsWith("flow_") && !table.startsWith("trace_")) {
                    result.add(table);
                }
            }
        }
        return result;
    }

    @Test
    @DisplayName("R8-P0-1：所有 DDL IPD 业务表均已在 tenant.excludes 中登记")
    void allDdlTablesExcluded() throws IOException {
        Set<String> ddl = ddlTables();
        Set<String> excluded = excludedTables();
        Set<String> missing = new HashSet<>(ddl);
        missing.removeAll(excluded);
        assertThat(missing)
            .as("以下 DDL 表未登记在 tenant.excludes：%s", missing)
            .isEmpty();
    }

    @Test
    @DisplayName("R8-P0-1：已知必含表均在 excludes 中")
    void knownMissingTablesAreCovered() throws IOException {
        Set<String> excluded = excludedTables();
        Set<String> required = new HashSet<>(Arrays.asList(
            "bonus_allocations", "project_scores", "contributions",
            "negative_feedbacks", "requirement_pool", "ai_model_configs",
            "system_config_versions", "legacy_imports", "receipt_ledger"
        ));
        Set<String> notFound = new HashSet<>(required);
        notFound.removeAll(excluded);
        assertThat(notFound)
            .as("以下表未在 tenant.excludes 中，需补登：%s", notFound)
            .isEmpty();
    }
}
