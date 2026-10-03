package org.ruoyi.service.shortdrama.impl;

import org.ruoyi.service.media.AtlasMediaSupport;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialThinking;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;

import org.ruoyi.common.chat.domain.dto.request.ChatRequest;
import org.ruoyi.common.chat.domain.vo.chat.ChatModelVo;
import org.ruoyi.common.chat.entity.image.ImageContext;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.chat.entity.video.VideoContext;
import org.ruoyi.common.chat.factory.ImageServiceFactory;
import org.ruoyi.common.chat.factory.VideoServiceFactory;
import org.ruoyi.common.chat.factory.AudioServiceFactory;
import org.ruoyi.common.chat.entity.audio.AudioContext;
import org.ruoyi.common.chat.service.chat.IChatModelService;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.service.media.AtlasMediaSupport;
import org.ruoyi.constant.ShortDramaImageConstants;
import org.ruoyi.domain.bo.shortdrama.ShortDramaIdeaBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaProjectBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptResult;
import org.ruoyi.domain.bo.shortdrama.ShortDramaStoryboardBo;
import org.ruoyi.domain.entity.shortdrama.ShortDramaAudio;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacter;
import org.ruoyi.domain.entity.shortdrama.ShortDramaCharacterAppearance;
import org.ruoyi.domain.entity.shortdrama.ShortDramaLocation;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaScript;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.domain.vo.shortdrama.ShortDramaAudioVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterAppearanceVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaLocationVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaProjectVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaScriptVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaStoryboardVo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaAudioBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterAppearanceBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaLocationBo;
import org.ruoyi.mapper.shortdrama.ShortDramaAudioMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaCharacterMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaCharacterAppearanceMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaLocationMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaScriptMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.factory.ChatServiceFactory;
import org.ruoyi.service.chat.AbstractChatService;
import org.ruoyi.service.media.AtlasPredictionService;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.ruoyi.service.shortdrama.IShortDramaVideoComposeService;
import org.springframework.stereotype.Service;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortDramaServiceImpl implements IShortDramaService {

    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate;
    private final ShortDramaProjectMapper projectMapper;
    private final ShortDramaScriptMapper scriptMapper;
    private final ShortDramaStoryboardMapper storyboardMapper;
    private final ShortDramaCharacterMapper characterMapper;
    private final ShortDramaCharacterAppearanceMapper characterAppearanceMapper;
    private final ShortDramaLocationMapper locationMapper;
    private final ShortDramaAudioMapper audioMapper;
    private final ShortDramaVisualAssetService visualAssets;
    private final ShortDramaSoundService sounds;
    private final ShortDramaCharacterVoiceService characterVoices;
    private final ShortDramaSkillCatalog skillCatalog;
    private final IChatModelService chatModelService;
    private final ChatServiceFactory chatServiceFactory;
    private final VideoServiceFactory videoServiceFactory;
    private final ImageServiceFactory imageServiceFactory;
    private final AudioServiceFactory audioServiceFactory;
    private final AtlasPredictionService atlasPredictionService;
    private final IShortDramaVideoComposeService videoComposeService;
    private final org.ruoyi.common.core.service.OssService ossService;
    private final java.util.Map<SseEmitter, AtomicBoolean> activeEmitters = new ConcurrentHashMap<>();
    private final java.util.Map<Long, AtomicBoolean> storyboardGenerationStates = new ConcurrentHashMap<>();
    private final java.util.Map<Long, ShortDramaSkillCatalog.Snapshot> planningSkillSnapshots = new ConcurrentHashMap<>();
    private final java.util.concurrent.Executor storyboardPlanningExecutor = java.util.concurrent.ForkJoinPool.commonPool();
    private final ShortDramaPlanningStatus planningStatuses = new ShortDramaPlanningStatus();
    private final ShortDramaPlanningStatus assetStatuses = new ShortDramaPlanningStatus("asset-status-v1");
    private final java.util.Map<Long, AtomicBoolean> assetGenerationStates = new ConcurrentHashMap<>();
    private final java.util.concurrent.ConcurrentHashMap<SseEmitter, ShortDramaPlanningProgress.Tracker> planningProgress = new java.util.concurrent.ConcurrentHashMap<>();
    private final ShortDramaSceneCandidateDiagnostics sceneCandidates = new ShortDramaSceneCandidateDiagnostics();
    private final ShortDramaVideoSubmissionStore videoSubmissions = new ShortDramaVideoSubmissionStore();
    private final ShortDramaImagePromptEvidence imagePromptEvidence = new ShortDramaImagePromptEvidence();
    private final ShortDramaSceneCheckpoint sceneCheckpoints = new ShortDramaSceneCheckpoint();

    /**
     * 代理流式输出视频，绕过 OSS 强制下载 header。
     * TODO: 待实现，当前为占位以满足接口契约。
     */
    @Override
    public StreamingResponseBody streamVideo(Long storyboardId, Long userId) {
        throw new UnsupportedOperationException("streamVideo 尚未实现");
    }

    // ==================== 查询 ====================

    @Override
    public List<ShortDramaProjectVo> listProjects(Long userId) {
        return projectMapper.selectVoList(new LambdaQueryWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getUserId, userId)
            .orderByDesc(ShortDramaProject::getId));
    }

    @Override
    public ShortDramaDetailVo getDetail(Long projectId, Long userId) {
        ShortDramaProject project = projectMapper.selectOne(new LambdaQueryWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .eq(ShortDramaProject::getUserId, userId));
        if (project == null) {
            return null;
        }
        ShortDramaScript script = scriptMapper.selectOne(new LambdaQueryWrapper<ShortDramaScript>()
            .eq(ShortDramaScript::getProjectId, projectId)
            .orderByDesc(ShortDramaScript::getId)
            .last("limit 1"));
        ShortDramaDetailVo detailVo = new ShortDramaDetailVo();
        detailVo.setProject(MapstructUtils.convert(project, ShortDramaProjectVo.class));
        detailVo.setScript(script == null ? null : MapstructUtils.convert(script, ShortDramaScriptVo.class));

        List<ShortDramaCharacterVo> characters = characterMapper.selectVoList(new LambdaQueryWrapper<ShortDramaCharacter>()
            .eq(ShortDramaCharacter::getProjectId, projectId));
        for (ShortDramaCharacterVo characterVo : characters) {
            List<ShortDramaCharacterAppearanceVo> appearances = characterAppearanceMapper.selectVoList(
                new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                    .eq(ShortDramaCharacterAppearance::getCharacterId, characterVo.getId())
                    .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            characterVo.setAppearances(appearances);
        }
        detailVo.setCharacters(characters);

        List<ShortDramaLocationVo> locations = locationMapper.selectVoList(new LambdaQueryWrapper<ShortDramaLocation>()
            .eq(ShortDramaLocation::getProjectId, projectId));
        detailVo.setLocations(locations);

        List<ShortDramaAudioVo> audios = audioMapper.selectVoList(new LambdaQueryWrapper<ShortDramaAudio>()
            .eq(ShortDramaAudio::getProjectId, projectId)
            .orderByAsc(ShortDramaAudio::getId));
        detailVo.setAudios(audios);

        List<ShortDramaStoryboardVo> storyboards = storyboardMapper.selectVoList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getProjectId, projectId)
            .orderByAsc(ShortDramaStoryboard::getSceneNo));
        detailVo.setStoryboards(storyboards);
        return detailVo;
    }

    // ==================== 创意生成：仅保存剧本，后续阶段由用户显式触发 ====================

    @Override
    public ShortDramaDetailVo createFromIdea(ShortDramaIdeaBo bo, Long userId) {
        ChatModelVo modelVo = validateAndGetModel(bo.getModel());
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), sanitizeIdeaInput(bo.getIdea())));

        // Phase 1: 固定格式剧本生成
        ShortDramaScriptResult polishResult = executePhase1_ScriptPolish(chatModel, bo);
        ShortDramaProject project = buildAndInsertProject(userId, polishResult, bo);
        ShortDramaScript script = buildAndInsertScript(project.getId(), polishResult, bo);

        return getDetail(project.getId(), userId);
    }

    @Override
    public SseEmitter createFromIdeaStream(ShortDramaIdeaBo bo, Long userId) {
        SseEmitter emitter = new SseEmitter(7_200_000L);
        AtomicBoolean emitterActive = new AtomicBoolean(true);
        activeEmitters.put(emitter, emitterActive);
        emitter.onCompletion(() -> closeEmitter(emitter));
        emitter.onTimeout(() -> closeEmitter(emitter));
        emitter.onError(error -> closeEmitter(emitter));

        final String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
        var progress = new ShortDramaScriptProgress(snapshot -> sendEmitterEvent(emitter,
            SseEmitter.event().name("progress").data(org.ruoyi.common.json.utils.JsonUtils.toJsonString(snapshot))));
        CompletableFuture.runAsync(() -> org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, () -> {
            Long projectId = null;
            try {
                ChatModelVo modelVo = validateAndGetModel(bo.getModel());
                AbstractChatService chatService = getChatService(modelVo);
                ChatRequest streamRequest = ShortDramaWritingRequest.forText(modelVo.getModelName(), sanitizeIdeaInput(bo.getIdea()));
                StreamingChatModel streamingModel = chatService.buildStreamingChatModel(modelVo, streamRequest);

                // Phase 1: 单次流式调用（JSON 元信息 → 分隔符 → 剧本正文逐字推送）
                emit(emitter, "polish", "running", "正在生成固定格式剧本...");
                ShortDramaScriptResult polishResult = executePhase1_Streaming(streamingModel, bo, emitter, progress);
                progress.state("saving");
                ShortDramaProject project = buildAndInsertProject(userId, polishResult, bo);
                projectId = project.getId();
                ShortDramaScript script = buildAndInsertScript(projectId, polishResult, bo);
                progress.state("done");
                emit(emitter, "polish", "done", "剧本生成完成");

                // 创建接口始终止于剧本。旧客户端的 scriptOnly=false 也不能提前分析资产或生成分镜。
                sendEmitterEvent(emitter, SseEmitter.event()
                    .name("complete")
                    .data("{\"projectId\":\"" + projectId + "\"}"));
                completeEmitter(emitter);
            } catch (Exception e) {
                progress.state("error");
                log.error("流式创建短剧失败", e);
                String errorData = projectId != null
                    ? "{\"message\":\"" + escapeJson(e.getMessage()) + "\",\"projectId\":\"" + projectId + "\"}"
                    : "{\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
                sendEmitterEvent(emitter, SseEmitter.event().name("error").data(errorData));
                completeEmitterWithError(emitter, e);
            }
        }));

        return emitter;
    }

    private boolean sendEmitterEvent(SseEmitter emitter, SseEmitter.SseEventBuilder event) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.get()) return false;
        try {
            emitter.send(event);
            return true;
        } catch (IOException | IllegalStateException e) {
            closeEmitter(emitter);
            return false;
        }
    }

    private void closeEmitter(SseEmitter emitter) {
        AtomicBoolean active = activeEmitters.remove(emitter);
        if (active != null) active.set(false);
    }

    private void completeEmitter(SseEmitter emitter) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.compareAndSet(true, false)) return;
        activeEmitters.remove(emitter);
        try { emitter.complete(); } catch (IllegalStateException ignored) { }
    }

    private void completeEmitterWithError(SseEmitter emitter, Throwable error) {
        AtomicBoolean active = activeEmitters.get(emitter);
        if (active == null || !active.compareAndSet(true, false)) return;
        activeEmitters.remove(emitter);
        try { emitter.completeWithError(error); } catch (IllegalStateException ignored) { }
    }

    private void emit(SseEmitter emitter, String phase, String status, String message) {
        String data = "{\"phase\":\"" + phase + "\",\"status\":\"" + status + "\",\"message\":\"" + escapeJson(message) + "\"}";
        var progress = emitter == null ? null : planningProgress.get(emitter);
        if (progress != null && phase.startsWith("storyboard_") && "running".equals(status)) progress.phase(phase, message);
        sendEmitterEvent(emitter, SseEmitter.event().name("phase").data(data));
    }

    private void emitStream(SseEmitter emitter, String phase, String text) {
        String data = JsonUtils.toJsonString(Map.of("phase", phase, "text", text));
        sendEmitterEvent(emitter, SseEmitter.event().name("stream").data(data));
    }

    private void emitStreamDone(SseEmitter emitter) {
        sendEmitterEvent(emitter, SseEmitter.event().name("stream")
            .data("{\"phase\":\"script\",\"status\":\"done\"}"));
    }

    /** 增量推送一个已完成的分镜 panel 给前端（流式规划时第一个完成就展示） */
    private void emitPanel(SseEmitter emitter, StoryboardPanelData panel) {
        if (emitter == null || panel == null) return;
        String data = "{\"phase\":\"storyboard_plan\",\"status\":\"panel\",\"panel\":" + JsonUtils.toJsonString(panel) + "}";
        sendEmitterEvent(emitter, SseEmitter.event().name("panel").data(data));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }

    private ShortDramaProject buildAndInsertProject(Long userId, ShortDramaScriptResult polishResult, ShortDramaIdeaBo bo) {
        ShortDramaProject project = new ShortDramaProject();
        project.setId(IdUtil.getSnowflakeNextId());
        project.setUserId(userId);
        project.setProjectName(firstNotBlank(bo.getProjectName(), polishResult.getProjectName(), "短剧项目"));
        project.setDescription(firstNotBlank(polishResult.getDescription(), ""));
        project.setOriginalIdea(bo.getIdea());
        project.setArtStyle(firstNotBlank(bo.getArtStyle(), "script-tone"));
        project.setAestheticSkillName(bo.getAestheticSkillName());
        project.setDirectorSkillName(bo.getDirectorSkillName());
        validateSkillBindings(project);
        project.setComposeAspectRatio(normalizeAspectRatio(bo.getAspectRatio()));
        project.setStatus("draft");
        projectMapper.insert(project);
        return project;
    }

    private ShortDramaScript buildAndInsertScript(Long projectId, ShortDramaScriptResult polishResult, ShortDramaIdeaBo bo) {
        ShortDramaScript script = new ShortDramaScript();
        script.setId(IdUtil.getSnowflakeNextId());
        script.setProjectId(projectId);
        script.setScriptName(firstNotBlank(polishResult.getScriptName(), "剧本"));
        script.setOutlineText(firstNotBlank(polishResult.getOutlineText(), ""));
        script.setScriptText(firstNotBlank(polishResult.getScriptText(), ""));
        script.setTone(firstNotBlank(polishResult.getTone(), "短剧"));
        script.setSourceType("llm");
        scriptMapper.insert(script);
        return script;
    }

    @Override
    public ShortDramaDetailVo polishScript(Long projectId, Long userId, String instruction) {
        ShortDramaProject project = validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectOne(new LambdaQueryWrapper<ShortDramaScript>()
            .eq(ShortDramaScript::getProjectId, projectId)
            .orderByDesc(ShortDramaScript::getId).last("limit 1"));
        if (StrUtil.isBlank(instruction)) throw new IllegalArgumentException("修改意见不能为空");
        String idea = script != null ? firstNotBlank(script.getScriptText(), script.getOutlineText(), project.getDescription()) : project.getDescription();
        ChatModelVo modelVo = findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), null));

        ShortDramaIdeaBo bo = new ShortDramaIdeaBo();
        bo.setIdea(idea);
        bo.setProjectName(project.getProjectName());
        bo.setArtStyle(project.getArtStyle());
        bo.setAestheticSkillName(project.getAestheticSkillName());
        bo.setDirectorSkillName(project.getDirectorSkillName());
        ShortDramaScriptResult result = executePhase1_ScriptPolish(chatModel, bo, instruction.trim(), null);

        if (script == null) {
            script = new ShortDramaScript();
            script.setId(IdUtil.getSnowflakeNextId());
            script.setProjectId(projectId);
            script.setSourceType("llm");
        }
        project.setStatus("script_changed");
        videoComposeService.invalidateComposition(projectId);
        project.setProjectName(firstNotBlank(result.getProjectName(), project.getProjectName()));
        project.setDescription(firstNotBlank(result.getDescription(), project.getDescription()));
        projectMapper.updateById(project);
        script.setScriptName(firstNotBlank(result.getScriptName(), script.getScriptName()));
        script.setOutlineText(firstNotBlank(result.getOutlineText(), script.getOutlineText()));
        script.setScriptText(firstNotBlank(result.getScriptText(), script.getScriptText()));
        script.setTone(firstNotBlank(result.getTone(), script.getTone()));
        script.setRevisionNotes(instruction.trim());
        if (script.getId() != null && scriptMapper.selectById(script.getId()) != null) {
            scriptMapper.updateById(script);
        } else {
            scriptMapper.insert(script);
        }
        return getDetail(projectId, userId);
    }

    // ==================== 项目/剧本 CRUD ====================

    @Override
    public Long saveProject(ShortDramaProjectBo bo, Long userId) {
        ShortDramaProject entity = MapstructUtils.convert(bo, ShortDramaProject.class);
        entity.setUserId(userId);
        if (entity.getId() != null) {
            var previous = validateProjectOwner(entity.getId(), userId);
            if (bo.getOriginalIdea() == null) entity.setOriginalIdea(previous.getOriginalIdea());
            if (bo.getAestheticSkillName() == null) entity.setAestheticSkillName(previous.getAestheticSkillName());
            if (bo.getDirectorSkillName() == null) entity.setDirectorSkillName(previous.getDirectorSkillName());
            if (bo.getArtStyle() == null) entity.setArtStyle(previous.getArtStyle());
            if (bo.getAestheticSkillName() != null && !Objects.equals(entity.getAestheticSkillName(), previous.getAestheticSkillName()) && StrUtil.isNotBlank(entity.getAestheticSkillName())) {
                var selected = skillCatalog.requireEnabled(entity.getAestheticSkillName(), "aesthetic");
                if (StrUtil.isNotBlank(selected.artStyle())) entity.setArtStyle(selected.artStyle());
            }
            if (bo.getDirectorSkillName() != null && !Objects.equals(entity.getDirectorSkillName(), previous.getDirectorSkillName()) && StrUtil.isNotBlank(entity.getDirectorSkillName()))
                skillCatalog.requireEnabled(entity.getDirectorSkillName(), "director");
        }
        else validateSkillBindings(entity);
        if (entity.getId() == null) {
            entity.setStatus(StrUtil.blankToDefault(entity.getStatus(), "draft"));
            projectMapper.insert(entity);
        } else {
            projectMapper.updateById(entity);
        }
        return entity.getId();
    }

    @Override
    public ShortDramaScriptVo saveScript(ShortDramaScriptBo bo, Long userId) {
        ShortDramaProject project = projectMapper.selectById(bo.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        ShortDramaScript existing = bo.getId() == null ? null : scriptMapper.selectById(bo.getId());
        if (existing != null && !bo.getProjectId().equals(existing.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        if (existing == null || !java.util.Objects.equals(existing.getScriptText(), bo.getScriptText())
            || !java.util.Objects.equals(existing.getOutlineText(), bo.getOutlineText())
            || (bo.getTone() != null && !java.util.Objects.equals(existing.getTone(), bo.getTone()))
            || (bo.getWorldbuilding() != null && !java.util.Objects.equals(existing.getWorldbuilding(), bo.getWorldbuilding()))) {
            project.setStatus("script_changed");
            projectMapper.updateById(project);
            videoComposeService.invalidateComposition(project.getId());
        }
        ShortDramaScript entity = MapstructUtils.convert(bo, ShortDramaScript.class);
        entity.setScriptText(normalizePlainScriptText(entity.getScriptText()));
        entity.setSourceType(StrUtil.blankToDefault(entity.getSourceType(), "manual"));
        if (existing != null) {
            if (bo.getCreationMode() == null) entity.setCreationMode(existing.getCreationMode());
            if (bo.getSourceMaterials() == null) entity.setSourceMaterials(existing.getSourceMaterials());
            if (bo.getWorldbuilding() == null) entity.setWorldbuilding(existing.getWorldbuilding());
            if (bo.getRevisionNotes() == null) entity.setRevisionNotes(existing.getRevisionNotes());
        }
        if (entity.getId() == null) {
            scriptMapper.insert(entity);
        } else {
            scriptMapper.updateById(entity);
        }
        return MapstructUtils.convert(entity, ShortDramaScriptVo.class);
    }

    // ==================== 分镜生成与规划 ====================

    @Override
    public List<ShortDramaStoryboardVo> generateStoryboards(Long projectId, Long scriptId, String model, Long userId) {
        return generateStoryboards(projectId, scriptId, model, userId, 1);
    }

    private List<ShortDramaStoryboardVo> generateStoryboards(Long projectId, Long scriptId, String model, Long userId, int minimumShotSeconds) {
        if (!beginStoryboardGeneration(scriptId)) {
            throw new IllegalStateException("该剧本正在生成分镜，请勿重复提交");
        }
        try {
        ShortDramaProject project = validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) {
            throw new IllegalArgumentException("剧本不存在");
        }
        videoComposeService.invalidateComposition(projectId);

        ChatModelVo modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), null));

        List<StoryboardPanelData> panels = executePhase3_StoryboardPlan(chatModel, null, script, projectId, null, modelVo.getModelName(), minimumShotSeconds);
        boolean sourceScenes = !panels.isEmpty();
        if (panels.isEmpty()) {
            panels = fallbackPanels(script);
        }
        if (!panels.isEmpty()) {
            executePhase6_StoryboardDetail(chatModel, null, panels, projectId, null, modelVo.getModelName());
            normalizeContinuityChain(panels, sourceScenes);
        }
        validateCompletedSceneBudgets(script, panels, minimumShotSeconds);
        return persistStoryboards(projectId, scriptId, panels, script);
        } finally {
            endStoryboardGeneration(scriptId);
        }
    }

    @Override
    public List<ShortDramaStoryboardVo> planStoryboard(Long projectId, Long scriptId, String model, Long userId) {
        return generateStoryboards(projectId, scriptId, model, userId);
    }

    @Override
    public List<ShortDramaStoryboardVo> planStoryboard(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds) {
        return generateStoryboards(projectId, scriptId, model, userId, checkedMinimumShotSeconds(minimumShotSeconds));
    }

    @Override
    public int importReviewedPlan(Long projectId, org.ruoyi.domain.bo.shortdrama.ShortDramaReviewedPlanBo review, Long userId) {
        validateProjectOwner(projectId,userId);
        int minimum = checkedMinimumShotSeconds(review.getMinimumShotSeconds());
        Long scriptId=review.getScriptId();
        if(!beginStoryboardGeneration(scriptId))throw new IllegalStateException("请等待当前分镜生成结束");
        try {
            var script=scriptMapper.selectById(scriptId);
            if(script==null || !projectId.equals(script.getProjectId()))throw new IllegalArgumentException("剧本不属于当前项目");
            if(!java.util.Objects.equals(script.getScriptText(),review.getExpectedScriptText()))throw new IllegalStateException("剧本已修改，请重新合并审阅稿");
            var existing=storyboardMapper.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId,projectId));
            if(!existing.isEmpty())throw new IllegalStateException("已有分镜请使用分镜修订入口；规划导入仅适用于尚未落库的项目");
            List<String> scenes=splitScriptScenes(script.getScriptText());
            var panels=review.getPanels();
            List<Integer> selected = reviewedSceneNumbers(scenes, panels, review.getSceneNumbers());
            var assets = planningAssets(projectId);
            if (review.getSceneNumbers() != null && !java.util.Objects.equals(assets.signature(), review.getExpectedAssetSignature()))
                throw new IllegalStateException("当前资产签名已变化或未确认，请重新审阅所选场次并读取checkpoint-status");
            // Validate every selected scene before writing any checkpoint. A partial import is explicit approval, never migration.
            for (int number : selected) {
                var group = panels.stream().filter(p -> java.util.Objects.equals(p.getSceneNumber(), number)).toList();
                validateScenePlan(scenes.get(number - 1), group, minimum);
                assets.validateSceneCast(scenes.get(number - 1), previousSceneBridge(scenes, number - 1), group);
                assets.referenced(group);
            }
            var model=StrUtil.isNotBlank(review.getModel())?validateAndGetModel(review.getModel()):findChatModel();
            String checkpoint = planningCheckpointKey(script, projectId, model.getModelName(), minimum);
            String style = artStyleSuffix(projectId), aspect = projectAspectRatio(projectId);
            for(int sceneNo : selected) {
                var group=panels.stream().filter(p->java.util.Objects.equals(p.getSceneNumber(),sceneNo)).toList();
                sceneCheckpoints.save(sceneIdentity(projectId, script, scenes, sceneNo - 1, style, aspect, model.getModelName(), minimum),
                    assets, group, true, "reviewed_import");
                RedisUtils.setCacheObject(checkpoint+":scene:"+sceneNo,JsonUtils.toJsonString(group),java.time.Duration.ofDays(7));
            }
            return panels.size();
        } finally { endStoryboardGeneration(scriptId); }
    }

    static void validateReviewedPlan(List<String> scenes,List<StoryboardPanelData> panels) {
        for (int sceneNo : reviewedSceneNumbers(scenes, panels, null)) {
            var group = panels.stream().filter(p -> java.util.Objects.equals(p.getSceneNumber(), sceneNo)).toList();
            validateScenePlan(scenes.get(sceneNo - 1), group);
        }
    }

    static List<Integer> reviewedSceneNumbers(List<String> scenes, List<StoryboardPanelData> panels, List<Integer> selected) {
        if(panels==null || panels.isEmpty() || panels.size()>500)throw new IllegalArgumentException("规划镜头数量无效");
        if(panels.stream().anyMatch(p->p.getSceneNumber()==null || p.getSceneNumber()<1 || p.getSceneNumber()>scenes.size()))throw new IllegalArgumentException("规划场次不属于当前剧本");
        List<Integer> numbers = selected == null ? java.util.stream.IntStream.rangeClosed(1, scenes.size()).boxed().toList() : new ArrayList<>(selected);
        if (numbers.isEmpty() || numbers.stream().anyMatch(n -> n == null || n < 1 || n > scenes.size())
            || new java.util.HashSet<>(numbers).size() != numbers.size()) throw new IllegalArgumentException("审阅场次须唯一且属于当前剧本");
        if (panels.stream().anyMatch(p -> !numbers.contains(p.getSceneNumber()))) throw new IllegalArgumentException("导入镜头包含未明确批准的场次");
        for(int sceneNo : numbers) {
            var group=panels.stream().filter(p->java.util.Objects.equals(p.getSceneNumber(),sceneNo)).toList();
            if(group.isEmpty())throw new IllegalArgumentException("缺少第"+sceneNo+"场规划");
        }
        return numbers;
    }

    @Override
    public java.util.Map<String, Object> storyboardCheckpointStatus(Long projectId, Long scriptId, String model, Long userId) {
        return storyboardCheckpointStatus(projectId, scriptId, model, userId, 1);
    }

    @Override
    public java.util.Map<String, Object> storyboardCheckpointStatus(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds) {
        int minimum = checkedMinimumShotSeconds(minimumShotSeconds);
        validateProjectOwner(projectId, userId);
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        String text = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        List<String> scenes = splitScriptScenes(text);
        var assets = planningAssets(projectId);
        String style = artStyleSuffix(projectId), aspect = projectAspectRatio(projectId);
        String legacy = planningCheckpointKey(script, projectId, modelVo.getModelName(), minimum);
        List<java.util.Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < scenes.size(); i++) {
            var identity = sceneIdentity(projectId, script, scenes, i, style, aspect, modelVo.getModelName(), minimum);
            var receipt = sceneCheckpoints.compatible(identity, assets, true);
            List<StoryboardPanelData> plan = receipt == null ? null : receipt.panels();
            String origin = receipt == null ? "none" : receipt.origin();
            if (plan == null) {
                plan = parsePanelList(RedisUtils.getCacheObject(legacy + ":scene:" + (i + 1)));
                if (plan != null && !plan.isEmpty()) origin = "legacy_exact_fingerprint";
            }
            boolean valid = false;
            try { if (plan != null && !plan.isEmpty()) { validateScenePlan(scenes.get(i), plan, minimum); assets.referenced(plan); assets.validateSceneCast(scenes.get(i), previousSceneBridge(scenes, i), plan); valid = true; } }
            catch (RuntimeException ignored) { /* Only current valid proof is advertised as reusable. */ }
            var draft = sceneCheckpoints.compatible(identity, assets, false);
            var entry = new LinkedHashMap<String, Object>();
            entry.put("sceneNumber", i + 1); entry.put("sceneHash", identity.sceneHash()); entry.put("contextHash", identity.contextHash());
            entry.put("recoverableValidated", valid); entry.put("recoverableDraft", draft != null);
            entry.put("state", valid ? "validated" : draft != null ? "draft_needs_validation" : "missing_or_incompatible");
            entry.put("proofSource", valid ? origin : "none");
            entry.put("panelCount", valid ? plan.size() : 0); entry.put("totalSeconds", valid ? plan.stream().mapToInt(StoryboardPanelData::getDuration).sum() : 0);
            if (valid) entry.put("bindings", assets.referenced(plan));
            entries.add(entry);
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", ShortDramaSceneCheckpoint.VERSION); result.put("projectId", projectId); result.put("scriptId", scriptId);
        result.put("scriptHash", cn.hutool.crypto.digest.DigestUtil.sha256Hex(text)); result.put("model", modelVo.getModelName());
        result.put("currentAssetSignature", assets.signature()); result.put("sceneCount", scenes.size()); result.put("scenes", entries);
        result.put("minimumShotSeconds", minimum);
        result.put("validatedSceneCount", entries.stream().filter(e -> Boolean.TRUE.equals(e.get("recoverableValidated"))).count());
        result.put("observedAt", java.time.Instant.now().toString());
        return result;
    }

    @Override
    public java.util.Map<String, Object> storyboardCandidates(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds) {
        int minimum = checkedMinimumShotSeconds(minimumShotSeconds == null ? 4 : minimumShotSeconds);
        validateProjectOwner(projectId, userId);
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        String text = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        List<String> scenes = splitScriptScenes(text);
        String style = artStyleSuffix(projectId), aspect = projectAspectRatio(projectId);
        List<java.util.Map<String, Object>> entries = new ArrayList<>();
        for (int i = 0; i < scenes.size(); i++) {
            var identity = sceneIdentity(projectId, script, scenes, i, style, aspect, modelVo.getModelName(), minimum);
            var entry = new LinkedHashMap<String, Object>();
            entry.put("sceneNumber", i + 1); entry.put("identity", identity); entry.put("candidates", sceneCandidates.read(identity));
            entries.add(entry);
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", ShortDramaSceneCandidateDiagnostics.VERSION); result.put("projectId", projectId); result.put("scriptId", scriptId);
        result.put("model", modelVo.getModelName()); result.put("scriptHash", cn.hutool.crypto.digest.DigestUtil.sha256Hex(text));
        result.put("minimumShotSeconds", minimum); result.put("state", "unreviewed_candidates_only"); result.put("validated", false);
        result.put("retentionDays", 7); result.put("maxResponseChars", ShortDramaSceneCandidateDiagnostics.MAX_RESPONSE_CHARS);
        result.put("maxCandidatesPerScene", ShortDramaSceneCandidateDiagnostics.MAX_CANDIDATES); result.put("scenes", entries);
        result.put("observedAt", java.time.Instant.now().toString());
        return result;
    }

    @Override
    public java.util.Map<String, Object> storyboardPlanningStatus(Long projectId, Long scriptId, String requestId, Long userId) {
        validateProjectOwner(projectId, userId);
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var status = planningStatuses.read(projectId, scriptId, ShortDramaPlanningStatus.checkedRequestId(requestId));
        var result = planningStatusData(status, projectId, scriptId);
        if (status != null && "done".equals(status.state()) && StrUtil.isNotBlank(status.storyboardHash())) {
            var current = storyboardMapper.selectVoList(new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getScriptId, scriptId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo));
            result.put("currentStoryboardIds", current.stream().map(ShortDramaStoryboardVo::getId).toList());
            result.put("currentStoryboardHash", storyboardReceiptHash(current));
        }
        result.put("currentScriptHash", cn.hutool.crypto.digest.DigestUtil.sha256Hex(firstNotBlank(script.getScriptText(), script.getOutlineText(), "")));
        result.put("observedAt", java.time.Instant.now().toString());
        return result;
    }

    private java.util.Map<String, Object> planningStatusData(ShortDramaPlanningStatus.Status status, Long projectId, Long scriptId) {
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", ShortDramaPlanningStatus.VERSION); result.put("projectId", projectId); result.put("scriptId", scriptId);
        result.put("requestId", status == null ? null : status.requestId());
        result.put("state", status == null ? (storyboardGenerationStates.containsKey(scriptId) ? "running" : "not_observed")
            : !status.terminal() && !planningStatuses.currentRuntime(status) ? "submission_unknown" : status.state());
        result.put("terminal", status != null && status.terminal());
        result.put("activeLock", storyboardGenerationStates.containsKey(scriptId));
        result.put("retrySafe", false); // Neither a missing receipt nor a dropped SSE proves that a paid request failed.
        if (status != null) {
            result.put("scriptHash", status.scriptHash()); result.put("model", status.model()); result.put("minimumShotSeconds", status.minimumShotSeconds());
            result.put("submittedAt", status.submittedAt()); result.put("updatedAt", status.updatedAt());
            result.put("panelCount", status.panelCount()); result.put("error", status.error());
            result.put("storyboardIds", status.storyboardIds()); result.put("storyboardHash", status.storyboardHash());
            var progress = ShortDramaPlanningProgress.read(projectId, scriptId, status.requestId());
            if (progress != null) result.put("progress", progress);
        }
        return result;
    }

    private static String storyboardReceiptHash(List<ShortDramaStoryboardVo> storyboards) {
        var payload = new ArrayList<LinkedHashMap<String, Object>>();
        storyboards.stream()
            .sorted(java.util.Comparator.comparing(ShortDramaStoryboardVo::getSceneNo)
                .thenComparing(ShortDramaStoryboardVo::getId))
            .forEach(row -> {
                var value = new LinkedHashMap<String, Object>();
                value.put("id", row.getId()); value.put("projectId", row.getProjectId()); value.put("scriptId", row.getScriptId());
                value.put("sceneNo", row.getSceneNo()); value.put("sceneTitle", row.getSceneTitle()); value.put("sceneText", row.getSceneText());
                value.put("sceneType", row.getSceneType()); value.put("shotType", row.getShotType()); value.put("cameraMove", row.getCameraMove());
                value.put("charactersJson", row.getCharactersJson()); value.put("locationName", row.getLocationName());
                value.put("photographyRules", row.getPhotographyRules()); value.put("actingNotes", row.getActingNotes());
                value.put("continuityJson", row.getContinuityJson()); value.put("sourceText", row.getSourceText());
                value.put("imagePrompt", row.getImagePrompt()); value.put("durationSeconds", row.getDurationSeconds());
                value.put("videoPrompt", row.getVideoPrompt()); value.put("videoUrl", row.getVideoUrl()); value.put("videoId", row.getVideoId());
                value.put("videoStatus", row.getVideoStatus()); value.put("lastFrameUrl", row.getLastFrameUrl());
                payload.add(value);
            });
        try {
            return cn.hutool.crypto.digest.DigestUtil.sha256Hex(org.ruoyi.service.media.AtlasMediaSupport.OBJECT_MAPPER.writeValueAsString(payload));
        } catch (java.io.IOException error) {
            throw new IllegalStateException("分镜完成回执哈希生成失败", error);
        }
    }

    private ShortDramaSceneCheckpoint.Assets planningAssets(Long projectId) {
        var characters = characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        var appearances = characters.isEmpty() ? List.<ShortDramaCharacterAppearance>of() : characterAppearanceMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacterAppearance>().in(ShortDramaCharacterAppearance::getCharacterId, characters.stream().map(ShortDramaCharacter::getId).toList()));
        var locations = locationMapper.selectList(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId));
        var project = projectMapper.selectById(projectId);
        String version = project != null && (StrUtil.isNotBlank(project.getAestheticSkillName()) || StrUtil.isNotBlank(project.getDirectorSkillName()))
            ? skillCatalog.projectVersion(project) : "";
        return new ShortDramaSceneCheckpoint.Assets(characters, appearances, locations, version);
    }

    private static ShortDramaSceneCheckpoint.Identity sceneIdentity(Long projectId, ShortDramaScript script, List<String> scenes,
        int index, String style, String aspect, String model, int minimum) {
        return ShortDramaSceneCheckpoint.identity(projectId, script, index + 1, scenes.get(index), previousSceneBridge(scenes, index), style, aspect, model, minimum);
    }

    private static String previousSceneBridge(List<String> scenes, int index) {
        String previous = index == 0 ? "" : scenes.get(index - 1);
        return previous.substring(Math.max(0, previous.length() - 1000));
    }

    private static int checkedMinimumShotSeconds(Integer minimum) {
        int value = minimum == null ? 1 : minimum;
        if (value < 1 || value > 15) throw new IllegalArgumentException("minimumShotSeconds须为1至15的整数");
        return value;
    }

    private String planningCheckpointKey(ShortDramaScript script, Long projectId, String model, int minimum) {
        var project = projectMapper.selectById(projectId);
        String selectedVersion = project != null && (StrUtil.isNotBlank(project.getAestheticSkillName()) || StrUtil.isNotBlank(project.getDirectorSkillName()))
            ? ":skills:" + skillCatalog.projectVersion(project) : "";
        return storyboardCheckpointKey(script, projectId) + ":parallel-v3:" + ShortDramaDirectorSkills.VERSION + ":" + ShortDramaDirectorSkills.PACING_VERSION + ":" + ShortDramaDirectorSkills.PLANNING_VERSION + ":"
            + cn.hutool.crypto.digest.DigestUtil.sha256Hex(model) + (minimum == 1 ? "" : ":min:" + minimum) + selectedVersion;
    }

    @Override
    public SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId) {
        return planStoryboardStream(projectId, scriptId, model, userId, 1);
    }

    @Override
    public SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds) {
        return planStoryboardStream(projectId, scriptId, model, userId, minimumShotSeconds, null);
    }

    @Override
    public SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds, String requestId) {
        int minimum = checkedMinimumShotSeconds(minimumShotSeconds);
        validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        ChatModelVo modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        String requested = ShortDramaPlanningStatus.checkedRequestId(requestId);
        String scriptHash = cn.hutool.crypto.digest.DigestUtil.sha256Hex(firstNotBlank(script.getScriptText(), script.getOutlineText(), ""));
        String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
        SseEmitter emitter = new SseEmitter(7_200_000L);
        AtomicBoolean emitterActive = new AtomicBoolean(true);
        activeEmitters.put(emitter, emitterActive);
        emitter.onCompletion(() -> closeEmitter(emitter));
        emitter.onTimeout(() -> closeEmitter(emitter));
        emitter.onError(error -> closeEmitter(emitter));

        if (requested != null) {
            var existing = planningStatuses.read(projectId, scriptId, requested);
            if (existing != null) {
                if (!scriptHash.equals(existing.scriptHash()) || !modelVo.getModelName().equals(existing.model()) || minimum != existing.minimumShotSeconds()) {
                    completeEmitter(emitter);
                    throw new IllegalArgumentException("requestId已用于不同的剧本、模型或镜头时长参数");
                }
                sendEmitterEvent(emitter, SseEmitter.event().name("submission").data(planningStatusData(existing, projectId, scriptId)));
                completeEmitter(emitter); return emitter; // Replay a receipt; never replay the model request.
            }
        }
        if (!beginStoryboardGeneration(scriptId)) {
            var busy = storyboardPlanningStatus(projectId, scriptId, null, userId);
            busy.put("message", "该剧本正在生成分镜，请查询原请求状态，勿重复提交");
            sendEmitterEvent(emitter, SseEmitter.event().name("error").data(busy));
            completeEmitter(emitter); return emitter;
        }
        final ShortDramaPlanningStatus.Status queued;
        try {
            var previous = planningStatuses.read(projectId, scriptId, null);
            if (previous != null && !previous.terminal()) {
                if (planningStatuses.currentRuntime(previous))
                    throw new IllegalStateException("原分镜请求尚未确认终态，请先查询requestId=" + previous.requestId() + "，勿重复提交");
                planningStatuses.update(previous, "error", null,
                    "服务进程已重启，原规划任务不在当前运行实例中；允许新requestId复用内容检查点继续规划");
            }
            queued = planningStatuses.queued(projectId, scriptId, requested == null ? java.util.UUID.randomUUID().toString() : requested,
                scriptHash, modelVo.getModelName(), minimum);
        } catch (RuntimeException e) { endStoryboardGeneration(scriptId); completeEmitter(emitter); throw e; }
        var progress = new ShortDramaPlanningProgress.Tracker(queued.requestId(), queued.submittedAt(),
            ShortDramaPlanningProgress.persistence(projectId, scriptId, queued.requestId(), snapshot ->
                sendEmitterEvent(emitter, SseEmitter.event().name("progress").data(JsonUtils.toJsonString(snapshot)))));
        planningProgress.put(emitter, progress);
        progress.phase("storyboard_plan", "分镜请求已登记，等待规划");
        // Queue ACK is buffered by SseEmitter before Spring installs its response handler.
        sendEmitterEvent(emitter, SseEmitter.event().name("submission").data(planningStatusData(queued, projectId, scriptId)));
        emit(emitter, "storyboard_plan", "queued", "分镜请求已登记排队，请用requestId查询，勿重复提交");
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, () -> {
                        planningStatuses.update(queued, "running", null, null);
                        videoComposeService.invalidateComposition(projectId);

                        AbstractChatService chatService = getChatService(modelVo);
                        ChatRequest planningStreamRequest = ShortDramaWritingRequest.forText(modelVo.getModelName(), firstNotBlank(script.getScriptText(), script.getOutlineText(), ""));
                        ChatModel chatModel = chatService.buildChatModel(modelVo, planningStreamRequest);
                        StreamingChatModel streamingModel = chatService.buildStreamingChatModel(modelVo, planningStreamRequest);

                        log.info("开始流式生成分镜: projectId={}, scriptId={}, model={}", projectId, scriptId, modelVo.getModelName());
                        emit(emitter, "storyboard_plan", "running", "正在规划分镜镜头，模型开始输出后会实时显示...");
                        List<StoryboardPanelData> panels = executePhase3_StoryboardPlan(chatModel, streamingModel, script, projectId, emitter, modelVo.getModelName(), minimum, queued.requestId());
                        if (panels.isEmpty()) {
                            throw new IllegalStateException("分镜规划失败，模型未返回有效镜头");
                        }
                        emit(emitter, "storyboard_plan", "done", "分镜规划完成，共 " + panels.size() + " 个镜头");

                        emit(emitter, "storyboard_detail", "running", "正在细化镜头提示词和时长...");
                        executePhase6_StoryboardDetail(chatModel, streamingModel, panels, projectId, emitter, modelVo.getModelName());
                        emit(emitter, "storyboard_detail", "done", "全部镜头细化完成");
                        normalizeContinuityChain(panels, true); // Space/time montage cuts must not renumber budget scenes.
                        validateCompletedSceneBudgets(script, panels, minimum);
                        progress.phase("persist", "完整镜头已校验，正在保存分镜");
                        List<ShortDramaStoryboardVo> storyboards = persistStoryboards(projectId, scriptId, panels, script);

                        // Keep validated content-addressed checkpoints for seven days; a successful run is reusable.
                        log.info("流式生成分镜完成: projectId={}, count={}", projectId, storyboards.size());
                        String storyboardHash = storyboardReceiptHash(storyboards);
                        var completed = planningStatuses.complete(queued, storyboards.stream().map(ShortDramaStoryboardVo::getId).toList(), storyboardHash);
                        progress.terminal("done");
                        sendEmitterEvent(emitter, SseEmitter.event().name("complete")
                            .data(planningStatusData(completed, projectId, scriptId)));
                        completeEmitter(emitter);
                    });
                } catch (Exception e) {
                    log.error("流式生成分镜失败: projectId={}, scriptId={}", projectId, scriptId, e);
                    // dynamic(...) has already restored the worker's tenant here. Persist the terminal
                    // receipt in the submission's namespace, not the executor thread's default tenant.
                    org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant,
                        () -> { planningStatuses.update(queued, "error", null, e.getMessage()); });
                    progress.terminal("error");
                    sendEmitterEvent(emitter, SseEmitter.event().name("error")
                        .data("{\"requestId\":\"" + queued.requestId() + "\",\"message\":\"" + escapeJson(ShortDramaSceneCandidateDiagnostics.redact(e.getMessage())) + "\"}"));
                    completeEmitterWithError(emitter, e);
                } finally {
                    planningProgress.remove(emitter);
                    endStoryboardGeneration(scriptId);
                }
            }, storyboardPlanningExecutor);
        } catch (RuntimeException e) {
            planningStatuses.update(queued, "error", null, "后台任务未接受排队请求: " + e.getMessage());
            progress.terminal("error"); planningProgress.remove(emitter);
            endStoryboardGeneration(scriptId); completeEmitterWithError(emitter, e);
        }
        return emitter;
    }

    private synchronized boolean beginStoryboardGeneration(Long scriptId) {
        if (assetGenerationStates.containsKey(scriptId)) throw new IllegalStateException("该剧本正在分析资产，请完成后再生成分镜");
        if (storyboardGenerationStates.putIfAbsent(scriptId, new AtomicBoolean(true)) != null) return false;
        try {
            var script = scriptMapper.selectById(scriptId);
            planningSkillSnapshots.put(scriptId, skillCatalog.snapshot(script == null ? null : projectMapper.selectById(script.getProjectId())));
            return true;
        } catch (RuntimeException error) { planningSkillSnapshots.remove(scriptId); storyboardGenerationStates.remove(scriptId); throw error; }
    }

    private void endStoryboardGeneration(Long scriptId) {
        planningSkillSnapshots.remove(scriptId);
        storyboardGenerationStates.remove(scriptId);
    }

    @Override
    public List<ShortDramaStoryboardVo> generatePhotographyRules(Long projectId, Long scriptId, Long userId) {
        validateProjectOwner(projectId, userId);
        List<ShortDramaStoryboard> existing = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getScriptId, scriptId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo));
        if (existing.isEmpty()) {
            return List.of();
        }
        List<StoryboardPanelData> panels = toPanelDataList(existing);
        ChatModelVo modelVo = findChatModel();
        ChatModel chatModel = getChatService(modelVo).buildChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), null));
        List<JsonNode> photographyRules = executePhase4_PhotographyRules(chatModel, panels, projectId);
        mergePhotographyRules(panels, photographyRules);
        for (int i = 0; i < existing.size() && i < panels.size(); i++) {
            existing.get(i).setPhotographyRules(panels.get(i).getPhotographyRules());
            storyboardMapper.updateById(existing.get(i));
        }
        return storyboardMapper.selectVoList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, scriptId)
            .orderByAsc(ShortDramaStoryboard::getSceneNo));
    }

    @Override
    public List<ShortDramaStoryboardVo> generateActingDirections(Long projectId, Long scriptId, Long userId) {
        validateProjectOwner(projectId, userId);
        List<ShortDramaStoryboard> existing = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getScriptId, scriptId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo));
        if (existing.isEmpty()) {
            return List.of();
        }
        List<StoryboardPanelData> panels = toPanelDataList(existing);
        ChatModelVo modelVo = findChatModel();
        ChatModel chatModel = getChatService(modelVo).buildChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), null));
        List<ActingDirectionResult> actingDirections = executePhase5_ActingDirections(chatModel, panels, projectId);
        mergeActingDirections(panels, actingDirections);
        for (int i = 0; i < existing.size() && i < panels.size(); i++) {
            existing.get(i).setActingNotes(panels.get(i).getActingNotes());
            storyboardMapper.updateById(existing.get(i));
        }
        return storyboardMapper.selectVoList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, scriptId)
            .orderByAsc(ShortDramaStoryboard::getSceneNo));
    }

    // ==================== 分镜 CRUD / 视频 ====================

    private void lockStoryboardStructure(Long projectId, Long scriptId, Long userId) {
        var project = projectMapper.selectOne(new LambdaQueryWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId).last("FOR UPDATE"));
        if (project == null || !userId.equals(project.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        if (storyboardGenerationStates.containsKey(scriptId)) throw new IllegalStateException("分镜正在生成，请完成后再增删");
        if (java.util.Set.of("pending", "queued", "processing", "running", "composing").contains(java.util.Objects.toString(project.getComposeStatus(), "")))
            throw new IllegalStateException("成片正在合成，请完成后再增删分镜");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShortDramaStoryboardVo addStoryboard(Long projectId, Long scriptId, Long afterId, Long userId) {
        lockStoryboardStructure(projectId, scriptId, userId);
        var shots = storyboardMapper.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, scriptId).orderByAsc(ShortDramaStoryboard::getSceneNo));
        int position = shots.size();
        if (afterId != null) {
            position = -1;
            for (int i = 0; i < shots.size(); i++) if (afterId.equals(shots.get(i).getId())) position = i + 1;
            if (position < 0) throw new IllegalArgumentException("插入位置的分镜不存在");
        }
        Map<Integer,Integer> numbering = new java.util.HashMap<>();
        for (int i = 0; i < shots.size(); i++) numbering.put(shots.get(i).getSceneNo(), i < position ? i + 1 : i + 2);
        visualAssets.reindexStoryboardProps(projectId, numbering, null);
        for (int i = shots.size() - 1; i >= position; i--) {
            storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, shots.get(i).getId()).set(ShortDramaStoryboard::getSceneNo, i + 2));
        }
        var shot = new ShortDramaStoryboard();
        shot.setProjectId(projectId); shot.setScriptId(scriptId); shot.setSceneNo(position + 1);
        shot.setSceneTitle("新增镜头"); shot.setVideoStatus("pending"); shot.setDurationSeconds(5);
        // Start with an independent draft; never duplicate a neighbour's media or task references.
        shot.setContinuityJson("{}"); shot.setVideoPrompt("");
        storyboardMapper.insert(shot);
        videoComposeService.invalidateComposition(projectId);
        return MapstructUtils.convert(shot, ShortDramaStoryboardVo.class);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStoryboard(Long storyboardId, Long userId) {
        var shot = storyboardMapper.selectById(storyboardId);
        if (shot == null) throw new IllegalArgumentException("分镜不存在");
        lockStoryboardStructure(shot.getProjectId(), shot.getScriptId(), userId);
        if (videoSubmissionInFlight(shot)) throw new IllegalStateException("此镜头视频仍在生成或等待确认，请查询完成后再删除");
        var before = storyboardMapper.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, shot.getScriptId()).orderByAsc(ShortDramaStoryboard::getSceneNo));
        Map<Integer,Integer> numbering = new java.util.HashMap<>();
        int next = 1;
        for (var existing : before) numbering.put(existing.getSceneNo(), storyboardId.equals(existing.getId()) ? 0 : next++);
        visualAssets.reindexStoryboardProps(shot.getProjectId(), numbering, storyboardId);
        storyboardMapper.deleteById(storyboardId);
        var remaining = storyboardMapper.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, shot.getScriptId()).orderByAsc(ShortDramaStoryboard::getSceneNo));
        for (int i = 0; i < remaining.size(); i++) {
            if (remaining.get(i).getSceneNo() != i + 1) storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, remaining.get(i).getId()).set(ShortDramaStoryboard::getSceneNo, i + 1));
        }
        videoComposeService.invalidateComposition(shot.getProjectId());
    }

    @Override
    public ShortDramaStoryboardVo saveStoryboard(ShortDramaStoryboardBo bo, Long userId) {
        ShortDramaStoryboard entity = MapstructUtils.convert(bo, ShortDramaStoryboard.class);
        ShortDramaStoryboard previous = null;
        ShortDramaProject project = projectMapper.selectById(entity.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        if (entity.getId() != null) {
            ShortDramaStoryboard existing = storyboardMapper.selectById(entity.getId());
            if (existing == null || !entity.getProjectId().equals(existing.getProjectId())) {
                throw new IllegalArgumentException("分镜不存在或不属于当前项目");
            }
            previous = existing;
        }
        if (entity.getDurationSeconds() == null || entity.getDurationSeconds() <= 0) {
            entity.setDurationSeconds(defaultDurationForSceneType(entity.getSceneType()));
        }
        ShortDramaVideoDuration.seconds(entity.getContinuityJson());
        // A manually reviewed new panel must meet the same director-content gate as model planning.
        // Blank drafts and edits to legacy panels remain editable; paid generation has its own gate.
        if (previous == null && StrUtil.isNotBlank(entity.getVideoPrompt())
            && !ShortDramaVisualAssetService.directInsert(entity.getContinuityJson())) {
            var issues = ShortDramaVideoPromptReview.issues(entity.getVideoPrompt(), ShortDramaVideoDuration.reviewSeconds(entity.getContinuityJson()), entity.getSourceText());
            if (!issues.isEmpty()) throw new IllegalArgumentException("新分镜导演稿未通过：" + String.join("；", issues));
        }
        if (entity.getId() == null) {
            entity.setVideoStatus("pending");
            storyboardMapper.insert(entity);
        } else {
            storyboardMapper.updateById(entity);
            entity = storyboardMapper.selectById(entity.getId());
            visualAssets.preserveFrameForVideoSettings(previous, entity);
        }
        videoComposeService.invalidateComposition(entity.getProjectId());
        return MapstructUtils.convert(entity, ShortDramaStoryboardVo.class);
    }

    @Override
    public ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId) {
        return generateVideo(storyboardId, videoModel, userId, null);
    }

    @Override
    public ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId, String requestId, boolean regenerate) {
        return generateVideo(storyboardId, videoModel, userId, null, requestId, regenerate);
    }

    /**
     * 生成单镜视频。
     * @param lastFrameUrl 上一镜末帧 URL（仅同场景相邻镜头传入，用于首帧承接）；跨场景或首镜传 null
     */
    public ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId, String lastFrameUrl) {
        return generateVideo(storyboardId, videoModel, userId, lastFrameUrl, null, false);
    }

    ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId, String lastFrameUrl, String requestId, boolean regenerate) {
        if (regenerate && StrUtil.isBlank(requestId)) throw new IllegalArgumentException("显式重生成须提供新的 requestId UUID");
        String id = ShortDramaVideoSubmissionStore.requestId(requestId);
        synchronized (videoSubmissions.lock(storyboardId)) {
            return generateVideoLocked(storyboardId, videoModel, userId, lastFrameUrl, id, regenerate);
        }
    }

    private ShortDramaStoryboardVo generateVideoLocked(Long storyboardId, String videoModel, Long userId, String lastFrameUrl, String requestId, boolean regenerate) {
        ShortDramaStoryboard storyboard = storyboardMapper.selectById(storyboardId);
        if (storyboard == null) throw new IllegalArgumentException("分镜不存在");
        ShortDramaProject project = projectMapper.selectById(storyboard.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
        var previousRequest = videoSubmissions.read(storyboardId, requestId);
        if (previousRequest == null && (("done".equals(storyboard.getVideoStatus()) && StrUtil.isNotBlank(storyboard.getVideoUrl()) && !regenerate)
            || videoSubmissionInFlight(storyboard))) return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        if (previousRequest == null && "done".equals(storyboard.getVideoStatus()) && !regenerate) {
            if (StrUtil.isNotBlank(storyboard.getVideoId())) return retrieveVideo(storyboardId, videoModel, userId);
            throw new IllegalStateException("原视频标记已完成但缺少文件与任务编号；请先核对原任务，再显式重生成");
        }
        if (ShortDramaVisualAssetService.directInsert(storyboard.getContinuityJson())) throw new IllegalStateException("本镜使用素材，不能通过AI视频模型重绘；请在剪辑阶段使用已绑定原图");
        if ("script_changed".equals(project.getStatus())) throw new IllegalStateException("剧本已修改，请重新分析资产和生成分镜后再生成视频");
        if (storyboardGenerationStates.containsKey(storyboard.getScriptId())) throw new IllegalStateException("分镜正在更新，请完成审阅后再生成视频");
        ChatModelVo modelVo = chatModelService.selectModelByName(videoModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到视频模型配置: " + videoModel);

        Integer videoSeconds = ShortDramaVideoDuration.seconds(storyboard.getContinuityJson());

        // 收集所有参考图（角色 + 场景 + 末帧承接）
        List<String> referenceImages = findStoryboardReferenceImages(storyboard, lastFrameUrl);
        var voicePlan = characterVoices.plan(storyboard, null, true);

        // 根据参考图数量自动切换模型
        if (referenceImages != null && !referenceImages.isEmpty() || !voicePlan.references().isEmpty()) {
            String targetModel;
            if ((referenceImages != null && referenceImages.size() >= 2) || videoModel.endsWith("/reference-to-video") || !voicePlan.references().isEmpty()) {
                targetModel = videoModel.replace("/text-to-video", "/reference-to-video")
                    .replace("/image-to-video", "/reference-to-video");
            } else {
                targetModel = videoModel.replace("/text-to-video", "/image-to-video")
                    .replace("/reference-to-video", "/image-to-video");
            }
            if (!targetModel.equals(videoModel)) {
                ChatModelVo switched = chatModelService.selectModelByName(targetModel);
                if (switched != null) {
                    modelVo = switched;
                    log.info("参考图({}张) → 切换模型: {}", referenceImages == null ? 0 : referenceImages.size(), targetModel);
                } else {
                    throw new IllegalArgumentException("当前镜头需要参考图，但对应模型未配置："+targetModel+"。请选择已配置的多参考图生视频模型，不能降级为文生视频。");
                }
            }
        }

        String enrichedPrompt = buildEnrichedVideoPrompt(storyboard, referenceImages, lastFrameUrl);
        ShortDramaVoiceContract.validateReferences(modelVo.getModelName(),voicePlan.references().stream().map(ShortDramaSoundService.Reference::duration).toList());
        if (!voicePlan.references().isEmpty() && modelVo.getModelName().startsWith("bytedance/seedance-2.0") && (referenceImages==null || referenceImages.isEmpty()))
            throw new IllegalArgumentException("Seedance 2.0声音参考需要至少一张角色或场景参考图；首帧仍为可选");
        enrichedPrompt += voicePlan.direction();

        VideoContext ctx = VideoContext.builder()
            .chatModelVo(modelVo)
            .prompt(enrichedPrompt)
            .size(projectAspectRatio(storyboard.getProjectId()))
            .seconds(videoSeconds)
            .resolution(JsonUtils.parseObject(StrUtil.blankToDefault(storyboard.getContinuityJson(), "{}"), JsonNode.class).path("video_resolution").asText("720p"))
            .referenceImages(referenceImages)
            .generateAudio(Boolean.TRUE)
            .returnLastFrame(Boolean.TRUE)
            .lastFrameUrl(lastFrameUrl)
            .build();
        var parameters = new LinkedHashMap<String, Object>();
        parameters.put("requestedModel", videoModel); parameters.put("actualModel", modelVo.getModelName());
        parameters.put("provider", modelVo.getProviderCode()); parameters.put("prompt", enrichedPrompt);
        parameters.put("seconds", ctx.getSeconds()); parameters.put("size", ctx.getSize()); parameters.put("resolution", ctx.getResolution());
        parameters.put("images", ctx.getReferenceImages()); parameters.put("audios", voicePlan.fingerprints());
        if (!voicePlan.bindings().isEmpty()) parameters.put("characterVoices", voicePlan.bindings());
        parameters.put("lastFrame", lastFrameUrl); parameters.put("generateAudio", ctx.getGenerateAudio());
        parameters.put("returnLastFrame", ctx.getReturnLastFrame()); parameters.put("regenerate", regenerate);
        String parametersHash = ShortDramaVideoSubmissionStore.hash(parameters);
        ShortDramaVideoSubmissionStore.match(previousRequest, parametersHash);
        if (previousRequest != null) return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        // Drafts remain editable. Only a paid submission requires the production review.
        ShortDramaVideoPromptReview.validate(storyboard.getSceneNo(), storyboard.getVideoPrompt(), ShortDramaVideoDuration.reviewSeconds(storyboard.getContinuityJson()), storyboard.getSourceText());
        ctx.setReferenceAudios(sounds.uploadReferences(voicePlan.references(),modelVo));
        String generationToken = "local:" + requestId;
        long now = System.currentTimeMillis();
        var receipt = new ShortDramaVideoSubmissionStore.Submission(requestId, storyboardId, project.getId(), userId,
            parametersHash, videoModel, modelVo.getModelName(), modelVo.getProviderCode(), null, null, null, "submitting", now, now,
            "", ShortDramaVideoSubmissionStore.VideoSnapshot.of(storyboard));
        if (!videoSubmissions.claim(receipt)) {
            ShortDramaVideoSubmissionStore.match(videoSubmissions.read(storyboardId, requestId), parametersHash);
            return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
        }
        if (!voicePlan.bindings().isEmpty()) videoSubmissions.voiceSnapshot(storyboardId,requestId,
            Map.of("bindings",voicePlan.bindings(),"references",voicePlan.fingerprints(),"referenceAudios",ctx.getReferenceAudios(),"direction",voicePlan.direction()));
        // Compare both observed fields: separate request UUIDs cannot submit concurrently.
        var claim = new LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, storyboardId)
            .eq(storyboard.getVideoStatus() != null, ShortDramaStoryboard::getVideoStatus, storyboard.getVideoStatus())
            .isNull(storyboard.getVideoStatus() == null, ShortDramaStoryboard::getVideoStatus)
            .eq(storyboard.getVideoId() != null, ShortDramaStoryboard::getVideoId, storyboard.getVideoId())
            .isNull(storyboard.getVideoId() == null, ShortDramaStoryboard::getVideoId)
            .eq(storyboard.getUpdateTime() != null, ShortDramaStoryboard::getUpdateTime, storyboard.getUpdateTime())
            .set(ShortDramaStoryboard::getVideoId, generationToken)
            .set(ShortDramaStoryboard::getVideoStatus, "generating");
        if (storyboardMapper.update(null, claim) == 0) {
            videoSubmissions.save(receipt.update(null, "not_submitted", "分镜已由其他请求更新；本请求未提交"));
            return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
        }

        MediaGenerationResponse response;
        try {
            response = videoServiceFactory.getOriginalService(modelVo.getProviderCode()).generateVideo(ctx);
        } catch (RuntimeException ex) {
            videoSubmissions.save(receipt.update(null, "submission_unknown", "提交结果未知；请核对上游任务记录，未自动重试"));
            storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, storyboardId)
                .eq(ShortDramaStoryboard::getVideoId, generationToken)
                .set(ShortDramaStoryboard::getVideoStatus, "submission_unknown"));
            log.warn("镜头{}视频提交结果未知，requestId={}，未重试: {}", storyboardId, requestId, ex.getMessage());
            return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
        }
        String videoUrl = null;
        String videoId = response != null && StrUtil.isNotBlank(response.getId()) ? response.getId() : generationToken;
        String videoStatus;
        String lastFrame = null;
        if (response != null && StrUtil.isNotBlank(response.getUrl())) {
            videoUrl = response.getUrl();
            lastFrame = response.getLastFrameUrl();
            videoStatus = "done";
        } else if (response != null && StrUtil.isNotBlank(response.getId())) {
            lastFrame = response.getLastFrameUrl();
            videoStatus = videoTerminalFailure(response.getStatus()) ? "failed" : "generating";
        } else if (response != null && videoTerminalFailure(response.getStatus())) {
            videoStatus = "failed";
        } else {
            videoStatus = "submission_unknown";
        }
        // Preserve the prediction ID even for a synchronous URL response.
        videoSubmissions.save(receipt.result(response == null ? null : response.getId(), videoUrl, lastFrame, videoStatus,
            "submission_unknown".equals(videoStatus) ? "服务未返回可确认任务；请核对上游记录，未自动重试" : ""));
        boolean accepted = "done".equals(videoStatus) || "generating".equals(videoStatus);
        if (("done".equals(videoStatus) || "failed".equals(videoStatus)) && (response == null || StrUtil.isBlank(response.getId()))) videoId = null;
        int completed = storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, storyboardId)
            .eq(ShortDramaStoryboard::getVideoId, generationToken)
            // During an uncertain regeneration the old finished file remains available.
            .set(accepted, ShortDramaStoryboard::getVideoUrl, videoUrl)
            .set(ShortDramaStoryboard::getVideoId, videoId)
            .set(ShortDramaStoryboard::getVideoStatus, videoStatus)
            .set(accepted, ShortDramaStoryboard::getLastFrameUrl, lastFrame));
        if (completed > 0 && accepted) {
            videoComposeService.invalidateComposition(project.getId());
        }
        return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
    }

    private static boolean videoSubmissionInFlight(ShortDramaStoryboard shot) {
        return "submission_unknown".equals(shot.getVideoStatus()) || "generating".equals(shot.getVideoStatus())
            || (shot.getVideoId() != null && shot.getVideoId().startsWith("local:"));
    }

    private static boolean videoTerminalFailure(String state) {
        return java.util.Set.of("failed", "canceled", "cancelled", "rejected").contains(java.util.Objects.toString(state, ""));
    }

    @Override
    public ShortDramaVideoSubmissionStore.Submission videoSubmission(Long storyboardId, String requestId, Long userId) {
        ShortDramaStoryboard storyboard = storyboardMapper.selectById(storyboardId);
        if (storyboard == null) throw new IllegalArgumentException("分镜不存在");
        validateProjectOwner(storyboard.getProjectId(), userId);
        return videoSubmissions.read(storyboardId, requestId);
    }

    /** 收集分镜关联的所有参考图：角色形象图（按出场顺序）+ 场景图 + 可选末帧承接 */
    private List<String> findStoryboardReferenceImages(ShortDramaStoryboard storyboard, String lastFrameUrl) {
        List<String> images = new ArrayList<>();
        String frame = videoStartFrame(storyboard);
        // The reviewed frame already resolves identity, set and props. Do not let the
        // original design sheets compete with it and recreate objects or camera angles.
        if (StrUtil.isNotBlank(frame) && !multiShotSegment(storyboard)) return List.of(frame);
        if (StrUtil.isNotBlank(frame)) images.add(frame);
        List<CharacterRef> chars = parseCharacterRefs(storyboard.getCharactersJson());
        if (chars != null) {
            for (CharacterRef ref : chars) {
                String img = findCharacterImageUrl(storyboard.getProjectId(), ref.getName(), ref.getAppearance());
                if (StrUtil.isNotBlank(img) && !images.contains(img)) {
                    images.add(img);
                }
            }
        }
        if (videoReferenceEnabled(storyboard, "video_reference_include_location") && StrUtil.isNotBlank(storyboard.getLocationName())) {
            String img = findLocationImageUrl(storyboard.getProjectId(), storyboard.getLocationName());
            if (StrUtil.isNotBlank(img) && !images.contains(img)) {
                images.add(img);
            }
        }
        for (String prop : visualAssets.readyPropReferences(storyboard)) if (!images.contains(prop)) images.add(prop);
        // 末帧承接：放在最后一张，@imageN 标记会自动绑定
        if (StrUtil.isNotBlank(lastFrameUrl) && !images.contains(lastFrameUrl)) {
            images.add(lastFrameUrl);
        }
        return images.isEmpty() ? null : images;
    }

    /** Explicit opt-in: one continuous performance segment may contain several editorial shots. */
    private String videoStartFrame(ShortDramaStoryboard storyboard) {
        return videoReferenceEnabled(storyboard, "video_use_start_frame") ? visualAssets.readyFrame(storyboard.getId()) : null;
    }

    private static boolean videoReferenceEnabled(ShortDramaStoryboard storyboard, String key) {
        try {
            return JsonUtils.parseObject(StrUtil.blankToDefault(storyboard.getContinuityJson(), "{}"), JsonNode.class)
                .path(key).asBoolean(true);
        } catch (Exception e) { throw new IllegalArgumentException("视频参考设置无效", e); }
    }

    private static boolean multiShotSegment(ShortDramaStoryboard storyboard) {
        try {
            return "multi_shot".equals(JsonUtils.parseObject(
                StrUtil.blankToDefault(storyboard.getContinuityJson(), "{}"), JsonNode.class)
                .path("video_reference_mode").asText());
        } catch (Exception ignored) { return false; }
    }

    /** 构建增强提示词：融合镜头语言、摄影规则、角色信息、表演指导、场景描述 */
    private String buildEnrichedVideoPrompt(ShortDramaStoryboard storyboard) {
        return buildEnrichedVideoPrompt(storyboard, null, null);
    }

    /** 构建增强提示词（含 @imageN 参考图引用，用于 reference-to-video 模型） */
    private String buildEnrichedVideoPrompt(ShortDramaStoryboard storyboard, java.util.List<String> refImages) {
        return buildEnrichedVideoPrompt(storyboard, refImages, null);
    }

    /** 构建增强提示词（含参考图 + 末帧首帧承接） */
    private String buildEnrichedVideoPrompt(ShortDramaStoryboard storyboard, java.util.List<String> refImages, String lastFrameUrl) {
        var selectedProject = projectMapper.selectById(storyboard.getProjectId());
        String selectedDirection = skillCatalog.selectedVisual(selectedProject, "aesthetic") + skillCatalog.selectedVisual(selectedProject, "director");
        boolean hasRefImages = refImages != null && !refImages.isEmpty();
        if (multiShotSegment(storyboard)) {
            String frame = videoStartFrame(storyboard);
            StringBuilder segment = new StringBuilder(ShortDramaDirectorSkills.load("video-prompt"));
            segment.append(selectedDirection).append("\n[连续表演段参考绑定]\n");
            if (StrUtil.isNotBlank(frame) && hasRefImages && refImages.contains(frame)) {
                segment.append("@image").append(refImages.indexOf(frame) + 1)
                    .append("是本段0秒起始关键帧；后续按明确的剪辑镜头切换机位，不将起始构图强制保持到结尾。\n");
            } else {
                segment.append("本段未绑定起始帧；以角色、场景和道具参考建立第一帧，再按导演稿完成连续表演与镜头切换。\n");
            }
            for (CharacterRef ref : parseCharacterRefs(storyboard.getCharactersJson())) {
                int index = findRefImageIndex(refImages, storyboard.getProjectId(), ref.getName(), ref.getAppearance());
                if (index < 0) throw new IllegalArgumentException("连续多镜段缺少角色参考：" + ref.getName());
                segment.append(ref.getName()).append("身份与服装对应@image").append(index + 1)
                    .append("；该图仅提供身份，人物何时入画以导演稿为准，不复制多视图或背景。\n");
            }
            int location = findLocationRefImageIndex(refImages, storyboard.getProjectId(), storyboard.getLocationName());
            if (location >= 0) segment.append("场景空间对应@image").append(location + 1)
                .append("；同场换景别保持物件位置、光源和摄影轴线。\n");
            segment.append(visualAssets.readyPropDirection(storyboard, refImages));
            segment.append(ShortDramaVideoDuration.direction(storyboard.getContinuityJson()))
                .append(storyboard.getVideoPrompt()).append("\n[原文声源]\n").append(storyboard.getSourceText());
            return segment.toString();
        }
        try {
            var c=JsonUtils.parseObject(storyboard.getContinuityJson(),JsonNode.class);
            if(hasRefImages && refImages.size()==1 && StrUtil.isNotBlank(videoStartFrame(storyboard)))return
                selectedDirection + ShortDramaVideoPromptReview.anchoredPrompt(storyboard.getVideoPrompt(), ShortDramaVideoDuration.reviewSeconds(storyboard.getContinuityJson()),
                    storyboard.getSourceText(), c.path("start_state").asText(), c.path("end_state").asText());
        } catch(Exception ignored) { }
        StringBuilder sb = new StringBuilder();
        sb.append(ShortDramaDirectorSkills.load("emotional-dialogue", "director-blocking", "real-material", "video-continuity", "video-prompt"));
        sb.append(selectedDirection);

        // 1. 镜头语言标签
        String frame = videoStartFrame(storyboard);
        if (StrUtil.isNotBlank(frame) && hasRefImages && refImages.contains(frame)) {
            sb.append("[本镜关键帧] @image").append(refImages.indexOf(frame)+1)
                .append("是已生成的本镜构图和起始状态；锁定人物身份、道具外观和站位，从该状态展开本镜动作。其他参考图只用于身份和空间，不复制多视图。\n");
        }
        sb.append("[镜头: ").append(firstNotBlank(storyboard.getSceneType(), "daily"));
        if (StrUtil.isNotBlank(storyboard.getShotType())) sb.append(", ").append(storyboard.getShotType());
        if (StrUtil.isNotBlank(storyboard.getCameraMove())) sb.append(", ").append(storyboard.getCameraMove());
        sb.append("]\n");

        // 1.1 核心动作描述前置（最高权重，确保模型优先关注当前镜头的具体可拍内容）
        if (StrUtil.isNotBlank(storyboard.getVideoPrompt())) {
            sb.append("[核心动作] ").append(storyboard.getVideoPrompt()).append("\n");
        }

        // 2. 摄影规则
        if (StrUtil.isNotBlank(storyboard.getPhotographyRules())) {
            try {
                JsonNode rules = JsonUtils.parseObject(storyboard.getPhotographyRules(), JsonNode.class);
                if (rules != null) {
                    StringBuilder photo = new StringBuilder("[摄影:");
                    JsonNode lighting = rules.path("lighting");
                    if (!lighting.isMissingNode()) {
                        String dir = lighting.path("direction").asText(null);
                        String qual = lighting.path("quality").asText(null);
                        if (dir != null) photo.append(" ").append(dir).append(qual != null ? qual : "").append("光");
                    }
                    String dof = rules.path("depth_of_field").asText(null);
                    if (dof != null) photo.append(", 景深:").append(dof);
                    String tone = rules.path("color_tone").asText(null);
                    if (tone != null) photo.append(", 色调:").append(tone);
                    if (photo.length() > 4) { photo.append("]"); sb.append(photo).append("\n"); }

                    JsonNode photoChars = rules.path("characters");
                    if (photoChars.isArray() && !photoChars.isEmpty()) {
                        sb.append("[站位] ");
                        for (int i = 0; i < photoChars.size(); i++) {
                            JsonNode pc = photoChars.get(i);
                            if (i > 0) sb.append("；");
                            sb.append(pc.path("name").asText(""))
                                .append("在").append(pc.path("screen_position").asText(""))
                                .append(pc.path("posture").asText(""))
                                .append("面向").append(pc.path("facing").asText(""));
                        }
                        sb.append("\n");
                    }
                }
            } catch (Exception e) { log.debug("解析摄影规则失败: {}", e.getMessage()); }
        }

        // 3. 角色信息（名字→年龄性别+视觉描述，附加站位 + @imageN 参考图标记）
        List<CharacterRef> chars = parseCharacterRefs(storyboard.getCharactersJson());
        if (chars != null && !chars.isEmpty()) {
            sb.append("[角色] ");
            for (int i = 0; i < chars.size(); i++) {
                CharacterRef ref = chars.get(i);
                if (i > 0) sb.append("；");
                ShortDramaCharacter ch = findCharacterByName(storyboard.getProjectId(), ref.getName());
                String desc = ch != null
                    ? firstNotBlank(ch.getAgeRange(), "") + firstNotBlank(ch.getGender(), "")
                    : ref.getName();
                sb.append(desc).append("(").append(ref.getName()).append(")");
                // 附加角色视觉描述，确保文字 prompt 也承载外貌信息，不能全靠参考图
                if (ch != null && StrUtil.isNotBlank(ch.getVisualDescription())) {
                    sb.append("，外貌：").append(ch.getVisualDescription());
                }
                // 多参考图模式：附加 @imageN 标记
                if (hasRefImages) {
                    int imgIdx = findRefImageIndex(refImages, storyboard.getProjectId(), ref.getName(), ref.getAppearance());
                    if (imgIdx >= 0) sb.append("@image").append(imgIdx + 1);
                }
                if (StrUtil.isNotBlank(ref.getSlot())) sb.append("在").append(ref.getSlot());
            }
            sb.append("\n");
        }

        // 角色参考图模式下，参考图容易被模型扩散到背景群众造成"所有人一张脸"。
        // 仅 reference-to-video 模式（有参考图）追加身份隔离规则；纯文生视频不需要。
        if (hasRefImages) {
            appendCharacterIdentityIsolationPrompt(sb, storyboard, chars, refImages);
        }

        // 4. 场景描述（+ @imageN 参考图标记）
        if (StrUtil.isNotBlank(storyboard.getLocationName())) {
            sb.append("[场景] ").append(storyboard.getLocationName());
            ShortDramaLocation loc = findLocationByName(storyboard.getProjectId(), storyboard.getLocationName());
            if (loc != null && StrUtil.isNotBlank(loc.getSummary())) {
                sb.append(" — ").append(loc.getSummary());
            }
            // 多参考图模式：附加 @imageN 标记
            if (hasRefImages) {
                int imgIdx = findLocationRefImageIndex(refImages, storyboard.getProjectId(), storyboard.getLocationName());
                if (imgIdx >= 0) sb.append("@image").append(imgIdx + 1);
            }
            sb.append("\n");
        }

        // 5. 表演指导
        if (StrUtil.isNotBlank(storyboard.getActingNotes())) {
            try {
                JsonNode actingArr = JsonUtils.parseObject(storyboard.getActingNotes(), JsonNode.class);
                if (actingArr != null && actingArr.isArray() && !actingArr.isEmpty()) {
                    sb.append("[表演] ");
                    for (int i = 0; i < actingArr.size(); i++) {
                        JsonNode an = actingArr.get(i);
                        if (i > 0) sb.append("；");
                        sb.append(an.path("name").asText("")).append(":")
                            .append(an.path("acting").asText(""));
                    }
                    sb.append("\n");
                }
            } catch (Exception e) { log.debug("解析表演指导失败: {}", e.getMessage()); }
        }

        // 6. 前后镜头连续性状态
        appendContinuityPrompt(sb, storyboard);

        // 6.1 首帧承接：当存在上一镜末帧 URL 时，提示模型首帧继承末帧画面
        if (StrUtil.isNotBlank(lastFrameUrl)) {
            int lastFrameImageIndex = refImages != null ? refImages.indexOf(lastFrameUrl) : -1;
            if (lastFrameImageIndex >= 0) {
                sb.append("[首帧承接] 当前视频第一帧必须严格继承 @image").append(lastFrameImageIndex + 1)
                    .append("（上一镜末帧）的人物位置、姿态、朝向、服装、道具和光线方向，")
                    .append("不得改变人物左右关系或重置场景，动作从该帧状态自然延续。\n");
            }
        }

        // 7. 画面描述（AI 撰写的分镜画面叙述）
        if (StrUtil.isNotBlank(storyboard.getSceneText())
            && !storyboard.getSceneText().equals(storyboard.getVideoPrompt())) {
            sb.append("[画面] ").append(storyboard.getSceneText()).append("\n");
        }

        // 8. 视觉参考（image_prompt 含光线/氛围/构图细节）
        // A reviewed/regenerated keyframe supersedes the original visual draft.
        // Re-appending that draft can reset props and framing to the rejected version.
        if (StrUtil.isBlank(frame) && StrUtil.isNotBlank(storyboard.getImagePrompt())) {
            sb.append("[视觉] ").append(storyboard.getImagePrompt()).append("\n");
        }

        // 9. 目标时长与节奏
        sb.append(visualAssets.readyPropDirection(storyboard, refImages));
        sb.append(ShortDramaVideoDuration.direction(storyboard.getContinuityJson()));
        sb.append("\n");

        // 10. 视觉风格
        String artStyle = ShortDramaImageConstants.artStylePrompt(skillCatalog.effectiveArtStyle(selectedProject));
        if (StrUtil.isNotBlank(artStyle)) {
            sb.append("\n[风格] ").append(artStyle);
        }

        // 11. 视频描述（核心驱动 prompt）
        sb.append("\n").append(storyboard.getVideoPrompt());
        if (StrUtil.isNotBlank(storyboard.getSourceText())) {
            sb.append("\n[已审阅剧情与声音原文]\n").append(storyboard.getSourceText())
                .append("\n对白内容、发言人和先后顺序以以上原文为准，不改写、不对调、不新增台词；手机录音只作画外声，不生成说话者实体。");
        }
        return sb.toString();
    }

    /**
     * 为分镜统一追加人物身份隔离规则。
     * <p>
     * 参考图只绑定对应具名角色，不能被模型当作群众的通用脸部模板；
     * 单人近景进一步限制为仅一张可辨认人脸，多角色镜则要求逐人绑定、互不混脸。
     */
    private void appendCharacterIdentityIsolationPrompt(StringBuilder sb,
                                                        ShortDramaStoryboard storyboard,
                                                        List<CharacterRef> chars,
                                                        List<String> refImages) {
        boolean hasRefImages = refImages != null && !refImages.isEmpty();
        int characterCount = chars == null ? 0 : chars.size();
        String shotType = firstNotBlank(storyboard.getShotType(), "");
        boolean closeShot = shotType.contains("特写") || shotType.contains("近景")
            || shotType.toLowerCase(Locale.ROOT).contains("close");
        boolean visibleExtras;
        try {
            JsonNode continuity = org.ruoyi.service.media.AtlasMediaSupport.OBJECT_MAPPER.readTree(firstNotBlank(storyboard.getContinuityJson(), "{}"));
            visibleExtras = ShortDramaBackgroundExtras.parse(ShortDramaBackgroundExtras.readText(continuity)).visible();
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("人物隔离的连续性字段无效", e);
        }

        sb.append("[人物身份隔离] ");
        if (characterCount == 0) {
            sb.append("本镜没有具名角色参考。若出现群众，每个人必须具有不同的脸型、五官比例、身高、体态、发型和服装细节；")
                .append("禁止复制脸、镜像人物、双胞胎式重复、同一人物贴图和整排相同表情。");
        } else if (characterCount == 1) {
            String characterName = chars.get(0).getName();
            sb.append("唯一具名角色为").append(characterName).append("；")
                .append(hasRefImages ? "该角色参考图只绑定此人，绝不能扩散给背景人物；" : "角色身份只属于此人；");
            if (closeShot && !visibleExtras) {
                sb.append("这是单人近景/特写，画面只允许一张可辨认人脸。其他人物必须留在画外，")
                    .append("不得出现背景人头、倒影脸、反射脸、画像脸、脸谱幻影或多重曝光；");
            } else {
                sb.append(visibleExtras ? "本镜已明确的0秒匿名群演必须按background_extras保留，群众不得复用" : "若剧情必须出现无名群众，群众不得复用").append(characterName)
                    .append("的五官，后景人物应弱化面部并保持彼此差异；");
            }
            sb.append("禁止克隆、分身、镜像脸和同脸不同装。");
        } else {
            sb.append("本镜有").append(characterCount).append("名具名角色：");
            for (int i = 0; i < chars.size(); i++) {
                if (i > 0) sb.append("、");
                CharacterRef ref = chars.get(i);
                sb.append(ref.getName());
                if (hasRefImages) {
                    int imageIndex = findRefImageIndex(refImages, storyboard.getProjectId(), ref.getName(), ref.getAppearance());
                    if (imageIndex >= 0) sb.append("仅绑定@image").append(imageIndex + 1);
                }
            }
            sb.append("。各角色必须保持各自独立五官、年龄、性别、发型和服装，禁止互相混脸、交换五官或把第一张参考脸复制给全员；")
                .append("无名群众同样不得复用任何具名角色的脸。画面中不得额外生成角色副本、镜像分身或重复人头。");
        }
        sb.append("\n");
    }

    /** 在参考图列表中找到指定角色图片的索引 */
    private int findRefImageIndex(java.util.List<String> refImages, Long projectId, String characterName) {
        return findRefImageIndex(refImages, projectId, characterName, null);
    }

    /** 在参考图列表中找到指定角色（含形象标识）图片的索引 */
    private int findRefImageIndex(java.util.List<String> refImages, Long projectId, String characterName, String appearance) {
        String targetUrl = findCharacterImageUrl(projectId, characterName, appearance);
        if (StrUtil.isBlank(targetUrl)) return -1;
        for (int i = 0; i < refImages.size(); i++) {
            if (targetUrl.equals(refImages.get(i))) return i;
        }
        return -1;
    }

    /** 在参考图列表中找到指定场景图片的索引 */
    private int findLocationRefImageIndex(java.util.List<String> refImages, Long projectId, String locationName) {
        String targetUrl = findLocationImageUrl(projectId, locationName);
        if (StrUtil.isBlank(targetUrl)) return -1;
        for (int i = 0; i < refImages.size(); i++) {
            if (targetUrl.equals(refImages.get(i))) return i;
        }
        return -1;
    }

    private String findCharacterImageUrl(Long projectId, String characterName) {
        return findCharacterImageUrl(projectId, characterName, null);
    }

    /**
     * 按角色名 + appearance 标识查找参考图。appearance 用于在多形象(青年/老年)间切换。
     * 匹配策略：changeReason 精确 → description 包含 → appearanceIndex 数字 → 都失败回退主形象(0)。
     */
    private String findCharacterImageUrl(Long projectId, String characterName, String appearance) {
        List<ShortDramaCharacter> characters = findCharactersByName(projectId, characterName);
        for (ShortDramaCharacter character : characters) {
            List<ShortDramaCharacterAppearance> appearances = characterAppearanceMapper.selectList(
                new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                    .eq(ShortDramaCharacterAppearance::getCharacterId, character.getId())
                    .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            ShortDramaCharacterAppearance matched = matchAppearance(appearances, appearance);
            String url = pickAppearanceImage(matched);
            if (StrUtil.isNotBlank(url)) return url;
            // 匹配形象无图，回退任何有图的形象
            for (ShortDramaCharacterAppearance ap : appearances) {
                url = pickAppearanceImage(ap);
                if (StrUtil.isNotBlank(url)) return url;
            }
            if (StrUtil.isNotBlank(character.getReferenceImageUrl())) {
                return character.getReferenceImageUrl();
            }
        }
        return null;
    }

    private ShortDramaCharacterAppearance matchAppearance(List<ShortDramaCharacterAppearance> appearances, String appearance) {
        return ShortDramaAppearanceResolver.resolve(appearances, appearance);
    }

    private String pickAppearanceImage(ShortDramaCharacterAppearance appearance) {
        if (appearance == null) return null;
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        if (urls.isEmpty()) return appearance.getReferenceImageUrl();
        int index = appearance.getSelectedImageIndex() != null && appearance.getSelectedImageIndex() >= 0
            && appearance.getSelectedImageIndex() < urls.size() ? appearance.getSelectedImageIndex() : 0;
        return urls.get(index);
    }

    private String findLocationImageUrl(Long projectId, String locationName) {
        ShortDramaLocation location = findLocationByName(projectId, locationName);
        if (location == null) return null;
        List<String> urls = readJsonStringList(location.getImageUrls());
        if (!urls.isEmpty()) {
            int idx = location.getSelectedImageIndex() != null && location.getSelectedImageIndex() >= 0
                && location.getSelectedImageIndex() < urls.size() ? location.getSelectedImageIndex() : 0;
            if (idx < urls.size()) return urls.get(idx);
        }
        // 回退到场景级别单图
        return location.getReferenceImageUrl();
    }

    private ShortDramaCharacter findCharacterByName(Long projectId, String name) {
        List<ShortDramaCharacter> characters = findCharactersByName(projectId, name);
        return characters.isEmpty() ? null : characters.get(0);
    }

    private List<ShortDramaCharacter> findCharactersByName(Long projectId, String name) {
        return characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>()
            .eq(ShortDramaCharacter::getProjectId, projectId)
            .eq(ShortDramaCharacter::getName, name)
            .orderByAsc(ShortDramaCharacter::getId));
    }

    private ShortDramaLocation findLocationByName(Long projectId, String name) {
        return locationMapper.selectOne(new LambdaQueryWrapper<ShortDramaLocation>()
            .eq(ShortDramaLocation::getProjectId, projectId)
            .eq(ShortDramaLocation::getName, name)
            .orderByAsc(ShortDramaLocation::getId)
            .last("limit 1"));
    }

    private List<CharacterRef> parseCharacterRefs(String charactersJson) {
        if (StrUtil.isBlank(charactersJson)) return null;
        try {
            return JsonUtils.parseArray(charactersJson, CharacterRef.class);
        } catch (Exception e) {
            log.debug("解析角色引用失败: {}", e.getMessage());
            return null;
        }
    }

    /** 从原始 JSON 响应中尽力提取视频 URL（firstOutput 解析失败时的兜底） */
    private String extractVideoUrlFromRaw(String raw) {
        if (StrUtil.isBlank(raw)) return null;
        try {
            JsonNode root = JsonUtils.parseObject(raw, JsonNode.class);
            if (root == null) return null;
            JsonNode outputs = root.path("data").path("outputs");
            if (outputs.isArray() && !outputs.isEmpty()) {
                JsonNode first = outputs.get(0);
                // 字符串直接返回
                if (first.isTextual()) return first.asText();
                // 对象尝试常见字段名
                if (first.isObject()) {
                    for (String key : new String[]{"url", "video_url", "videoUrl", "result"}) {
                        String val = first.path(key).asText(null);
                        if (StrUtil.isNotBlank(val)) return val;
                    }
                }
            }
        } catch (Exception e) { log.debug("从原始响应提取URL失败: {}", e.getMessage()); }
        return null;
    }

    @Override
    public ShortDramaStoryboardVo retrieveVideo(Long storyboardId, String videoModel, Long userId) {
        ShortDramaStoryboard storyboard = storyboardMapper.selectById(storyboardId);
        if (storyboard == null) {
            throw new IllegalArgumentException("分镜不存在");
        }
        ShortDramaProject project = projectMapper.selectById(storyboard.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        if ("done".equals(storyboard.getVideoStatus()) && StrUtil.isNotBlank(storyboard.getVideoUrl())) {
            return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        }
        String observedId = storyboard.getVideoId();
        var receipt = videoSubmissions.forPrediction(storyboardId, observedId);
        if (receipt != null && "done".equals(receipt.status()) && StrUtil.isNotBlank(receipt.videoUrl())) {
            storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, storyboardId).eq(ShortDramaStoryboard::getVideoId, observedId)
                .set(ShortDramaStoryboard::getVideoId, receipt.predictionId())
                .set(ShortDramaStoryboard::getVideoUrl, receipt.videoUrl())
                .set(ShortDramaStoryboard::getLastFrameUrl, receipt.lastFrameUrl())
                .set(ShortDramaStoryboard::getVideoStatus, "done"));
            videoComposeService.invalidateComposition(project.getId());
            return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
        }
        String predictionId = receipt != null && StrUtil.isNotBlank(receipt.predictionId()) ? receipt.predictionId() : observedId;
        if (StrUtil.isBlank(predictionId) || predictionId.startsWith("local:")) {
            if (receipt != null && ("submission_unknown".equals(receipt.status()) ||
                ("submitting".equals(receipt.status()) && System.currentTimeMillis() - receipt.createdAt() > TimeUnit.MINUTES.toMillis(10)))) {
                videoSubmissions.save(receipt.update(null, "submission_unknown", "尚未取得上游任务编号；请核对上游记录，禁止重交"));
                storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                    .eq(ShortDramaStoryboard::getId, storyboardId).eq(ShortDramaStoryboard::getVideoId, observedId)
                    .set(ShortDramaStoryboard::getVideoStatus, "submission_unknown"));
                return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
            }
            return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        }
        // Never reconstruct the queried provider/model from a client dropdown after a switch.
        String actualModel = receipt != null ? receipt.actualModel() : videoModel;
        ChatModelVo modelVo = chatModelService.selectModelByName(actualModel);
        if (modelVo == null) {
            throw new IllegalArgumentException("未找到原视频任务模型配置: " + actualModel);
        }
        VideoContext ctx = VideoContext.builder()
            .chatModelVo(modelVo)
            .prompt("retrieve")
            .videoId(predictionId)
            .build();
        MediaGenerationResponse response;
        try {
            response = videoServiceFactory.getOriginalService(receipt != null ? receipt.providerCode() : modelVo.getProviderCode())
                .retrieveVideo(ctx);
        } catch (RuntimeException ex) {
            if (receipt != null) videoSubmissions.save(receipt.update(predictionId, receipt.status(), "任务查询暂时失败；保留原任务编号，未重交"));
            log.warn("镜头{}视频查询暂时失败，保留predictionId={}: {}", storyboardId, predictionId, ex.getMessage());
            return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        }
        String videoUrl = null;
        String videoStatus = null;
        String lastFrame = null;
        if (response != null && StrUtil.isNotBlank(response.getUrl())) {
            videoUrl = response.getUrl();
            lastFrame = response.getLastFrameUrl();
            videoStatus = "done";
        } else if (response != null && ("completed".equals(response.getStatus()) || "succeeded".equals(response.getStatus()))) {
            // 已完成但 URL 提取失败，尝试从原始响应中提取
            String fallbackUrl = extractVideoUrlFromRaw(response.getRawResponse());
            if (StrUtil.isNotBlank(fallbackUrl)) {
                videoUrl = fallbackUrl;
                lastFrame = response.getLastFrameUrl();
                videoStatus = "done";
            } else {
                log.warn("视频已完成但无法提取URL, predictionId={}, raw={}", predictionId,
                    response.getRawResponse() != null ? response.getRawResponse().substring(0, Math.min(300, response.getRawResponse().length())) : "null");
                videoStatus = "submission_unknown";
            }
        } else if (response != null && videoTerminalFailure(response.getStatus())) {
            videoStatus = "failed";
        }
        if (videoStatus == null && observedId != null && observedId.startsWith("local:")) videoStatus = "generating";
        if (videoStatus != null) {
            if (receipt != null) videoSubmissions.save(receipt.result(predictionId, videoUrl, lastFrame, videoStatus,
                "submission_unknown".equals(videoStatus) ? "上游报告完成但未返回媒体；保留任务编号，请核对记录" : ""));
            int updated = storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, storyboardId)
                .eq(ShortDramaStoryboard::getVideoId, observedId)
                .set(ShortDramaStoryboard::getVideoId, predictionId)
                .set("done".equals(videoStatus), ShortDramaStoryboard::getVideoUrl, videoUrl)
                .set(ShortDramaStoryboard::getVideoStatus, videoStatus)
                .set(StrUtil.isNotBlank(lastFrame), ShortDramaStoryboard::getLastFrameUrl, lastFrame));
            if (updated > 0) {
                videoComposeService.invalidateComposition(project.getId());
            }
        }
        return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
    }

    @Override
    public List<ShortDramaStoryboardVo> generateAllVideos(Long projectId, String videoModel, Long userId) {
        return generateAllVideos(projectId, videoModel, userId, null, null);
    }

    @Override
    public List<ShortDramaStoryboardVo> generateAllVideos(Long projectId, String videoModel, Long userId, Integer sceneStart, Integer sceneCount) {
        if (sceneStart != null && sceneStart < 1) throw new IllegalArgumentException("sceneStart 必须为实际镜号，至少1");
        if (sceneCount != null && (sceneCount < 1 || sceneCount > 100)) throw new IllegalArgumentException("sceneCount 须为1至100");
        ShortDramaProject project = projectMapper.selectById(projectId);
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        List<ShortDramaStoryboard> storyboards = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getProjectId, projectId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo));

        var selected = selectVideoRange(storyboards, sceneStart, sceneCount);
        for (ShortDramaStoryboard shot : selected) {
            if (("done".equals(shot.getVideoStatus()) && StrUtil.isNotBlank(shot.getVideoUrl())) || videoSubmissionInFlight(shot)) continue;
            ShortDramaVideoDuration.seconds(shot.getContinuityJson());
            ShortDramaVideoPromptReview.validate(shot.getSceneNo(), shot.getVideoPrompt(), ShortDramaVideoDuration.reviewSeconds(shot.getContinuityJson()), shot.getSourceText());
        }
        var selectedIds = selected.stream().map(ShortDramaStoryboard::getId).collect(java.util.stream.Collectors.toSet());
        List<VideoGenerationGroup> groups = new ArrayList<>();
        for (var fullGroup : groupContinuousScenes(storyboards)) {
            var shots = fullGroup.stream().filter(shot -> selectedIds.contains(shot.getId())).toList();
            if (shots.isEmpty()) continue;
            int offset = fullGroup.indexOf(shots.get(0));
            String initialFrame = null;
            ShortDramaStoryboard first = shots.get(0);
            if (offset > 0 && !videoSubmissionInFlight(first) && !("done".equals(first.getVideoStatus()) && StrUtil.isNotBlank(first.getVideoUrl()))) {
                ShortDramaStoryboard prior = fullGroup.get(offset - 1);
                // A ranged batch may query the predecessor, but must never submit outside its selected range.
                var previous = retrieveVideo(prior.getId(), videoModel, userId);
                if (previous != null && "generating".equals(previous.getVideoStatus()) && previous.getVideoId() != null && !previous.getVideoId().startsWith("local:"))
                    previous = ensureVideoDone(prior.getId(), videoModel, userId);
                if (previous == null || !"done".equals(previous.getVideoStatus()) || StrUtil.isBlank(previous.getLastFrameUrl()))
                    throw new IllegalStateException("本批起点需要镜头" + prior.getSceneNo() + "的已完成末帧；请先完成上一镜，未提交本批");
                initialFrame = previous.getLastFrameUrl();
            }
            groups.add(new VideoGenerationGroup(shots, initialFrame));
        }

        // 跨场景组并发，组上限 4
        int parallel = Math.min(4, Math.max(1, groups.size()));
        java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(parallel,
            r -> { Thread t = new Thread(r, "short-drama-video-gen"); t.setDaemon(true); return t; });
        try {
            List<java.util.concurrent.Future<List<ShortDramaStoryboardVo>>> futures = new ArrayList<>();
            for (VideoGenerationGroup group : groups) {
                futures.add(pool.submit(() -> generateGroupSerial(group.shots(), videoModel, userId, group.initialFrame())));
            }
            // 按 sceneNo 顺序汇总结果
            List<ShortDramaStoryboardVo> result = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            for (java.util.concurrent.Future<List<ShortDramaStoryboardVo>> f : futures) {
                try { result.addAll(f.get()); }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("视频生成等待被中断，请检查各镜头状态", e);
                }
                catch (java.util.concurrent.ExecutionException e) {
                    failures.add(e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
                }
            }
            if (!failures.isEmpty()) throw new IllegalStateException(String.join("；", failures));
            result.sort(java.util.Comparator.comparing(v -> v.getSceneNo() == null ? Integer.MAX_VALUE : v.getSceneNo()));
            return result;
        } finally {
            pool.shutdownNow();
        }
    }

    private record VideoGenerationGroup(List<ShortDramaStoryboard> shots, String initialFrame) { }

    static List<ShortDramaStoryboard> selectVideoRange(List<ShortDramaStoryboard> shots, Integer sceneStart, Integer sceneCount) {
        return shots.stream().filter(shot -> sceneStart == null || (shot.getSceneNo() != null && shot.getSceneNo() >= sceneStart))
            .limit(sceneCount == null ? Long.MAX_VALUE : sceneCount).toList();
    }

    static List<List<ShortDramaStoryboard>> groupContinuousScenes(List<ShortDramaStoryboard> storyboards) {
        List<List<ShortDramaStoryboard>> groups = new ArrayList<>();
        String previousKey = null;
        for (ShortDramaStoryboard sb : storyboards) {
            String scene = "";
            if (StrUtil.isNotBlank(sb.getContinuityJson())) {
                scene = cn.hutool.json.JSONUtil.parseObj(sb.getContinuityJson()).getStr("scene_number", "");
            }
            String key = scene + "|" + StrUtil.blankToDefault(sb.getLocationName(), "");
            if (!key.equals(previousKey)) groups.add(new ArrayList<>());
            groups.get(groups.size() - 1).add(sb);
            previousKey = key;
        }
        return groups;
    }

    /** 同场景组内串行生成；包括第一镜在内，完成并取得末帧后才允许下一镜提交。 */
    List<ShortDramaStoryboardVo> generateGroupSerial(List<ShortDramaStoryboard> group, String videoModel, Long userId) {
        return generateGroupSerial(group, videoModel, userId, null);
    }

    List<ShortDramaStoryboardVo> generateGroupSerial(List<ShortDramaStoryboard> group, String videoModel, Long userId, String initialFrame) {
        List<ShortDramaStoryboardVo> result = new ArrayList<>();
        String prevLastFrameUrl = initialFrame;
        for (int index = 0; index < group.size(); index++) {
            ShortDramaStoryboard sb = group.get(index);
            ShortDramaStoryboardVo vo = generateVideo(sb.getId(), videoModel, userId, prevLastFrameUrl);
            if (vo != null && ("submission_unknown".equals(vo.getVideoStatus())
                || (vo.getVideoId() != null && vo.getVideoId().startsWith("local:"))))
                throw new IllegalStateException("镜头" + sb.getSceneNo() + "提交结果尚未确认，已停止本组，禁止重复提交");
            if (vo == null || !"done".equals(vo.getVideoStatus())) {
                vo = ensureVideoDone(sb.getId(), videoModel, userId);
            }
            if (vo == null || !"done".equals(vo.getVideoStatus())) {
                throw new IllegalStateException("镜头" + sb.getSceneNo() + "尚未成功完成，已停止后续连续镜头");
            }
            result.add(vo);
            prevLastFrameUrl = vo.getLastFrameUrl();
            if (index < group.size() - 1 && StrUtil.isBlank(prevLastFrameUrl)) {
                throw new IllegalStateException("镜头" + sb.getSceneNo() + "缺少末帧，已停止后续连续镜头");
            }
        }
        return result;
    }

    /**
     * 后台同步轮询单镜视频直到 done/failed（用于同场景末帧拼接时拿到末帧再喂下一镜）。
     * 单镜累计轮询不超过 5 分钟，超时按当前状态返回。
     */
    ShortDramaStoryboardVo ensureVideoDone(Long storyboardId, String videoModel, Long userId) {
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        try {
            while (System.currentTimeMillis() < deadline) {
                ShortDramaStoryboardVo vo = retrieveVideo(storyboardId, videoModel, userId);
                if (vo == null) return null;
                if ("done".equals(vo.getVideoStatus()) || "failed".equals(vo.getVideoStatus()) || "submission_unknown".equals(vo.getVideoStatus())
                    || (vo.getVideoId() != null && vo.getVideoId().startsWith("local:"))) {
                    return vo;
                }
                try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return vo; }
            }
        } catch (Exception e) {
            log.warn("镜头{}末帧等待轮询异常: {}", storyboardId, e.getMessage());
        }
        return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
    }

    // ==================== 资产分析与管理 ====================

    @Override
    public ShortDramaDetailVo analyzeAssets(Long projectId, Long scriptId, Long userId, String model) {
        ShortDramaProject project = validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) {
            throw new IllegalArgumentException("剧本不存在");
        }
        ChatModelVo modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        if (!beginAssetGeneration(scriptId)) throw new IllegalStateException("该剧本已有文字生成任务，请查询原任务");
        try {
            generateUnifiedAssets(project, script, modelVo, userId, null, null);
            return getDetail(projectId, userId);
        } finally { assetGenerationStates.remove(scriptId); }
    }

    private synchronized boolean beginAssetGeneration(Long scriptId) {
        if (storyboardGenerationStates.containsKey(scriptId)) return false;
        return assetGenerationStates.putIfAbsent(scriptId, new AtomicBoolean(true)) == null;
    }

    private static String assetSourceHash(ShortDramaScript script) {
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(firstNotBlank(script.getScriptText(), script.getOutlineText(), "")
            + "\n" + ShortDramaScriptPreparation.worldContext(script) + "\n" + Objects.toString(script.getTone(), ""));
    }

    @Override
    public java.util.Map<String, Object> assetAnalysisStatus(Long projectId, Long scriptId, String requestId, Long userId) {
        validateProjectOwner(projectId, userId);
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var status = assetStatuses.read(projectId, scriptId, ShortDramaPlanningStatus.checkedRequestId(requestId));
        var data = assetStatusData(status, projectId, scriptId);
        data.put("currentScriptHash", assetSourceHash(script));
        data.put("observedAt", java.time.Instant.now().toString());
        return data;
    }

    private java.util.Map<String, Object> assetStatusData(ShortDramaPlanningStatus.Status status, Long projectId, Long scriptId) {
        var data = new LinkedHashMap<String, Object>();
        boolean active = assetGenerationStates.containsKey(scriptId);
        data.put("schemaVersion", "asset-status-v1"); data.put("projectId", projectId); data.put("scriptId", scriptId);
        data.put("requestId", status == null ? null : status.requestId());
        data.put("state", status == null ? (active ? "running" : "not_observed")
            : !status.terminal() && !assetStatuses.currentRuntime(status) ? "submission_unknown" : status.state());
        data.put("terminal", status != null && status.terminal()); data.put("activeLock", active); data.put("retrySafe", false);
        if (status != null) {
            data.put("scriptHash", status.scriptHash()); data.put("model", status.model());
            data.put("submittedAt", status.submittedAt()); data.put("updatedAt", status.updatedAt());
            data.put("assetCount", status.panelCount()); data.put("error", status.error());
            var progress = ShortDramaPlanningProgress.read(projectId, scriptId, "assets:" + status.requestId());
            if (progress != null) data.put("progress", progress);
        }
        return data;
    }

    @Override
    public SseEmitter analyzeAssetsStream(Long projectId, Long scriptId, Long userId, String model, String requestId) {
        var project = validateProjectOwner(projectId, userId);
        var script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
        var modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        String requested = ShortDramaPlanningStatus.checkedRequestId(requestId), sourceHash = assetSourceHash(script);
        String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
        var emitter = new SseEmitter(7_200_000L);
        activeEmitters.put(emitter, new AtomicBoolean(true));
        emitter.onCompletion(() -> closeEmitter(emitter)); emitter.onTimeout(() -> closeEmitter(emitter)); emitter.onError(error -> closeEmitter(emitter));
        if (requested != null) {
            var existing = assetStatuses.read(projectId, scriptId, requested);
            if (existing != null) {
                if (!sourceHash.equals(existing.scriptHash()) || !modelVo.getModelName().equals(existing.model())) {
                    completeEmitter(emitter); throw new IllegalArgumentException("requestId已用于不同的剧本或模型");
                }
                sendEmitterEvent(emitter, SseEmitter.event().name("submission").data(assetStatusData(existing, projectId, scriptId)));
                completeEmitter(emitter); return emitter;
            }
        }
        if (!beginAssetGeneration(scriptId)) {
            completeEmitter(emitter); throw new IllegalStateException("该剧本已有文字生成任务，请查询原任务");
        }
        final ShortDramaPlanningStatus.Status queued;
        try {
            var previous = assetStatuses.read(projectId, scriptId, null);
            if (previous != null && !previous.terminal()) {
                if (assetStatuses.currentRuntime(previous)) throw new IllegalStateException("原资产请求尚未结束，请查询原任务");
                assetStatuses.update(previous, "error", null, "服务已重启，原资产任务不在当前实例中，原资产保留");
            }
            queued = assetStatuses.queued(projectId, scriptId, requested == null ? UUID.randomUUID().toString() : requested, sourceHash, modelVo.getModelName(), 0);
        } catch (RuntimeException e) { assetGenerationStates.remove(scriptId); completeEmitter(emitter); throw e; }
        var progress = new ShortDramaPlanningProgress.Tracker(queued.requestId(), queued.submittedAt(),
            ShortDramaPlanningProgress.persistence(projectId, scriptId, "assets:" + queued.requestId(), snapshot ->
                sendEmitterEvent(emitter, SseEmitter.event().name("progress").data(JsonUtils.toJsonString(snapshot)))));
        planningProgress.put(emitter, progress);
        progress.phase("assets", "资产分析请求已登记，等待模型返回");
        sendEmitterEvent(emitter, SseEmitter.event().name("submission").data(assetStatusData(queued, projectId, scriptId)));
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, () -> {
                        assetStatuses.update(queued, "running", null, null);
                        int count = generateUnifiedAssets(project, script, modelVo, userId, emitter, progress);
                        var completed = assetStatuses.update(queued, "done", count, null);
                        progress.terminal("done");
                        sendEmitterEvent(emitter, SseEmitter.event().name("complete").data(assetStatusData(completed, projectId, scriptId)));
                        completeEmitter(emitter);
                    });
                } catch (Exception e) {
                    log.warn("资产分析未完成 projectId={}, requestId={}", projectId, queued.requestId(), e);
                    org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, () -> assetStatuses.update(queued, "error", null, e.getMessage()));
                    progress.terminal("error");
                    sendEmitterEvent(emitter, SseEmitter.event().name("error").data(Map.of("requestId", queued.requestId(), "message", ShortDramaSceneCandidateDiagnostics.redact(Objects.toString(e.getMessage(), "资产分析失败")))));
                    completeEmitterWithError(emitter, e);
                } finally { planningProgress.remove(emitter); assetGenerationStates.remove(scriptId); }
            }, storyboardPlanningExecutor);
        } catch (RuntimeException e) {
            assetStatuses.update(queued, "error", null, "后台未接受任务：" + e.getMessage()); progress.terminal("error");
            planningProgress.remove(emitter); assetGenerationStates.remove(scriptId); completeEmitterWithError(emitter, e);
        }
        return emitter;
    }

    private int generateUnifiedAssets(ShortDramaProject project, ShortDramaScript script, ChatModelVo modelVo, Long userId,
        SseEmitter emitter, ShortDramaPlanningProgress.Tracker progress) {
        String source = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        if (StrUtil.isBlank(source)) throw new IllegalArgumentException("请先保存有效剧本正文");
        Long projectId = project.getId();
        var snapshot = skillCatalog.snapshot(project);
        var request = ShortDramaWritingRequest.forText(modelVo.getModelName(), source);
        var service = getChatService(modelVo);
        var chat = service.buildChatModel(modelVo, request);
        var streaming = emitter == null ? null : service.buildStreamingChatModel(modelVo, request);
        int[] ordinals = new int[4];
        var preview = new ShortDramaAssetGeneration.Preview((category, item) -> {
            if (progress != null) progress.assetCard(category, ++ordinals[category], "draft", item);
        });
        if (progress != null) progress.phase("assets", "正在提取角色、场景和道具，返回后逐项显示");
        String raw = streamingChat(streaming, chat, ShortDramaAssetGeneration.prompt(source + ShortDramaScriptPreparation.worldContext(script), script.getTone(), snapshot.direction()),
            emitter, "assets", preview::accept, "角色、场景和道具");
        var result = ShortDramaAssetGeneration.parse(raw);
        List<AssetGeneratedCharacter> extractedCharacters = result.characters().stream()
            .map(item -> parseJson(item.toString(), AssetGeneratedCharacter.class)).toList();
        List<AssetGeneratedLocation> extractedLocations = result.locations().stream()
            .map(item -> parseJson(item.toString(), AssetGeneratedLocation.class)).toList();
        requireAssetCharacters(extractedCharacters); requireAssetLocations(extractedLocations);
        if (progress != null) progress.phase("persist", "资产清单已校验，正在保存");
        String sourceHash = assetSourceHash(script);
        transactionTemplate.executeWithoutResult(status -> {
            // Lock the script row so a concurrent script save cannot slip between verification and commit.
            var current = scriptMapper.selectOne(new LambdaQueryWrapper<ShortDramaScript>().eq(ShortDramaScript::getId, script.getId()).last("FOR UPDATE"));
            if (current == null || !sourceHash.equals(assetSourceHash(current))) throw new IllegalStateException("分析期间剧本已修改，本轮结果未保存，原资产保留");
            skillCatalog.verify(snapshot, projectMapper.selectById(projectId));
            Set<String> characterNames = characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId))
                .stream().map(c -> c.getName().trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            for (var character : extractedCharacters) {
                character.setName(character.getName().trim());
                if (characterNames.add(character.getName().toLowerCase(Locale.ROOT))) insertCharacter(projectId, character, character.getVisualDescription());
            }
            Set<String> locationNames = locationMapper.selectList(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId))
                .stream().map(l -> l.getName().trim().toLowerCase(Locale.ROOT)).collect(java.util.stream.Collectors.toSet());
            for (var location : extractedLocations) {
                if (!locationNames.add(location.getName().trim().toLowerCase(Locale.ROOT))) continue;
                var entity = new ShortDramaLocation(); entity.setId(IdUtil.getSnowflakeNextId()); entity.setProjectId(projectId);
                entity.setName(location.getName().trim()); entity.setSummary(location.getSummary());
                entity.setHasCrowd(Boolean.TRUE.equals(location.getHasCrowd())); entity.setCrowdDescription(location.getCrowdDescription());
                entity.setAvailableSlots(JsonUtils.toJsonString(location.getAvailableSlots())); entity.setDescriptions(JsonUtils.toJsonString(location.getDescriptions()));
                locationMapper.insert(entity);
            }
            visualAssets.saveAnalyzedProps(projectId, result.props(), userId);
        });
        if (progress != null) {
            for (int category = 1; category <= 3; category++) {
                progress.clearDrafts(category);
                var items = category == 1 ? result.characters() : category == 2 ? result.locations() : result.props();
                for (int i = 0; i < items.size(); i++) progress.assetCard(category, i + 1, "ready", items.get(i));
            }
        }
        log.info("资产单次生成完成: projectId={}, model={}, characters={}, locations={}, props={}", projectId, modelVo.getModelName(),
            result.characters().size(), result.locations().size(), result.props().size());
        return result.size();
    }

    @Override
    public ShortDramaCharacterVo saveCharacter(ShortDramaCharacterBo bo, Long userId) {
        ShortDramaCharacter entity = MapstructUtils.convert(bo, ShortDramaCharacter.class);
        validateProjectOwner(entity.getProjectId(), userId);
        if (entity.getId() == null) {
            characterMapper.insert(entity);
        } else {
            characterMapper.updateById(entity);
        }
        ShortDramaCharacterVo vo = MapstructUtils.convert(entity, ShortDramaCharacterVo.class);
        vo.setAppearances(characterAppearanceMapper.selectVoList(
            new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                .eq(ShortDramaCharacterAppearance::getCharacterId, entity.getId())
                .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex)));
        return vo;
    }

    @Override
    public ShortDramaLocationVo saveLocation(ShortDramaLocationBo bo, Long userId) {
        ShortDramaLocation entity = MapstructUtils.convert(bo, ShortDramaLocation.class);
        validateProjectOwner(entity.getProjectId(), userId);
        if (entity.getId() == null) {
            locationMapper.insert(entity);
        } else {
            locationMapper.updateById(entity);
        }
        return MapstructUtils.convert(entity, ShortDramaLocationVo.class);
    }

    @Override
    public ShortDramaCharacterVo generateCharacterImage(Long characterId, String imageModel, String referenceImageUrl, Long userId) {
        ShortDramaCharacter character = characterMapper.selectById(characterId);
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String basePrompt = firstNotBlank(character.getVisualDescription(), character.getName());
        String finalPrompt = buildCharacterPrompt(basePrompt, character.getProjectId());
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size("3:2")
                .seed(ShortDramaImageConstants.styleSeed(character.getProjectId()))
                .image(referenceImage).build());
        if (StrUtil.isNotBlank(imageUrl)) {
            character.setReferenceImageUrl(imageUrl);
            characterMapper.updateById(character);
        }
        ShortDramaCharacterVo vo = MapstructUtils.convert(character, ShortDramaCharacterVo.class);
        vo.setAppearances(characterAppearanceMapper.selectVoList(
            new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                .eq(ShortDramaCharacterAppearance::getCharacterId, characterId)
                .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex)));
        return vo;
    }

    @Override
    public ShortDramaLocationVo generateLocationImage(Long locationId, String imageModel, String referenceImageUrl, Long userId) {
        return generateLocationImage(locationId, imageModel, referenceImageUrl, null, userId);
    }

    @Override
    public ShortDramaLocationVo generateLocationImage(Long locationId, String imageModel, String referenceImageUrl, String revisionRequirements, Long userId) {
        boolean revision = ShortDramaLocationArtPrompt.isRevision(null, revisionRequirements);
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String prompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
        String finalPrompt = buildLocationPrompt(location.getName(), prompt, location.getProjectId(), revisionRequirements);
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        if (readJsonStringList(location.getImageUrls()).size() >= ShortDramaImageConstants.MAX_IMAGE_VARIANTS)
            throw new IllegalArgumentException("场景图片已达上限，请审阅并删除不需要的候选后再生成");
        var evidence = imagePromptEvidence.prepare("location", locationId, location.getProjectId(), userId, imageModel,
            revision ? "location_revision" : "location", revisionRequirements, finalPrompt, referenceImage);
        log.info("短剧地点图提示词证据 attemptId={} assetId={} purpose={} promptSha256={}",
            evidence.attemptId(), locationId, evidence.referencePurpose(), evidence.promptSha256());
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size(projectAspectRatio(location.getProjectId())).image(referenceImage).build());
        imagePromptEvidence.submitted(evidence, null, StrUtil.isNotBlank(imageUrl) ? "completed" : "no_image_returned");
        if (StrUtil.isNotBlank(imageUrl)) {
            List<String> urls = readJsonStringList(location.getImageUrls());
            List<String> descs = readJsonStringList(location.getImageDescriptions());
            if (urls.contains(imageUrl)) return MapstructUtils.convert(location, ShortDramaLocationVo.class);
            if (revision) {
                location.setPreviousImageUrls(location.getImageUrls());
                location.setPreviousDescriptions(location.getImageDescriptions());
            }
            addImageUrl(urls, imageUrl, descs, finalPrompt);
            location.setImageUrls(JsonUtils.toJsonString(urls));
            location.setImageDescriptions(JsonUtils.toJsonString(descs));
            if (!revision) {
                location.setReferenceImageUrl(imageUrl);
                location.setSelectedImageIndex(urls.size() - 1);
            }
            locationMapper.updateById(location);
        }
        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
    }

    @Override
    public Boolean deleteCharacter(Long characterId, Long userId) {
        ShortDramaCharacter character = characterMapper.selectById(characterId);
        if (character == null) return false;
        validateProjectOwner(character.getProjectId(), userId);
        characterAppearanceMapper.delete(new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
            .eq(ShortDramaCharacterAppearance::getCharacterId, characterId));
        return characterMapper.deleteById(characterId) > 0;
    }

    @Override
    public Boolean deleteLocation(Long locationId, Long userId) {
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) return false;
        validateProjectOwner(location.getProjectId(), userId);
        return locationMapper.deleteById(locationId) > 0;
    }

    @Override
    public ShortDramaCharacterAppearanceVo saveAppearance(ShortDramaCharacterAppearanceBo bo, Long userId) {
        ShortDramaCharacterAppearance entity = MapstructUtils.convert(bo, ShortDramaCharacterAppearance.class);
        ShortDramaCharacter character = characterMapper.selectById(entity.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        if (entity.getAppearanceIndex() == null) entity.setAppearanceIndex(0);
        if (entity.getChangeReason() == null) entity.setChangeReason("手动添加");
        if (entity.getId() == null) {
            characterAppearanceMapper.insert(entity);
        } else {
            characterAppearanceMapper.updateById(entity);
        }
        return MapstructUtils.convert(entity, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public Boolean deleteAppearance(Long appearanceId, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) return false;
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) return false;
        validateProjectOwner(character.getProjectId(), userId);
        return characterAppearanceMapper.deleteById(appearanceId) > 0;
    }

    @Override
    public ShortDramaCharacterAppearanceVo generateAppearanceImage(Long appearanceId, String imageModel, String referenceImageUrl, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
        String finalPrompt = buildCharacterPrompt(basePrompt, character.getProjectId());
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size("3:2")
                .seed(ShortDramaImageConstants.styleSeed(character.getProjectId()))
                .image(referenceImage).build());
        if (StrUtil.isNotBlank(imageUrl)) {
            // 备份当前状态到 previous 字段
            appearance.setPreviousImageUrls(appearance.getImageUrls());
            appearance.setPreviousDescriptions(appearance.getImageDescriptions());
            // 追加新图
            appearance.setReferenceImageUrl(imageUrl);
            List<String> urls = readJsonStringList(appearance.getImageUrls());
            List<String> descs = readJsonStringList(appearance.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
            appearance.setImageUrls(JsonUtils.toJsonString(urls));
            appearance.setImageDescriptions(JsonUtils.toJsonString(descs));
            appearance.setSelectedImageIndex(urls.size() - 1);
            characterAppearanceMapper.updateById(appearance);
        }
        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaCharacterAppearanceVo regenerateAppearanceImage(Long appearanceId, String imageModel, String referenceImageUrl, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
        String finalPrompt = buildCharacterPrompt(basePrompt, character.getProjectId());
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size("3:2")
                .seed(ShortDramaImageConstants.styleSeed(character.getProjectId()))
                .image(referenceImage).build());
        if (StrUtil.isNotBlank(imageUrl)) {
            appearance.setReferenceImageUrl(imageUrl);
            List<String> urls = readJsonStringList(appearance.getImageUrls());
            List<String> descs = readJsonStringList(appearance.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
            appearance.setImageUrls(JsonUtils.toJsonString(urls));
            appearance.setImageDescriptions(JsonUtils.toJsonString(descs));
            appearance.setSelectedImageIndex(urls.size() - 1);
            characterAppearanceMapper.updateById(appearance);
        }
        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaCharacterAppearanceVo selectAppearanceImage(Long appearanceId, Integer index, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        if (index < 0 || index >= urls.size()) throw new IllegalArgumentException("图片索引无效: " + index);
        appearance.setSelectedImageIndex(index);
        if (urls.get(index) != null) appearance.setReferenceImageUrl(urls.get(index));
        characterAppearanceMapper.updateById(appearance);
        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaCharacterAppearanceVo deleteAppearanceImage(Long appearanceId, Integer index, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        List<String> descs = readJsonStringList(appearance.getImageDescriptions());
        if (urls.size() <= 1) throw new IllegalStateException("至少保留一张角色图片，无法删除最后一张");
        if (index < 0 || index >= urls.size()) throw new IllegalArgumentException("图片索引无效: " + index);
        appearance.setPreviousImageUrls(appearance.getImageUrls());
        appearance.setPreviousDescriptions(appearance.getImageDescriptions());
        urls.remove((int) index);
        if (index < descs.size()) descs.remove((int) index);
        int selected = normalizeSelectedIndexAfterDelete(appearance.getSelectedImageIndex(), index, urls.size());
        appearance.setImageUrls(JsonUtils.toJsonString(urls));
        appearance.setImageDescriptions(JsonUtils.toJsonString(descs));
        appearance.setSelectedImageIndex(selected);
        appearance.setReferenceImageUrl(urls.get(selected));
        characterAppearanceMapper.updateById(appearance);
        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaCharacterAppearanceVo undoAppearanceImage(Long appearanceId, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);
        if (StrUtil.isBlank(appearance.getPreviousImageUrls())) throw new IllegalStateException("没有可撤销的图片");
        appearance.setImageUrls(appearance.getPreviousImageUrls());
        appearance.setImageDescriptions(appearance.getPreviousDescriptions());
        appearance.setPreviousImageUrls(null);
        appearance.setPreviousDescriptions(null);
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        int idx = urls.isEmpty() ? -1 : urls.size() - 1;
        appearance.setSelectedImageIndex(idx);
        appearance.setReferenceImageUrl(idx >= 0 ? urls.get(idx) : null);
        characterAppearanceMapper.updateById(appearance);
        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaLocationVo regenerateLocationImage(Long locationId, String imageModel, String referenceImageUrl, Long userId) {
        return regenerateLocationImage(locationId, imageModel, referenceImageUrl, null, userId);
    }

    @Override
    public ShortDramaLocationVo regenerateLocationImage(Long locationId, String imageModel, String referenceImageUrl, String revisionRequirements, Long userId) {
        return generateLocationImage(locationId, imageModel, referenceImageUrl, revisionRequirements, userId);
    }

    @Override
    public ShortDramaLocationVo selectLocationImage(Long locationId, Integer index, Long userId) {
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        List<String> urls = readJsonStringList(location.getImageUrls());
        if (index < 0 || index >= urls.size()) throw new IllegalArgumentException("图片索引无效: " + index);
        location.setSelectedImageIndex(index);
        if (urls.get(index) != null) location.setReferenceImageUrl(urls.get(index));
        locationMapper.updateById(location);
        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
    }

    @Override
    public ShortDramaLocationVo deleteLocationImage(Long locationId, Integer index, Long userId) {
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        List<String> urls = readJsonStringList(location.getImageUrls());
        List<String> descs = readJsonStringList(location.getImageDescriptions());
        if (urls.size() <= 1) throw new IllegalStateException("至少保留一张场景图片，无法删除最后一张");
        if (index < 0 || index >= urls.size()) throw new IllegalArgumentException("图片索引无效: " + index);
        location.setPreviousImageUrls(location.getImageUrls());
        location.setPreviousDescriptions(location.getImageDescriptions());
        urls.remove((int) index);
        if (index < descs.size()) descs.remove((int) index);
        int selected = normalizeSelectedIndexAfterDelete(location.getSelectedImageIndex(), index, urls.size());
        location.setImageUrls(JsonUtils.toJsonString(urls));
        location.setImageDescriptions(JsonUtils.toJsonString(descs));
        location.setSelectedImageIndex(selected);
        location.setReferenceImageUrl(urls.get(selected));
        locationMapper.updateById(location);
        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
    }

    private static int normalizeSelectedIndexAfterDelete(Integer selectedIndex, int deletedIndex, int remainingSize) {
        int selected = selectedIndex != null ? selectedIndex : 0;
        if (selected > deletedIndex) selected--;
        else if (selected == deletedIndex) selected = Math.min(deletedIndex, remainingSize - 1);
        return Math.max(0, Math.min(selected, remainingSize - 1));
    }

    @Override
    public ShortDramaLocationVo undoLocationImage(Long locationId, Long userId) {
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        if (StrUtil.isBlank(location.getPreviousImageUrls())) throw new IllegalStateException("没有可撤销的图片");
        location.setImageUrls(location.getPreviousImageUrls());
        location.setImageDescriptions(location.getPreviousDescriptions());
        location.setPreviousImageUrls(null);
        location.setPreviousDescriptions(null);
        List<String> urls = readJsonStringList(location.getImageUrls());
        int idx = urls.isEmpty() ? -1 : urls.size() - 1;
        location.setSelectedImageIndex(idx);
        location.setReferenceImageUrl(idx >= 0 ? urls.get(idx) : null);
        locationMapper.updateById(location);
        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
    }

    // ==================== 异步图片生成（轮询进度） ====================

    @Override
    public String uploadReferenceImage(org.springframework.web.multipart.MultipartFile file, String model, Long userId) {
        if (userId == null) throw new IllegalArgumentException("用户未登录");
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("参考图不能为空");
        if (file.getSize() > 10L * 1024 * 1024) throw new IllegalArgumentException("参考图不能超过10MB");
        String contentType = firstNotBlank(file.getContentType(), "");
        if (!("image/jpeg".equalsIgnoreCase(contentType)
            || "image/png".equalsIgnoreCase(contentType)
            || "image/webp".equalsIgnoreCase(contentType))) {
            throw new IllegalArgumentException("参考图仅支持JPEG、PNG或WebP");
        }
        ChatModelVo modelVo = chatModelService.selectModelByName(model);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + model);
        try {
            return imageServiceFactory.getOriginalService(modelVo.getProviderCode())
                .uploadMedia(modelVo, file.getBytes(), file.getOriginalFilename(), contentType);
        } catch (IOException e) {
            throw new IllegalStateException("读取参考图失败: " + e.getMessage(), e);
        }
    }

    @Override
    public MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl, Long userId) {
        return startImageGeneration(assetType, assetId, model, referenceImageUrl, null, null, userId);
    }

    @Override
    public MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl,
                                                       String referencePurpose, String revisionRequirements, Long userId) {
        return startImageGeneration(assetType, assetId, model, referenceImageUrl, referencePurpose,
            revisionRequirements, null, userId);
    }

    @Override
    public MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl,
                                                       String referencePurpose, String revisionRequirements,
                                                       List<String> styleReferenceImageUrls, Long userId) {
        boolean locationAsset = "location".equals(assetType);
        boolean revision = locationAsset ? ShortDramaLocationArtPrompt.isRevision(referencePurpose, revisionRequirements)
            : ShortDramaCharacterArtPrompt.isRevision(referencePurpose, revisionRequirements);
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        List<String> styleImages = styleReferenceImageUrls == null ? List.of()
            : styleReferenceImageUrls.stream().map(ShortDramaServiceImpl::validateReferenceImageUrl).toList();
        List<String> imageReferences = ShortDramaImageStyleReferences.ordered(referenceImage, styleImages);
        if (revision && !locationAsset && (!"appearance".equals(assetType) || StrUtil.isBlank(referenceImage))) {
            throw new IllegalArgumentException("身份修订仅用于角色形象，且必须提供身份参考图");
        }
        ChatModelVo modelVo = chatModelService.selectModelByName(model);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + model);

        String prompt;
        String size;
        Long projectId;
        Integer seed = null;
        if ("appearance".equals(assetType)) {
            ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(assetId);
            if (appearance == null) throw new IllegalArgumentException("形象不存在");
            ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
            if (character == null) throw new IllegalArgumentException("角色不存在");
            validateProjectOwner(character.getProjectId(), userId);
            projectId = character.getProjectId();
            if (readJsonStringList(appearance.getImageUrls()).size() >= ShortDramaImageConstants.MAX_IMAGE_VARIANTS) {
                throw new IllegalArgumentException("角色图片已达上限，请审阅并删除不需要的候选后再生成");
            }
            String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
            prompt = buildCharacterPrompt(basePrompt, projectId, referencePurpose, revisionRequirements);
            size = "3:2";
            seed = ShortDramaImageConstants.styleSeed(character.getProjectId());
        } else if ("location".equals(assetType)) {
            ShortDramaLocation location = locationMapper.selectById(assetId);
            if (location == null) throw new IllegalArgumentException("场景不存在");
            validateProjectOwner(location.getProjectId(), userId);
            projectId = location.getProjectId();
            if (readJsonStringList(location.getImageUrls()).size() >= ShortDramaImageConstants.MAX_IMAGE_VARIANTS)
                throw new IllegalArgumentException("场景图片已达上限，请审阅并删除不需要的候选后再生成");
            String basePrompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
            prompt = buildLocationPrompt(location.getName(), basePrompt, projectId, revisionRequirements);
            size = projectAspectRatio(location.getProjectId());
            seed = ShortDramaImageConstants.styleSeed(location.getProjectId());
        } else {
            throw new IllegalArgumentException("不支持的资产类型: " + assetType);
        }

        if (!styleImages.isEmpty()) {
            prompt += ShortDramaImageStyleReferences.direction(locationAsset, imageReferences.size());
        }
        ShortDramaImagePromptEvidence.Evidence evidence = imagePromptEvidence.prepare(assetType, assetId, projectId, userId,
            model, locationAsset ? (revision ? "location_revision" : "location") : (revision ? "identity_revision" : "identity"), revisionRequirements, prompt, referenceImage);
        log.info("短剧图片提示词证据 attemptId={} assetType={} assetId={} purpose={} promptSha256={}",
            evidence.attemptId(), assetType, assetId, evidence.referencePurpose(), evidence.promptSha256());
        MediaGenerationResponse response = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .startImageGeneration(ImageContext.builder()
                .chatModelVo(modelVo).prompt(prompt).size(size).seed(seed)
                .image(referenceImage).referenceImages(styleImages.isEmpty() ? null : imageReferences).build());
        try {
            imagePromptEvidence.submitted(evidence, response == null ? null : response.getId(), response == null ? "unknown" : response.getStatus());
        } catch (IllegalStateException ex) {
            String prediction = response == null ? null : response.getId();
            log.error("图片任务证据保存失败 attemptId={} predictionId={} promptSha256={}",
                evidence.attemptId(), prediction, evidence.promptSha256());
            throw new IllegalStateException(ex.getMessage() + "；attemptId=" + evidence.attemptId() + "，predictionId=" + prediction, ex);
        }
        return response;
    }

    @Override
    public ShortDramaCharacterAppearanceVo confirmAppearanceImage(Long appearanceId, String predictionId, String model, Long userId) {
        ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(appearanceId);
        if (appearance == null) throw new IllegalArgumentException("形象不存在");
        ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
        if (character == null) throw new IllegalArgumentException("角色不存在");
        validateProjectOwner(character.getProjectId(), userId);

        ChatModelVo modelVo = chatModelService.selectModelByName(model);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + model);

        MediaGenerationResponse result = atlasPredictionService.retrieve(modelVo, predictionId);
        if (!"completed".equals(result.getStatus())) {
            throw new IllegalStateException("图片尚未生成完成，当前状态: " + result.getStatus());
        }
        String imageUrl = result.getUrl();
        if (StrUtil.isBlank(imageUrl)) {
            throw new IllegalStateException("图片生成完成但未获取到URL");
        }

        ShortDramaImagePromptEvidence.Evidence evidence = imagePromptEvidence.forPrediction("appearance", appearanceId, userId, model, predictionId);
        String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
        String finalPrompt = evidence == null ? buildCharacterPrompt(basePrompt, character.getProjectId()) : evidence.finalPrompt();
        boolean revision = evidence != null && evidence.revision();
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        List<String> descs = readJsonStringList(appearance.getImageDescriptions());
        if (urls.contains(imageUrl)) return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
        appearance.setPreviousImageUrls(appearance.getImageUrls());
        appearance.setPreviousDescriptions(appearance.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
        appearance.setImageUrls(JsonUtils.toJsonString(urls));
        appearance.setImageDescriptions(JsonUtils.toJsonString(descs));
        if (!revision) {
            appearance.setReferenceImageUrl(imageUrl);
            appearance.setSelectedImageIndex(urls.size() - 1);
        }
        characterAppearanceMapper.updateById(appearance);

        return MapstructUtils.convert(appearance, ShortDramaCharacterAppearanceVo.class);
    }

    @Override
    public ShortDramaLocationVo confirmLocationImage(Long locationId, String predictionId, String model, Long userId) {
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);

        ChatModelVo modelVo = chatModelService.selectModelByName(model);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + model);

        MediaGenerationResponse result = atlasPredictionService.retrieve(modelVo, predictionId);
        if (!"completed".equals(result.getStatus())) {
            throw new IllegalStateException("图片尚未生成完成，当前状态: " + result.getStatus());
        }
        String imageUrl = result.getUrl();
        if (StrUtil.isBlank(imageUrl)) {
            throw new IllegalStateException("图片生成完成但未获取到URL");
        }

        String prompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
        ShortDramaImagePromptEvidence.Evidence evidence = imagePromptEvidence.forPrediction("location", locationId, userId, model, predictionId);
        String finalPrompt = evidence == null ? buildLocationPrompt(location.getName(), prompt, location.getProjectId()) : evidence.finalPrompt();
        boolean revision = evidence != null && evidence.revision();

        List<String> urls = readJsonStringList(location.getImageUrls());
        List<String> descs = readJsonStringList(location.getImageDescriptions());
        if (urls.contains(imageUrl)) return MapstructUtils.convert(location, ShortDramaLocationVo.class);
        location.setPreviousImageUrls(location.getImageUrls());
        location.setPreviousDescriptions(location.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
        location.setImageUrls(JsonUtils.toJsonString(urls));
        location.setImageDescriptions(JsonUtils.toJsonString(descs));
        if (!revision) {
            location.setReferenceImageUrl(imageUrl);
            location.setSelectedImageIndex(urls.size() - 1);
        }
        locationMapper.updateById(location);

        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
    }

    /** 追加图片 URL 并校验上限 */
    private static void addImageUrl(List<String> urls, String url, List<String> descs, String desc) {
        if (urls.size() >= ShortDramaImageConstants.MAX_IMAGE_VARIANTS) {
            throw new IllegalArgumentException("图片已达上限（最多" + ShortDramaImageConstants.MAX_IMAGE_VARIANTS + "张），请先撤销后再生成");
        }
        urls.add(url);
        descs.add(desc);
    }

    /** 安全地将 JSON 字符串解析为 String 列表，解析失败返回空列表 */
    private static List<String> readJsonStringList(String json) {
        if (StrUtil.isBlank(json)) return new ArrayList<>();
        try {
            List<String> list = JsonUtils.parseArray(json, String.class);
            return list != null ? list : new ArrayList<>();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 场景只使用一个主描述。兼容历史数据：旧记录可能包含三个候选描述，固定取第一个非空项。
     */
    private static String primaryLocationDescription(ShortDramaLocation location) {
        if (location == null || StrUtil.isBlank(location.getDescriptions())) {
            return null;
        }
        return readJsonStringList(location.getDescriptions()).stream()
            .filter(StrUtil::isNotBlank)
            .findFirst()
            .orElse(null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteProject(Long projectId, Long userId) {
        validateProjectOwner(projectId, userId);
        videoComposeService.deleteComposition(projectId);
        storyboardMapper.delete(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId, projectId));
        List<Long> characterIds = characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>()
            .eq(ShortDramaCharacter::getProjectId, projectId)
            .select(ShortDramaCharacter::getId)).stream().map(ShortDramaCharacter::getId).toList();
        if (!characterIds.isEmpty()) {
            characterAppearanceMapper.delete(new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                .in(ShortDramaCharacterAppearance::getCharacterId, characterIds));
        }
        characterMapper.delete(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        locationMapper.delete(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId));
        scriptMapper.delete(new LambdaQueryWrapper<ShortDramaScript>().eq(ShortDramaScript::getProjectId, projectId));
        return projectMapper.deleteById(projectId) > 0;
    }

    // ==================== 通用流式调用辅助 ====================

    /**
     * 使用 StreamingChatModel 调用 LLM，原始输出逐字推送到 SSE，返回完整响应文本。
     * 仅当 emitter 和 model 均非 null 时启用流式；否则退化为同步调用。
     */
    private String streamingChat(StreamingChatModel streamingModel, ChatModel chatModel,
                                 String prompt, SseEmitter emitter, String streamPhase) {
        return streamingChat(streamingModel, chatModel, prompt, emitter, streamPhase, null);
    }

    /**
     * 流式回调只接收新增正文，避免逐 token 重扫完整响应。进度只记录字符数与时间，不暴露思考正文。
     */
    private String streamingChat(StreamingChatModel streamingModel, ChatModel chatModel,
                                 String prompt, SseEmitter emitter, String streamPhase,
                                 java.util.function.Consumer<String> onPartial) {
        return streamingChat(streamingModel, chatModel, prompt, emitter, streamPhase, onPartial, streamPhase);
    }

    private String streamingChat(StreamingChatModel streamingModel, ChatModel chatModel,
                                 String prompt, SseEmitter emitter, String streamPhase,
                                 java.util.function.Consumer<String> onPartial, String callLabel) {
        if (streamingModel == null || emitter == null) {
            return chatModel.chat(prompt);
        }
        var progress = planningProgress.get(emitter);
        String callId = progress == null ? null : progress.startCall(streamPhase, callLabel, prompt.length());
        final long callStartedAt = System.currentTimeMillis();
        final java.util.concurrent.atomic.AtomicLong firstContentAt = new java.util.concurrent.atomic.AtomicLong();
        StringBuilder buf = new StringBuilder();
        CompletableFuture<Void> done = new CompletableFuture<>();
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "short-drama-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleAtFixedRate(() -> {
            if (progress != null) progress.heartbeat();
            else emit(emitter, streamPhase, "running", "模型仍在处理中，请稍候...");
        }, 15, 15, TimeUnit.SECONDS);
        // 首 token 超时兜底:部分聚合站对 stream=true 既不返回内容也不报错,
        // Seed 2.1 Pro 长总纲首正文实测超过 11 分钟，需与 Atlas transport 的期限一致。
        final AtomicBoolean firstTokenReceived = new AtomicBoolean(false);
        final java.util.concurrent.atomic.AtomicLong lastTokenAt = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
        heartbeat.scheduleAtFixedRate(() -> { if (firstTokenReceived.get() && System.currentTimeMillis()-lastTokenAt.get()>90_000) done.completeExceptionally(new RuntimeException("模型输出停滞超过90秒，已完成部分保留")); }, 15, 15, TimeUnit.SECONDS);
        heartbeat.schedule(() -> {
            if (firstTokenReceived.compareAndSet(false, true)) {
                done.completeExceptionally(new RuntimeException(
                    "模型在15分钟内未返回正文，已完成场次保留，请查询原请求后再恢复"));
            }
        }, 15, TimeUnit.MINUTES);
        List<ChatMessage> messages = List.of(UserMessage.from(prompt));
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialThinking(PartialThinking thinking) {
                if (done.isDone() || thinking == null || thinking.text() == null) return;
                lastTokenAt.set(System.currentTimeMillis());
                if (progress != null) progress.activity(callId, thinking.text(), true);
            }
            @Override
            public void onPartialResponse(String text) {
                if (done.isDone() || text == null || text.isEmpty()) return;
                firstContentAt.compareAndSet(0, System.currentTimeMillis());
                if (progress != null) progress.activity(callId, text, false);
                firstTokenReceived.set(true);
                lastTokenAt.set(System.currentTimeMillis());
                buf.append(text);
                // Detail batches run independently; do not interleave their JSON in the UI.
                if (!"storyboard_detail".equals(streamPhase) && !"storyboard_plan_parallel".equals(streamPhase)) emitStream(emitter, streamPhase, text);
                if (onPartial != null) {
                    try { onPartial.accept(text); } catch (Exception ex) {
                        log.warn("流式增量回调异常: {}", ex.getMessage());
                    }
                }
            }
            @Override
            public void onCompleteResponse(ChatResponse response) {
                emitStreamDone(emitter);
                done.complete(null);
            }
            @Override
            public void onError(Throwable error) {
                done.completeExceptionally(error);
            }
        });
        try {
            done.orTimeout(30, TimeUnit.MINUTES).join();
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException(cause instanceof java.util.concurrent.TimeoutException
                ? "模型在30分钟内未完成当前批次，请检查原请求状态后再恢复"
                : "模型调用失败：" + cause.getMessage(), cause);
        } finally {
            heartbeat.shutdownNow();
            if (progress != null) progress.finishCall(callId, !done.isCompletedExceptionally());
            log.info("分镜模型阶段: requestId={}, phase={}, label={}, promptChars={}, outputChars={}, firstContentMs={}, elapsedMs={}, success={}",
                progress == null ? "untracked" : progress.snapshot().requestId(), streamPhase, callLabel, prompt.length(), buf.length(),
                firstContentAt.get() == 0 ? -1 : firstContentAt.get() - callStartedAt, System.currentTimeMillis() - callStartedAt, !done.isCompletedExceptionally());
        }
        return buf.toString();
    }

    // ==================== Phase 1: 固定格式剧本生成（单次流式调用，元信息 JSON + 剧本正文）====================

    private static final String SCRIPT_DELIMITER = "===SCRIPT===";

    /** Read-only snapshots of the actual production templates; no project data or model call. */
    static java.util.Map<String, String> systemPromptCatalog() {
        var result = new java.util.LinkedHashMap<String, String>();
        result.put("剧本写作与打磨", PHASE1_COMBINED_SYSTEM);
        result.put("提取角色档案", buildCharacterProfilePrompt("{{剧本正文与风格基调}}"));
        result.put("提取场景资产", buildLocationCreatePrompt("{{剧本正文与风格基调}}"));
        result.put("提取关键道具", ShortDramaVisualAssetService.SCRIPT_PROP_EXTRACTION_PROMPT);
        result.put("分镜道具关联", ShortDramaVisualAssetService.PROP_EXTRACTION_PROMPT);
        result.put("分镜拆解与节奏规划", buildStoryboardPlanPrompt("{{剧本正文}}", "{{角色库}}", "{{场景库}}", "{{角色关系}}", "{{人物形象}}", "{{场景描述}}", "{{项目风格}}", "{{画幅}}"));
        result.put("摄影与机位规则", buildCinematographerPrompt("{{分镜规划}}", 0, "{{场景描述}}", "{{角色信息}}"));
        result.put("表演与对白指导", buildActingDirectionPrompt("{{分镜规划}}", 0, "{{角色信息}}"));
        result.put("分镜首帧与视频导演稿", buildStoryboardDetailPrompt("{{分镜规划}}", "{{人物年龄与性别}}", "{{场景描述}}", "{{项目风格}}", "{{画幅}}"));
        return java.util.Collections.unmodifiableMap(result);
    }

    private static final String PHASE1_COMBINED_SYSTEM = """
        你是短剧编剧。只完成一件事：把用户输入写成固定格式、可直接审阅的纯文本剧本。

        输出顺序必须为：
        第一行单行 JSON：{"projectName":"项目名","description":"30至80字简介","scriptName":"剧本名","tone":"类型与基调","outlineText":"简明因果大纲"}
        第二行：===SCRIPT===
        之后只写剧本正文。

        剧本正文固定格式（结构以《甲申逆命 E01｜青芜第一炮》为模板，但不得复用其人物、情节或设定）：
        《剧名》第一集：集名

        一　地点或事件。时间。

        用可见、可听的事实建立环境与当下处境。

        写人物动作、事件变化和场间承接。

        角色名（可选状态或声源）：台词

        二　地点或事件。时间。
        ……

        第一集完。

        格式约束：
        1. 正文禁止 Markdown，不得出现标题井号、星号加粗、引用符、列表符或代码围栏。
        2. 每场必须以“中文场次序号＋全角空格＋地点或事件＋时间”开头，例如“一　黑屏。冬。”或“二　县衙二堂。接上。”。
        3. JSON的tone按当前故事自动概括风格与基调，包含叙事类型、情绪、节奏及适合角色和场景的视觉方向；以用户明确要求为准，不固定套用其他作品风格。正文只写场景、行动、声音和对白；不写镜号、机位、运镜、逐秒安排、画面比例、生成规则或创作说明。
        4. 场次标题后直接写自然的动作与环境段落，不加“场景”“行动”“说明”等模板标签。
        5. 保留用户给出的核心人物、冲突、关键转折和结局。每场都要改变剧情状态，场与场之间必须有明确因果承接。
        6. 对白符合人物身份和语境，删掉解释剧情的套话、总结句和机械问答。
        7. 若用户提供“当前剧本”和“修改意见”，以当前剧本为底稿，只按意见重写；未提及的人物、事实、因果和结局保持一致。
        8. 军事内容只写非操作性叙事，不输出武器制造参数或使用教程。
        9. 内容规模服从用户的原事件：测试片段、一个简单动作或几句话的场面不扩成多场完整故事，不增加挑战、支线、往返、失败重试或解释对白。未给时长时按原事件自然长度写；有明确短片时长要求时先估算对白与独占动作容量，再写能容纳的剧本，不把长故事写完后交给分镜硬压。

        除规定的 JSON、分隔符和剧本正文外，不输出任何说明。
        """;

    /**
     * 同步版本：ChatModel 单次调用，解析 "JSON\\n===SCRIPT===\\n剧本" 格式
     */
    private static String sanitizeIdeaInput(String idea) {
        if (idea == null) return "";
        String cleaned = idea
            .replaceAll("(?im)^\\s*(喜欢|不喜欢)\\s*$", "")
            .replaceAll("\\[([^\\]]+)]\\(https?://[^)]+\\)", "$1")
            .replaceAll("https?://\\S+", "")
            .replaceAll("(?im)^\\s*(ent\\.sina\\.cn|k\\.sina\\.cn|来源[:：]?|\\[?\\+\\d+]?)[^\\n]*$", "")
            .replaceAll("(?m)^[ \t]+$", "")
            .replaceAll("\n{3,}", "\n\n")
            .trim();
        return cleaned.length() > 12000 ? cleaned.substring(0, 12000) : cleaned;
    }

    private static int countSceneHeadings(String scriptText) {
        if (StrUtil.isBlank(scriptText)) return 0;
        int count = 0;
        for (String line : scriptText.split("\\R")) {
            String normalized = line.trim();
            if (normalized.matches("^[一二三四五六七八九十百零〇]+　\\S+.*")) count++;
        }
        return count;
    }

    static String normalizePlainScriptText(String source) {
        if (source == null) return "";
        String normalized = source.replace("\r\n", "\n").replace('\r', '\n')
            .replaceAll("(?m)^\\s*```[^\\n]*$", "")
            .replaceAll("(?m)^\\s{0,3}#{1,6}\\s*", "")
            .replaceAll("(?m)^\\s{0,3}>\\s?", "")
            .replaceAll("(?m)^\\s*[-+*]\\s+(?=\\S)", "")
            .replaceAll("\\*\\*([^*\\n]+)\\*\\*", "$1")
            .replaceAll("__([^_\\n]+)__", "$1")
            .replaceAll("(?m)[ \\t]+$", "")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
        return normalized;
    }

    private static String phase1UserPrompt(ShortDramaIdeaBo bo, String revisionInstruction) {
        String projectName = firstNotBlank(bo.getProjectName(), "短剧项目");
        String revision = StrUtil.trim(revisionInstruction);
        if (StrUtil.isNotBlank(revision)) {
            return "用户期望项目名：" + projectName
                + "\n\n当前剧本：\n" + sanitizeIdeaInput(bo.getIdea())
                + "\n\n修改意见：\n" + revision;
        }
        return "用户期望项目名：" + projectName + "\n\n故事想法：\n" + sanitizeIdeaInput(bo.getIdea());
    }

    private static ShortDramaScriptResult requireFixedScript(ShortDramaScriptResult result) {
        if (result == null || StrUtil.isBlank(result.getScriptText())) {
            throw new RuntimeException("剧本生成失败：模型返回格式异常，请重试");
        }
        result.setScriptText(normalizePlainScriptText(result.getScriptText()));
        if (countSceneHeadings(result.getScriptText()) == 0) {
            throw new RuntimeException("剧本生成失败：正文未按固定场次格式输出，请重试");
        }
        return result;
    }

    private ShortDramaScriptResult executePhase1_ScriptPolish(ChatModel chatModel, ShortDramaIdeaBo bo,
                                                               String revisionInstruction,
                                                               Consumer<String> onPhase) {
        String prompt = PHASE1_COMBINED_SYSTEM + "\n\n" + phase1UserPrompt(bo, revisionInstruction);

        long t0 = System.currentTimeMillis();
        String resp = chatModel.chat(prompt);
        log.info("Phase 1 剧本生成: elapsed={}ms len={}", System.currentTimeMillis() - t0,
            resp != null ? resp.length() : 0);

        ShortDramaScriptResult result = requireFixedScript(parsePhase1Response(resp));
        if (onPhase != null) onPhase.accept("outline_done");
        return result;
    }

    private ShortDramaScriptResult executePhase1_ScriptPolish(ChatModel chatModel, ShortDramaIdeaBo bo) {
        return executePhase1_ScriptPolish(chatModel, bo, null, null);
    }

    /**
     * 流式版本：StreamingChatModel 单次调用，边收边解析，JSON 完成后立即推流剧本正文
     */
    private ShortDramaScriptResult executePhase1_Streaming(StreamingChatModel streamingModel,
                                                            ShortDramaIdeaBo bo, SseEmitter emitter, ShortDramaScriptProgress progress) {
        String systemPrompt = PHASE1_COMBINED_SYSTEM;
        String userPrompt = phase1UserPrompt(bo, null);
        List<ChatMessage> messages = List.of(
            SystemMessage.from(systemPrompt), UserMessage.from(userPrompt));

        StringBuilder buf = new StringBuilder();
        ShortDramaScriptResult[] result = {null};
        boolean[] scriptStreaming = {false};
        int[] streamedScriptChars = {0};
        boolean[] thinkingStarted = {false};
        boolean[] contentStarted = {false};
        CompletableFuture<Void> streamDone = new CompletableFuture<>();

        long t0 = System.currentTimeMillis();
        progress.start(systemPrompt.length() + userPrompt.length());
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialThinking(PartialThinking thinking) {
                if (thinking == null || StrUtil.isEmpty(thinking.text())) return;
                if (!thinkingStarted[0]) {
                    thinkingStarted[0] = true;
                    emit(emitter, "polish", "running", "模型正在处理剧情，收到正文后实时显示...");
                    log.info("Phase 1 首次思考输出: elapsed={}ms", System.currentTimeMillis() - t0);
                }
                progress.thinking(thinking.text().length());
            }

            @Override
            public void onPartialResponse(String text) {
                if (StrUtil.isEmpty(text)) return;
                if (!contentStarted[0]) {
                    contentStarted[0] = true;
                    emit(emitter, "outline", "running", "正在整理故事信息，随后输出剧本正文...");
                }
                progress.content(text.length());
                buf.append(text);
                if (!scriptStreaming[0]) {
                    // 检查 JSON 元信息是否已完整（由 ===SCRIPT=== 分隔符标记）
                    int delim = buf.indexOf(SCRIPT_DELIMITER);
                    if (delim >= 0) {
                        String jsonPart = buf.substring(0, delim).trim();
                        String json = extractJson(jsonPart);
                        if (json != null) {
                            result[0] = parseJson(json, ShortDramaScriptResult.class);
                        }
                        scriptStreaming[0] = true;
                        log.info("Phase 1 流式 JSON 解析完成: elapsed={}ms ok={}",
                            System.currentTimeMillis() - t0, result[0] != null);
                        emit(emitter, "outline", "done", "剧本信息已生成，正在输出正文...");
                        // 分隔符后面的已有文字作为剧本开头推送
                        String tail = buf.substring(delim + SCRIPT_DELIMITER.length());
                        streamedScriptChars[0] = tail.length();
                        if (StrUtil.isNotBlank(tail.trim())) {
                            progress.script(tail.length());
                            emitStream(emitter, "script", tail);
                        }
                    }
                } else {
                    streamedScriptChars[0] += text.length();
                    progress.script(text.length());
                    emitStream(emitter, "script", text);
                }
            }
            @Override
            public void onCompleteResponse(ChatResponse response) {
                // 某些兼容供应商只回调完整响应，或仅在完整响应中补上最后一段。
                String completeText = response != null && response.aiMessage() != null ? response.aiMessage().text() : null;
                if (StrUtil.isNotBlank(completeText) && completeText.startsWith(buf.toString())) {
                    if (completeText.length() > buf.length()) progress.content(completeText.length() - buf.length());
                    buf.setLength(0);
                    buf.append(completeText);
                }
                // 兜底：如果没找到分隔符，尝试从完整响应提取 JSON
                if (!scriptStreaming[0] || result[0] == null) {
                    result[0] = parsePhase1Response(buf.toString());
                }
                // 兜底：如果分隔符后有文字但没通过 onPartial 推完，补推
                if (result[0] != null) {
                    String fullText = buf.toString();
                    int delim = fullText.indexOf(SCRIPT_DELIMITER);
                    String scriptText = delim >= 0
                        ? fullText.substring(delim + SCRIPT_DELIMITER.length())
                        : extractScriptAfterJson(fullText);
                    if (StrUtil.isBlank(scriptText)) scriptText = result[0].getScriptText();
                    if (StrUtil.isBlank(result[0].getScriptText()) && StrUtil.isNotBlank(scriptText)) {
                        result[0].setScriptText(StringUtils.strip(scriptText));
                    }
                    if (scriptText != null && scriptText.length() > streamedScriptChars[0]) {
                        String suffix = scriptText.substring(streamedScriptChars[0]);
                        progress.script(suffix.length());
                        emitStream(emitter, "script", suffix);
                    }
                }
                emitStreamDone(emitter);
                log.info("Phase 1 流式完成: totalElapsed={}ms scriptLen={}",
                    System.currentTimeMillis() - t0,
                    result[0] != null && result[0].getScriptText() != null ? result[0].getScriptText().length() : 0);
                streamDone.complete(null);
            }
            @Override
            public void onError(Throwable error) {
                log.error("Phase 1 流式输出错误", error);
                streamDone.completeExceptionally(error);
            }
        });

        try {
            streamDone.join();
        } catch (Exception e) {
            throw new RuntimeException("剧本生成失败：" + e.getMessage(), e);
        }
        return requireFixedScript(result[0]);
    }

    private ShortDramaScriptResult parsePhase1Response(String resp) {
        if (StrUtil.isBlank(resp)) return null;
        // 尝试分隔符切分
        int delim = resp.indexOf(SCRIPT_DELIMITER);
        String jsonPart, scriptPart;
        if (delim >= 0) {
            jsonPart = resp.substring(0, delim);
            scriptPart = resp.substring(delim + SCRIPT_DELIMITER.length());
        } else {
            jsonPart = resp;
            scriptPart = extractScriptAfterJson(resp);
        }
        String json = extractJson(jsonPart);
        if (json == null) return null;
        ShortDramaScriptResult result = parseJson(json, ShortDramaScriptResult.class);
        if (result != null && StrUtil.isNotBlank(scriptPart)) {
            result.setScriptText(normalizePlainScriptText(StringUtils.strip(scriptPart)));
        }
        return result;
    }

    /** 兜底：JSON 后面的非 JSON 文本当作剧本 */
    private static String extractScriptAfterJson(String text) {
        int end = text.lastIndexOf('}');
        if (end >= 0 && end < text.length() - 1) {
            String after = text.substring(end + 1).trim();
            return after.replaceFirst("^[\\s\\-\\n\\r=]+", "");
        }
        return "";
    }

    // ==================== Phase 2: 资产分析（并发）====================

    private void executePhase2_AssetAnalysis(ChatModel chatModel, Long projectId, ShortDramaScript script) {
        executePhase2_AssetAnalysis(chatModel, null, projectId, script, null);
    }

    private void executePhase2_AssetAnalysis(ChatModel chatModel, StreamingChatModel streamModel,
                                             Long projectId, ShortDramaScript script, SseEmitter emitter) {
        executePhase2_AssetAnalysis(chatModel, streamModel, projectId, script, emitter, false);
    }

    private void executePhase2_AssetAnalysis(ChatModel chatModel, StreamingChatModel streamModel,
                                             Long projectId, ShortDramaScript script, SseEmitter emitter, boolean replaceAssets) {
        String source = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        if (StrUtil.isBlank(source)) throw new IllegalArgumentException("资产分析需要已保存的有效剧本，世界观不能替代正文");
        String scriptText = source + ShortDramaScriptPreparation.worldContext(script);
        var frozenAssetSkills = skillCatalog.snapshot(projectMapper.selectById(projectId));
        String assetDirection = frozenAssetSkills.direction();

        try {
            List<AssetGeneratedCharacter> characters;
            List<AssetGeneratedLocation> locations;

            if (emitter != null && streamModel != null) {
                // 流式模式：串行执行，用户能看到每个阶段的原始输出
                emit(emitter, "assets_chars", "running", "正在分析角色档案...");
                String charsResp = streamingChat(streamModel, chatModel, buildCharacterProfilePrompt(scriptText) + assetDirection, emitter, "assets");
                characters = parseCharacterList(charsResp);
                requireAssetCharacters(characters);
                emit(emitter, "assets_chars", "done", "角色分析完成，提取 " + characters.size() + " 个角色");

                emit(emitter, "assets_locs", "running", "正在分析场景站位...");
                StreamingChatModel locsStreamModel = buildStreamingChatModel();
                String locsResp = streamingChat(locsStreamModel, chatModel, buildLocationCreatePrompt(scriptText) + assetDirection, emitter, "assets");
                locations = parseLocationList(locsResp);
                requireAssetLocations(locations);
                emit(emitter, "assets_locs", "done", "场景分析完成，提取 " + locations.size() + " 个场景");
            } else {
                // 非流式：保持原有并发模式，速度更快
                CompletableFuture<List<AssetGeneratedCharacter>> charsFuture =
                    CompletableFuture.supplyAsync(() -> {
                        String resp = chatModel.chat(buildCharacterProfilePrompt(scriptText) + assetDirection);
                        return parseCharacterList(resp);
                    });
                CompletableFuture<List<AssetGeneratedLocation>> locsFuture =
                    CompletableFuture.supplyAsync(() -> {
                        String resp = chatModel.chat(buildLocationCreatePrompt(scriptText) + assetDirection);
                        return parseLocationList(resp);
                    });
                characters = charsFuture.join();
                locations = locsFuture.join();
            }

            requireAssetCharacters(characters);
            requireAssetLocations(locations);
            List<AssetGeneratedCharacter> checkedCharacters = deduplicateCharacters(characters);
            List<AssetGeneratedLocation> checkedLocations = deduplicateLocations(locations);

            // Worker tasks only prepare data. A failed sibling may still finish its
            // provider request, but it cannot write assets after this operation fails.
            List<CompletableFuture<String>> visualFutures = new ArrayList<>();
            for (AssetGeneratedCharacter c : checkedCharacters) {
                visualFutures.add(CompletableFuture.supplyAsync(() -> {
                    String visualDesc = "";
                    try {
                        String vr = chatModel.chat(buildCharacterVisualPrompt(c, script.getTone()) + assetDirection);
                        AssetCharacterVisualResult visResult = parseJson(extractJson(vr), AssetCharacterVisualResult.class);
                        if (visResult != null && visResult.getCharacters() != null) {
                            for (AssetCharVisual cv : visResult.getCharacters()) {
                                if (cv.getAppearances() != null) {
                                    for (AssetAppearanceDesc ad : cv.getAppearances()) {
                                        if (ad.getDescriptions() != null && !ad.getDescriptions().isEmpty()) {
                                            visualDesc = ad.getDescriptions().get(0);
                                            break;
                                        }
                                    }
                                }
                                if (StrUtil.isNotBlank(visualDesc)) break;
                            }
                        }
                    } catch (Exception e) {
                        throw new IllegalStateException("角色「" + c.getName() + "」视觉描述生成失败", e);
                    }
                    String description = firstNotBlank(visualDesc, c.getVisualKeywords(), "");
                    if (StrUtil.isBlank(description)) throw new IllegalStateException("角色「" + c.getName() + "」没有有效视觉描述");
                    return description;
                }));
            }
            CompletableFuture.allOf(visualFutures.toArray(new CompletableFuture[0])).join();
            List<String> visualDescriptions = visualFutures.stream().map(CompletableFuture::join).toList();

            // Preserve approved assets until both extraction branches and every visual
            // description succeed. The replacement and all inserts share one transaction.
            transactionTemplate.executeWithoutResult(status -> {
              skillCatalog.verify(frozenAssetSkills, projectMapper.selectById(projectId));
              if (replaceAssets) clearAssets(projectId);
              for (int index = 0; index < checkedCharacters.size(); index++) {
                insertCharacter(projectId, checkedCharacters.get(index), visualDescriptions.get(index));
              }
              for (AssetGeneratedLocation loc : checkedLocations) {
                ShortDramaLocation entity = new ShortDramaLocation();
                entity.setId(IdUtil.getSnowflakeNextId());
                entity.setProjectId(projectId);
                entity.setName(firstNotBlank(loc.getName(), "未命名场景"));
                entity.setSummary(loc.getSummary());
                entity.setHasCrowd(loc.getHasCrowd() != null ? loc.getHasCrowd() : false);
                entity.setCrowdDescription(loc.getCrowdDescription());
                entity.setAvailableSlots(loc.getAvailableSlots() != null ? JsonUtils.toJsonString(loc.getAvailableSlots()) : null);
                entity.setDescriptions(loc.getDescriptions() != null ? JsonUtils.toJsonString(loc.getDescriptions()) : null);
                locationMapper.insert(entity);
              }
            });
        } catch (Exception e) {
            Throwable cause = e;
            while (cause instanceof java.util.concurrent.CompletionException && cause.getCause() != null) cause = cause.getCause();
            log.warn("Phase 2 资产分析失败，项目{}原有资产保留: {}", projectId, cause.getMessage(), cause);
            throw new IllegalStateException("资产分析未完成：" + firstNotBlank(cause.getMessage(), cause.getClass().getSimpleName())
                + "。已保存剧本与原有资产保留，请核对请求结果后手动继续", cause);
        }
    }

    private static void requireAssetCharacters(List<AssetGeneratedCharacter> characters) {
        if (characters == null || characters.isEmpty()) throw new IllegalStateException("模型未返回可解析的角色档案");
        if (characters.stream().anyMatch(character -> character == null || StrUtil.isBlank(character.getName()))) {
            throw new IllegalStateException("角色档案缺少有效角色名称");
        }
    }

    private static void requireAssetLocations(List<AssetGeneratedLocation> locations) {
        if (locations == null || locations.isEmpty()) throw new IllegalStateException("模型未返回可解析的场景档案");
        if (locations.stream().anyMatch(location -> location == null || StrUtil.isBlank(location.getName()))) {
            throw new IllegalStateException("场景档案缺少有效场景名称");
        }
    }

    private List<AssetGeneratedCharacter> deduplicateCharacters(List<AssetGeneratedCharacter> characters) {
        LinkedHashMap<String, AssetGeneratedCharacter> unique = new LinkedHashMap<>();
        for (AssetGeneratedCharacter character : characters) {
            String name = firstNotBlank(character.getName(), "未命名").trim();
            unique.putIfAbsent(name.toLowerCase(java.util.Locale.ROOT), character);
        }
        return new ArrayList<>(unique.values());
    }

    private List<AssetGeneratedLocation> deduplicateLocations(List<AssetGeneratedLocation> locations) {
        LinkedHashMap<String, AssetGeneratedLocation> unique = new LinkedHashMap<>();
        for (AssetGeneratedLocation location : locations) {
            String name = firstNotBlank(location.getName(), "未命名场景").trim();
            unique.putIfAbsent(name.toLowerCase(java.util.Locale.ROOT), location);
        }
        return new ArrayList<>(unique.values());
    }

    private void insertCharacter(Long projectId, AssetGeneratedCharacter c, String visualDesc) {
        ShortDramaCharacter entity = new ShortDramaCharacter();
        entity.setId(IdUtil.getSnowflakeNextId());
        entity.setProjectId(projectId);
        entity.setName(firstNotBlank(c.getName(), "未命名"));
        entity.setAliases(c.getAliases());
        entity.setIntroduction(c.getIntroduction());
        entity.setRoleLevel(firstNotBlank(c.getRoleLevel(), "B"));
        entity.setGender(c.getGender());
        entity.setAgeRange(c.getAgeRange());
        entity.setPersonalityTags(c.getPersonalityTags());
        entity.setCostumeTier(c.getCostumeTier() != null ? c.getCostumeTier() : 2);
        entity.setVisualDescription(firstNotBlank(visualDesc, c.getVisualKeywords()));
        characterMapper.insert(entity);

        ShortDramaCharacterAppearance appearance = new ShortDramaCharacterAppearance();
        appearance.setId(IdUtil.getSnowflakeNextId());
        appearance.setCharacterId(entity.getId());
        appearance.setAppearanceIndex(0);
        appearance.setChangeReason("初始形象");
        appearance.setDescription(entity.getVisualDescription());
        characterAppearanceMapper.insert(appearance);
    }

    private static String buildCharacterProfilePrompt(String scriptText) {
        return ShortDramaDirectorSkills.load("emotional-dialogue", "story-causality", "visual-world", "character-art-direction")
            + ShortDramaAssetAesthetics.characterReference() + """
            你是专业的"选角指导"。请基于提供的文本（小说、剧本或混合格式），分析并输出所有需要制作形象的角色档案信息。

            【你的职责】
            - 识别需要在画面中出现的角色
            - 根据剧情发展和角色身份判断每个角色的重要性层级
            - 分析角色的性格和背景
            - 输出结构化的角色档案（供后续视觉生成使用）
            - 分析角色之间的关系、称呼映射，生成角色介绍（introduction）

            【筛选规则】
            ✅ 必须提取：剧本人物行中列出的角色、有台词且参与剧情互动的角色、贯穿故事主线的核心人物、对剧情有实际推动作用的配角、在画面中需要出镜的角色
            ❌ 不提取：无名无特征的纯路人、仅被提及但从未出场的角色、没有台词也没有互动的背景人物

            【角色介绍 introduction 规则】
            每个角色必须有 introduction 字段，包含：
            1. 叙述视角映射：如果是第一人称叙述，明确说明"我"对应此角色
            2. 角色身份定位：描述角色在故事中的身份（主角/配角/反派等）
            3. 角色关系：与其他主要角色的关系
            4. 称呼映射：其他角色对此角色的常用称呼

            示例："故事主角，小说以第一人称「我」叙述，真名林墨。苏晴的丈夫，张三的女婿。被苏晴称呼为「老公」、「墨哥」，被下属称呼为「林总」。"

            【角色重要性层级判断规则】
            S级（绝对主角）：故事的核心视角人物，剧情围绕其展开。第一人称叙述中的"我"通常是S级
            A级（核心配角）：与主角有大量互动的重要角色，男二号/女二号/主要反派等。对主线剧情有重大影响
            B级（重要配角）：多次出场、有名有姓、推动某条支线剧情
            C级（次要角色）：偶尔出场、戏份较少但有具体形象
            D级（群众演员）：有短暂出镜需求的小角色

            【服装华丽度 costume_tier】
            5级（皇室/顶奢级）：皇室成员、顶级富豪，服装极致华丽，有精美的刺绣、镶嵌或定制剪裁
            4级（贵族/精英级）：贵族、企业家，服装精致考究，使用高档面料和精致细节
            3级（专业/品质级）：中产阶级、专业人士，服装得体有品，剪裁讲究
            2级（日常/普通级）：普通人、学生，服装简洁日常，款式普通但整洁
            1级（朴素/功能级）：平民、劳动者，服装以功能为主，剪裁、材料与修补依个人身份和工作区分

            【性格标签 personality_tags】
            气质类：高冷、温柔、阳光、忧郁、神秘、妩媚、清冷、热情
            性格类：腹黑、傲娇、毒舌、话痨、闷骚、直爽、圆滑、固执
            态度类：自信、自卑、孤僻、合群、叛逆、顺从
            用逗号分隔，至少2个，最多5个

            【视觉关键词 visual_keywords】
            写可落地的选角和妆造特征：年龄感、面部轮廓、眉眼组合、修整程度、服装材料、劳动痕迹。
            不用「禁欲系」「奶狗系」等标签替代具体人物细节；不按主配角重要性统一美化脸部。
            用逗号分隔

            【输出格式】
            只返回JSON，禁止markdown标记或注释：
            {
              "new_characters": [
                {
                  "name": "角色名",
                  "aliases": "别名1,别名2",
                  "introduction": "角色介绍（身份、关系、称呼映射）",
                  "roleLevel": "S/A/B/C/D",
                  "gender": "男/女",
                  "ageRange": "约二十五岁",
                  "archetype": "角色在剧情中的身份与职责",
                  "personalityTags": "谨慎,务实",
                  "costumeTier": 3,
                  "visualKeywords": "窄长脸,直眉,素面棉布衣",
                  "suggestedColors": "深蓝,金色",
                  "primaryIdentifier": "剧本明确的固定辨识点；无则填写无，不凭空加痣或疤"
                }
              ]
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。

            剧本内容：
            %s
            """.formatted(scriptText);
    }

    private static String buildCharacterVisualPrompt(AssetGeneratedCharacter character, String tone) {
        String levelDesc = switch (firstNotBlank(character.getRoleLevel(), "B")) {
            case "S" -> "180-220字，有清晰选角辨识度；脸部与体态贴合年龄、身份和经历，不要求五官精致或偶像化";
            case "A" -> "150-180字，有可见个人特征，与其他人物的轮廓和妆造可区分，不统一美化";
            case "B" -> "120-150字，有基本的辨识特征";
            case "C" -> "80-120字，简洁但完整的形象描述";
            default -> "50-80字，基础形象即可";
        };
        return ShortDramaDirectorSkills.load("visual-world", "character-art-direction")
            + ShortDramaAssetAesthetics.characterReference() + """
            你是专业的"角色视觉设计师"。根据角色档案信息，生成详细的人物外貌描述（用于AI图片生成）。

            【视觉层级规范】描述长度要求：%s

            【描述规范 - 必须按优先级包含以下内容】

            🎭 面部特征（最重要！必须详细）：
            - 脸型：瓜子脸、鹅蛋脸、方脸、长脸等具体脸型
            - 五官组合：眼睛、鼻子、嘴巴、眉毛的形状和特点
            - 眼睛：双眼皮/单眼皮、眼型、大小
            - 鼻子：高挺、小巧、笔直、精致等
            - 嘴唇：薄厚、形状（小巧、丰润）
            - 眉毛：浓淡、形状（剑眉、柳叶眉）
            - 独特记号：只写剧本或已确认设定中的痣、伤侧、衣物辨识点；没有就不新增
            - 年龄与肤质：符合年龄的自然状态，细节服从视觉风格，不统一精修或夸张衰老

            💇 发型描写（必须详细）：
            - 发色：乌黑、深棕、栗色、金棕等
            - 发长：齐耳短发、及肩、过肩、及腰
            - 发型：自然披散、高马尾、低马尾、丸子头、盘发、寸头、中分、偏分、背头
            - 发质：柔顺、自然卷、微卷、蓬松、服帖
            - 刘海：齐刘海、空气刘海、无刘海、中分刘海、侧分刘海、碎发刘海

            👤 体态：身形（修长、健硕、纤细、匀称）、身高感（高挑、娇小、适中）

            👔 服装配饰：上衣、下装、鞋子（必填！）、配饰；补充剪裁、材料厚薄、接缝与有依据的固定磨损位置

            【身份卡边界】允许符合年龄和身份的肤质、自然肤色及稳定的眉眼肩颈习惯。表情和性格须转为可见的轻微状态，不固定哭泣或愤怒等具体场次表演。禁止背景、剧情动作、抽象气质和「可能」「或」等不确定描述。

            【服装华丽度对照】
            5级：刺绣、镶嵌、定制剪裁、稀有面料
            4级：高档面料、精致细节、品质配饰
            3级：得体剪裁、有设计感
            2级：简洁日常款式
            1级：基础款式、功能性为主

            【输出格式】只返回JSON：
            {
              "characters": [
                {
                  "name": "角色名",
                  "appearances": [
                    {
                      "id": 0,
                      "descriptions": ["完整外貌描述"],
                      "change_reason": "初始形象"
                    }
                  ]
                }
              ]
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。

            角色名：%s
            性别：%s
            年龄段：%s
            性格标签：%s
            服装华丽度：%s级
            视觉关键词：%s
            风格基调：%s
            辨识标志：%s
            """.formatted(levelDesc,
            character.getName(),
            firstNotBlank(character.getGender(), "未指定"),
            firstNotBlank(character.getAgeRange(), "未指定"),
            firstNotBlank(character.getPersonalityTags(), ""),
            character.getCostumeTier() != null ? character.getCostumeTier() : 2,
            firstNotBlank(character.getVisualKeywords(), ""),
            firstNotBlank(tone, "短剧"),
            firstNotBlank(character.getPrimaryIdentifier(), "无"));
    }

    private static String buildLocationCreatePrompt(String scriptText) {
        return ShortDramaDirectorSkills.load("visual-world") + ShortDramaAssetAesthetics.locationReference() + """
            你是"场景资产建立师"。请基于文本筛选需要制作画面的场景，生成用于出图的资产JSON。

            【筛选规则】
            ✅ 必须提取：剧本场景头部中出现的地点、角色实际身处产生互动的场所、剧情主线发生的核心地点、多次出现或戏份较重的场景、有明确空间描写需要制作背景画面的地点
            ❌ 不提取：一次性路过仅提及但无剧情发生的地点、意境类比喻类修辞类描述、抽象空间或无法具象化的概念、纯过渡性场景

            【场景生成要求 - 全景空间版】
            核心要求：生成宽广的空间全景，展示场景的完整面貌，而非局部特写！
            - 镜头应该使用广角/远景视角，能看到整个空间的全貌
            - 展示空间的完整边界（墙壁、地面、天花板/天空）
            - 严格按照原文的场景描述来描写，原文描述的场景是最优先级

            每条描述必须包含：
            1. 开头以「场景名」标注空间属性
            2. 宽广空间感（最重要）：室内能看到2-3面墙壁、地板、部分天花板；室外能看到开阔视野、远处地平线
            3. 空间定位与规模、空间层次（前景/中景/背景）、物体布局（使用位置词：左侧/右侧/中央/角落/靠窗/远处）
            4. 光线方向：光从哪个方向照入
            5. 可落位空间：必须说明哪些区域留有可供人物站立的空白空间，至少2-3个后续可作为人物落位锚点的关键物体或区域

            每个场景只生成1条中文环境描述（100-150字），2-6个available_slots。描述可由用户编辑，不要提供相似候选方案。

            ⚠️ 场景图禁止出现任何有名有姓的角色！场景图是纯粹的背景板。无名的模糊背景群众（如"宾客""路人"）可以出现。

            available_slots每条必须是完整的位置描述短语，如「皇宫正中龙椅前方台阶下的位置」「教室后排靠窗那组课桌外侧的位置」

            命名规则："地点_时间/状态"如"客厅_白天"

            【输出格式】只返回JSON：
            {
              "locations": [
                {
                  "name": "场景_时间",
                  "summary": "场景简要说明",
                  "hasCrowd": true/false,
                  "crowdDescription": "人群类型描述",
                  "availableSlots": ["位置1完整描述", "位置2完整描述"],
                  "descriptions": ["「场景名」唯一完整描述"]
                }
              ]
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。

            剧本内容：
            %s
            """.formatted(scriptText);
    }

    // ==================== Phase 3: 分镜规划 ====================

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, ShortDramaScript script, Long projectId) {
        return executePhase3_StoryboardPlan(chatModel, null, script, projectId, null);
    }

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, ShortDramaScript script, Long projectId, SseEmitter emitter) {
        return executePhase3_StoryboardPlan(chatModel, null, script, projectId, emitter);
    }

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, StreamingChatModel streamingModel,
        ShortDramaScript script, Long projectId, SseEmitter emitter) {
        return executePhase3_StoryboardPlan(chatModel, streamingModel, script, projectId, emitter, findChatModel().getModelName());
    }

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, StreamingChatModel streamingModel,
        ShortDramaScript script, Long projectId, SseEmitter emitter, String modelName) {
        return executePhase3_StoryboardPlan(chatModel, streamingModel, script, projectId, emitter, modelName, 1);
    }

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, StreamingChatModel streamingModel,
        ShortDramaScript script, Long projectId, SseEmitter emitter, String modelName, int minimum) {
        return executePhase3_StoryboardPlan(chatModel, streamingModel, script, projectId, emitter, modelName, minimum, java.util.UUID.randomUUID().toString());
    }

    private List<StoryboardPanelData> executePhase3_StoryboardPlan(ChatModel chatModel, StreamingChatModel streamingModel,
        ShortDramaScript script, Long projectId, SseEmitter emitter, String modelName, int minimum, String requestId) {
        String text=firstNotBlank(script.getScriptText(),script.getOutlineText(), "");
        if(StrUtil.isBlank(text))return List.of();
        String locsLib=buildLocationsLibString(projectId);
        String style=artStyleSuffix(projectId),aspect=projectAspectRatio(projectId);
        var assets = planningAssets(projectId);
        String checkpoint = planningCheckpointKey(script, projectId, modelName, minimum);
        List<String> scenes=splitScriptScenes(text);
        var progress = emitter == null ? null : planningProgress.get(emitter);
        var completed=new java.util.concurrent.atomic.AtomicInteger();
        List<Integer> indices=java.util.stream.IntStream.range(0,scenes.size()).boxed().toList();
        String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
        var byScene=ShortDramaParallel.mapOrdered(indices,3,sceneIndex->org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant,()->{
            String context=ShortDramaScriptPreparation.worldContext(script)+"\n全剧大纲（只作因果上下文）：\n"+firstNotBlank(script.getOutlineText(),text)
                +"\n当前第"+(sceneIndex+1)+"/"+scenes.size()+"场，只拆本场：\n"+scenes.get(sceneIndex);
            if(sceneIndex>0) {
                String previous=scenes.get(sceneIndex-1);
                context+="\n上一场原文结尾（只作桥梁参考，不重复生成）："+previous.substring(Math.max(0,previous.length()-1000));
            }
            String sceneScript=scenes.get(sceneIndex);
            var scoped = sceneCharacterLibraries(assets, sceneScript, previousSceneBridge(scenes, sceneIndex));
            String prompt=buildStoryboardPlanPrompt(context,scoped.get("names"),locsLib,scoped.get("introductions"),scoped.get("appearances"),scoped.get("descriptions"),style,aspect);
            prompt+="\n本次只拆以下场次，时长预算以此标题为准："+sceneScript.lines().findFirst().orElse("")
                +"\n本场出镜和发声只以本场原文及明确的上一场承接为据。世界观、大纲、全剧角色库只校对身份与材料，不能据此添加本场未安排的演员、CG或精灵。"
                +"\n在同一次规划中按原文有效表演时间组织同空间连续动作与对白，再安排合适的生成段数。用户没有指定时间时按原事件实际容量估算，不按题材、句数或预设片长扩充。不追求固定镜数，也不新增对白、阻碍、反应轮次或过场；描述画面细节不增加事件时长。时长、节奏与承接检查仅作审阅建议，不是必须满足的数值或逐字相等限制。"+ShortDramaSceneBudget.scaffold(sceneScript, minimum);
            final String initialPrompt = prompt;
            var identity = sceneIdentity(projectId, script, scenes, sceneIndex, style, aspect, modelName, minimum);
            String key=checkpoint+":scene:"+(sceneIndex+1);
            var proof = sceneCheckpoints.compatible(identity, assets, true);
            String cached = proof == null ? RedisUtils.getCacheObject(key) : null;
            List<StoryboardPanelData> result=null;
            boolean recoveredDraft = false;
            if(proof != null || StrUtil.isNotBlank(cached)) {
                result = proof == null ? parsePanelList(cached) : proof.panels();
                if (result != null && !result.isEmpty()) try { assets.referenced(result); assets.validateSceneCast(sceneScript, previousSceneBridge(scenes, sceneIndex), result); ShortDramaShotDesign.validate(result); validateScenePlan(sceneScript,result,minimum); }
                catch (RuntimeException invalid) {
                    if (!(invalid instanceof ShortDramaSceneCheckpoint.BindingMismatch)) sceneCheckpoints.save(identity, assets, result, false, "validation_failed");
                    result=null;
                }
            }
            if (result != null && !result.isEmpty() && emitter != null)
                emit(emitter,"storyboard_plan","running","第"+(sceneIndex+1)+"场已恢复已校验规划（未请求模型）");
            if(result==null || result.isEmpty()) {
                var draftProof = sceneCheckpoints.compatible(identity, assets, false);
                String draft = draftProof == null ? RedisUtils.getCacheObject(key+":draft") : null;
                if (draftProof != null || StrUtil.isNotBlank(draft)) {
                    result = draftProof == null ? parsePanelList(draft) : draftProof.panels();
                    if (result != null && !result.isEmpty()) {
                        String issue = "恢复上次未通过的场次；保留原文对白与人物表演，重新核对时间表";
                        try { assets.referenced(result); assets.validateSceneCast(sceneScript, previousSceneBridge(scenes, sceneIndex), result); ShortDramaShotDesign.validate(result); validateScenePlan(sceneScript,result,minimum); issue = null; }
                        catch (ShortDramaSceneCheckpoint.BindingMismatch invalid) { result = null; issue = invalid.getMessage(); }
                        catch (ShortDramaShotDesign.InvalidDesign invalid) { result = null; issue = invalid.getMessage(); }
                        catch (RuntimeException invalid) { issue=invalid.getMessage(); }
                        if (result != null && issue == null) {
                            recoveredDraft = true;
                            sceneCheckpoints.save(identity, assets, result, true, "draft_recovered");
                            RedisUtils.setCacheObject(key, JsonUtils.toJsonString(result), java.time.Duration.ofDays(7));
                        } else { result = null; prompt = initialPrompt + "\n上次输出无法读取或绑定：" + issue + "。返回完整本场JSON，保留原文。"; }
                    }
                }
                // Retry only unreadable output or unusable references; creative reviews never request a rewrite.
                for(int attempt=1;attempt<=2 && !recoveredDraft;attempt++) {
                    String candidateId = null;
                    try {
                    if(emitter!=null)emit(emitter,"storyboard_plan","running","第"+(sceneIndex+1)+"场规划（第"+attempt+"次）");
                    if (progress != null) progress.clearDrafts(sceneIndex + 1);
                    var partialOrdinal = new java.util.concurrent.atomic.AtomicInteger();
                    var preview = new ShortDramaPanelStreamParser(node -> {
                        if (progress != null) progress.card(sceneIndex + 1, partialOrdinal.incrementAndGet(), "draft", node);
                    });
                    String response=emitter==null?chatModel.chat(prompt):streamingChat(streamingModel!=null?streamingModel:buildStreamingChatModel(),chatModel,prompt,emitter,"storyboard_plan_parallel", preview::accept, "第"+(sceneIndex+1)+"场规划 · 第"+attempt+"次");
                    candidateId = sceneCandidates.recordResponse(identity, requestId, attempt, "plan", response);
                    result = parseRawPanelList(response);
                    if (result != null) ShortDramaShotDesign.expandDeltas(result);
                    if(result==null || result.isEmpty())throw new IllegalStateException("模型未返回有效镜头");
                    assets.referenced(result);
                    assets.validateSceneCast(sceneScript, previousSceneBridge(scenes, sceneIndex), result);
                    sceneCheckpoints.save(identity, assets, result, false, "native_draft");
                    RedisUtils.setCacheObject(key+":draft",JsonUtils.toJsonString(result),java.time.Duration.ofDays(7));
                    ShortDramaShotDesign.validate(result);
                    validateScenePlan(sceneScript,result,minimum);
                    sceneCheckpoints.save(identity, assets, result, true, "native_validated");
                    RedisUtils.setCacheObject(key,JsonUtils.toJsonString(result),java.time.Duration.ofDays(7));
                    sceneCandidates.checked(identity, candidateId, null);
                    break;
                }catch(Exception e){
                    log.warn("分镜规划校验未通过: requestId={}, scene={}, attempt={}, issue={}", requestId, sceneIndex + 1, attempt, ShortDramaSceneCandidateDiagnostics.redact(e.getMessage()));
                    if (candidateId != null) sceneCandidates.checked(identity, candidateId, e.getMessage());
                    if(attempt>=2)throw new IllegalStateException("第"+(sceneIndex+1)+"场失败，其他已完成场次保留："+e.getMessage(),e);
                    result = null;
                    prompt = initialPrompt + "\n上次输出无法读取或绑定：" + e.getMessage()
                        + "。请返回完整本场JSON，使用登记的准确角色、形象和场景名称；不要因为格式修复而增删原剧情。";
                }
                }
            }
            if (progress != null) {
                progress.clearDrafts(sceneIndex + 1);
                for (int ordinal = 0; ordinal < result.size(); ordinal++)
                    progress.card(sceneIndex + 1, ordinal + 1, "planned", AtlasMediaSupport.OBJECT_MAPPER.valueToTree(result.get(ordinal)));
            }
            if(emitter!=null)emit(emitter,"storyboard_plan","running","分场规划已完成 "+completed.incrementAndGet()+"/"+scenes.size()+" 场（最多3路并发）");
            return result;
        }));
        List<StoryboardPanelData> all=new ArrayList<>();
        for(int i=0;i<byScene.size();i++)for(var panel:byScene.get(i)) {
            panel.setPanelNumber(all.size()+1);panel.setSceneNumber(i+1);all.add(panel);
            if (progress != null) progress.bindGlobal(panel.getPanelNumber(), i + 1, byScene.get(i).indexOf(panel) + 1);
            if(emitter!=null)emitPanel(emitter,panel);
        }
        return all;
    }

    static void validateScenePlan(String scene, List<StoryboardPanelData> panels) {
        validateScenePlan(scene, panels, 1);
    }

    private static void validateCompletedSceneBudgets(ShortDramaScript script, List<StoryboardPanelData> panels, int minimum) {
        var scenes = splitScriptScenes(firstNotBlank(script.getScriptText(), script.getOutlineText(), ""));
        for (int i = 0; i < scenes.size(); i++) {
            int number = i + 1;
            var group = panels.stream().filter(p -> java.util.Objects.equals(p.getSceneNumber(), number)).toList();
            if (group.isEmpty()) throw new IllegalStateException("细化后缺少第" + number + "场，未落库");
            ShortDramaShotDesign.validate(group);
            validateScenePlan(scenes.get(i), group, minimum);
        }
    }

    static void validateScenePlan(String scene, List<StoryboardPanelData> panels, int minimum) {
        validateSceneDuration(scene, panels, minimum);
        for (int i = 0; i < panels.size(); i++) {
            var panel = panels.get(i);
            ShortDramaTiming.validate(i + 1, panel.getDuration(), panel.getSourceText(), buildContinuityJson(panel));
        }
        for (String issue : scenePlanIssues(scene, panels)) log.info("分镜内容审阅建议（继续保存）：{}", issue);
    }

    static List<String> scenePlanIssues(String scene, List<StoryboardPanelData> panels) {
        List<String> errors = new ArrayList<>();
        var estimate = java.util.regex.Pattern.compile("预计\\s*(\\d+(?:\\.\\d+)?)\\s*秒").matcher(scene.lines().findFirst().orElse(""));
        if (estimate.find()) {
            double expected = Double.parseDouble(estimate.group(1));
            int actual = panels.stream().mapToInt(p -> p.getDuration() == null ? 0 : p.getDuration()).sum();
            if (Math.abs(expected - actual) > Math.max(.5, expected * .1))
                errors.add("本场预计" + expected + "秒，当前安排" + actual + "秒，可按实际内容与节奏审阅");
        }
        for (int i=0;i<panels.size();i++) {
            var panel=panels.get(i);
            try { errors.addAll(ShortDramaTiming.issues(i+1,panel.getDuration(),panel.getSourceText(),buildContinuityJson(panel))); }
            catch (RuntimeException e) { errors.add(e.getMessage()); }
        }
        StringBuilder spoken=new StringBuilder();
        var quotes=java.util.regex.Pattern.compile("「([^」]+)」");
        for(var p:panels) {
            var m=quotes.matcher(firstNotBlank(p.getSourceText(),""));
            while(m.find())spoken.append(m.group(1).replaceAll("[^\\p{IsHan}A-Za-z0-9]",""));
        }
        int cursor=0; var original=quotes.matcher(scene);
        while(original.find()) {
            String line=original.group(1).replaceAll("[^\\p{IsHan}A-Za-z0-9]","");
            if(line.isBlank())continue;
            int at=spoken.indexOf(line,cursor);
            if(at<0)errors.add("遗漏或乱序原文对白：「"+original.group(1)+"」；跨镜拆句须保留原字句，source_text只含本镜部分");
            else cursor=at+line.length();
        }
        return List.copyOf(errors);
    }

    static void validateSceneDuration(String scene, List<StoryboardPanelData> panels) {
        validateSceneDuration(scene, panels, 1);
    }

    static void validateSceneDuration(String scene, List<StoryboardPanelData> panels, int minimum) {
        if (panels == null || panels.isEmpty()) throw new IllegalStateException("本场未返回有效分镜");
        if (panels.stream().anyMatch(p -> p == null || p.getDuration() == null || p.getDuration() <= 0))
            throw new IllegalStateException("制作估算时长须为正整数秒");
    }

    static String buildSceneBudgetRepairPrompt(String scene, List<StoryboardPanelData> panels, String error) {
        return ShortDramaSceneBudget.repairPrompt(scene, panels, error);
    }

    static List<StoryboardPanelData> applyTimingRepair(List<StoryboardPanelData> originals, String response) {
        return ShortDramaSceneBudget.applyRepair(originals, response);
    }

    private static String sceneBudgetScaffold(String scene) {
        return ShortDramaSceneBudget.scaffold(scene);
    }

    private String storyboardCheckpointKey(ShortDramaScript script, Long projectId) {
        List<org.ruoyi.common.mybatis.core.domain.BaseEntity> assets = new ArrayList<>();
        var chars = characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        assets.addAll(chars);
        assets.addAll(locationMapper.selectList(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId)));
        if (!chars.isEmpty()) assets.addAll(characterAppearanceMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
            .in(ShortDramaCharacterAppearance::getCharacterId, chars.stream().map(ShortDramaCharacter::getId).toList())));
        ShortDramaProject project = projectMapper.selectById(projectId);
        return checkpointKey(projectId, script.getId(), script.getScriptText(), script.getOutlineText()+ShortDramaScriptPreparation.worldContext(script),
            project.getArtStyle(), projectAspectRatio(projectId), planningAssetSignature(assets));
    }

    static String planningAssetSignature(List<? extends org.ruoyi.common.mybatis.core.domain.BaseEntity> assets) {
        List<String> records = new ArrayList<>();
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (var asset : assets) {
            JsonNode data = mapper.valueToTree(asset);
            ObjectNode relevant = JsonNodeFactory.instance.objectNode();
            relevant.put("type", asset.getClass().getSimpleName());
            for (String field : List.of("id", "characterId", "name", "gender", "ageRange", "roleLevel",
                "personalityTags", "introduction", "visualDescription", "summary", "descriptions",
                "availableSlots", "hasCrowd", "crowdDescription", "appearanceIndex", "changeReason", "description")) {
                if (data.has(field)) relevant.set(field, data.get(field));
            }
            records.add(relevant.toString());
        }
        // Image completion and selection must not discard successful text-planning checkpoints.
        java.util.Collections.sort(records);
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(String.join("\n", records));
    }

    static String checkpointKey(Long projectId, Long scriptId, String script, String outline, String style, String aspect, String assetVersion) {
        String input = String.join("\n", StrUtil.nullToEmpty(script), StrUtil.nullToEmpty(outline), StrUtil.nullToEmpty(style), aspect, assetVersion);
        return "short-drama:checkpoint:v1:" + projectId + ":" + scriptId + ":" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(input);
    }

    static List<String> splitScriptScenes(String text) {
        List<String> scenes = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean seenHeader = false;
        for (String line : text.split("\\R")) {
            boolean heading = line.strip().matches("^(?:#{1,6}\\s*)?(?:第[一二三四五六七八九十0-9]+场[：:、. ]*)?(?:内景|外景|内外景|闪回[：:]?\\s*(?:内景|外景)).*");
            // Numbered screenplay headings are scene boundaries too; ordinary prose stays intact.
            heading = heading || line.strip().matches("^#{2,6}\\s+[一二三四五六七八九十百0-9]+[\\s　、.．]+\\S.*");
            // This is the plain-text heading emitted by our own initial screenplay writer.
            heading = heading || line.strip().matches("^[一二三四五六七八九十百]+[\\s　]+[^：:\\n]+[。．.]\\s*(?:[^：:\\n]+[。．.]|（[^\\n]*）)?$");
            if (heading && seenHeader) { scenes.add(current.toString().trim()); current.setLength(0); }
            if (heading) seenHeader = true;
            current.append(line).append("\n");
        }
        if (!current.toString().isBlank()) scenes.add(current.toString().trim());
        return scenes;
    }

    static String buildStoryboardPlanPrompt(String text, String charsLib, String locsLib,
                                                     String charsIntro, String charsAppearanceList, String charsFullDesc,
                                                     String artStyle, String aspectRatio) {
        artStyle = ShortDramaDirectorSkills.compactSelected(artStyle, "emotional-dialogue", "story-causality", "cinematic-storyboard", "director-blocking", "real-material", "director-review");
        return ShortDramaDirectorSkills.load("emotional-dialogue", "story-causality", "cinematic-storyboard", "director-blocking", "real-material", "director-review") + """
            你是专业的分镜规划师，为原剧本安排连贯的视频生成段。先设计段落，再安排段内摄影子镜头，不把每次换景别、反应或摄影切点都保存为独立生成任务。

            【三级规划流程 - 最高优先级】
            在同一次请求中完成以下三级制作规划，在scene_plan中给简短的制作摘要，再输出panels；不输出推理过程，不追求固定镜数：
            1. 原文预算场 scene：本次只拆一个原文场，scene_number保留原文场身份；同场多地点或跨年蒙太奇只划分segment，不按空间或年份重排预算场
            2. 预演 segment：先估算原文对白、独占动作和必要停顿的真实时间；未给时长时由内容决定，不预设片长。说话人切换、情绪反应或动作阶段本身不构成拆镜理由
            3. 生成段 panel：一条JSON就是一次视频生成，等于一个segment，时长按实际内容估算。先按原事件的因果、对白与动作容量安排连贯表演，再决定自然分段和摄影切点。段内可切换摄影角度或注意中心，不按句子或摄影切点逐项创建任务；不使用总秒数除以上限作为固定镜数公式

            每个片段必须形成小型因果闭环：bridge_in触发 → segment_goal → 连续动作/对话 → segment_result → bridge_out。
            相邻片段必须通过动作、情绪、视线或声音中的至少一种桥梁衔接，禁止在片段边界冻结后跳转。

            【先规划段落，再写摄影子镜头】
            - scene_plan先写core_event（本场真正发生的原事件）、estimated_seconds（原对白与有效表演的粗估总秒数）、segment_outline（自然段落安排）
            - segment_outline每项写goal、estimated_seconds、camera_beats、boundary_reason。camera_beats只是段内拍法，可是一镜连续完成，也可有多个自然切点；不把这些子镜头分别列为panels。boundary_reason说明开场，或后续新目标、明确换时换地、对白动作容量为何需要另段；摄影换角度不是划段理由
            - 原文几句话讲一次连续操作时，先整体预演“触发—操作—可见结果”，将空间建立和反应穿插到主动作中。不要先给每句话建一段，再为这些段落凑目标、结果和时长
            - 先按人物持续目标聚合核心事件。门闩卡住、抬闩、推门是一次操作，不把开始状态、问题、应对、恢复分别分配一个生成段。起始空间随主动作建立，恢复后的姿态是动作末尾的落点；展开panels前，在本次规划内合并能连续完成的相邻安排，不用后置镜数拦截
            - 总量锚定用于规划：先按本场事件规模决定大致表演长度，再安排自然分段。字数只参考内容规模，材料、衣着、光色与环境的描述不等于需要额外播放的时间
            - 原文多场、多事件或长对白时保留其真实容量；短动作不用凑固定段长、固定子镜头数或完整短剧套路。结尾结果成立即可收束

            【按原文规模安排生成段】
            - 按可拍动作和场次时间决定镜数，不按字数凑镜头，不强制建立镜、反应镜独立成镜
            - 按有效表演时间、信息重点和自然节奏安排合适的生成段，不能先列建立、触发、反应、决定、行动、结果七类镜头再填剧情。有明确场次预算才核对总时长（误差不超过10%%）；无预算就按原事件自然长度结束
            - 同一连续事件可以同段完成，也可按观看重点和表演节奏自然分段；不要求几句话固定一段，不追求指定镜数。多场、多事件、长对白保留真实内容容量
            - 同框可看清的触发、操作和结果可以放在同段；对白与听者反应可在同一双人构图中完成，不为每次应答机械另建生成任务。新增panel的motivation写清叙事、观看重点或节奏理由
            - 不延长或扩写原剧情：禁止添加试探、失败、重试、解释性对白、无关出发抵达或第三次反应；环境、衣料、运镜的精细描写与原动作并行，不单独成为镜头
            - 普通单镜按对白、独占动作与自然停顿预算分配，固定机位和安静反应均可，不强制每秒变化
            - 单镜按实际对白或动作安排节奏，不强制三段动作，不因估算秒数差异增拆镜头
            - 每个panel就是一个生成片段，segment_number与本场panel序号一一对应；不要在一个segment内另列多个panel。段内可先全景看清关系，再用局部摄影切点看清手与绳的接触，之后回到动作结果
            - 时长必须由实际动作、对白和运镜容量决定，禁止按场景类型机械给长时长
            - 原文只支持1-3秒内容时，应与相邻因果动作合并，禁止靠环境空转填充成长镜头
            - 对话密集场景：先计算发音时长再拆生成段，同一段可让不同人物依次说话和同步回应，不能同时抢声；长对白可在段内切到倾听画面，保留完整原话

            【分镜原则】
            1. 场景信息可在主动作镜头内建立，不强制另加建立镜头
            2. 同一目的下连续动作可以同镜，切镜由注意力或关系变化驱动
            3. 对话按真实发音和必要反应预算拆镜，不给每段固定镜数
            4. 情绪转折可以留在稳定双人中景，只有细节承担信息时才用特写

            【每个分镜包含】
            - panel_number: 全剧镜头序号
            - scene_number: 当前原文预算场身份，不因本场内部地点/时间变化递增
            - segment_number: 当前场内片段序号；与自然生成段对应
            - segment_goal: 当前片段要完成的唯一剧情任务
            - segment_result: 片段结束时完成的剧情变化
            - bridge_in: 本片段如何承接上一片段，首片段写开场建立
            - bridge_out: 留给下一片段承接的动作、情绪、视线或声音
            - description: 按实际段内摄影顺序写观众看到与听到的画面、动作与回应；段内有切点时写清“切到/切回”和信息目的，无必要时保持一镜连续。禁止主观情绪词，不新增原文事件
            - shot_design: focus、viewer_gain、framing、axis、movement、motivation、transition、cut_out均必填。首镜或显式换时/换地镜另写完整cut_in、state_in，预先列齐本段后续会变化的演员、器物、环境与声音属性及原文初态；每镜写state_delta对象，只含本镜真正变化的初态已有键，无变化为{}。后续continuous镜省略cut_in/state_in；后台逐字继承前镜cut_out/state_out，并用state_delta合成末态供审阅。不要反复抄写相同状态，新出现的状态可在当前段补充，不猜造此前初态；完整state_in/state_out旧格式仍可用，文字差异仅供审阅。
            - characters: [{name, appearance, slot}]，name必须与资产库一致；appearance只能逐字复制“角色形象列表”中方括号后的changeReason，或只写方括号内的数字索引，禁止把服装描述、年龄说明或“某某形象：……”写进appearance
            - location: 从场景资产库选择，名字完全一致
            - scene_type: daily/emotion/action/epic/suspense
            - source_text: 对应原文片段（必填，保留实际对白与说话人）。保留动作段落与对白的换行；每句对白独占一行，写为“角色名：「原句」”，不得把对白后的动作接在同一行。字幕、文书文字标为“画面文字：”，不要当成对白。叙述性冒号后换行，禁止将整段动作压平拼进说话人行，否则时长估算会误把动作当台词。
            - performance_beats: [{name,acting}]，逐个出镜角色写本镜触发、意图、可见回应与未解决的情绪；禁止通用表情模板。无人镜头为[]
            - start_state: 当前镜头0秒静态状态；连续同一时刻承接上一镜end_state，明确时间跳转/跨年匹配切可在同一几何空间重新建立当前年龄、服饰、季候和姿态
            - end_state: 当前镜头最后一帧状态，明确人物位置、朝向、姿态、道具和动作结果
            - continuity_action: 与下一镜头衔接的未完成动作、视线方向或运动趋势
            - spatial_anchor: 场景空间锚点，明确左右、前后、远近和人物相对位置
            - present_characters: 本镜0秒画面中的登记角色精确名字，只写具名/已登记演员与明确可见CG；后续入场者放characters但不提前加入首帧名单，画外声不列入
            - background_extras: 可空文字字段；仅描述本镜0秒原文明确安排的匿名群演，格式“0秒：可见人数=6-8；身份服饰=匿名军士，粗布军服；位置=梯架两侧；姿态动作=双手扶梯、尚未登顶”，不得创建名字或ID
            - beat_type: 当前镜头唯一的剧情职责，只能是setup/trigger/reaction/decision/action/result/transition之一
            - narrative_cause: 当前镜头发生的直接原因，必须来自上一镜头story_result或剧本中的明确触发
            - character_goal: 当前焦点角色在这一刻想达成的具体目标
            - story_action: 为实现目标采取的可见行动或说出的关键台词
            - story_result: 当前行动造成的新信息、新阻碍、位置变化或关系变化，镜头结束前必须发生
            - next_hook: 有后续生成段时写其承接的问题、动作或后果；本场结尾写明确收束，不凭空制造下一镜
            - duration: 制作估算的正整数秒。以对白实读、独占动作、反应停顿为参考，允许估算差异，不是视频模型的固定秒数。
            - timing: {spoken_text:实际发音台词（金额日期展开读音）, speech_rate:语速默认4老人3.2, action_seconds:不与对白重叠的动作秒数, pause_seconds:必要停顿秒数（无则0）, action_note:动作与对白是否同步, pacing_note:较长非对白时段的具体事件/独占动作及其叙事必要性}。
            - duration与timing均为估算参考，真实对白可以和视线、表情、运镜同时进行，不把误差当成必须拆镜的理由。
            - 先按有效内容安排自然节奏，再估时。眨眼、转头、观察服饰可与对白并行；pacing_note可解释有意义的动作和停顿，不必凑最短整数秒或严格满足总预算。保留动作发起、结果与反应的因果关系，不为凑时长增加事件。


            【片段间过渡设计 - 最高优先级】
            1. 连续动作：前片段bridge_out写动作起始态，后片段bridge_in必须从动作进行时或完成时开始
            2. 情绪延续：前片段结尾用反应、眼神、微表情或肢体细节铺垫，后片段首镜强化或反转
            3. 视线匹配：前片段角色看向某物，后片段首镜展示该物或其造成的结果
            4. 声音桥接：前片段末尾的台词关键词或物理声源，可由后片段首镜画面回应
            5. 场景切换：必须由出发、开门、转身离场、时间标记、声音先入或结果揭示触发，禁止无因硬切

            【剧情因果链 - 最高优先级】
            在输出JSON前，先在内部按剧本原文顺序建立完整因果主线，但不要输出分析过程。
            1. beat_type标记每段的主要剧情职责，不是拆镜清单。同一关注中心的触发、应对、结果应在同段连续演完，不为职责标签拆成独立生成任务
            2. 镜头N必须产生story_result；镜头N+1的narrative_cause必须直接承接该story_result或next_hook
            3. 禁止“因为剧本接下来这样写”式跳跃。角色改变位置、情绪、目标、关系或道具状态时，必须先出现触发和过渡动作
            4. 若状态从A变到C，必须展示B的过渡过程，可在同一连续镜头内完成，例如站立→坐下→坐着，不必为每个状态拆镜
            5. 对话不能只是轮流说话：关键台词带来的认知、决定或行动变化可以由同框听者同步反应体现，不强制留到下一镜
            6. 场景切换必须有剧情原因和转场钩子，例如人物出发、时间推进、视线落向目标或结果揭示
            7. source_text必须严格按原文顺序覆盖，不得将后文结果提前，也不得遗漏导致因果断裂的关键动作
            8. 禁止连续镜头重复同一信息；每一镜结束时剧情状态必须相对开头发生可说明的变化
            9. 新角色、新道具、新能力、新地点首次出现必须有建立或触发，不得凭空加入
            10. 最终自检整条链：删除任一镜头后若不影响理解，说明该镜头无剧情作用，应合并或重写

            【叙事连续性与空间锚定 - 最高优先级】
            1. 同一location且同一连续时刻内，下一镜start_state承接上一镜end_state；原文明示跨年/时间跳转时可匹配切重建当前年龄、服饰、季候与姿态，保留几何，禁止要求人物在镜内瞬变
            2. 登记演员是否可见由本镜0秒构图及原文决定；跨原文场不得仅因上一场未写离场就继承演员，背景匿名群众使用background_extras，不升级为角色资产
            3. 连续动作应包含开始、过程与结果；能够在预算内完成时优先放在同一镜头，只有叙事视点或时间容量确实要求时才拆镜
            4. 保持180度轴线：同一对话或对峙中，人物左右关系、面对方向不得无理由反转
            5. 道具归属、手持状态、服装形象、伤势、光线方向和时间必须连续
            6. 每个镜头生成前自检：人物从哪里来、现在在哪里、面向谁、正在做什么、镜头结束后停在哪里
            7. 场景切换时start_state明确写“新场景建立”，不得伪装成连续动作

            【全局视觉约束 - 最高优先级】
            - 项目视觉风格：%s。所有description必须明确遵守该风格，禁止混入真人写实、摄影棚实拍等冲突表达
            - 项目画面比例：%s。构图和角色站位必须适应该画幅，避免主体被裁切

            【输出格式】
            只返回JSON对象，先scene_plan制作摘要，后panels实际生成段；estimated_seconds是规划参考，不是指定镜数的公式。示例只有一个完整动作段，不是所有故事都固定一段：
{"scene_plan":{"core_event":"来者推门打断桌前人的工作，对方抬头回应","estimated_seconds":8,"segment_outline":[{"goal":"同框看清推门引起抬头的结果","estimated_seconds":8,"camera_beats":["门与桌同框呈现推门、抬头与视线相遇"]}]},"panels":[{"panel_number":1,"scene_number":1,"segment_number":1,"segment_goal":"张三进入办公室并打断李四","segment_result":"李四注意力转向张三","bridge_in":"开场建立办公室与人物位置","bridge_out":"两人视线相遇，动作收束","description":"...","shot_design":{"focus":"张三推门时李四抬头的动作接触与反应","viewer_gain":"李四注意被来者打断","framing":"门与桌同侧中景，张三在右、李四在左","axis":"办公桌南侧，人物左右固定","movement":"固定机位","motivation":"同框看清推门触发抬头的因果","transition":"opening","cut_in":"门关闭，李四正看桌上文件","cut_out":"张三推门停下，李四抬头，两人视线相遇","state_in":{"张三.位置":"门外右侧","李四.视线":"桌上文件","光线.方向":"左窗"},"state_delta":{"张三.位置":"门内右侧","李四.视线":"张三"}},"timing":{"spoken_text":"","speech_rate":4,"action_seconds":6,"pause_seconds":2,"action_note":"推门与抬头先后发生","pacing_note":"人物进入并引起注意"},"characters":[{"name":"张三","appearance":"初始形象","slot":"门口"},{"name":"李四","appearance":"初始形象","slot":"桌后"}],"location":"办公室_白天","scene_type":"daily","source_text":"...","start_state":"张三位于门外右侧，面向办公室，李四看文件","end_state":"张三停在门内右侧，李四抬头与其视线相遇","continuity_action":"视线相遇后收束","spatial_anchor":"张三画面右侧，李四画面左侧办公桌后","present_characters":["李四"],"beat_type":"trigger","narrative_cause":"张三推门进入，打断李四看文件","character_goal":"让李四注意来者","story_action":"张三推门进入，李四抬头","story_result":"李四被打断并抬头看向张三","next_hook":"本场视线相遇后收束，不新增下一镜","duration":8}]}
            ⚠️ 严格JSON，不得输出markdown。

            角色资产库：%s
            场景资产库：%s
            角色介绍：%s
            角色形象列表：%s
            角色完整描述：%s

            剧本内容：
            %s
            """.formatted(artStyle, aspectRatio, charsLib, locsLib, charsIntro, charsAppearanceList, charsFullDesc, text) + ShortDramaBackgroundExtras.promptRules();
    }

    // ==================== Phase 4: 摄影规则 ====================

    private List<JsonNode> executePhase4_PhotographyRules(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId) {
        return executePhase4_PhotographyRules(chatModel, panels, projectId, null);
    }

    private List<JsonNode> executePhase4_PhotographyRules(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId, SseEmitter emitter) {
        try {
            String panelsJson = JsonUtils.toJsonString(panels);
            String locsDesc = buildLocationsDescString(projectId);
            String charsInfo = buildCharactersInfoString(projectId);
            String prompt = buildCinematographerPrompt(panelsJson, panels.size(), locsDesc, charsInfo) + projectDirection(projectId);
            String response;
            if (emitter != null) {
                response = streamingChat(buildStreamingChatModel(), chatModel, prompt, emitter, "photography");
            } else {
                response = chatModel.chat(prompt);
            }
            // 用 JsonNode 直接保存原始 JSON，避免 POJO 反序列化丢失字段
            JsonNode array = parseJsonNode(extractJson(response));
            if (array != null && array.isArray()) {
                List<JsonNode> rules = new ArrayList<>();
                int matched = 0;
                for (JsonNode node : array) {
                    if (!node.isObject()) continue;
                    rules.add(node);
                    int pn = node.path("panel_number").asInt(-1);
                    if (pn <= 0) continue;
                    int idx = pn - 1;
                    if (idx >= 0 && idx < panels.size()) {
                        matched++;
                    }
                }
                if (matched == 0 && emitter != null) {
                    emit(emitter, "photography", "error", "摄影规则JSON解析成功但未匹配到任何镜头");
                } else if (matched > 0 && emitter != null) {
                    emit(emitter, "photography", "done", "摄影规则设计完成（" + matched + "/" + panels.size() + "）");
                }
                return rules;
            } else if (emitter != null) {
                emit(emitter, "photography", "error", "摄影规则生成失败：LLM返回格式异常");
            }
        } catch (Exception e) {
            log.warn("Phase 4 摄影规则生成失败: {}", e.getMessage());
            if (emitter != null) emit(emitter, "photography", "error", "摄影规则生成异常：" + e.getMessage());
        }
        return List.of();
    }

    private static String buildCinematographerPrompt(String panelsJson, int panelCount, String locsDesc, String charsInfo) {
        return ShortDramaDirectorSkills.load("emotional-dialogue", "director-blocking", "visual-world") + """
            你是一位经验丰富的电影摄影指导(Director of Photography)。你的任务是为一组分镜中的每个镜头分别设计摄影规则。

            【核心职责】
            分析整组分镜后，为每个镜头单独设计以下视觉要素：
            1. 灯光设置 - 光源方向和质感
            2. 角色位置 - 画面中的具体位置
            3. 景深设置 - 根据镜头类型确定景深
            4. 色调风格 - 整体色彩氛围

            【景深参考】
            全景/远景：深景深（T8.0），清晰展现空间
            中景：中等景深（T4.0）
            近景：浅景深（T2.8），轻微背景虚化
            特写：极浅景深（T1.8），强烈背景虚化
            越肩镜头：浅景深，前景肩膀虚化

            【对话镜头景深规则 - 口型同步要求】
            任何角色说话的镜头，如果出现多张脸，必须使用浅景深或极浅景深（T2.8或更小）
            说话者脸部必须清晰聚焦，背景中的其他角色必须虚化

            【输出格式】
            返回JSON数组，每个元素对应一个镜头的摄影规则。数组长度必须=%d。

            {
              "panel_number": 1,
              "scene_number": 1,
              "segment_number": 1,
              "segment_goal": "当前片段唯一剧情任务",
              "segment_result": "当前片段结束时的剧情变化",
              "bridge_in": "承接上一片段的桥梁",
              "bridge_out": "引向下一片段的桥梁",
              "scene_summary": "场景描述",
              "lighting": {"direction": "主光从画面右侧窗户照入", "quality": "柔和的自然光，暖色调"},
              "characters": [{"name": "角色名", "screen_position": "画面左侧", "posture": "站立", "facing": "面向右侧"}],
              "depth_of_field": "深景深（T8.0），清晰展现宫殿空间",
              "color_tone": "暖色调，温馨氛围"
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。使用相对方向（画面左侧/右侧），禁止使用东南西北。

            分镜数据（共%d个镜头）：
            %s

            场景描述：
            %s

            角色信息：
            %s
            """.formatted(panelCount, panelCount, panelsJson, locsDesc, charsInfo);
    }

    // ==================== Phase 5: 表演指导 ====================

    private List<ActingDirectionResult> executePhase5_ActingDirections(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId) {
        return executePhase5_ActingDirections(chatModel, panels, projectId, null);
    }

    private List<ActingDirectionResult> executePhase5_ActingDirections(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId, SseEmitter emitter) {
        try {
            String panelsJson = JsonUtils.toJsonString(panels);
            String charsInfo = buildCharactersInfoString(projectId);
            String prompt = buildActingDirectionPrompt(panelsJson, panels.size(), charsInfo) + projectDirection(projectId);
            String response;
            if (emitter != null) {
                response = streamingChat(buildStreamingChatModel(), chatModel, prompt, emitter, "acting");
            } else {
                response = chatModel.chat(prompt);
            }
            List<ActingDirectionResult> results = parseJsonArray(extractJson(response), ActingDirectionResult.class);
            if (results != null) {
                int matched = 0;
                for (ActingDirectionResult r : results) {
                    if (r.getPanelNumber() == null) continue;
                    int idx = r.getPanelNumber() - 1;
                    if (idx >= 0 && idx < panels.size()) {
                        matched++;
                    }
                }
                if (matched == 0 && emitter != null) {
                    emit(emitter, "acting", "error", "表演指导JSON解析成功但未匹配到任何镜头");
                } else if (matched > 0 && emitter != null) {
                    emit(emitter, "acting", "done", "表演指导编写完成（" + matched + "/" + panels.size() + "）");
                }
                return results;
            } else if (emitter != null) {
                emit(emitter, "acting", "error", "表演指导生成失败：LLM返回格式异常");
            }
        } catch (Exception e) {
            log.warn("Phase 5 表演指导生成失败: {}", e.getMessage());
            if (emitter != null) emit(emitter, "acting", "error", "表演指导生成异常：" + e.getMessage());
        }
        return List.of();
    }

    private static String buildActingDirectionPrompt(String panelsJson, int panelCount, String charsInfo) {
        return ShortDramaDirectorSkills.load("director-blocking") + """
            你是一位经验丰富的表演指导(Acting Director)。你的任务是为一组分镜中的每个镜头设计角色的表演细节。

            【核心职责】
            分析整组分镜后，为每个镜头中的角色用一句话描述完整的表演指令，包含：
            - 情绪状态与强度
            - 面部表情细节
            - 肢体语言与姿态
            - 微动作与视线

            【表演风格匹配 scene_type】
            daily（日常）：自然松弛，微表情为主，动作幅度小
            emotion（情感）：细腻层次，眼神戏份重，情绪渐进
            action（动作）：爆发力强，动作干脆，表情夸张
            epic（史诗）：庄重仪式感，姿态端正，动作缓慢有力
            suspense（悬疑）：紧绷警觉，肢体僵硬，眼神游移

            【表演描述词库】
            表情：眼眶泛红、眉头紧锁、嘴角上扬、目光闪躲、瞳孔收缩、嘴唇颤抖、咬紧牙关
            肢体：握紧拳头、身体前倾、双手交握、肩膀耸起、转身背对、后退一步
            微动作：轻轻眨眼、咽口水、深呼吸、手指轻颤、舔嘴唇、胸口起伏

            【禁止规则】
            禁止抽象情绪词：悲伤、愤怒、紧张→改用可见表现
            禁止身份称呼：母亲、父亲→改用角色名

            【输出格式】
            返回JSON数组，每个镜头一个对象。数组长度必须=%d。

            {
              "panel_number": 1,
              "characters": [
                {"name": "角色名", "acting": "嘴角微扬眼神柔和地看向对方，身体微微前倾，双手自然垂放，轻轻眨眼"}
              ]
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。

            分镜数据（共%d个镜头）：
            %s

            角色信息：
            %s
            """.formatted(panelCount, panelCount, panelsJson, charsInfo);
    }

    /**
     * 按 panel_number 合并细化分镜、摄影规则和表演指导。
     * 与生成阶段解耦，避免并发任务直接修改同一组分镜对象。
     */
    static List<StoryboardPanelData> mergePanelsWithRules(List<StoryboardPanelData> finalPanels,
                                                           List<JsonNode> photographyRules,
                                                           List<ActingDirectionResult> actingDirections) {
        if (finalPanels == null) {
            throw new IllegalArgumentException("分镜合并失败：细化分镜不能为空");
        }
        validatePhotographyRules(finalPanels, photographyRules);
        validateActingDirections(finalPanels, actingDirections);
        applyPhotographyRules(finalPanels, photographyRules);
        applyActingDirections(finalPanels, actingDirections);
        return finalPanels;
    }

    private static List<JsonNode> buildLocalPhotographyRules(List<StoryboardPanelData> panels) {
        List<JsonNode> rules = new ArrayList<>();
        for (StoryboardPanelData panel : panels) {
            ObjectNode rule = JsonNodeFactory.instance.objectNode();
            rule.put("panel_number", panel.getPanelNumber());
            rule.put("scene_summary", firstNotBlank(panel.getDescription(), panel.getSourceText(), ""));

            ObjectNode lighting = rule.putObject("lighting");
            lighting.put("direction", "根据场景主光方向保持连续");
            lighting.put("quality", "沿用当前地点和时段的真实主光，禁止因情绪标签改变色温、方向或曝光");

            ArrayNode characters = rule.putArray("characters");
            if (panel.getCharacters() != null) {
                for (CharacterRef ref : panel.getCharacters()) {
                    ObjectNode character = characters.addObject();
                    character.put("name", firstNotBlank(ref.getName(), "角色"));
                    character.put("screen_position", firstNotBlank(ref.getSlot(), "画面主体区域"));
                    character.put("posture", "保持符合当前动作的自然姿态");
                    character.put("facing", "面向动作目标或对话对象");
                }
            }

            rule.put("depth_of_field", "依景别保留叙事所需的人物与道具清晰度，对话双人镜避免一人失焦");
            rule.put("color_tone", "同地点同时间保持曝光、白平衡与参考图一致");
            rule.put("axis", "先建立空间；同一对话轴同侧拍摄，反打保留视线方向，改变轴侧须经过中性机位或明确移动");
            rules.add(rule);
        }
        return rules;
    }

    static List<ActingDirectionResult> buildLocalActingDirections(List<StoryboardPanelData> panels) {
        List<ActingDirectionResult> directions = new ArrayList<>();
        for (StoryboardPanelData panel : panels) {
            ActingDirectionResult result = new ActingDirectionResult();
            result.setPanelNumber(panel.getPanelNumber());
            ArrayNode characters = JsonNodeFactory.instance.arrayNode();
            if (panel.getCharacters() != null) {
                for (CharacterRef ref : panel.getCharacters()) {
                    ObjectNode character = characters.addObject();
                    character.put("name", firstNotBlank(ref.getName(), "角色"));
                    String acting = null;
                    if (panel.getPerformanceBeats() != null && panel.getPerformanceBeats().isArray()) {
                        for (JsonNode beat : panel.getPerformanceBeats()) {
                            if (ref.getName() != null && ref.getName().equals(beat.path("name").asText())) {
                                acting = beat.path("acting").asText(null);
                                break;
                            }
                        }
                    }
                    character.put("acting", firstNotBlank(acting,
                        "依据本镜原文回应，不新增情绪或台词。原文：" + firstNotBlank(panel.getSourceText(), panel.getDescription(), "")
                        + "；当前行动：" + firstNotBlank(panel.getStoryAction(), "按原文行动")
                        + "；回应后的结果：" + firstNotBlank(panel.getStoryResult(), panel.getEndState(), "保持原文状态")));
                }
            }
            result.setCharacters(characters);
            directions.add(result);
        }
        return directions;
    }

    private static void mergePhotographyRules(List<StoryboardPanelData> panels, List<JsonNode> photographyRules) {
        validatePhotographyRules(panels, photographyRules);
        applyPhotographyRules(panels, photographyRules);
    }

    private static void mergeActingDirections(List<StoryboardPanelData> panels,
                                               List<ActingDirectionResult> actingDirections) {
        validateActingDirections(panels, actingDirections);
        applyActingDirections(panels, actingDirections);
    }

    private static void validatePhotographyRules(List<StoryboardPanelData> panels, List<JsonNode> photographyRules) {
        if (panels == null) {
            throw new IllegalArgumentException("分镜合并失败：分镜不能为空");
        }
        for (int index = 0; index < panels.size(); index++) {
            Integer panelNumber = panels.get(index).getPanelNumber();
            if (findPhotographyRule(photographyRules, panelNumber) == null) {
                throw new IllegalStateException("分镜合并失败：镜头 " + panelNumber
                    + " 缺少摄影规则（index=" + index + "）");
            }
        }
    }

    private static void validateActingDirections(List<StoryboardPanelData> panels,
                                                  List<ActingDirectionResult> actingDirections) {
        if (panels == null) {
            throw new IllegalArgumentException("分镜合并失败：分镜不能为空");
        }
        for (int index = 0; index < panels.size(); index++) {
            Integer panelNumber = panels.get(index).getPanelNumber();
            if (findActingDirection(actingDirections, panelNumber) == null) {
                throw new IllegalStateException("分镜合并失败：镜头 " + panelNumber
                    + " 缺少表演指导（index=" + index + "）");
            }
        }
    }

    private static void applyPhotographyRules(List<StoryboardPanelData> panels, List<JsonNode> photographyRules) {
        for (StoryboardPanelData panel : panels) {
            JsonNode rule = findPhotographyRule(photographyRules, panel.getPanelNumber());
            panel.setPhotographyRules(rule.toString());
        }
    }

    private static void applyActingDirections(List<StoryboardPanelData> panels,
                                              List<ActingDirectionResult> actingDirections) {
        for (StoryboardPanelData panel : panels) {
            ActingDirectionResult acting = findActingDirection(actingDirections, panel.getPanelNumber());
            JsonNode characters = acting.getCharacters();
            panel.setActingNotes(characters == null ? null : characters.toString());
        }
    }

    private static JsonNode findPhotographyRule(List<JsonNode> photographyRules, Integer panelNumber) {
        if (photographyRules == null || panelNumber == null) return null;
        for (JsonNode rule : photographyRules) {
            if (rule != null && rule.path("panel_number").asInt(Integer.MIN_VALUE) == panelNumber) {
                return rule;
            }
        }
        return null;
    }

    private static ActingDirectionResult findActingDirection(List<ActingDirectionResult> actingDirections,
                                                              Integer panelNumber) {
        if (actingDirections == null || panelNumber == null) return null;
        for (ActingDirectionResult acting : actingDirections) {
            if (acting != null && panelNumber.equals(acting.getPanelNumber())) {
                return acting;
            }
        }
        return null;
    }

    // ==================== Phase 6: 分镜细化 ====================

    private void executePhase6_StoryboardDetail(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId) {
        executePhase6_StoryboardDetail(chatModel, null, panels, projectId, null);
    }

    private void executePhase6_StoryboardDetail(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId, SseEmitter emitter) {
        executePhase6_StoryboardDetail(chatModel, null, panels, projectId, emitter);
    }

    private void executePhase6_StoryboardDetail(ChatModel chatModel, StreamingChatModel streamingModel,
                                                 List<StoryboardPanelData> panels, Long projectId, SseEmitter emitter) {
        executePhase6_StoryboardDetail(chatModel, streamingModel, panels, projectId, emitter, "default");
        if (emitter != null) emit(emitter, "storyboard_detail", "done", "全部镜头细化完成");
    }

    private void executePhase6_StoryboardDetail(ChatModel chatModel, StreamingChatModel streamingModel,
            List<StoryboardPanelData> panels, Long projectId, SseEmitter emitter, String modelName) {
        if (panels.size() > 8) {
            String tenant = org.ruoyi.common.tenant.helper.TenantHelper.getTenantId();
            int batches = (panels.size() + 7) / 8;
            java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(Math.min(3, batches));
            java.util.concurrent.atomic.AtomicInteger finished = new java.util.concurrent.atomic.AtomicInteger();
            try {
                List<java.util.concurrent.Future<?>> futures = new ArrayList<>();
                for (int offset = 0; offset < panels.size(); offset += 8) {
                    List<StoryboardPanelData> batch = panels.subList(offset, Math.min(offset + 8, panels.size()));
                    futures.add(pool.submit(() -> org.ruoyi.common.tenant.helper.TenantHelper.dynamic(tenant, () -> {
                        executePhase6_StoryboardDetail(chatModel, streamingModel, batch, projectId, emitter, modelName);
                        int count = finished.incrementAndGet();
                        if (emitter != null) emit(emitter, "storyboard_detail", "running", "镜头细化已完成 " + count + "/" + batches + " 批");
                    })));
                }
                List<String> errors = new ArrayList<>();
                for (java.util.concurrent.Future<?> future : futures) {
                    try { future.get(); }
                    catch (java.util.concurrent.ExecutionException e) { errors.add(e.getCause().getMessage()); }
                }
                if (!errors.isEmpty()) throw new IllegalStateException(String.join("；", errors));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("镜头细化被中断，已完成批次已保存", e);
            } finally {
                pool.shutdownNow();
            }
            return;
        }
        try {
            String panelsJson = detailInput(panels);
            var detailLibrary = panelCharacterLibraries(planningAssets(projectId), panels);
            String charsAgeGender = detailLibrary.get("names") + "\n" + detailLibrary.get("descriptions")
                + "\n【已确认的角色形象，必须逐镜保持，禁止自行换装】\n" + detailLibrary.get("appearances");
            String locsDesc = buildLocationsDescString(projectId);
            String prompt = buildStoryboardDetailPrompt(panelsJson, charsAgeGender, locsDesc,
                artStyleSuffix(projectId), projectAspectRatio(projectId));
            String detailCacheKey = "short-drama:checkpoint:v1:" + projectId + ":detail:v2:" + cn.hutool.crypto.digest.DigestUtil.sha256Hex(modelName + "\n" + prompt);
            String response = RedisUtils.getCacheObject(detailCacheKey);
            List<StoryboardDetailResult> results = null;
            if (StrUtil.isNotBlank(response)) {
                try { results = reviewStoryboardDetailResponse(response, panels); }
                catch (RuntimeException invalid) { response = null; }
            }
            if (StrUtil.isBlank(response)) {
                String repairBaseline = null;
                java.util.Set<Integer> repairNumbers = java.util.Set.of();
                for (int attempt = 1; attempt <= 2; attempt++) {
                    try {
                        if (emitter != null) {
                            StreamingChatModel activeStreamingModel = streamingModel != null ? streamingModel : buildStreamingChatModel();
                            response = streamingChat(activeStreamingModel, chatModel, prompt, emitter, "storyboard_detail", null,
                                (attempt == 1 ? "镜" + panels.get(0).getPanelNumber() + "–" + panels.get(panels.size()-1).getPanelNumber() + "细化" : "镜" + repairNumbers + "修复") + " · 第" + attempt + "次");
                        } else {
                            response = chatModel.chat(prompt);
                        }
                        if (repairBaseline != null && repairNumbers.size() < panels.size()) response = mergeDetailRepair(repairBaseline, response, repairNumbers);
                        results = reviewStoryboardDetailResponse(response, panels);
                        break;
                    } catch (Exception e) {
                        if (attempt == 2) throw e;
                        log.warn("分镜细化校验触发修复: projectId={}, panels={}, reason={}", projectId,
                            panels.stream().map(StoryboardPanelData::getPanelNumber).toList(), ShortDramaSceneCandidateDiagnostics.redact(e.getMessage()));
                        repairBaseline = response;
                        repairNumbers = detailRepairNumbers(response, panels);
                        final var invalidNumbers = repairNumbers;
                        var repairPanels = panels.stream().filter(panel -> invalidNumbers.contains(panel.getPanelNumber())).toList();
                        prompt = buildStoryboardDetailPrompt(detailInput(repairPanels), charsAgeGender, locsDesc,
                            artStyleSuffix(projectId), projectAspectRatio(projectId))
                            + "\n只修复以下镜号：" + repairNumbers + "；已通过镜头保留。具体校验问题：" + e.getMessage();
                        if (emitter != null) emit(emitter, "storyboard_detail", "running", "本批细化未通过，正在修复：" + e.getMessage() + "；已完成批次已保存");
                    }
                }
            }
            if (results != null) {
                int matched = 0;
                for (StoryboardDetailResult r : results) {
                    if (r.getPanelNumber() == null) continue;
                    StoryboardPanelData panel = panels.stream().filter(p -> r.getPanelNumber().equals(p.getPanelNumber())).findFirst().orElse(null);
                    if (panel != null) {
                        panel.setShotType(r.getShotType());
                        panel.setCameraMove(r.getCameraMove());
                        panel.setVideoPrompt(r.getVideoPrompt());
                        panel.setImagePrompt(r.getImagePrompt());
                        panel.setSceneTitle(r.getSceneTitle());
                        panel.setStartState(firstNotBlank(r.getStartState(), panel.getStartState()));
                        panel.setEndState(firstNotBlank(r.getEndState(), panel.getEndState()));
                        panel.setContinuityAction(firstNotBlank(r.getContinuityAction(), panel.getContinuityAction()));
                        panel.setSpatialAnchor(firstNotBlank(r.getSpatialAnchor(), panel.getSpatialAnchor()));
                        if (panel.getPresentCharacters() == null && r.getPresentCharacters() != null) {
                            panel.setPresentCharacters(r.getPresentCharacters());
                        }
                        if (r.getBackgroundExtras() != null) panel.setBackgroundExtras(r.getBackgroundExtras());


                        panel.setSegmentGoal(firstNotBlank(r.getSegmentGoal(), panel.getSegmentGoal()));
                        panel.setSegmentResult(firstNotBlank(r.getSegmentResult(), panel.getSegmentResult()));
                        panel.setBridgeIn(firstNotBlank(r.getBridgeIn(), panel.getBridgeIn()));
                        panel.setBridgeOut(firstNotBlank(r.getBridgeOut(), panel.getBridgeOut()));
                        panel.setBeatType(firstNotBlank(r.getBeatType(), panel.getBeatType()));
                        panel.setNarrativeCause(firstNotBlank(r.getNarrativeCause(), panel.getNarrativeCause()));
                        panel.setCharacterGoal(firstNotBlank(r.getCharacterGoal(), panel.getCharacterGoal()));
                        panel.setStoryAction(firstNotBlank(r.getStoryAction(), panel.getStoryAction()));
                        panel.setStoryResult(firstNotBlank(r.getStoryResult(), panel.getStoryResult()));
                        panel.setNextHook(firstNotBlank(r.getNextHook(), panel.getNextHook()));
                        if (StrUtil.isNotBlank(r.getDescription())) {
                            panel.setDescription(r.getDescription());
                        }
                        matched++;
                    }
                }
                // Natural pauses are valid. Do not inflate action count merely to fill duration.
                if (matched == 0 && emitter != null) {
                    emit(emitter, "storyboard_detail", "error", "分镜细化JSON解析成功但未匹配到任何镜头");
                } else if (matched > 0 && emitter != null) {
                    emit(emitter, "storyboard_detail", "running", "本批细化完成（" + matched + "/" + panels.size() + "）");
                }
            }
            if (panels.stream().anyMatch(p -> StrUtil.isBlank(p.getVideoPrompt()) || StrUtil.isBlank(p.getImagePrompt()))) {
                throw new IllegalStateException("镜头细化不完整，旧分镜已保留，请重试");
            }
            RedisUtils.setCacheObject(detailCacheKey, response, java.time.Duration.ofDays(7));
            var progress = emitter == null ? null : planningProgress.get(emitter);
            if (progress != null) for (var panel : panels)
                progress.ready(panel.getPanelNumber(), AtlasMediaSupport.OBJECT_MAPPER.valueToTree(panel));
        } catch (Exception e) {
            throw new IllegalStateException("分镜细化失败：" + e.getMessage(), e);
        }
    }

    /** Review the complete batch before merging; malformed output cannot partially replace a draft. */
    static List<StoryboardDetailResult> reviewStoryboardDetailResponse(String response, List<StoryboardPanelData> panels) {
        List<StoryboardDetailResult> results = parseJsonArray(extractJson(response), StoryboardDetailResult.class);
        if (results == null || results.isEmpty()) throw new IllegalStateException("分镜细化未返回有效JSON数组");
        java.util.Set<Integer> seen = new java.util.HashSet<>();
        List<String> errors = new ArrayList<>();
        for (StoryboardDetailResult detail : results) {
            var panel = panels.stream().filter(p -> p.getPanelNumber().equals(detail.getPanelNumber())).findFirst().orElse(null);
            if (panel == null || !seen.add(detail.getPanelNumber())) {
                errors.add("细化返回了未知或重复镜号：" + detail.getPanelNumber()); continue;
            }
            detail.setVideoPrompt(ShortDramaVideoPromptReview.normalizeDialogue(detail.getVideoPrompt(), panel.getSourceText()));
            if (panel.getShotDesign() != null && !panel.getShotDesign().isNull()) {
                // Detailing enriches directions. Preserve planned metadata even if the model paraphrases it.
                detail.setStartState(panel.getStartState());
                detail.setEndState(panel.getEndState());
            }
            if (StrUtil.isBlank(detail.getImagePrompt())) errors.add("镜头" + detail.getPanelNumber() + "缺少首帧提示词");
            try {
                ShortDramaBackgroundExtras.parse(panel.getBackgroundExtras());
                if (detail.getBackgroundExtras() != null && !Objects.toString(panel.getBackgroundExtras(), "").trim().equals(detail.getBackgroundExtras().trim()))
                    errors.add("镜头" + detail.getPanelNumber() + "细化不得增加、删除或改写已规划的0秒匿名群演");
                Set<String> permitted = new HashSet<>(panel.getPresentCharacters() == null ? List.of() : panel.getPresentCharacters());
                if (panel.getPresentCharacters() == null && panel.getCharacters() != null) panel.getCharacters().forEach(ref -> permitted.add(ref.getName()));
                if (detail.getPresentCharacters() != null && !permitted.containsAll(detail.getPresentCharacters()))
                    errors.add("镜头" + detail.getPanelNumber() + "细化不得增加未规划的具名出镜角色或CG");
            } catch (IllegalArgumentException e) { errors.add("镜头" + detail.getPanelNumber() + "：" + e.getMessage()); }
            if (StrUtil.isBlank(detail.getVideoPrompt())) errors.add("镜头" + detail.getPanelNumber() + "缺少视频提示词");
            for (String issue : ShortDramaVideoPromptReview.issues(detail.getVideoPrompt(), panel.getDuration(), panel.getSourceText()))
                log.info("镜头{}导演稿审阅建议（继续保存）：{}", detail.getPanelNumber(), issue);
        }
        for (var panel : panels) if (!seen.contains(panel.getPanelNumber())) errors.add("细化遗漏镜头" + panel.getPanelNumber());
        if (!errors.isEmpty()) throw new IllegalStateException(String.join("；", errors));
        return results;
    }

    static void validateVideoPromptPanels(List<StoryboardPanelData> panels) {
        for (var panel : panels) ShortDramaVideoPromptReview.validate(panel.getPanelNumber(), panel.getVideoPrompt(),
            panel.getDuration(), panel.getSourceText());
    }

    static String buildStoryboardDetailPrompt(String panelsJson, String charsAgeGender, String locsDesc,
                                                      String artStyle, String aspectRatio) {
        return ShortDramaDirectorSkills.load("cinematic-storyboard", "video-prompt") + """
            你负责把已校验分镜落实为可执行的首帧和连续导演描述，不重新规划剧情。
            只细化本批给定镜号。shot_design中的focus、framing、axis、movement、motivation、cut_in/out与物理状态是已审阅契约；机位、主动作、重心、持物与声画接点均须落实，不另选姿态或拍法。
            原文、角色形象、出镜名单、群演、场地和原对白为权威；无对白时写清实际环境声。后台保留这些原始字段、起止状态、timing与所有资产绑定，无需在输出中重复。
            video_prompt从给定首态展开：建立本镜前中后景和视线、写动作发起及可见结果与反应，具体机位和主导运镜有动机，沿既有光源写光色材质，声音有方向与远近，最后准确交到end_state。按实际演员与事件写自然段，保留原话及声源，不写逐秒清单或通用导演前缀，不用抽象结果摘要代替可拍过程。
            image_prompt只描述本镜start_state的静态首帧，含具体姿态、接触、位置、人物服装、所见道具、背景与光线；不提前画出结束动作。
            每镜保留一个主导运镜。时长与timing用于预算，不能靠多余事件、停顿、换装、额外切镜或删台词填充。沿用原scene/segment身份，不重新分组、增删或重排镜头。
            视觉方向：%s
            构图画幅：%s
            只返回JSON数组，每镜仅补充以下字段，不复述整套原规划：
            {"panel_number":1,"shot_type":"平视中景","camera_move":"固定","sceneTitle":"本镜具体关注点","description":"当前动作与结果","video_prompt":"完整连续导演描述","image_prompt":"静态首帧描述"}
            如明确返回start_state/end_state等原始字段，其值必须逐字保持。字符串内对白用「」；JSON必须有效。
            已校验规划：%s
            本批角色档案：%s
            本批场景资料：%s
            """.formatted(ShortDramaDirectorSkills.compactSelected(artStyle, "cinematic-storyboard", "video-prompt"), aspectRatio, panelsJson, charsAgeGender, locsDesc)
            + ShortDramaBackgroundExtras.promptRules();
    }

    /** Keep approved input metadata; compact null/unused transport fields rather than asking the model to echo them. */
    static String detailInput(List<StoryboardPanelData> panels) {
        var array = AtlasMediaSupport.OBJECT_MAPPER.createArrayNode();
        for (var panel : panels) {
            JsonNode full = AtlasMediaSupport.OBJECT_MAPPER.valueToTree(panel);
            var input = array.addObject();
            for (String field : List.of("panel_number", "scene_number", "segment_number", "segment_goal", "segment_result",
                    "description", "source_text", "duration", "timing", "shot_design", "start_state", "end_state",
                    "spatial_anchor", "continuity_action", "present_characters", "background_extras", "characters", "location",
                    "narrative_cause", "story_action", "story_result", "performance_beats")) {
                JsonNode value = full.get(field);
                if (value != null && !value.isNull()) input.set(field, value);
            }
        }
        return array.toString();
    }

    static java.util.Set<Integer> detailRepairNumbers(String response, List<StoryboardPanelData> panels) {
        var failed = new java.util.LinkedHashSet<Integer>();
        try {
            JsonNode array = AtlasMediaSupport.OBJECT_MAPPER.readTree(extractJson(response));
            if (!array.isArray()) throw new IllegalArgumentException();
            var known = panels.stream().map(StoryboardPanelData::getPanelNumber).collect(java.util.stream.Collectors.toSet());
            for (JsonNode row : array) if (!known.contains(row.path("panel_number").asInt(-1))) throw new IllegalArgumentException();
            for (var panel : panels) {
                var rows = AtlasMediaSupport.OBJECT_MAPPER.createArrayNode();
                for (JsonNode row : array) if (row.path("panel_number").asInt(-1) == panel.getPanelNumber()) rows.add(row);
                try { reviewStoryboardDetailResponse(rows.toString(), List.of(panel)); }
                catch (RuntimeException invalid) { failed.add(panel.getPanelNumber()); }
            }
            if (failed.isEmpty()) panels.forEach(panel -> failed.add(panel.getPanelNumber())); // Unrecognized transport/extra rows need a full repair.
        } catch (Exception invalid) { panels.forEach(panel -> failed.add(panel.getPanelNumber())); }
        return failed;
    }

    static String mergeDetailRepair(String original, String repair, java.util.Set<Integer> repairedNumbers) {
        try {
            JsonNode old = AtlasMediaSupport.OBJECT_MAPPER.readTree(extractJson(original));
            JsonNode patch = AtlasMediaSupport.OBJECT_MAPPER.readTree(extractJson(repair));
            if (!old.isArray() || !patch.isArray()) return repair;
            var merged = AtlasMediaSupport.OBJECT_MAPPER.createArrayNode();
            for (JsonNode row : old) if (!repairedNumbers.contains(row.path("panel_number").asInt(-1))) merged.add(row);
            for (JsonNode row : patch) merged.add(row);
            return merged.toString();
        } catch (Exception invalid) { return repair; }
    }

    // ==================== 持久化 ====================

    private List<ShortDramaStoryboardVo> persistStoryboards(Long projectId, Long scriptId, List<StoryboardPanelData> panels, ShortDramaScript script) {
        var frozen = planningSkillSnapshots.get(scriptId);
        if (frozen != null) skillCatalog.verify(frozen, projectMapper.selectById(projectId));
        if (panels.isEmpty()) throw new IllegalStateException("没有有效分镜，旧分镜已保留");
        validateVideoPromptPanels(panels); // Before the transaction deletes any existing storyboard.
        // A reviewed paid shot may already exist while the remaining episode is being planned.
        // Preserve only explicitly locked, completed shots in their exact ordinal slot.  The
        // lock lives in continuityJson so ordinary old drafts are still replaced atomically.
        Map<Integer, ShortDramaStoryboard> lockedBySceneNo = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getScriptId, scriptId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo))
            .stream().filter(this::isLockedApprovedStoryboard)
            .collect(java.util.stream.Collectors.toMap(
                ShortDramaStoryboard::getSceneNo, value -> value, (left, right) -> left, LinkedHashMap::new));
        for (var entry : lockedBySceneNo.entrySet()) {
            if (entry.getKey() == null || entry.getKey() < 1 || entry.getKey() > panels.size())
                throw new IllegalStateException("锁定成片镜头号超出新规划范围，旧分镜已保留");
            Integer plannedDuration = panels.get(entry.getKey() - 1).getDuration();
            if (!Objects.equals(entry.getValue().getDurationSeconds(), plannedDuration))
                throw new IllegalStateException("锁定成片镜头" + entry.getKey() + "时长与新规划不一致，旧分镜已保留");
        }
        return transactionTemplate.execute(status -> {
        var delete = new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getScriptId, scriptId);
        if (!lockedBySceneNo.isEmpty()) delete.notIn(ShortDramaStoryboard::getId,
            lockedBySceneNo.values().stream().map(ShortDramaStoryboard::getId).toList());
        storyboardMapper.delete(delete);
        List<ShortDramaStoryboardVo> result = new ArrayList<>();
        int sceneNo = 1;
        for (StoryboardPanelData panel : panels) {
            ShortDramaStoryboard locked = lockedBySceneNo.get(sceneNo);
            if (locked != null) {
                result.add(MapstructUtils.convert(locked, ShortDramaStoryboardVo.class));
                sceneNo++;
                continue;
            }
            ShortDramaStoryboard entity = new ShortDramaStoryboard();
            entity.setId(IdUtil.getSnowflakeNextId());
            entity.setProjectId(projectId);
            entity.setScriptId(scriptId);
            entity.setSceneNo(sceneNo);
            entity.setSceneTitle(firstNotBlank(panel.getSceneTitle(), "镜头 " + sceneNo));
            entity.setSceneText(firstNotBlank(panel.getDescription(), panel.getSourceText(), ""));
            entity.setSceneType(firstNotBlank(panel.getSceneType(), "daily"));
            entity.setShotType(firstNotBlank(panel.getShotType(), "平视中景"));
            entity.setCameraMove(firstNotBlank(panel.getCameraMove(), "缓推"));
            entity.setDurationSeconds(panel.getDuration() != null && panel.getDuration() > 0 ? panel.getDuration() : defaultDurationForSceneType(entity.getSceneType()));
            entity.setVideoPrompt(panel.getVideoPrompt());
            entity.setVideoStatus("pending");
            entity.setLocationName(panel.getLocation());
            entity.setSourceText(panel.getSourceText());
            entity.setImagePrompt(panel.getImagePrompt());
            if (panel.getCharacters() != null && !panel.getCharacters().isEmpty()) {
                entity.setCharactersJson(JsonUtils.toJsonString(panel.getCharacters()));
            }
            entity.setPhotographyRules(panel.getPhotographyRules());
            entity.setActingNotes(panel.getActingNotes());
            entity.setContinuityJson(buildContinuityJson(panel));
            storyboardMapper.insert(entity);
            result.add(MapstructUtils.convert(entity, ShortDramaStoryboardVo.class));
            sceneNo++;
        }
        ShortDramaProject project = projectMapper.selectById(projectId);
        project.setStatus("storyboard_ready");
        projectMapper.updateById(project);
        return result;
        });
    }

    private boolean isLockedApprovedStoryboard(ShortDramaStoryboard storyboard) {
        if (!"done".equals(storyboard.getVideoStatus())
            || StrUtil.isBlank(storyboard.getVideoId()) || StrUtil.isBlank(storyboard.getVideoUrl())) return false;
        try {
            JsonNode continuity = JsonUtils.parseObject(
                StrUtil.blankToDefault(storyboard.getContinuityJson(), "{}"), JsonNode.class);
            return continuity.path("lock_storyboard").asBoolean(false);
        } catch (Exception malformed) {
            throw new IllegalStateException("锁定成片镜头的连续性JSON无效，旧分镜已保留", malformed);
        }
    }

    // ==================== 辅助方法：资产库字符串构建 ====================

    /** The same explicit source-name/alias rule as cast validation; dialogue speakers and required CG stay available. */
    static java.util.Map<String, String> sceneCharacterLibraries(ShortDramaSceneCheckpoint.Assets assets, String scene, String previous) {
        Set<String> names = assets.sourceNames(scene, previous);
        return characterLibraries(assets, names);
    }

    /** Detail batches receive their actual identity dependencies, including original offscreen speakers. */
    static java.util.Map<String, String> panelCharacterLibraries(ShortDramaSceneCheckpoint.Assets assets, List<StoryboardPanelData> panels) {
        Set<String> names = new HashSet<>();
        for (var binding : assets.referenced(panels)) if ("character".equals(binding.kind())) names.add(binding.name());
        return characterLibraries(assets, names);
    }

    private static java.util.Map<String, String> characterLibraries(ShortDramaSceneCheckpoint.Assets assets, Set<String> names) {
        var selected = assets.characters().stream().filter(c -> names.contains(c.getName())).toList();
        StringBuilder library = new StringBuilder(), intro = new StringBuilder(), appearances = new StringBuilder(), description = new StringBuilder();
        for (var c : selected) {
            library.append("- ").append(c.getName()).append("，").append(firstNotBlank(c.getRoleLevel(), "B")).append("级，")
                .append(firstNotBlank(c.getGender(), "未知")).append("，").append(firstNotBlank(c.getAgeRange(), "未知年龄"))
                .append("；明确原文别名：").append(Objects.toString(c.getAliases(), "无")).append("\n");
            intro.append("- ").append(c.getName()).append("：").append(Objects.toString(c.getIntroduction(), "")).append("\n");
            description.append("- ").append(c.getName()).append("：").append(firstNotBlank(c.getVisualDescription(), c.getIntroduction(), "无描述")).append("\n");
            appearances.append("- ").append(c.getName()).append("的形象：");
            for (var a : assets.appearances()) if (Objects.equals(a.getCharacterId(), c.getId()))
                appearances.append("[").append(a.getAppearanceIndex()).append("]").append(firstNotBlank(a.getChangeReason(), "形象"))
                    .append("：").append(firstNotBlank(a.getDescription(), c.getVisualDescription(), "沿用已有参考图")).append("；");
            appearances.append("\n");
        }
        return java.util.Map.of("names", firstNotBlank(library.toString(), "暂无本场明确登记角色，匿名群众按background_extras描述"),
            "introductions", firstNotBlank(intro.toString(), "暂无"), "appearances", firstNotBlank(appearances.toString(), "暂无"),
            "descriptions", firstNotBlank(description.toString(), "暂无"));
    }

    private String buildCharactersLibString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            sb.append("- ").append(c.getName())
                .append("（").append(firstNotBlank(c.getRoleLevel(), "B")).append("级")
                .append("，").append(firstNotBlank(c.getGender(), "未知"))
                .append("，").append(firstNotBlank(c.getAgeRange(), "未知年龄"))
                .append("，").append(firstNotBlank(c.getPersonalityTags(), ""))
                .append("）\n");
        }
        return sb.toString();
    }

    private String buildLocationsLibString(Long projectId) {
        List<ShortDramaLocation> locs = locationMapper.selectList(
            new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId));
        if (locs.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaLocation l : locs) {
            sb.append("- ").append(l.getName());
            if (StrUtil.isNotBlank(l.getSummary())) {
                sb.append("：").append(l.getSummary());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildCharactersIntroString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            sb.append("- ").append(c.getName());
            if (StrUtil.isNotBlank(c.getIntroduction())) {
                sb.append("：").append(c.getIntroduction());
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildCharactersAppearanceListString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            List<ShortDramaCharacterAppearance> appearances = characterAppearanceMapper.selectList(
                new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                    .eq(ShortDramaCharacterAppearance::getCharacterId, c.getId())
                    .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            sb.append("- ").append(c.getName()).append("的形象：");
            for (ShortDramaCharacterAppearance a : appearances) {
                sb.append("[").append(a.getAppearanceIndex()).append("]")
                    .append(firstNotBlank(a.getChangeReason(), "形象")).append("、");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildCharactersFullDescString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            sb.append("- ").append(c.getName()).append("：")
                .append(firstNotBlank(c.getVisualDescription(), c.getIntroduction(), "无描述"))
                .append("\n");
        }
        return sb.toString();
    }

    String buildCharacterAppearanceConstraints(Long projectId) {
        StringBuilder text = new StringBuilder();
        for (ShortDramaCharacter character : characterMapper.selectList(new LambdaQueryWrapper<ShortDramaCharacter>()
                .eq(ShortDramaCharacter::getProjectId, projectId))) {
            List<ShortDramaCharacterAppearance> appearances = characterAppearanceMapper.selectList(
                new LambdaQueryWrapper<ShortDramaCharacterAppearance>().eq(ShortDramaCharacterAppearance::getCharacterId, character.getId())
                    .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            if (appearances.isEmpty()) text.append(character.getName()).append("：").append(character.getVisualDescription()).append("\n");
            for (ShortDramaCharacterAppearance appearance : appearances) {
                text.append(character.getName()).append(" / ").append(appearance.getChangeReason()).append("：")
                    .append(firstNotBlank(appearance.getDescription(), character.getVisualDescription(), "沿用已有参考图")).append("\n");
            }
        }
        return text.toString();
    }

    private String buildLocationsDescString(Long projectId) {
        List<ShortDramaLocation> locs = locationMapper.selectList(
            new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId));
        if (locs.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaLocation l : locs) {
            sb.append("- ").append(l.getName()).append("：")
                .append(firstNotBlank(primaryLocationDescription(l), l.getSummary(), "无描述"))
                .append("\n");
        }
        return sb.toString();
    }

    private String buildCharactersInfoString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            sb.append("- ").append(c.getName())
                .append("（").append(firstNotBlank(c.getGender(), "未知")).append("，")
                .append(firstNotBlank(c.getAgeRange(), "未知")).append("，")
                .append(firstNotBlank(c.getRoleLevel(), "B")).append("级")
                .append("）\n");
        }
        return sb.toString();
    }

    private String buildCharactersAgeGenderString(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        if (chars.isEmpty()) return "暂无";
        StringBuilder sb = new StringBuilder();
        for (ShortDramaCharacter c : chars) {
            sb.append("- ").append(c.getName())
                .append("：").append(firstNotBlank(c.getGender(), "未知")).append("，")
                .append(firstNotBlank(c.getAgeRange(), "未知")).append("\n");
        }
        return sb.toString();
    }

    // ==================== 辅助方法 ====================

    private ChatModelVo validateAndGetModel(String modelName) {
        ChatModelVo modelVo = chatModelService.selectModelByName(modelName);
        if (modelVo == null) {
            throw new IllegalArgumentException("未找到模型配置: " + modelName);
        }
        return modelVo;
    }

    private ChatModelVo findChatModel() {
        org.ruoyi.common.chat.domain.bo.chat.ChatModelBo query = new org.ruoyi.common.chat.domain.bo.chat.ChatModelBo();
        query.setCategory("chat");
        query.setProviderCode("atlas");
        List<ChatModelVo> models = chatModelService.queryAvailableList(query);
        if (models != null && !models.isEmpty()) return models.get(0);
        throw new IllegalArgumentException("无可用聊天模型");
    }

    private AbstractChatService getChatService(ChatModelVo modelVo) {
        return chatServiceFactory.getOriginalService(shortDramaProviderCode(modelVo.getProviderCode(), modelVo.getModelName()));
    }

    public static String shortDramaProviderCode(String provider, String model) {
        return (org.ruoyi.enums.ChatModeType.OPEN_AI.getCode().equalsIgnoreCase(provider)
            || org.ruoyi.enums.ChatModeType.ATLAS.getCode().equalsIgnoreCase(provider))
            && StrUtil.containsIgnoreCase(model, "deepseek")
            ? org.ruoyi.enums.ChatModeType.DEEP_SEEK.getCode() : provider;
    }

    // ==================== 语音资产 ====================

    @Override
    public ShortDramaAudioVo saveAudio(ShortDramaAudioBo bo, Long userId) {
        validateProjectOwner(bo.getProjectId(), userId);
        ShortDramaAudio entity = MapstructUtils.convert(bo, ShortDramaAudio.class);
        if (entity.getAudioType() == null) entity.setAudioType("narration");
        if (entity.getId() == null) {
            entity.setId(IdUtil.getSnowflakeNextId());
            audioMapper.insert(entity);
        } else {
            ShortDramaAudio existing = audioMapper.selectById(entity.getId());
            if (existing == null || !userId.equals(projectMapper.selectById(existing.getProjectId()).getUserId())) {
                throw new IllegalArgumentException("语音资产不存在或无权限");
            }
            audioMapper.updateById(entity);
        }
        return MapstructUtils.convert(entity, ShortDramaAudioVo.class);
    }

    @Override
    public Boolean deleteAudio(Long audioId, Long userId) {
        ShortDramaAudio audio = audioMapper.selectById(audioId);
        if (audio == null) return false;
        validateProjectOwner(audio.getProjectId(), userId);
        return audioMapper.deleteById(audioId) > 0;
    }

    @Override
    public List<ShortDramaAudioVo> listAudios(Long projectId, Long userId) {
        validateProjectOwner(projectId, userId);
        return audioMapper.selectVoList(new LambdaQueryWrapper<ShortDramaAudio>()
            .eq(ShortDramaAudio::getProjectId, projectId)
            .orderByAsc(ShortDramaAudio::getId));
    }

    @Override
    public ShortDramaAudioVo generateAudio(Long audioId, String audioModel, Long userId) {
        ShortDramaAudio audio = audioMapper.selectById(audioId);
        if (audio == null) throw new IllegalArgumentException("语音资产不存在");
        validateProjectOwner(audio.getProjectId(), userId);
        if (StrUtil.isBlank(audio.getText())) throw new IllegalArgumentException("语音文案不能为空");
        ChatModelVo modelVo = chatModelService.selectModelByName(audioModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到语音模型配置: " + audioModel);
        if (audioModel.startsWith("suno/")) throw new IllegalArgumentException("Suno 是音乐模型，请使用音乐创作面板；对白配音需选择语音模型");
        if (!org.ruoyi.enums.ModelType.AUDIO.getKey().equals(modelVo.getCategory())) {
            throw new IllegalArgumentException("模型分类不是语音模型: " + audioModel);
        }

        // Role-bound Seed Audio samples are executable references; appearance.voice is only a description.
        var speech = ShortDramaCharacterVoiceService.MODEL.equals(audioModel) ? characterVoices.speech(audio)
            : new ShortDramaCharacterVoiceService.Speech(audio.getText(),List.of());
        List<java.util.Map<String, String>> references = speech.references();
        AudioContext ctx = AudioContext.builder()
            .chatModelVo(modelVo)
            .input(speech.text())
            .voice(ShortDramaCharacterVoiceService.MODEL.equals(audioModel) || StrUtil.isBlank(audio.getVoice()) ? null : audio.getVoice())
            .responseFormat("mp3")
            .references(references)
            .build();
        MediaGenerationResponse response = audioServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateSpeech(ctx);

        String audioUrl;
        Long audioOssId;
        if (response != null && StrUtil.isNotBlank(response.getB64Json())) {
            // OpenAI 同步模式：base64 → OSS
            byte[] audioBytes = java.util.Base64.getDecoder().decode(response.getB64Json());
            org.ruoyi.common.core.domain.dto.OssDTO uploaded = uploadAudioBytes(audioBytes);
            audioUrl = uploaded.getUrl();
            audioOssId = uploaded.getOssId();
        } else if (response != null && StrUtil.isNotBlank(response.getId())) {
            // Atlas 异步模式：轮询拿 URL，再下载转存 OSS（统一存储，避免 Atlas 链路过期）
            String predictionId = response.getId();
            if (StrUtil.isNotBlank(response.getUrl())) {
                audioUrl = response.getUrl();
                audioOssId = null;
            } else {
                MediaGenerationResponse polled = pollAudioDone(modelVo, predictionId);
                if (polled == null || StrUtil.isBlank(polled.getUrl())) {
                    throw new RuntimeException("语音异步生成超时或失败，predictionId=" + predictionId);
                }
                audioUrl = polled.getUrl();
                audioOssId = null;
            }
        } else {
            throw new RuntimeException("语音生成失败，模型未返回音频数据或任务ID");
        }
        audio.setAudioOssId(audioOssId);
        audio.setAudioUrl(audioUrl);
        audioMapper.updateById(audio);
        return MapstructUtils.convert(audio, ShortDramaAudioVo.class);
    }

    /** Atlas 异步音频轮询，累计不超过 3 分钟。 */
    private MediaGenerationResponse pollAudioDone(ChatModelVo modelVo, String predictionId) {
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(3);
        while (System.currentTimeMillis() < deadline) {
            MediaGenerationResponse resp = atlasPredictionService.retrieve(modelVo, predictionId);
            if (resp != null && ("completed".equals(resp.getStatus()) || "succeeded".equals(resp.getStatus()))
                && StrUtil.isNotBlank(resp.getUrl())) {
                return resp;
            }
            if (resp != null && "failed".equals(resp.getStatus())) {
                throw new RuntimeException("语音异步生成失败: " + resp.getRawResponse());
            }
            try { Thread.sleep(2000); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); return resp; }
        }
        return null;
    }

    private org.ruoyi.common.core.domain.dto.OssDTO uploadAudioBytes(byte[] bytes) {
        java.nio.file.Path tmp = null;
        try {
            tmp = java.nio.file.Files.createTempFile("short-drama-audio-", ".mp3");
            java.nio.file.Files.write(tmp, bytes);
            org.ruoyi.common.core.domain.dto.OssDTO uploaded = ossService.uploadFile(tmp.toFile());
            if (uploaded == null || uploaded.getOssId() == null) {
                throw new RuntimeException("语音文件上传对象存储失败");
            }
            return uploaded;
        } catch (java.io.IOException e) {
            throw new RuntimeException("语音文件写入失败: " + e.getMessage(), e);
        } finally {
            if (tmp != null) {
                try { java.nio.file.Files.deleteIfExists(tmp); } catch (java.io.IOException ignored) {}
            }
        }
    }

    private StreamingChatModel buildStreamingChatModel() {
        ChatModelVo modelVo = findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        return chatService.buildStreamingChatModel(modelVo, ShortDramaWritingRequest.forText(modelVo.getModelName(), null));
    }

    private ShortDramaProject validateProjectOwner(Long projectId, Long userId) {
        ShortDramaProject project = projectMapper.selectById(projectId);
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        return project;
    }

    private void clearAssets(Long projectId) {
        List<ShortDramaCharacter> chars = characterMapper.selectList(
            new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        for (ShortDramaCharacter c : chars) {
            characterAppearanceMapper.delete(new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                .eq(ShortDramaCharacterAppearance::getCharacterId, c.getId()));
        }
        characterMapper.delete(new LambdaQueryWrapper<ShortDramaCharacter>().eq(ShortDramaCharacter::getProjectId, projectId));
        locationMapper.delete(new LambdaQueryWrapper<ShortDramaLocation>().eq(ShortDramaLocation::getProjectId, projectId));
        audioMapper.delete(new LambdaQueryWrapper<ShortDramaAudio>().eq(ShortDramaAudio::getProjectId, projectId));
    }

    static void normalizeContinuityChain(List<StoryboardPanelData> panels) {
        normalizeContinuityChain(panels, false);
    }

    /** Native Phase 3 scene IDs name source-script budget groups, not visual spaces or montage stages. */
    static void normalizeContinuityChain(List<StoryboardPanelData> panels, boolean preserveSourceScenes) {
        if (preserveSourceScenes) {
            Integer previousSource = null;
            for (var panel : panels) {
                Integer source = panel.getSceneNumber();
                if (source == null || source < 1 || previousSource != null && source < previousSource)
                    throw new IllegalArgumentException("原文场身份须为正整数并按剧本顺序排列，不得按地点重新分配");
                previousSource = source;
            }
        }
        StoryboardPanelData previous = null;
        int sceneNumber = 1;
        int segmentNumber = 1;
        int segmentDuration = 0;
        String previousLocation = null;
        Integer previousRequestedScene = null;
        for (StoryboardPanelData current : panels) {
            Integer requestedScene = current.getSceneNumber();
            String currentLocation = firstNotBlank(current.getLocation(), "");
            boolean newSpace = previous != null && !java.util.Objects.equals(previousLocation, currentLocation);
            boolean sourceChanged = previous != null && requestedScene != null && previousRequestedScene != null
                && !requestedScene.equals(previousRequestedScene);
            boolean newScene = sourceChanged || !preserveSourceScenes && newSpace;
            boolean continuityReset = newScene || newSpace;
            if (newScene) {
                sceneNumber++;
                segmentNumber = 1;
                segmentDuration = 0;
            }
            if (preserveSourceScenes && newSpace && !newScene) {
                // Keep the budget scene; a new space gets a distinct segment and cannot inherit old blocking.
                segmentNumber++;
                segmentDuration = 0;
            }
            int duration = current.getDuration() != null && current.getDuration() > 0
                ? current.getDuration() : 6;
            current.setDuration(duration);
            previousRequestedScene = requestedScene;
            boolean requestedNewSegment = current.getSegmentNumber() != null && current.getSegmentNumber() > segmentNumber;
            if (previous != null && !continuityReset && (requestedNewSegment || segmentDuration + duration > 15)) {
                segmentNumber++;
                segmentDuration = 0;
            }
            current.setSceneNumber(preserveSourceScenes ? requestedScene : sceneNumber);
            current.setSegmentNumber(segmentNumber);
            segmentDuration += duration;
            previousLocation = currentLocation;
            if (previous == null) {
                current.setStartState(firstNotBlank(current.getStartState(), "新场景建立，按当前描述确定人物初始位置与姿态"));
                current.setNarrativeCause(firstNotBlank(current.getNarrativeCause(), "剧本开场建立人物、目标或冲突"));
                current.setBridgeIn(firstNotBlank(current.getBridgeIn(), "开场建立"));
            } else {
                String previousConsequence = firstNotBlank(previous.getStoryResult(), previous.getNextHook(), previous.getContinuityAction(), previous.getEndState());
                current.setNarrativeCause(firstNotBlank(current.getNarrativeCause(), previousConsequence));
                if (current.getSegmentNumber().equals(previous.getSegmentNumber()) && current.getSceneNumber().equals(previous.getSceneNumber())) {
                    current.setBridgeIn(firstNotBlank(current.getBridgeIn(), previous.getContinuityAction(), previous.getNextHook(), previous.getEndState()));
                } else {
                    current.setBridgeIn(firstNotBlank(current.getBridgeIn(), previous.getBridgeOut(), previous.getNextHook(), previous.getStoryResult()));
                    previous.setBridgeOut(firstNotBlank(previous.getBridgeOut(), current.getBridgeIn()));
                }
            }
            if (previous != null && !continuityReset) {
                String inheritedState = firstNotBlank(previous.getEndState(), previous.getDescription(), previous.getSourceText(), "");
                current.setStartState(firstNotBlank(current.getStartState(), inheritedState));
                if (StrUtil.isBlank(current.getSpatialAnchor())) {
                    current.setSpatialAnchor(previous.getSpatialAnchor());
                }
                if ((current.getPresentCharacters() == null)
                    && previous.getPresentCharacters() != null) {
                    current.setPresentCharacters(new ArrayList<>(previous.getPresentCharacters()));
                }
            } else {
                current.setStartState(firstNotBlank(current.getStartState(), "新场景建立：" + firstNotBlank(current.getLocation(), "新地点") + "，重新交代人物位置、朝向与环境"));
            }
            current.setEndState(firstNotBlank(current.getEndState(), current.getDescription(), current.getSourceText(), current.getStartState()));
            current.setContinuityAction(firstNotBlank(current.getContinuityAction(), "从当前结束姿态自然承接下一镜头"));
            current.setBeatType(firstNotBlank(current.getBeatType(), previous == null ? "setup" : "action"));
            current.setCharacterGoal(firstNotBlank(current.getCharacterGoal(), "回应当前剧情原因并推动局面变化"));
            current.setStoryAction(firstNotBlank(current.getStoryAction(), current.getDescription(), current.getSourceText(), "执行当前剧情动作"));
            current.setStoryResult(firstNotBlank(current.getStoryResult(), current.getEndState(), "当前行动形成可见结果"));
            current.setNextHook(firstNotBlank(current.getNextHook(), current.getContinuityAction(), "下一镜头回应当前结果"));
            current.setSegmentGoal(firstNotBlank(current.getSegmentGoal(), current.getCharacterGoal(), "完成当前连续剧情动作"));
            current.setSegmentResult(firstNotBlank(current.getSegmentResult(), current.getStoryResult(), current.getEndState()));
            current.setBridgeOut(firstNotBlank(current.getBridgeOut(), current.getNextHook(), current.getContinuityAction()));
            if (current.getPresentCharacters() == null) {
                List<String> present = new ArrayList<>();
                if (current.getCharacters() != null) {
                    for (CharacterRef ref : current.getCharacters()) {
                        if (StrUtil.isNotBlank(ref.getName())) present.add(ref.getName());
                    }
                }
                current.setPresentCharacters(present);
            }
            previous = current;
        }
    }

    private static String buildContinuityJson(StoryboardPanelData panel) {
        ObjectNode continuity = JsonNodeFactory.instance.objectNode();
        continuity.put("start_state", firstNotBlank(panel.getStartState(), ""));
        continuity.put("end_state", firstNotBlank(panel.getEndState(), ""));
        continuity.put("continuity_action", firstNotBlank(panel.getContinuityAction(), ""));
        continuity.put("spatial_anchor", firstNotBlank(panel.getSpatialAnchor(), ""));
        ArrayNode presentCharacters = continuity.putArray("present_characters");
        if (panel.getPresentCharacters() != null) panel.getPresentCharacters().forEach(presentCharacters::add);
        continuity.put("scene_number", panel.getSceneNumber() != null ? panel.getSceneNumber() : 1);
        continuity.put("segment_number", panel.getSegmentNumber() != null ? panel.getSegmentNumber() : 1);
        continuity.put("segment_goal", firstNotBlank(panel.getSegmentGoal(), ""));
        continuity.put("segment_result", firstNotBlank(panel.getSegmentResult(), ""));
        continuity.put("bridge_in", firstNotBlank(panel.getBridgeIn(), ""));
        continuity.put("bridge_out", firstNotBlank(panel.getBridgeOut(), ""));
        continuity.put("beat_type", firstNotBlank(panel.getBeatType(), "action"));
        continuity.put("narrative_cause", firstNotBlank(panel.getNarrativeCause(), ""));
        continuity.put("character_goal", firstNotBlank(panel.getCharacterGoal(), ""));
        continuity.put("story_action", firstNotBlank(panel.getStoryAction(), ""));
        continuity.put("story_result", firstNotBlank(panel.getStoryResult(), ""));
        continuity.put("next_hook", firstNotBlank(panel.getNextHook(), ""));
        if (panel.getShotDesign() != null) continuity.set("shot_design", panel.getShotDesign());
        if (panel.getPerformanceBeats() != null) continuity.set("performance_beats", panel.getPerformanceBeats());
        if (StrUtil.isNotBlank(panel.getBackgroundExtras())) {
            ShortDramaBackgroundExtras.parse(panel.getBackgroundExtras());
            continuity.put("background_extras", panel.getBackgroundExtras());
        }
        if (panel.getTiming() != null) continuity.set("timing", panel.getTiming());
        return continuity.toString();
    }

    private static void applyContinuityJson(StoryboardPanelData panel, String continuityJson) {
        if (StrUtil.isBlank(continuityJson)) return;
        try {
            JsonNode continuity = JsonUtils.parseObject(continuityJson, JsonNode.class);
            panel.setStartState(continuity.path("start_state").asText(null));
            panel.setEndState(continuity.path("end_state").asText(null));
            panel.setContinuityAction(continuity.path("continuity_action").asText(null));
            if (continuity.has("background_extras")) {
                JsonNode extras = continuity.path("background_extras");
                if (!extras.isNull() && !extras.isTextual()) throw new IllegalArgumentException("background_extras必须为文字字段");
                panel.setBackgroundExtras(extras.asText(null));
            }
            panel.setSpatialAnchor(continuity.path("spatial_anchor").asText(null));
            JsonNode present = continuity.path("present_characters");
            if (present.isArray()) {
                List<String> names = new ArrayList<>();
                present.forEach(node -> names.add(node.asText()));
                panel.setPresentCharacters(names);
            }
            if (continuity.has("scene_number")) panel.setSceneNumber(continuity.path("scene_number").asInt(1));
            if (continuity.has("segment_number")) panel.setSegmentNumber(continuity.path("segment_number").asInt(1));
            panel.setSegmentGoal(continuity.path("segment_goal").asText(null));
            panel.setSegmentResult(continuity.path("segment_result").asText(null));
            panel.setBridgeIn(continuity.path("bridge_in").asText(null));
            panel.setBridgeOut(continuity.path("bridge_out").asText(null));
            panel.setBeatType(continuity.path("beat_type").asText(null));
            panel.setNarrativeCause(continuity.path("narrative_cause").asText(null));
            panel.setCharacterGoal(continuity.path("character_goal").asText(null));
            panel.setStoryAction(continuity.path("story_action").asText(null));
            panel.setStoryResult(continuity.path("story_result").asText(null));
            panel.setNextHook(continuity.path("next_hook").asText(null));
            if (continuity.has("shot_design")) panel.setShotDesign(continuity.get("shot_design"));
            if (continuity.has("timing")) panel.setTiming(continuity.get("timing"));
            if (continuity.has("performance_beats")) panel.setPerformanceBeats(continuity.get("performance_beats"));
        } catch (Exception e) {
            log.debug("解析分镜连续性失败: {}", e.getMessage());
        }
    }

    private void appendContinuityPrompt(StringBuilder prompt, ShortDramaStoryboard storyboard) {
        ShortDramaStoryboard previous = storyboardMapper.selectOne(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getProjectId, storyboard.getProjectId())
            .lt(ShortDramaStoryboard::getSceneNo, storyboard.getSceneNo())
            .orderByDesc(ShortDramaStoryboard::getSceneNo)
            .last("limit 1"));
        if (previous != null && StrUtil.isNotBlank(previous.getContinuityJson())) {
            prompt.append("[上一镜头结束状态] ").append(previous.getContinuityJson()).append("\n");
        }
        if (StrUtil.isNotBlank(storyboard.getContinuityJson())) {
            prompt.append("[当前镜头连续性与剧情节拍] ").append(storyboard.getContinuityJson()).append("\n");
            prompt.append("[剧情执行要求] 镜头必须从narrative_cause开始，拍出story_action，并以story_result结束；next_hook必须自然引向下一镜头。\n");
        }
        if (previous != null && previous.getLocationName() != null
            && previous.getLocationName().equals(storyboard.getLocationName())) {
            prompt.append("[强制承接] 当前视频第一帧必须继承上一镜头最后状态，保持人物位置、朝向、姿态、道具、光线和运动方向连续。\n");
        } else if (previous != null) {
            prompt.append("[场景切换] 使用明确的新场景建立画面，不要伪装成上一镜头的连续动作。\n");
        }
    }

    private List<StoryboardPanelData> toPanelDataList(List<ShortDramaStoryboard> storyboards) {
        List<StoryboardPanelData> panels = new ArrayList<>();
        for (ShortDramaStoryboard sb : storyboards) {
            StoryboardPanelData panel = new StoryboardPanelData();
            panel.setPanelNumber(sb.getSceneNo());
            panel.setDescription(sb.getSceneText());
            panel.setSceneType(sb.getSceneType());
            panel.setLocation(sb.getLocationName());
            panel.setSourceText(sb.getSourceText());
            if (StrUtil.isNotBlank(sb.getCharactersJson())) {
                try {
                    panel.setCharacters(JsonUtils.parseArray(sb.getCharactersJson(), CharacterRef.class));
                } catch (Exception ignored) {}
            }
            panels.add(panel);
        }
        return panels;
    }

    private List<StoryboardPanelData> fallbackPanels(ShortDramaScript script) {
        String text = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        List<String> chunks = splitToChunks(text);
        List<StoryboardPanelData> panels = new ArrayList<>();
        int no = 1;
        for (String chunk : chunks) {
            StoryboardPanelData panel = new StoryboardPanelData();
            panel.setPanelNumber(no);
            panel.setDescription(chunk);
            panel.setSceneType("daily");
            panel.setSourceText(chunk);
            panels.add(panel);
            no++;
        }
        return panels;
    }

    private static List<String> splitToChunks(String text) {
        if (StrUtil.isBlank(text)) return List.of();
        String normalized = text.replace("\r\n", "\n").replace("\r", "\n");
        String[] lines = normalized.split("\n+");
        List<String> chunks = new ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            String[] sentences = trimmed.split("(?<=[。！？!?；;])");
            for (String sentence : sentences) {
                String item = sentence.trim();
                if (!item.isEmpty()) chunks.add(item);
            }
        }
        if (chunks.isEmpty()) chunks.add(normalized.trim());
        return chunks.stream().filter(StrUtil::isNotBlank).limit(12).toList();
    }

    // ==================== JSON 解析 ====================

    private static String extractJson(String response) {
        if (StrUtil.isBlank(response)) return null;
        String text = response.trim();

        // 1. 剥离 markdown 代码块
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?\\s*", "");
            text = text.replaceFirst("\\s*```$", "");
        }

        // 2. 定位 JSON 边界
        int start = text.indexOf('{');
        int arrStart = text.indexOf('[');
        if (start >= 0 && (arrStart < 0 || start < arrStart)) {
            int end = text.lastIndexOf('}');
            if (end > start) return repairJson(text.substring(start, end + 1));
        }
        if (arrStart >= 0) {
            int end = text.lastIndexOf(']');
            if (end > arrStart) return repairJson(text.substring(arrStart, end + 1));
        }
        return repairJson(text);
    }

    /**
     * 修复 LLM 返回 JSON 的常见问题：
     * - 「」被误用作 JSON 结构引号（旧 prompt 遗留的副作用）
     * - 尾逗号
     * - 字符串值中未转义的换行符
     */
    private static String repairJson(String json) {
        if (StrUtil.isBlank(json)) return json;
        // 先试原样
        if (JsonUtils.isJson(json) || JsonUtils.isJsonArray(json)) return json;
        String fixed = json;
        // 修复：「」→ "（模型照字面执行了旧 prompt 指令）
        if (fixed.contains("「") || fixed.contains("」")) {
            fixed = fixed.replace("「", "\"").replace("」", "\"");
        }
        // 修复：尾逗号
        fixed = fixed.replaceAll(",(\\s*[}\\]])", "$1");
        // 修复：JSON 字符串值中未转义的实际换行符（替换为空格，避免破坏 JSON 结构）
        if (fixed.contains("\n") || fixed.contains("\r")) {
            fixed = fixed.replace("\r\n", " ").replace("\r", " ").replace("\n", " ");
        }
        return fixed;
    }

    private static <T> T parseJson(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) return null;
        try {
            return JsonUtils.parseObject(json, clazz);
        } catch (Exception e) {
            // 原始解析失败时用修复版重试一次
            String repaired = repairJson(json);
            if (!repaired.equals(json)) {
                try {
                    return JsonUtils.parseObject(repaired, clazz);
                } catch (Exception ignored) {}
            }
            log.warn("JSON解析失败[{}]: {} raw={}", clazz.getSimpleName(), e.getMessage(),
                json.length() > 300 ? json.substring(0, 300) + "..." : json);
            return null;
        }
    }

    private static <T> List<T> parseJsonArray(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) return null;
        try {
            if (json.trim().startsWith("[")) {
                return JsonUtils.parseArray(json, clazz);
            }
        } catch (Exception e) {
            // 原始解析失败时用修复版重试一次
            String repaired = repairJson(json);
            if (!repaired.equals(json)) {
                try {
                    return JsonUtils.parseArray(repaired, clazz);
                } catch (Exception ignored) {}
            }
            log.warn("JSON数组解析失败[{}]: {} raw={}", clazz.getSimpleName(), e.getMessage(),
                json.length() > 300 ? json.substring(0, 300) + "..." : json);
        }
        return null;
    }

    /** 从 LLM 响应中提取 JSON 并解析为 JsonNode（保留完整字段，避免 POJO 映射丢失） */
    private static JsonNode parseJsonNode(String json) {
        if (StrUtil.isBlank(json)) return null;
        try {
            return JsonUtils.parseObject(json.trim(), JsonNode.class);
        } catch (Exception e) {
            String repaired = repairJson(json);
            if (!repaired.equals(json)) {
                try { return JsonUtils.parseObject(repaired, JsonNode.class); } catch (Exception ignored) {}
            }
            log.warn("JsonNode解析失败: {}", e.getMessage());
            return null;
        }
    }

    private List<AssetGeneratedCharacter> parseCharacterList(String response) {
        String json = extractJson(response);
        if (StrUtil.isBlank(json)) return List.of();
        try {
            if (json.trim().startsWith("[")) {
                return JsonUtils.parseArray(json, AssetGeneratedCharacter.class);
            }
            AssetGeneratedCharacterList obj = JsonUtils.parseObject(json, AssetGeneratedCharacterList.class);
            return obj != null && obj.getNew_characters() != null ? obj.getNew_characters() : List.of();
        } catch (Exception e) {
            log.warn("角色提取JSON解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<AssetGeneratedLocation> parseLocationList(String response) {
        String json = extractJson(response);
        if (StrUtil.isBlank(json)) return List.of();
        try {
            if (json.trim().startsWith("[")) {
                return JsonUtils.parseArray(json, AssetGeneratedLocation.class);
            }
            AssetGeneratedLocationList obj = JsonUtils.parseObject(json, AssetGeneratedLocationList.class);
            return obj != null && obj.getLocations() != null ? obj.getLocations() : List.of();
        } catch (Exception e) {
            log.warn("场景提取JSON解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<StoryboardPanelData> parsePanelList(String response) {
        var panels = parseRawPanelList(response);
        if (panels != null) ShortDramaShotDesign.expandDeltas(panels);
        return panels;
    }

    private List<StoryboardPanelData> parseRawPanelList(String response) {
        String json = extractJson(response);
        if (StrUtil.isBlank(json)) return null;
        try {
            return ShortDramaPlanResponse.parse(json);
        } catch (ShortDramaShotDesign.InvalidDesign e) {
            throw e;
        } catch (Exception e) {
            log.warn("分镜规划JSON解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 内部 DTO ====================

    @Data
    public static class StoryboardPanelData {
        @JsonProperty("background_extras")
        private String backgroundExtras;
        @JsonProperty("panel_number")
        private Integer panelNumber;
        @JsonProperty("scene_number")
        private Integer sceneNumber;
        @JsonProperty("segment_number")
        private Integer segmentNumber;
        @JsonProperty("segment_goal")
        private String segmentGoal;
        @JsonProperty("segment_result")
        private String segmentResult;
        @JsonProperty("bridge_in")
        private String bridgeIn;
        @JsonProperty("bridge_out")
        private String bridgeOut;
        private String description;
        private List<CharacterRef> characters;
        private String location;
        @JsonProperty("scene_type")
        private String sceneType;
        @JsonProperty("source_text")
        private String sourceText;
        private Integer duration;
        private JsonNode timing;
        @JsonProperty("start_state")
        private String startState;
        @JsonProperty("end_state")
        private String endState;
        @JsonProperty("continuity_action")
        private String continuityAction;
        @JsonProperty("spatial_anchor")
        private String spatialAnchor;
        @JsonProperty("present_characters")
        private List<String> presentCharacters;
        @JsonProperty("beat_type")
        private String beatType;
        @JsonProperty("narrative_cause")
        private String narrativeCause;
        @JsonProperty("character_goal")
        private String characterGoal;
        @JsonProperty("story_action")
        private String storyAction;
        @JsonProperty("story_result")
        private String storyResult;
        @JsonProperty("next_hook")
        private String nextHook;
        @JsonProperty("performance_beats")
        private JsonNode performanceBeats;
        @JsonProperty("shot_design")
        private JsonNode shotDesign;
        // Phase 4 fills:
        private String photographyRules;
        // Phase 5 fills:
        private String actingNotes;
        // Phase 6 fills:
        @JsonProperty("shot_type")
        private String shotType;
        @JsonProperty("camera_move")
        private String cameraMove;
        @JsonProperty("video_prompt")
        private String videoPrompt;
        @JsonProperty("image_prompt")
        private String imagePrompt;
        private String sceneTitle;
    }

    @Data
    public static class CharacterRef {
        private String name;
        private String appearance;
        private String slot;

        /**
         * Planning models occasionally wrap the single appearance identifier in
         * an array. Accept that harmless shape drift while keeping ambiguous
         * multi-value bindings invalid.
         */
        @JsonSetter("appearance")
        public void setAppearance(JsonNode value) {
            if (value == null || value.isNull()) {
                appearance = null;
                return;
            }
            if (value.isTextual() || value.isNumber()) {
                appearance = value.asText();
                return;
            }
            if (value.isArray() && value.size() == 1
                && (value.get(0).isTextual() || value.get(0).isNumber())) {
                appearance = value.get(0).asText();
                return;
            }
            throw new IllegalArgumentException("characters.appearance必须是单个形象标识");
        }

        public void setAppearance(String value) {
            appearance = value;
        }
    }

    @Data
    static class ActingDirectionResult {
        @JsonProperty("panel_number")
        private Integer panelNumber;
        private JsonNode characters;
    }

    @Data
    static class StoryboardDetailResult {
        @JsonProperty("background_extras")
        private String backgroundExtras;
        @JsonProperty("panel_number")
        private Integer panelNumber;
        @JsonProperty("scene_number")
        private Integer sceneNumber;
        @JsonProperty("segment_number")
        private Integer segmentNumber;
        @JsonProperty("segment_goal")
        private String segmentGoal;
        @JsonProperty("segment_result")
        private String segmentResult;
        @JsonProperty("bridge_in")
        private String bridgeIn;
        @JsonProperty("bridge_out")
        private String bridgeOut;
        @JsonProperty("shot_type")
        private String shotType;
        @JsonProperty("camera_move")
        private String cameraMove;
        private String description;
        @JsonProperty("video_prompt")
        private String videoPrompt;
        @JsonProperty("image_prompt")
        private String imagePrompt;
        private String sceneTitle;
        @JsonProperty("start_state")
        private String startState;
        @JsonProperty("end_state")
        private String endState;
        @JsonProperty("continuity_action")
        private String continuityAction;
        @JsonProperty("spatial_anchor")
        private String spatialAnchor;
        @JsonProperty("present_characters")
        private List<String> presentCharacters;
        @JsonProperty("beat_type")
        private String beatType;
        @JsonProperty("narrative_cause")
        private String narrativeCause;
        @JsonProperty("character_goal")
        private String characterGoal;
        @JsonProperty("story_action")
        private String storyAction;
        @JsonProperty("story_result")
        private String storyResult;
        @JsonProperty("next_hook")
        private String nextHook;
    }

    @Data
    private static class AssetGeneratedCharacter {
        private String name;
        private String aliases;
        private String introduction;
        private String roleLevel;
        private String gender;
        private String ageRange;
        private String archetype;
        private String personalityTags;
        private Integer costumeTier;
        private String visualKeywords;
        private String visualDescription;
        private String suggestedColors;
        private String primaryIdentifier;
    }

    @Data
    private static class AssetGeneratedCharacterList {
        private List<AssetGeneratedCharacter> new_characters;
    }

    @Data
    private static class AssetCharacterVisualResult {
        private List<AssetCharVisual> characters;
    }

    @Data
    private static class AssetCharVisual {
        private String name;
        private List<AssetAppearanceDesc> appearances;
    }

    @Data
    private static class AssetAppearanceDesc {
        private Integer id;
        private List<String> descriptions;
        private String change_reason;
    }

    @Data
    private static class AssetGeneratedLocation {
        private String name;
        private String summary;
        private Boolean hasCrowd;
        private String crowdDescription;
        private List<String> availableSlots;
        private List<String> descriptions;
    }

    @Data
    private static class AssetGeneratedLocationList {
        private List<AssetGeneratedLocation> locations;
    }

    /** 查找项目的视觉风格 prompt 后缀，找不到返回空字符串 */
    private String artStyleSuffix(Long projectId) {
        ShortDramaProject project = projectMapper.selectById(projectId);
        if (project == null) return "";
        return ShortDramaImageConstants.artStylePrompt(skillCatalog.effectiveArtStyle(project)) + projectDirection(projectId);
    }

    /** All character generation and editing paths share style-aware identity reference rules. */
    private String buildCharacterPrompt(String basePrompt, Long projectId) {
        return buildCharacterPrompt(basePrompt, projectId, null, null);
    }

    private String buildCharacterPrompt(String basePrompt, Long projectId, String referencePurpose, String revisionRequirements) {
        ShortDramaProject project = projectMapper.selectById(projectId);
        return ShortDramaCharacterArtPrompt.reference(basePrompt, effectiveArtStyle(project), referencePurpose,
            revisionRequirements, assetWorldbuilding(projectId)) + selectedVisual(project, "aesthetic");
    }

    private String buildLocationPrompt(String name, String description, Long projectId) {
        return buildLocationPrompt(name, description, projectId, null);
    }

    private String buildLocationPrompt(String name, String description, Long projectId, String revisionRequirements) {
        ShortDramaProject project = projectMapper.selectById(projectId);
        return ShortDramaLocationArtPrompt.reference(name, description, effectiveArtStyle(project), assetWorldbuilding(projectId), revisionRequirements)
            + selectedVisual(project, "aesthetic");
    }

    private String effectiveArtStyle(ShortDramaProject project) {
        if (skillCatalog != null) return skillCatalog.effectiveArtStyle(project);
        return project == null ? null : project.getArtStyle();
    }

    private String selectedVisual(ShortDramaProject project, String type) {
        return skillCatalog == null ? "" : skillCatalog.selectedVisual(project, type);
    }

    private String projectDirection(Long projectId) {
        var project = projectMapper.selectById(projectId);
        for (var frozen : planningSkillSnapshots.values()) if (Objects.equals(projectId, frozen.projectId())) {
            skillCatalog.verify(frozen, project);
            return frozen.direction();
        }
        return skillCatalog.selected(project, "aesthetic") + skillCatalog.selected(project, "director");
    }
    private ShortDramaProject ideaProject(ShortDramaIdeaBo idea) {
        var project = new ShortDramaProject(); project.setAestheticSkillName(idea.getAestheticSkillName()); project.setDirectorSkillName(idea.getDirectorSkillName());
        project.setArtStyle(idea.getArtStyle()); return project;
    }
    private void validateSkillBindings(ShortDramaProject project) {
        if (StrUtil.isNotBlank(project.getAestheticSkillName())) {
            var skill = skillCatalog.requireEnabled(project.getAestheticSkillName(), "aesthetic");
            if (StrUtil.isNotBlank(skill.artStyle())) project.setArtStyle(skill.artStyle());
        }
        if (StrUtil.isNotBlank(project.getDirectorSkillName())) skillCatalog.requireEnabled(project.getDirectorSkillName(), "director");
    }

    /** Assets belong to the latest saved screenplay for this project; no global or previous-project setting is reused. */
    private String assetWorldbuilding(Long projectId) {
        ShortDramaScript script = scriptMapper.selectOne(new LambdaQueryWrapper<ShortDramaScript>()
            .eq(ShortDramaScript::getProjectId, projectId).orderByDesc(ShortDramaScript::getId).last("limit 1"));
        return script == null ? "" : ShortDramaScriptPreparation.worldContext(script);
    }

    private String projectAspectRatio(Long projectId) {
        ShortDramaProject project = projectMapper.selectById(projectId);
        return project == null ? "9:16" : normalizeAspectRatio(project.getComposeAspectRatio());
    }

    private static String normalizeAspectRatio(String aspectRatio) {
        return switch (firstNotBlank(aspectRatio, "9:16")) {
            case "16:9", "4:3", "1:1", "3:4", "9:16", "21:9" -> aspectRatio;
            default -> "9:16";
        };
    }

    /** 根据场景类型返回默认时长（秒），取值在各类型推荐范围的中位 */
    private static int defaultDurationForSceneType(String sceneType) {
        if (sceneType == null) return 6;
        return switch (sceneType) {
            case "action" -> 5;
            case "daily" -> 6;
            case "suspense" -> 7;
            case "emotion" -> 8;
            case "epic" -> 11;
            default -> 6;
        };
    }

    /**
     * 校验用户传入的图生图参考图。仅允许公开 HTTP(S) URL 或图片 data URL，
     * 避免把任意协议和本地文件路径转交给图片供应商。
     */
    private static String validateReferenceImageUrl(String referenceImageUrl) {
        if (StrUtil.isBlank(referenceImageUrl)) return null;
        String value = referenceImageUrl.trim();
        if (value.length() > 10_000) {
            throw new IllegalArgumentException("参考图地址过长");
        }
        if (value.startsWith("data:image/")) return value;
        try {
            java.net.URI uri = java.net.URI.create(value);
            String scheme = uri.getScheme();
            if (("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                && StrUtil.isNotBlank(uri.getHost())) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            // 统一在下方返回面向调用方的错误。
        }
        throw new IllegalArgumentException("参考图仅支持 HTTP(S) URL 或 data:image URL");
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) return value;
        }
        return "";
    }
}
