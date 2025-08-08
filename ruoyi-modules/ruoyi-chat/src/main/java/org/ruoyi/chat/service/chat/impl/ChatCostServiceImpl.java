package org.ruoyi.chat.service.chat.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.ruoyi.chat.enums.BillingType;
import org.ruoyi.chat.event.ChatMessageCreatedEvent;
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
import org.springframework.context.ApplicationEventPublisher;
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

    private final ApplicationEventPublisher eventPublisher;

    /**
     * 扣除用户余额（仅计费与累计，不保存消息）
     */
    @Override
    public void deductToken(ChatRequest chatRequest) {
        if (chatRequest.getUserId() == null || chatRequest.getSessionId() == null) {
            return;
        }

        int tokens = TikTokensUtil.tokens(chatRequest.getModel(), chatRequest.getPrompt());
        log.debug("deductToken->本次提交token数: {}", tokens);

        String modelName = chatRequest.getModel();
        ChatModelVo chatModelVo = chatModelService.selectModelByName(modelName);
        BigDecimal unitPrice = BigDecimal.valueOf(chatModelVo.getModelPrice());

        // 按次计费：每次调用都直接扣费，不累计token
        if (BillingType.TIMES.getCode().equals(chatModelVo.getModelType())) {
            BigDecimal numberCost = unitPrice.setScale(2, RoundingMode.HALF_UP);
            deductUserBalance(chatRequest.getUserId(), numberCost.doubleValue());
            log.debug("deductToken->按次数扣费，费用: {}，模型: {}", numberCost, modelName);
            return;
        }

        // 按token计费：累加并按阈值批量扣费，保留余数
        final int threshold = 100;

        // 获得记录的累计token数
        ChatUsageToken chatToken = chatTokenService.queryByUserId(chatRequest.getUserId(), modelName);
        if (chatToken == null) {
            chatToken = new ChatUsageToken();
            chatToken.setToken(0);
        }

        int previousUnpaid = chatToken.getToken();
        int totalTokens = previousUnpaid + tokens;
        log.debug("deductToken->未付费token数: {}，本次累计后总数: {}", previousUnpaid, totalTokens);

        int billable = (totalTokens / threshold) * threshold; // 可计费整批token数
        int remainder = totalTokens - billable;               // 结算后保留的余数

        if (billable > 0) {
            BigDecimal numberCost = unitPrice
                .multiply(BigDecimal.valueOf(billable))
                .setScale(2, RoundingMode.HALF_UP);
            log.debug("deductToken->按token扣费，结算token数量: {}，单价: {}，费用: {}", billable, unitPrice, numberCost);
            deductUserBalance(chatRequest.getUserId(), numberCost.doubleValue());
        } else {
            log.debug("deductToken->未达到计费阈值({})，累积到下次", threshold);
        }

        // 保存剩余tokens（保留余数）
        chatToken.setModelName(modelName);
        chatToken.setUserId(chatRequest.getUserId());
        chatToken.setToken(remainder);
        chatTokenService.editToken(chatToken);
    }

    /**
     * 保存聊天消息记录（不进行计费）
     */
    @Override
    public void saveMessage(ChatRequest chatRequest) {
        if (chatRequest.getUserId() == null || chatRequest.getSessionId() == null) {
            return;
        }
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(chatRequest.getUserId());
        chatMessageBo.setSessionId(chatRequest.getSessionId());
        chatMessageBo.setRole(chatRequest.getRole());
        chatMessageBo.setContent(chatRequest.getPrompt());
        chatMessageBo.setModelName(chatRequest.getModel());

    

        chatMessageService.insertByBo(chatMessageBo);
    }



    @Override
    public void publishBillingEvent(ChatRequest chatRequest) {
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(
            chatRequest.getUserId(),
            chatRequest.getSessionId(),
            chatRequest.getModel(),
            chatRequest.getRole(),
            chatRequest.getPrompt()
        ));
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

        BigDecimal userBalance = BigDecimal.valueOf(sysUser.getUserBalance() == null ? 0D : sysUser.getUserBalance())
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = BigDecimal.valueOf(numberCost == null ? 0D : numberCost)
            .setScale(2, RoundingMode.HALF_UP);

        log.debug("deductUserBalance->准备扣除: {}，当前余额: {}", cost, userBalance);

        if (userBalance.compareTo(cost) < 0 || userBalance.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("余额不足, 请充值");
        }

        BigDecimal newBalance = userBalance.subtract(cost);
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            newBalance = BigDecimal.ZERO;
        }
        newBalance = newBalance.setScale(2, RoundingMode.HALF_UP);

        sysUserMapper.update(null,
            new LambdaUpdateWrapper<SysUser>()
                .set(SysUser::getUserBalance, newBalance.doubleValue())
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
    @Override
    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }
}
