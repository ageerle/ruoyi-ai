package org.ruoyi.job.snailjob;

import cn.hutool.core.date.DateUtil;
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
 * DAG工作流任务-模拟微信账单任务
 * <a href="https://juejin.cn/post/7487860254114644019"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "wechatBillTask")
public class WechatBillTask {

    public ExecuteResult jobExecute(JobArgs jobArgs) throws InterruptedException {
        BillDto billDto = new BillDto();
        billDto.setBillId(123456789L);
        billDto.setBillChannel("wechat");
        // 从上下文中获得清算日期并设置，如果上下文中清算日期
        // 是sysdate设置为当前日期；否则取管理页面设置的值
        String settlementDate = (String) jobArgs.getWfContext().get("settlementDate");
        if (StrUtil.equals(settlementDate, "sysdate")) {
            settlementDate = DateUtil.today();
        }
        billDto.setBillDate(settlementDate);
        billDto.setBillAmount(new BigDecimal("1234.56"));
        // 把billDto对象放入上下文进行传递
        jobArgs.appendContext("wechat", JsonUtils.toJsonString(billDto));
        SnailJobLog.REMOTE.info("上下文: {}", jobArgs.getWfContext());
        return ExecuteResult.success(billDto);
    }

}
