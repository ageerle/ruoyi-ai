package org.ruoyi.config;


import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import lombok.val;
import me.chanjar.weixin.common.api.WxConsts;
import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import me.chanjar.weixin.cp.constant.WxCpConsts;
import me.chanjar.weixin.cp.message.WxCpMessageHandler;
import me.chanjar.weixin.cp.message.WxCpMessageRouter;
import org.ruoyi.handler.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * 单实例配置
 *
 * @author <a href="https://github.com/binarywang">Binary Wang</a>
 */
@Configuration
@EnableConfigurationProperties(WxCpProperties.class)
public class WxCpConfiguration {
    private final LogHandler logHandler;
    private NullHandler nullHandler;
    private LocationHandler locationHandler;
    private MenuHandler menuHandler;
    private MsgHandler msgHandler;
    private final UnsubscribeHandler unsubscribeHandler;
    private SubscribeHandler subscribeHandler;
    private WxCpProperties properties;

    private static Map<Integer, WxCpMessageRouter> routers = Maps.newHashMap();
    private static Map<Integer, WxCpService> cpServices = Maps.newHashMap();

    @Autowired
    public WxCpConfiguration(LogHandler logHandler, NullHandler nullHandler, LocationHandler locationHandler,
                             MenuHandler menuHandler, MsgHandler msgHandler, UnsubscribeHandler unsubscribeHandler,
                             SubscribeHandler subscribeHandler, WxCpProperties properties) {
        this.logHandler = logHandler;
        this.nullHandler = nullHandler;
        this.locationHandler = locationHandler;
        this.menuHandler = menuHandler;
        this.msgHandler = msgHandler;
        this.unsubscribeHandler = unsubscribeHandler;
        this.subscribeHandler = subscribeHandler;
        this.properties = properties;
    }


    public static Map<Integer, WxCpMessageRouter> getRouters() {
        return routers;
    }

    public static WxCpService getCpService(Integer agentId) {
        return cpServices.get(agentId);
    }

    @PostConstruct
    public void initServices() {
        cpServices = this.properties.getAppConfigs().stream().map(a -> {
            val configStorage = new WxCpDefaultConfigImpl();
            configStorage.setCorpId(this.properties.getCorpId());
            configStorage.setAgentId(a.getAgentId());
            configStorage.setCorpSecret(a.getSecret());
            configStorage.setToken(a.getToken());
            configStorage.setAesKey(a.getAesKey());
            val service = new WxCpServiceImpl();
            service.setWxCpConfigStorage(configStorage);
            routers.put(a.getAgentId(), this.newRouter(service));
            return service;
        }).collect(Collectors.toMap(service -> service.getWxCpConfigStorage().getAgentId(), a -> a));
    }

    private WxCpMessageRouter newRouter(WxCpService wxCpService) {
        final val newRouter = new WxCpMessageRouter(wxCpService);

        // 记录所有事件的日志 （异步执行）
        newRouter.rule().handler(this.logHandler).next();

        // 自定义菜单事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.MenuButtonType.CLICK).handler(this.menuHandler).end();

        // 点击菜单链接事件（这里使用了一个空的处理器，可以根据自己需要进行扩展）
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.MenuButtonType.VIEW).handler(this.nullHandler).end();

        // 关注事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.EventType.SUBSCRIBE).handler(this.subscribeHandler)
            .end();

        // 取消关注事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.EventType.UNSUBSCRIBE)
            .handler((WxCpMessageHandler) this.unsubscribeHandler).end();

        // 上报地理位置事件
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.EventType.LOCATION).handler(this.locationHandler)
            .end();

        // 接收地理位置消息
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.LOCATION)
            .handler(this.locationHandler).end();

        // 扫码事件（这里使用了一个空的处理器，可以根据自己需要进行扩展）
        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxConsts.EventType.SCAN).handler((WxCpMessageHandler) this.nullHandler).end();

        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxCpConsts.EventType.CHANGE_CONTACT).handler(new ContactChangeHandler()).end();

        newRouter.rule().async(false).msgType(WxConsts.XmlMsgType.EVENT)
            .event(WxCpConsts.EventType.ENTER_AGENT).handler(new EnterAgentHandler()).end();

        // 默认
        newRouter.rule().async(false).handler(this.msgHandler).end();

        return newRouter;
    }

}
