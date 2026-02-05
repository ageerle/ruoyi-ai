package org.ruoyi.job.snailjob;

import cn.hutool.core.convert.Convert;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.dto.JobArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.springframework.stereotype.Component;

/**
 * 静态分片 根据服务端任务参数分片
 * <a href="https://juejin.cn/post/7426232375703896101"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "testStaticShardingJob")
public class TestStaticShardingJob {

    public ExecuteResult jobExecute(JobArgs jobArgs) {
        String jobParams = Convert.toStr(jobArgs.getJobParams());
        SnailJobLog.LOCAL.info("开始执行分片任务,参数:{}", jobParams);
        // 获得jobArgs 中传入的开始id和结束id
        String[] split = jobParams.split(",");
        Long fromId = Long.parseLong(split[0]);
        Long toId = Long.parseLong(split[1]);
        // 模拟数据库操作,对范围id,进行加密处理
        try {
            SnailJobLog.REMOTE.info("开始对id范围:{}进行加密处理", fromId + "-" + toId);
            Thread.sleep(3000);
            SnailJobLog.REMOTE.info("对id范围:{}进行加密处理完成", fromId + "-" + toId);
        } catch (InterruptedException e) {
            return ExecuteResult.failure("任务执行失败");
        }
        return ExecuteResult.success("执行分片任务完成");
    }
}
