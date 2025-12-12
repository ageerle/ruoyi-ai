package org.ruoyi.service;

/**
 * 企业微信聊天管理Service接口
 *
 * @author ageerle
 * @date 2025-04-08
 */
public interface IChatVxService {


    /**
     * 企业微信应用回复
     *
     * @param prompt 提示词
     * @return 回复内容
     */
    String chat(String prompt);

}
