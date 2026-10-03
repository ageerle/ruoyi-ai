package org.ruoyi.service.shortdrama;

import org.ruoyi.common.chat.entity.media.MediaGenerationResponse;
import org.ruoyi.domain.bo.shortdrama.ShortDramaAudioBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaCharacterAppearanceBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaLocationBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaProjectBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaScriptBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaStoryboardBo;
import org.ruoyi.domain.bo.shortdrama.ShortDramaIdeaBo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaAudioVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaCharacterAppearanceVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaLocationVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaProjectVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaScriptVo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaStoryboardVo;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IShortDramaService {

    List<ShortDramaProjectVo> listProjects(Long userId);

    ShortDramaDetailVo getDetail(Long projectId, Long userId);

    ShortDramaDetailVo createFromIdea(ShortDramaIdeaBo bo, Long userId);

    /** 流式创建短剧，通过 SSE 推送各阶段进度 */
    SseEmitter createFromIdeaStream(ShortDramaIdeaBo bo, Long userId);

    Long saveProject(ShortDramaProjectBo bo, Long userId);

    ShortDramaScriptVo saveScript(ShortDramaScriptBo bo, Long userId);

    List<ShortDramaStoryboardVo> generateStoryboards(Long projectId, Long scriptId, String model, Long userId);

    ShortDramaStoryboardVo saveStoryboard(ShortDramaStoryboardBo bo, Long userId);

    ShortDramaStoryboardVo addStoryboard(Long projectId, Long scriptId, Long afterId, Long userId);

    void deleteStoryboard(Long storyboardId, Long userId);

    ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId);

    ShortDramaStoryboardVo generateVideo(Long storyboardId, String videoModel, Long userId, String requestId, boolean regenerate);

    org.ruoyi.service.shortdrama.impl.ShortDramaVideoSubmissionStore.Submission videoSubmission(Long storyboardId, String requestId, Long userId);

    ShortDramaStoryboardVo retrieveVideo(Long storyboardId, String videoModel, Long userId);

    /** 代理流式输出视频，绕过 OSS 强制下载 header */
    StreamingResponseBody streamVideo(Long storyboardId, Long userId);

    List<ShortDramaStoryboardVo> generateAllVideos(Long projectId, String videoModel, Long userId);

    List<ShortDramaStoryboardVo> generateAllVideos(Long projectId, String videoModel, Long userId, Integer sceneStart, Integer sceneCount);

    ShortDramaDetailVo analyzeAssets(Long projectId, Long scriptId, Long userId, String model);
    SseEmitter analyzeAssetsStream(Long projectId, Long scriptId, Long userId, String model, String requestId);
    java.util.Map<String, Object> assetAnalysisStatus(Long projectId, Long scriptId, String requestId, Long userId);

    /** 按修改意见重新生成固定格式剧本 */
    ShortDramaDetailVo polishScript(Long projectId, Long userId, String instruction);

    /** Phase 3-6: 分镜规划+摄影规则+表演指导+分镜细化 */
    List<ShortDramaStoryboardVo> planStoryboard(Long projectId, Long scriptId, String model, Long userId);
    List<ShortDramaStoryboardVo> planStoryboard(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds);

    /** Phase 3-6: 流式生成分镜并推送实时进度 */
    SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId);
    SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds);
    SseEmitter planStoryboardStream(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds, String requestId);
    java.util.Map<String, Object> storyboardPlanningStatus(Long projectId, Long scriptId, String requestId, Long userId);
    java.util.Map<String, Object> storyboardCandidates(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds);

    int importReviewedPlan(Long projectId, org.ruoyi.domain.bo.shortdrama.ShortDramaReviewedPlanBo review, Long userId);

    java.util.Map<String, Object> storyboardCheckpointStatus(Long projectId, Long scriptId, String model, Long userId);
    java.util.Map<String, Object> storyboardCheckpointStatus(Long projectId, Long scriptId, String model, Long userId, Integer minimumShotSeconds);

    /** Phase 4: 重新生成摄影规则 */
    List<ShortDramaStoryboardVo> generatePhotographyRules(Long projectId, Long scriptId, Long userId);

    /** Phase 5: 重新生成表演指导 */
    List<ShortDramaStoryboardVo> generateActingDirections(Long projectId, Long scriptId, Long userId);

    ShortDramaCharacterVo saveCharacter(ShortDramaCharacterBo bo, Long userId);

    ShortDramaLocationVo saveLocation(ShortDramaLocationBo bo, Long userId);

    ShortDramaCharacterVo generateCharacterImage(Long characterId, String imageModel, String referenceImageUrl, Long userId);

    ShortDramaLocationVo generateLocationImage(Long locationId, String imageModel, String referenceImageUrl, Long userId);
    ShortDramaLocationVo generateLocationImage(Long locationId, String imageModel, String referenceImageUrl, String revisionRequirements, Long userId);

    Boolean deleteCharacter(Long characterId, Long userId);

    Boolean deleteLocation(Long locationId, Long userId);

    ShortDramaCharacterAppearanceVo saveAppearance(ShortDramaCharacterAppearanceBo bo, Long userId);

    Boolean deleteAppearance(Long appearanceId, Long userId);

    ShortDramaCharacterAppearanceVo generateAppearanceImage(Long appearanceId, String imageModel, String referenceImageUrl, Long userId);

    ShortDramaCharacterAppearanceVo regenerateAppearanceImage(Long appearanceId, String imageModel, String referenceImageUrl, Long userId);

    ShortDramaCharacterAppearanceVo selectAppearanceImage(Long appearanceId, Integer index, Long userId);

    ShortDramaCharacterAppearanceVo deleteAppearanceImage(Long appearanceId, Integer index, Long userId);

    ShortDramaCharacterAppearanceVo undoAppearanceImage(Long appearanceId, Long userId);

    ShortDramaLocationVo regenerateLocationImage(Long locationId, String imageModel, String referenceImageUrl, Long userId);
    ShortDramaLocationVo regenerateLocationImage(Long locationId, String imageModel, String referenceImageUrl, String revisionRequirements, Long userId);

    ShortDramaLocationVo selectLocationImage(Long locationId, Integer index, Long userId);

    ShortDramaLocationVo deleteLocationImage(Long locationId, Integer index, Long userId);

    ShortDramaLocationVo undoLocationImage(Long locationId, Long userId);

    /** 异步启动图片生成，返回 prediction 信息供前端轮询 */
    MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl, Long userId);

    /** Explicit identity revision produces an unselected candidate and retains approved continuity. */
    MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl,
                                                String referencePurpose, String revisionRequirements, Long userId);

    MediaGenerationResponse startImageGeneration(String assetType, Long assetId, String model, String referenceImageUrl,
                                                String referencePurpose, String revisionRequirements,
                                                java.util.List<String> styleReferenceImageUrls, Long userId);

    /** 上传到图片模型供应商，返回仅用于当前生成会话的临时参考图 URL。 */
    String uploadReferenceImage(MultipartFile file, String model, Long userId);

    /** 轮询确认形象图片生成结果并保存 */
    ShortDramaCharacterAppearanceVo confirmAppearanceImage(Long appearanceId, String predictionId, String model, Long userId);

    /** 轮询确认场景图片生成结果并保存 */
    ShortDramaLocationVo confirmLocationImage(Long locationId, String predictionId, String model, Long userId);

    Boolean deleteProject(Long projectId, Long userId);

    // ==================== 语音资产 ====================

    ShortDramaAudioVo saveAudio(ShortDramaAudioBo bo, Long userId);

    Boolean deleteAudio(Long audioId, Long userId);

    List<ShortDramaAudioVo> listAudios(Long projectId, Long userId);

    /** 生成语音：TTS 合成音频，上传 OSS，回写 audioUrl/audioOssId */
    ShortDramaAudioVo generateAudio(Long audioId, String audioModel, Long userId);
}
