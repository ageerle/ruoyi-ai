package com.xmzs.system.service.impl;


import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.xmzs.common.chat.entity.chat.ChatCompletion;
import com.xmzs.common.core.exception.ServiceException;
import com.xmzs.system.domain.ChatToken;
import com.xmzs.system.domain.SysUser;
import com.xmzs.system.domain.bo.ChatMessageBo;
import com.xmzs.system.mapper.SysUserMapper;
import com.xmzs.system.service.IChatService;
import com.xmzs.system.service.IChatMessageService;
import com.xmzs.system.service.IChatTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * @author hncboy
 * @date 2023/3/22 19:41
 * 聊天相关业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IChatServiceImpl implements IChatService {

    private final SysUserMapper sysUserMapper;


    private final IChatMessageService chatMessageService;

    private final IChatTokenService chatTokenService;


    /**
     * 根据消耗的tokens扣除余额
     *
     * @param chatMessageBo
     *
     */
    public void deductToken(ChatMessageBo chatMessageBo) {
        // 计算总token数
        ChatToken chatToken = chatTokenService.queryByUserId(chatMessageBo.getUserId(), chatMessageBo.getModelName());
        if(chatToken == null){
            chatToken = new ChatToken();
            chatToken.setToken(0);
        }
        int totalTokens = chatToken.getToken()+ chatMessageBo.getTotalTokens();
        // 如果总token数大于等于1000,进行费用扣除
        if (totalTokens >= 1000) {
            // 计算费用
            int token1 = totalTokens / 1000;
            int token2 = totalTokens % 1000;
            if(token2 > 0){
                // 保存剩余tokens
                chatToken.setToken(token2);
                chatTokenService.editToken(chatToken);
            }else {
                chatTokenService.resetToken(chatMessageBo.getUserId(), chatMessageBo.getModelName());
            }
            chatMessageBo.setDeductCost(token1 * ChatCompletion.getModelCost(chatMessageBo.getModelName()));
            // 扣除用户余额
            deductUserBalance(chatMessageBo.getUserId(), chatMessageBo.getDeductCost());
        } else {
            chatMessageBo.setDeductCost(0d);
            chatMessageBo.setRemark("不满1kToken,计入下一次!");
            chatToken.setToken(totalTokens);
            chatToken.setModelName(chatMessageBo.getModelName());
            chatToken.setUserId(chatMessageBo.getUserId());
            chatTokenService.editToken(chatToken);
        }
        // 保存消息记录
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 从用户余额中扣除费用
     *
     * @param userId     用户ID
     * @param numberCost 要扣除的费用
     */
    @Override
    public void deductUserBalance(Long userId, Double numberCost) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            return;
        }

        Double userBalance = sysUser.getUserBalance();
        if (userBalance < numberCost) {
            throw new ServiceException("余额不足，请联系管理员充值!");
        }

        sysUserMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getUserBalance, Math.max(userBalance - numberCost, 0))
                .eq(SysUser::getUserId, userId));
    }

}
