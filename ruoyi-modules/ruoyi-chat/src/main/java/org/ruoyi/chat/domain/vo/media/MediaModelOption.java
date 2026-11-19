package org.ruoyi.chat.domain.vo.media;

/** Model selection data for the media workbench. */
public record MediaModelOption(
    String modelName,
    String modelDescribe,
    String providerCode,
    boolean supported,
    String queryType
) {
}
