package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.domain.vo.shortdrama.ShortDramaStoryboardVo;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaVideoContinuityTest {
    @Test
    void waitsForFirstShotBeforeSubmittingNextWithItsActualLastFrame() {
        var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        doReturn(video("generating", null)).when(service).generateVideo(1L, "model", 1L, null);
        doReturn(video("done", "frame-1")).when(service).ensureVideoDone(1L, "model", 1L);
        doReturn(video("done", "frame-2")).when(service).generateVideo(2L, "model", 1L, "frame-1");
        var result = service.generateGroupSerial(List.of(shot(1, 1), shot(2, 1)), "model", 1L);
        var order = inOrder(service);
        order.verify(service).generateVideo(1L, "model", 1L, null);
        order.verify(service).ensureVideoDone(1L, "model", 1L);
        order.verify(service).generateVideo(2L, "model", 1L, "frame-1");
        assertEquals(2, result.size());
    }

    @Test
    void missingLastFrameOrFailedShotNeverSubmitsDownstreamShot() {
        for (String status : List.of("done", "failed", "generating")) {
            var service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
            doReturn(video(status, null)).when(service).generateVideo(1L, "model", 1L, null);
            doReturn(video(status, null)).when(service).ensureVideoDone(1L, "model", 1L);
            assertThrows(IllegalStateException.class, () -> service.generateGroupSerial(
                List.of(shot(1, 1), shot(2, 1)), "model", 1L));
            verify(service, never()).generateVideo(eq(2L), anyString(), anyLong(), nullable(String.class));
        }
    }

    @Test
    void samePhysicalLocationAfterTimeJumpDoesNotInheritEarlierSceneFrame() {
        var groups = ShortDramaServiceImpl.groupContinuousScenes(List.of(shot(1, 1), shot(2, 1), shot(3, 2)));
        assertEquals(2, groups.size());
        assertEquals(2, groups.get(0).size());
        assertEquals(3L, groups.get(1).get(0).getId());
    }

    private static ShortDramaStoryboard shot(long id, int scene) {
        var shot = new ShortDramaStoryboard();
        shot.setId(id);
        shot.setSceneNo((int) id);
        shot.setLocationName("修理铺");
        shot.setContinuityJson("{\"scene_number\":" + scene + "}");
        return shot;
    }

    private static ShortDramaStoryboardVo video(String status, String frame) {
        var video = new ShortDramaStoryboardVo();
        video.setVideoStatus(status);
        video.setLastFrameUrl(frame);
        return video;
    }
}
