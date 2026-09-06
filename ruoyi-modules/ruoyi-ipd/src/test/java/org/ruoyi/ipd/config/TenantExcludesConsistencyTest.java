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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("dev")
class TenantExcludesConsistencyTest {

    private static final Path REPO_ROOT = Path.of(System.getProperty("user.dir"))
            .getParent().getParent();
    private static final Path APP_YML = REPO_ROOT.resolve(
            "ruoyi-admin/src/main/resources/application.yml");
    private static final Path SQL_DIR = REPO_ROOT.resolve("docs/script/sql/update");

    private static final Pattern CREATE_TABLE_PAT = Pattern.compile(
        "CREATE TABLE\s+(?:IF NOT EXISTS\s+)?[`']?([a-z_]+)[`']?\s*\(",
        Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCLUDE_ITEM_PAT = Pattern.compile("^\s*-\s+([a-z_]+)\s*$");

    private Set<String> ddlTables() throws IOException {
        Set<String> result = new HashSet<>();
        try (Stream<Path> files = Files.walk(SQL_DIR)) {
            files.filter(Files::isRegularFile)
                 .filter(x -> x.toString().endsWith(".sql"))
                 .forEach(x -> {
                    try {
                        String c = Files.readString(x);
                        Matcher m = CREATE_TABLE_PAT.matcher(c);
                        while (m.find()) {
                            String name = m.group(1).toLowerCase();
                            if (!name.startsWith("sys_") && !name.startsWith("flow_")
                                && !name.startsWith("trace_") && !name.startsWith("snail")) {
                                result.add(name);
                            }
                        }
                    } catch (IOException ignored) {}
                 });
        }
        return result;
    }

    private Set<String> excludedTables() throws IOException {
        String content = Files.readString(APP_YML);
        int start = content.indexOf("# 多租户配置");
        if (start < 0) return Set.of();
        int end = content.indexOf("\n# ", start);
        if (end < 0) end = content.length();
        String section = content.substring(start, end);
        Set<String> result = new HashSet<>();
        for (String line : section.split("\n")) {
            Matcher m = EXCLUDE_ITEM_PAT.matcher(line);
            if (m.matches()) {
                String table = m.group(1).toLowerCase();
                if (!table.startsWith("sys_") && !table.startsWith("flow_")
                    && !table.startsWith("trace_")) {
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
    @DisplayName("R8-P0-1：已知缺失表 9 张均在 excludes 中")
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
            .as("以下表未在 tenant.excludes 中：%s", notFound)
            .isEmpty();
    }
}
