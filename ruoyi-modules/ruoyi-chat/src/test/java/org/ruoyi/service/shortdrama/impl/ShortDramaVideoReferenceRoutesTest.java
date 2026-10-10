package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.controller.shortdrama.ShortDramaVideoReferenceController;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("dev")
class ShortDramaVideoReferenceRoutesTest {
    @Test void listRouteUsesCurrentUserAndReturnsReferencesWithoutAModelCall() throws Exception {
        var references = mock(ShortDramaVideoReferenceService.class);
        when(references.list(10L, 1L)).thenReturn(List.of());
        var mvc = MockMvcBuilders.standaloneSetup(new ShortDramaVideoReferenceController(references)).build();
        try (var login = mockStatic(LoginHelper.class)) {
            login.when(LoginHelper::getUserId).thenReturn(1L);
            mvc.perform(get("/short-drama/10/video-references"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isEmpty());
        }
        verify(references).list(10L, 1L);
        verifyNoMoreInteractions(references);
    }

    @Test void ownershipAndAtlasChecksRunBeforeReadingOrUploadingReferences() {
        var projects = mock(ShortDramaProjectMapper.class);
        var images = mock(ImageServiceFactory.class);
        var project = new ShortDramaProject(); project.setUserId(2L);
        when(projects.selectById(10L)).thenReturn(project);
        var references = new ShortDramaVideoReferenceService(projects, images);
        assertThrows(IllegalArgumentException.class, () -> references.list(10L, 1L));
        var model = new ChatModelVo(); model.setProviderCode("openai");
        assertThrows(IllegalArgumentException.class, () -> references.transport(10L, model, List.of()));
        verifyNoInteractions(images);
    }
}
