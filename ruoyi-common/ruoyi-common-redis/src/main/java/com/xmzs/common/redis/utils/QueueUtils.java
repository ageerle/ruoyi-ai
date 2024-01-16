package com.xmzs.common.redis.utils;

import com.xmzs.common.core.utils.SpringUtils;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.redisson.api.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 分布式队列工具
 * 轻量级队列 重量级数据量 请使用 MQ
 * 要求 redis 5.X 以上
 *
 * @author Lion Li
 * @version 3.6.0 新增
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueueUtils {

    private static final RedissonClient CLIENT = SpringUtils.getBean(RedissonClient.class);


    /**
     * 获取客户端实例
     */
    public static RedissonClient getClient() {
        return CLIENT;
    }

    /**
     * 添加普通队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     */
    public static <T> boolean addQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.offer(data);
    }

    /**
     * 通用获取一个队列数据 没有数据返回 null(不支持延迟队列)
     *
     * @param queueName 队列名
     */
    public static <T> T getQueueObject(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.poll();
    }

    /**
     * 通用删除队列数据(不支持延迟队列)
     */
    public static <T> boolean removeQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.remove(data);
    }

    /**
     * 通用销毁队列 所有阻塞监听 报错(不支持延迟队列)
     */
    public static <T> boolean destroyQueue(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        return queue.delete();
    }

    /**
     * 添加延迟队列数据 默认毫秒
     *
     * @param queueName 队列名
     * @param data      数据
     * @param time      延迟时间
     */
    public static <T> void addDelayedQueueObject(String queueName, T data, long time) {
        addDelayedQueueObject(queueName, data, time, TimeUnit.MILLISECONDS);
    }

    /**
     * 添加延迟队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     * @param time      延迟时间
     * @param timeUnit  单位
     */
    public static <T> void addDelayedQueueObject(String queueName, T data, long time, TimeUnit timeUnit) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        delayedQueue.offer(data, time, timeUnit);
    }

    /**
     * 获取一个延迟队列数据 没有数据返回 null
     *
     * @param queueName 队列名
     */
    public static <T> T getDelayedQueueObject(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        return delayedQueue.poll();
    }

    /**
     * 删除延迟队列数据
     */
    public static <T> boolean removeDelayedQueueObject(String queueName, T data) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        return delayedQueue.remove(data);
    }

    /**
     * 销毁延迟队列 所有阻塞监听 报错
     */
    public static <T> void destroyDelayedQueue(String queueName) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        RDelayedQueue<T> delayedQueue = CLIENT.getDelayedQueue(queue);
        delayedQueue.destroy();
    }

    /**
     * 添加优先队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     */
    public static <T> boolean addPriorityQueueObject(String queueName, T data) {
        RPriorityBlockingQueue<T> priorityBlockingQueue = CLIENT.getPriorityBlockingQueue(queueName);
        return priorityBlockingQueue.offer(data);
    }

    /**
     * 尝试设置 有界队列 容量 用于限制数量
     *
     * @param queueName 队列名
     * @param capacity  容量
     */
    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        return boundedBlockingQueue.trySetCapacity(capacity);
    }

    /**
     * 尝试设置 有界队列 容量 用于限制数量
     *
     * @param queueName 队列名
     * @param capacity  容量
     * @param destroy   已存在是否销毁
     */
    public static <T> boolean trySetBoundedQueueCapacity(String queueName, int capacity, boolean destroy) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        if (boundedBlockingQueue.isExists() && destroy) {
            destroyQueue(queueName);
        }
        return boundedBlockingQueue.trySetCapacity(capacity);
    }

    /**
     * 添加有界队列数据
     *
     * @param queueName 队列名
     * @param data      数据
     * @return 添加成功 true 已达到界限 false
     */
    public static <T> boolean addBoundedQueueObject(String queueName, T data) {
        RBoundedBlockingQueue<T> boundedBlockingQueue = CLIENT.getBoundedBlockingQueue(queueName);
        return boundedBlockingQueue.offer(data);
    }

    /**
     * 订阅阻塞队列(可订阅所有实现类 例如: 延迟 优先 有界 等)
     */
    public static <T> void subscribeBlockingQueue(String queueName, Consumer<T> consumer) {
        RBlockingQueue<T> queue = CLIENT.getBlockingQueue(queueName);
        queue.subscribeOnElements(consumer);
    }

}
