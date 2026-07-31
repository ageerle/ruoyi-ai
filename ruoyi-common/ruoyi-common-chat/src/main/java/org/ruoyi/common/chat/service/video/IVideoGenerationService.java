package org.ruoyi.common.chat.service.video;

import jakarta.validation.Valid;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;

/**
 * Public text-to-video service.
 */
public interface IVideoGenerationService {

    MediaGenerationResponse generateVideo(@Valid VideoContext videoContext);

    MediaGenerationResponse retrieveVideo(@Valid VideoContext videoContext);

    String getProviderName();
}
