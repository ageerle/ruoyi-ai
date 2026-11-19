package org.ruoyi.chat.service.shortdrama;

import org.ruoyi.chat.domain.bo.shortdrama.ShortDramaComposeVideoBo;
import org.ruoyi.chat.domain.vo.shortdrama.ShortDramaComposeVideoVo;

import java.nio.file.Path;

public interface IShortDramaVideoComposeService {

    ShortDramaComposeVideoVo composeVideo(Long projectId, ShortDramaComposeVideoBo bo, Long userId);

    ShortDramaComposeVideoVo getComposedVideo(Long projectId, Long userId);

    Path getLocalComposedVideo(Long projectId, Long userId);

    void invalidateComposition(Long projectId);

    void deleteComposition(Long projectId);
}
