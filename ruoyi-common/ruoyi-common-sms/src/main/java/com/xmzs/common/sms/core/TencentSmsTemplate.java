package com.xmzs.common.sms.core;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import com.xmzs.common.core.utils.StringUtils;
import com.xmzs.common.json.utils.JsonUtils;
import com.xmzs.common.sms.config.properties.SmsProperties;
import com.xmzs.common.sms.entity.SmsResult;
import com.xmzs.common.sms.exception.SmsException;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20190711.SmsClient;
import com.tencentcloudapi.sms.v20190711.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20190711.models.SendSmsResponse;
import com.tencentcloudapi.sms.v20190711.models.SendStatus;
import lombok.SneakyThrows;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tencent 短信模板
 *
 * @author Lion Li
 * @version 4.2.0
 */
public class TencentSmsTemplate implements SmsTemplate {

    private SmsProperties properties;

    private SmsClient client;

    @SneakyThrows(Exception.class)
    public TencentSmsTemplate(SmsProperties smsProperties) {
        this.properties = smsProperties;
        Credential credential = new Credential(smsProperties.getAccessKeyId(), smsProperties.getAccessKeySecret());
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint(smsProperties.getEndpoint());
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        this.client = new SmsClient(credential, "", clientProfile);
    }

    @Override
    public SmsResult send(String phones, String templateId, Map<String, String> param) {
        if (StringUtils.isBlank(phones)) {
            throw new SmsException("手机号不能为空");
        }
        if (StringUtils.isBlank(templateId)) {
            throw new SmsException("模板ID不能为空");
        }
        SendSmsRequest req = new SendSmsRequest();
        Set<String> set = Arrays.stream(phones.split(StringUtils.SEPARATOR)).map(p -> "+86" + p).collect(Collectors.toSet());
        req.setPhoneNumberSet(ArrayUtil.toArray(set, String.class));
        if (CollUtil.isNotEmpty(param)) {
            req.setTemplateParamSet(ArrayUtil.toArray(param.values(), String.class));
        }
        req.setTemplateID(templateId);
        req.setSign(properties.getSignName());
        req.setSmsSdkAppid(properties.getSdkAppId());
        try {
            SendSmsResponse resp = client.SendSms(req);
            SmsResult.SmsResultBuilder builder = SmsResult.builder()
                .isSuccess(true)
                .message("send success")
                .response(JsonUtils.toJsonString(resp));
            for (SendStatus sendStatus : resp.getSendStatusSet()) {
                if (!"Ok".equals(sendStatus.getCode())) {
                    builder.isSuccess(false).message(sendStatus.getMessage());
                    break;
                }
            }
            return builder.build();
        } catch (Exception e) {
            throw new SmsException(e.getMessage());
        }
    }

}
