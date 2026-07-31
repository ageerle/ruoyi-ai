package org.ruoyi.service.audio;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.service.audio.IAudioGenerationService;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
public abstract class AbstractAudioGenerationService implements IAudioGenerationService {

    @Override
    public MediaGenerationResponse generateSpeech(AudioContext audioContext) {
        return doGenerateSpeech(audioContext);
    }

    protected abstract MediaGenerationResponse doGenerateSpeech(AudioContext audioContext);
}
