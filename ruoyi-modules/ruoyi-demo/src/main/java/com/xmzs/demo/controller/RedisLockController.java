package com.xmzs.demo.controller;

import com.baomidou.lock.LockInfo;
import com.baomidou.lock.LockTemplate;
import com.baomidou.lock.annotation.Lock4j;
import com.baomidou.lock.executor.RedissonLockExecutor;
import com.xmzs.common.core.domain.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalTime;


/**
 * 测试分布式锁的样例
 *
 * @author shenxinquan
 */
@Slf4j
@RestController
@RequestMapping("/demo/redisLock")
public class RedisLockController {

    @Autowired
    private LockTemplate lockTemplate;

    /**
     * 测试lock4j 注解
     */
    @Lock4j(keys = {"#key"})
    @GetMapping("/testLock4j")
    public R<String> testLock4j(String key, String value) {
        System.out.println("start:" + key + ",time:" + LocalTime.now().toString());
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("end :" + key + ",time:" + LocalTime.now().toString());
        return R.ok("操作成功", value);
    }

    /**
     * 测试lock4j 工具
     */
    @GetMapping("/testLock4jLockTemplate")
    public R<String> testLock4jLockTemplate(String key, String value) {
        final LockInfo lockInfo = lockTemplate.lock(key, 30000L, 5000L, RedissonLockExecutor.class);
        if (null == lockInfo) {
            throw new RuntimeException("业务处理中,请稍后再试");
        }
        // 获取锁成功，处理业务
        try {
            try {
                Thread.sleep(8000);
            } catch (InterruptedException e) {
                //
            }
            System.out.println("执行简单方法1 , 当前线程:" + Thread.currentThread().getName());
        } finally {
            //释放锁
            lockTemplate.releaseLock(lockInfo);
        }
        //结束
        return R.ok("操作成功", value);
    }

}
