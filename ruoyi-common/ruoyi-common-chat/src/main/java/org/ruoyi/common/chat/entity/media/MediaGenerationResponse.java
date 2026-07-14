package org.ruoyi.common.chat.entity.media;

import lombok.Builder;
import lombok.Data;

/**
 * Unified media generation response.
 */
@Data
@Builder
public class MediaGenerationResponse {

    private String type;

    private String mimeType;

    private String url;

    private String b64Json;

    private String dataUrl;

    private String id;

    private String status;

    private String rawResponse;
}
