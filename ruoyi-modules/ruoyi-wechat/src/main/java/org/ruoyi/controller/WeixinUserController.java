package org.ruoyi.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.domain.dto.VisitorLoginUserDto;
import org.ruoyi.common.core.service.UserLoginService;
import org.ruoyi.domin.WeixinQrCode;
import org.ruoyi.util.WeixinApiUtil;
import org.ruoyi.util.WeixinQrCodeCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 微信公众号登录
 *
 * @author ageerle
 * @date 2025-05-03
 */
@Slf4j
@RestController
public class WeixinUserController {

    @Autowired
    private WeixinApiUtil weixinApiUtil;

    @Autowired
    private UserLoginService userLoginService;

    @GetMapping(value = "/user/qrcode")
    public R<WeixinQrCode> getQrCode() {
        WeixinQrCode qrCode = weixinApiUtil.getQrCode();
        qrCode.setUrl(null);
        qrCode.setExpireSeconds(null);
        return R.ok(qrCode);
    }

    /**
     * 校验是否扫描完成
     * 完成，返回 JWT
     * 未完成，返回 check failed
     */
    @GetMapping(value = "/user/login/qrcode")
    public R<VisitorLoginUserDto> userLogin(String ticket,String clientId) {
        String openId = WeixinQrCodeCacheUtil.get(ticket);
        if (StringUtils.isNotEmpty(openId)) {
            log.info("login success,open id:{}", openId);
            VisitorLoginUserDto loginBody = userLoginService.mpLogin(openId,clientId);
            return R.ok(loginBody);
        }
        log.info("login error,ticket:{}", ticket);
        return R.fail("check failed");
    }
}
