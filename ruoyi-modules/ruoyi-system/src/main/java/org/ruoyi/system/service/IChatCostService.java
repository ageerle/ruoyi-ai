package org.ruoyi.system.service;

import org.ruoyi.system.domain.bo.ChatMessageBo;

public interface IChatCostService {

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


    /**
     * 扣除任务费用并且保存记录
     *
     * @param type 任务类型
     * @param prompt 任务描述
     * @param cost 扣除费用
     */
    void taskDeduct(String type,String prompt, double cost);


    /**
     * 判断用户是否付费
     */
    void checkUserGrade();
}
