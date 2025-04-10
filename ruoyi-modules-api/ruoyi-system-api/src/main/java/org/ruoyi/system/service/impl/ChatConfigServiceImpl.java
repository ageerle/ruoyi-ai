package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.system.domain.ChatConfig;
import org.ruoyi.system.domain.bo.ChatConfigBo;
import org.ruoyi.system.domain.vo.ChatConfigVo;
import org.ruoyi.system.mapper.ChatConfigMapper;
import org.springframework.stereotype.Service;


/**
 * 配置信息Service业务层处理
 *
 * @author ageerle
 * @date 2025-04-08
 */
@RequiredArgsConstructor
@Service
public class ChatConfigServiceImpl implements ConfigService {

    private final ChatConfigMapper baseMapper;

    /**
     * 根据配置类型和配置key获取值
     *
     * @param category 分类
     * @param configKey key名称
     * @return
     */
    @Override
    public String getConfigValue(String category,String configKey) {
        ChatConfigBo bo = new ChatConfigBo();
        bo.setCategory(category);
        bo.setConfigName(configKey);
        LambdaQueryWrapper<ChatConfig> lqw = buildQueryWrapper(bo);
        ChatConfigVo chatConfigVo = baseMapper.selectVoOne(lqw);
        return chatConfigVo.getConfigValue();
    }

    private LambdaQueryWrapper<ChatConfig> buildQueryWrapper(ChatConfigBo bo) {
        LambdaQueryWrapper<ChatConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(StringUtils.isNotBlank(bo.getCategory()), ChatConfig::getCategory, bo.getCategory());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigName()), ChatConfig::getConfigName, bo.getConfigName());
        lqw.eq(StringUtils.isNotBlank(bo.getConfigValue()), ChatConfig::getConfigValue, bo.getConfigValue());
        return lqw;
    }
}
