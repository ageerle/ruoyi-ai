package org.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.domain.model.LoginUser;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.exception.base.BaseException;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.ChatToken;
import org.ruoyi.system.domain.SysUser;
import org.ruoyi.system.domain.bo.ChatMessageBo;
import org.ruoyi.system.domain.bo.SysModelBo;
import org.ruoyi.system.domain.vo.SysModelVo;
import org.ruoyi.system.mapper.SysUserMapper;
import org.ruoyi.system.service.IChatCostService;
import org.ruoyi.system.service.IChatMessageService;
import org.ruoyi.system.service.IChatTokenService;
import org.ruoyi.system.service.ISysModelService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author hncboy
 * @date 2023/3/22 19:41
 * 聊天相关业务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatCostServiceImpl implements IChatCostService {

    private final SysUserMapper sysUserMapper;

    private final IChatMessageService chatMessageService;

    private final IChatTokenService chatTokenService;

    private final ISysModelService sysModelService;

    /**
     * 根据消耗的tokens扣除余额
     *
     * @param chatMessageBo
     */
    public void deductToken(ChatMessageBo chatMessageBo) {
        // 计算总token数
        ChatToken chatToken = chatTokenService.queryByUserId(chatMessageBo.getUserId(), chatMessageBo.getModelName());
        if (chatToken == null) {
            chatToken = new ChatToken();
            chatToken.setToken(0);
        }
        int totalTokens = chatToken.getToken() + chatMessageBo.getTotalTokens();
        // 如果总token数大于等于1000,进行费用扣除
        if (totalTokens >= 1000) {
            // 计算费用
            int token1 = totalTokens / 1000;
            int token2 = totalTokens % 1000;
            if (token2 > 0) {
                // 保存剩余tokens
                chatToken.setModelName(chatMessageBo.getModelName());
                chatToken.setUserId(chatMessageBo.getUserId());
                chatToken.setToken(token2);
                chatTokenService.editToken(chatToken);
            } else {
                chatTokenService.resetToken(chatMessageBo.getUserId(), chatMessageBo.getModelName());
            }
            // 扣除用户余额
            SysModelBo sysModelBo = new SysModelBo();
            sysModelBo.setModelName(chatMessageBo.getModelName());
            List<SysModelVo> sysModelList = sysModelService.queryList(sysModelBo);
            double modelPrice = sysModelList.get(0).getModelPrice();
            Double numberCost = token1 * modelPrice;
            deductUserBalance(chatMessageBo.getUserId(), numberCost);
            chatMessageBo.setDeductCost(numberCost);
        } else {
            // 扣除用户余额
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
     *
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
        if("0".equals(sysUser.getUserGrade())){
            throw new BaseException("This model is currently unavailable for free users. Please make any amount of deposit to access.");
        }
    }

    /**
     * 获取用户Id
     *
     * @return
     */
    public Long getUserId() {
        LoginUser loginUser = LoginHelper.getLoginUser();
        if (loginUser == null) {
            throw new BaseException("用户未登录！");
        }
        return loginUser.getUserId();
    }
}
