package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.*;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.shortdrama.IShortDramaVideoComposeService;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaStoryboardEditingTest {
    ShortDramaServiceImpl service;
    ShortDramaProjectMapper projects;
    ShortDramaScriptMapper scripts;
    ShortDramaStoryboardMapper boards;
    ShortDramaProject project;
    ShortDramaStoryboard shot;
    @BeforeEach void setup() {
        service = mock(ShortDramaServiceImpl.class, CALLS_REAL_METHODS);
        projects = mock(ShortDramaProjectMapper.class); scripts = mock(ShortDramaScriptMapper.class);
        boards = mock(ShortDramaStoryboardMapper.class);
        ReflectionTestUtils.setField(service, "visualAssets", mock(ShortDramaVisualAssetService.class));
        ReflectionTestUtils.setField(service, "projectMapper", projects);
        ReflectionTestUtils.setField(service, "scriptMapper", scripts);
        ReflectionTestUtils.setField(service, "storyboardMapper", boards);
        ReflectionTestUtils.setField(service, "videoComposeService", mock(IShortDramaVideoComposeService.class));
        ReflectionTestUtils.setField(service, "storyboardGenerationStates", new ConcurrentHashMap<>());
        project = new ShortDramaProject(); project.setId(1L); project.setUserId(7L);
        when(projects.selectOne(any())).thenReturn(project);
        var script = new ShortDramaScript(); script.setId(2L); script.setProjectId(1L);
        when(scripts.selectById(2L)).thenReturn(script);
        shot = new ShortDramaStoryboard(); shot.setId(3L); shot.setProjectId(1L); shot.setScriptId(2L); shot.setSceneNo(1);
        when(boards.selectById(3L)).thenReturn(shot);
        when(boards.selectList(any())).thenReturn(List.of());
    }
    @Test void foreignOwnerCannotDelete() {
        assertThrows(IllegalArgumentException.class, () -> service.deleteStoryboard(3L, 8L));
        verify(boards, never()).deleteById(any(java.io.Serializable.class));
    }
    @Test void scriptFromAnotherProjectCannotBeUsed() {
        var other = new ShortDramaScript(); other.setProjectId(9L);
        when(scripts.selectById(2L)).thenReturn(other);
        assertThrows(IllegalArgumentException.class, () -> service.addStoryboard(1L, 2L, null, 7L));
    }
    @Test void pendingVideoCannotBeDeleted() {
        shot.setVideoStatus("submission_unknown");
        assertThrows(IllegalStateException.class, () -> service.deleteStoryboard(3L, 7L));
    }
    @Test void compositionCannotBeInterrupted() {
        project.setComposeStatus("processing");
        assertThrows(IllegalStateException.class, () -> service.deleteStoryboard(3L, 7L));
    }
    @Test void lastShotMayBeDeletedWithoutMinimumShotLimit() {
        service.deleteStoryboard(3L, 7L);
        verify(boards).deleteById(3L);
        verify(boards, never()).updateById(any(ShortDramaStoryboard.class));
    }
    @Test void missingInsertionAnchorIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> service.addStoryboard(1L, 2L, 999L, 7L));
    }
}
