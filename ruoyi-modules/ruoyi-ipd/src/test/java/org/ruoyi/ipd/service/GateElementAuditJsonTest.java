package org.ruoyi.ipd.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.ruoyi.ipd.domain.AuditLog;
import org.ruoyi.ipd.domain.GateElement;
import org.ruoyi.ipd.mapper.GateElementMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * DEF-1 回归（QA-03 矩阵实测发现）：audit_logs.after_data 为 MySQL JSON 列，
 * GateElementService.audit 纯文本直写曾触发 MysqlDataTruncation → create 全量回滚。
 * 本类锁死「audit 收到的 afterData 必为合法 JSON」契约（mock 层；真机 HTTP 证据见 QA-03 报告）。
 */
@Tag("dev")
@ExtendWith(MockitoExtension.class)
class GateElementAuditJsonTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Mock
    private GateElementMapper gateElementMapper;
    @Mock
    private AuditLogService auditLogService;

    private GateElementService service;

    @BeforeEach
    void setUp() {
        service = new GateElementService(gateElementMapper, auditLogService);
    }

    private AuditLog capturedAudit() {
        ArgumentCaptor<AuditLog> cap = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogService).append(cap.capture());
        return cap.getValue();
    }

    private static void assertValidJson(String afterData, String expectDetailPart) {
        assertThat(afterData).isNotBlank();
        JsonNode node;
        try {
            node = JSON.readTree(afterData);
        } catch (Exception e) {
            throw new AssertionError("afterData 非合法 JSON: " + afterData, e);
        }
        assertThat(node.get("detail").asText()).contains(expectDetailPart);
    }

    @Test
    @DisplayName("DEF-1 create 审计 afterData 为合法 JSON 且含 gate/element（修复前纯文本直写）")
    void createAuditAfterDataIsValidJson() {
        when(gateElementMapper.selectCount(any())).thenReturn(0L);
        when(gateElementMapper.insert(any(GateElement.class))).thenAnswer(inv -> {
            GateElement e = inv.getArgument(0);
            e.setId(66L);
            return 1;
        });
        GateElement e = new GateElement();
        e.setGateCode("G1");
        e.setElementCode("QA03-JSON");
        e.setElementName("QA03要素");

        service.create(e, "900101");

        AuditLog log = capturedAudit();
        assertThat(log.getEntityType()).isEqualTo("GATE_ELEMENT");
        assertThat(log.getAction()).isEqualTo("CREATE");
        assertThat(log.getEntityId()).isEqualTo(66L);
        assertValidJson(log.getAfterData(), "G1/QA03-JSON");
    }

    @Test
    @DisplayName("DEF-1 update 审计 afterData 为合法 JSON")
    void updateAuditAfterDataIsValidJson() {
        GateElement exist = new GateElement();
        exist.setId(66L);
        exist.setGateCode("G1");
        exist.setElementCode("QA03-JSON");
        exist.setElementName("旧名");
        when(gateElementMapper.selectById(66L)).thenReturn(exist);
        GateElement patch = new GateElement();
        patch.setId(66L);
        patch.setElementName("新名");

        service.update(patch, "900101");

        AuditLog log = capturedAudit();
        assertThat(log.getAction()).isEqualTo("UPDATE");
        assertValidJson(log.getAfterData(), "QA03-JSON");
    }

    @Test
    @DisplayName("DEF-1 disable 审计 afterData 为合法 JSON 且含 disabled 标记")
    void disableAuditAfterDataIsValidJson() {
        GateElement exist = new GateElement();
        exist.setId(66L);
        exist.setElementCode("QA03-JSON");
        when(gateElementMapper.selectById(66L)).thenReturn(exist);

        service.disable(66L, "900101");

        AuditLog log = capturedAudit();
        assertValidJson(log.getAfterData(), "disabled");
    }

    @Test
    @DisplayName("DEF-1 三写路径 detail 含引号/斜杠等字符仍产出合法 JSON（转义安全）")
    void detailWithSpecialCharsStaysValidJson() {
        GateElement exist = new GateElement();
        exist.setId(67L);
        exist.setElementCode("X\"Y\\Z");
        when(gateElementMapper.selectById(67L)).thenReturn(exist);

        service.disable(67L, "900101");

        AuditLog log = capturedAudit();
        assertValidJson(log.getAfterData(), "X\"Y\\Z");
    }
}
