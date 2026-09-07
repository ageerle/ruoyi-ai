package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1ErrorCode;
import org.ruoyi.ipd.common.IpdBusinessException;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.Product;
import org.ruoyi.ipd.domain.Project;
import org.ruoyi.ipd.mapper.ProductMapper;
import org.ruoyi.ipd.mapper.ProjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P1-1.1 [0 紧急] 产品与项目双向 1:1 绑定修复卡验收。
 *
 * <p>覆盖 7 个维度（≥6）：
 * <ol>
 *   <li>正例：项目 → 唯一产品 绑定成功（两端条件 UPDATE 都 affected=1 + 终态自洽 + 审计）</li>
 *   <li>反例：产品已绑 A，再绑 B → 拒绝 409 STATE_CONFLICT（条件 UPDATE 反映 0 + 终态不写审计）</li>
 *   <li>反例：产品 A 已绑项目 X，再绑项目 Y → 拒绝 409 STATE_CONFLICT</li>
 *   <li>解绑：解绑后 product.project_id / project.product_id 都为 null，可重新绑定</li>
 *   <li>跨组守卫：actor.groupId != product.groupId → 403 FORBIDDEN（非 SUPER_ADMIN）</li>
 *   <li>审计：bind/unbind 写 audit_logs（operator_id == actor.id，action 对应）</li>
 *   <li>并发语义：条件 UPDATE 受影响行数（success=1 / affected=0）的并发安全语义验证</li>
 * </ol>
 *
 * <p>注：维度 7 模拟「条件 UPDATE 受影响行数」的并发安全语义——条件 UPDATE 的 MySQL 行锁 + DB UNIQUE 兜底
 * 是 round 8 + W5-E-2.4 已验证过的成熟模式（HandoverDisableConcurrencyTest / KpiSharedCollectionService），
 * 本测试重点在 P1-1.1 业务语义而非多线程并发框架。
 *
 * @Tag("dev") 类级——maven-surefire-plugin groups=${profiles.active}（dev）下默认跑通。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P1_1_1_BidirectionalBindingTest {

    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProjectMapper projectMapper;
    @Mock
    private AuditLogService auditLogService;

    private ProductService service;

    @BeforeAll
    static void initTableInfo() {
        // MyBatis-Plus LambdaUpdateWrapper 需要 TableInfoHelper 已注册（否则
        // "can not find lambda cache for this entity [org.ruoyi.ipd.domain.Product]"）。
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, Product.class);
        TableInfoHelper.initTableInfo(assistant, Project.class);
    }

    @BeforeEach
    void setUp() {
        service = new ProductService(productMapper, projectMapper, auditLogService);
    }

    private Product product(Long id, Long projectId, Long groupId) {
        Product p = new Product();
        p.setId(id);
        p.setProductName("人脸门禁 Pro");
        p.setSource(Product.SRC_PM_NEW);
        p.setGroupId(groupId);
        p.setProjectId(projectId);
        p.setDelFlag("0");
        return p;
    }

    private Project project(Long id, Long productId) {
        Project pr = new Project();
        pr.setId(id);
        pr.setName("人脸门禁项目");
        pr.setProductId(productId);
        pr.setDelFlag("0");
        return pr;
    }

    /**
     * 模拟「条件 UPDATE 成功」——把 projectId 写入 in-memory Product，
     * 让后续 selectById 自洽终态校验通过（对应真实 DB 行为）。
     */
    private void stubProductUpdateApply(ProjectIdApplier applier) {
        doAnswer(inv -> {
            applier.apply();
            return 1;
        }).when(productMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    /** 模拟「条件 UPDATE 成功」——projectId 写入 in-memory Project。 */
    private void stubProjectUpdateApply(ProjectIdApplier applier) {
        doAnswer(inv -> {
            applier.apply();
            return 1;
        }).when(projectMapper).update(isNull(), any(LambdaUpdateWrapper.class));
    }

    @FunctionalInterface
    private interface ProjectIdApplier {
        void apply();
    }

    // ========== 维度 1：正例 ==========

    @Test
    @DisplayName("维度1 正例：项目 → 唯一产品 绑定成功（两端条件 UPDATE 都 affected=1 + 终态自洽 + 审计）")
    void bindProject_success() {
        Product p = product(3L, null, 7L);
        Project pr = project(9L, null);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        // 模拟 DB UPDATE 写入 in-memory 对象（自洽终态校验依赖）
        stubProductUpdateApply(() -> p.setProjectId(9L));
        stubProjectUpdateApply(() -> pr.setProductId(3L));

        service.bindProject(3L, 9L, 1L, 7L, "MARKET_PM");

        // 终态自洽：product.projectId == 9
        assertThat(p.getProjectId()).isEqualTo(9L);
        assertThat(pr.getProductId()).isEqualTo(3L);
        // 条件 UPDATE 调两次（product + project）
        verify(productMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, times(1)).update(isNull(), any(LambdaUpdateWrapper.class));
        // 终态自洽校验触发重读（product 端 require + 自洽终态；project 端 requireProject + 自洽终态）
        verify(productMapper, times(2)).selectById(3L);
        verify(projectMapper, times(2)).selectById(9L);
        // 审计 1 次
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        assertThat(cap.getValue().getAction()).isEqualTo("PRODUCT_BIND_PROJECT");
        assertThat(cap.getValue().getOperatorId()).isEqualTo(1L);
    }

    // ========== 维度 2：反例 —— 产品已绑 A，再绑 B 拒绝 ==========

    @Test
    @DisplayName("维度2 反例：产品已绑 A，再绑 B → 拒绝 409 STATE_CONFLICT（A 未解绑）")
    void bindProject_alreadyBoundToOther_reject409() {
        Product p = product(3L, 9L /* 已绑 9 */, 7L);
        when(productMapper.selectById(3L)).thenReturn(p);

        // 业务规则前置：product.projectId == 9，请求绑 100，应在条件 UPDATE 前已被业务规则拦下
        IpdBusinessException ex = (IpdBusinessException) assertThatThrownBy(() ->
            service.bindProject(3L, 100L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("一个产品仅对应一个项目")
            .actual();
        assertThat(ex.getErrorCode()).isEqualTo(ApiV1ErrorCode.STATE_CONFLICT);

        // 拒绝路径：不再走条件 UPDATE（前置已拒绝）
        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        // 不写审计
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    // ========== 维度 3：反例 —— 项目 A 已绑产品 X，再绑产品 Y 拒绝 ==========

    @Test
    @DisplayName("维度3 反例：项目 A 已绑产品 X，再绑给产品 Y → 拒绝 409 STATE_CONFLICT")
    void bindProject_projectTakenByOtherProduct_reject409() {
        Product p = product(3L, null /* 待绑 */, 7L);
        Project pr = project(9L, 50L /* 已被产品 50 占用 */);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);

        assertThatThrownBy(() -> service.bindProject(3L, 9L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("该项目已关联其他产品");

        // project 端条件 UPDATE 不应被触发（前置已拒绝）
        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    // ========== 维度 4：解绑后重新绑定 ==========

    @Test
    @DisplayName("维度4 解绑：解绑后 product.project_id / project.product_id 都为 null，可重新绑定")
    void unbind_thenRebind() {
        // 第一阶段：已绑状态
        Product p = product(3L, 9L, 7L);
        Project pr = project(9L, 3L);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        // 解绑：两端条件 UPDATE affected=1（写入 null）
        stubProductUpdateApply(() -> p.setProjectId(null));
        stubProjectUpdateApply(() -> pr.setProductId(null));

        service.unbindProject(3L, 9L, 1L, 7L, "MARKET_PM");

        // 终态：两端 project_id / product_id 应为 null
        assertThat(p.getProjectId()).isNull();
        assertThat(pr.getProductId()).isNull();
        // 解绑审计
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, atLeastOnce()).append(cap.capture());
        assertThat(cap.getAllValues().stream()
            .anyMatch(a -> "PRODUCT_UNBIND_PROJECT".equals(a.getAction()))).isTrue();

        // 第二阶段：重新绑到不同项目（100）——验证 product.project_id 已被解绑
        Product p2 = product(3L, null, 7L);
        Project pr100 = project(100L, null);
        // 重读返回解绑后的状态
        when(productMapper.selectById(3L)).thenReturn(p2);
        when(projectMapper.selectById(100L)).thenReturn(pr100);
        // 第二段 update stub 同样模拟 DB 写入
        stubProductUpdateApply(() -> p2.setProjectId(100L));
        stubProjectUpdateApply(() -> pr100.setProductId(3L));

        service.bindProject(3L, 100L, 1L, 7L, "MARKET_PM");

        assertThat(p2.getProjectId()).isEqualTo(100L);
        assertThat(pr100.getProductId()).isEqualTo(3L);
    }

    // ========== 维度 5：跨组守卫 ==========

    @Test
    @DisplayName("维度5 跨组守卫：actor.groupId != product.groupId → 拒绝 403 FORBIDDEN（非 SUPER_ADMIN）")
    void bindProject_crossGroup_reject403() {
        Product p = product(3L, null, 7L /* 产品组 7 */);
        when(productMapper.selectById(3L)).thenReturn(p);

        // 操作人在产品组 99，跨组
        IpdBusinessException ex5 = (IpdBusinessException) assertThatThrownBy(() ->
            service.bindProject(3L, 9L, 1L, 99L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("横向越权")
            .actual();
        assertThat(ex5.getErrorCode()).isEqualTo(ApiV1ErrorCode.FORBIDDEN);

        // 守卫先于任何写操作
        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("维度5 跨组守卫：SUPER_ADMIN 豁免（actor.groupId 任意）→ 通过")
    void bindProject_superAdmin_exempt() {
        Product p = product(3L, null, 7L);
        Project pr = project(9L, null);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        stubProductUpdateApply(() -> p.setProjectId(9L));
        stubProjectUpdateApply(() -> pr.setProductId(3L));

        // SUPER_ADMIN 任意 groupId 都通过
        service.bindProject(3L, 9L, 99L, 999L, "SUPER_ADMIN");

        assertThat(p.getProjectId()).isEqualTo(9L);
        assertThat(pr.getProductId()).isEqualTo(3L);
    }

    // ========== 维度 6：审计一致 ==========

    @Test
    @DisplayName("维度6 审计：bind 写 audit_logs（operator_id == actor.id，action=PRODUCT_BIND_PROJECT）")
    void bindProject_auditConsistent() {
        Product p = product(3L, null, 7L);
        Project pr = project(9L, null);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        stubProductUpdateApply(() -> p.setProjectId(9L));
        stubProjectUpdateApply(() -> pr.setProductId(3L));

        service.bindProject(3L, 9L, 42L /* 操作人 */, 7L, "MARKET_PM");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getOperatorId()).isEqualTo(42L);
        assertThat(log.getAction()).isEqualTo("PRODUCT_BIND_PROJECT");
        assertThat(log.getEntityType()).isEqualTo("products");
        assertThat(log.getEntityId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("维度6 审计：unbind 写 audit_logs（operator_id == actor.id，action=PRODUCT_UNBIND_PROJECT）")
    void unbindProject_auditConsistent() {
        Product p = product(3L, 9L, 7L);
        Project pr = project(9L, 3L);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        stubProductUpdateApply(() -> p.setProjectId(null));
        stubProjectUpdateApply(() -> pr.setProductId(null));

        service.unbindProject(3L, 9L, 77L /* 操作人 */, 7L, "MARKET_PM");

        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService, times(1)).append(cap.capture());
        AuditLog log = cap.getValue();
        assertThat(log.getOperatorId()).isEqualTo(77L);
        assertThat(log.getAction()).isEqualTo("PRODUCT_UNBIND_PROJECT");
        assertThat(log.getEntityType()).isEqualTo("products");
        assertThat(log.getEntityId()).isEqualTo(3L);
    }

    // ========== 维度 7：并发守卫（条件 UPDATE 反映语义 + 事务回滚） ==========

    @Test
    @DisplayName("维度7 并发守卫（语义级）：product 端条件 UPDATE 失败 → 拒绝 409，事务回滚不写审计")
    void bindProject_concurrentRace_productUpdateLoses_reject409() {
        // 模拟「竞态已发生」：product 端条件 UPDATE 反映 0（其他事务已抢到 product_id）
        Product p = product(3L, null, 7L);
        Project pr = project(9L, null);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        // 业务规则前置检查通过（product.projectId=null 且 project.productId=null），
        // 但条件 UPDATE 抢锁失败（之前并发请求已抢到 product_id）
        when(productMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.bindProject(3L, 9L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("条件更新冲突");

        // project 端条件 UPDATE 不应被触发（product 端失败 → 事务回滚）
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        // 不写审计
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    @Test
    @DisplayName("维度7 并发守卫（语义级）：project 端条件 UPDATE 失败 → 拒绝 409，事务回滚不写审计")
    void bindProject_concurrentRace_projectUpdateLoses_reject409() {
        // 模拟「竞态已发生」：product 端条件 UPDATE 反映 1（抢到 product_id），
        // 但 project 端条件 UPDATE 反映 0（其他事务已抢到 project.product_id）
        Product p = product(3L, null, 7L);
        Project pr = project(9L, null);
        when(productMapper.selectById(3L)).thenReturn(p);
        when(projectMapper.selectById(9L)).thenReturn(pr);
        stubProductUpdateApply(() -> p.setProjectId(9L)); // product 端抢到
        when(projectMapper.update(isNull(), any(LambdaUpdateWrapper.class))).thenReturn(0);

        assertThatThrownBy(() -> service.bindProject(3L, 9L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("该项目已被其他产品绑定");

        // 不写审计
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    // ========== 辅助：解绑自洽 ==========

    @Test
    @DisplayName("解绑守卫：未绑状态下解绑 → 拒绝 409（终态自洽）")
    void unbind_notBound_reject409() {
        Product p = product(3L, null /* 未绑 */, 7L);
        when(productMapper.selectById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.unbindProject(3L, 9L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("产品未绑定该项目");

        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }

    // ========== 辅助：GUEST_OTHER 拒绝 ==========

    @Test
    @DisplayName("GUEST_OTHER 占位不可绑定 → 拒绝 409 STATE_CONFLICT")
    void bindProject_guestOther_reject409() {
        Product p = product(3L, null, 7L);
        p.setSource(Product.SRC_GUEST_OTHER);
        when(productMapper.selectById(3L)).thenReturn(p);

        assertThatThrownBy(() -> service.bindProject(3L, 9L, 1L, 7L, "MARKET_PM"))
            .isInstanceOf(IpdBusinessException.class)
            .hasMessageContaining("游客");

        verify(productMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(projectMapper, never()).update(isNull(), any(LambdaUpdateWrapper.class));
        verify(auditLogService, never()).append(any(AuditLog.class));
    }
}