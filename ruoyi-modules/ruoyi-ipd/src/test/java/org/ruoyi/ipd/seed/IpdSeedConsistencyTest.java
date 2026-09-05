package org.ruoyi.ipd.seed;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * P0-8 种子 SQL 一致性断言：33 要素 / 15 否决位 / G1..G5 分布 7/6/5/8/7 / 17 认证模板
 * （防种子文件与 v1 要素文档、v3 BR-IPD-05b 清单漂移；文件即事实，行数即口径）
 */
@Tag("dev")
class IpdSeedConsistencyTest {

    private static final Path SEED = Paths.get("../../docs/script/sql/update/2026-09-05-ipd-p0-seed-elements.sql");

    private String seed() throws IOException {
        assertThat(Files.exists(SEED)).as("种子 SQL 存在: " + SEED.toAbsolutePath()).isTrue();
        return Files.readString(SEED.toAbsolutePath());
    }

    @Test
    @DisplayName("要素总数 = 33，G1..G5 = 7/6/5/8/7")
    void elementCounts() throws IOException {
        String sql = seed();
        Matcher m = Pattern.compile("INSERT INTO gate_review_elements").matcher(sql);
        int total = 0;
        while (m.find()) {
            total++;
        }
        assertThat(total).isEqualTo(33);
        assertThat(countGate(sql, "'G1'")).isEqualTo(7);
        assertThat(countGate(sql, "'G2'")).isEqualTo(6);
        assertThat(countGate(sql, "'G3'")).isEqualTo(5);
        assertThat(countGate(sql, "'G4'")).isEqualTo(8);
        assertThat(countGate(sql, "'G5'")).isEqualTo(7);
    }

    @Test
    @DisplayName("否决位 = 15（按要素表逐项 ❌；原文汇总行 14 与逐项表不一致，以逐项为准）")
    void vetoCount() throws IOException {
        String sql = seed();
        Matcher m = Pattern.compile("INSERT INTO gate_review_elements[^;]*?, '1', [0-9]+, '1',").matcher(sql);
        int veto = 0;
        while (m.find()) {
            veto++;
        }
        assertThat(veto).isEqualTo(15);
    }

    @Test
    @DisplayName("认证模板 = 17 项，覆盖 5 区域（CN/US/EU/SA/AE/IN/KR/JP/AU）")
    void certCount() throws IOException {
        String sql = seed();
        Matcher m = Pattern.compile("INSERT INTO cert_templates").matcher(sql);
        int total = 0;
        while (m.find()) {
            total++;
        }
        assertThat(total).isEqualTo(17);
        for (String cc : new String[]{"'CN'", "'US'", "'EU'", "'SA'", "'AE'", "'IN'", "'KR'", "'JP'", "'AU'"}) {
            assertThat(sql).contains(cc);
        }
        assertThat(sql).contains("SABER/SASO");
        assertThat(sql).contains("GDPR");
    }

    private int countGate(String sql, String gate) {
        Matcher m = Pattern.compile(Pattern.quote(gate) + ", 'G[0-9]-[0-9]+'").matcher(sql);
        int n = 0;
        while (m.find()) {
            n++;
        }
        return n;
    }
}