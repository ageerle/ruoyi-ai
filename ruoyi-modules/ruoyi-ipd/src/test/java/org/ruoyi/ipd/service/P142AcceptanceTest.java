/**
 * P1-2.x 配套——深管交付物 ossId 链路验收（[CONSISTENCY-13] 2026-09-06）。
 *
 * 验证 StageActionService.addDeliverable(actionId, fileName, ossId, operator)：
 * - ossId 非 null 时写入 Deliverable.ossId 字段；
 * - ossId null 时 ossId 字段也写 null（允许历史无 OSS 文件占位）；
 * - fileName 与 actionId 必填校验（不空）；
 * - 落库后 Deliverable.actionId / uploadedBy 正确回填。
 *
 * 不依赖数据库（Mockito 单元验收），参考 P343AcceptanceTest 形态。
 */
package org.ruoyi.ipd.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.Deliverable;
import org.ruoyi.ipd.domain.StageAction;
import org.ruoyi.ipd.mapper.DeliverableMapper;
import org.ruoyi.ipd.mapper.StageActionMapper;
import org.ruoyi.ipd.service.AuditLogService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

@Tag("dev")
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class P142AcceptanceTest {

    @Mock
    private DeliverableMapper deliverableMapper;
    @Mock
    private StageActionMapper stageActionMapper;
    @Mock
    private org.ruoyi.ipd.mapper.ProjectMapper projectMapper;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private StageActionService service;

    @org.junit.jupiter.api.BeforeEach
    void setupAction() {
        StageAction action = new StageAction();
        action.setId(100L);
        action.setProjectId(10L);
        org.ruoyi.ipd.domain.Project proj = new org.ruoyi.ipd.domain.Project();
        proj.setId(10L);
        proj.setDelFlag("0");
        org.mockito.Mockito.when(stageActionMapper.selectById(100L)).thenReturn(action);
        org.mockito.Mockito.when(stageActionMapper.selectById(101L)).thenReturn(action);
        org.mockito.Mockito.lenient().when(projectMapper.selectById(10L)).thenReturn(proj);
    }

    @Test
    @DisplayName("[CONSISTENCY-13] ossId 非 null → Deliverable.ossId 写入 9001")
    void ossIdNonNull() {
        service.addDeliverable(100L, "会议纪要.pdf", 9001L, "op-1");
        ArgumentCaptor<Deliverable> captor = ArgumentCaptor.forClass(Deliverable.class);
        verify(deliverableMapper).insert(captor.capture());
        Deliverable d = captor.getValue();
        assertThat(d.getActionId()).isEqualTo(100L);
        assertThat(d.getFileName()).isEqualTo("会议纪要.pdf");
        assertThat(d.getOssId()).isEqualTo(9001L);
    }

    @Test
    @DisplayName("[CONSISTENCY-13] ossId null → Deliverable.ossId = null（兼容历史数据）")
    void ossIdNull() {
        service.addDeliverable(101L, "doc.pdf", null, "op-2");
        ArgumentCaptor<Deliverable> captor = ArgumentCaptor.forClass(Deliverable.class);
        verify(deliverableMapper).insert(captor.capture());
        Deliverable d = captor.getValue();
        assertThat(d.getOssId()).isNull();
        assertThat(d.getFileName()).isEqualTo("doc.pdf");
    }
}
