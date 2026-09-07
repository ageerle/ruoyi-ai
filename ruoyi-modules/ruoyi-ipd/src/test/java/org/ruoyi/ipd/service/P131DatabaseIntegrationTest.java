package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.mapping.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.transaction.SpringManagedTransactionFactory;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.domain.ActionDef;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.domain.ProjectStage;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.support.NoopTransactionManager;
import org.ruoyi.ipd.mapper.ProjectStageMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.mapper.KpiRecordMapper;
import org.ruoyi.ipd.seed.ActionCatalog;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * P1-3.1：真实 MyBatis-Spring Mapper、MySQL 与业务服务的 Spring 事务代理集成。
 * 显式启用，只使用受限本机账号；随机正 ID 不消耗数据库自增序列，所有夹具最终回滚。
 * 保存点用例证明事务内已有夹具恢复及新图撤回，不代表已提交数据恢复、完整应用启动或真实审计链。
 */
@Tag("dev")
@EnabledIfSystemProperty(named = "ipd.scope.mysql.enabled", matches = "true")
class P131DatabaseIntegrationTest {
    private static final Set<String> FIXTURE_TABLES = Set.of("products", "projects", "project_stages", "stage_actions");
    private static final long OPERATOR = 7L;
    private final Map<String, List<Long>> created = new LinkedHashMap<>();
    private long nextId = 8_000_000_000_000_000L + Math.floorMod(new SecureRandom().nextLong(), 900_000_000_000_000L);
    private Map<String, String> client;
    private URLClassLoader driverLoader;
    private Driver driver;
    private Connection connection;
    private MybatisConfiguration configuration;
    private DataSourceTransactionManager transactions;
    private ProjectMapper projects;
    private ProductMapper products;
    private KpiRecordMapper kpis;
    private ProjectStageMapper stages;
    private StageActionMapper actions;
    private ProjectBootstrapService bootstrap;

    @BeforeEach
    void prepareRealMybatisAndSpringTransactions() throws Exception {
        String configPath = System.getProperty("ipd.scope.mysql.clientConfig");
        String driverPath = System.getProperty("ipd.scope.mysql.driverJar");
        assertThat(configPath).as("显式指定 mysql-app.cnf，不接受生产配置").isNotBlank();
        assertThat(driverPath).as("只使用已安装 JDBC driver，不安装依赖").isNotBlank();
        client = new HashMap<>();
        for (String line : Files.readAllLines(Path.of(configPath))) {
            int equals = line.indexOf('=');
            if (equals > 0) client.put(line.substring(0, equals).trim(), line.substring(equals + 1).trim());
        }
        assertThat(client.get("host")).isEqualTo("127.0.0.1");
        assertThat(client.get("port")).isEqualTo("13306");
        assertThat(client.get("user")).isEqualTo("ipd_app");
        driverLoader = new URLClassLoader(new java.net.URL[]{Path.of(driverPath).toUri().toURL()}, getClass().getClassLoader());
        driver = (Driver) driverLoader.loadClass("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
        connection = connect();
        connection.setAutoCommit(false);
        SingleConnectionDataSource dataSource = new SingleConnectionDataSource(connection, true);
        transactions = new DataSourceTransactionManager(dataSource);
        transactions.setNestedTransactionAllowed(true);
        configuration = new MybatisConfiguration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setLogImpl(NoLoggingImpl.class);
        configuration.setEnvironment(new Environment("p131-local-rollback", new SpringManagedTransactionFactory(), dataSource));
        GlobalConfigUtils.setGlobalConfig(configuration, new GlobalConfig().setBanner(false)
            .setDbConfig(new GlobalConfig.DbConfig().setIdType(IdType.ASSIGN_ID))
            .setIdentifierGenerator(entity -> {
                if (entity instanceof ProjectStage) return id("project_stages");
                if (entity instanceof StageAction) return id("stage_actions");
                if (entity instanceof Project) return id("projects");
                if (entity instanceof Product) return id("products");
                throw new IllegalArgumentException("Unexpected fixture entity " + entity.getClass().getName());
            }));
        configuration.addMapper(ProjectMapper.class);
        configuration.addMapper(ProductMapper.class);
        configuration.addMapper(ProjectStageMapper.class);
        configuration.addMapper(StageActionMapper.class);
        configuration.addMapper(KpiRecordMapper.class);
        SqlSessionTemplate sql = new SqlSessionTemplate(new MybatisSqlSessionFactoryBuilder().build(configuration));
        projects = sql.getMapper(ProjectMapper.class);
        products = sql.getMapper(ProductMapper.class);
        kpis = sql.getMapper(KpiRecordMapper.class);
        stages = sql.getMapper(ProjectStageMapper.class);
        actions = sql.getMapper(StageActionMapper.class);
        bootstrap = proxy(new ProjectBootstrapService(stages, actions));
    }

    @AfterEach
    void rollbackAndVerifyNoGeneratedFixtureRemainsOnIndependentConnection() throws Exception {
        try {
            try {
                if (connection != null) connection.rollback();
            } finally {
                if (connection != null) connection.close();
            }
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
            if (driver != null && !created.isEmpty()) {
                try (Connection verifier = connect()) {
                    for (var entry : created.entrySet()) {
                        assertThat(FIXTURE_TABLES).contains(entry.getKey());
                        String placeholders = String.join(",", java.util.Collections.nCopies(entry.getValue().size(), "?"));
                        // 表名为本测试固定白名单，ID为本测试分配的随机正数；不读取或修改其他业务行。
                        try (PreparedStatement statement = verifier.prepareStatement("SELECT COUNT(*) FROM " + entry.getKey() + " WHERE id IN (" + placeholders + ")")) {
                            for (int i = 0; i < entry.getValue().size(); i++) statement.setLong(i + 1, entry.getValue().get(i));
                            try (ResultSet rows = statement.executeQuery()) {
                                rows.next();
                                assertThat(rows.getInt(1)).as("独立连接确认夹具零残留: " + entry.getKey()).isZero();
                            }
                        }
                    }
                }
            }
        } finally {
            if (driverLoader != null) driverLoader.close();
        }
    }

    @Test
    void hardwareBootstrapPersistsSixStagesAndSixtyNineUniqueActionsWithCorrectParents() {
        withRollback(() -> {
            long project = project("HARDWARE");
            assertThat(bootstrap.bootstrap(project, OPERATOR)).isEqualTo(6);
            assertCompleteGraph(project, "HARDWARE");
        });
    }

    @Test
    void softwareAndSolutionKeepAllActionsAndOnlySolutionPromotesV11ToDeep() {
        withRollback(() -> {
            for (String template : List.of("SOFTWARE", "SOLUTION")) {
                long project = project(template);
                assertThat(bootstrap.bootstrap(project, OPERATOR)).isEqualTo(6);
                assertCompleteGraph(project, template);
            }
        });
    }

    @Test
    void secondBootstrapReturnsZeroWithoutChangingAnyExistingGraphRow() {
        withRollback(() -> {
            long project = project("SOLUTION");
            assertThat(bootstrap.bootstrap(project, OPERATOR)).isEqualTo(6);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertThat(bootstrap.bootstrap(project, OPERATOR)).isZero();
            assertThat(graphSnapshot(project)).isEqualTo(before);
            assertCompleteGraph(project, "SOLUTION");
        });
    }

    @Test
    void partialStageGraphIsRejectedWithoutFillingOrReplacingRows() {
        withRollback(() -> {
            long project = project("HARDWARE");
            long stage = id("project_stages");
            update("INSERT INTO project_stages (id,project_id,stage_code,sort_order,tenant_id,del_flag) VALUES (?,?,'CONCEPT',10,'000000','0')", stage, project);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(project, OPERATOR)), ApiV1ErrorCode.STATE_CONFLICT);
            assertThat(graphSnapshot(project)).isEqualTo(before);
        });
    }

    @Test
    void fullCountsWithWrongActionStageAreRejectedRatherThanAcceptedAsComplete() {
        withRollback(() -> {
            long project = project("HARDWARE");
            bootstrap.bootstrap(project, OPERATOR);
            long wrongStage = number("SELECT id FROM project_stages WHERE project_id=? AND stage_code='PLAN'", project);
            update("UPDATE stage_actions SET stage_id=? WHERE project_id=? AND action_code='C01'", wrongStage, project);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(project, OPERATOR)), ApiV1ErrorCode.STATE_CONFLICT);
            assertThat(graphSnapshot(project)).isEqualTo(before);
        });
    }

    @Test
    void existingGraphWithWrongTemplateDepthIsRejectedWithoutRepair() {
        withRollback(() -> {
            long project = project("SOLUTION");
            bootstrap.bootstrap(project, OPERATOR);
            update("UPDATE stage_actions SET depth='LIGHT' WHERE project_id=? AND action_code='V11'", project);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(project, OPERATOR)), ApiV1ErrorCode.STATE_CONFLICT);
            assertThat(graphSnapshot(project)).isEqualTo(before);
        });
    }

    @Test
    void completeGraphPlusOrphanActionIsRejectedEvenWhenLiveJoinedQueryWouldHideIt() {
        withRollback(() -> {
            long project = project("HARDWARE");
            bootstrap.bootstrap(project, OPERATOR);
            long orphan = id("stage_actions");
            update("INSERT INTO stage_actions (id,project_id,stage_id,action_code,action_name,owner_role,depth,tenant_id,del_flag) VALUES (?,?,?,'X99','P131-orphan','MARKET_PM','LIGHT','000000','0')",
                orphan, project, nextId++);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertThat(count("project_stages", project)).isEqualTo(6);
            assertThat(count("stage_actions", project)).isEqualTo(70);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(project, OPERATOR)), ApiV1ErrorCode.STATE_CONFLICT);
            assertThat(graphSnapshot(project)).isEqualTo(before);
        });
    }

    @Test
    void deletedOrForeignGraphRowsAreNotTreatedAsAnEmptyGraph() {
        withRollback(() -> {
            for (String corruption : List.of("DELETED_STAGE", "FOREIGN_STAGE", "DELETED_ACTION", "FOREIGN_ACTION")) {
                long project = project("HARDWARE");
                if (corruption.endsWith("STAGE")) {
                    long stage = id("project_stages");
                    update("INSERT INTO project_stages (id,project_id,stage_code,tenant_id,del_flag) VALUES (?,?,'CONCEPT',?,?)",
                        stage, project, corruption.startsWith("FOREIGN") ? "999999" : "000000",
                        corruption.startsWith("DELETED") ? "1" : "0");
                } else {
                    long action = id("stage_actions");
                    update("INSERT INTO stage_actions (id,project_id,stage_id,action_code,action_name,owner_role,depth,tenant_id,del_flag) VALUES (?,?,?,'C01','P131-hidden','MARKET_PM','DEEP',?,?)",
                        action, project, nextId++, corruption.startsWith("FOREIGN") ? "999999" : "000000",
                        corruption.startsWith("DELETED") ? "1" : "0");
                }
                Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
                assertServiceCode(() -> nested(() -> bootstrap.bootstrap(project, OPERATOR)), ApiV1ErrorCode.STATE_CONFLICT);
                assertThat(graphSnapshot(project)).as(corruption).isEqualTo(before);
            }
        });
    }

    @Test
    void completeExistingGraphPreservesProgressAndNeverResetsBusinessState() {
        withRollback(() -> {
            long project = project("HARDWARE");
            bootstrap.bootstrap(project, OPERATOR);
            update("UPDATE project_stages SET status='IN_PROGRESS' WHERE project_id=? AND stage_code='CONCEPT'", project);
            update("UPDATE stage_actions SET status='DONE' WHERE project_id=? AND action_code='C01'", project);
            Map<String, List<Map<String, Object>>> before = graphSnapshot(project);
            assertThat(bootstrap.bootstrap(project, OPERATOR)).isZero();
            assertThat(graphSnapshot(project)).isEqualTo(before);
        });
    }

    @Test
    void mandatoryProxyRejectsInvocationWithoutSpringTransactionBeforeMapperAccess() {
        ProjectStageMapper stageSpy = spy(stages);
        StageActionMapper actionSpy = spy(actions);
        ProjectBootstrapService mandatory = proxy(new ProjectBootstrapService(stageSpy, actionSpy));
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        assertThatThrownBy(() -> mandatory.bootstrap(nextId++, OPERATOR)).isInstanceOf(IllegalTransactionStateException.class);
        verifyNoInteractions(stageSpy, actionSpy);
    }

    @Test
    void projectLockRejectsMissingDeletedAndForeignTenantParents() {
        withRollback(() -> {
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(null, OPERATOR)), ApiV1ErrorCode.PARAM_INVALID);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(nextId++, OPERATOR)), ApiV1ErrorCode.NOT_FOUND);
            long deleted = project("HARDWARE");
            long foreign = project("HARDWARE");
            update("UPDATE projects SET del_flag='1' WHERE id=?", deleted);
            update("UPDATE projects SET tenant_id='999999' WHERE id=?", foreign);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(deleted, OPERATOR)), ApiV1ErrorCode.NOT_FOUND);
            assertServiceCode(() -> nested(() -> bootstrap.bootstrap(foreign, OPERATOR)), ApiV1ErrorCode.NOT_FOUND);
            assertEmptyGraph(deleted);
            assertEmptyGraph(foreign);
        });
    }

    @Test
    void projectLookupUsesUncachedForUpdateAndReturnsTheDatabaseTemplate() {
        withRollback(() -> {
            long project = project("SOLUTION");
            for (String method : List.of("selectProjectForBootstrap", "selectLiveByProject", "selectActionsForBootstrap",
                "selectAllStageIdsForBootstrap", "selectAllActionIdsForBootstrap")) {
                var statement = configuration.getMappedStatement(ProjectStageMapper.class.getName() + "." + method);
                assertThat(statement.getBoundSql(Map.of("projectId", project)).getSql().strip()).as(method).endsWith("FOR UPDATE");
                assertThat(statement.isUseCache()).as(method).isFalse();
                assertThat(statement.isFlushCacheRequired()).as(method).isTrue();
            }
            assertThat(stages.selectProjectForBootstrap(project).getTemplateType()).isEqualTo("SOLUTION");
            // 夹具未提交；这里只证明实际锁查询/模板回读，不声称对已提交行的并发互斥验收。
        });
    }

    @Test
    void stageInsertFailureRollsBackEarlierStagesAndActionsToTheSavepoint() {
        withRollback(() -> {
            long project = project("HARDWARE");
            ProjectStageMapper failing = spy(stages);
            AtomicInteger inserted = new AtomicInteger();
            IllegalStateException original = new IllegalStateException("P131 forced stage insertion failure");
            doAnswer(call -> {
                if (inserted.get() == 2) {
                    assertThat(count("project_stages", project)).isEqualTo(2);
                    assertThat(count("stage_actions", project)).isGreaterThan(0);
                    throw original;
                }
                int rows = stages.insert((ProjectStage) call.getArgument(0));
                inserted.incrementAndGet();
                return rows;
            }).when(failing).insert(any(ProjectStage.class));
            ProjectBootstrapService target = proxy(new ProjectBootstrapService(failing, actions));
            assertThat(catchThrowable(() -> nested(() -> target.bootstrap(project, OPERATOR)))).isSameAs(original);
            assertThat(inserted.get()).isEqualTo(2);
            assertEmptyGraph(project);
            assertThat(number("SELECT COUNT(*) FROM projects WHERE id=?", project)).isEqualTo(1);
        });
    }

    @Test
    void actionInsertFailureAfterActualSqlRollsBackEveryNewGraphRow() {
        withRollback(() -> {
            long project = project("HARDWARE");
            StageActionMapper failing = spy(actions);
            AtomicInteger inserted = new AtomicInteger();
            IllegalStateException original = new IllegalStateException("P131 forced action failure after real SQL");
            doAnswer(call -> {
                int rows = actions.insert((StageAction) call.getArgument(0));
                if (inserted.incrementAndGet() == 10) {
                    assertThat(count("stage_actions", project)).isEqualTo(10);
                    assertThat(count("project_stages", project)).isGreaterThan(0);
                    throw original;
                }
                return rows;
            }).when(failing).insert(any(StageAction.class));
            ProjectBootstrapService target = proxy(new ProjectBootstrapService(stages, failing));
            assertThat(catchThrowable(() -> nested(() -> target.bootstrap(project, OPERATOR)))).isSameAs(original);
            assertThat(inserted.get()).isEqualTo(10);
            assertEmptyGraph(project);
            assertThat(number("SELECT COUNT(*) FROM projects WHERE id=?", project)).isEqualTo(1);
        });
    }

    @Test
    void incorrectAffectedRowCountAfterRealInsertIsInternalErrorAndRollsBack() {
        withRollback(() -> {
            long project = project("HARDWARE");
            ProjectStageMapper failing = spy(stages);
            doAnswer(call -> {
                assertThat(stages.insert((ProjectStage) call.getArgument(0))).isEqualTo(1);
                assertThat(count("project_stages", project)).isEqualTo(1);
                return 0;
            }).when(failing).insert(any(ProjectStage.class));
            ProjectBootstrapService target = proxy(new ProjectBootstrapService(failing, actions));
            assertServiceCode(() -> nested(() -> target.bootstrap(project, OPERATOR)), ApiV1ErrorCode.INTERNAL_ERROR);
            assertEmptyGraph(project);
        });
    }

    @Test
    void invalidReturnedIdAfterRealInsertIsInternalErrorAndRollsBack() {
        withRollback(() -> {
            long project = project("HARDWARE");
            ProjectStageMapper failing = spy(stages);
            doAnswer(call -> {
                ProjectStage entity = call.getArgument(0);
                int rows = stages.insert(entity);
                assertThat(entity.getId()).isPositive();
                assertThat(count("project_stages", project)).isEqualTo(1);
                entity.setId(0L);
                return rows;
            }).when(failing).insert(any(ProjectStage.class));
            ProjectBootstrapService target = proxy(new ProjectBootstrapService(failing, actions));
            assertServiceCode(() -> nested(() -> target.bootstrap(project, OPERATOR)), ApiV1ErrorCode.INTERNAL_ERROR);
            assertEmptyGraph(project);
        });
    }

    @Test
    void realProjectCreateBindsProductAndBootstrapsCompleteGraphBeforeAudit() {
        withRollback(() -> {
            long product = product();
            Project request = createRequest(product, "SOLUTION");
            AuditLogService audit = mock(AuditLogService.class);
            GateEngine gates = mock(GateEngine.class);
            AtomicBoolean observed = new AtomicBoolean();
            doAnswer(call -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                assertThat(number("SELECT project_id FROM products WHERE id=?", product)).isEqualTo(request.getId());
                assertCompleteGraph(request.getId(), "SOLUTION");
                observed.set(true);
                return null;
            }).when(audit).append(any(AuditLog.class));
            ProjectCertService certs = mock(ProjectCertService.class);
            when(certs.syncFromProject(any(), any())).thenReturn(0);
            ProjectService service = proxy(new ProjectService(projects, products, actions, kpis, audit, gates, bootstrap, certs, NoopTransactionManager.INSTANCE, null /* P2-6.2 */));
            Project result = service.create(request, OPERATOR);
            assertThat(result.getId()).isPositive();
            assertThat(observed.get()).isTrue();
            verify(audit, times(1)).append(any(AuditLog.class));
            verifyNoInteractions(gates);
        });
    }

    @Test
    void createAuditFailureRestoresUncommittedProductAndRollsBackProjectStagesAndActions() {
        withRollback(() -> {
            long product = product();
            Project request = createRequest(product, "HARDWARE");
            assertThat(scalar("SELECT project_id FROM products WHERE id=?", product)).isNull();
            AuditLogService audit = mock(AuditLogService.class);
            GateEngine gates = mock(GateEngine.class);
            AtomicBoolean observed = new AtomicBoolean();
            IllegalStateException original = new IllegalStateException("P131 forced audit failure after complete project graph");
            doAnswer(call -> {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isTrue();
                assertThat(number("SELECT COUNT(*) FROM projects WHERE id=?", request.getId())).isEqualTo(1);
                assertThat(number("SELECT project_id FROM products WHERE id=?", product)).isEqualTo(request.getId());
                assertCompleteGraph(request.getId(), "HARDWARE");
                observed.set(true);
                throw original;
            }).when(audit).append(any(AuditLog.class));
            ProjectCertService certs = mock(ProjectCertService.class);
            when(certs.syncFromProject(any(), any())).thenReturn(0);
            ProjectService service = proxy(new ProjectService(projects, products, actions, kpis, audit, gates, bootstrap, certs, NoopTransactionManager.INSTANCE, null /* P2-6.2 */));
            assertThat(catchThrowable(() -> nested(() -> service.create(request, OPERATOR)))).isSameAs(original);
            assertThat(observed.get()).isTrue();
            assertThat(request.getId()).isPositive();
            assertThat(number("SELECT COUNT(*) FROM products WHERE id=?", product)).isEqualTo(1);
            assertThat(scalar("SELECT project_id FROM products WHERE id=?", product)).isNull();
            assertThat(number("SELECT COUNT(*) FROM projects WHERE id=?", request.getId())).isZero();
            assertEmptyGraph(request.getId());
            verify(audit, times(1)).append(any(AuditLog.class));
            verifyNoInteractions(gates);
            // 产品在外层未提交事务中创建；这里只验证保存点恢复。外层随后回滚，独立连接核对四表零残留。
        });
    }

    private void assertCompleteGraph(long project, String template) throws Exception {
        String markets = (String) scalar("SELECT target_markets FROM projects WHERE id=?", project);
        List<Map<String, Object>> stageRows = rows("SELECT * FROM project_stages WHERE project_id=? ORDER BY sort_order,id", project);
        List<Map<String, Object>> actionRows = rows("SELECT * FROM stage_actions WHERE project_id=? ORDER BY action_code,id", project);
        assertThat(stageRows).hasSize(6);
        assertThat(actionRows).hasSize(69);
        assertThat(stageRows).extracting(row -> row.get("stage_code"))
            .containsExactly("CONCEPT", "PLAN", "DEV", "VALID", "LAUNCH", "LIFECYCLE");
        Set<Long> allIds = new HashSet<>();
        Map<String, Long> stageByCode = new HashMap<>();
        for (int i = 0; i < stageRows.size(); i++) {
            Map<String, Object> row = stageRows.get(i);
            long id = ((Number) row.get("id")).longValue();
            assertThat(id).isPositive();
            assertThat(allIds.add(id)).isTrue();
            stageByCode.put((String) row.get("stage_code"), id);
            assertThat(((Number) row.get("sort_order")).intValue()).isEqualTo((i + 1) * 10);
            assertInitialMetadata(row, project);
        }
        Map<String, ActionDef> definitions = new HashMap<>();
        for (ActionDef def : ActionCatalog.ALL) definitions.put(def.code(), def);
        assertThat(definitions).hasSize(69);
        Set<String> found = new HashSet<>();
        int deep = 0;
        for (Map<String, Object> row : actionRows) {
            long id = ((Number) row.get("id")).longValue();
            assertThat(id).isPositive();
            assertThat(allIds.add(id)).isTrue();
            String code = (String) row.get("action_code");
            assertThat(found.add(code)).isTrue();
            ActionDef expected = definitions.get(code);
            assertThat(expected).as("已登记动作 " + code).isNotNull();
            assertThat(((Number) row.get("stage_id")).longValue()).isEqualTo(stageByCode.get(expected.stage()));
            assertThat(row.get("action_name")).isEqualTo(expected.name());
            assertThat(row.get("owner_role")).isEqualTo(expected.ownerRole());
            String expectedDepth = ActionCatalog.expectedDepth(expected, template);
            assertThat(row.get("depth")).as(code + " 模板深度").isEqualTo(expectedDepth);
            assertThat(row.get("is_blocking")).isEqualTo(expected.blocking() ? "1" : "0");
            assertThat(row.get("is_bio_feature")).isEqualTo(expected.bioFeature() ? "1" : "0");
            boolean applicable = ActionCatalog.applicableTo(expected, template, markets);
            assertThat(row.get("status")).as(code + " 模板适用性").isEqualTo(applicable ? "NOT_STARTED" : "NA");
            assertThat(((Number) row.get("project_id")).longValue()).isEqualTo(project);
            assertThat(row.get("tenant_id")).isEqualTo("000000");
            assertThat(row.get("del_flag")).isEqualTo("0");
            assertThat(((Number) row.get("create_by")).longValue()).isEqualTo(OPERATOR);
            assertThat(((Number) row.get("update_by")).longValue()).isEqualTo(OPERATOR);
            assertThat(row.get("create_time")).isNotNull();
            if ("DEEP".equals(row.get("depth"))) deep++;
        }
        assertThat(found).containsExactlyInAnyOrderElementsOf(definitions.keySet());
        assertThat(deep).isEqualTo("SOLUTION".equals(template) ? 43 : 42);
        assertThat(allIds).hasSize(75);
    }

    private void assertInitialMetadata(Map<String, Object> row, long project) {
        assertThat(((Number) row.get("project_id")).longValue()).isEqualTo(project);
        assertThat(row.get("status")).isEqualTo("NOT_STARTED");
        assertThat(row.get("tenant_id")).isEqualTo("000000");
        assertThat(row.get("del_flag")).isEqualTo("0");
        assertThat(((Number) row.get("create_by")).longValue()).isEqualTo(OPERATOR);
        assertThat(((Number) row.get("update_by")).longValue()).isEqualTo(OPERATOR);
        assertThat(row.get("create_time")).isNotNull();
    }

    private Map<String, List<Map<String, Object>>> graphSnapshot(long project) throws Exception {
        return Map.of("stages", rows("SELECT * FROM project_stages WHERE project_id=? ORDER BY id", project),
            "actions", rows("SELECT * FROM stage_actions WHERE project_id=? ORDER BY id", project));
    }

    private void assertEmptyGraph(long project) throws Exception {
        assertThat(count("project_stages", project)).isZero();
        assertThat(count("stage_actions", project)).isZero();
    }

    private long count(String table, long project) throws Exception {
        assertThat(Set.of("project_stages", "stage_actions")).contains(table);
        return number("SELECT COUNT(*) FROM " + table + " WHERE project_id=?", project);
    }

    private long product() throws Exception {
        long product = id("products");
        update("INSERT INTO products (id,product_name,tenant_id,del_flag) VALUES (?,?,'000000','0')", product, "P131-product-" + product);
        return product;
    }

    private long project(String template) throws Exception {
        long product = product();
        long project = id("projects");
        update("INSERT INTO projects (id,code,name,product_id,template_type,level,tenant_id,del_flag) VALUES (?,?,?,?,?,'A','000000','0')",
            project, "P131-" + project, "P131-project-" + project, product, template);
        update("UPDATE products SET project_id=? WHERE id=?", project, product);
        return project;
    }

    /**
     * 构造满足 P1-2.1 基线校验的创建请求（目标市场 JSON、主组、四基准）。
     *
     * @param product  已插入产品主键
     * @param template HARDWARE|SOFTWARE|SOLUTION
     * @return 待 ProjectService.create 落库的项目实体
     */
    private Project createRequest(long product, String template) {
        return Project.builder().name("P131-create-" + product).productId(product).templateType(template)
            .targetMarkets("[\"CN\",\"SA\"]").level("A").mainGroupId(900001L)
            .targetSalesAmount(new BigDecimal("5000000"))
            .targetChannelCount(10).targetNps(50).targetSceneCount(5)
            .tenantId("000000").delFlag("0").build();
    }

    private long id(String table) {
        assertThat(FIXTURE_TABLES).contains(table);
        long id = nextId++;
        created.computeIfAbsent(table, ignored -> new ArrayList<>()).add(id);
        return id;
    }

    @SuppressWarnings("unchecked")
    private <T> T proxy(T target) {
        TransactionInterceptor advice = new TransactionInterceptor();
        advice.setTransactionManager(transactions);
        advice.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
        ProxyFactory factory = new ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(advice);
        T proxy = (T) factory.getProxy();
        assertThat(AopUtils.isAopProxy(proxy)).isTrue();
        return proxy;
    }

    @FunctionalInterface
    private interface CheckedWork { void run() throws Exception; }

    private void withRollback(CheckedWork work) {
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            status.setRollbackOnly();
            try {
                assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isTrue();
                work.run();
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("P131 fixture SQL failed", failure);
            }
        });
    }

    private void nested(CheckedWork work) {
        TransactionTemplate savepoint = new TransactionTemplate(transactions);
        savepoint.setPropagationBehavior(TransactionDefinition.PROPAGATION_NESTED);
        savepoint.executeWithoutResult(status -> {
            assertThat(status.hasSavepoint()).isTrue();
            try {
                work.run();
            } catch (RuntimeException | Error failure) {
                throw failure;
            } catch (Exception failure) {
                throw new IllegalStateException("P131 nested fixture failed", failure);
            }
        });
    }

    private void assertServiceCode(CheckedWork work, ApiV1ErrorCode code) {
        Throwable error = catchThrowable(work::run);
        assertThat(error).isInstanceOf(ServiceException.class);
        assertThat(((ServiceException) error).getCode()).isEqualTo(code.getCode());
    }

    private void update(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            assertThat(statement.executeUpdate()).isEqualTo(1);
        }
    }

    private Object scalar(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getObject(1);
            }
        }
    }

    private long number(String sql, Object... values) throws Exception {
        return ((Number) scalar(sql, values)).longValue();
    }

    private List<Map<String, Object>> rows(String sql, Object... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, values);
            try (ResultSet result = statement.executeQuery()) {
                List<Map<String, Object>> rows = new ArrayList<>();
                while (result.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= result.getMetaData().getColumnCount(); i++) {
                        row.put(result.getMetaData().getColumnLabel(i), result.getObject(i));
                    }
                    rows.add(row);
                }
                return rows;
            }
        }
    }

    private static void bind(PreparedStatement statement, Object... values) throws Exception {
        for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
    }

    private Connection connect() throws Exception {
        Properties properties = new Properties();
        properties.setProperty("user", client.get("user"));
        properties.setProperty("password", client.get("password"));
        return driver.connect("jdbc:mysql://127.0.0.1:13306/ipd_dev?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai", properties);
    }
}
