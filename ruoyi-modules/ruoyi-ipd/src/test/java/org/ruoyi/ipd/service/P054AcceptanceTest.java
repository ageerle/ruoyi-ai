package org.ruoyi.ipd.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.mapper.AuditLogMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * P0-5.4 验收：审计查询/导出范围（P0-5.4 验收要点）。
 *
 * <p>AC-AUD-04：普通 PM 导出审计日志 ⇒ 仅能导出与本人相关的日志
 * <br>AC-AUD-05：组长导出 / 超管导出 ⇒ 分别为本组 / 全局
 * <br>AC-AUD-06：未登录游客访问审计接口 ⇒ 返回 2xxxx，无任何数据（由 IpdWebSecurityConfig 拦截，单元层不重复覆盖）
 * <p>本类只覆盖 {@link AuditLogService#listByOperatorIds} / {@link AuditLogService#countByOperatorIds}
 * 的接口契约：page 范围透传 + scope 解析（null=全局 / ids=限定）。
 * <p>HTTP 层真库验收另见 evidence-p054-http-acceptance-*.json。
 * <p>形态为 Mockito 单元验收；不得据此标 done（BR-真库）。
 * <p><b>避免 SQL 字符串断言</b>：MyBatis-Plus LambdaQueryWrapper 需要 TableInfo 缓存，纯单测无
 * MybatisPlusInterceptor 时调 getSqlSegment() 会抛"can not find lambda cache"，
 * 故只断言 Page 参数与调用次数，不钻 SQL 内部。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class P054AcceptanceTest {

    @Mock private AuditLogMapper auditLogMapper;

    private AuditLogService service;

    @BeforeEach
    void setUp() {
        service = new AuditLogService(auditLogMapper);
    }

    @Test
    @DisplayName("AC-AUD-04：本人范围 — 调用 selectPage 时透传 page+wrapper，wrapper 不为 null")
    void ownScopeInvokesSelectPage() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(List.of(900101L), 1, 20);

        assertThat(result).isNotNull();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<AuditLog>> pageCap = ArgumentCaptor.forClass(IPage.class);
        verify(auditLogMapper).selectPage(pageCap.capture(), any(LambdaQueryWrapper.class));
        assertThat(pageCap.getValue().getCurrent()).isEqualTo(1);
        assertThat(pageCap.getValue().getSize()).isEqualTo(20);
    }

    @Test
    @DisplayName("AC-AUD-05 组长：operatorIds=[组内多 personId] 同样走 selectPage（不限单值）")
    void groupLeaderScopeAcceptsList() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        List<Long> groupIds = List.of(900101L, 900102L, 900103L);
        IPage<AuditLog> result = service.listByOperatorIds(groupIds, 1, 20);

        assertThat(result).isNotNull();
        verify(auditLogMapper).selectPage(any(IPage.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("AC-AUD-05 超管：operatorIds=null ⇒ 仍调 selectPage（不加 IN）")
    void superAdminScopeNoFilter() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(null, 1, 20);

        assertThat(result).isNotNull();
        verify(auditLogMapper).selectPage(any(IPage.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("空集合 [] 走 selectPage（与 null 行为不同 = service 不抛）")
    void emptyListDoesNotThrow() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        IPage<AuditLog> result = service.listByOperatorIds(List.of(), 1, 20);

        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("页大小上限 200：pageSize=1000 透传后被 clamp 到 200")
    void pageSizeClamped() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<AuditLog>> pageCap = ArgumentCaptor.forClass(IPage.class);
        service.listByOperatorIds(List.of(900101L), 1, 1000);

        verify(auditLogMapper).selectPage(pageCap.capture(), any(LambdaQueryWrapper.class));
        IPage<AuditLog> page = pageCap.getValue();
        assertThat(page.getSize()).isEqualTo(200);
    }

    @Test
    @DisplayName("负 pageNo/0 pageSize 兜底为 1/1")
    void negativePageParamsClamped() {
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<IPage<AuditLog>> pageCap = ArgumentCaptor.forClass(IPage.class);
        service.listByOperatorIds(List.of(900101L), -5, 0);

        verify(auditLogMapper).selectPage(pageCap.capture(), any(LambdaQueryWrapper.class));
        IPage<AuditLog> page = pageCap.getValue();
        assertThat(page.getCurrent()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(1);
    }

    @Test
    @DisplayName("countByOperatorIds：null → 走 selectCount；ids=[单值] → 同样走 selectCount")
    void countScopeInvokesSelectCount() {
        when(auditLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(42L);

        long allCount = service.countByOperatorIds(null);
        long ownCount = service.countByOperatorIds(List.of(900101L));

        assertThat(allCount).isEqualTo(42L);
        assertThat(ownCount).isEqualTo(42L);
        verify(auditLogMapper, org.mockito.Mockito.times(2)).selectCount(any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("BR-AUD：导出范围 = 列表范围（同一 groupIds 两次调用都用 selectCount + selectPage）")
    void exportAndListShareScope() {
        when(auditLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(7L);
        when(auditLogMapper.selectPage(any(IPage.class), any(LambdaQueryWrapper.class)))
            .thenReturn(stubPage());

        List<Long> groupIds = List.of(900101L, 900102L);
        long countForExport = service.countByOperatorIds(groupIds);
        service.listByOperatorIds(groupIds, 1, 20);

        assertThat(countForExport).isEqualTo(7L);
        verify(auditLogMapper).selectCount(any(LambdaQueryWrapper.class));
        verify(auditLogMapper).selectPage(any(IPage.class), any(LambdaQueryWrapper.class));
    }

    @Test
    @DisplayName("Service 不感知角色 — 任何 scope 输入都不抛（角色判定在 Controller 层）")
    void serviceIsRoleAgnostic() {
        when(auditLogMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        // service 接受 null 与空集，不抛
        long c1 = service.countByOperatorIds(null);
        long c2 = service.countByOperatorIds(List.of());
        assertThat(c1).isEqualTo(0L);
        assertThat(c2).isEqualTo(0L);
    }

    private IPage<AuditLog> stubPage() {
        return new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>();
    }
}
