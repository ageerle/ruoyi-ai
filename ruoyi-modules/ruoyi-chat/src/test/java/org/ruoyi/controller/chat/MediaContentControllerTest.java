package org.ruoyi.controller.chat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.service.media.AtlasMediaContentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class MediaContentControllerTest {
    @Test
    void returnsInlineNonCachedBytes() throws Exception {
        var models = mock(IChatModelService.class);
        var service = mock(AtlasMediaContentService.class);
        var model = new ChatModelVo();
        when(models.selectModelByName("image-model")).thenReturn(model);
        when(service.retrieve(model, "task-123")).thenReturn(new AtlasMediaContentService.Content(new byte[]{1, 2}, "image/jpeg"));
        var result = new MediaContentController(models, service).content("image-model", "task-123");
        assertEquals("image/jpeg", result.getHeaders().getContentType().toString());
        assertEquals("inline", result.getHeaders().getFirst("Content-Disposition"));
        assertEquals("no-store", result.getHeaders().getCacheControl());
        assertEquals("nosniff", result.getHeaders().getFirst("X-Content-Type-Options"));
        assertEquals(2, result.getHeaders().getContentLength());
        assertArrayEquals(new byte[]{1, 2}, result.getBody());
    }
}
