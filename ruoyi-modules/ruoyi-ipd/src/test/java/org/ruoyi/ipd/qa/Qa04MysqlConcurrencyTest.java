package org.ruoyi.ipd.qa;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.sql.DataSource;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QA-04 验收线 3：隔离库 ipd_qa04 真实 MySQL 并发约束验证（卡面 3a/3b/3c）。
 *
 * 门控：-Dipd.scope.mysql.enabled=true -Dipd.scope.mysql.driverJar=<mysql-connector-j jar>
 * 口令：-Dqa04.db.password=<qa04_runner 口令>（或环境变量 QA04_DB_PASSWORD；缺省则本类全部跳过）
 * 隔离库由 root 预先导入 29 张 IPD DDL（含 p143 version 列与 perf01 唯一键），测试只读写 ipd_qa04。
 * 夹具一律随机大 ID，测试结束定向清理；不触碰 ipd_dev 与运行中的应用实例。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "ipd.scope.mysql.enabled", matches = "true")
class Qa04MysqlConcurrencyTest {

    private static final String URL = "jdbc:mysql://127.0.0.1:13306/ipd_qa04";
    private static final String USER = "qa04_runner";
    private static final SecureRandom RND = new SecureRandom();
    private static final List<Long> FIXTURE_IDS = new ArrayList<>();

    private static URLClassLoader driverLoader;
    private static Driver driver;

    private static synchronized Driver driver() throws Exception {
        if (driver == null) {
            String jar = System.getProperty("ipd.scope.mysql.driverJar");
            org.assertj.core.api.Assertions.assertThat(jar)
                .as("-Dipd.scope.mysql.driverJar 未指定").isNotBlank();
            driverLoader = new URLClassLoader(new java.net.URL[]{Path.of(jar).toUri().toURL()},
                Qa04MysqlConcurrencyTest.class.getClassLoader());
            driver = (Driver) driverLoader.loadClass("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        }
        return driver;
    }

    private static Connection connect() throws Exception {
        final String pwd = System.getProperty("qa04.db.password",
            System.getenv("QA04_DB_PASSWORD") == null ? "" : System.getenv("QA04_DB_PASSWORD"));
        org.assertj.core.api.Assertions.assertThat(pwd)
            .as("qa04_runner 口令未提供（-Dqa04.db.password 或 QA04_DB_PASSWORD）").isNotBlank();
        java.util.Properties props = new java.util.Properties();
        props.setProperty("user", USER);
        props.setProperty("password", pwd);
        return driver().connect(URL, props);
    }

    private static long nextId() {
        long id = 9_100_000_000_000_000L + Math.floorMod(RND.nextLong(), 400_000_000_000_000L);
        FIXTURE_IDS.add(id);
        return id;
    }

    private static int exec(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            return ps.executeUpdate();
        }
    }

    private static long scalar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    @AfterAll
    static void cleanupFixtures() throws Exception {
        if (FIXTURE_IDS.isEmpty()) {
            return;
        }
        try (Connection c = connect()) {
            for (Long id : FIXTURE_IDS) {
                exec(c, "DELETE FROM projects WHERE id=?", id);
                exec(c, "DELETE FROM stage_actions WHERE id=?", id);
                exec(c, "DELETE FROM project_stages WHERE id=?", id);
                exec(c, "DELETE FROM audit_logs WHERE entity_id=?", id);
                exec(c, "DELETE FROM products WHERE id=?", id);
                exec(c, "DELETE FROM deliverables WHERE id=?", id);
            }
        }
        if (driverLoader != null) {
            driverLoader.close();
        }
    }

    // ------------------------------------------------------------------
    // 3a. 唯一约束并发：uk_projects_code 两线程同 code 并发插入 → 一胜一 1062；
    //     失败方换号重试成功（对齐 ProjectService.create 的 CODE_CONFLICT_MAX_RETRY 设计）
    // ------------------------------------------------------------------

    private static final String INSERT_PROJECT =
        "INSERT INTO projects (id, code, name, product_id, template_type, level, current_stage, source, status) "
            + "VALUES (?, ?, 'QA-04 并发夹具', ?, 'HARDWARE', 'A', 'CONCEPT', 'NEW', 'DRAFT')";

    /** 插入成功返回 null；撞唯一键返回 "DUPLICATE"；其他异常原样抛出。 */
    private static String tryInsertProject(Connection c, long id, String code, long productId) throws Exception {
        try {
            exec(c, INSERT_PROJECT, id, code, productId);
            FIXTURE_IDS.add(id);
            return null;
        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                return "DUPLICATE";
            }
            throw ex;
        }
    }

    private void seedProduct(Connection c, long productId) throws Exception {
        exec(c, "INSERT INTO products (id, product_name, source, status) VALUES (?, 'QA-04 产品', 'PM_NEW', 'ACTIVE')", productId);
        FIXTURE_IDS.add(productId);
    }

    @Test
    void duplicateCodeRaceProducesExactlyOneWinnerAndLoserRetrySucceeds() throws Exception {
        try (Connection c1 = connect(); Connection c2 = connect()) {
            c1.setAutoCommit(true);
            c2.setAutoCommit(true);
            for (int round = 0; round < 5; round++) {
                // 每个 racer 独立 product：uk_projects_product(product_id) 只应约束 1:1 绑定，
                // 不能让败者的换号重试撞上产品唯一键（服务层真实场景=不同产品抢同一编码段）
                long productA = nextId();
                long productB = nextId();
                seedProduct(c1, productA);
                seedProduct(c2, productB);
                String code = "PRJ-QA04-" + RND.nextInt(1_000_000);
                long idA = nextId();
                long idB = nextId();
                CountDownLatch start = new CountDownLatch(1);
                AtomicInteger outcomeA = new AtomicInteger(-1); // 0=成功 1062=冲突
                AtomicInteger outcomeB = new AtomicInteger(-1);
                Thread a = new Thread(() -> {
                    try {
                        start.await();
                        outcomeA.set(outcomeOf(tryInsertProject(c1, idA, code, productA)));
                    } catch (Exception e) {
                        outcomeA.set(-2);
                    }
                });
                Thread b = new Thread(() -> {
                    try {
                        start.await();
                        outcomeB.set(outcomeOf(tryInsertProject(c2, idB, code, productB)));
                    } catch (Exception e) {
                        outcomeB.set(-2);
                    }
                });
                a.start();
                b.start();
                start.countDown();
                a.join(10_000);
                b.join(10_000);
                boolean oneWinner = (outcomeA.get() == 0 && outcomeB.get() == 1062)
                    || (outcomeA.get() == 1062 && outcomeB.get() == 0);
                assertThat(oneWinner)
                    .as("round %d: A=%d B=%d，同 code 并发必须一胜一 1062", round, outcomeA.get(), outcomeB.get())
                    .isTrue();
                // 败者换号重试（服务层 CODE_CONFLICT_MAX_RETRY 的 SQL 层等价物）：
                // 同 product 重试会撞 uk_projects_product，必须用败者自己的 product
                if (outcomeA.get() == 1062) {
                    assertThat(tryInsertProject(c1, idA, code + "R", productA)).isNull();
                }
                if (outcomeB.get() == 1062) {
                    assertThat(tryInsertProject(c2, idB, code + "R", productB)).isNull();
                }
            }
        }
    }

    private static int outcomeOf(String result) {
        if (result == null) {
            return 0;
        }
        return "DUPLICATE".equals(result) ? 1062 : -2;
    }

    // ------------------------------------------------------------------
    // 3b. 乐观锁并发：stage_actions @Version，同 id 并发 transit 语义 → 仅 1 成功。
    //     用与生产一致的 MyBatis-Plus 映射层（OptimisticLockerInnerInterceptor）真实执行。
    // ------------------------------------------------------------------

    private static SqlSessionFactory mpFactory(DataSource ds) {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(NoLoggingImpl.class);
        GlobalConfig global = new GlobalConfig().setBanner(false)
            .setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.ASSIGN_ID));
        GlobalConfigUtils.setGlobalConfig(configuration, global);
        configuration.setEnvironment(new Environment("qa04-optimistic", new JdbcTransactionFactory(), ds));
        com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor interceptor =
            new com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor());
        configuration.addInterceptor(interceptor);
        configuration.addMapper(StageActionMapper.class);
        configuration.addMapper(DeliverableMapper.class);
        return new MybatisSqlSessionFactoryBuilder().build(configuration);
    }

    private long seedActionGraph(Connection c, String depth) throws Exception {
        long projectId = nextId();
        long stageId = nextId();
        long actionId = nextId();
        exec(c, "INSERT INTO projects (id, code, name, product_id, template_type, level, current_stage, source, status) "
            + "VALUES (?, ?, 'QA-04 乐观锁夹具', ?, 'HARDWARE', 'A', 'CONCEPT', 'NEW', 'DRAFT')", projectId, "PRJ-QA04-OL" + RND.nextInt(1_000_000), nextId());
        exec(c, "INSERT INTO project_stages (id, project_id, stage_code, sort_order, status) VALUES (?, ?, 'CONCEPT', 10, 'ACTIVE')", stageId, projectId);
        exec(c, "INSERT INTO stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, version) "
                + "VALUES (?, ?, ?, 'C01', '市场调研', 'MARKET_PM', ?, 'NOT_STARTED', 0)", actionId, projectId, stageId, depth);
        return actionId;
    }

    @Test
    void concurrentVersionUpdateOnlyOneWins() throws Exception {
        try (Connection c = connect()) {
            c.setAutoCommit(true);
            long actionId = seedActionGraph(c, "DEEP");
            // 驱动由外部 jar 加载，DriverManager 对 child classloader 不可见——
            // 覆写 getConnection 直接 driver.connect，绕开 DriverManager
            UnpooledDataSource ds = new UnpooledDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    try {
                        return connect();
                    } catch (SQLException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SQLException(e);
                    }
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    return getConnection();
                }
            };
            SqlSessionFactory factory = mpFactory(ds);

            CountDownLatch bothLoaded = new CountDownLatch(2);
            AtomicInteger rowsA = new AtomicInteger(-1);
            AtomicInteger rowsB = new AtomicInteger(-1);
            Thread t1 = new Thread(() -> runTransitRace(factory, actionId, "IN_PROGRESS", bothLoaded, rowsA));
            Thread t2 = new Thread(() -> runTransitRace(factory, actionId, "DELAYED", bothLoaded, rowsB));
            t1.start();
            t2.start();
            t1.join(15_000);
            t2.join(15_000);

            assertThat((rowsA.get() == 1 ? 1 : 0) + (rowsB.get() == 1 ? 1 : 0))
                .as("并发同 id 双更新成功线程数（A=%d B=%d）必须为 1", rowsA.get(), rowsB.get())
                .isEqualTo(1);
            try (SqlSession check = factory.openSession(true)) {
                StageAction after = check.getMapper(StageActionMapper.class).selectById(actionId);
                assertThat(after.getVersion()).as("版本号只前进 1").isEqualTo(1);
                assertThat(List.of("IN_PROGRESS", "DELAYED")).contains(after.getStatus());
            }
        }
    }

    private void runTransitRace(SqlSessionFactory factory, long actionId, String target,
                                CountDownLatch bothLoaded, AtomicInteger rowsOut) {
        try (SqlSession session = factory.openSession(true)) {
            StageActionMapper mapper = session.getMapper(StageActionMapper.class);
            StageAction loaded = mapper.selectById(actionId);
            assertThat(loaded).isNotNull();
            bothLoaded.countDown();
            bothLoaded.await();
            loaded.setStatus(target);
            rowsOut.set(mapper.updateById(loaded));
        } catch (Exception e) {
            rowsOut.set(-2);
        }
    }

    // ------------------------------------------------------------------
    // 3c. 事务回滚：业务插入 + 审计写入失败（非法 JSON 进 after_data JSON 列）→ 全回滚零残留。
    //     对齐 ProjectBootstrapService/StageActionService「业务+审计同事务」的事务边界。
    // ------------------------------------------------------------------

    @Test
    void auditWriteFailureRollsBackBusinessInsertWithZeroResidue() throws Exception {
        String brokenJson = "{\"actionCode\":\"C01\",\"status\":";
        long projectId = nextId();
        long auditEntityId = nextId();
        try (Connection business = connect(); Connection verifier = connect()) {
            business.setAutoCommit(false);
            exec(business, INSERT_PROJECT, projectId, "PRJ-QA04-RB" + RND.nextInt(1_000_000), nextId());
            int auditRows;
            try {
                exec(business, "INSERT INTO audit_logs (id, operator_name, operator_role, action, entity_type, "
                        + "entity_id, before_data, after_data, reason, prev_hash, curr_hash) "
                        + "VALUES (?, 'qa04', 'PM', 'CREATE', 'PROJECT', ?, '{\"n\":1}', ?, '回归', ?, ?)",
                    nextId(), auditEntityId, brokenJson, "0".repeat(64), "f".repeat(64));
                auditRows = 1; // 不应到达：JSON 列拒绝非法文本（严格 SQL 模式）
            } catch (SQLException auditFailure) {
                auditRows = 0;
            }
            assertThat(auditRows).as("前置条件：非法 JSON 必须被 audit_logs.after_data 拒绝").isEqualTo(0);
            business.rollback();

            assertThat(scalar(verifier, "SELECT COUNT(*) FROM projects WHERE id=?", projectId))
                .as("业务插入必须随审计失败回滚").isZero();
            assertThat(scalar(verifier, "SELECT COUNT(*) FROM audit_logs WHERE entity_id=?", auditEntityId))
                .as("审计零残留").isZero();

            // 阳性对照：合法 JSON + 提交 → 两表都可见（证明上述零残留不是连接/权限伪象）
            long okProjectId = nextId();
            exec(verifier, "SET autocommit=0");
            exec(verifier, INSERT_PROJECT, okProjectId, "PRJ-QA04-OK" + RND.nextInt(1_000_000), nextId());
            exec(verifier, "INSERT INTO audit_logs (id, operator_name, operator_role, action, entity_type, "
                    + "entity_id, before_data, after_data, reason, prev_hash, curr_hash) "
                    + "VALUES (?, 'qa04', 'PM', 'CREATE', 'PROJECT', ?, '{\"n\":1}', '{\"n\":2}', '回归', ?, ?)",
                nextId(), okProjectId, "0".repeat(64), "f".repeat(64));
            exec(verifier, "COMMIT");
            assertThat(scalar(verifier, "SELECT COUNT(*) FROM projects WHERE id=?", okProjectId)).isEqualTo(1);
            assertThat(scalar(verifier, "SELECT COUNT(*) FROM audit_logs WHERE entity_id=?", okProjectId)).isEqualTo(1);
            FIXTURE_IDS.add(okProjectId);
        }
    }

    // ------------------------------------------------------------------
    // 验收线 2 真库正反例：软删行不出现在带 del_flag 过滤的查询（BR-IPD-03 查询链）。
    // ------------------------------------------------------------------

    @Test
    void softDeletedDeliverableExcludedFromFilteredCountAndList() throws Exception {
        try (Connection c = connect()) {
            c.setAutoCommit(true);
            long projectId = nextId();
            long stageId = nextId();
            long actionId = nextId();
            long keepId = nextId();
            long goneId = nextId();
            exec(c, "INSERT INTO projects (id, code, name, product_id, template_type, level, current_stage, source, status) "
                + "VALUES (?, ?, 'QA-04 软删正反例', ?, 'HARDWARE', 'A', 'CONCEPT', 'NEW', 'DRAFT')", projectId, "PRJ-QA04-SD" + RND.nextInt(1_000_000), nextId());
            exec(c, "INSERT INTO project_stages (id, project_id, stage_code, sort_order, status) VALUES (?, ?, 'CONCEPT', 10, 'ACTIVE')", stageId, projectId);
            exec(c, "INSERT INTO stage_actions (id, project_id, stage_id, action_code, action_name, owner_role, depth, status, version) "
                + "VALUES (?, ?, ?, 'C01', '市场调研', 'MARKET_PM', 'DEEP', 'IN_PROGRESS', 0)", actionId, projectId, stageId);
            exec(c, "INSERT INTO deliverables (id, action_id, project_id, file_name, del_flag) "
                + "VALUES (?, ?, ?, 'keep.pdf', '0')", keepId, actionId, projectId);
            exec(c, "INSERT INTO deliverables (id, action_id, project_id, file_name, del_flag) "
                + "VALUES (?, ?, ?, 'gone.pdf', '1')", goneId, actionId, projectId);

            UnpooledDataSource ds = new UnpooledDataSource() {
                @Override
                public Connection getConnection() throws SQLException {
                    try {
                        return connect();
                    } catch (SQLException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new SQLException(e);
                    }
                }

                @Override
                public Connection getConnection(String username, String password) throws SQLException {
                    return getConnection();
                }
            };
            SqlSessionFactory factory = mpFactory(ds);
            try (SqlSession session = factory.openSession(true)) {
                DeliverableMapper mapper = session.getMapper(DeliverableMapper.class);
                long visible = mapper.selectCount(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Deliverable>()
                        .eq(Deliverable::getActionId, actionId)
                        .eq(Deliverable::getDelFlag, "0"));
                assertThat(visible).as("del_flag='0' 过滤计数只应看到未删交付物").isEqualTo(1L);
                java.util.List<Deliverable> rows = mapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Deliverable>()
                        .eq(Deliverable::getActionId, actionId));
                assertThat(rows).as("无过滤列表包含两行（正反例对照）").hasSize(2);
            }
        }
    }
}
