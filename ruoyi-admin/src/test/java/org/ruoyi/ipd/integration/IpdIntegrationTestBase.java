package org.ruoyi.ipd.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
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
 * IPD 真活集成测试基类（病根 1 根治：测试金字塔倒置）。
 *
 * <p>2026-09-08 按「病根1-基类真跑验证-20260908.md」方向 B 迁入 ruoyi-admin test 域：
 * RuoYiAIApplication 在本模块（org.ruoyi 包根），测试类包 org.ruoyi.ipd.integration
 * 可向上搜到 @SpringBootConfiguration，全量 bean 接线与 16039 运行态同构；
 * ruoyi-ipd（库模块）test 域保持纯 Mockito 契约测试。
 *
 * <p>数据源钉死（双实例坑实证：dev profile master 指 3306 Docker ruoyi-ai 库，
 * 真库是本机原生 13306/ipd_dev）——properties 显式覆盖三键，
 * TestPropertySource 优先级高于一切 profile 文件，杜绝 profile 顺序连错库。
 * 密码走 env 占位 {@code ${IPD_IT_DB_PASSWORD:}}，凭据不入版本库不上命令行：
 * <pre>
 * export IPD_IT_DB_PASSWORD=$(python3 -c "import json;print(json.load(open('.codex/ipd-dev/config/credentials.json'))['mysql_app'])")
 * mvn -o -pl ruoyi-admin -Dtest=HandoverIntegrationTest -Dipd.scope.it.enabled=true test
 * </pre>
 *
 * <p>Redis 走 dev profile 默认 localhost:6379（本机 docker 实例）；
 * 连接池预算（QA-05-P1：max_connections=151，常态 4 实例×20）下本测试容器
 * 追加 1×20 = 100/151，仍在预算内。
 *
 * <p>提供：RANDOM_PORT 真起容器 / MockMvc 真活 HTTP / 真库探测 helper /
 * @Transactional 自动 rollback（零污染）/ 登录 helper。
 *
 * @see org.ruoyi.RuoYiAIApplication
 * @see org.ruoyi.ipd.controller.HandoverController
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.dynamic.datasource.master.url=jdbc:mysql://127.0.0.1:13306/ipd_dev"
            + "?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull"
            + "&useSSL=true&serverTimezone=GMT%2B8&autoReconnect=true"
            + "&rewriteBatchedStatements=true&allowPublicKeyRetrieval=true"
            + "&nullCatalogMeansCurrent=true",
        "spring.datasource.dynamic.datasource.master.username=ipd_app",
        "spring.datasource.dynamic.datasource.master.password=${IPD_IT_DB_PASSWORD:}"
    })
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
