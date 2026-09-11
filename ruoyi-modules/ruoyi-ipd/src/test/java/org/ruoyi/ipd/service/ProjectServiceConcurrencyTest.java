package org.ruoyi.ipd.service;

import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;
import org.ruoyi.ipd.support.NoopTransactionManager;
import org.springframework.dao.DuplicateKeyException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-01 取号并发 + 撞号收口单测。
 *
 * <p><b>本类为什么不再断言「50 并发 nextCode() 必得 50 个互不相同的编码」（2026-09-08 重写）</b>：
 * 旧版本断言的正是这句话，而它<b>不是生产契约</b>。{@link ProjectService#create} 的 Javadoc 明写
 * 「READ_COMMITTED 下 synchronized(nextCode) 无法覆盖『取号→提交』窗口，HTTP 并发会撞号」——
 * 唯一性由 {@code uk_projects_code} 唯一键 + {@code CODE_CONFLICT_MAX_RETRY=8} 重试兜底，
 * <b>不在 nextCode() 这一层</b>；nextCode() 只是「读当年最大号 +1」的纯读算，DB 状态不变时
 * 并发调用返回同一个号本就是正确行为。
 *
 * <p>旧测试用 {@code sharedMax.incrementAndGet()} 在锁<b>外</b>推进模拟 DB，等于亲手把那个
 * 「取号→提交」窗口造了出来，于是断言成立与否完全取决于调度时序：本机多核稳定绿，
 * GitHub Actions 2 vCPU runner 上 50 线程挤 20 池线程必红（PR #11 首次实跑即被 ipd-test-gate 抓到，
 * 见 {@code scripts/ci/ipd-test-red-baseline.txt}）。这不是「偶发抖动」，是断言指向了不存在的契约。
 *
 * <p>重写后四条断言各对应一个<b>真实且可确定性验证</b>的契约，且都不依赖线程调度时序：
 * <ol>
 *   <li>{@code nextCode()} 的读-算临界区确实互斥（模拟 DB 的「读 + 占号」两步都置于被测锁之内；
 *       反向验证实测：摘掉 {@code synchronized} 后本条立即重号变红 → PERF-01 的回归力是真的）</li>
 *   <li>{@code nextCode()} 确有 {@code synchronized} 修饰 + {@code @Lock4j} 跨 JVM 锁（反射钉，确定性）</li>
 *   <li>撞号后 {@code create()} 换新号重试成功（生产真正的唯一性收口）</li>
 *   <li>重试耗尽恰好 8 次后抛「项目编码冲突，请重试」（钉住 {@code CODE_CONFLICT_MAX_RETRY}）</li>
 * </ol>
 *
 * <p><b>①为什么必须手写 Proxy 假 mapper 而不能用 Mockito mock</b>：Mockito 的
 * {@code InvocationContainerImpl} 对 answer 调用自带 mutex 同步，{@code thenAnswer} 里的代码
 * 本就被串行化了。反向验证实测（2026-09-08）：改用 mock 时即便摘掉 {@code nextCode()} 的
 * {@code synchronized}，并发断言依旧全绿 —— 测试通过但对 PERF-01 零回归力，
 * 属「绿灯 ≠ 有保护」的又一形态。Proxy 的 InvocationHandler 没有这层同步，竞态窗口才是真的。
 *
 * <p>③④ 此前在 CI 里<b>零覆盖</b>：唯一验证该机制的 {@code Qa04MysqlConcurrencyTest}（3a 线）被
 * {@code @EnabledIfSystemProperty(ipd.scope.mysql.enabled)} 门控，CI 实跑为 4 tests / 4 skipped
 * （PR #11 run 34221338056 实证）。也就是说：在跑的并发测试断言了生产没有的性质，
 * 而生产真正依赖的收口机制无人看守。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class ProjectServiceConcurrencyTest {

    /** 与 nextCode() 同源计算，避免跨年时 substring 解析错位（旧版硬编码 "PRJ-2026-"）。 */
    private static final String PREFIX = "PRJ-" + Calendar.getInstance().get(Calendar.YEAR) + "-";

    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private GateEngine gateEngine;
    @Mock
    private ProjectBootstrapService projectBootstrapService;
    @Mock
    private ProjectCertService projectCertService;
    @Mock
    private org.ruoyi.ipd.mapper.StageActionMapper stageActionMapper;
    @Mock
    private org.ruoyi.ipd.mapper.KpiRecordMapper kpiRecordMapper;

    private ProjectService service;

    @BeforeEach
    void setUp() {
        service = new ProjectService(projectMapper, productMapper, stageActionMapper, kpiRecordMapper,
            auditLogService, gateEngine,
            projectBootstrapService, projectCertService, NoopTransactionManager.INSTANCE,
            null /* P2-6.2 */);
    }

    @Test
    @DisplayName("nextCode 并发 50 线程：读-算临界区互斥，50 个编码互不相同")
    void nextCode_concurrent50_shouldReturnDistinctCodes() throws Exception {
        // 模拟 DB 的「读当年最大号」+「新号落库占位」两步，两步都在 nextCode() 的 synchronized
        // 临界区内发生 —— 这正是 @Lock4j 在生产中要达到的效果（锁覆盖读与占号，而非只覆盖读）。
        // 假 mapper 见 racingMapper() 的说明：必须绕开 Mockito 的内部 mutex，否则本条断言不可能失败。
        AtomicInteger dbMaxSeq = new AtomicInteger(0);
        ProjectService racing = new ProjectService(racingMapper(dbMaxSeq), productMapper,
            stageActionMapper, kpiRecordMapper, auditLogService, gateEngine,
            projectBootstrapService, projectCertService, NoopTransactionManager.INSTANCE,
            null /* P2-6.2 */);

        int n = 50;
        ExecutorService pool = Executors.newFixedThreadPool(20);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(n);
        Set<String> codes = ConcurrentHashMap.newKeySet();
        AtomicInteger errors = new AtomicInteger(0);

        for (int i = 0; i < n; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    String code = racing.nextCode();
                    if (!codes.add(code)) {
                        errors.incrementAndGet();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        boolean finished = done.await(30, TimeUnit.SECONDS);
        pool.shutdownNow();

        assertThat(finished).as("50 threads should finish in 30s").isTrue();
        assertThat(errors.get()).as("no duplicate codes, no errors").isZero();
        assertThat(codes).as("all 50 codes distinct").hasSize(n);
    }

    @Test
    @DisplayName("nextCode 双层锁在位：synchronized 修饰 + @Lock4j 键 'ipd:project:code'")
    void nextCode_carriesBothLockLayers() throws Exception {
        // 反射钉而非行为钉：修饰符与注解是确定性的，不受调度时序影响。
        // 上一条并发测试证明「锁在时不重号」，本条证明「锁确实没被摘」——两条合起来
        // 才等价于旧版想要而没能可靠表达的 PERF-01 回归意图。
        Method nextCode = ProjectService.class.getDeclaredMethod("nextCode");

        assertThat(Modifier.isSynchronized(nextCode.getModifiers()))
            .as("PERF-01 同 JVM 兜底锁不可摘（摘掉后并发测试立即重号）")
            .isTrue();

        Lock4j lock = nextCode.getAnnotation(Lock4j.class);
        assertThat(lock).as("PERF-01 跨 JVM 分布式锁不可摘（生产多实例唯一性依赖它）").isNotNull();
        assertThat(lock.keys()).containsExactly("'ipd:project:code'");
    }

    @Test
    @DisplayName("撞号收口：insert 撞 uk_projects_code → 换新号重试成功，编码已重新生成")
    void create_retriesOnDuplicateKey_andSucceedsWithNewCode() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        // 忠实模拟 DB：撞号意味着该号已被并发事务提交，重试时 selectList 必须能读到它，
        // 否则 nextCode() 会算出同一个号，重试永远撞同一堵墙（这也是 create() 里
        // project.setCode(null) 的意义——强制重新取号）。
        AtomicInteger committed = new AtomicInteger(0);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenAnswer(inv -> {
            int cur = committed.getAndIncrement();
            if (cur == 0) {
                return List.<Project>of();
            }
            Project p = new Project();
            p.setCode(PREFIX + String.format("%03d", cur));
            return List.of(p);
        });
        doThrow(new DuplicateKeyException("Duplicate entry for key 'uk_projects_code'"))
            .doReturn(1)
            .when(projectMapper).insert(any(Project.class));

        Project created = service.create(base("S", null, null), 1L);

        // 第一次取号得 001 并撞键，第二次必须换到 002 —— 证明重试真的重新取号，
        // 而不是拿同一个号反复撞。
        assertThat(created.getCode()).isEqualTo(PREFIX + "002");
        assertThat(created.getCurrentStage()).isEqualTo("CONCEPT");
        assertThat(created.getStatus()).isEqualTo("DRAFT");
        verify(projectMapper, times(2)).insert(any(Project.class));
        verify(projectBootstrapService).bootstrap(created.getId(), 1L);
        verify(auditLogService).append(any());
    }

    @Test
    @DisplayName("撞号收口：重试耗尽恰好 8 次 → 抛「项目编码冲突，请重试」")
    void create_givesUpAfterMaxRetry() {
        when(productMapper.selectById(50L)).thenReturn(product50());
        when(projectMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(projectMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        doThrow(new DuplicateKeyException("Duplicate entry for key 'uk_projects_code'"))
            .when(projectMapper).insert(any(Project.class));

        assertThatThrownBy(() -> service.create(base("S", null, null), 1L))
            .isInstanceOf(ServiceException.class)
            .hasMessageContaining("项目编码冲突");

        // 钉住 CODE_CONFLICT_MAX_RETRY=8：改大改小本条都会失败。重试预算是撞号收敛概率的
        // 直接函数（8 次下 50 并发撞号收敛概率已足够），属需要显式决策的参数，
        // 不是可以随手调整的实现细节；异常塌成 500 或无限重试同样都是事故。
        verify(projectMapper, times(8)).insert(any(Project.class));
    }

    /**
     * 手写 Proxy 假 mapper —— 刻意不用 Mockito mock，理由见类 Javadoc。
     *
     * <p>{@code selectList} 内是非原子的 {@code get()} 后 {@code set()}（不用
     * {@code incrementAndGet()}），中间夹一次 {@code Thread.yield()} 放大竞态窗口：
     * 持锁时整个 handler 被串行执行，yield 不改变语义、断言恒成立；
     * 一旦摘掉 {@code synchronized}，两步之间就会插进别的线程，重号立刻暴露。
     *
     * <p>未预期的方法一律抛异常而非返回 null：静默返回 null 会让「被测代码多调了一个方法」
     * 这类实现漂移悄悄溜过去。
     */
    private ProjectMapper racingMapper(AtomicInteger dbMaxSeq) {
        return (ProjectMapper) Proxy.newProxyInstance(
            getClass().getClassLoader(),
            new Class<?>[]{ProjectMapper.class},
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "selectList": {
                        int cur = dbMaxSeq.get();
                        Thread.yield();
                        dbMaxSeq.set(cur + 1);
                        if (cur == 0) {
                            return List.of();
                        }
                        Project p = new Project();
                        p.setCode(PREFIX + String.format("%03d", cur));
                        return List.of(p);
                    }
                    case "toString":
                        return "racingMapper";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                    case "equals":
                        return proxy == args[0];
                    default:
                        throw new UnsupportedOperationException(
                            "racingMapper 未实现 " + method.getName()
                                + "（nextCode() 只应调 selectList，多调即实现漂移）");
                }
            });
    }

    /** 与 ProjectServiceTest 同形的最小合法立项请求（S 级、四基准齐、归属产品 50）。 */
    private Project base(String level, String coefficient, String reason) {
        Project p = new Project();
        p.setName("人脸门禁 S 级");
        p.setProductId(50L);
        p.setTemplateType("HARDWARE");
        p.setTargetMarkets("[\"SA\"]");
        p.setMainGroupId(7L);
        p.setLevel(level);
        p.setLevelCoefficient(coefficient == null ? null : new BigDecimal(coefficient));
        p.setLevelCoefficientReason(reason);
        p.setTargetSalesAmount(new BigDecimal("5000000"));
        p.setTargetChannelCount(10);
        p.setTargetNps(70);
        p.setTargetSceneCount(5);
        return p;
    }

    private Product product50() {
        Product product = new Product();
        product.setId(50L);
        product.setProductName("人脸门禁");
        product.setDelFlag("0");
        return product;
    }
}
