package org.ruoyi.demo.controller;

import lombok.RequiredArgsConstructor;
import org.ruoyi.common.core.domain.R;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;

/**
 * 短信演示案例
 * 请先阅读文档 否则无法使用
 *
 * @author Lion Li
 * @version 4.2.0
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/demo/sms")
public class SmsController {
    /**
     * 发送短信Aliyun
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendAliyun")
    public R<Object> sendAliyun(String phones, String templateId) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
        map.put("code", "1234");
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        SmsResponse smsResponse = smsBlend.sendMessage(phones, templateId, map);
        return R.ok(smsResponse);
    }

    /**
     * 发送短信Tencent
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendTencent")
    public R<Object> sendTencent(String phones, String templateId) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(1);
//        map.put("2", "测试测试");
        map.put("1", "1234");
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config2");
        SmsResponse smsResponse = smsBlend.sendMessage(phones, templateId, map);
        return R.ok(smsResponse);
    }

    /**
     * 添加黑名单
     *
     * @param phone 手机号
     */
    @GetMapping("/addBlacklist")
    public R<Object> addBlacklist(String phone){
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        smsBlend.joinInBlacklist(phone);
        return R.ok();
    }

    /**
     * 移除黑名单
     *
     * @param phone 手机号
     */
    @GetMapping("/removeBlacklist")
    public R<Object> removeBlacklist(String phone){
        SmsBlend smsBlend = SmsFactory.getSmsBlend("config1");
        smsBlend.removeFromBlacklist(phone);
        return R.ok();
    }

}
