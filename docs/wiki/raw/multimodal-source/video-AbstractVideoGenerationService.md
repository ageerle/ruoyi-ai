---
source: file:///Users/mac/Documents/ruoyi-ai/ruoyi-modules/ruoyi-chat/src/main/java/org/ruoyi/service/video/AbstractVideoGenerationService.java
collected: 2026-09-04
published: 2026-09-04
topic: multimodal-source
---

# AbstractVideoGenerationService

```java
package org.ruoyi.service.video;

import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.chat.service.video.IVideoGenerationService;
import org.springframework.validation.annotation.Validated;

@Slf4j
@Validated
public abstract class AbstractVideoGenerationService implements IVideoGenerationService {

    @Override
    public MediaGenerationResponse generateVideo(VideoContext videoContext) {
        return doGenerateVideo(videoContext);
    }

    @Override
    public MediaGenerationResponse retrieveVideo(VideoContext videoContext) {
        return doRetrieveVideo(videoContext);
    }

    protected abstract MediaGenerationResponse doGenerateVideo(VideoContext videoContext);

    protected abstract MediaGenerationResponse doRetrieveVideo(VideoContext videoContext);
}

```
