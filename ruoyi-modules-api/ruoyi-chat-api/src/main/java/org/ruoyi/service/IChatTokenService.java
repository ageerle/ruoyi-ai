package org.ruoyi.service;

import org.ruoyi.domain.ChatToken;

/**
 * 聊天消息Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatTokenService {

    /**
     * 查询用户token
     */
    ChatToken queryByUserId(Long userId, String modelName);

    /**
     * 清空用户token
     */
    void resetToken(Long userId,String modelName);

    /**
     * 修改用户token
     */
    void editToken(ChatToken chatToken);

}
