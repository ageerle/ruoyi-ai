package org.ruoyi.chat.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.chat.enums.BillingType;
import org.ruoyi.chat.enums.UserGradeType;
import org.ruoyi.chat.service.chat.IChatCostService;
import org.ruoyi.common.chat.request.ChatRequest;
import org.ruoyi.common.chat.utils.TikTokensUtil;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.domain.ChatUsageToken;
import org.ruoyi.domain.bo.ChatMessageBo;
import org.ruoyi.domain.vo.ChatModelVo;
import org.ruoyi.service.IChatMessageService;
import org.ruoyi.service.IChatModelService;
import org.ruoyi.service.IChatTokenService;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.mapper.SysUserMapper;
import org.springframework.stereotype.Service;


/**
 * 计费管理Service实现
 *
 * @author ageerle
 * @date 2025-04-08
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCostServiceImpl implements IChatCostService {

    private final SysUserMapper sysUserMapper;

    private final IChatMessageService chatMessageService;

    private final IChatTokenService chatTokenService;

    private final IChatModelService chatModelService;

    /**
     * 扣除用户余额
     */
    @Override
    public void deductToken(ChatRequest chatRequest) {
        if(chatRequest.getUserId()==null || chatRequest.getSessionId()==null){
            return;
        }

        int tokens = TikTokensUtil.tokens(chatRequest.getModel(), chatRequest.getPrompt());

        String modelName = chatRequest.getModel();

        ChatMessageBo chatMessageBo = new ChatMessageBo();

        // 设置用户id
        chatMessageBo.setUserId(chatRequest.getUserId());
        // 设置对话角色
        chatMessageBo.setRole(chatRequest.getRole());
        // 设置会话id
        chatMessageBo.setSessionId(chatRequest.getSessionId());
        // 设置对话内容
        chatMessageBo.setContent(chatRequest.getPrompt());

        // 计算总token数
        ChatUsageToken chatToken = chatTokenService.queryByUserId(chatMessageBo.getUserId(), modelName);
        if (chatToken == null) {
            chatToken = new ChatUsageToken();
            chatToken.setToken(0);
        }
        int totalTokens = chatToken.getToken() + tokens;
        // 如果总token数大于等于1000,进行费用扣除
        if (totalTokens >= 1000) {
            // 计算费用
            int token1 = totalTokens / 1000;
            int token2 = totalTokens % 1000;
            if (token2 > 0) {
                // 保存剩余tokens
                chatToken.setModelName(modelName);
                chatToken.setUserId(chatMessageBo.getUserId());
                chatToken.setToken(token2);
                chatTokenService.editToken(chatToken);
            } else {
                chatTokenService.resetToken(chatMessageBo.getUserId(), modelName);
            }
            ChatModelVo chatModelVo = chatModelService.selectModelByName(modelName);
            double cost = chatModelVo.getModelPrice();
            if (BillingType.TIMES.getCode().equals(chatModelVo.getModelType())) {
                // 按次数扣费
                deductUserBalance(chatMessageBo.getUserId(), cost);
                chatMessageBo.setDeductCost(cost);
            }else {
                // 按token扣费
                Double numberCost = token1 * cost;
                deductUserBalance(chatMessageBo.getUserId(), numberCost);
                chatMessageBo.setDeductCost(numberCost);
            }

        } else {
            deductUserBalance(chatMessageBo.getUserId(), 0.0);
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
        if (userBalance < numberCost || userBalance == 0) {
            throw new ServiceException("余额不足, 请充值");
        }
        sysUserMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getUserBalance, Math.max(userBalance - numberCost, 0))
                .eq(SysUser::getUserId, userId));
    }

    /**
     * 扣除任务费用
     */
    @Override
    public void taskDeduct(String type,String prompt, double cost) {
        // 判断用户是否付费
        checkUserGrade();
        // 扣除费用
        deductUserBalance(getUserId(), cost);
        // 保存消息记录
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(getUserId());
        chatMessageBo.setModelName(type);
        chatMessageBo.setContent(prompt);
        chatMessageBo.setDeductCost(cost);
        chatMessageBo.setTotalTokens(0);
        chatMessageService.insertByBo(chatMessageBo);
    }

    /**
     * 判断用户是否付费
     */
    @Override
    public void checkUserGrade() {
        SysUser sysUser = sysUserMapper.selectById(getUserId());
        if(UserGradeType.UNPAID.getCode().equals(sysUser.getUserGrade())){
            throw new BaseException("该模型仅限付费用户使用。请升级套餐，开启高效体验之旅！");
        }
    }

    /**
     * 获取用户Id
     */
    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }
}
