package org.ruoyi.service.shortdrama.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.dto.OssDTO;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.OssService;
import org.ruoyi.common.core.utils.file.FileUtils;
import org.ruoyi.common.tenant.helper.TenantHelper;
import org.ruoyi.domain.entity.shortdrama.ShortDramaAudio;
import org.ruoyi.domain.entity.shortdrama.ShortDramaProject;
import org.ruoyi.domain.entity.shortdrama.ShortDramaStoryboard;
import org.ruoyi.mapper.shortdrama.ShortDramaAudioMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaProjectMapper;
import org.ruoyi.mapper.shortdrama.ShortDramaStoryboardMapper;
import org.ruoyi.service.shortdrama.composition.CompositionArtifact;
import org.ruoyi.service.shortdrama.composition.CompositionSource;
import org.ruoyi.service.shortdrama.composition.CompositionSpec;
import org.ruoyi.service.shortdrama.composition.FfmpegCompositionProperties;
import org.ruoyi.service.shortdrama.composition.FfmpegVideoComposer;
import org.ruoyi.service.shortdrama.download.SafeVideoSourceDownloader;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ShortDramaVideoComposeWorker {

    private final ShortDramaProjectMapper projectMapper;
    private final ShortDramaStoryboardMapper storyboardMapper;
    private final ShortDramaAudioMapper audioMapper;
    private final FfmpegVideoComposer videoComposer;
    private final FfmpegCompositionProperties compositionProperties;
    private final SafeVideoSourceDownloader sourceDownloader;
    private final OssService ossService;

    @Async("videoCompositionExecutor")
    public void composeAsync(ShortDramaVideoComposeJob job) {
        TenantHelper.dynamic(job.tenantId(), () -> composeWithinTenant(job));
    }

    private void composeWithinTenant(ShortDramaVideoComposeJob job) {
        Path workDirectory = null;
        Long uploadedOssId = null;
        try {
            if (!markProcessing(job)) {
                return;
            }
            List<ShortDramaStoryboard> storyboards = selectReadyStoryboards(job.projectId(), job.storyboardIds());
            workDirectory = Files.createTempDirectory("short-drama-compose-" + job.projectId() + "-");
            List<CompositionSource> sources = downloadSources(job, storyboards, workDirectory);
            if (!updateProgress(job, 50)) {
                return;
            }

            // 旁白语音资产下载到工作目录（ossId → 本地文件）
            Path narrationAudioPath = downloadNarration(job, workDirectory);

            CompositionArtifact artifact = videoComposer.compose(new CompositionSpec(
                sources,
                job.transitionType(),
                job.transitionDurationSeconds().doubleValue(),
                job.aspectRatio(),
                narrationAudioPath,
                job.watermark()
            ), workDirectory);
            if (!updateProgress(job, 85)) {
                return;
            }

            ShortDramaProject current = selectActiveProject(job);
            if (current == null) {
                return;
            }
            Long previousOssId = current.getComposedVideoOssId();
            boolean localStorage = "local".equalsIgnoreCase(compositionProperties.getStorageMode());
            if (localStorage) {
                persistLocalComposition(job.projectId(), artifact.path());
            } else {
                OssDTO uploaded = ossService.uploadFile(artifact.path().toFile());
                if (uploaded == null || uploaded.getOssId() == null) {
                    throw new ServiceException("成片上传对象存储失败");
                }
                uploadedOssId = uploaded.getOssId();
            }
            if (!updateProgress(job, 95)) {
                if (localStorage) deleteLocalComposition(job.projectId());
                else deleteOssBestEffort(uploadedOssId, "删除已失效的新成片");
                uploadedOssId = null;
                return;
            }

            BigDecimal duration = BigDecimal.valueOf(artifact.durationSeconds())
                .setScale(3, RoundingMode.HALF_UP);
            int published = projectMapper.update(null, activeJobUpdate(job)
                .set(ShortDramaProject::getComposedVideoOssId, localStorage ? null : uploadedOssId)
                .set(ShortDramaProject::getComposeStatus, ShortDramaVideoComposeServiceImpl.STATUS_DONE)
                .set(ShortDramaProject::getComposeJobId, null)
                .set(ShortDramaProject::getComposeProgress, 100)
                .set(ShortDramaProject::getComposedVideoDurationSeconds, duration)
                .set(ShortDramaProject::getComposeErrorMessage, null)
                .set(ShortDramaProject::getComposedAt, new Date())
                .set(ShortDramaProject::getUpdateTime, new Date()));
            if (published == 0) {
                if (localStorage) deleteLocalComposition(job.projectId());
                else deleteOssBestEffort(uploadedOssId, "删除并发失效的新成片");
            } else {
                deleteOssBestEffort(previousOssId, "删除被替换的旧成片");
            }
            uploadedOssId = null;
        } catch (Throwable ex) {
            if (uploadedOssId != null) {
                deleteOssBestEffort(uploadedOssId, "清理发布失败的新成片");
            }
            markFailed(job, ex);
            log.warn("短剧视频合成失败, projectId={}, jobId={}: {}",
                job.projectId(), job.jobId(), shortMessage(ex));
        } finally {
            deleteWorkDirectory(workDirectory);
        }
    }

    private boolean markProcessing(ShortDramaVideoComposeJob job) {
        return projectMapper.update(null, new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, job.projectId())
            .eq(ShortDramaProject::getComposeStatus, ShortDramaVideoComposeServiceImpl.STATUS_PENDING)
            .eq(ShortDramaProject::getComposeJobId, job.jobId())
            .set(ShortDramaProject::getComposeStatus, ShortDramaVideoComposeServiceImpl.STATUS_PROCESSING)
            .set(ShortDramaProject::getComposeProgress, 1)
            .set(ShortDramaProject::getUpdateTime, new Date())) > 0;
    }

    private List<ShortDramaStoryboard> selectReadyStoryboards(Long projectId, List<Long> storyboardIds) {
        List<ShortDramaStoryboard> storyboards = storyboardMapper.selectList(
            new LambdaQueryWrapper<ShortDramaStoryboard>()
                .eq(ShortDramaStoryboard::getProjectId, projectId)
                .in(ShortDramaStoryboard::getId, storyboardIds)
                .eq(ShortDramaStoryboard::getVideoStatus, ShortDramaVideoComposeServiceImpl.STATUS_DONE)
                .isNotNull(ShortDramaStoryboard::getVideoUrl)
                .ne(ShortDramaStoryboard::getVideoUrl, "")
                .orderByAsc(ShortDramaStoryboard::getSceneNo));
        if (storyboards.size() != storyboardIds.size()) {
            throw new ServiceException("部分所选分镜视频已失效，请重新选择");
        }
        if (storyboards.size() > compositionProperties.getMaxClips()) {
            throw new ServiceException("分镜数量超过视频合成上限: " + compositionProperties.getMaxClips());
        }
        return storyboards;
    }

    private List<CompositionSource> downloadSources(
        ShortDramaVideoComposeJob job,
        List<ShortDramaStoryboard> storyboards,
        Path workDirectory
    ) throws IOException {
        List<CompositionSource> sources = new ArrayList<>(storyboards.size());
        long maxSourceBytes = compositionProperties.getMaxSourceBytes();
        long maxTotalSourceBytes = compositionProperties.getMaxTotalSourceBytes();
        if (maxSourceBytes <= 0 || maxTotalSourceBytes <= 0 || maxSourceBytes > maxTotalSourceBytes) {
            throw new ServiceException("视频合成下载大小限制配置无效");
        }
        long totalBytes = 0;
        for (int index = 0; index < storyboards.size(); index++) {
            if (!isActive(job)) {
                return List.of();
            }
            ShortDramaStoryboard storyboard = storyboards.get(index);
            Path target = workDirectory.resolve("clip-" + String.format("%04d", index + 1) + ".mp4");
            long remainingTotalBytes = maxTotalSourceBytes - totalBytes;
            totalBytes += sourceDownloader.download(
                storyboard.getVideoUrl(),
                target,
                maxSourceBytes,
                remainingTotalBytes
            );
            Double fallbackDuration = storyboard.getDurationSeconds() == null
                ? null
                : storyboard.getDurationSeconds().doubleValue();
            sources.add(new CompositionSource(target, fallbackDuration));
            int progress = 10 + (int) Math.round(30.0 * (index + 1) / storyboards.size());
            if (!updateProgress(job, progress)) {
                return List.of();
            }
        }
        return sources;
    }

    private boolean updateProgress(ShortDramaVideoComposeJob job, int progress) {
        return projectMapper.update(null, activeJobUpdate(job)
            .set(ShortDramaProject::getComposeProgress, Math.max(0, Math.min(99, progress)))
            .set(ShortDramaProject::getUpdateTime, new Date())) > 0;
    }

    /**
     * 将旁白语音资产下载为本地文件。语音资产未指定或不存在时返回 null（不混入旁白）。
     */
    private Path downloadNarration(ShortDramaVideoComposeJob job, Path workDirectory) throws IOException {
        if (job.narrationAudioId() == null) {
            return null;
        }
        ShortDramaAudio audio = audioMapper.selectById(job.narrationAudioId());
        if (audio == null || StrUtil.isBlank(audio.getAudioUrl())) {
            log.warn("旁白语音资产不存在或无音频URL, audioId={}", job.narrationAudioId());
            return null;
        }
        Path target = workDirectory.resolve("narration.mp3");
        long maxSourceBytes = compositionProperties.getMaxSourceBytes();
        sourceDownloader.download(audio.getAudioUrl(), target, maxSourceBytes, maxSourceBytes);
        return target;
    }

    private boolean isActive(ShortDramaVideoComposeJob job) {
        return selectActiveProject(job) != null;
    }

    private ShortDramaProject selectActiveProject(ShortDramaVideoComposeJob job) {
        ShortDramaProject project = projectMapper.selectById(job.projectId());
        if (project == null
            || !ShortDramaVideoComposeServiceImpl.STATUS_PROCESSING.equals(project.getComposeStatus())
            || !Objects.equals(job.jobId(), project.getComposeJobId())) {
            return null;
        }
        return project;
    }

    private LambdaUpdateWrapper<ShortDramaProject> activeJobUpdate(ShortDramaVideoComposeJob job) {
        return new LambdaUpdateWrapper<ShortDramaProject>()
            .eq(ShortDramaProject::getId, job.projectId())
            .eq(ShortDramaProject::getComposeStatus, ShortDramaVideoComposeServiceImpl.STATUS_PROCESSING)
            .eq(ShortDramaProject::getComposeJobId, job.jobId());
    }

    private void markFailed(ShortDramaVideoComposeJob job, Throwable ex) {
        projectMapper.update(null, activeJobUpdate(job)
            .set(ShortDramaProject::getComposeStatus, ShortDramaVideoComposeServiceImpl.STATUS_FAILED)
            .set(ShortDramaProject::getComposeJobId, null)
            .set(ShortDramaProject::getComposeErrorMessage, shortMessage(ex))
            .set(ShortDramaProject::getComposedAt, null)
            .set(ShortDramaProject::getUpdateTime, new Date()));
    }

    private Path localCompositionPath(Long projectId) {
        return Path.of(compositionProperties.getLocalOutputDirectory())
            .toAbsolutePath().normalize()
            .resolve(projectId.toString())
            .resolve("composed.mp4");
    }

    private void persistLocalComposition(Long projectId, Path source) throws IOException {
        Path target = localCompositionPath(projectId);
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        log.info("短剧成片已保存到本地: {}", target);
    }

    private void deleteLocalComposition(Long projectId) {
        try {
            Files.deleteIfExists(localCompositionPath(projectId));
        } catch (IOException ex) {
            log.warn("删除本地成片失败, projectId={}: {}", projectId, ex.getMessage());
        }
    }
    private void deleteWorkDirectory(Path workDirectory) {
        if (workDirectory == null) {
            return;
        }
        try {
            if (!FileUtils.del(workDirectory)) {
                log.warn("未能完整清理视频合成临时目录: {}", workDirectory);
            }
        } catch (Exception ex) {
            log.warn("清理视频合成临时目录失败, path={}: {}", workDirectory, ex.getMessage());
        }
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

    private static String shortMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (StrUtil.isBlank(message)) {
            message = throwable.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }
}
