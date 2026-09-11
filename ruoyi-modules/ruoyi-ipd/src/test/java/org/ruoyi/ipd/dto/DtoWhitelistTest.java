package org.ruoyi.ipd.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CODE-01 Part B：Controller 入参 DTO 白名单安全测试。
 *
 * <p>安全属性：客户端 JSON 携带服务端权威字段（code/status/currentStage/source/projectId/
 * tenantId/delFlag/id）时——
 * <ul>
 *   <li>@JsonIgnoreProperties(ignoreUnknown=true) 静默丢弃（防 schema 探测）</li>
 *   <li>Req 上根本没有这些字段 → 注入面为零（防越权写）</li>
 * </ul>
 * 修复前：Controller 直接收 Entity，Project.code/status/source 可被客户端注入。
 */
@Tag("dev")
class DtoWhitelistTest {

    private final ObjectMapper json = new ObjectMapper();

    @Test
    @DisplayName("ProjectCreateReq：恶意注入 code/status/currentStage/source/delFlag 全部被丢弃")
    void projectCreateReq_ignoresServerOwnedFields() throws Exception {
        String malicious = """
            {"name":"测试","productId":50,"level":"S","levelCoefficient":1.8,
             "levelCoefficientReason":"旗舰",
             "code":"PRJ-2099-001","status":"ACTIVE","currentStage":"LAUNCH",
             "source":"LEGACY","tenantId":"999999","delFlag":"1","id":777}
            """;
        ProjectCreateReq req = json.readValue(malicious, ProjectCreateReq.class);
        assertThat(req.name()).isEqualTo("测试");
        assertThat(req.levelCoefficient()).isEqualByComparingTo(new BigDecimal("1.8"));
        // 白名单外字段：静默丢弃，且 Req 无对应字段
        assertNoFields(ProjectCreateReq.class, "code", "status", "currentStage", "source", "tenantId", "delFlag", "id");
    }

    @Test
    @DisplayName("ProductCreateReq：恶意注入 projectId/status/delFlag 被丢弃")
    void productCreateReq_ignoresServerOwnedFields() throws Exception {
        String malicious = """
            {"productCode":"P01","productName":"门禁","groupId":11,
             "projectId":666,"status":"INACTIVE","delFlag":"1","id":888,"tenantId":"x"}
            """;
        ProductCreateReq req = json.readValue(malicious, ProductCreateReq.class);
        assertThat(req.productName()).isEqualTo("门禁");
        assertNoFields(ProductCreateReq.class, "projectId", "status", "tenantId", "delFlag", "id");
    }

    private static void assertNoFields(Class<?> type, String... banned) {
        for (java.lang.reflect.Field f : type.getDeclaredFields()) {
            for (String b : banned) {
                assertThat(f.getName())
                    .as("%s must not declare field '%s'", type.getSimpleName(), b)
                    .isNotEqualTo(b);
            }
        }
    }

    @Test
    @DisplayName("GateElementCreateReq：恶意注入 enabled 越权字段外全丢弃；UpdateReq 不收编码")
    void gateElementReqs_ignoresServerOwnedFields() throws Exception {
        String maliciousCreate = """
            {"gateCode":"G1","elementCode":"GE01","elementName":"要素",
             "id":999,"tenantId":"x","delFlag":"1"}
            """;
        GateElementCreateReq create = json.readValue(maliciousCreate, GateElementCreateReq.class);
        assertThat(create.elementCode()).isEqualTo("GE01");

        // update 不允许改 gateCode/elementCode（编码不可变）
        String maliciousUpdate = """
            {"elementName":"新名","gateCode":"G5","elementCode":"GE99","id":999}
            """;
        GateElementUpdateReq update = json.readValue(maliciousUpdate, GateElementUpdateReq.class);
        assertThat(update.elementName()).isEqualTo("新名");
        assertNoFields(GateElementUpdateReq.class, "gateCode", "elementCode", "id");
    }
}
