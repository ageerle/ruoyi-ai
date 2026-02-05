package org.ruoyi.job.snailjob;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.aizuda.snailjob.client.job.core.MapHandler;
import com.aizuda.snailjob.client.job.core.annotation.JobExecutor;
import com.aizuda.snailjob.client.job.core.annotation.MapExecutor;
import com.aizuda.snailjob.client.job.core.dto.MapArgs;
import com.aizuda.snailjob.common.log.SnailJobLog;
import com.aizuda.snailjob.model.dto.ExecuteResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Map任务 动态分配 只分片不关注结果
 * <a href="https://juejin.cn/post/7446362500478894106"></a>
 *
 * @author 老马
 */
@Component
@JobExecutor(name = "testMapJobAnnotation")
public class TestMapJobAnnotation {

    @MapExecutor
    public ExecuteResult doJobMapExecute(MapArgs mapArgs, MapHandler mapHandler) {
        // 生成1~200数值并分片
        int partitionSize = 50;
        List<List<Integer>> partition = IntStream.rangeClosed(1, 200)
            .boxed()
            .collect(Collectors.groupingBy(i -> (i - 1) / partitionSize))
            .values()
            .stream()
            .toList();
        SnailJobLog.REMOTE.info("端口:{}完成分配任务", SpringUtil.getProperty("server.port"));
        return mapHandler.doMap(partition, "doCalc");
    }

    @MapExecutor(taskName = "doCalc")
    public ExecuteResult doCalc(MapArgs mapArgs) {
        List<Integer> sourceList = (List<Integer>) mapArgs.getMapResult();
        // 遍历sourceList的每一个元素,计算出一个累加值partitionTotal
        int partitionTotal = sourceList.stream().mapToInt(i -> i).sum();
        // 打印日志到服务器
        ThreadUtil.sleep(3, TimeUnit.SECONDS);
        SnailJobLog.REMOTE.info("端口:{},partitionTotal:{}", SpringUtil.getProperty("server.port"), partitionTotal);
        return ExecuteResult.success(partitionTotal);
    }

}
