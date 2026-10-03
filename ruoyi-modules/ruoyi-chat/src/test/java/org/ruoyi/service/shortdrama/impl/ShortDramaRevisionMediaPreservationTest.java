package org.ruoyi.service.shortdrama.impl;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import io.github.linpeilie.Converter;
import org.ruoyi.domain.bo.shortdrama.ShortDramaRevisionBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaStoryboardBo;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("dev")
class ShortDramaRevisionMediaPreservationTest {
    @Test void reviewedPromptRevisionKeepsPaidMediaAndFrameAssociation() {
        var projects = mock(ShortDramaProjectMapper.class);
        var scripts = mock(ShortDramaScriptMapper.class);
        var boards = mock(ShortDramaStoryboardMapper.class);
        var frames = mock(ShortDramaVisualAssetMapper.class);
        var visual = mock(ShortDramaVisualAssetService.class);
        var drama = mock(IShortDramaService.class);
        var transactions = mock(TransactionTemplate.class);
        doAnswer(call -> {
            Consumer<TransactionStatus> action = call.getArgument(0);
            action.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactions).executeWithoutResult(any());
        var service = new ShortDramaRevisionService(projects, scripts, boards,
            mock(ShortDramaCharacterMapper.class), mock(ShortDramaLocationMapper.class),
            frames, visual, drama, transactions);

        var project = new ShortDramaProject();
        project.setId(1L); project.setUserId(7L); project.setStatus("storyboard_ready");
        project.setComposeStatus("done"); project.setComposedVideoOssId(99L);
        when(projects.selectById(1L)).thenReturn(project);
        var script = new ShortDramaScript();
        script.setId(2L); script.setProjectId(1L); script.setScriptText("原剧本");
        when(scripts.selectById(2L)).thenReturn(script);

        var current = new ShortDramaStoryboard();
        current.setId(3L); current.setProjectId(1L); current.setScriptId(2L); current.setSceneNo(1);
        current.setUpdateTime(new Date(123456789L)); current.setDurationSeconds(5);
        current.setSourceText("角色把茶盏放回桌沿。"); current.setImagePrompt("原身份与空间");
        current.setContinuityJson("{}"); current.setVideoPrompt("旧导演稿");
        current.setVideoId("paid-task"); current.setVideoStatus("done");
        current.setVideoUrl("https://example.com/approved.mp4");
        current.setLastFrameUrl("https://example.com/approved-last.png");
        when(boards.selectList(any())).thenReturn(List.of(current));

        var incoming = new ShortDramaStoryboardBo();
        incoming.setId(3L); incoming.setProjectId(1L); incoming.setScriptId(2L); incoming.setSceneNo(1);
        incoming.setUpdateTime(current.getUpdateTime()); incoming.setDurationSeconds(5);
        incoming.setSourceText(current.getSourceText()); incoming.setImagePrompt(current.getImagePrompt());
        incoming.setContinuityJson(current.getContinuityJson());
        incoming.setVideoPrompt("茶馆内平视中景，角色把茶盏放回桌沿，杯底擦过旧木，近处杯声轻响，窗光照在衣袖上；手离开后停在杯沿。");
        var revision = new ShortDramaRevisionBo();
        revision.setScriptId(2L); revision.setExpectedScriptText("原剧本");
        revision.setScriptText("原剧本"); revision.setStoryboards(List.of(incoming));
        revision.setTone("真人影视写实"); revision.setWorldbuilding("保留旧版身份，采用本轮写实材质");
        revision.setRevisionNotes("仅修订描述，媒体另留候选");
        var factory = new org.springframework.beans.factory.support.DefaultListableBeanFactory();
        var converter = mock(Converter.class);
        when(converter.convert(any(Object.class), any(Class.class))).thenAnswer(call -> {
            Object target = ((Class<?>) call.getArgument(1)).getDeclaredConstructor().newInstance();
            org.springframework.beans.BeanUtils.copyProperties(call.getArgument(0), target);
            return target;
        });
        factory.registerSingleton("converter", converter);
        new cn.hutool.extra.spring.SpringUtil().postProcessBeanFactory(factory);
        service.apply(1L, revision, 7L);
        var capture = ArgumentCaptor.forClass(ShortDramaStoryboard.class);
        verify(boards).updateById(capture.capture());
        var saved = capture.getValue();
        assertEquals("paid-task", saved.getVideoId());
        assertEquals("done", saved.getVideoStatus());
        assertEquals(current.getVideoUrl(), saved.getVideoUrl());
        assertEquals(current.getLastFrameUrl(), saved.getLastFrameUrl());
        verify(boards, never()).update(any(), any());
        verifyNoInteractions(frames);
        verify(visual).synchronizeExistingFrames(1L, java.util.Set.of());
        var scriptCapture = ArgumentCaptor.forClass(ShortDramaScript.class);
        verify(scripts).updateById(scriptCapture.capture());
        assertEquals(revision.getTone(), scriptCapture.getValue().getTone());
        assertEquals(revision.getWorldbuilding(), scriptCapture.getValue().getWorldbuilding());
        assertEquals(revision.getRevisionNotes(), scriptCapture.getValue().getRevisionNotes());
        var projectCapture = ArgumentCaptor.forClass(ShortDramaProject.class);
        verify(projects).updateById(projectCapture.capture());
        assertEquals("done", projectCapture.getValue().getComposeStatus());
        assertEquals(99L, projectCapture.getValue().getComposedVideoOssId());
    }
}
