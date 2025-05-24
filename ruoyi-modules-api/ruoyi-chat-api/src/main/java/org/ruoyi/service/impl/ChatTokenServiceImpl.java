package org.ruoyi.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatToken;
import org.ruoyi.domain.ChatUsageToken;
import org.ruoyi.mapper.ChatUsageTokenMapper;
import org.ruoyi.service.IChatTokenService;
import org.springframework.stereotype.Service;

/**
 * 聊天消息Service业务层处理
 *
 * @author Lion Li
 * @date 2023-11-26
 */
@RequiredArgsConstructor
@Service
public class ChatTokenServiceImpl implements IChatTokenService {

    private final ChatUsageTokenMapper baseMapper;

    @Override
    public ChatUsageToken queryByUserId(Long userId, String modelName) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<ChatUsageToken>()
                .eq(ChatUsageToken::getUserId, userId)
                .eq(ChatUsageToken::getModelName, modelName)
                .last("limit 1")
        );
    }

    /**
     * 清空用户token
     *
     */
    @Override
    public void resetToken(Long userId,String modelName) {
        ChatUsageToken chatToken = queryByUserId(userId, modelName);
        chatToken.setToken(0);
        baseMapper.updateById(chatToken);
    }

    /**
     * 增加用户token
     *
     */
    @Override
    public void editToken(ChatUsageToken chatToken) {
        if(chatToken.getId() == null){
            baseMapper.insert(chatToken);
        }else {
            baseMapper.updateById(chatToken);
        }
    }
}
