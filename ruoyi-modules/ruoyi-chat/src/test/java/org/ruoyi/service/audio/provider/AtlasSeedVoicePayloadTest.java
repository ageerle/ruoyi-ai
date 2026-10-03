package org.ruoyi.service.audio.provider;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class AtlasSeedVoicePayloadTest {
    private AudioContext context(List<Map<String,String>> references) {var model=new ChatModelVo();model.setModelName("bytedance/seed-audio-1.0");return AudioContext.builder().chatModelVo(model).input("只说正文").references(references).build();}
    @Test void imageUrlAndAudioDataUseExactlyOneDocumentedSource() {
        var image=AtlasSpeechPayloadBuilder.build(context(List.of(Map.of("imageUrl","https://example.com/role.png"))));
        assertEquals("https://example.com/role.png",image.path("references").get(0).path("image_url").asText());
        var audio=AtlasSpeechPayloadBuilder.build(context(List.of(Map.of("audioData","YWJj"))));
        assertEquals("YWJj",audio.path("references").get(0).path("audio_data").asText());
        assertFalse(audio.has("voice"));
    }
    @Test void rejectsMixedSourcesAndTooManyReferences() {
        assertThrows(IllegalArgumentException.class,()->AtlasSpeechPayloadBuilder.build(context(List.of(Map.of("speaker","voice","audioUrl","https://example.com/a.mp3")))));
        assertThrows(IllegalArgumentException.class,()->AtlasSpeechPayloadBuilder.build(context(List.of(Map.of("imageUrl","https://example.com/a.png"),Map.of("audioData","YWJj")))));
        assertThrows(IllegalArgumentException.class,()->AtlasSpeechPayloadBuilder.build(context(Collections.nCopies(4,Map.of("audioData","YWJj")))));
    }
}
