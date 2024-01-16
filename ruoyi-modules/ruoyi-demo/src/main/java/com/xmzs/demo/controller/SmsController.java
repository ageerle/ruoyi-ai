package com.xmzs.demo.controller;

import com.xmzs.common.core.domain.R;
import com.xmzs.common.core.utils.SpringUtils;
import com.xmzs.common.sms.config.properties.SmsProperties;
import com.xmzs.common.sms.core.SmsTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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

    private final SmsProperties smsProperties;
//    private final SmsTemplate smsTemplate; // 可以使用spring注入
//    private final AliyunSmsTemplate smsTemplate; // 也可以注入某个厂家的模板工具

    /**
     * 发送短信Aliyun
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendAliyun")
    public R<Object> sendAliyun(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("aliyunSmsTemplate")) {
            return R.fail("阿里云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
        map.put("code", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

    /**
     * 发送短信Tencent
     *
     * @param phones     电话号
     * @param templateId 模板ID
     */
    @GetMapping("/sendTencent")
    public R<Object> sendTencent(String phones, String templateId) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        if (!SpringUtils.containsBean("tencentSmsTemplate")) {
            return R.fail("腾讯云依赖未引入！");
        }
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        Map<String, String> map = new HashMap<>(1);
//        map.put("2", "测试测试");
        map.put("1", "1234");
        Object send = smsTemplate.send(phones, templateId, map);
        return R.ok(send);
    }

}
