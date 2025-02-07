package org.ruoyi.common.sms.core;

import com.aliyun.dysmsapi20170525.Client;
import com.aliyun.dysmsapi20170525.models.SendSmsRequest;
import com.aliyun.dysmsapi20170525.models.SendSmsResponse;
import com.aliyun.teaopenapi.models.Config;
import lombok.SneakyThrows;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.json.utils.JsonUtils;
import org.ruoyi.common.sms.config.properties.SmsProperties;
import org.ruoyi.common.sms.entity.SmsResult;
import org.ruoyi.common.sms.exception.SmsException;

import java.util.Map;

/**
 * Aliyun 短信模板
 *
 * @author Lion Li
 * @version 4.2.0
 */
public class AliyunSmsTemplate implements SmsTemplate {

    private SmsProperties properties;

    private Client client;

    @SneakyThrows(Exception.class)
    public AliyunSmsTemplate(SmsProperties smsProperties) {
        this.properties = smsProperties;
        Config config = new Config()
            // 您的AccessKey ID
            .setAccessKeyId(smsProperties.getAccessKeyId())
            // 您的AccessKey Secret
            .setAccessKeySecret(smsProperties.getAccessKeySecret())
            // 访问的域名
            .setEndpoint(smsProperties.getEndpoint());
        this.client = new Client(config);
    }

    @Override
    public SmsResult send(String phones, String templateId, Map<String, String> param) {
        if (StringUtils.isBlank(phones)) {
            throw new SmsException("手机号不能为空");
        }
        if (StringUtils.isBlank(templateId)) {
            throw new SmsException("模板ID不能为空");
        }
        SendSmsRequest req = new SendSmsRequest()
            .setPhoneNumbers(phones)
            .setSignName(properties.getSignName())
            .setTemplateCode(templateId)
            .setTemplateParam(JsonUtils.toJsonString(param));
        try {
            SendSmsResponse resp = client.sendSms(req);
            return SmsResult.builder()
                .isSuccess("OK".equals(resp.getBody().getCode()))
                .message(resp.getBody().getMessage())
                .response(JsonUtils.toJsonString(resp))
                .build();
        } catch (Exception e) {
            throw new SmsException(e.getMessage());
        }
    }

}
