package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.domain.bo.shortdrama.ShortDramaComposeVideoBo;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.domain.vo.shortdrama.ShortDramaComposeVideoVo;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.service.shortdrama.IShortDramaVideoComposeService;
import org.ruoyi.service.shortdrama.composition.AspectRatio;
import org.ruoyi.service.shortdrama.composition.FfmpegCompositionProperties;
import org.ruoyi.service.shortdrama.composition.TransitionType;
import org.ruoyi.common.tenant.helper.TenantHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortDramaVideoComposeServiceImpl implements IShortDramaVideoComposeService {

    static final String STATUS_PENDING = "pending";
    static final String STATUS_PROCESSING = "processing";
    static final String STATUS_DONE = "done";
    static final String STATUS_FAILED = "failed";

    private final ShortDramaProjectMapper projectMapper;
    private final ShortDramaStoryboardMapper storyboardMapper;
    private final ShortDramaVideoComposeWorker composeWorker;
    private final OssService ossService;
    private final FfmpegCompositionProperties compositionProperties;

    @Override
    public ShortDramaComposeVideoVo composeVideo(Long projectId, ShortDramaComposeVideoBo bo, Long userId) {
        selectOwnedProject(projectId, userId);
        validateReadyStoryboards(projectId, bo.getStoryboardIds());

        TransitionType transitionType = TransitionType.fromValue(bo.getTransitionType());
        AspectRatio aspectRatio = AspectRatio.fromValue(bo.getAspectRatio());
        BigDecimal transitionDuration = normalizeTransitionDuration(transitionType, bo.getTransitionDurationSeconds());
        String jobId = UUID.randomUUID().toString();
        Date now = new Date();
        Date staleBefore = staleBefore(now);

        int claimed = projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .eq(ShortDramaProject::getUserId, userId)
            .nested(w -> w.isNull(ShortDramaProject::getComposeStatus)
                .or()
                .notIn(ShortDramaProject::getComposeStatus, STATUS_PENDING, STATUS_PROCESSING)
                .or(stale -> stale
                    .in(ShortDramaProject::getComposeStatus, STATUS_PENDING, STATUS_PROCESSING)
                    .and(expired -> expired
                        .isNull(ShortDramaProject::getUpdateTime)
                        .or()
                        .le(ShortDramaProject::getUpdateTime, staleBefore))))
            .set(ShortDramaProject::getComposeStatus, STATUS_PENDING)
            .set(ShortDramaProject::getComposeJobId, jobId)
            .set(ShortDramaProject::getComposeProgress, 0)
            .set(ShortDramaProject::getComposeTransitionType, transitionType.value())
            .set(ShortDramaProject::getComposeTransitionDurationSeconds, transitionDuration)
            .set(ShortDramaProject::getComposeAspectRatio, aspectRatio.value())
            .set(ShortDramaProject::getComposedVideoDurationSeconds, null)
            .set(ShortDramaProject::getComposeErrorMessage, null)
            .set(ShortDramaProject::getComposedAt, null)
            .set(ShortDramaProject::getUpdateTime, now));
        if (claimed == 0) {
            throw new ServiceException("该项目已有视频合成任务正在排队或处理中");
        }

        ShortDramaVideoComposeJob job = new ShortDramaVideoComposeJob(
            projectId,
            jobId,
            TenantHelper.getTenantId(),
            transitionType,
            transitionDuration,
            aspectRatio,
            bo.getStoryboardIds()
        );
        try {
            composeWorker.composeAsync(job);
        } catch (RuntimeException ex) {
            markDispatchFailed(job, ex);
            throw new ServiceException("视频合成任务提交失败，请稍后重试").setDetailMessage(ex.getMessage());
        }
        ShortDramaProject state = projectMapper.selectById(projectId);
        if (state == null) {
            throw new ServiceException("项目已被删除");
        }
        return toVo(state);
    }

    @Override
    public ShortDramaComposeVideoVo getComposedVideo(Long projectId, Long userId) {
        ShortDramaProject project = selectOwnedProject(projectId, userId);
        if (StrUtil.isBlank(project.getComposeStatus())) {
            return null;
        }
        return toVo(project);
    }

    @Override
    public Path getLocalComposedVideo(Long projectId, Long userId) {
        ShortDramaProject project = selectOwnedProject(projectId, userId);
        if (!STATUS_DONE.equals(project.getComposeStatus())) {
            throw new ServiceException("成片尚未生成完成");
        }
        Path path = Path.of(compositionProperties.getLocalOutputDirectory())
            .toAbsolutePath().normalize()
            .resolve(projectId.toString())
            .resolve("composed.mp4");
        if (!Files.isRegularFile(path)) {
            throw new ServiceException("本地成片文件不存在，请重新合成");
        }
        return path;
    }
    @Override
    public void invalidateComposition(Long projectId) {
        projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .set(ShortDramaProject::getComposeStatus, null)
            .set(ShortDramaProject::getComposeJobId, null)
            .set(ShortDramaProject::getComposeProgress, 0)
            .set(ShortDramaProject::getComposeErrorMessage, null)
            .set(ShortDramaProject::getComposedVideoDurationSeconds, null)
            .set(ShortDramaProject::getComposedAt, null)
            .set(ShortDramaProject::getUpdateTime, new Date()));
    }

    @Override
    public void deleteComposition(Long projectId) {
        int invalidated = projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .set(ShortDramaProject::getComposeStatus, null)
            .set(ShortDramaProject::getComposeJobId, null)
            .set(ShortDramaProject::getComposeProgress, 0)
            .set(ShortDramaProject::getComposeErrorMessage, null)
            .set(ShortDramaProject::getComposedVideoDurationSeconds, null)
            .set(ShortDramaProject::getComposedAt, null)
            .set(ShortDramaProject::getUpdateTime, new Date()));
        if (invalidated == 0) {
            return;
        }

        ShortDramaProject project = projectMapper.selectById(projectId);
        if (project == null) {
            return;
        }
        Long ossId = project.getComposedVideoOssId();
        if (ossId == null) {
            return;
        }
        int cleared = projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .eq(ShortDramaProject::getComposedVideoOssId, ossId)
            .set(ShortDramaProject::getComposedVideoOssId, null)
            .set(ShortDramaProject::getUpdateTime, new Date()));
        if (cleared > 0) {
            deleteOssAfterCommit(ossId, "删除项目成片");
        }
    }

    private ShortDramaProject selectOwnedProject(Long projectId, Long userId) {
        ShortDramaProject project = projectMapper.selectOne(new LambdaQueryWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, projectId)
            .eq(ShortDramaProject::getUserId, userId)
            .last("limit 1"));
        if (project == null) {
            throw new ServiceException("项目不存在或无权限");
        }
        return project;
    }

    private void validateReadyStoryboards(Long projectId, List<Long> storyboardIds) {
        if (storyboardIds == null || storyboardIds.size() < 2) {
            throw new ServiceException("至少选择 2 个分镜视频进行合成");
        }
        List<Long> distinctIds = storyboardIds.stream().distinct().toList();
        if (distinctIds.size() != storyboardIds.size()) {
            throw new ServiceException("所选分镜存在重复项");
        }
        List<ShortDramaStoryboard> storyboards = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getProjectId, projectId)
                .in(ShortDramaStoryboard::getId, distinctIds));
        if (storyboards.size() != distinctIds.size()) {
            throw new ServiceException("所选分镜不存在或不属于当前项目");
        }
        for (ShortDramaStoryboard storyboard : storyboards) {
            if (!STATUS_DONE.equals(storyboard.getVideoStatus()) || StrUtil.isBlank(storyboard.getVideoUrl())) {
                throw new ServiceException("分镜 " + storyboard.getSceneNo() + " 的视频尚未生成完成");
            }
        }
    }

    private BigDecimal normalizeTransitionDuration(TransitionType type, BigDecimal requested) {
        try {
            return compositionProperties.normalizeTransitionDuration(type, requested);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException("转场时长无效: " + ex.getMessage());
        }
    }

    private Date staleBefore(Date now) {
        Duration staleAfter = compositionProperties.getJobStaleAfter();
        if (staleAfter == null || staleAfter.isZero() || staleAfter.isNegative()) {
            throw new ServiceException("视频合成任务超时租约配置必须大于0");
        }
        return Date.from(Instant.ofEpochMilli(now.getTime()).minus(staleAfter));
    }

    private ShortDramaComposeVideoVo toVo(ShortDramaProject project) {
        boolean completed = STATUS_DONE.equals(project.getComposeStatus());
        Long ossId = completed ? project.getComposedVideoOssId() : null;
        String videoUrl = null;
        if (ossId != null) {
            try {
                videoUrl = ossService.selectUrlByIds(ossId.toString());
            } catch (Exception ex) {
                log.warn("解析短剧成片OSS地址失败, projectId={}, ossId={}: {}",
                    project.getId(), ossId, ex.getMessage());
            }
        }
        return ShortDramaComposeVideoVo.builder()
            .projectId(project.getId())
            .status(project.getComposeStatus())
            .progress(project.getComposeProgress())
            .transitionType(project.getComposeTransitionType())
            .transitionDurationSeconds(project.getComposeTransitionDurationSeconds())
            .aspectRatio(project.getComposeAspectRatio())
            .outputDurationSeconds(project.getComposedVideoDurationSeconds())
            .videoOssId(ossId)
            .videoUrl(videoUrl)
            .errorMessage(project.getComposeErrorMessage())
            .composedAt(project.getComposedAt())
            .build();
    }

    private void markDispatchFailed(ShortDramaVideoComposeJob job, RuntimeException ex) {
        projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, job.projectId())
            .eq(ShortDramaProject::getComposeStatus, STATUS_PENDING)
            .eq(ShortDramaProject::getComposeJobId, job.jobId())
            .set(ShortDramaProject::getComposeStatus, STATUS_FAILED)
            .set(ShortDramaProject::getComposeJobId, null)
            .set(ShortDramaProject::getComposeProgress, 0)
            .set(ShortDramaProject::getComposeErrorMessage, shortMessage(ex))
            .set(ShortDramaProject::getUpdateTime, new Date()));
    }

    private void deleteOssBestEffort(Long ossId, String action) {
        if (ossId == null) {
            return;
        }
        try {
            ossService.deleteFile(ossId);
        } catch (Exception ex) {
            log.warn("{}失败, ossId={}: {}", action, ossId, ex.getMessage());
        }
    }

    private void deleteOssAfterCommit(Long ossId, String action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteOssBestEffort(ossId, action);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                deleteOssBestEffort(ossId, action);
            }
        });
    }

    private static String shortMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (StrUtil.isBlank(message)) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
