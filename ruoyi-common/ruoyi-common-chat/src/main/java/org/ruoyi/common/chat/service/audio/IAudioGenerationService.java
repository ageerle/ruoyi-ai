package org.ruoyi.common.chat.service.audio;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;

/**
 * Public text-to-speech service.
 */
public interface IAudioGenerationService {

    MediaGenerationResponse generateSpeech(@Valid AudioContext audioContext);

    String getProviderName();
}
