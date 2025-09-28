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

            // 更新消息的计费信息到备注
            updateMessageBilling(chatRequest, tokens, numberCost.doubleValue(), chatModelVo.getModelType());
            return;
        }

        // 按token计费：累加并按阈值批量扣费，保留余数
        final int threshold = 1000;

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
            // 计算批次数：每1000个Token为一批，每批扣费单价
            int batches = billable / threshold;
            BigDecimal numberCost = unitPrice
                .multiply(BigDecimal.valueOf(batches))
                .setScale(2, RoundingMode.HALF_UP);
            log.debug("deductToken->按token扣费，结算token数量: {}，批次数: {}，单价: {}，费用: {}", 
                      billable, batches, unitPrice, numberCost);

            try {
                // 先尝试扣费
                deductUserBalance(chatRequest.getUserId(), numberCost.doubleValue());
                // 扣费成功后，保存余数
                chatToken.setModelName(modelName);
                chatToken.setUserId(chatRequest.getUserId());
                chatToken.setToken(remainder);
                chatTokenService.editToken(chatToken);
                log.debug("deductToken->扣费成功，更新余数: {}", remainder);

                // 更新消息的计费信息到备注
                updateMessageBilling(chatRequest, billable, numberCost.doubleValue(), chatModelVo.getModelType());
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
            
            // 虽未扣费，但要更新消息的基本信息（实际token数、计费类型等）
            updateMessageWithoutBilling(chatRequest, tokens, chatModelVo.getModelType());
        }
    }

    /**
     * 保存聊天消息记录（不进行计费）
     * 保存成功后将消息ID设置到ChatRequest中，供后续扣费使用
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

//        // 基础消息信息，计费相关数据（tokens、费用、计费类型等）在扣费时统一设置
//        chatMessageBo.setTotalTokens(0);  // 初始设为0，扣费时更新
//        chatMessageBo.setDeductCost(null);
//        chatMessageBo.setBillingType(null);
//        chatMessageBo.setRemark("用户消息");

        try {
            chatMessageService.insertByBo(chatMessageBo);
            // 保存成功后，将生成的消息ID设置到ChatRequest中
            chatRequest.setMessageId(chatMessageBo.getId());
            log.debug("saveMessage->成功保存消息，消息ID: {}, 用户ID: {}, 会话ID: {}",
                      chatMessageBo.getId(), chatRequest.getUserId(), chatRequest.getSessionId());
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
            chatRequest.getPrompt(),
            chatRequest.getMessageId()
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
        final int threshold = 1000;
        ChatUsageToken chatToken = chatTokenService.queryByUserId(chatRequest.getUserId(), modelName);
        int previousUnpaid = (chatToken == null) ? 0 : chatToken.getToken();
        int totalTokens = previousUnpaid + tokens;

        int billable = (totalTokens / threshold) * threshold;
        if (billable > 0) {
            // 计算批次数：每1000个Token为一批，每批扣费单价
            int batches = billable / threshold;
            BigDecimal numberCost = unitPrice
                .multiply(BigDecimal.valueOf(batches))
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
     * 更新消息的基本信息（不涉及扣费）
     */
    private void updateMessageWithoutBilling(ChatRequest chatRequest, int actualTokens, String billingTypeCode) {
        // 检查是否有消息ID可以更新
        if (chatRequest.getMessageId() == null) {
            log.warn("updateMessageWithoutBilling->消息ID为空，无法更新基本信息");
            return;
        }

        try {
            // 创建更新对象，只更新基本信息，不涉及扣费
            ChatMessageBo updateMessage = new ChatMessageBo();
            updateMessage.setId(chatRequest.getMessageId());
            updateMessage.setTotalTokens(actualTokens);  // 设置实际token数
            updateMessage.setBillingType(billingTypeCode); // 设置计费类型
            updateMessage.setRemark("用户消息（累计中，未达扣费阈值）"); // 说明状态

            // 更新消息
            chatMessageService.updateByBo(updateMessage);
            log.debug("updateMessageWithoutBilling->更新消息基本信息成功，消息ID: {}, 实际tokens: {}, 计费类型: {}",
                      chatRequest.getMessageId(), actualTokens, billingTypeCode);
        } catch (Exception e) {
            log.error("updateMessageWithoutBilling->更新消息基本信息失败，消息ID: {}", chatRequest.getMessageId(), e);
            // 更新失败不影响主流程，只记录错误日志
        }
    }

    /**
     * 更新消息的计费信息到备注字段
     */
    private void updateMessageBilling(ChatRequest chatRequest, int billedTokens, double cost, String billingTypeCode) {
        // 检查是否有消息ID可以更新
        if (chatRequest.getMessageId() == null) {
            log.warn("updateMessageBilling->消息ID为空，无法更新计费信息");
            return;
        }

        try {
            // 计算本次消息的实际token数
            int actualTokens = TikTokensUtil.tokens(chatRequest.getModel(), chatRequest.getPrompt());

            // 构建计费信息
            String billingInfo = buildBillingInfo(billingTypeCode, billedTokens, cost);

            // 创建更新对象
            ChatMessageBo updateMessage = new ChatMessageBo();
            updateMessage.setId(chatRequest.getMessageId());
            updateMessage.setTotalTokens(actualTokens);  // 设置实际token数
            updateMessage.setDeductCost(cost);
            updateMessage.setRemark(billingInfo);
            updateMessage.setBillingType(billingTypeCode);

            // 更新消息
            chatMessageService.updateByBo(updateMessage);
            log.debug("updateMessageBilling->更新消息计费信息成功，消息ID: {}, 实际tokens: {}, 计费tokens: {}, 费用: {}",
                      chatRequest.getMessageId(), actualTokens, billedTokens, cost);
        } catch (Exception e) {
            log.error("updateMessageBilling->更新消息计费信息失败，消息ID: {}", chatRequest.getMessageId(), e);
            // 更新失败不影响主流程，只记录错误日志
        }
    }

    /**
     * 构建计费信息字符串
     */
    private String buildBillingInfo(String billingTypeCode, int billedTokens, double cost) {
        // 使用枚举获取计费类型并构建计费信息
        BillingType billingType = BillingType.fromCode(billingTypeCode);
        if (billingType != null) {
            return switch (billingType) {
                case TIMES -> String.format("%s：消耗 %d tokens，扣费 %.2f 元", billingType.getDescription(), billedTokens, cost);
                case TOKEN -> String.format("%s：结算 %d tokens，扣费 %.2f 元", billingType.getDescription(), billedTokens, cost);
            };
        } else {
            return String.format("系统计费：处理 %d tokens，扣费 %.2f 元", billedTokens, cost);
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
        chatMessageBo.setRemark(String.format("任务计费：%s，扣费 %.2f 元", type, cost));
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

    /**
     * 检查用户余额是否足够支付预估费用
     */
    @Override
    public boolean checkBalanceSufficient(ChatRequest chatRequest) {
        if (chatRequest.getUserId() == null) {
            log.warn("当前未登录");
            return true;
        }

        try {
            // 重用现有的预检查逻辑，但不抛异常，只返回boolean
            preCheckBalance(chatRequest);
            return true; // 预检查通过，余额充足
        } catch (ServiceException e) {
            log.debug("checkBalanceSufficient->余额不足，用户ID: {}, 模型: {}, 错误: {}", 
                      chatRequest.getUserId(), chatRequest.getModel(), e.getMessage());
            return false; // 预检查失败，余额不足
        } catch (Exception e) {
            log.error("checkBalanceSufficient->检查余额时发生异常，用户ID: {}, 模型: {}", 
                      chatRequest.getUserId(), chatRequest.getModel(), e);
            return false; // 异常情况视为余额不足，保守处理
        }
    }
}
