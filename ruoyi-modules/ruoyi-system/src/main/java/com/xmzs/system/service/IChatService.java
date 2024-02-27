package com.xmzs.system.service;

import com.xmzs.system.domain.bo.ChatMessageBo;

/**
 * @author hncboy
 * @date 2023/3/22 19:41
 * 聊天相关业务接口
 */
public interface IChatService {


    /**
     * 根据消耗的tokens扣除余额
     *
     * @param chatMessageBo
     * @return 结果
     */

    void deductToken(ChatMessageBo chatMessageBo);

    /**
     * 扣除用户的余额
     *
     */
    void deductUserBalance(Long userId, Double numberCost);
}
