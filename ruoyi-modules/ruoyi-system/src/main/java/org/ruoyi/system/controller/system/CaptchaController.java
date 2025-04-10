package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import cn.hutool.captcha.AbstractCaptcha;
import cn.hutool.captcha.generator.CodeGenerator;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.constant.GlobalConstants;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.ConfigService;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.ruoyi.common.core.utils.reflect.ReflectUtils;
import org.ruoyi.common.mail.utils.MailUtils;
import org.ruoyi.common.redis.utils.RedisUtils;
import org.ruoyi.common.sms.config.properties.SmsProperties;
import org.ruoyi.common.sms.core.SmsTemplate;
import org.ruoyi.common.sms.entity.SmsResult;
import org.ruoyi.common.web.config.properties.CaptchaProperties;
import org.ruoyi.common.web.enums.CaptchaType;
import org.ruoyi.system.domain.request.EmailRequest;
import org.ruoyi.system.domain.vo.CaptchaVo;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * 验证码操作处理
 *
 * @author Lion Li
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class CaptchaController {

    private final CaptchaProperties captchaProperties;
    private final SmsProperties smsProperties;
    private final ConfigService configService;

    /**
     * 短信验证码
     *
     * @param phonenumber 用户手机号
     */
    @GetMapping("/resource/sms/code")
    public R<Void> smsCode(@NotBlank(message = "{user.phonenumber.not.blank}") String phonenumber) {
        if (!smsProperties.getEnabled()) {
            return R.fail("当前系统没有开启短信功能！");
        }
        String key = GlobalConstants.CAPTCHA_CODE_KEY + phonenumber;
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        // 验证码模板id 自行处理 (查数据库或写死均可)
        String templateId = "";
        Map<String, String> map = new HashMap<>(1);
        map.put("code", code);
        SmsTemplate smsTemplate = SpringUtils.getBean(SmsTemplate.class);
        SmsResult result = smsTemplate.send(phonenumber, templateId, map);
        if (!result.isSuccess()) {
            log.error("验证码短信发送异常 => {}", result);
            return R.fail(result.getMessage());
        }
        return R.ok();
    }

    /**
     * 邮箱验证码
     *
     * @param emailRequest 用户邮箱
     */
    @PostMapping("/resource/email/code")
    public R<Void> emailCode(@RequestBody @Valid EmailRequest emailRequest) {
        String key = GlobalConstants.CAPTCHA_CODE_KEY + emailRequest.getUsername();
        String code = RandomUtil.randomNumbers(4);
        RedisUtils.setCacheObject(key, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        // 检验邮箱后缀
        String suffix = configService.getConfigValue("mail", "suffix");
        String prompt = configService.getConfigValue("mail", "prompt");
        if(StringUtils.isNotEmpty(suffix)){
            String[] invalidDomains = suffix.split(",");
            for (String domain : invalidDomains) {
                if (emailRequest.getUsername().endsWith(domain)) {
                    throw new ServiceException(prompt);
                }
            }
        }
        // 自定义邮箱模板
        String model = configService.getConfigValue("mail", "mailModel");
        String mailTitle = configService.getConfigValue("mail", "mailTitle");
        String replacedModel = model.replace("{code}", code);
        try {
           MailUtils.sendHtml(emailRequest.getUsername(), mailTitle, replacedModel);
        } catch (Exception e) {
            log.error("邮箱验证码发送异常 => {}", e.getMessage());
            return R.fail(e.getMessage());
        }
        return R.ok();
    }

    /**
     * 生成验证码
     */
    @GetMapping("/auth/code")
    public R<CaptchaVo> getCode() {
        CaptchaVo captchaVo = new CaptchaVo();
        boolean captchaEnabled = captchaProperties.getEnable();
        if (!captchaEnabled) {
            captchaVo.setCaptchaEnabled(false);
            return R.ok(captchaVo);
        }
        // 保存验证码信息
        String uuid = IdUtil.simpleUUID();
        String verifyKey = GlobalConstants.CAPTCHA_CODE_KEY + uuid;
        // 生成验证码
        CaptchaType captchaType = captchaProperties.getType();
        boolean isMath = CaptchaType.MATH == captchaType;
        Integer length = isMath ? captchaProperties.getNumberLength() : captchaProperties.getCharLength();
        CodeGenerator codeGenerator = ReflectUtils.newInstance(captchaType.getClazz(), length);
        AbstractCaptcha captcha = SpringUtils.getBean(captchaProperties.getCategory().getClazz());
        captcha.setGenerator(codeGenerator);
        captcha.createCode();
        String code = captcha.getCode();
        if (isMath) {
            ExpressionParser parser = new SpelExpressionParser();
            Expression exp = parser.parseExpression(StringUtils.remove(code, "="));
            code = exp.getValue(String.class);
        }
        RedisUtils.setCacheObject(verifyKey, code, Duration.ofMinutes(Constants.CAPTCHA_EXPIRATION));
        captchaVo.setUuid(uuid);
        captchaVo.setImg(captcha.getImageBase64());
        return R.ok(captchaVo);
    }

}
