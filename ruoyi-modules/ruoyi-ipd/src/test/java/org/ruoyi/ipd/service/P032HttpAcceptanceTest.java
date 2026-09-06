package org.ruoyi.ipd.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.ipd.controller.SystemConfigController;
import org.ruoyi.ipd.domain.SystemConfig;
import org.ruoyi.ipd.security.IpdPermission;
import org.ruoyi.ipd.security.IpdPermissionException;
import org.ruoyi.ipd.security.IpdPermissionExceptionHandler;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
@DisplayName("P032 SystemConfigController HTTP 端点验收")
class P032HttpAcceptanceTest {

    private SystemConfigService configService;
    private IpdPermission ipdPermission;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        configService = mock(SystemConfigService.class);
        ipdPermission = mock(IpdPermission.class);
        mvc = MockMvcBuilders
            .standaloneSetup(new SystemConfigController(configService, ipdPermission))
            .setControllerAdvice(new IpdPermissionExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("AC-CFG-03：GET list 超管可调回全量参数")
    void listAdminOk() throws Exception {
        SystemConfig c1 = SystemConfig.builder().id(1L).configKey("bonus.salesSource").configValue("SHIPMENT").build();
        SystemConfig c2 = SystemConfig.builder().id(2L).configKey("allowance.L3").configValue("2200").build();
        when(configService.list()).thenReturn(List.of(c1, c2));
        mvc.perform(get("/api/v1/system-configs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data[0].configKey").value("bonus.salesSource"))
            .andExpect(jsonPath("$.data[1].configKey").value("allowance.L3"));
    }

    @Test
    @DisplayName("GET list 非超管 → 403")
    void listNonAdminForbidden() throws Exception {
        doThrow(new IpdPermissionException(403, org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN))
            .when(ipdPermission).requireAdmin();
        mvc.perform(get("/api/v1/system-configs"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("AC-CFG-03：GET /{key} 返回当前值（即时生效）")
    void getByKeyReturnsCurrentValue() throws Exception {
        when(configService.getValue("gate.signDeadlineDays", "")).thenReturn("3");
        mvc.perform(get("/api/v1/system-configs/gate.signDeadlineDays"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.key").value("gate.signDeadlineDays"))
            .andExpect(jsonPath("$.data.value").value("3"));
    }

    @Test
    @DisplayName("AC-CFG-03：PUT 更新后立即返回新值 + invalidated=true")
    void updateReturnsNewValueAndInvalidated() throws Exception {
        when(configService.getValue(anyString(), anyString())).thenReturn("7");
        String body = "{\"value\":\"7\"}";
        mvc.perform(put("/api/v1/system-configs/gate.signDeadlineDays").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.key").value("gate.signDeadlineDays"))
            .andExpect(jsonPath("$.data.value").value("7"))
            .andExpect(jsonPath("$.data.invalidated").value("true"));
        verify(configService, times(1)).invalidate("gate.signDeadlineDays");
    }

    @Test
    @DisplayName("PUT update 非超管 → 403")
    void updateNonAdminForbidden() throws Exception {
        doThrow(new IpdPermissionException(403, org.ruoyi.ipd.common.ApiV1ErrorCode.FORBIDDEN))
            .when(ipdPermission).requireAdmin();
        String body = "{\"value\":\"7\"}";
        mvc.perform(put("/api/v1/system-configs/gate.signDeadlineDays").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("PUT 空 body → 400（@Valid 校验）")
    void updateMissingValueFieldRejects() throws Exception {
        mvc.perform(put("/api/v1/system-configs/gate.signDeadlineDays").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }
}
