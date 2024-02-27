package com.xmzs.controller;

import cn.dev33.satoken.annotation.SaIgnore;
import com.xmzs.common.wechat.Wechat;
import com.xmzs.system.cofing.KeywordConfig;
import com.xmzs.system.cofing.QqConfig;
import com.xmzs.system.cofing.WechatConfig;
import com.xmzs.system.handler.WechatMessageHandler;
import com.xmzs.system.service.ISseService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
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
    @PostMapping("/getQr")
    public void getQr() {
        //微信
        if (wechatConfig.getEnable()){
            log.info("正在登录微信,请按提示操作：");
            wechatBot = new Wechat(new WechatMessageHandler(sseService, keywordConfig), wechatConfig.getQrPath());
            wechatBot.start();
        }
    }
}
