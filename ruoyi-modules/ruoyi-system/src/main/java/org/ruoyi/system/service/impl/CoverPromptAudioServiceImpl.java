package org.ruoyi.system.service.impl;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.system.mapper.CoverPromptAudioMapper;
import org.ruoyi.system.service.ICoverPromptAudioService;
import org.springframework.stereotype.Service;

/**
 * 翻唱用户参考音频Service业务层处理
 *
 * @author NSL
 * @since 2024-12-25
 */
@Service
@Slf4j
public class CoverPromptAudioServiceImpl implements ICoverPromptAudioService {

    @Resource
    private CoverPromptAudioMapper coverPromptAudioMapper;

}
