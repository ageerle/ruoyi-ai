package com.xmzs.common.wechat.controller;


import com.xmzs.common.core.exception.base.BaseException;
import com.xmzs.common.wechat.utils.SleepUtils;
import com.xmzs.common.wechat.utils.enums.URLEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xmzs.common.wechat.api.WechatTools;
import com.xmzs.common.wechat.core.Core;
import com.xmzs.common.wechat.service.ILoginService;
import com.xmzs.common.wechat.service.impl.LoginServiceImpl;
import com.xmzs.common.wechat.thread.CheckLoginStatusThread;

import com.xmzs.common.wechat.utils.tools.CommonTools;

/**
 * 登陆控制器
 *
 * @author https://github.com/yaphone
 * @version 1.0
 * @date 创建时间：2017年5月13日 下午12:56:07
 */
public class LoginController {
    private static Logger LOG = LoggerFactory.getLogger(LoginController.class);
    private ILoginService loginService = new LoginServiceImpl();
    private static Core core = Core.getInstance();


    /**
     * 获取二维码地址
     * 风险:已登录账号不可调用该接口,会移除当前core信息
     * @return
     */
    public String login_1() {
        if (core.isAlive()) {
            LOG.warn("微信已登陆");
            throw new BaseException("微信已登陆");
        }
        LOG.info("1.获取微信UUID");
        while (loginService.getUuid() == null) {
            LOG.warn("1.1. 获取微信UUID失败，一秒后重新获取");
            SleepUtils.sleep(1000);
        }
        LOG.info("2. 获取登陆二维码图片");
        return URLEnum.QRCODE_URL.getUrl() + core.getUuid();
    }


    public void login_2() {
        LOG.info("3. 请扫描二维码图片，并在手机上确认");
        if (!core.isAlive()) {
            loginService.login();
            core.setAlive(true);
            LOG.info(("登陆成功"));
        }
        LOG.info("4. 登陆超时，请重新扫描二维码图片");


        LOG.info("5. 登陆成功，微信初始化");
        if (!loginService.webWxInit()) {
            LOG.info("6. 微信初始化异常");
            System.exit(0);
        }

        LOG.info("6. 开启微信状态通知");
        loginService.wxStatusNotify();

        LOG.info("7. 清除。。。。");
        CommonTools.clearScreen();
        LOG.info(String.format("欢迎回来， %s", core.getNickName()));

        LOG.info("8. 开始接收消息");
        loginService.startReceiving();

        LOG.info("9. 获取联系人信息");
        loginService.webWxGetContact();

        LOG.info("10. 获取群好友及群好友列表");
        loginService.WebWxBatchGetContact();

        LOG.info("11. 缓存本次登陆好友相关消息");
        WechatTools.setUserInfo(); // 登陆成功后缓存本次登陆好友相关消息（NickName, UserName）

        LOG.info("12.开启微信状态检测线程");
        new Thread(new CheckLoginStatusThread()).start();
    }
}
