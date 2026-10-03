package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaAssetGenerationTest {
    static final String RESULT = """
        {"characters":[{"name":"匠人","visualDescription":"竹护腕，衣襟含\\\"结\\\"与{扣}","introduction":"匠人"}],
        "locations":[{"name":"山谷","descriptions":["古代山谷"],"availableSlots":["空中"]}],
        "props":[{"name":"风筝","description":"竹骨与布翼"},{"name":"绳索","description":"麻绳连接竹骨"}]}
        """;
    @Test void validatesTheEntireSingleResultBeforeAnySave() {
        var result = ShortDramaAssetGeneration.parse(RESULT);
        assertEquals(4, result.size()); assertEquals(2, result.props().size());
        assertThrows(IllegalStateException.class, () -> ShortDramaAssetGeneration.parse(RESULT.replace("麻绳连接竹骨", "")));
        assertThrows(IllegalStateException.class, () -> ShortDramaAssetGeneration.parse(RESULT.substring(0, RESULT.length() - 4)));
        assertThrows(IllegalStateException.class, () -> ShortDramaAssetGeneration.parse("{\"characters\":[],\"locations\":[],\"props\":[]}"));
    }
    @Test void incrementalPreviewsSurviveEveryChunkBoundaryAndNestedArrays() {
        var assets = new ArrayList<String>();
        var preview = new ShortDramaAssetGeneration.Preview((category, item) -> assets.add(category + ":" + item.path("name").asText()));
        for (char character : RESULT.toCharArray()) preview.accept(String.valueOf(character));
        assertEquals(List.of("1:匠人", "2:山谷", "3:风筝", "3:绳索"), assets);
    }
    @Test void flashDisablesReasoningEvenForLongScripts() {
        var request = ShortDramaWritingRequest.forText("deepseek-ai/deepseek-v4.1-flash", "正文".repeat(5000));
        assertEquals(false, request.getEnableThinking()); assertEquals("none", request.getReasoningEffort());
    }
}
