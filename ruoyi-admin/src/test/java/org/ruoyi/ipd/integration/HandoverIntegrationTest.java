package org.ruoyi.ipd.integration;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-7.1 单项目角色移交 —— 真活集成测试（病根 1 根治验证，2026-09-08 解锁）。
 *
 * <p>2026-09-08 按「病根1-基类真跑验证-20260908.md」方向 B 迁入 ruoyi-admin test 域
 * 并解锁原 @Disabled：test classpath 可达 RuoYiAIApplication（@SpringBootConfiguration
 * 障碍消除）、datasource 三键由基类 properties 钉死 13306/ipd_dev、
 * 密码走 env {@code IPD_IT_DB_PASSWORD}、Redis 走 dev profile 默认 localhost:6379。
 *
 * <p>门控：真活依赖本机 MySQL 13306 + Redis 6379 运行态，CI/兄弟会话默认
 * {@code mvn test} 显示为 skipped（不红）；本机真跑：
 * <pre>
 * export IPD_IT_DB_PASSWORD=$(python3 -c "import json;print(json.load(open('.codex/ipd-dev/config/credentials.json'))['mysql_app'])")
 * mvn -o -pl ruoyi-admin -Dtest=HandoverIntegrationTest -Dipd.scope.it.enabled=true test
 * </pre>
 *
 * <p>验证 {@link IpdIntegrationTestBase} 真起 Spring Boot 容器 + 连真库
 * ipd_dev @ 13306 + MockMvc 真活探测 {@code /api/v1/handovers} 端点。
 * 与 Mockito Mock 契约测试（如 P271AcceptanceTest）的区别：真容器、真 HTTP、
 * 真库、@Transactional 自动 rollback 零污染。
 *
 * <p>契约修正：基类 javadoc 示例用的 {@code {"fromPersonId":123,"toPersonId":456}} 与真实
 * {@code HandoverController.InitiateRequest(projectId, role, toPersonId, note, approvalRef, onBehalf)}
 * 不符——fromPersonId 由会话 actor 推导，非入参。本测试以真实契约为准。
 *
 * @see IpdIntegrationTestBase
 * @see org.ruoyi.ipd.controller.HandoverController
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "ipd.scope.it.enabled", matches = "true")
@DisplayName("P2-7.1 移交真活集成测试（验证 IpdIntegrationTestBase 能真跑）")
class HandoverIntegrationTest extends IpdIntegrationTestBase {

    @Test
    @DisplayName("0) Spring 容器真起：mockMvc / dataSource 注入非空")
    void contextBootsWithRealBeans() {
        assertThat(mockMvc).as("MockMvc 应被 @AutoConfigureMockMvc 注入").isNotNull();
        assertThat(objectMapper).as("ObjectMapper 应被容器注入").isNotNull();
        assertThat(dataSource).as("DataSource 应被容器注入（连真库）").isNotNull();
    }

    @Test
    @DisplayName("1) admin 真登录拿 token：POST /api/v1/auth/login → 200 code=0 data.token")
    void adminLoginReturnsRealToken() throws Exception {
        String token = loginAsAdmin();
        assertThat(token).as("登录应返回非空 Bearer token").isNotBlank();
    }

    @Test
    @DisplayName("2) 移交收件箱真活：GET /api/v1/handovers/inbox → 200 code=0")
    void handoverInboxIsAlive() throws Exception {
        String token = loginAsAdmin();
        mockMvc.perform(get("/api/v1/handovers/inbox")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("3) 真库可达：handover_records 表 SELECT COUNT(*) 不抛异常")
    void handoverRecordsTableIsReachable() throws Exception {
        int rows = tableRowCount("handover_records");
        assertThat(rows).as("handover_records 真库行数应 >= 0（表可达）").isGreaterThanOrEqualTo(0);
    }
}
