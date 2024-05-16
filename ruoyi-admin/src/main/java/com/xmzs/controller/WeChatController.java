package com.xmzs.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xmzs.common.core.domain.R;
import com.xmzs.common.wechat.Wechat;
import com.xmzs.common.wechat.controller.LoginController;
import com.xmzs.common.wechat.core.MsgCenter;
import com.xmzs.system.cofing.KeywordConfig;
import com.xmzs.system.cofing.WechatConfig;
import com.xmzs.system.handler.WechatMessageHandler;
import com.xmzs.system.service.ISseService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 个人微信扩展控制器
 *
 * @author WangLe
 */
@SaIgnore
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
public class WeChatController {

    @Getter
    private Wechat wechatBot;

    private final WechatConfig wechatConfig;

    private final ISseService sseService;

    private final  KeywordConfig keywordConfig;

    /**
     * 获取微信登录二维码
     *
     */
    @GetMapping("/getQr")
    public R<String> getQr() {
        //微信
        if (wechatConfig.getEnable()){
            log.info("正在登录微信,请按提示操作：");
            wechatBot = new Wechat(new WechatMessageHandler(sseService, keywordConfig));
            // 登陆
            LoginController login = new LoginController();
            String qrCode = login.login_1();
            new Thread(login::login_2).start();
            wechatBot.start();
            return R.ok(qrCode);
        }else {
            return R.fail();
        }
    }

}
