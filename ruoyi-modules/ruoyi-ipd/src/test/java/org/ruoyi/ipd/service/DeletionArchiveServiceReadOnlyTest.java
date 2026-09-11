package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.DeletionRequest;
import org.ruoyi.ipd.mapper.DeletionRequestMapper;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Batch-5 #9 SEC-LOW-6：DeletionArchiveService.listArchive() 必须为只读事务。
 *
 * <p>DeletionArchiveService 是归档区查询入口（仅超管可见）。原实现裸 selectList：
 * <ul>
 *   <li>无 @Transactional 注解 → 调用者持有事务时，listArchive 在 READ_COMMITTED/REPEATABLE_READ
 *       下读到的是写入事务未提交前的视图；缺隔离保证。</li>
 *   <li>无 readOnly=true → MySQL JDBC driver 不会发送 {@code connection.setReadOnly(true)}，
 *       InnoDB 无法启用 consistent snapshot 优化；Hibernate flush 也不会跳过。</li>
 * </ul>
 *
 * <p>本测试三维度证明只读契约：
 * <ol>
 *   <li>反射：{@code listArchive} 方法 {@code @Transactional(readOnly=true)} 属性存在</li>
 *   <li>行为侧：mock 下调用 listArchive 后，{@code auditLogService.append} 与
 *       {@code deletionRequestMapper.update} 一次都不被触发——证明无写事务副作用</li>
 *   <li>业务侧：未 PURGED 的 DELETED 申请能被 list 返回</li>
 * </ol>
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class DeletionArchiveServiceReadOnlyTest {

    @Mock private DeletionRequestMapper deletionRequestMapper;
    @Mock private AuditLogService auditLogService;
    @Mock private IpdPermission ipdPermission;

    private DeletionArchiveService service;

    private DeletionArchiveService newService() {
        return new DeletionArchiveService(deletionRequestMapper, auditLogService, ipdPermission);
    }

    @Test
    @DisplayName("反射：listArchive 方法声明 @Transactional(readOnly=true)")
    void listArchiveHasReadOnlyTransactionalAnnotation() throws NoSuchMethodException {
        Method m = DeletionArchiveService.class.getDeclaredMethod("listArchive");

        Transactional tx = m.getAnnotation(Transactional.class);

        assertThat(tx)
            .as("listArchive() 必须标注 @Transactional，否则只读优化失效")
            .isNotNull();
        assertThat(tx.readOnly())
            .as("@Transactional(readOnly=true) 是只读事务优化前提")
            .isTrue();
    }

    @Test
    @DisplayName("反射：purge 仍带 @Transactional(rollbackFor=Exception.class)——本次改动不污染 purge 写路径")
    void purgeAnnotationUntouched() throws NoSuchMethodException {
        Method purge = DeletionArchiveService.class.getDeclaredMethod("purge", Long.class);

        Transactional tx = purge.getAnnotation(Transactional.class);

        assertThat(tx)
            .as("purge 是写事务，必须保留 @Transactional")
            .isNotNull();
        assertThat(tx.readOnly())
            .as("purge 不应被本次误标 readOnly")
            .isFalse();
        boolean rollbackCoversException = false;
        for (Class<? extends Throwable> rf : tx.rollbackFor()) {
            if (rf.isAssignableFrom(Exception.class)) { rollbackCoversException = true; break; }
        }
        assertThat(rollbackCoversException)
            .as("purge 写事务必须 rollbackFor=Exception.class（G-11 审计链/软删原子性）")
            .isTrue();
    }

    @Test
    @DisplayName("行为：listArchive 不触发任何写操作——audit append / mapper update 全 zero interaction")
    void listArchiveNeverWritesAnything() {
        service = newService();
        IpdActor admin = new IpdActor(9001L, "archive-admin", "SUPER_ADMIN", 0L);
        when(ipdPermission.requireAdmin()).thenReturn(admin);
        when(deletionRequestMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of());

        List<DeletionRequest> result = service.listArchive();

        // 1) 授权守卫：listArchive 必须调用一次 requireAdmin（超管鉴权）
        verify(ipdPermission).requireAdmin();

        // 2) SELECT 走通：listArchive 把 LambdaQueryWrapper 传给 mapper
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<DeletionRequest>> queryCap =
            ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(deletionRequestMapper).selectList(queryCap.capture());

        // 3) 关键不变量：listArchive 不能产生任何写副作用
        verify(auditLogService, never()).append(any(AuditLog.class));
        verify(deletionRequestMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
        verify(deletionRequestMapper, never()).updateById(any(DeletionRequest.class));
        verify(deletionRequestMapper, never()).insert(any(DeletionRequest.class));
        verify(deletionRequestMapper, never()).delete(any(LambdaQueryWrapper.class));

        // 4) 查询结果透传
        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    @DisplayName("业务：listArchive 返回未被 PURGED 的 DELETED 申请（仅 selectList 路径）")
    void listArchiveReturnsUnpurgedDeletedRecords() {
        service = newService();
        IpdActor admin = new IpdActor(9002L, "archive-admin-2", "SUPER_ADMIN", 0L);
        when(ipdPermission.requireAdmin()).thenReturn(admin);

        DeletionRequest r1 = new DeletionRequest();
        r1.setId(101L);
        r1.setStatus(DeletionRequestService.ST_DELETED);
        r1.setRemark(null); // DEF-8：remark IS NULL 必须被放行

        DeletionRequest r2 = new DeletionRequest();
        r2.setId(102L);
        r2.setStatus(DeletionRequestService.ST_DELETED);
        r2.setRemark("manual-remark-not-purged"); // 非 PURGED 前缀，正常展示

        when(deletionRequestMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(List.of(r1, r2));

        List<DeletionRequest> result = service.listArchive();

        assertThat(result)
            .as("listArchive 应透传 mapper 返回的归档区记录")
            .hasSize(2)
            .extracting(DeletionRequest::getId)
            .containsExactly(101L, 102L);

        // 守卫：依旧 no-write
        verify(auditLogService, never()).append(any(AuditLog.class));
        verify(deletionRequestMapper, never()).update(any(), any(LambdaUpdateWrapper.class));
    }
}
