package org.ruoyi.common.core.service;

/**
 * @description: 基于ThreadLocal封装工具类，用户保存和获取当前登录用户Sa-Token token值
 * @author: yzm
 **/
public class BaseContext {
    private static ThreadLocal<String> threadLocal = new ThreadLocal<>();

    /**
     * @description: 设置值
     * @author: yzm
     * @param: [token] 线程token
     **/
    public static void setCurrentToken(String token){
        threadLocal.set(token);
    }
    /**
     * @description: 获取值
     * @author: yzm
     **/
    public static String getCurrentToken(){
        return threadLocal.get();
    }
}