package com.xmzs.common.chat.domain.response;

import lombok.Data;

/**
 * @author WangLe
 */
@Data
public class MetadataResponse {
    private String promptMP3StorageUrl;
    private String promptOriginAudioStorageUrl;
    private String description;
    private boolean preProcess;
}
