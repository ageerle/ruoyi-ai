package org.ruoyi.controller.shortdrama;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Tag("dev")
class ShortDramaLocationImageRequestTest {
    @Test void allThreeSubmissionEndpointsPassOptionalRequestOnlyRequirementsAndKeepBodyOmissionCompatible() throws Exception {
        var service = mock(IShortDramaService.class); var controller = mock(ShortDramaController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controller, "shortDramaService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            for (String route : new String[]{"generate-image", "regenerate"}) {
                mvc.perform(post("/short-drama/location/50/" + route).param("model", "edit-model")
                    .contentType(MediaType.APPLICATION_JSON).content("{\"revisionRequirements\":\"改为清晨冷蓝光\"}"))
                    .andExpect(status().isOk());
            }
            mvc.perform(post("/short-drama/image/start").param("assetType", "location").param("assetId", "50").param("model", "edit-model")
                .contentType(MediaType.APPLICATION_JSON).content("{\"referencePurpose\":\"location_revision\",\"revisionRequirements\":\"清空错误武器\"}"))
                .andExpect(status().isOk());
            mvc.perform(post("/short-drama/location/50/generate-image").param("model", "edit-model")).andExpect(status().isOk());
        }
        verify(service).generateLocationImage(50L, "edit-model", null, "改为清晨冷蓝光", 1L);
        verify(service).regenerateLocationImage(50L, "edit-model", null, "改为清晨冷蓝光", 1L);
        verify(service).startImageGeneration("location", 50L, "edit-model", null, "location_revision", "清空错误武器", null, 1L);
        verify(service).generateLocationImage(50L, "edit-model", null, null, 1L);
    }
    @Test void oversizedRequirementsAreRejectedByBothLocationAndSharedDtoBeforeServiceSubmission() throws Exception {
        var service = mock(IShortDramaService.class); var controller = mock(ShortDramaController.class, CALLS_REAL_METHODS);
        ReflectionTestUtils.setField(controller, "shortDramaService", service);
        var mvc = MockMvcBuilders.standaloneSetup(controller).build();
        String json = "{\"revisionRequirements\":\"" + "光".repeat(4001) + "\"}";
        for (String route : new String[]{"generate-image", "regenerate"})
            mvc.perform(post("/short-drama/location/50/" + route).param("model", "edit-model").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/short-drama/image/start").param("assetType", "location").param("assetId", "50").param("model", "edit-model")
            .contentType(MediaType.APPLICATION_JSON).content(json)).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
