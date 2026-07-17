package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    private final ShortDramaProjectMapper projectMapper;
    private final ShortDramaScriptMapper scriptMapper;
    private final ShortDramaStoryboardMapper storyboardMapper;
    private final ShortDramaCharacterMapper characterMapper;
    private final ShortDramaCharacterAppearanceMapper characterAppearanceMapper;
    private final ShortDramaLocationMapper locationMapper;
    private final ShortDramaAudioMapper audioMapper;
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

    // ==================== 核心：六阶段流水线 ====================

    @Override
    public ShortDramaDetailVo createFromIdea(ShortDramaIdeaBo bo, Long userId) {
        ChatModelVo modelVo = validateAndGetModel(bo.getModel());
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo);

        // Phase 1: 剧本打磨
        ShortDramaScriptResult polishResult = executePhase1_ScriptPolish(chatModel, bo);
        ShortDramaProject project = buildAndInsertProject(userId, polishResult, bo);
        ShortDramaScript script = buildAndInsertScript(project.getId(), polishResult);

        // Phase 2: 资产分析（角色+场景提取）
        executePhase2_AssetAnalysis(chatModel, project.getId(), script);

        // Phase 3-6: 分镜流水线
        List<StoryboardPanelData> panels = executeStoryboardPipeline(chatModel, script, project.getId());

        return getDetail(project.getId(), userId);
    }

    @Override
    public SseEmitter createFromIdeaStream(ShortDramaIdeaBo bo, Long userId) {
        SseEmitter emitter = new SseEmitter(1_800_000L);
        AtomicBoolean emitterActive = new AtomicBoolean(true);
        activeEmitters.put(emitter, emitterActive);
        emitter.onCompletion(() -> closeEmitter(emitter));
        emitter.onTimeout(() -> closeEmitter(emitter));
        emitter.onError(error -> closeEmitter(emitter));

        CompletableFuture.runAsync(() -> {
            Long projectId = null;
            try {
                ChatModelVo modelVo = validateAndGetModel(bo.getModel());
                AbstractChatService chatService = getChatService(modelVo);
                ChatModel chatModel = chatService.buildChatModel(modelVo);
                StreamingChatModel streamingModel = chatService.buildStreamingChatModel(modelVo, new ChatRequest());

                // Phase 1: 单次流式调用（JSON 元信息 → 分隔符 → 剧本正文逐字推送）
                emit(emitter, "polish", "running", "正在生成故事大纲...");
                ShortDramaScriptResult polishResult = executePhase1_Streaming(streamingModel, bo, emitter);
                ShortDramaProject project = buildAndInsertProject(userId, polishResult, bo);
                projectId = project.getId();
                ShortDramaScript script = buildAndInsertScript(projectId, polishResult);
                emit(emitter, "polish", "done", "剧本打磨完成");

                // Phase 2: 资产分析（角色 + 场景 并发，流式输出）
                emit(emitter, "assets", "running", "正在分析角色和场景...");
                StreamingChatModel assetsStreamModel = chatService.buildStreamingChatModel(modelVo, new ChatRequest());
                executePhase2_AssetAnalysis(chatModel, assetsStreamModel, projectId, script, emitter);
                long charCount = characterMapper.selectCount(new LambdaQueryWrapper<ShortDramaCharacter>()
                    .eq(ShortDramaCharacter::getProjectId, projectId));
                long locCount = locationMapper.selectCount(new LambdaQueryWrapper<ShortDramaLocation>()
                    .eq(ShortDramaLocation::getProjectId, projectId));
                emit(emitter, "assets", "done", "提取了 " + charCount + " 个角色、" + locCount + " 个场景");

                // Phase 3-6: 分镜流水线（各子阶段内部创建 StreamingChatModel 实现流式输出）
                List<StoryboardPanelData> panels = executeStoryboardPipeline(chatModel, script, projectId, emitter);
                emit(emitter, "storyboard", "done", "分镜完成，共 " + panels.size() + " 个镜头");

                // 完成
                sendEmitterEvent(emitter, SseEmitter.event()
                    .name("complete")
                    .data("{\"projectId\":\"" + projectId + "\"}"));
                completeEmitter(emitter);
            } catch (Exception e) {
                log.error("流式创建短剧失败", e);
                String errorData = projectId != null
                    ? "{\"message\":\"" + escapeJson(e.getMessage()) + "\",\"projectId\":\"" + projectId + "\"}"
                    : "{\"message\":\"" + escapeJson(e.getMessage()) + "\"}";
                sendEmitterEvent(emitter, SseEmitter.event().name("error").data(errorData));
                completeEmitterWithError(emitter, e);
            }
        });

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
        sendEmitterEvent(emitter, SseEmitter.event().name("phase").data(data));
    }

    private void emitStream(SseEmitter emitter, String phase, String text) {
        String data = "{\"phase\":\"" + phase + "\",\"text\":\"" + escapeJson(text) + "\"}";
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
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private ShortDramaProject buildAndInsertProject(Long userId, ShortDramaScriptResult polishResult, ShortDramaIdeaBo bo) {
        ShortDramaProject project = new ShortDramaProject();
        project.setId(IdUtil.getSnowflakeNextId());
        project.setUserId(userId);
        project.setProjectName(firstNotBlank(bo.getProjectName(), polishResult.getProjectName(), "短剧项目"));
        project.setDescription(firstNotBlank(polishResult.getDescription(), ""));
        project.setArtStyle(firstNotBlank(bo.getArtStyle(), ShortDramaImageConstants.DEFAULT_ART_STYLE));
        project.setComposeAspectRatio(normalizeAspectRatio(bo.getAspectRatio()));
        project.setStatus("draft");
        projectMapper.insert(project);
        return project;
    }

    private ShortDramaScript buildAndInsertScript(Long projectId, ShortDramaScriptResult polishResult) {
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

    private List<StoryboardPanelData> executeStoryboardPipeline(ChatModel chatModel, ShortDramaScript script, Long projectId) {
        return executeStoryboardPipeline(chatModel, script, projectId, null);
    }

    private List<StoryboardPanelData> executeStoryboardPipeline(ChatModel chatModel, ShortDramaScript script, Long projectId, SseEmitter emitter) {
        // Phase 3: 分镜规划（流式输出由 executePhase3_StoryboardPlan 内部处理）
        if (emitter != null) emit(emitter, "storyboard_plan", "running", "正在规划分镜镜头...");
        List<StoryboardPanelData> panels = executePhase3_StoryboardPlan(chatModel, script, projectId, emitter);
        // Phase 3 的成功/失败由子方法内部 emit（因为流式输出的 done/error 在 streamingChat 回调中处理）
        if (emitter != null && !panels.isEmpty()) emit(emitter, "storyboard_plan", "done", "分镜规划完成，共 " + panels.size() + " 个镜头");

        if (!panels.isEmpty()) {
            List<StoryboardPanelData> planPanels = panels;

            // 摄影规则与表演指导使用确定性本地规则生成，避免为整组镜头额外发起两次大模型请求。
            if (emitter != null) emit(emitter, "photography", "running", "正在生成摄影规则...");
            List<JsonNode> photographyRules = buildLocalPhotographyRules(planPanels);
            if (emitter != null) emit(emitter, "photography", "done", "摄影规则生成完成");

            if (emitter != null) emit(emitter, "acting", "running", "正在生成表演指导...");
            List<ActingDirectionResult> actingDirections = buildLocalActingDirections(planPanels);
            if (emitter != null) emit(emitter, "acting", "done", "表演指导生成完成");

            // 分镜细化保留一次模型调用，统一生成景别、运镜和时长匹配的视频/图片提示词。
            if (emitter != null) emit(emitter, "storyboard_detail", "running", "正在细化分镜详情...");
            executePhase6_StoryboardDetail(chatModel, planPanels, projectId, emitter);
            normalizeContinuityChain(planPanels);
            panels = mergePanelsWithRules(planPanels, photographyRules, actingDirections);
        }
        persistStoryboards(projectId, script.getId(), panels, script);
        return panels;
    }

    @Override
    public ShortDramaDetailVo polishScript(Long projectId, Long userId) {
        ShortDramaProject project = validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectOne(new LambdaQueryWrapper<ShortDramaScript>()
            .eq(ShortDramaScript::getProjectId, projectId)
            .orderByDesc(ShortDramaScript::getId).last("limit 1"));
        String idea = script != null ? firstNotBlank(script.getScriptText(), script.getOutlineText(), project.getDescription()) : project.getDescription();
        ChatModelVo modelVo = findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo);

        ShortDramaIdeaBo bo = new ShortDramaIdeaBo();
        bo.setIdea(idea);
        bo.setProjectName(project.getProjectName());
        ShortDramaScriptResult result = executePhase1_ScriptPolish(chatModel, bo);

        if (script == null) {
            script = new ShortDramaScript();
            script.setId(IdUtil.getSnowflakeNextId());
            script.setProjectId(projectId);
            script.setSourceType("llm");
        }
        project.setProjectName(firstNotBlank(result.getProjectName(), project.getProjectName()));
        project.setDescription(firstNotBlank(result.getDescription(), project.getDescription()));
        projectMapper.updateById(project);
        script.setScriptName(firstNotBlank(result.getScriptName(), script.getScriptName()));
        script.setOutlineText(firstNotBlank(result.getOutlineText(), script.getOutlineText()));
        script.setScriptText(firstNotBlank(result.getScriptText(), script.getScriptText()));
        script.setTone(firstNotBlank(result.getTone(), script.getTone()));
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
        ShortDramaScript entity = MapstructUtils.convert(bo, ShortDramaScript.class);
        entity.setSourceType(StrUtil.blankToDefault(entity.getSourceType(), "manual"));
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
        // 删除旧分镜
        storyboardMapper.delete(new LambdaQueryWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getScriptId, scriptId));

        ChatModelVo modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo);

        List<StoryboardPanelData> panels = executePhase3_StoryboardPlan(chatModel, script, projectId);
        if (panels.isEmpty()) {
            panels = fallbackPanels(script);
        }
        if (!panels.isEmpty()) {
            List<JsonNode> photographyRules = buildLocalPhotographyRules(panels);
            List<ActingDirectionResult> actingDirections = buildLocalActingDirections(panels);
            executePhase6_StoryboardDetail(chatModel, panels, projectId);
            normalizeContinuityChain(panels);
            panels = mergePanelsWithRules(panels, photographyRules, actingDirections);
        }
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
    public SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId) {
        SseEmitter emitter = new SseEmitter(1_800_000L);
        AtomicBoolean emitterActive = new AtomicBoolean(true);
        activeEmitters.put(emitter, emitterActive);
        emitter.onCompletion(() -> closeEmitter(emitter));
        emitter.onTimeout(() -> closeEmitter(emitter));
        emitter.onError(error -> closeEmitter(emitter));

        CompletableFuture.runAsync(() -> {
            if (!beginStoryboardGeneration(scriptId)) {
                sendEmitterEvent(emitter, SseEmitter.event().name("error")
                    .data("{\"message\":\"该剧本正在生成分镜，请勿重复提交\"}"));
                completeEmitter(emitter);
                return;
            }
            try {
                validateProjectOwner(projectId, userId);
                ShortDramaScript script = scriptMapper.selectById(scriptId);
                if (script == null || !projectId.equals(script.getProjectId())) {
                    throw new IllegalArgumentException("剧本不存在");
                }

                videoComposeService.invalidateComposition(projectId);
                storyboardMapper.delete(new LambdaQueryWrapper<ShortDramaStoryboard>()
                    .eq(ShortDramaStoryboard::getScriptId, scriptId));

                ChatModelVo modelVo = StrUtil.isNotBlank(model) ? validateAndGetModel(model) : findChatModel();
                AbstractChatService chatService = getChatService(modelVo);
                ChatModel chatModel = chatService.buildChatModel(modelVo);
                StreamingChatModel streamingModel = chatService.buildStreamingChatModel(modelVo, new ChatRequest());

                log.info("开始流式生成分镜: projectId={}, scriptId={}, model={}", projectId, scriptId, modelVo.getModelName());
                emit(emitter, "storyboard_plan", "running", "正在规划分镜镜头，模型开始输出后会实时显示...");
                List<StoryboardPanelData> panels = executePhase3_StoryboardPlan(chatModel, streamingModel, script, projectId, emitter);
                if (panels.isEmpty()) {
                    throw new IllegalStateException("分镜规划失败，模型未返回有效镜头");
                }
                emit(emitter, "storyboard_plan", "done", "分镜规划完成，共 " + panels.size() + " 个镜头");

                emit(emitter, "photography", "running", "正在生成摄影规则...");
                List<JsonNode> photographyRules = buildLocalPhotographyRules(panels);
                emit(emitter, "photography", "done", "摄影规则生成完成");

                emit(emitter, "acting", "running", "正在生成表演指导...");
                List<ActingDirectionResult> actingDirections = buildLocalActingDirections(panels);
                emit(emitter, "acting", "done", "表演指导生成完成");

                emit(emitter, "storyboard_detail", "running", "正在细化镜头提示词和时长...");
                executePhase6_StoryboardDetail(chatModel, streamingModel, panels, projectId, emitter);
                normalizeContinuityChain(panels);
                panels = mergePanelsWithRules(panels, photographyRules, actingDirections);
                List<ShortDramaStoryboardVo> storyboards = persistStoryboards(projectId, scriptId, panels, script);

                log.info("流式生成分镜完成: projectId={}, count={}", projectId, storyboards.size());
                sendEmitterEvent(emitter, SseEmitter.event().name("complete")
                    .data("{\"projectId\":\"" + projectId + "\",\"count\":" + storyboards.size() + "}"));
                completeEmitter(emitter);
            } catch (Exception e) {
                log.error("流式生成分镜失败: projectId={}, scriptId={}", projectId, scriptId, e);
                sendEmitterEvent(emitter, SseEmitter.event().name("error")
                    .data("{\"message\":\"" + escapeJson(e.getMessage()) + "\"}"));
                completeEmitterWithError(emitter, e);
            } finally {
                endStoryboardGeneration(scriptId);
            }
        });
        return emitter;
    }

    private boolean beginStoryboardGeneration(Long scriptId) {
        return storyboardGenerationStates.putIfAbsent(scriptId, new AtomicBoolean(true)) == null;
    }

    private void endStoryboardGeneration(Long scriptId) {
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
        ChatModel chatModel = getChatService(modelVo).buildChatModel(modelVo);
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
        ChatModel chatModel = getChatService(modelVo).buildChatModel(modelVo);
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

    @Override
    public ShortDramaStoryboardVo saveStoryboard(ShortDramaStoryboardBo bo, Long userId) {
        ShortDramaStoryboard entity = MapstructUtils.convert(bo, ShortDramaStoryboard.class);
        ShortDramaProject project = projectMapper.selectById(entity.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        if (entity.getId() != null) {
            ShortDramaStoryboard existing = storyboardMapper.selectById(entity.getId());
            if (existing == null || !entity.getProjectId().equals(existing.getProjectId())) {
                throw new IllegalArgumentException("分镜不存在或不属于当前项目");
            }
        }
        if (entity.getDurationSeconds() == null || entity.getDurationSeconds() <= 0) {
            entity.setDurationSeconds(defaultDurationForSceneType(entity.getSceneType()));
        }
        if (entity.getId() == null) {
            entity.setVideoStatus("pending");
            storyboardMapper.insert(entity);
        } else {
            storyboardMapper.updateById(entity);
            entity = storyboardMapper.selectById(entity.getId());
        }
        videoComposeService.invalidateComposition(entity.getProjectId());
        return MapstructUtils.convert(entity, ShortDramaStoryboardVo.class);
    }

    @Override
    public ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId) {
        return generateVideo(storyboardId, videoModel, userId, null);
    }

    /**
     * 生成单镜视频。
     * @param lastFrameUrl 上一镜末帧 URL（仅同场景相邻镜头传入，用于首帧承接）；跨场景或首镜传 null
     */
    public ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId, String lastFrameUrl) {
        ShortDramaStoryboard storyboard = storyboardMapper.selectById(storyboardId);
        if (storyboard == null) throw new IllegalArgumentException("分镜不存在");
        ShortDramaProject project = projectMapper.selectById(storyboard.getProjectId());
        if (project == null || !userId.equals(project.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
        ChatModelVo modelVo = chatModelService.selectModelByName(videoModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到视频模型配置: " + videoModel);

        // 收集所有参考图（角色 + 场景 + 末帧承接）
        List<String> referenceImages = findStoryboardReferenceImages(storyboard, lastFrameUrl);

        // 根据参考图数量自动切换模型
        if (referenceImages != null && !referenceImages.isEmpty()) {
            String targetModel;
            if (referenceImages.size() >= 2) {
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
                    log.info("参考图({}张) → 切换模型: {}", referenceImages.size(), targetModel);
                } else {
                    log.info("参考图({}张)但模型未注册: {}，保持原模型", referenceImages.size(), targetModel);
                }
            }
        }

        String enrichedPrompt = buildEnrichedVideoPrompt(storyboard, referenceImages, lastFrameUrl);

        VideoContext ctx = VideoContext.builder()
            .chatModelVo(modelVo)
            .prompt(enrichedPrompt)
            .size(projectAspectRatio(storyboard.getProjectId()))
            .seconds(storyboard.getDurationSeconds())
            .referenceImages(referenceImages)
            .generateAudio(Boolean.TRUE)
            .returnLastFrame(Boolean.TRUE)
            .lastFrameUrl(lastFrameUrl)
            .build();
        String generationToken = "local:" + UUID.randomUUID();
        storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, storyboardId)
            .set(ShortDramaStoryboard::getVideoUrl, null)
            .set(ShortDramaStoryboard::getVideoId, generationToken)
            .set(ShortDramaStoryboard::getVideoStatus, "generating"));
        videoComposeService.invalidateComposition(project.getId());

        MediaGenerationResponse response;
        try {
            response = videoServiceFactory.getOriginalService(modelVo.getProviderCode()).generateVideo(ctx);
        } catch (RuntimeException ex) {
            int failed = storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, storyboardId)
                .eq(ShortDramaStoryboard::getVideoId, generationToken)
                .set(ShortDramaStoryboard::getVideoId, null)
                .set(ShortDramaStoryboard::getVideoStatus, "failed"));
            if (failed > 0) {
                videoComposeService.invalidateComposition(project.getId());
            }
            throw ex;
        }
        String videoUrl = null;
        String videoId = null;
        String videoStatus;
        String lastFrame = null;
        if (response != null && StrUtil.isNotBlank(response.getUrl())) {
            videoUrl = response.getUrl();
            lastFrame = response.getLastFrameUrl();
            videoStatus = "done";
        } else if (response != null && StrUtil.isNotBlank(response.getId())) {
            videoId = response.getId();
            lastFrame = response.getLastFrameUrl();
            videoStatus = "generating";
        } else if (response != null && "processing".equals(response.getStatus())) {
            videoId = response.getId();
            lastFrame = response.getLastFrameUrl();
            videoStatus = "generating";
        } else {
            videoStatus = "failed";
        }
        int completed = storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
            .eq(ShortDramaStoryboard::getId, storyboardId)
            .eq(ShortDramaStoryboard::getVideoId, generationToken)
            .set(ShortDramaStoryboard::getVideoUrl, videoUrl)
            .set(ShortDramaStoryboard::getVideoId, videoId)
            .set(ShortDramaStoryboard::getVideoStatus, videoStatus)
            .set(StrUtil.isNotBlank(lastFrame), ShortDramaStoryboard::getLastFrameUrl, lastFrame));
        if (completed > 0) {
            videoComposeService.invalidateComposition(project.getId());
        }
        return MapstructUtils.convert(storyboardMapper.selectById(storyboardId), ShortDramaStoryboardVo.class);
    }

    /** 收集分镜关联的所有参考图：角色形象图（按出场顺序）+ 场景图 + 可选末帧承接 */
    private List<String> findStoryboardReferenceImages(ShortDramaStoryboard storyboard, String lastFrameUrl) {
        List<String> images = new ArrayList<>();
        List<CharacterRef> chars = parseCharacterRefs(storyboard.getCharactersJson());
        if (chars != null) {
            for (CharacterRef ref : chars) {
                String img = findCharacterImageUrl(storyboard.getProjectId(), ref.getName(), ref.getAppearance());
                if (StrUtil.isNotBlank(img) && !images.contains(img)) {
                    images.add(img);
                }
            }
        }
        if (StrUtil.isNotBlank(storyboard.getLocationName())) {
            String img = findLocationImageUrl(storyboard.getProjectId(), storyboard.getLocationName());
            if (StrUtil.isNotBlank(img) && !images.contains(img)) {
                images.add(img);
            }
        }
        // 末帧承接：放在最后一张，@imageN 标记会自动绑定
        if (StrUtil.isNotBlank(lastFrameUrl) && !images.contains(lastFrameUrl)) {
            images.add(lastFrameUrl);
        }
        return images.isEmpty() ? null : images;
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
        boolean hasRefImages = refImages != null && !refImages.isEmpty();
        StringBuilder sb = new StringBuilder();

        // 1. 镜头语言标签
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

        // 3. 角色信息（名字→年龄性别，附加站位 + @imageN 参考图标记）
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
                // 多参考图模式：附加 @imageN 标记
                if (hasRefImages) {
                    int imgIdx = findRefImageIndex(refImages, storyboard.getProjectId(), ref.getName());
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
        if (StrUtil.isNotBlank(storyboard.getImagePrompt())) {
            sb.append("[视觉] ").append(storyboard.getImagePrompt()).append("\n");
        }

        // 9. 目标时长与节奏
        int durationSeconds = storyboard.getDurationSeconds() != null ? storyboard.getDurationSeconds() : 6;
        sb.append("[时长] ").append(durationSeconds).append("秒，动作与运镜必须完整覆盖全程");
        if (durationSeconds >= 8) {
            sb.append("，按前段、中段、后段三个连续节拍展开，避免动作提前结束");
        }
        sb.append("\n");

        // 10. 视觉风格
        String artStyle = artStyleSuffix(storyboard.getProjectId());
        if (StrUtil.isNotBlank(artStyle)) {
            sb.append("\n[风格] ").append(artStyle);
        }

        // 11. 视频描述（核心驱动 prompt）
        sb.append("\n").append(storyboard.getVideoPrompt());
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

        sb.append("[人物身份隔离] ");
        if (characterCount == 0) {
            sb.append("本镜没有具名角色参考。若出现群众，每个人必须具有不同的脸型、五官比例、身高、体态、发型和服装细节；")
                .append("禁止复制脸、镜像人物、双胞胎式重复、同一人物贴图和整排相同表情。");
        } else if (characterCount == 1) {
            String characterName = chars.get(0).getName();
            sb.append("唯一具名角色为").append(characterName).append("；")
                .append(hasRefImages ? "该角色参考图只绑定此人，绝不能扩散给背景人物；" : "角色身份只属于此人；");
            if (closeShot) {
                sb.append("这是单人近景/特写，画面只允许一张可辨认人脸。其他人物必须留在画外，")
                    .append("不得出现背景人头、倒影脸、反射脸、画像脸、脸谱幻影或多重曝光；");
            } else {
                sb.append("若剧情必须出现无名群众，群众不得复用").append(characterName)
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
                    int imageIndex = findRefImageIndex(refImages, storyboard.getProjectId(), ref.getName());
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
        String targetUrl = findCharacterImageUrl(projectId, characterName);
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
        if (appearances == null || appearances.isEmpty()) return null;
        if (StrUtil.isBlank(appearance) || "初始形象".equals(appearance)) {
            return appearances.get(0);
        }
        // 1. changeReason 精确匹配
        for (ShortDramaCharacterAppearance ap : appearances) {
            if (appearance.equals(ap.getChangeReason())) return ap;
        }
        // 2. changeReason 包含匹配
        for (ShortDramaCharacterAppearance ap : appearances) {
            if (StrUtil.isNotBlank(ap.getChangeReason()) && ap.getChangeReason().contains(appearance)) return ap;
        }
        // 3. description 包含匹配
        for (ShortDramaCharacterAppearance ap : appearances) {
            if (StrUtil.isNotBlank(ap.getDescription()) && ap.getDescription().contains(appearance)) return ap;
        }
        // 4. appearanceIndex 数字匹配
        try {
            int idx = Integer.parseInt(appearance);
            if (idx >= 0 && idx < appearances.size()) return appearances.get(idx);
        } catch (NumberFormatException ignored) {}
        // 5. 回退主形象
        return appearances.get(0);
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
        if (StrUtil.isBlank(storyboard.getVideoId()) || storyboard.getVideoId().startsWith("local:")) {
            return MapstructUtils.convert(storyboard, ShortDramaStoryboardVo.class);
        }
        String predictionId = storyboard.getVideoId();
        ChatModelVo modelVo = chatModelService.selectModelByName(videoModel);
        if (modelVo == null) {
            throw new IllegalArgumentException("未找到视频模型配置: " + videoModel);
        }
        VideoContext ctx = VideoContext.builder()
            .chatModelVo(modelVo)
            .prompt("retrieve")
            .videoId(predictionId)
            .build();
        MediaGenerationResponse response = videoServiceFactory.getOriginalService(modelVo.getProviderCode())
            .retrieveVideo(ctx);
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
                videoStatus = "failed";
            }
        } else if (response != null && "failed".equals(response.getStatus())) {
            videoStatus = "failed";
        }
        if (videoStatus != null) {
            int updated = storyboardMapper.update(null, new LambdaUpdateWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getId, storyboardId)
                .eq(ShortDramaStoryboard::getVideoId, predictionId)
                .set(ShortDramaStoryboard::getVideoUrl, videoUrl)
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
        ShortDramaProject project = projectMapper.selectById(projectId);
        if (project == null || !userId.equals(project.getUserId())) {
            throw new IllegalArgumentException("项目不存在或无权限");
        }
        List<ShortDramaStoryboard> storyboards = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getProjectId, projectId)
                .orderByAsc(ShortDramaStoryboard::getSceneNo));

        // 按 locationName 分组：同场景内串行（保末帧拼接），跨场景组并发
        List<List<ShortDramaStoryboard>> groups = new ArrayList<>();
        List<ShortDramaStoryboard> currentGroup = new ArrayList<>();
        String currentLoc = null;
        for (ShortDramaStoryboard sb : storyboards) {
            String loc = StrUtil.blankToDefault(sb.getLocationName(), "");
            if (!loc.equals(currentLoc)) {
                if (!currentGroup.isEmpty()) { groups.add(currentGroup); currentGroup = new ArrayList<>(); }
                currentLoc = loc;
            }
            currentGroup.add(sb);
        }
        if (!currentGroup.isEmpty()) groups.add(currentGroup);

        // 跨场景组并发，组上限 4
        int parallel = Math.min(4, Math.max(1, groups.size()));
        java.util.concurrent.ExecutorService pool = Executors.newFixedThreadPool(parallel,
            r -> { Thread t = new Thread(r, "short-drama-video-gen"); t.setDaemon(true); return t; });
        try {
            List<java.util.concurrent.Future<List<ShortDramaStoryboardVo>>> futures = new ArrayList<>();
            for (List<ShortDramaStoryboard> group : groups) {
                futures.add(pool.submit(() -> generateGroupSerial(group, videoModel, userId)));
            }
            // 按 sceneNo 顺序汇总结果
            List<ShortDramaStoryboardVo> result = new ArrayList<>();
            for (java.util.concurrent.Future<List<ShortDramaStoryboardVo>> f : futures) {
                try { result.addAll(f.get()); }
                catch (Exception e) { log.warn("视频生成分组失败: {}", e.getMessage()); }
            }
            result.sort(java.util.Comparator.comparing(v -> v.getSceneNo() == null ? Integer.MAX_VALUE : v.getSceneNo()));
            return result;
        } finally {
            pool.shutdownNow();
        }
    }

    /** 同场景组内串行生成：上一镜末帧喂下一镜首帧。 */
    private List<ShortDramaStoryboardVo> generateGroupSerial(List<ShortDramaStoryboard> group, String videoModel, Long userId) {
        List<ShortDramaStoryboardVo> result = new ArrayList<>();
        String prevLastFrameUrl = null;
        for (ShortDramaStoryboard sb : group) {
            try {
                String lastFrameForThis = StrUtil.isNotBlank(prevLastFrameUrl) ? prevLastFrameUrl : null;
                ShortDramaStoryboardVo vo = generateVideo(sb.getId(), videoModel, userId, lastFrameForThis);
                if (lastFrameForThis != null) {
                    vo = ensureVideoDone(sb.getId(), videoModel, userId);
                }
                result.add(vo);
                ShortDramaStoryboard latest = storyboardMapper.selectById(sb.getId());
                prevLastFrameUrl = (latest != null && StrUtil.isNotBlank(latest.getLastFrameUrl())) ? latest.getLastFrameUrl() : null;
            } catch (Exception e) {
                log.warn("镜头{}视频生成失败: {}", sb.getSceneNo(), e.getMessage());
                ShortDramaStoryboard current = storyboardMapper.selectById(sb.getId());
                result.add(MapstructUtils.convert(current != null ? current : sb, ShortDramaStoryboardVo.class));
                prevLastFrameUrl = null;
            }
        }
        return result;
    }

    /**
     * 后台同步轮询单镜视频直到 done/failed（用于同场景末帧拼接时拿到末帧再喂下一镜）。
     * 单镜累计轮询不超过 5 分钟，超时按当前状态返回。
     */
    private ShortDramaStoryboardVo ensureVideoDone(Long storyboardId, String videoModel, Long userId) {
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
        try {
            while (System.currentTimeMillis() < deadline) {
                ShortDramaStoryboardVo vo = retrieveVideo(storyboardId, videoModel, userId);
                if (vo == null) return null;
                if ("done".equals(vo.getVideoStatus()) || "failed".equals(vo.getVideoStatus())) {
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
    public ShortDramaDetailVo analyzeAssets(Long projectId, Long scriptId, Long userId) {
        ShortDramaProject project = validateProjectOwner(projectId, userId);
        ShortDramaScript script = scriptMapper.selectById(scriptId);
        if (script == null || !projectId.equals(script.getProjectId())) {
            throw new IllegalArgumentException("剧本不存在");
        }
        clearAssets(projectId);
        ChatModelVo modelVo = findChatModel();
        AbstractChatService chatService = getChatService(modelVo);
        ChatModel chatModel = chatService.buildChatModel(modelVo);
        executePhase2_AssetAnalysis(chatModel, projectId, script);
        return getDetail(projectId, userId);
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
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String prompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
        String finalPrompt = ShortDramaImageConstants.LOCATION_PROMPT_PREFIX + prompt +
            ShortDramaImageConstants.LOCATION_PROMPT_SUFFIX + artStyleSuffix(location.getProjectId());
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size(projectAspectRatio(location.getProjectId())).image(referenceImage).build());
        if (StrUtil.isNotBlank(imageUrl)) {
            location.setReferenceImageUrl(imageUrl);
            List<String> urls = readJsonStringList(location.getImageUrls());
            List<String> descs = readJsonStringList(location.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
            location.setImageUrls(JsonUtils.toJsonString(urls));
            location.setImageDescriptions(JsonUtils.toJsonString(descs));
            location.setSelectedImageIndex(urls.size() - 1);
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
        ShortDramaLocation location = locationMapper.selectById(locationId);
        if (location == null) throw new IllegalArgumentException("场景不存在");
        validateProjectOwner(location.getProjectId(), userId);
        ChatModelVo modelVo = chatModelService.selectModelByName(imageModel);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + imageModel);
        String prompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
        String finalPrompt = ShortDramaImageConstants.LOCATION_PROMPT_PREFIX + prompt +
            ShortDramaImageConstants.LOCATION_PROMPT_SUFFIX + artStyleSuffix(location.getProjectId());
        String referenceImage = validateReferenceImageUrl(referenceImageUrl);
        String imageUrl = imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .generateImage(org.ruoyi.common.chat.entity.image.ImageContext.builder()
                .chatModelVo(modelVo).prompt(finalPrompt).size(projectAspectRatio(location.getProjectId())).image(referenceImage).build());
        if (StrUtil.isNotBlank(imageUrl)) {
            location.setReferenceImageUrl(imageUrl);
            List<String> urls = readJsonStringList(location.getImageUrls());
            List<String> descs = readJsonStringList(location.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
            location.setImageUrls(JsonUtils.toJsonString(urls));
            location.setImageDescriptions(JsonUtils.toJsonString(descs));
            location.setSelectedImageIndex(urls.size() - 1);
            locationMapper.updateById(location);
        }
        return MapstructUtils.convert(location, ShortDramaLocationVo.class);
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
        ChatModelVo modelVo = chatModelService.selectModelByName(model);
        if (modelVo == null) throw new IllegalArgumentException("未找到图片模型配置: " + model);

        String prompt;
        String size;
        Integer seed = null;
        if ("appearance".equals(assetType)) {
            ShortDramaCharacterAppearance appearance = characterAppearanceMapper.selectById(assetId);
            if (appearance == null) throw new IllegalArgumentException("形象不存在");
            ShortDramaCharacter character = characterMapper.selectById(appearance.getCharacterId());
            if (character == null) throw new IllegalArgumentException("角色不存在");
            validateProjectOwner(character.getProjectId(), userId);
            String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
            prompt = buildCharacterPrompt(basePrompt, character.getProjectId());
            size = "3:2";
            seed = ShortDramaImageConstants.styleSeed(character.getProjectId());
        } else if ("location".equals(assetType)) {
            ShortDramaLocation location = locationMapper.selectById(assetId);
            if (location == null) throw new IllegalArgumentException("场景不存在");
            validateProjectOwner(location.getProjectId(), userId);
            String basePrompt = firstNotBlank(primaryLocationDescription(location), location.getSummary(), location.getName());
            prompt = ShortDramaImageConstants.LOCATION_PROMPT_PREFIX + basePrompt +
                ShortDramaImageConstants.LOCATION_PROMPT_SUFFIX + artStyleSuffix(location.getProjectId());
            size = projectAspectRatio(location.getProjectId());
            seed = ShortDramaImageConstants.styleSeed(location.getProjectId());
        } else {
            throw new IllegalArgumentException("不支持的资产类型: " + assetType);
        }

        return imageServiceFactory.getOriginalService(modelVo.getProviderCode())
            .startImageGeneration(ImageContext.builder()
                .chatModelVo(modelVo).prompt(prompt).size(size).seed(seed)
                .image(validateReferenceImageUrl(referenceImageUrl)).build());
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

        String basePrompt = firstNotBlank(appearance.getDescription(), character.getVisualDescription(), character.getName());
        String finalPrompt = buildCharacterPrompt(basePrompt, character.getProjectId());

        appearance.setPreviousImageUrls(appearance.getImageUrls());
        appearance.setPreviousDescriptions(appearance.getImageDescriptions());
        appearance.setReferenceImageUrl(imageUrl);
        List<String> urls = readJsonStringList(appearance.getImageUrls());
        List<String> descs = readJsonStringList(appearance.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
        appearance.setImageUrls(JsonUtils.toJsonString(urls));
        appearance.setImageDescriptions(JsonUtils.toJsonString(descs));
        appearance.setSelectedImageIndex(urls.size() - 1);
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
        String finalPrompt = ShortDramaImageConstants.LOCATION_PROMPT_PREFIX + prompt +
            ShortDramaImageConstants.LOCATION_PROMPT_SUFFIX + artStyleSuffix(location.getProjectId());

        location.setPreviousImageUrls(location.getImageUrls());
        location.setPreviousDescriptions(location.getImageDescriptions());
        location.setReferenceImageUrl(imageUrl);
        List<String> urls = readJsonStringList(location.getImageUrls());
        List<String> descs = readJsonStringList(location.getImageDescriptions());
        addImageUrl(urls, imageUrl, descs, finalPrompt);
        location.setImageUrls(JsonUtils.toJsonString(urls));
        location.setImageDescriptions(JsonUtils.toJsonString(descs));
        location.setSelectedImageIndex(urls.size() - 1);
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
     * 流式调用模型。onPartial 回调在每次新 token 到来时以当前完整 buffer 调用，
     * 调用方可在此做增量解析（如分镜 panel 增量推送）。onPartial 为 null 时行为同旧版。
     */
    private String streamingChat(StreamingChatModel streamingModel, ChatModel chatModel,
                                 String prompt, SseEmitter emitter, String streamPhase,
                                 java.util.function.Consumer<String> onPartial) {
        if (streamingModel == null || emitter == null) {
            return chatModel.chat(prompt);
        }
        StringBuilder buf = new StringBuilder();
        CompletableFuture<Void> done = new CompletableFuture<>();
        ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "short-drama-sse-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
        heartbeat.scheduleAtFixedRate(() -> emit(emitter, streamPhase, "running", "模型仍在处理中，请稍候..."),
            15, 15, TimeUnit.SECONDS);
        List<ChatMessage> messages = List.of(UserMessage.from(prompt));
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String text) {
                buf.append(text);
                emitStream(emitter, streamPhase, text);
                if (onPartial != null) {
                    try { onPartial.accept(buf.toString()); } catch (Exception ex) {
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
            throw new RuntimeException("模型在30分钟内未完成响应，请检查模型服务状态或更换模型后重试", cause);
        } finally {
            heartbeat.shutdownNow();
        }
        return buf.toString();
    }

    // ==================== Phase 1: 剧本打磨（单次流式调用，元信息 JSON + 剧本正文）====================

    private static final String SCRIPT_DELIMITER = "===SCRIPT===";

    private static final String PHASE1_COMBINED_SYSTEM = """
        你是顶级短剧编剧、剧本统筹和场景规划师。根据用户提供的故事资料，创作一部剧情完整、可拍摄、具有多场次推进的短剧，而不是只截取结局或高潮片段。

        【输出格式 - 严格按顺序】
        先输出一行 JSON 元信息，换行后输出 "===SCRIPT===", 再换行后输出完整剧本正文（1200-2500字）。

        JSON 格式（一行完成，不要换行）：
        {"projectName":"项目名","description":"简介(30-80字)","scriptName":"剧本名","tone":"类型与基调","outlineText":"完整分场大纲(500-1000字)"}

        【完整故事弧 - 最高优先级】
        1. 必须覆盖用户资料中的完整主线，禁止只选择决赛、告白、复仇等最后高潮单独成篇
        2. 至少包含5个有不同剧情功能的场次；资料跨度较大时建议6-10场
        3. 场次必须覆盖：人物与困境建立 → 改变契机 → 发展/训练或关系推进 → 重大挫折或背叛 → 主角自主决定与重组 → 高潮行动 → 结果与主题落点
        4. 每场都必须改变剧情状态，说明角色目标、阻碍、行动和结果；禁止多个场次只是重复训练、重复争吵或重复比赛
        5. 反派倒戈、关系破裂、能力掌握、团队团结、比赛逆转等重大变化必须提前铺垫，不能突然发生
        6. 开场不得直接进入最终决战，除非用户明确要求只写高潮片段

        【分场要求】
        - 使用标准场景头：内景/外景 + 具体地点 + 时间
        - 地点变化、时间跳变或一个戏剧单元完成时另起新场
        - 每场包含：场景建立、明确冲突、角色行动、可见结果、引向下一场的钩子
        - 场间用动作、决定、视线、声音、时间推进或人物出发进行衔接
        - 闪回最多使用一次，且不能替代正常的前期铺垫

        【剧情忠实与提炼】
        - 用户输入可能包含新闻摘要、角色资料、链接来源和重复段落。提取其中的故事事实，不要把网址、媒体名称、点赞按钮或来源标签写进剧本
        - 保留核心人物、核心冲突、关键反转与结局；可压缩次要人物和重复信息，但不得删除导致主线断裂的阶段
        - 不照搬现实演员姓名作为角色说明；只使用故事中的角色名
        - 不虚构用户资料之外的重大设定、冠军、背叛原因或人物关系；必要的过渡动作可以合理补全

        【剧本格式】
        场景头
        场景环境与人物初始状态
        连续动作描述
        角色名：「台词」
        该场结果与进入下一场的动作钩子

        【台词与动作】
        - 对话必须推动认知、关系、决定或行动变化
        - 重要决定先有触发，再有反应，最后付诸行动
        - 将心理活动转译为可见动作、表情、选择或简短独白
        - 比赛、战斗和训练必须写清动作过程与结果，不能只用“经过努力”“最终获胜”概括

        【自检 - 输出前内部完成】
        - 是否至少5个场景头，且地点/时间/剧情功能有变化
        - 是否覆盖了用户资料中的起点、发展、低谷、反击和结局
        - 删除任一场是否会造成剧情断裂；若不会，该场应合并或强化
        - 是否存在角色无原因倒戈、突然掌握能力、突然团结或突然获胜
        - 是否误把最终高潮写成了整部剧本

        只输出上述格式，不要任何额外说明。
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
            if (normalized.matches("^(内景|外景|内外景|闪回[：:]?\\s*(内景|外景)?).*")) count++;
        }
        return count;
    }

    private static boolean scriptNeedsExpansion(ShortDramaScriptResult result) {
        if (result == null || StrUtil.isBlank(result.getScriptText())) return true;
        return result.getScriptText().length() < 1000 || countSceneHeadings(result.getScriptText()) < 5;
    }

    private ShortDramaScriptResult expandIncompleteScript(ChatModel chatModel, ShortDramaIdeaBo bo,
                                                           ShortDramaScriptResult initial) {
        String prompt = PHASE1_COMBINED_SYSTEM
            + "\n\n以下初稿场次不足或过度集中在高潮。请基于原始资料完整重写，不要只修改局部。"
            + "\n原始资料：\n" + sanitizeIdeaInput(bo.getIdea())
            + "\n\n不合格初稿：\n" + (initial == null ? "无" : firstNotBlank(initial.getScriptText(), "无"));
        ShortDramaScriptResult expanded = parsePhase1Response(chatModel.chat(prompt));
        return expanded != null && !scriptNeedsExpansion(expanded) ? expanded : initial;
    }

    private ShortDramaScriptResult executePhase1_ScriptPolish(ChatModel chatModel, ShortDramaIdeaBo bo,
                                                               Consumer<String> onPhase) {
        String projectName = firstNotBlank(bo.getProjectName(), "短剧项目");
        String prompt = PHASE1_COMBINED_SYSTEM + "\n\n用户期望项目名：" + projectName + "\n用户创意：" + sanitizeIdeaInput(bo.getIdea());

        long t0 = System.currentTimeMillis();
        String resp = chatModel.chat(prompt);
        log.info("Phase 1 剧本打磨: elapsed={}ms len={}", System.currentTimeMillis() - t0,
            resp != null ? resp.length() : 0);

        ShortDramaScriptResult result = parsePhase1Response(resp);
        if (result == null || StrUtil.isBlank(result.getScriptText())) {
            throw new RuntimeException("剧本打磨失败：LLM 返回格式异常，请重试");
        }
        if (scriptNeedsExpansion(result)) {
            log.warn("Phase 1 剧本场次不足，自动扩写: length={} scenes={}",
                result.getScriptText().length(), countSceneHeadings(result.getScriptText()));
            result = expandIncompleteScript(chatModel, bo, result);
        }
        if (onPhase != null) onPhase.accept("outline_done");
        return result;
    }

    private ShortDramaScriptResult executePhase1_ScriptPolish(ChatModel chatModel, ShortDramaIdeaBo bo) {
        return executePhase1_ScriptPolish(chatModel, bo, null);
    }

    /**
     * 流式版本：StreamingChatModel 单次调用，边收边解析，JSON 完成后立即推流剧本正文
     */
    private ShortDramaScriptResult executePhase1_Streaming(StreamingChatModel streamingModel,
                                                            ShortDramaIdeaBo bo, SseEmitter emitter) {
        String projectName = firstNotBlank(bo.getProjectName(), "短剧项目");
        String systemPrompt = PHASE1_COMBINED_SYSTEM;
        String userPrompt = "用户期望项目名：" + projectName + "\n用户创意：" + sanitizeIdeaInput(bo.getIdea());
        List<ChatMessage> messages = List.of(
            SystemMessage.from(systemPrompt), UserMessage.from(userPrompt));

        StringBuilder buf = new StringBuilder();
        ShortDramaScriptResult[] result = {null};
        boolean[] scriptStreaming = {false};
        CompletableFuture<Void> streamDone = new CompletableFuture<>();

        long t0 = System.currentTimeMillis();
        streamingModel.chat(messages, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String text) {
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
                        emit(emitter, "outline", "done", "大纲已生成，正在展开完整剧本...");
                        // 分隔符后面的已有文字作为剧本开头推送
                        String tail = buf.substring(delim + SCRIPT_DELIMITER.length());
                        if (StrUtil.isNotBlank(tail.trim())) {
                            emitStream(emitter, "script", tail);
                        }
                    }
                } else {
                    emitStream(emitter, "script", text);
                }
            }
            @Override
            public void onCompleteResponse(ChatResponse response) {
                // 兜底：如果没找到分隔符，尝试从完整响应提取 JSON
                if (!scriptStreaming[0] || result[0] == null) {
                    result[0] = parsePhase1Response(buf.toString());
                }
                // 兜底：如果分隔符后有文字但没通过 onPartial 推完，补推
                if (result[0] != null) {
                    String fullText = buf.toString();
                    int delim = fullText.indexOf(SCRIPT_DELIMITER);
                    String scriptText = delim >= 0
                        ? fullText.substring(delim + SCRIPT_DELIMITER.length()).trim()
                        : extractScriptAfterJson(fullText);
                    if (StrUtil.isBlank(result[0].getScriptText()) && StrUtil.isNotBlank(scriptText)) {
                        result[0].setScriptText(StringUtils.strip(scriptText));
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
            throw new RuntimeException("剧本打磨失败：" + e.getMessage(), e);
        }
        if (result[0] == null || StrUtil.isBlank(result[0].getScriptText())) {
            throw new RuntimeException("剧本打磨失败：LLM 返回格式异常，请重试");
        }
        if (scriptNeedsExpansion(result[0])) {
            emit(emitter, "script", "running", "检测到剧情场次不足，正在扩展完整故事线...");
            ChatModelVo fallbackModel = findChatModel();
            ChatModel fallbackChatModel = getChatService(fallbackModel).buildChatModel(fallbackModel);
            result[0] = expandIncompleteScript(fallbackChatModel, bo, result[0]);
            emit(emitter, "script", "done", "完整多场次剧本已生成");
        }
        return result[0];
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
            result.setScriptText(StringUtils.strip(scriptPart));
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
        String scriptText = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
        if (StrUtil.isBlank(scriptText)) return;

        try {
            List<AssetGeneratedCharacter> characters;
            List<AssetGeneratedLocation> locations;

            if (emitter != null && streamModel != null) {
                // 流式模式：串行执行，用户能看到每个阶段的原始输出
                emit(emitter, "assets_chars", "running", "正在分析角色档案...");
                String charsResp = streamingChat(streamModel, chatModel, buildCharacterProfilePrompt(scriptText), emitter, "assets");
                characters = parseCharacterList(charsResp);
                emit(emitter, "assets_chars", "done", "角色分析完成，提取 " + characters.size() + " 个角色");

                emit(emitter, "assets_locs", "running", "正在分析场景站位...");
                StreamingChatModel locsStreamModel = buildStreamingChatModel();
                String locsResp = streamingChat(locsStreamModel, chatModel, buildLocationCreatePrompt(scriptText), emitter, "assets");
                locations = parseLocationList(locsResp);
                emit(emitter, "assets_locs", "done", "场景分析完成，提取 " + locations.size() + " 个场景");
            } else {
                // 非流式：保持原有并发模式，速度更快
                CompletableFuture<List<AssetGeneratedCharacter>> charsFuture =
                    CompletableFuture.supplyAsync(() -> {
                        String resp = chatModel.chat(buildCharacterProfilePrompt(scriptText));
                        return parseCharacterList(resp);
                    });
                CompletableFuture<List<AssetGeneratedLocation>> locsFuture =
                    CompletableFuture.supplyAsync(() -> {
                        String resp = chatModel.chat(buildLocationCreatePrompt(scriptText));
                        return parseLocationList(resp);
                    });
                characters = charsFuture.join();
                locations = locsFuture.join();
            }

            characters = deduplicateCharacters(characters);
            locations = deduplicateLocations(locations);

            // 2b: 并发为每个角色生成视觉描述（不论流式与否，均并发执行）
            List<CompletableFuture<Void>> visualFutures = new ArrayList<>();
            for (AssetGeneratedCharacter c : characters) {
                visualFutures.add(CompletableFuture.runAsync(() -> {
                    String visualDesc = "";
                    try {
                        String vr = chatModel.chat(buildCharacterVisualPrompt(c, script.getTone()));
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
                        log.warn("角色「{}」视觉描述生成失败: {}", c.getName(), e.getMessage());
                    }

                    synchronized (this) {
                        insertCharacter(projectId, c, visualDesc);
                    }
                }));
            }
            CompletableFuture.allOf(visualFutures.toArray(new CompletableFuture[0])).join();

            // 插入场景
            for (AssetGeneratedLocation loc : locations) {
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
        } catch (Exception e) {
            log.warn("Phase 2 资产分析失败: {}", e.getMessage());
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
        return """
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
            1级（朴素/统一级）：平民、底层劳动者，服装朴素统一，基础款式

            【性格标签 personality_tags】
            气质类：高冷、温柔、阳光、忧郁、神秘、妩媚、清冷、热情
            性格类：腹黑、傲娇、毒舌、话痨、闷骚、直爽、圆滑、固执
            态度类：自信、自卑、孤僻、合群、叛逆、顺从
            用逗号分隔，至少2个，最多5个

            【视觉关键词 visual_keywords】
            风格类：精英气质、街头潮流、学院风、复古优雅、运动活力、文艺气息、冷淡极简
            特征类：病弱感、禁欲系、狼狗系、奶狗系、御姐范、萝莉感、大叔味
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
                  "archetype": "角色原型（如霸道总裁）",
                  "personalityTags": "高冷,腹黑",
                  "costumeTier": 3,
                  "visualKeywords": "精英气质,禁欲系",
                  "suggestedColors": "深蓝,金色",
                  "primaryIdentifier": "眼角泪痣（仅S/A级需要）"
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
            case "S" -> "180-220字，必须有极高的视觉辨识度和主角气质，五官精致，禁止用「普通」「平凡」「不起眼」";
            case "A" -> "150-180字，有明显的个人特色和记忆点，长相精致有吸引力";
            case "B" -> "120-150字，有基本的辨识特征";
            case "C" -> "80-120字，简洁但完整的形象描述";
            default -> "50-80字，基础形象即可";
        };
        return """
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
            - 独特记号：痣（位置）、雀斑、小疤痕等

            💇 发型描写（必须详细）：
            - 发色：乌黑、深棕、栗色、金棕等
            - 发长：齐耳短发、及肩、过肩、及腰
            - 发型：自然披散、高马尾、低马尾、丸子头、盘发、寸头、中分、偏分、背头
            - 发质：柔顺、自然卷、微卷、蓬松、服帖
            - 刘海：齐刘海、空气刘海、无刘海、中分刘海、侧分刘海、碎发刘海

            👤 体态：身形（修长、健硕、纤细、匀称）、身高感（高挑、娇小、适中）

            👔 服装配饰：上衣、下装、鞋子（必填！）、配饰

            【禁止描写】皮肤颜色、眼睛颜色、唇色、表情/姿态/动作、背景/环境、情绪形容词、抽象气质、「可能」「或」等不确定描述

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
        return """
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
        try {
            String text = firstNotBlank(script.getScriptText(), script.getOutlineText(), "");
            if (StrUtil.isBlank(text)) return List.of();

            String charsLib = buildCharactersLibString(projectId);
            String locsLib = buildLocationsLibString(projectId);
            String charsIntro = buildCharactersIntroString(projectId);
            String charsAppearanceList = buildCharactersAppearanceListString(projectId);
            String charsFullDesc = buildCharactersFullDescString(projectId);

            String prompt = buildStoryboardPlanPrompt(text, charsLib, locsLib, charsIntro, charsAppearanceList, charsFullDesc,
                artStyleSuffix(projectId), projectAspectRatio(projectId));
            String response;
            if (emitter != null) {
                StreamingChatModel activeStreamingModel = streamingModel != null ? streamingModel : buildStreamingChatModel();
                // 流式增量：每解析出一个完整 panel 就推给前端，不等整个数组完成
                final org.ruoyi.service.shortdrama.support.IncrementalJsonArrayExtractor<StoryboardPanelData> extractor =
                    new org.ruoyi.service.shortdrama.support.IncrementalJsonArrayExtractor<>(StoryboardPanelData.class);
                response = streamingChat(activeStreamingModel, chatModel, prompt, emitter, "storyboard_plan", buf -> {
                    List<StoryboardPanelData> fresh = extractor.feed(buf);
                    for (StoryboardPanelData p : fresh) {
                        if (p != null) emitPanel(emitter, p);
                    }
                });
            } else {
                response = chatModel.chat(prompt);
            }
            List<StoryboardPanelData> panels = parsePanelList(response);
            if (panels != null && !panels.isEmpty()) {
                for (int i = 0; i < panels.size(); i++) {
                    panels.get(i).setPanelNumber(i + 1);
                }
                return panels;
            }
        } catch (Exception e) {
            log.warn("Phase 3 分镜规划失败: {}", e.getMessage());
        }
        return List.of();
    }

    private static String buildStoryboardPlanPrompt(String text, String charsLib, String locsLib,
                                                     String charsIntro, String charsAppearanceList, String charsFullDesc,
                                                     String artStyle, String aspectRatio) {
        return """
            你是专业的分镜规划师。请根据剧本内容将故事拆解成连续的分镜头。

            【三级规划流程 - 最高优先级】
            必须先在内部完成以下三级规划，再输出镜头JSON；不要输出分析过程：
            1. 分场 scene：按地点变化、时间跳变或戏剧单元收束切场，同一场是同一时空下的连续戏
            2. 切片段 segment：每场沿叙事顺序在情绪转折、动作段落、说话人切换或信息揭示处切分；每片段总时长不得超过15秒
            3. 拆镜头 panel：每个片段内部拆成连续可拍镜头，每镜只承担一个剧情职责

            每个片段必须形成小型因果闭环：bridge_in触发 → segment_goal → 连续动作/对话 → segment_result → bridge_out。
            相邻片段必须通过动作、情绪、视线或声音中的至少一种桥梁衔接，禁止在片段边界冻结后跳转。

            【镜头数量控制 - 最高优先级】
            - 目标比例：每15-25个有效剧情字符≈1个镜头，根据动作和台词密度调整
            - 硬上限：最多40个镜头，超过时优先合并重复信息，不得删除因果中间态
            - 普通镜头5-8秒；无台词镜头也应优先保持5-8秒并包含持续可见变化
            - 单镜超过8秒只允许用于不可拆的连续台词、连续动作或史诗建立镜头，并必须包含前段→中段→后段的持续变化；任何单镜不得超过15秒
            - 每个片段内所有镜头duration之和必须≤15秒，超过必须拆成新片段
            - 时长必须由实际动作、对白和运镜容量决定，禁止按场景类型机械给长时长
            - 原文只支持1-3秒内容时，应与相邻因果动作合并，禁止靠环境空转填充成长镜头
            - 对话密集场景：多句对话可合并到一个镜头

            【分镜原则】
            1. 每个场景开始→1个建立镜头
            2. 每个关键动作→1个镜头
            3. 每段对话→1-2个镜头（说话者+必要时听者反应）
            4. 情绪高潮点→1个特写镜头

            【每个分镜包含】
            - panel_number: 全剧镜头序号
            - scene_number: 场次序号；地点/时间变化时递增
            - segment_number: 当前场内片段序号；每片段总时长≤15秒
            - segment_goal: 当前片段要完成的唯一剧情任务
            - segment_result: 片段结束时完成的剧情变化
            - bridge_in: 本片段如何承接上一片段，首片段写开场建立
            - bridge_out: 留给下一片段承接的动作、情绪、视线或声音
            - description: 画面描述（可视化元素，禁止主观情绪词）
            - characters: [{name, appearance, slot}]，name必须与资产库一致
            - location: 从场景资产库选择，名字完全一致
            - scene_type: daily/emotion/action/epic/suspense
            - source_text: 对应原文片段（必填）
            - start_state: 当前镜头第一帧状态，必须继承上一镜头end_state（场景切换除外）
            - end_state: 当前镜头最后一帧状态，明确人物位置、朝向、姿态、道具和动作结果
            - continuity_action: 与下一镜头衔接的未完成动作、视线方向或运动趋势
            - spatial_anchor: 场景空间锚点，明确左右、前后、远近和人物相对位置
            - present_characters: 当前场景仍然在场的全部人物；未明确离场不得消失
            - beat_type: 当前镜头唯一的剧情职责，只能是setup/trigger/reaction/decision/action/result/transition之一
            - narrative_cause: 当前镜头发生的直接原因，必须来自上一镜头story_result或剧本中的明确触发
            - character_goal: 当前焦点角色在这一刻想达成的具体目标
            - story_action: 为实现目标采取的可见行动或说出的关键台词
            - story_result: 当前行动造成的新信息、新阻碍、位置变化或关系变化，镜头结束前必须发生
            - next_hook: story_result中必须在下一镜头得到回应的问题、动作或后果
            - duration: 镜头时长(秒)，根据scene_type差异化分配：
              · daily（日常对话）：5-8秒
              · emotion（情绪高潮）：6-10秒
              · action（动作冲突）：5-8秒
              · epic（史诗大景）：8-15秒
              · suspense（悬疑紧张）：6-10秒

            【片段间过渡设计 - 最高优先级】
            1. 连续动作：前片段bridge_out写动作起始态，后片段bridge_in必须从动作进行时或完成时开始
            2. 情绪延续：前片段结尾用反应、眼神、微表情或肢体细节铺垫，后片段首镜强化或反转
            3. 视线匹配：前片段角色看向某物，后片段首镜展示该物或其造成的结果
            4. 声音桥接：前片段末尾的台词关键词或物理声源，可由后片段首镜画面回应
            5. 场景切换：必须由出发、开门、转身离场、时间标记、声音先入或结果揭示触发，禁止无因硬切

            【剧情因果链 - 最高优先级】
            在输出JSON前，先在内部按剧本原文顺序建立完整因果主线，但不要输出分析过程。
            1. 每个镜头只能承担一个主要剧情职责：建立、触发、反应、决定、行动、结果或转场
            2. 镜头N必须产生story_result；镜头N+1的narrative_cause必须直接承接该story_result或next_hook
            3. 禁止“因为剧本接下来这样写”式跳跃。角色改变位置、情绪、目标、关系或道具状态时，必须先出现触发和过渡动作
            4. 若状态从A跳到C，必须增加展示B的镜头，例如站立→坐着必须展示坐下，平静→愤怒必须展示触发与表情变化
            5. 对话不能只是轮流说话：每句关键台词必须改变对方认知、决定或行动，并在下一镜展示反应
            6. 场景切换必须有剧情原因和转场钩子，例如人物出发、时间推进、视线落向目标或结果揭示
            7. source_text必须严格按原文顺序覆盖，不得将后文结果提前，也不得遗漏导致因果断裂的关键动作
            8. 禁止连续镜头重复同一信息；每一镜结束时剧情状态必须相对开头发生可说明的变化
            9. 新角色、新道具、新能力、新地点首次出现必须有建立或触发，不得凭空加入
            10. 最终自检整条链：删除任一镜头后若不影响理解，说明该镜头无剧情作用，应合并或重写

            【叙事连续性与空间锚定 - 最高优先级】
            1. 同一location内，镜头N+1的start_state必须逐项承接镜头N的end_state，不得重置人物姿态或位置
            2. 角色进入场景后，在明确离场、切为拍不到该角色的特写/反打、或切换场景前，必须持续存在于present_characters
            3. 连续动作必须拆成“动作开始→动作过程→动作结果”，下一镜头从上一镜头尚未完成的动作或结果继续
            4. 保持180度轴线：同一对话或对峙中，人物左右关系、面对方向不得无理由反转
            5. 道具归属、手持状态、服装形象、伤势、光线方向和时间必须连续
            6. 每个镜头生成前自检：人物从哪里来、现在在哪里、面向谁、正在做什么、镜头结束后停在哪里
            7. 场景切换时start_state明确写“新场景建立”，不得伪装成连续动作

            【全局视觉约束 - 最高优先级】
            - 项目视觉风格：%s。所有description必须明确遵守该风格，禁止混入真人写实、摄影棚实拍等冲突表达
            - 项目画面比例：%s。构图和角色站位必须适应该画幅，避免主体被裁切

            【输出格式】
            只返回JSON数组：
[{"panel_number":1,"scene_number":1,"segment_number":1,"segment_goal":"张三进入办公室并打断李四","segment_result":"李四注意力转向张三","bridge_in":"开场建立办公室与人物位置","bridge_out":"张三的话音落下，李四抬头形成反应钩子","description":"...","characters":[{"name":"张三","appearance":"初始形象","slot":"门口"}],"location":"办公室_白天","scene_type":"daily","source_text":"...","start_state":"张三位于门外右侧，面向办公室","end_state":"张三推门后停在门内右侧，身体朝向李四","continuity_action":"张三保持推门手势并准备开口","spatial_anchor":"张三画面右侧，李四画面左侧办公桌后","present_characters":["张三","李四"],"beat_type":"trigger","narrative_cause":"张三收到必须立即开会的消息","character_goal":"让李四停止工作并听取通知","story_action":"张三推门进入并开口宣布开会","story_result":"李四被打断并抬头看向张三","next_hook":"下一镜展示李四对通知的反应","duration":8}]
            ⚠️ 严格JSON，不得输出markdown。

            角色资产库：%s
            场景资产库：%s
            角色介绍：%s
            角色形象列表：%s
            角色完整描述：%s

            剧本内容：
            %s
            """.formatted(artStyle, aspectRatio, charsLib, locsLib, charsIntro, charsAppearanceList, charsFullDesc, text);
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
            String prompt = buildCinematographerPrompt(panelsJson, panels.size(), locsDesc, charsInfo);
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
        return """
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
            String prompt = buildActingDirectionPrompt(panelsJson, panels.size(), charsInfo);
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
        return """
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
            lighting.put("quality", switch (firstNotBlank(panel.getSceneType(), "daily")) {
                case "suspense" -> "低调硬光，保留阴影层次";
                case "emotion" -> "柔和侧光，突出面部情绪";
                case "action" -> "高反差光线，强化动作轮廓";
                case "epic" -> "大范围自然光，突出空间规模";
                default -> "自然柔光，肤色与环境协调";
            });

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

            String sceneType = firstNotBlank(panel.getSceneType(), "daily");
            rule.put("depth_of_field", switch (sceneType) {
                case "epic" -> "深景深，清晰展现环境规模";
                case "emotion" -> "浅景深，突出角色表情";
                case "suspense" -> "中浅景深，保留环境压迫感";
                default -> "中等景深，兼顾角色与环境";
            });
            rule.put("color_tone", switch (sceneType) {
                case "suspense" -> "冷暗色调";
                case "emotion" -> "克制柔和色调";
                case "action" -> "高对比高饱和色调";
                case "epic" -> "宏大通透色调";
                default -> "自然统一色调";
            });
            rules.add(rule);
        }
        return rules;
    }

    private static List<ActingDirectionResult> buildLocalActingDirections(List<StoryboardPanelData> panels) {
        List<ActingDirectionResult> directions = new ArrayList<>();
        for (StoryboardPanelData panel : panels) {
            ActingDirectionResult result = new ActingDirectionResult();
            result.setPanelNumber(panel.getPanelNumber());
            ArrayNode characters = JsonNodeFactory.instance.arrayNode();
            if (panel.getCharacters() != null) {
                for (CharacterRef ref : panel.getCharacters()) {
                    ObjectNode character = characters.addObject();
                    character.put("name", firstNotBlank(ref.getName(), "角色"));
                    character.put("acting", localActingText(panel.getSceneType(), panel.getDuration()));
                }
            }
            result.setCharacters(characters);
            directions.add(result);
        }
        return directions;
    }

    private static String localActingText(String sceneType, Integer duration) {
        String performance = switch (firstNotBlank(sceneType, "daily")) {
            case "emotion" -> "表情逐步变化，先克制呼吸，再通过眼神和细微肢体释放情绪";
            case "action" -> "动作预备清晰，发力过程连贯，结束后保留重心与呼吸反馈";
            case "epic" -> "动作沉稳有力量，视线跟随环境或对手变化，保持画面张力";
            case "suspense" -> "控制呼吸与眨眼频率，用迟疑、停顿和缓慢转头制造紧张感";
            default -> "保持自然呼吸和目光交流，动作从起始状态平滑过渡到结束状态";
        };
        if (duration != null && duration >= 8) {
            performance += "，按前段、中段、后段完成三个连续表演节拍";
        }
        return performance;
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
        try {
            String panelsJson = JsonUtils.toJsonString(panels);
            String charsAgeGender = buildCharactersAgeGenderString(projectId);
            String locsDesc = buildLocationsDescString(projectId);
            String prompt = buildStoryboardDetailPrompt(panelsJson, charsAgeGender, locsDesc,
                artStyleSuffix(projectId), projectAspectRatio(projectId));
            String response;
            if (emitter != null) {
                StreamingChatModel activeStreamingModel = streamingModel != null ? streamingModel : buildStreamingChatModel();
                response = streamingChat(activeStreamingModel, chatModel, prompt, emitter, "storyboard_detail");
            } else {
                response = chatModel.chat(prompt);
            }
            List<StoryboardDetailResult> results = parseJsonArray(extractJson(response), StoryboardDetailResult.class);
            if (results != null) {
                int matched = 0;
                for (StoryboardDetailResult r : results) {
                    if (r.getPanelNumber() == null) continue;
                    int idx = r.getPanelNumber() - 1;
                    if (idx >= 0 && idx < panels.size()) {
                        StoryboardPanelData panel = panels.get(idx);
                        panel.setShotType(r.getShotType());
                        panel.setCameraMove(r.getCameraMove());
                        panel.setVideoPrompt(r.getVideoPrompt());
                        panel.setImagePrompt(r.getImagePrompt());
                        panel.setSceneTitle(r.getSceneTitle());
                        panel.setStartState(firstNotBlank(r.getStartState(), panel.getStartState()));
                        panel.setEndState(firstNotBlank(r.getEndState(), panel.getEndState()));
                        panel.setContinuityAction(firstNotBlank(r.getContinuityAction(), panel.getContinuityAction()));
                        panel.setSpatialAnchor(firstNotBlank(r.getSpatialAnchor(), panel.getSpatialAnchor()));
                        if (r.getPresentCharacters() != null && !r.getPresentCharacters().isEmpty()) {
                            panel.setPresentCharacters(r.getPresentCharacters());
                        }
                        panel.setSceneNumber(r.getSceneNumber() != null ? r.getSceneNumber() : panel.getSceneNumber());
                        panel.setSegmentNumber(r.getSegmentNumber() != null ? r.getSegmentNumber() : panel.getSegmentNumber());
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
                // 节拍校验 + 二次补写：video_prompt 节拍数低于 ⌈duration/3⌉ 时补写一次
                if (matched > 0 && streamingModel == null && emitter == null) {
                    ensureVideoPromptBeats(chatModel, panels, projectId);
                }
                if (matched == 0 && emitter != null) {
                    emit(emitter, "storyboard_detail", "error", "分镜细化JSON解析成功但未匹配到任何镜头");
                } else if (matched > 0 && emitter != null) {
                    emit(emitter, "storyboard_detail", "done", "分镜细化完成（" + matched + "/" + panels.size() + "）");
                }
            } else if (emitter != null) {
                emit(emitter, "storyboard_detail", "error", "分镜细化失败：LLM返回格式异常");
            }
        } catch (Exception e) {
            log.warn("Phase 6 分镜细化失败: {}", e.getMessage());
            if (emitter != null) emit(emitter, "storyboard_detail", "error", "分镜细化异常：" + e.getMessage());
        }
    }

    /**
     * 校验每个 panel 的 video_prompt 节拍数（按顿号/逗号/句号粗估可见动作短语），
     * 低于 ⌈duration/3⌉ 时发起一次二次 LLM 调用补写。补写后再次校验，仍不达标则保留并记 warn。
     * 仅在非流式（同步生成）模式下执行，避免流式场景重复请求。
     */
    private void ensureVideoPromptBeats(ChatModel chatModel, List<StoryboardPanelData> panels, Long projectId) {
        List<StoryboardPanelData> deficient = new ArrayList<>();
        for (StoryboardPanelData panel : panels) {
            if (StrUtil.isBlank(panel.getVideoPrompt())) continue;
            int duration = panel.getDuration() != null && panel.getDuration() > 0 ? panel.getDuration() : 6;
            int required = Math.max(2, (duration + 2) / 3);
            int actual = countBeats(panel.getVideoPrompt());
            if (actual < required) {
                deficient.add(panel);
            }
        }
        if (deficient.isEmpty()) return;
        try {
            String supplementPrompt = buildVideoPromptSupplementPrompt(deficient, artStyleSuffix(projectId), projectAspectRatio(projectId));
            String response = chatModel.chat(supplementPrompt);
            List<StoryboardDetailResult> results = parseJsonArray(extractJson(response), StoryboardDetailResult.class);
            if (results != null) {
                for (StoryboardDetailResult r : results) {
                    if (r.getPanelNumber() == null) continue;
                    int idx = r.getPanelNumber() - 1;
                    if (idx >= 0 && idx < panels.size() && StrUtil.isNotBlank(r.getVideoPrompt())) {
                        StoryboardPanelData panel = panels.get(idx);
                        if (StrUtil.isNotBlank(r.getDescription())) panel.setDescription(r.getDescription());
                        panel.setVideoPrompt(r.getVideoPrompt());
                        log.info("Phase 6 节拍补写完成 panel={} 节拍 {}->{}",
                            r.getPanelNumber(),
                            countBeats(panel.getVideoPrompt()),
                            panel.getVideoPrompt());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Phase 6 节拍补写失败: {}", e.getMessage());
        }
    }

    /** 粗估 video_prompt 的可见节拍数：按顿号、逗号、分号、句号、换行切分的动作短语数。 */
    private static int countBeats(String videoPrompt) {
        if (StrUtil.isBlank(videoPrompt)) return 0;
        String[] parts = videoPrompt.split("[、，,；;。\n]");
        int count = 0;
        for (String p : parts) {
            String t = p.trim();
            if (t.length() >= 2) count++;
        }
        return count;
    }

    private static String buildVideoPromptSupplementPrompt(List<StoryboardPanelData> deficient, String artStyle, String aspectRatio) {
        StringBuilder json = new StringBuilder();
        for (StoryboardPanelData p : deficient) {
            json.append(JsonUtils.toJsonString(p)).append(",");
        }
        if (json.length() > 0 && json.charAt(json.length() - 1) == ',') json.deleteCharAt(json.length() - 1);
        return """
            以下是 video_prompt 节拍数不足的分镜，每个镜头的 video_prompt 必须按时长写出足够可见节拍。
            按导演笔记风格重写 video_prompt（景别+机位、按时序的动作节拍、运镜、光影方向、道具、台词），禁止参数堆砌。
            duration 为 4-7 秒写 3 个连续节拍，8 秒以上按前段/中段/后段写至少 3 节拍。保留原有信息，只扩写动作细节。
            只返回 JSON 数组，字段：panel_number、video_prompt、description。

            视觉风格：%s
            画幅：%s
            待补写分镜：[%s]
            """.formatted(artStyle, aspectRatio, json);
    }

    private static String buildStoryboardDetailPrompt(String panelsJson, String charsAgeGender, String locsDesc,
                                                      String artStyle, String aspectRatio) {
        return """
            你是顶级电影分镜师。根据分镜规划和场景类型，设计镜头语言和视频提示词。

            【你的职责】
            为每个分镜设计景别、视角、镜头运动，撰写video_prompt（用年龄段+性别替代角色名），撰写image_prompt。

            【镜头语言库】
            景别：大远景/远景/全景/中景/近景/特写/极端特写
            视角：平视/仰拍/俯拍/越肩/荷兰角/主观视角
            镜头运动：固定/缓推/缓拉/跟随/急推/急拉/环绕/升起/俯冲/手持晃动

            【根据scene_type选择镜头风格】
            daily：中景、近景为主，平视+越肩，优先使用缓推/缓拉/轻微跟随
            emotion：近景、特写捕捉情绪，缓慢推进、环绕运镜
            action：景别快速切换，特写+全景交替，仰拍/俯拍/荷兰角，急推急拉/跟随/手持晃动
            epic：必须有大远景建立规模，俯拍/升起/俯冲
            suspense：主观视角/荷兰角，缓慢推进制造压迫

            【全局视觉约束 - 最高优先级】
            - 视觉风格：%s。description、video_prompt、image_prompt都必须显式体现该风格
            - 严禁使用与目标风格冲突的表达；动漫/漫画风格不得写成真人、实拍、照片级人物
            - 画面比例：%s。镜头构图、主体位置和留白必须适应该比例

            【三级结构审核与修正 - 最高优先级】
            在设计镜头语言前，先审核输入的scene_number/segment_number分组并直接在输出中修正字段：
            - 每个segment的duration总和必须≤15秒；超过时在合理剧情节点增加segment_number
            - 普通单镜5-8秒；超过8秒只允许用于不可拆的连续台词、连续动作或史诗建立镜头；任何镜头≤15秒
            - 同一segment内必须围绕同一个segment_goal，结尾落实segment_result
            - 相邻segment的bridge_out与bridge_in必须能直接连读；不匹配时重写桥梁字段和当前镜头描述
            - 检查A→C跳跃：位置、姿态、情绪、信息或道具状态缺少B过程时，必须把B过程融入相邻镜头描述
            - 不得改变镜头顺序或剧本事件顺序，不得新增剧本外剧情

            【剧情节拍落实 - 最高优先级】
            - 输入中的beat_type、narrative_cause、character_goal、story_action、story_result、next_hook必须完整保留
            - description和video_prompt必须实际拍出story_action以及story_result，不能只拍角色站立或环境氛围
            - 镜头开头先回应narrative_cause，镜头结尾必须出现story_result，并留下next_hook
            - 如果story_result无法在当前时长内可视化，必须简化动作而不是丢掉结果
            - 不得擅自加入输入因果链之外的新事件、台词、能力或角色行为

            【前后镜头连续性 - 最高优先级】
            - 输入已经包含start_state、end_state、continuity_action、spatial_anchor、present_characters，必须完整保留并落实到description/video_prompt/image_prompt
            - 当前镜头开头必须明确从start_state开始，结尾必须准确停在end_state
            - video_prompt必须按“继承上一镜状态→执行当前动作→停在可供下一镜承接的状态”书写
            - 同场景相邻镜头不得改变人物左右关系、朝向、道具所在手、站坐姿态、伤势和光线方向
            - 反打/特写可以不显示其他人，但不得暗示其他人消失或换位
            - 如果输入连续性字段冲突，优先修正当前镜头以承接上一镜头，不得各镜头独立创作

            【时长匹配规则 - 最高优先级】
            - 输入中的duration就是目标视频秒数，video_prompt必须完整覆盖该时长
            - duration为4-7秒：至少写清开始动作、持续变化、结束状态
            - duration为8秒及以上：必须按“前段→中段→后段”写至少3个连续可见节拍，并包含持续运镜或环境变化
            - 禁止用一个瞬时动作支撑长镜头，例如仅“吐信、转头、抬手”却标10秒
            - description也必须同步扩写这些节拍，保证画面描述与video_prompt一致

            【video_prompt撰写规则 - 重要】
            video_prompt 是发给视频模型的核心可拍指令，必须用"导演笔记"风格写，每个字都可拍、按时序展开。视频模型不认识名字，必须用年龄段+性别替代角色：
            - 年龄段：少年/少女(10-16)、年轻男子/年轻女子(17-30)、中年男子/中年女子(31-50)、老年男子/老年女子(50+)
            - 必须依次包含以下可拍维度：
              1) 景别+机位架设位置：如"中景，机位架设在店内深处正对门口"
              2) 主体动作（按时序）：按 duration 分档写连续可见节拍——4-7秒写"开始动作→持续变化→结束状态"；8秒及以上按"前段→中段→后段"写至少3个连续可见动作。每秒至少一个可见动作变化，禁止用一个瞬时动作支撑长镜头
              3) 运镜：从镜头运动词库选一个主导运镜（推/拉/摇/移/跟/环绕/手持/固定），写明方向与节奏，禁止只用"缓缓"这种无信息量词
              4) 光影：方向+色温+阴影色，与当前镜头光源位置绑死（如"晨光从卷帘门缝隙逆光射入，发丝边缘泛柔光，店内深处阴影偏冷蓝"），禁止情绪词
              5) 道具/穿着：从 source_text 和角色设定提取具体道具与穿着，写进动作流
              6) 台词：若 source_text 有台词，以「角色说的话」标注并标语气（小声/平淡/恳求），供后续口型对齐；无台词则不写
            - 禁止参数堆砌（8K/HDR/fps/Rec.色域这类），视频模型不认
            - 禁止纯静态描述，特写镜头必须使用"固定镜头"
            - description 必须与 video_prompt 的节拍、光影、动作一致

            【动态优先原则 - 核心规则】
            视频不能僵硬！每个video_prompt必须按时序含可见动作。即使对话场景也要有动作变化。
            ✅ 正确示例（6秒、2节拍）："中景，机位架设在店内深处正对门口。年轻女子从右侧卷帘门推门进入，门推开约45°，晨光从门缝逆光射入在她发丝边缘形成柔光晕。她站定在门口，身体微前倾又顿住，手里攥着一张揉皱的纸，眼神在店内游移后落向画面中央偏左的座位。镜头手持跟随，从门口缓推至她停步处，保持中景距离。她小声开口：「那个……这里理发吗？」"
            ❌ 错误："年轻女子坐在沙发上，镜头固定"（无节拍、无光影、无运镜节奏）

            【image_prompt撰写规则】
            - 使用角色实际名字（不是年龄段+性别）
            - 包含角色位置、场景环境、光线氛围
            - 纯视觉描述，禁止抽象词

            【输出格式】
            只返回JSON数组。保留输入中的所有原始字段，补充shot_type、camera_move、video_prompt、image_prompt、sceneTitle：

            {
              "panel_number": 1,
              "shot_type": "平视中景",
              "camera_move": "缓推",
              "description": "从start_state开始并停在end_state的优化画面描述",
              "start_state": "继承的镜头起始状态",
              "end_state": "可供下一镜承接的镜头结束状态",
              "continuity_action": "延续到下一镜头的动作或视线",
              "spatial_anchor": "人物左右前后与朝向",
              "present_characters": ["张三"],
              "beat_type": "action",
              "narrative_cause": "上一镜头留下的直接原因",
              "character_goal": "当前角色的具体目标",
              "story_action": "镜头中执行的关键动作",
              "story_result": "动作造成的剧情变化",
              "next_hook": "下一镜必须回应的后果",
              "video_prompt": "中景，机位架设在办公室深处正对会议桌。年轻男子站在深棕色实木桌前，双手撑在桌面上，身体微前倾，目光扫过桌前众人后停住。镜头从中景手持缓推至他上半身，保持平视距离，约两秒推到位后固定。午后阳光从右侧窗户斜照进来，在他脸侧形成暖侧光，桌面阴影偏冷。他抬头环视，低声开口：「开会了。」",
              "image_prompt": "张三站在办公室中央，双手撑在深棕色实木桌面上，表情严肃，午后阳光从右侧窗户斜照进来",
              "sceneTitle": "张三宣布开会",
              "characters": [{"name":"张三","appearance":"初始形象","slot":"办公室中央"}],
              "location": "办公室_白天",
              "scene_type": "daily",
              "source_text": "张三对大家说：开会了"
            }
            ⚠️ JSON安全：严格遵守JSON标准格式。字符串值内的双引号必须转义为\"。对话引号统一使用「」代替英文双引号。特写镜头必须使用固定镜头。

            分镜规划：%s

            角色年龄性别信息：%s

            场景描述：%s
            """.formatted(artStyle, aspectRatio, panelsJson, charsAgeGender, locsDesc);
    }

    // ==================== 持久化 ====================

    private List<ShortDramaStoryboardVo> persistStoryboards(Long projectId, Long scriptId, List<StoryboardPanelData> panels, ShortDramaScript script) {
        List<ShortDramaStoryboardVo> result = new ArrayList<>();
        int sceneNo = 1;
        for (StoryboardPanelData panel : panels) {
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
            entity.setVideoPrompt(firstNotBlank(panel.getVideoPrompt(), buildVideoPromptFallback(script, panel.getDescription(), sceneNo, panel.getSceneType(), panel.getDuration())));
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
        return result;
    }

    // ==================== 辅助方法：资产库字符串构建 ====================

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
        List<ChatModelVo> models = chatModelService.queryList(query);
        if (models != null && !models.isEmpty()) {
            return models.get(0);
        }
        throw new IllegalArgumentException("无可用聊天模型");
    }

    private AbstractChatService getChatService(ChatModelVo modelVo) {
        return chatServiceFactory.getOriginalService(modelVo.getProviderCode());
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
        if (!org.ruoyi.enums.ModelType.AUDIO.getKey().equals(modelVo.getCategory())) {
            throw new IllegalArgumentException("模型分类不是语音模型: " + audioModel);
        }

        // 对白类型：从关联镜头收集出场角色及其子形象音色，作为 references + @audioN 标记
        List<java.util.Map<String, String>> references = buildAudioReferences(audio);
        AudioContext ctx = AudioContext.builder()
            .chatModelVo(modelVo)
            .input(audio.getText())
            .voice(StrUtil.isBlank(audio.getVoice()) ? null : audio.getVoice())
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

    /**
     * 对白类型音频：从关联镜头的出场角色收集子形象音色，构造 references（speaker）。
     * text 中可用 @audioN 引用对应角色。旁白类型返回空列表。
     */
    private List<java.util.Map<String, String>> buildAudioReferences(ShortDramaAudio audio) {
        if (!"dialogue".equals(audio.getAudioType()) || audio.getLinkedStoryboardId() == null) {
            return List.of();
        }
        ShortDramaStoryboard sb = storyboardMapper.selectById(audio.getLinkedStoryboardId());
        if (sb == null) return List.of();
        List<CharacterRef> refs = parseCharacterRefs(sb.getCharactersJson());
        if (refs == null || refs.isEmpty()) return List.of();
        List<java.util.Map<String, String>> result = new ArrayList<>();
        for (CharacterRef ref : refs) {
            ShortDramaCharacter ch = findCharacterByName(sb.getProjectId(), ref.getName());
            if (ch == null) continue;
            // 取该角色的主形象（appearanceIndex=0）音色
            List<ShortDramaCharacterAppearance> aps = characterAppearanceMapper.selectList(
                new LambdaQueryWrapper<ShortDramaCharacterAppearance>()
                    .eq(ShortDramaCharacterAppearance::getCharacterId, ch.getId())
                    .orderByAsc(ShortDramaCharacterAppearance::getAppearanceIndex));
            for (ShortDramaCharacterAppearance ap : aps) {
                if (StrUtil.isNotBlank(ap.getVoice())) {
                    java.util.Map<String, String> r = new LinkedHashMap<>();
                    r.put("speaker", ap.getVoice());
                    result.add(r);
                    break;
                }
            }
        }
        return result;
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
        return chatService.buildStreamingChatModel(modelVo, new ChatRequest());
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

    private static void normalizeContinuityChain(List<StoryboardPanelData> panels) {
        StoryboardPanelData previous = null;
        int sceneNumber = 1;
        int segmentNumber = 1;
        int segmentDuration = 0;
        String previousLocation = null;
        for (StoryboardPanelData current : panels) {
            String currentLocation = firstNotBlank(current.getLocation(), "");
            if (previousLocation != null && !previousLocation.equals(currentLocation)) {
                sceneNumber++;
                segmentNumber = 1;
                segmentDuration = 0;
            }
            int duration = current.getDuration() != null && current.getDuration() > 0
                ? Math.max(5, Math.min(current.getDuration(), 15)) : 6;
            current.setDuration(duration);
            boolean requestedNewSegment = current.getSegmentNumber() != null && current.getSegmentNumber() > segmentNumber;
            if (previous != null && (requestedNewSegment || segmentDuration + duration > 15)) {
                segmentNumber++;
                segmentDuration = 0;
            }
            current.setSceneNumber(sceneNumber);
            current.setSegmentNumber(segmentNumber);
            segmentDuration += duration;
            previousLocation = currentLocation;
            if (previous == null) {
                current.setStartState(firstNotBlank(current.getStartState(), "新场景建立，按当前描述确定人物初始位置与姿态"));
                current.setNarrativeCause(firstNotBlank(current.getNarrativeCause(), "剧本开场建立人物、目标或冲突"));
                current.setBridgeIn(firstNotBlank(current.getBridgeIn(), "开场建立"));
            } else {
                String previousConsequence = firstNotBlank(previous.getStoryResult(), previous.getNextHook(), previous.getContinuityAction(), previous.getEndState());
                current.setNarrativeCause(previousConsequence);
                if (current.getSegmentNumber().equals(previous.getSegmentNumber()) && current.getSceneNumber().equals(previous.getSceneNumber())) {
                    current.setBridgeIn(firstNotBlank(current.getBridgeIn(), previous.getContinuityAction(), previous.getNextHook(), previous.getEndState()));
                } else {
                    current.setBridgeIn(firstNotBlank(current.getBridgeIn(), previous.getBridgeOut(), previous.getNextHook(), previous.getStoryResult()));
                    previous.setBridgeOut(firstNotBlank(previous.getBridgeOut(), current.getBridgeIn()));
                }
            }
            if (previous != null && firstNotBlank(previous.getLocation(), "").equals(firstNotBlank(current.getLocation(), ""))) {
                String inheritedState = firstNotBlank(previous.getEndState(), previous.getDescription(), previous.getSourceText(), "");
                current.setStartState(inheritedState);
                if (StrUtil.isBlank(current.getSpatialAnchor())) {
                    current.setSpatialAnchor(previous.getSpatialAnchor());
                }
                if ((current.getPresentCharacters() == null || current.getPresentCharacters().isEmpty())
                    && previous.getPresentCharacters() != null) {
                    current.setPresentCharacters(new ArrayList<>(previous.getPresentCharacters()));
                }
            } else {
                current.setStartState("新场景建立：" + firstNotBlank(current.getLocation(), "新地点") + "，重新交代人物位置、朝向与环境");
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
            if (current.getPresentCharacters() == null || current.getPresentCharacters().isEmpty()) {
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
        return continuity.toString();
    }

    private static void applyContinuityJson(StoryboardPanelData panel, String continuityJson) {
        if (StrUtil.isBlank(continuityJson)) return;
        try {
            JsonNode continuity = JsonUtils.parseObject(continuityJson, JsonNode.class);
            panel.setStartState(continuity.path("start_state").asText(null));
            panel.setEndState(continuity.path("end_state").asText(null));
            panel.setContinuityAction(continuity.path("continuity_action").asText(null));
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

    /** 按 scene_type 选具体运镜描述，消除"缓缓推近"这类无信息量词。 */
    private static String cameraMoveForScene(String sceneType) {
        return switch (firstNotBlank(sceneType, "daily")) {
            case "action" -> "手持跟移，允许轻微晃动，快速横移跟随动作";
            case "emotion" -> "从中景缓推至近景，两秒到位后固定，聚焦面部";
            case "epic" -> "大远景缓拉升起，展现环境规模后停住";
            case "suspense" -> "斯坦尼康式低速缓推，略带左右摇摆模拟紧张";
            default -> "平视中景手持缓推，约两秒到位后固定";
        };
    }

    /**
     * Phase 6 失败时的兜底 video_prompt。按 duration 分档写可拍节拍，
     * 不再一句"镜头缓缓推近，自然光线"。
     */
    private static String buildVideoPromptFallback(ShortDramaScript script, String text, int sceneNo,
                                                    String sceneType, Integer duration) {
        String tone = firstNotBlank(script.getTone(), "短剧");
        String camera = cameraMoveForScene(sceneType);
        String light = switch (firstNotBlank(sceneType, "daily")) {
            case "suspense" -> "低调硬光，保留阴影层次";
            case "emotion" -> "柔和侧光，突出面部情绪";
            case "action" -> "高反差侧光，强化动作轮廓";
            case "epic" -> "大范围自然光，突出空间规模";
            default -> "自然柔光，主光从画面侧上方斜照";
        };
        int dur = duration != null && duration > 0 ? duration : 6;
        String beats;
        if (dur >= 8) {
            beats = "前段：" + text + "；中段：动作持续变化、视线或姿态推进；后段：收束在结束状态，留出下一镜承接";
        } else {
            beats = "开始动作：" + text + "；持续变化：动作与视线推进；结束状态：收束停住";
        }
        return "中景，镜头" + camera + "。" + beats + "。" + tone + "风格。" + light + "。短剧镜头" + sceneNo;
    }

    private static String buildVideoPromptFallback(ShortDramaScript script, String text, int sceneNo) {
        return buildVideoPromptFallback(script, text, sceneNo, "daily", 6);
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
        String json = extractJson(response);
        if (StrUtil.isBlank(json)) return null;
        try {
            if (json.trim().startsWith("[")) {
                return JsonUtils.parseArray(json, StoryboardPanelData.class);
            }
        } catch (Exception e) {
            log.warn("分镜规划JSON解析失败: {}", e.getMessage());
        }
        return null;
    }

    // ==================== 内部 DTO ====================

    @Data
    public static class StoryboardPanelData {
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
    }

    @Data
    static class ActingDirectionResult {
        @JsonProperty("panel_number")
        private Integer panelNumber;
        private JsonNode characters;
    }

    @Data
    private static class StoryboardDetailResult {
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
        return ShortDramaImageConstants.artStylePrompt(project.getArtStyle());
    }

    /**
     * 拼装角色生图最终 prompt：风格前缀 + 构图前缀 + 角色描述 + 三视图后缀 + 风格后缀。
     * 把风格词放在 prompt 最前面，避免被角色描述稀释导致画风漂移、每个角色风格不一致。
     */
    private String buildCharacterPrompt(String basePrompt, Long projectId) {
        String styleSuffix = artStyleSuffix(projectId);
        String stylePrefix = StrUtil.isNotBlank(styleSuffix) ? styleSuffix + "，" : "";
        return stylePrefix + ShortDramaImageConstants.CHARACTER_PROMPT_PREFIX
            + basePrompt + ShortDramaImageConstants.CHARACTER_PROMPT_SUFFIX;
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
