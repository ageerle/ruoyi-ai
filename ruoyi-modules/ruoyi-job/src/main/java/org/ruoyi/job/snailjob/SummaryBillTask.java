package org.ruoyi.job.snailjob;

import cn.hutool.core.util.StrUtil;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.job.entity.BillDto;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * DAG工作流任务-模拟汇总账单任务
 * <a href="https://juejin.cn/post/7487860254114644019"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "summaryBillTask")
public class SummaryBillTask {

    public ExecuteResult jobExecute(JobArgs jobArgs) throws InterruptedException {
        // 获得微信账单
        BigDecimal wechatAmount = BigDecimal.valueOf(0);
        String wechat = (String) jobArgs.getWfContext("wechat");
        if (StrUtil.isNotBlank(wechat)) {
            BillDto wechatBillDto = JsonUtils.parseObject(wechat, BillDto.class);
            wechatAmount = wechatBillDto.getBillAmount();
        }
        // 获得支付宝账单
        BigDecimal alipayAmount = BigDecimal.valueOf(0);
        String alipay = (String) jobArgs.getWfContext("alipay");
        if (StrUtil.isNotBlank(alipay)) {
            BillDto alipayBillDto = JsonUtils.parseObject(alipay, BillDto.class);
            alipayAmount = alipayBillDto.getBillAmount();
        }
        // 汇总账单
        BigDecimal totalAmount = wechatAmount.add(alipayAmount);
        SnailJobLog.REMOTE.info("总金额: {}", totalAmount);
        return ExecuteResult.success(totalAmount);
    }

}
