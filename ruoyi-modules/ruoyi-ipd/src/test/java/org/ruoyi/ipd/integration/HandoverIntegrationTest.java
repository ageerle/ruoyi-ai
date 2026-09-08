package org.ruoyi.ipd.integration;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * P2-7.1 单项目角色移交 —— 真活集成测试示例（病根 1 根治验证）。
 *
 * <p>目的：验证 {@link IpdIntegrationTestBase} 能真起 Spring Boot 容器 + 连真库
 * ipd_dev @ 13306 + MockMvc 真活探测 {@code /api/v1/handovers} 端点。
 *
 * <p>与 Mockito Mock 契约测试（如 P271AcceptanceTest）的区别：
 * <ul>
 *   <li>本测试 {@code @SpringBootTest(webEnvironment=RANDOM_PORT)} 真起容器</li>
 *   <li>真发 HTTP 请求过 {@code IpdAuthController#login} + {@code HandoverController#inbox}</li>
 *   <li>真连 ipd_dev 库做 {@code SELECT COUNT(*)}</li>
 *   <li>{@code @Transactional} 自动 rollback，零污染</li>
 * </ul>
 *
 * <p>契约修正：基类 javadoc 示例用的 {@code {"fromPersonId":123,"toPersonId":456}} 与真实
 * {@code HandoverController.InitiateRequest(projectId, role, toPersonId, note, approvalRef, onBehalf)}
 * 不符——fromPersonId 由会话 actor 推导，非入参。本测试以真实契约为准。
 *
 * @see IpdIntegrationTestBase
 * @see org.ruoyi.ipd.controller.HandoverController
 */
@Tag("dev")
@Disabled("""
    需先补 ruoyi-ipd test 域真活基建才能跑（2026-09-08 实证：mvn -o -pl ruoyi-modules/ruoyi-ipd \
    -Dtest=HandoverIntegrationTest test → IllegalStateException: Unable to find a @SpringBootConfiguration）。
    缺三样：① ruoyi-ipd test 域可达的 @SpringBootConfiguration（主 app RuoYiAIApplication 在 ruoyi-admin，非 test 依赖）；
    ② test resources application.yml（datasource 指真库 13306/ipd_dev + Redis + Sa-Token，凭据不入版本库）；
    ③ Redis/真库运行态。补齐后删本 @Disabled 即真跑。见 docs/ipd-系统说明/验收/病根1-基类真跑验证-20260908.md。
    """)
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
