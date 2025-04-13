package org.ruoyi.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.ruoyi.domain.ChatToken;
import org.ruoyi.mapper.ChatTokenMapper;
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

    private final ChatTokenMapper baseMapper;

    @Override
    public ChatToken queryByUserId(Long userId, String modelName) {
        return baseMapper.selectOne(
            new LambdaQueryWrapper<ChatToken>()
                .eq(ChatToken::getUserId, userId)
                .eq(ChatToken::getModelName, modelName)
                .last("limit 1")
        );
    }

    /**
     * 清空用户token
     *
     */
    @Override
    public void resetToken(Long userId,String modelName) {
        ChatToken chatToken = queryByUserId(userId, modelName);
        chatToken.setToken(0);
        baseMapper.updateById(chatToken);
    }

    /**
     * 增加用户token
     *
     */
    @Override
    public void editToken(ChatToken chatToken) {
        if(chatToken.getId() == null){
            baseMapper.insert(chatToken);
        }else {
            baseMapper.updateById(chatToken);
        }
    }
}
