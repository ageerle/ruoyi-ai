package org.ruoyi.system.mapper;

import org.ruoyi.common.mybatis.core.mapper.BaseMapperPlus;
import org.ruoyi.system.domain.vo.cover.CoverPromptAudioVo;

import java.util.List;

/**
 * 翻唱用户参考音频Mapper接口
 *
 * @author NSL
 * @since 2024-12-25
 */
public interface CoverPromptAudioMapper extends BaseMapperPlus<CoverPromptAudio, CoverPromptAudioVo> {

    /**
     * 获取最近一次翻唱记录
     * @param userId 用户id
     * @return 翻唱记录
     */
    List<CoverPromptAudioVo> selectLatestVoByUserId(Long userId);
}
