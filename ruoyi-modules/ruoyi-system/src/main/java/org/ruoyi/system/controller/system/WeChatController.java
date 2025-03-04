package org.ruoyi.system.controller.system;

import cn.dev33.satoken.annotation.SaIgnore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.wechat.itchat4j.api.WechatTools;
import org.ruoyi.common.wechat.itchat4j.controller.LoginController;
import org.ruoyi.common.wechat.itchat4j.core.MsgCenter;
import org.ruoyi.common.wechat.itchat4j.face.IMsgHandlerFace;
import org.ruoyi.common.wechat.web.base.BaseException;
import org.ruoyi.system.domain.bo.WxRobConfigBo;
import org.ruoyi.system.domain.vo.WxRobConfigVo;
import org.ruoyi.system.handler.MyMsgHandler;
import org.ruoyi.system.service.ISseService;
import org.ruoyi.system.service.IWxRobConfigService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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

    private final ISseService sseService;

    private final IWxRobConfigService wxRobConfigService;

    /**
     * 登录第一步，获取二维码链接
     * @throws BaseException
     */
    @PostMapping("/getQr")
    public String getQr(@RequestParam String uniqueKey) {
        LoginController login = new LoginController(uniqueKey);
        try {
            return login.login_1();
        } catch (BaseException e) {
            throw new ServiceException("获取二维码失败："+ e.getMessage());
        }
    }

    @PostMapping("/wxLogin")
    public Boolean wxLogin(@RequestParam String uniqueKey) {
        LoginController login = new LoginController(uniqueKey);
        return login.login_2();
    }

    @PostMapping("/wxInit")
    public Boolean wxInit(@RequestParam String uniqueKey) {
        LoginController login = new LoginController(uniqueKey);
        // 开启消息处理线程
        WxRobConfigBo wxRobConfigBo = new WxRobConfigBo();
        wxRobConfigBo.setUniqueKey(uniqueKey);
        List<WxRobConfigVo> wxRobConfigVos = wxRobConfigService.queryList(wxRobConfigBo);
        //查询机器人对应的用户
        start(uniqueKey,new MyMsgHandler(uniqueKey,sseService,wxRobConfigVos.get(0)));
        return login.login_3();
    }

    @PostMapping("/wxLogout")
    public void wxLogout(@RequestParam String uniqueKey) {
        WechatTools.logout(uniqueKey);
    }

    public void start(String uniqueKey,IMsgHandlerFace msgHandler) {
        log.info("7.+++开启消息处理线程["+uniqueKey+"]+++");
        new Thread(() -> MsgCenter.handleMsg(uniqueKey,msgHandler)).start();
    }
}
