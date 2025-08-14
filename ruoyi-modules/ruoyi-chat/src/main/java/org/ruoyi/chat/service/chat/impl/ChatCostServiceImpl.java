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
        if (chatRequest.getUserId() == null) {
            log.warn("deductToken->用户ID为空，跳过计费");
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
            
            // 清理可能存在的历史累计token（模型计费方式可能发生过变更）
            ChatUsageToken existingToken = chatTokenService.queryByUserId(chatRequest.getUserId(), modelName);
            if (existingToken != null && existingToken.getToken() > 0) {
                existingToken.setToken(0);
                chatTokenService.editToken(existingToken);
                log.debug("deductToken->按次计费，清理历史累计token: {}", existingToken.getToken());
            }
            
            // 记录账单消息
            saveBillingRecord(chatRequest, tokens, numberCost.doubleValue(), "TIMES_BILLING");
            return;
        }

        // 按token计费：累加并按阈值批量扣费，保留余数
        final int threshold = 100;

        // 获得记录的累计token数
        // TODO: 这里存在并发竞态条件，需要在chatTokenService层面添加乐观锁或分布式锁
        ChatUsageToken chatToken = chatTokenService.queryByUserId(chatRequest.getUserId(), modelName);
        if (chatToken == null) {
            chatToken = new ChatUsageToken();
            chatToken.setToken(0);
            chatToken.setModelName(modelName);
            chatToken.setUserId(chatRequest.getUserId());
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
            
            try {
                // 先尝试扣费
                deductUserBalance(chatRequest.getUserId(), numberCost.doubleValue());
                // 扣费成功后，保存余数
                chatToken.setModelName(modelName);
                chatToken.setUserId(chatRequest.getUserId());
                chatToken.setToken(remainder);
                chatTokenService.editToken(chatToken);
                log.debug("deductToken->扣费成功，更新余数: {}", remainder);
                
                // 记录账单消息
                saveBillingRecord(chatRequest, billable, numberCost.doubleValue(), "TOKEN_BILLING");
            } catch (ServiceException e) {
                // 余额不足时，不更新token累计，保持原有累计数
                log.warn("deductToken->余额不足，本次token累计保持不变: {}", totalTokens);
                throw e; // 重新抛出异常
            }
        } else {
            // 未达阈值，累积token
            log.debug("deductToken->未达到计费阈值({})，累积到下次", threshold);
            chatToken.setModelName(modelName);
            chatToken.setUserId(chatRequest.getUserId());
            chatToken.setToken(totalTokens);
            chatTokenService.editToken(chatToken);
        }
    }

    /**
     * 保存聊天消息记录（不进行计费）
     */
    @Override
    public void saveMessage(ChatRequest chatRequest) {
        if (chatRequest.getUserId() == null || chatRequest.getSessionId() == null) {
            log.warn("saveMessage->用户ID或会话ID为空，跳过保存消息");
            return;
        }
        
        // 验证消息内容
        if (chatRequest.getPrompt() == null || chatRequest.getPrompt().trim().isEmpty()) {
            log.warn("saveMessage->消息内容为空，跳过保存");
            return;
        }
        
        ChatMessageBo chatMessageBo = new ChatMessageBo();
        chatMessageBo.setUserId(chatRequest.getUserId());
        chatMessageBo.setSessionId(chatRequest.getSessionId());
        chatMessageBo.setRole(chatRequest.getRole());
        chatMessageBo.setContent(chatRequest.getPrompt().trim());
        chatMessageBo.setModelName(chatRequest.getModel());
        
        // 计算并保存本次消息的token数
        int tokens = TikTokensUtil.tokens(chatRequest.getModel(), chatRequest.getPrompt());
        chatMessageBo.setTotalTokens(tokens);
        
        // 普通消息不涉及扣费，deductCost保持null
        chatMessageBo.setDeductCost(null);
        chatMessageBo.setRemark("用户消息");

        try {
            chatMessageService.insertByBo(chatMessageBo);
            log.debug("saveMessage->成功保存消息，用户ID: {}, 会话ID: {}, tokens: {}", 
                      chatRequest.getUserId(), chatRequest.getSessionId(), tokens);
        } catch (Exception e) {
            log.error("saveMessage->保存消息失败", e);
            throw new ServiceException("保存消息失败");
        }
    }



    @Override
    public void publishBillingEvent(ChatRequest chatRequest) {
        log.debug("publishBillingEvent->发布计费事件，用户ID: {}，会话ID: {}，模型: {}", 
                  chatRequest.getUserId(), chatRequest.getSessionId(), chatRequest.getModel());
        
        // 预检查：评估可能的扣费金额，如果余额不足则直接抛异常
        try {
            preCheckBalance(chatRequest);
        } catch (ServiceException e) {
            log.warn("publishBillingEvent->预检查余额不足，用户ID: {}，模型: {}", 
                     chatRequest.getUserId(), chatRequest.getModel());
            throw e; // 直接抛出，阻止消息保存和对话继续
        }
        
        eventPublisher.publishEvent(new ChatMessageCreatedEvent(
            chatRequest.getUserId(),
            chatRequest.getSessionId(),
            chatRequest.getModel(),
            chatRequest.getRole(),
            chatRequest.getPrompt()
        ));
        log.debug("publishBillingEvent->计费事件发布完成");
    }
    
    /**
     * 预检查用户余额是否足够支付可能的费用
     */
    private void preCheckBalance(ChatRequest chatRequest) {
        if (chatRequest.getUserId() == null) {
            return;
        }
        
        int tokens = TikTokensUtil.tokens(chatRequest.getModel(), chatRequest.getPrompt());
        String modelName = chatRequest.getModel();
        ChatModelVo chatModelVo = chatModelService.selectModelByName(modelName);
        BigDecimal unitPrice = BigDecimal.valueOf(chatModelVo.getModelPrice());
        
        // 按次计费：直接检查单次费用
        if (BillingType.TIMES.getCode().equals(chatModelVo.getModelType())) {
            BigDecimal numberCost = unitPrice.setScale(2, RoundingMode.HALF_UP);
            checkUserBalanceWithoutDeduct(chatRequest.getUserId(), numberCost.doubleValue());
            return;
        }
        
        // 按token计费：检查累计后可能的费用
        final int threshold = 100;
        ChatUsageToken chatToken = chatTokenService.queryByUserId(chatRequest.getUserId(), modelName);
        int previousUnpaid = (chatToken == null) ? 0 : chatToken.getToken();
        int totalTokens = previousUnpaid + tokens;
        
        int billable = (totalTokens / threshold) * threshold;
        if (billable > 0) {
            BigDecimal numberCost = unitPrice
                .multiply(BigDecimal.valueOf(billable))
                .setScale(2, RoundingMode.HALF_UP);
            checkUserBalanceWithoutDeduct(chatRequest.getUserId(), numberCost.doubleValue());
        }
    }
    
    /**
     * 检查用户余额是否足够，但不扣除
     */
    private void checkUserBalanceWithoutDeduct(Long userId, Double numberCost) {
        SysUser sysUser = sysUserMapper.selectById(userId);
        if (sysUser == null) {
            throw new ServiceException("用户不存在");
        }
        
        BigDecimal userBalance = BigDecimal.valueOf(sysUser.getUserBalance() == null ? 0D : sysUser.getUserBalance())
            .setScale(2, RoundingMode.HALF_UP);
        BigDecimal cost = BigDecimal.valueOf(numberCost == null ? 0D : numberCost)
            .setScale(2, RoundingMode.HALF_UP);
        
        if (userBalance.compareTo(cost) < 0 || userBalance.compareTo(BigDecimal.ZERO) == 0) {
            throw new ServiceException("余额不足, 请充值。当前余额: " + userBalance + "，需要: " + cost);
        }
    }
    
    /**
     * 保存账单记录
     */
    private void saveBillingRecord(ChatRequest chatRequest, int billedTokens, double cost, String billingType) {
        try {
            ChatMessageBo billingMessage = new ChatMessageBo();
            billingMessage.setUserId(chatRequest.getUserId());
            billingMessage.setSessionId(chatRequest.getSessionId());
            billingMessage.setRole("system"); // 系统账单消息
            billingMessage.setModelName(chatRequest.getModel());
            billingMessage.setTotalTokens(billedTokens);
            billingMessage.setDeductCost(cost);
            billingMessage.setRemark(billingType);
            
            // 构建账单消息内容
            String content;
            if ("TIMES_BILLING".equals(billingType)) {
                content = String.format("按次计费：消耗 %d tokens，扣费 %.2f 元", billedTokens, cost);
            } else {
                content = String.format("按量计费：结算 %d tokens，扣费 %.2f 元", billedTokens, cost);
            }
            billingMessage.setContent(content);
            
            chatMessageService.insertByBo(billingMessage);
            log.debug("saveBillingRecord->保存账单记录成功，用户ID: {}, 计费类型: {}, 费用: {}", 
                      chatRequest.getUserId(), billingType, cost);
        } catch (Exception e) {
            log.error("saveBillingRecord->保存账单记录失败", e);
            // 账单记录失败不影响主流程，只记录错误日志
        }
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
