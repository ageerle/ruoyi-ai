package org.ruoyi.ipd.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * IPD 真活集成测试基类（病根 1 根治：测试金字塔倒置）
 *
 * <p>背景：ruoyi-ipd 模块 102 张 AcceptanceTest 全是 Mockito Mock 契约测试（74%）
 * 或直接调用 handler（如 Api01AcceptanceTest），<b>0 张真活集成测试</b>。
 * commit 写「整链验收」「真活」但实际 Mock 单测，导致看板卡误判翻 done。
 *
 * <p>本基类提供：
 * <ul>
 *   <li>@SpringBootTest(webEnvironment=RANDOM_PORT) 真起 Spring Boot 容器</li>
 *   <li>@AutoConfigureMockMvc 真活 HTTP 端点探测</li>
 *   <li>@ActiveProfiles("ipd-local,dev") 连真库 ipd_dev @ 13306</li>
 *   <li>@Transactional 自动 rollback（零污染）</li>
 *   <li>登录 helper（拿 admin token）</li>
 *   <li>业务表行数探测 helper</li>
 * </ul>
 *
 * <p>用法：
 * <pre>{@code
 * @Tag("dev")
 * class HandoverIntegrationTest extends IpdIntegrationTestBase {
 *     @Test
 *     void 单项目角色移交真活() throws Exception {
 *         String token = loginAsAdmin();
 *         mockMvc.perform(post("/api/v1/handovers")
 *                 .header("Authorization", "Bearer " + token)
 *                 .contentType(MediaType.APPLICATION_JSON)
 *                 .content("{\"fromPersonId\":123,\"toPersonId\":456}"))
 *             .andExpect(status().isOk())
 *             .andExpect(jsonPath("$.code").value(0));
 *         assertThat(tableRowCount("handover_records")).isGreaterThan(0);
 *     }
 * }
 * }</pre>
 *
 * <p>边界：
 * <ul>
 *   <li>本基类不动现有 Mockito Mock 单测（保留为 *ContractTest 契约测试）</li>
 *   <li>翻 done 硬门禁要求：必须有 *IntegrationTest 真活证据（见 scripts/check-done-gate.py）</li>
 *   <li>真库 ipd_dev @ 13306（docker ruoyi-ai-mysql），凭证走 .codex/ipd-dev/config/application-ipd-local.yml</li>
 * </ul>
 *
 * @see scripts/check-done-gate.py 翻 done 硬门禁脚本
 * @see docs/ipd-系统说明/验收/全局病根除-20260908.md 病根除报告
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles({"ipd-local", "dev"})
@Transactional
@Tag("dev")
public abstract class IpdIntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected DataSource dataSource;

    /** 默认 admin 账号（按 memory「ruoyi-ai 后端真活实例端口表」） */
    protected static final String DEFAULT_ADMIN_USERNAME = "ipd-admin";
    protected static final String DEFAULT_ADMIN_PASSWORD = "Ipd@123456";

    @BeforeEach
    void setUpIntegration() {
        // 子类可 override 做额外 setup
    }

    /**
     * 登录拿 admin token（真活 HTTP 端点）
     *
     * @return Bearer token（不含 "Bearer " 前缀）
     */
    protected String loginAsAdmin() throws Exception {
        return login(DEFAULT_ADMIN_USERNAME, DEFAULT_ADMIN_PASSWORD);
    }

    /**
     * 登录拿 token（真活 HTTP 端点）
     *
     * @param username 用户名
     * @param password 密码
     * @return Bearer token（不含 "Bearer " 前缀）
     */
    protected String login(String username, String password) throws Exception {
        Map<String, String> body = Map.of("username", username, "password", password);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.token").exists())
            .andReturn();

        String json = result.getResponse().getContentAsString();
        Map<String, Object> resp = objectMapper.readValue(json, Map.class);
        Map<String, Object> data = (Map<String, Object>) resp.get("data");
        return (String) data.get("token");
    }

    /**
     * 探测业务表行数（真库 SELECT COUNT(*)）
     *
     * @param tableName 表名
     * @return 行数
     */
    protected int tableRowCount(String tableName) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + tableName)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * 探测业务表是否有特定行（真库 SELECT COUNT(*) WHERE）
     *
     * @param tableName 表名
     * @param whereClause WHERE 子句（不含 "WHERE"）
     * @return 行数
     */
    protected int tableRowCountWhere(String tableName, String whereClause) throws Exception {
        String sql = "SELECT COUNT(*) FROM " + tableName + " WHERE " + whereClause;
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        }
    }

    /**
     * 执行真库 SQL（INSERT/UPDATE/DELETE，@Transactional 自动 rollback）
     *
     * @param sql SQL 语句
     * @return 影响行数
     */
    protected int executeSql(String sql) throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            return stmt.executeUpdate(sql);
        }
    }

    /**
     * 探测 HTTP 端点是否真活（GET 200）
     *
     * @param endpoint 端点路径（如 /api/v1/handovers）
     * @param token    Bearer token
     * @return true 如果 HTTP 200 + code=0
     */
    protected boolean isEndpointAlive(String endpoint, String token) {
        try {
            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(endpoint)
                    .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
