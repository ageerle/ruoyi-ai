package org.ruoyi.controller.shortdrama;

import cn.dev33.satoken.annotation.SaCheckPermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterAppearanceBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaComposeVideoBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaAudioBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaLocationBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaProjectBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaStoryboardBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaIdeaBo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterAppearanceVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaComposeVideoVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaAudioVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaLocationVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaProjectVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaScriptVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaStoryboardVo;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.ruoyi.service.shortdrama.IShortDramaVideoComposeService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/short-drama")
public class ShortDramaController {

    private final IShortDramaService shortDramaService;

    private final IShortDramaVideoComposeService videoComposeService;

    private final OssService ossService;

    // ==================== 项目 ====================

    @GetMapping("/projects")
    public R<List<ShortDramaProjectVo>> projects() {
        return R.ok(shortDramaService.listProjects(LoginHelper.getUserId()));
    }

    @GetMapping("/{projectId}")
    public R<ShortDramaDetailVo> detail(@PathVariable Long projectId) {
        return R.ok(shortDramaService.getDetail(projectId, LoginHelper.getUserId()));
    }

    @PostMapping("/create-from-idea")
    public R<ShortDramaDetailVo> createFromIdea(@Valid @RequestBody ShortDramaIdeaBo bo) {
        return R.ok(shortDramaService.createFromIdea(bo, LoginHelper.getUserId()));
    }

    /** SSE 流式创建：逐阶段推送进度，避免用户等待焦虑 */
    @PostMapping("/create-from-idea/stream")
    public SseEmitter createFromIdeaStream(@Valid @RequestBody ShortDramaIdeaBo bo) {
        return shortDramaService.createFromIdeaStream(bo, LoginHelper.getUserId());
    }

    @PostMapping("/project")
    public R<Long> saveProject(@Valid @RequestBody ShortDramaProjectBo bo) {
        return R.ok(shortDramaService.saveProject(bo, LoginHelper.getUserId()));
    }

    @PutMapping("/project")
    public R<Long> updateProject(@Valid @RequestBody ShortDramaProjectBo bo) {
        return R.ok(shortDramaService.saveProject(bo, LoginHelper.getUserId()));
    }

    @DeleteMapping("/project/{projectId}")
    public R<Void> deleteProject(@NotNull @PathVariable Long projectId) {
        shortDramaService.deleteProject(projectId, LoginHelper.getUserId());
        return R.ok();
    }

    // ==================== 剧本 ====================

    @PostMapping("/script")
    public R<ShortDramaScriptVo> saveScript(@Valid @RequestBody ShortDramaScriptBo bo) {
        return R.ok(shortDramaService.saveScript(bo, LoginHelper.getUserId()));
    }

    // ==================== 分镜 ====================

    @PostMapping("/storyboards/generate")
    public R<List<ShortDramaStoryboardVo>> generate(@NotNull @RequestParam Long projectId,
                                                    @NotNull @RequestParam Long scriptId,
                                                    @RequestParam(required = false) String model) {
        return R.ok(shortDramaService.generateStoryboards(projectId, scriptId, model, LoginHelper.getUserId()));
    }

    @PostMapping("/storyboard")
    public R<ShortDramaStoryboardVo> saveStoryboard(@Valid @RequestBody ShortDramaStoryboardBo bo) {
        return R.ok(shortDramaService.saveStoryboard(bo, LoginHelper.getUserId()));
    }

    @PostMapping("/storyboard/{storyboardId}/generate-video")
    public R<ShortDramaStoryboardVo> generateVideo(@NotNull @PathVariable Long storyboardId,
                                                    @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.generateVideo(storyboardId, model, LoginHelper.getUserId()));
    }

    @GetMapping("/storyboard/{storyboardId}/video-result")
    public R<ShortDramaStoryboardVo> videoResult(@NotNull @PathVariable Long storyboardId,
                                                  @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.retrieveVideo(storyboardId, model, LoginHelper.getUserId()));
    }

    @PostMapping("/{projectId}/generate-all-videos")
    public R<List<ShortDramaStoryboardVo>> generateAllVideos(@NotNull @PathVariable Long projectId,
                                                              @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.generateAllVideos(projectId, model, LoginHelper.getUserId()));
    }

    @PostMapping("/{projectId}/compose-video")
    public R<ShortDramaComposeVideoVo> composeVideo(@NotNull @PathVariable Long projectId,
                                                     @Valid @RequestBody ShortDramaComposeVideoBo bo) {
        return R.ok(videoComposeService.composeVideo(projectId, bo, LoginHelper.getUserId()));
    }

    @GetMapping("/{projectId}/compose-video")
    public R<ShortDramaComposeVideoVo> getComposedVideo(@NotNull @PathVariable Long projectId) {
        return R.ok(videoComposeService.getComposedVideo(projectId, LoginHelper.getUserId()));
    }

    @GetMapping("/{projectId}/compose-video/download")
    public void downloadComposedVideo(@NotNull @PathVariable Long projectId, HttpServletResponse response)
        throws IOException {
        ShortDramaComposeVideoVo composition = videoComposeService.getComposedVideo(projectId, LoginHelper.getUserId());
        if (composition == null || !"done".equals(composition.getStatus())) {
            throw new ServiceException("成片尚未生成完成");
        }
        if (composition.getVideoOssId() != null) {
            ossService.downloadFile(composition.getVideoOssId(), response);
            return;
        }
        Path localVideo = videoComposeService.getLocalComposedVideo(projectId, LoginHelper.getUserId());
        response.setContentType("video/mp4");
        response.setHeader("Content-Disposition", "attachment; filename=short-drama-" + projectId + ".mp4");
        response.setContentLengthLong(Files.size(localVideo));
        Files.copy(localVideo, response.getOutputStream());
    }

    // ==================== 阶段式流水线端点 ====================

    /** Phase 1: 剧本打磨 */
    @PostMapping("/{projectId}/polish-script")
    public R<ShortDramaDetailVo> polishScript(@NotNull @PathVariable Long projectId) {
        return R.ok(shortDramaService.polishScript(projectId, LoginHelper.getUserId()));
    }

    // ==================== 资产分析 ====================

    /** Phase 2: 资产分析（角色+场景提取） */
    @PostMapping("/{projectId}/analyze-assets")
    public R<ShortDramaDetailVo> analyzeAssets(@NotNull @PathVariable Long projectId,
                                               @NotNull @RequestParam Long scriptId) {
        return R.ok(shortDramaService.analyzeAssets(projectId, scriptId, LoginHelper.getUserId()));
    }

    // ==================== 分镜流水线 ====================

    /** Phase 3-6: 分镜规划+摄影规则+表演指导+分镜细化 */
    @PostMapping("/{projectId}/plan-storyboard")
    public R<List<ShortDramaStoryboardVo>> planStoryboard(@NotNull @PathVariable Long projectId,
                                                           @NotNull @RequestParam Long scriptId,
                                                           @RequestParam(required = false) String model) {
        return R.ok(shortDramaService.planStoryboard(projectId, scriptId, model, LoginHelper.getUserId()));
    }

    /** Phase 3-6: SSE 流式生成分镜，持续推送规划和细化进度 */
    @PostMapping("/{projectId}/plan-storyboard/stream")
    public SseEmitter planStoryboardStream(@NotNull @PathVariable Long projectId,
                                            @NotNull @RequestParam Long scriptId,
                                            @RequestParam(required = false) String model) {
        return shortDramaService.planStoryboardStream(projectId, scriptId, model, LoginHelper.getUserId());
    }

    /** Phase 4: 重新生成摄影规则 */
    @PostMapping("/{projectId}/photography-rules")
    public R<List<ShortDramaStoryboardVo>> generatePhotographyRules(@NotNull @PathVariable Long projectId,
                                                                     @NotNull @RequestParam Long scriptId) {
        return R.ok(shortDramaService.generatePhotographyRules(projectId, scriptId, LoginHelper.getUserId()));
    }

    /** Phase 5: 重新生成表演指导 */
    @PostMapping("/{projectId}/acting-directions")
    public R<List<ShortDramaStoryboardVo>> generateActingDirections(@NotNull @PathVariable Long projectId,
                                                                     @NotNull @RequestParam Long scriptId) {
        return R.ok(shortDramaService.generateActingDirections(projectId, scriptId, LoginHelper.getUserId()));
    }

    // ==================== 角色管理 ====================

    @PostMapping("/character")
    public R<ShortDramaCharacterVo> saveCharacter(@Valid @RequestBody ShortDramaCharacterBo bo) {
        return R.ok(shortDramaService.saveCharacter(bo, LoginHelper.getUserId()));
    }

    @PutMapping("/character")
    public R<ShortDramaCharacterVo> updateCharacter(@Valid @RequestBody ShortDramaCharacterBo bo) {
        return R.ok(shortDramaService.saveCharacter(bo, LoginHelper.getUserId()));
    }

    @DeleteMapping("/character/{characterId}")
    public R<Void> deleteCharacter(@NotNull @PathVariable Long characterId) {
        shortDramaService.deleteCharacter(characterId, LoginHelper.getUserId());
        return R.ok();
    }

    @PostMapping("/character/{characterId}/generate-image")
    public R<ShortDramaCharacterVo> generateCharacterImage(@NotNull @PathVariable Long characterId,
                                                            @NotBlank @RequestParam String model,
                                                            @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.generateCharacterImage(characterId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    // ==================== 角色形象管理 ====================

    @PostMapping("/character-appearance")
    public R<ShortDramaCharacterAppearanceVo> saveAppearance(@Valid @RequestBody ShortDramaCharacterAppearanceBo bo) {
        return R.ok(shortDramaService.saveAppearance(bo, LoginHelper.getUserId()));
    }

    @PutMapping("/character-appearance")
    public R<ShortDramaCharacterAppearanceVo> updateAppearance(@Valid @RequestBody ShortDramaCharacterAppearanceBo bo) {
        return R.ok(shortDramaService.saveAppearance(bo, LoginHelper.getUserId()));
    }

    @DeleteMapping("/character-appearance/{appearanceId}")
    public R<Void> deleteAppearance(@NotNull @PathVariable Long appearanceId) {
        shortDramaService.deleteAppearance(appearanceId, LoginHelper.getUserId());
        return R.ok();
    }

    @PostMapping("/character-appearance/{appearanceId}/generate-image")
    public R<ShortDramaCharacterAppearanceVo> generateAppearanceImage(@NotNull @PathVariable Long appearanceId,
                                                                       @NotBlank @RequestParam String model,
                                                                       @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.generateAppearanceImage(appearanceId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    @PostMapping("/character-appearance/{appearanceId}/regenerate")
    public R<ShortDramaCharacterAppearanceVo> regenerateAppearanceImage(@NotNull @PathVariable Long appearanceId,
                                                                         @NotBlank @RequestParam String model,
                                                                         @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.regenerateAppearanceImage(appearanceId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    @PostMapping("/character-appearance/{appearanceId}/select-image")
    public R<ShortDramaCharacterAppearanceVo> selectAppearanceImage(@NotNull @PathVariable Long appearanceId,
                                                                     @NotNull @RequestParam Integer index) {
        return R.ok(shortDramaService.selectAppearanceImage(appearanceId, index, LoginHelper.getUserId()));
    }

    @DeleteMapping("/character-appearance/{appearanceId}/image")
    public R<ShortDramaCharacterAppearanceVo> deleteAppearanceImage(@NotNull @PathVariable Long appearanceId,
                                                                     @NotNull @RequestParam Integer index) {
        return R.ok(shortDramaService.deleteAppearanceImage(appearanceId, index, LoginHelper.getUserId()));
    }

    @PostMapping("/character-appearance/{appearanceId}/undo-image")
    public R<ShortDramaCharacterAppearanceVo> undoAppearanceImage(@NotNull @PathVariable Long appearanceId) {
        return R.ok(shortDramaService.undoAppearanceImage(appearanceId, LoginHelper.getUserId()));
    }

    // ==================== 场景管理 ====================

    @PostMapping("/location")
    public R<ShortDramaLocationVo> saveLocation(@Valid @RequestBody ShortDramaLocationBo bo) {
        return R.ok(shortDramaService.saveLocation(bo, LoginHelper.getUserId()));
    }

    @PutMapping("/location")
    public R<ShortDramaLocationVo> updateLocation(@Valid @RequestBody ShortDramaLocationBo bo) {
        return R.ok(shortDramaService.saveLocation(bo, LoginHelper.getUserId()));
    }

    @DeleteMapping("/location/{locationId}")
    public R<Void> deleteLocation(@NotNull @PathVariable Long locationId) {
        shortDramaService.deleteLocation(locationId, LoginHelper.getUserId());
        return R.ok();
    }

    @PostMapping("/location/{locationId}/generate-image")
    public R<ShortDramaLocationVo> generateLocationImage(@NotNull @PathVariable Long locationId,
                                                          @NotBlank @RequestParam String model,
                                                          @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.generateLocationImage(locationId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    @PostMapping("/location/{locationId}/regenerate")
    public R<ShortDramaLocationVo> regenerateLocationImage(@NotNull @PathVariable Long locationId,
                                                            @NotBlank @RequestParam String model,
                                                            @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.regenerateLocationImage(locationId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    @PostMapping("/location/{locationId}/select-image")
    public R<ShortDramaLocationVo> selectLocationImage(@NotNull @PathVariable Long locationId,
                                                        @NotNull @RequestParam Integer index) {
        return R.ok(shortDramaService.selectLocationImage(locationId, index, LoginHelper.getUserId()));
    }

    @DeleteMapping("/location/{locationId}/image")
    public R<ShortDramaLocationVo> deleteLocationImage(@NotNull @PathVariable Long locationId,
                                                        @NotNull @RequestParam Integer index) {
        return R.ok(shortDramaService.deleteLocationImage(locationId, index, LoginHelper.getUserId()));
    }

    @PostMapping("/location/{locationId}/undo-image")
    public R<ShortDramaLocationVo> undoLocationImage(@NotNull @PathVariable Long locationId) {
        return R.ok(shortDramaService.undoLocationImage(locationId, LoginHelper.getUserId()));
    }

    // ==================== 语音资产管理 ====================

    @PostMapping("/audio")
    public R<ShortDramaAudioVo> saveAudio(@Valid @RequestBody ShortDramaAudioBo bo) {
        return R.ok(shortDramaService.saveAudio(bo, LoginHelper.getUserId()));
    }

    @PutMapping("/audio")
    public R<ShortDramaAudioVo> updateAudio(@Valid @RequestBody ShortDramaAudioBo bo) {
        return R.ok(shortDramaService.saveAudio(bo, LoginHelper.getUserId()));
    }

    @DeleteMapping("/audio/{audioId}")
    public R<Void> deleteAudio(@NotNull @PathVariable Long audioId) {
        shortDramaService.deleteAudio(audioId, LoginHelper.getUserId());
        return R.ok();
    }

    @GetMapping("/audio/list")
    public R<List<ShortDramaAudioVo>> listAudios(@NotNull @RequestParam Long projectId) {
        return R.ok(shortDramaService.listAudios(projectId, LoginHelper.getUserId()));
    }

    @PostMapping("/audio/{audioId}/generate-speech")
    public R<ShortDramaAudioVo> generateAudio(@NotNull @PathVariable Long audioId,
                                               @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.generateAudio(audioId, model, LoginHelper.getUserId()));
    }

    // ==================== 异步图片生成（轮询进度） ====================

    /** 上传本地照片到图片供应商，返回当前生成会话使用的临时 URL。 */
    @PostMapping(value = "/image/upload-reference", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public R<String> uploadReferenceImage(@RequestPart("file") MultipartFile file,
                                          @NotBlank @RequestParam String model) {
        String temporaryUrl = shortDramaService.uploadReferenceImage(file, model, LoginHelper.getUserId());
        return R.ok("上传成功", temporaryUrl);
    }

    /** 异步启动图片生成，返回 predictionId 供前端轮询 */
    @PostMapping("/image/start")
    public R<MediaGenerationResponse> startImage(@NotBlank @RequestParam String assetType,
                                                  @NotNull @RequestParam Long assetId,
                                                  @NotBlank @RequestParam String model,
                                                  @RequestParam(required = false) String referenceImageUrl) {
        return R.ok(shortDramaService.startImageGeneration(assetType, assetId, model, referenceImageUrl, LoginHelper.getUserId()));
    }

    /** 轮询确认形象图片并保存 */
    @PostMapping("/character-appearance/{id}/confirm-image")
    public R<ShortDramaCharacterAppearanceVo> confirmAppearanceImage(@NotNull @PathVariable Long id,
                                                                      @NotBlank @RequestParam String predictionId,
                                                                      @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.confirmAppearanceImage(id, predictionId, model, LoginHelper.getUserId()));
    }

    /** 轮询确认场景图片并保存 */
    @PostMapping("/location/{id}/confirm-image")
    public R<ShortDramaLocationVo> confirmLocationImage(@NotNull @PathVariable Long id,
                                                         @NotBlank @RequestParam String predictionId,
                                                         @NotBlank @RequestParam String model) {
        return R.ok(shortDramaService.confirmLocationImage(id, predictionId, model, LoginHelper.getUserId()));
    }
}
