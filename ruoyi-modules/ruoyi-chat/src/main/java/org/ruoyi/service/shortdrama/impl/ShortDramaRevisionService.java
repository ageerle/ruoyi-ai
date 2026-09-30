package org.ruoyi.service.shortdrama.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.utils.MapstructUtils;
import org.ruoyi.domain.bo.shortdrama.ShortDramaRevisionBo;
import org.ruoyi.domain.entity.shortdrama.*;
import org.ruoyi.domain.vo.shortdrama.ShortDramaDetailVo;
import org.ruoyi.mapper.shortdrama.*;
import org.ruoyi.service.shortdrama.IShortDramaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.util.*;
import java.util.stream.Collectors;

/** Applies an explicitly reviewed, complete revision without rerunning generative steps. */
@Service
@RequiredArgsConstructor
public class ShortDramaRevisionService {
    private final ShortDramaProjectMapper projects;
    private final ShortDramaScriptMapper scripts;
    private final ShortDramaStoryboardMapper boards;
    private final ShortDramaCharacterMapper characters;
    private final ShortDramaLocationMapper locations;
    private final ShortDramaVisualAssetMapper visualAssets;
    private final ShortDramaVisualAssetService visualService;
    private final IShortDramaService drama;
    private final TransactionTemplate transactions;

    public ShortDramaDetailVo apply(Long projectId, ShortDramaRevisionBo revision, Long userId) {
        transactions.executeWithoutResult(tx -> {
            var project = projects.selectById(projectId);
            if (project == null || !userId.equals(project.getUserId())) throw new IllegalArgumentException("项目不存在或无权限");
            if (!Set.of("storyboard_ready", "script_changed").contains(project.getStatus())) throw new IllegalStateException("请等待项目生成完成再导入修订稿");
            var script = scripts.selectById(revision.getScriptId());
            if (script == null || !projectId.equals(script.getProjectId())) throw new IllegalArgumentException("剧本不属于当前项目");
            if (!Objects.equals(script.getScriptText(), revision.getExpectedScriptText())) throw new IllegalStateException("剧本已发生变化，请重新导出并合并修订");
            var existing = boards.selectList(new LambdaQueryWrapper<ShortDramaStoryboard>().eq(ShortDramaStoryboard::getProjectId, projectId));
            if (existing.stream().anyMatch(s -> "generating".equals(s.getVideoStatus()))) throw new IllegalStateException("请等待运行中的视频任务完成再修订");
            var ids = existing.stream().map(ShortDramaStoryboard::getId).collect(Collectors.toSet());
            var incoming = revision.getStoryboards();
            if (incoming == null || incoming.size() != ids.size() || !ids.equals(incoming.stream().map(s -> s.getId()).collect(Collectors.toSet()))) throw new IllegalArgumentException("修订稿必须完整覆盖当前项目的镜头，不能增删或重复镜号");
            var currentById = existing.stream().collect(Collectors.toMap(ShortDramaStoryboard::getId, s -> s));
            for (var s : incoming) {
                var current = currentById.get(s.getId());
                if (!projectId.equals(s.getProjectId()) || !script.getId().equals(s.getScriptId()) || !Objects.equals(current.getSceneNo(), s.getSceneNo())) throw new IllegalArgumentException("分镜项目、剧本或镜号不匹配");
                if (s.getUpdateTime() == null || !Objects.equals(s.getUpdateTime(), current.getUpdateTime())) throw new IllegalStateException("镜头" + s.getSceneNo() + "已发生变化，请重新导出再修订");
                if (s.getSourceText() == null || s.getSourceText().isBlank() || s.getVideoPrompt() == null || s.getVideoPrompt().isBlank()) throw new IllegalArgumentException("原文及视频提示词不能为空");
                ShortDramaTiming.validate(s.getSceneNo(), s.getDurationSeconds(), s.getSourceText(), s.getContinuityJson());
            }
            if (revision.getCharacters() != null) for (var c : revision.getCharacters()) {
                var current = characters.selectById(c.getId());
                if (current == null || !projectId.equals(current.getProjectId()) || !projectId.equals(c.getProjectId()) || !Objects.equals(current.getName(), c.getName())) throw new IllegalArgumentException("角色不属于当前项目或角色名变化");
                characters.updateById(MapstructUtils.convert(c, ShortDramaCharacter.class));
            }
            if (revision.getLocations() != null) for (var l : revision.getLocations()) {
                var current = locations.selectById(l.getId());
                if (current == null || !projectId.equals(current.getProjectId()) || !projectId.equals(l.getProjectId()) || !Objects.equals(current.getName(), l.getName())) throw new IllegalArgumentException("场景不属于当前项目或场景名变化");
                locations.updateById(MapstructUtils.convert(l, ShortDramaLocation.class));
            }
            Set<Long> changedFrames = new HashSet<>();
            for (var s : incoming) {
                var current=currentById.get(s.getId());
                if (!Objects.equals(current.getImagePrompt(),s.getImagePrompt()) || !Objects.equals(current.getContinuityJson(),s.getContinuityJson())) {
                    changedFrames.add(s.getId());
                    visualAssets.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShortDramaVisualAsset>()
                        .eq(ShortDramaVisualAsset::getProjectId,projectId).eq(ShortDramaVisualAsset::getStoryboardId,s.getId())
                        .set(ShortDramaVisualAsset::getStatus,"obsolete"));
                }
                boolean videoChanged=changedFrames.contains(s.getId()) || !Objects.equals(current.getVideoPrompt(),s.getVideoPrompt())
                    || !Objects.equals(current.getSourceText(),s.getSourceText()) || !Objects.equals(current.getDurationSeconds(),s.getDurationSeconds());
                if(videoChanged) {
                    boards.update(null,new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ShortDramaStoryboard>()
                        .eq(ShortDramaStoryboard::getId,s.getId()).set(ShortDramaStoryboard::getVideoStatus,"pending")
                        .set(ShortDramaStoryboard::getVideoId,null).set(ShortDramaStoryboard::getVideoUrl,null).set(ShortDramaStoryboard::getLastFrameUrl,null));
                }
                s.setUpdateTime(null); boards.updateById(MapstructUtils.convert(s, ShortDramaStoryboard.class));
            }
            script.setScriptText(revision.getScriptText());
            if (revision.getOutlineText() != null) script.setOutlineText(revision.getOutlineText());
            scripts.updateById(script);
            visualService.synchronizeExistingFrames(projectId, changedFrames);
            project.setStatus("storyboard_ready");
            projects.updateById(project);
        });
        return drama.getDetail(projectId, userId);
    }
}
