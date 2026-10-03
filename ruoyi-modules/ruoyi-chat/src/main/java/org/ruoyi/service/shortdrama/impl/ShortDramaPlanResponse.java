package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.service.media.AtlasMediaSupport;
import java.io.IOException;
import java.util.List;

/** A scene plan guides generation; it is not a post-generation shot-count limit. */
final class ShortDramaPlanResponse {
    private ShortDramaPlanResponse() {}

    static List<ShortDramaServiceImpl.StoryboardPanelData> parse(String json) throws IOException {
        var mapper = AtlasMediaSupport.OBJECT_MAPPER;
        var root = mapper.readTree(json);
        if (root == null) return null;
        var panels = root.isArray() ? root : root.path("panels");
        if (!panels.isArray()) return null;
        return mapper.readerForListOf(ShortDramaServiceImpl.StoryboardPanelData.class)
            .without(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(panels);
    }
}
