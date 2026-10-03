package org.ruoyi.controller.shortdrama;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.common.web.handler.GlobalExceptionHandler;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class ShortDramaReviewedImportRequestTest {
    private static final String BODY = "{\"scriptId\":20,\"expectedScriptText\":\"外景 修渠处（预计5秒）\",\"panels\":[{\"scene_number\":1,\"panel_number\":1,\"duration\":5}]}";
    @Test void bothNormalRoutesReturnKnownBindingFailureAndRetainSuccessResponse() throws Exception {
        var service = mock(IShortDramaService.class); var controller = mock(ShortDramaController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controller, "shortDramaService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            when(service.importReviewedPlan(eq(10L), any(), eq(1L))).thenThrow(new IllegalArgumentException("场景绑定无法唯一核对:县城城头及城外郊野_秋日"));
            for (String route : new String[]{"import-reviewed-plan", "plan-storyboard/review"})
                mvc.perform(post("/short-drama/10/" + route).contentType(MediaType.APPLICATION_JSON).content(BODY))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.msg", containsString("场景绑定无法唯一核对")))
                    .andExpect(jsonPath("$.msg", containsString("县城城头及城外郊野_秋日")));
            when(service.importReviewedPlan(eq(10L), any(), eq(1L))).thenReturn(3);
            mvc.perform(post("/short-drama/10/import-reviewed-plan").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200)).andExpect(jsonPath("$.data").value(3));
        }
    }
    @Test void credentialsAndOversizedMessagesAreSanitizedWhileUnexpectedInfrastructureErrorsStayGlobal() throws Exception {
        var service = mock(IShortDramaService.class); var controller = mock(ShortDramaController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controller, "shortDramaService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new GlobalExceptionHandler()).build();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            when(service.importReviewedPlan(eq(10L), any(), eq(1L))).thenThrow(new IllegalStateException("当前资产签名已变化 Authorization: Bearer secret-bearer password=secret-password\n" + "甲".repeat(3000)));
            var result = mvc.perform(post("/short-drama/10/import-reviewed-plan").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", containsString("当前资产签名已变化")))
                .andExpect(jsonPath("$.msg", not(containsString("secret-bearer"))))
                .andExpect(jsonPath("$.msg", not(containsString("secret-password")))).andReturn();
            var message = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8)).path("msg").asText();
            org.junit.jupiter.api.Assertions.assertTrue(message.length() <= 2048);
            when(service.importReviewedPlan(eq(10L), any(), eq(1L))).thenThrow(new IllegalStateException("driver pool internal-host password=private-pool-secret"));
            mvc.perform(post("/short-drama/10/import-reviewed-plan").contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg", not(containsString("internal-host"))))
                .andExpect(jsonPath("$.msg", not(containsString("private-pool-secret"))));
        }
    }
}
