package org.ruoyi.ipd.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.common.ApiV1Response;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.security.IpdActor;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.service.AuditLogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PERF-P0-3：{@link AuditLogController#listByScope} 接入 4-arg 游标分页（@Tag("dev") 必须）。
 *
 * <p>覆盖 2 个维度：
 * <ol>
 *   <li>传 {@code beforeSeq} 走 4-arg 游标路径，返回结构含 {@code cursorMode=true} 与
 *       {@code nextBeforeSeq}（取本页最小 seq-1）</li>
 *   <li>不传 {@code beforeSeq} 走 3-arg 兼容路径，返回结构不含 {@code cursorMode} 与 {@code nextBeforeSeq}</li>
 * </ol>
 *
 * <p>AC：PERF-P0-3 / QA-05-P3 接入。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class AuditLogControllerCursorTest {

    @Mock
    private org.ruoyi.ipd.mapper.AuditLogMapper auditLogMapper;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private IpdPermission ipdPermission;
    @Mock
    private org.ruoyi.ipd.mapper.PersonMapper personMapper;

    @InjectMocks
    private AuditLogController controller;

    /** PERF-P0-3 #1：beforeSeq 非 null ⇒ 4-arg 路径 + 返回结构含 nextBeforeSeq */
    @Test
    @DisplayName("PERF-P0-3 #1：listByScope 传 beforeSeq 走 4-arg 游标路径，返回 cursorMode=true + nextBeforeSeq")
    @SuppressWarnings("unchecked")
    void controllerCursorPagingReturnsNextBeforeSeq() {
        // 场景：MARKET_PM 角色 → operatorIds=[actor.id]=OWN，scope 标签正确
        IpdActor actor = new IpdActor(900101L, "tester", "MARKET_PM", null);
        when(ipdPermission.requireInternal()).thenReturn(actor);

        // service.listByOperatorIds(4-arg) 返回含真实 seq 的 Page（seq=100,300,250 倒序）
        Page<AuditLog> fakePage = new Page<>();
        List<AuditLog> records = new ArrayList<>();
        for (long s : new long[]{100L, 300L, 250L}) {
            AuditLog r = new AuditLog();
            r.setSeq(s);
            records.add(r);
        }
        fakePage.setRecords(records);
        when(auditLogService.listByOperatorIds(eq(List.of(900101L)), eq(1), eq(20), eq(500L)))
            .thenReturn(fakePage);

        ApiV1Response<Map<String, Object>> resp = controller.listByScope(1, 20, 500L);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        Map<String, Object> body = resp.getData();
        assertThat(body).isNotNull();
        // 游标模式必须显式声明 + 给出下一页游标
        assertThat(body).containsEntry("cursorMode", true);
        // 本页最小 seq=100 → nextBeforeSeq = 100-1 = 99（strict less than 上界）
        assertThat(body).containsEntry("nextBeforeSeq", 99L);
        assertThat(body.get("scope")).isEqualTo("OWN");
        // service 4-arg 路径必须被调到，3-arg 路径不能被调
        verify(auditLogService).listByOperatorIds(List.of(900101L), 1, 20, 500L);
        verify(auditLogService, never()).listByOperatorIds(any(), anyInt(), anyInt());
    }

    /** PERF-P0-3 #2：不传 beforeSeq ⇒ 3-arg 兼容路径 + 返回结构无 nextBeforeSeq/cursorMode */
    @Test
    @DisplayName("PERF-P0-3 #2：listByScope 不传 beforeSeq 走 3-arg 兼容路径，返回无 nextBeforeSeq/cursorMode")
    void controller3ArgFallsBackToOffset() {
        IpdActor actor = new IpdActor(900101L, "tester", "MARKET_PM", null);
        when(ipdPermission.requireInternal()).thenReturn(actor);

        Page<AuditLog> fakePage = new Page<>();
        fakePage.setRecords(List.of());
        when(auditLogService.listByOperatorIds(eq(List.of(900101L)), eq(1), eq(20)))
            .thenReturn(fakePage);

        ApiV1Response<Map<String, Object>> resp = controller.listByScope(1, 20, null);

        assertThat(resp.getCode()).isEqualTo(ApiV1Response.CODE_SUCCESS);
        Map<String, Object> body = resp.getData();
        assertThat(body).isNotNull();
        // 兼容路径：cursorMode 与 nextBeforeSeq 都不在 body 内
        assertThat(body).doesNotContainKey("cursorMode");
        assertThat(body).doesNotContainKey("nextBeforeSeq");
        // service 3-arg 路径必须被调到，4-arg 路径不能被调
        verify(auditLogService).listByOperatorIds(List.of(900101L), 1, 20);
        verify(auditLogService, never()).listByOperatorIds(any(), anyInt(), anyInt(), anyLong());
    }

    /** PERF-P0-3 #3（边界）：游标模式但本页空记录 → nextBeforeSeq=null（无下一页可翻） */
    @Test
    @DisplayName("PERF-P0-3 #3：游标模式 + 空记录 → nextBeforeSeq=null（避免客户端死循环）")
    @SuppressWarnings("unchecked")
    void cursorModeEmptyPageReturnsNullNextBeforeSeq() {
        IpdActor actor = new IpdActor(900101L, "tester", "MARKET_PM", null);
        when(ipdPermission.requireInternal()).thenReturn(actor);

        Page<AuditLog> emptyPage = new Page<>();
        emptyPage.setRecords(List.of());
        when(auditLogService.listByOperatorIds(any(), anyInt(), anyInt(), anyLong()))
            .thenReturn(emptyPage);

        ApiV1Response<Map<String, Object>> resp = controller.listByScope(1, 20, 9999L);

        Map<String, Object> body = resp.getData();
        assertThat(body).containsEntry("cursorMode", true);
        // 边界：空页 → nextBeforeSeq=null（前端拿 null 即可停翻页，不会再用 0/1 误发请求）
        assertThat(body).containsEntry("nextBeforeSeq", null);
    }
}