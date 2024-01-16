package com.xmzs.system.service;

import com.xmzs.system.domain.ChatToken;

/**
 * 聊天消息Service接口
 *
 * @author Lion Li
 * @date 2023-11-26
 */
public interface IChatTokenService {

    /**
     * 查询用户token
     */
    ChatToken queryByUserId(Long userId,String modelName);

    /**
     * 清空用户token
     */
    void resetToken(Long userId,String modelName);

    void editToken(ChatToken chatToken);

}
