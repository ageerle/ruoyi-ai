package com.xmzs.demo.controller.queue;

import cn.hutool.core.util.RandomUtil;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.redis.utils.QueueUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 优先队列 演示案例
 * <p>
 * 轻量级队列 重量级数据量 请使用 MQ
 * <p>
 * 集群测试通过 同一个消息只会被消费一次 做好事务补偿
 * 集群测试流程 在其中一台发送数据 两端分别调用获取接口 一次获取一条
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/queue/priority")
public class PriorityQueueController {

    /**
     * 添加队列数据
     *
     * @param queueName 队列名
     */
    @GetMapping("/add")
    public R<Void> add(String queueName) {
        // 用完了一定要销毁 否则会一直存在
        boolean b = QueueUtils.destroyQueue(queueName);
        log.info("通道: {} , 删除: {}", queueName, b);

        for (int i = 0; i < 10; i++) {
            int randomNum = RandomUtil.randomInt(10);
            PriorityDemo data = new PriorityDemo();
            data.setName("data-" + i);
            data.setOrderNum(randomNum);
            if (QueueUtils.addPriorityQueueObject(queueName, data)) {
                log.info("通道: {} , 发送数据: {}", queueName, data);
            } else {
                log.info("通道: {} , 发送数据: {}, 发送失败", queueName, data);
            }
        }
        return R.ok("操作成功");
    }

    /**
     * 删除队列数据
     *
     * @param queueName 队列名
     * @param name      对象名
     * @param orderNum  排序号
     */
    @GetMapping("/remove")
    public R<Void> remove(String queueName, String name, Integer orderNum) {
        PriorityDemo data = new PriorityDemo();
        data.setName(name);
        data.setOrderNum(orderNum);
        if (QueueUtils.removeQueueObject(queueName, data)) {
            log.info("通道: {} , 删除数据: {}", queueName, data);
        } else {
            return R.fail("操作失败");
        }
        return R.ok("操作成功");
    }

    /**
     * 获取队列数据
     *
     * @param queueName 队列名
     */
    @GetMapping("/get")
    public R<Void> get(String queueName) {
        PriorityDemo data;
        do {
            data = QueueUtils.getQueueObject(queueName);
            log.info("通道: {} , 获取数据: {}", queueName, data);
        } while (data != null);
        return R.ok("操作成功");
    }

}
