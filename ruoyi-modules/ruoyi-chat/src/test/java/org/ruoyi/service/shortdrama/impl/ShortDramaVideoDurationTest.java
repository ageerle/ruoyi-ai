package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaVideoDurationTest {
    @Test void estimatesNeverBecomeVideoSeconds() {
        assertNull(ShortDramaVideoDuration.seconds(null));
        assertNull(ShortDramaVideoDuration.seconds("{\"duration\":15,\"timing\":{\"action_seconds\":15}}"));
        assertNull(ShortDramaVideoDuration.seconds("{\"video_seconds\":null}"));
        assertTrue(ShortDramaVideoDuration.direction("{}").contains("完整保留"));
        assertFalse(ShortDramaVideoDuration.direction("{}").contains("15秒"));
    }
    @Test void onlyExplicitPositiveIntegersAndAutoArePassedThrough() {
        assertEquals(3,ShortDramaVideoDuration.seconds("{\"video_seconds\":3}"));
        assertEquals(75,ShortDramaVideoDuration.seconds("{\"video_seconds\":75}"));
        assertEquals(-1,ShortDramaVideoDuration.seconds("{\"video_seconds\":-1}"));
        assertNull(ShortDramaVideoDuration.reviewSeconds("{\"video_seconds\":-1}"));
        for(String value:new String[]{"0","-2","3.5","\"15\"","2147483648"})
            assertThrows(IllegalArgumentException.class,()->ShortDramaVideoDuration.seconds("{\"video_seconds\":"+value+"}"));
    }
    @Test void optionalSecondsStillReviewActionsAndOriginalDialogue() {
        String prose="御书房中景固定，朱承晏放下文书，烛光照到手指，低声说「请父皇准我查账。」父皇静听，环境声轻微。";
        assertTrue(ShortDramaVideoPromptReview.issues(prose,null,ShortDramaVideoPromptReviewTest.SOURCE).isEmpty());
        assertFalse(ShortDramaVideoPromptReview.issues(prose.replace("请父皇准我查账。","查一下。"),null,ShortDramaVideoPromptReviewTest.SOURCE).isEmpty());
        var anchored=ShortDramaVideoPromptReview.anchoredPrompt(prose,null,ShortDramaVideoPromptReviewTest.SOURCE,"站立","手留纸边");
        assertFalse(anchored.contains("【目标时长】"));
        assertTrue(anchored.contains("未指定视频秒数"));
    }
}
