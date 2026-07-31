package org.ruoyi.service.shortdrama;

import org.ruoyi.domain.bo.shortdrama.ShortDramaComposeVideoBo;
import org.ruoyi.domain.vo.shortdrama.ShortDramaComposeVideoVo;

import java.nio.file.Path;

public interface IShortDramaVideoComposeService {

    ShortDramaComposeVideoVo composeVideo(Long projectId, ShortDramaComposeVideoBo bo, Long userId);

    ShortDramaComposeVideoVo getComposedVideo(Long projectId, Long userId);

    Path getLocalComposedVideo(Long projectId, Long userId);

    void invalidateComposition(Long projectId);

    void deleteComposition(Long projectId);
}
