package org.ruoyi.controller;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.domin.WeixinQrCode;
import org.ruoyi.service.VxLoginService;
import org.ruoyi.system.domain.vo.LoginVo;
import org.ruoyi.util.WeixinApiUtil;
import org.ruoyi.util.WeixinQrCodeCacheUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author https://www.wdbyte.com
 */
@Slf4j
@RestController
public class WeixinUserController {

    @Autowired
    private WeixinApiUtil weixinApiUtil;

    @Autowired
    private VxLoginService loginService;

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
    public R<LoginVo> userLogin(String ticket) {
        String openId = WeixinQrCodeCacheUtil.get(ticket);
        if (StringUtils.isNotEmpty(openId)) {
            log.info("login success,open id:{}", openId);
            LoginVo loginVo = loginService.mpLogin(openId);
            return R.ok(loginVo);
        }
        log.info("login error,ticket:{}", ticket);
        return R.fail("check failed");
    }
}
