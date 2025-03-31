package tech.ordinaryroad.live.chat.client.kuaishou.client;

import cn.hutool.core.thread.ThreadUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.commons.client.enums.ClientStatusEnums;
import tech.ordinaryroad.live.chat.client.kuaishou.config.KuaishouLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.kuaishou.listener.IKuaishouMsgListener;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouDanmuMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouGiftMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouLikeMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.netty.handler.KuaishouBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.PayloadTypeOuterClass;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.WebGiftFeedOuterClass;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mjz
 * @date 2024/1/5
 */
@Slf4j
class KuaishouLiveChatClientTest {

    Map<String, List<WebGiftFeedOuterClass.WebGiftFeed>> map = new HashMap<>();

    static Object lock = new Object();
    KuaishouLiveChatClient client;

    @Test
    void example() throws InterruptedException {
        String cookie = System.getenv("cookie");
//        String cookie ="";
        log.error("cookie: {}", cookie);
        KuaishouLiveChatClientConfig config = KuaishouLiveChatClientConfig.builder()
                // TODO 浏览器Cookie
                .cookie(cookie)
                .roomId("3xpbyu443usqwau")
                .roomId("DX204317461")
                .roomId("xzx11234")
                .roomId("N-ik-T8-20")
                .roomId("3x9f7e3t3fsr74k")
                .roomId("3xiqpb2riusznvq")
                .roomId("QQ2027379716")
                .roomId("xiannvwan1008")
                // 祁天道
                .roomId("t8888888")
                .roomId("by529529")
                // 大师2
                .roomId("3x6pb6bcmjrarvs")
                .roomId("3xbyfeffjhky7b2")
                // 月神
                .roomId("YUE99999")
                .roomId("mengyu980726")
                // 大师1
                .roomId("3xkz5pb2kx3q4u6")
                .build();

        client = new KuaishouLiveChatClient(config, new IKuaishouMsgListener() {
            @Override
            public void onMsg(IMsg msg) {
                // log.debug("收到{}消息 {}", msg.getClass(), msg);
            }

            @Override
            public void onCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
                // log.debug("收到CMD消息{} {}", cmd, cmdMsg);
            }

            @Override
            public void onOtherCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
                log.debug("收到其他CMD消息 {}", cmd);
            }

            @Override
            public void onUnknownCmd(String cmdString, IMsg msg) {
                log.debug("收到未知CMD消息 {}", cmdString);
            }

            @Override
            public void onDanmuMsg(KuaishouBinaryFrameHandler binaryFrameHandler, KuaishouDanmuMsg msg) {
                log.info("{} 收到弹幕 [{}] {}({})：{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onGiftMsg(KuaishouBinaryFrameHandler binaryFrameHandler, KuaishouGiftMsg msg) {
                String mergeKey = msg.getMsg().getMergeKey();
                map.computeIfAbsent(mergeKey, s -> new ArrayList<>()).add(msg.getMsg());

                log.info("{} 收到礼物 [{}] {}({}) {} {}({})x{}({}) mergeKey:{},comboCount:{}, batchSize:{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), "赠送", msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice(), msg.getMsg().getMergeKey(), msg.getMsg().getComboCount(), msg.getMsg().getBatchSize());
            }
        });

        client.addStatusChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                ClientStatusEnums newValue = (ClientStatusEnums) evt.getNewValue();
                if (newValue == ClientStatusEnums.CONNECTED) {
                    // 连接成功5秒后发送弹幕
                    ThreadUtil.execAsync(() -> {
                        ThreadUtil.sleep(10000);
                        client.sendDanmu("666666", new Runnable() {
                            @Override
                            public void run() {
                                log.warn("弹幕发送成功");
                            }
                        });
                    });
                }
            }
        });

        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

    @Test
    void clickLike() throws Exception {
        String cookie = System.getenv("cookie");
//        String cookie ="";
        log.error("cookie: {}", cookie);
        KuaishouLiveChatClientConfig config = KuaishouLiveChatClientConfig.builder()
                .cookie(cookie)
                .roomId("K6866676")
                .roomId("xinsang_")
                .roomId("lollaopu")
                .roomId("YTC2844073618")
                .build();

        client = new KuaishouLiveChatClient(config, new IKuaishouMsgListener() {
            @Override
            public void onMsg(IMsg msg) {
//                log.debug("收到{}消息 {}", msg.getClass(), msg);
            }

            @Override
            public void onCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
//                log.debug("收到CMD消息{} {}", cmd, cmdMsg);
            }

            @Override
            public void onOtherCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
//                log.debug("收到其他CMD消息 {}", cmd);
            }

            @Override
            public void onUnknownCmd(String cmdString, IMsg msg) {
//                log.debug("收到未知CMD消息 {}", cmdString);
            }

            @Override
            public void onDanmuMsg(KuaishouBinaryFrameHandler binaryFrameHandler, KuaishouDanmuMsg msg) {
                log.info("{} 收到弹幕 [{}] {}({})：{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onGiftMsg(KuaishouBinaryFrameHandler binaryFrameHandler, KuaishouGiftMsg msg) {
                log.info("{} 收到礼物 [{}] {}({}) {} {}({})x{}({})", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), "赠送", msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice());
            }

            @Override
            public void onLikeMsg(KuaishouBinaryFrameHandler binaryFrameHandler, KuaishouLikeMsg msg) {
                log.info("{} 收到点赞 [{}] {}({})", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }
        });

        client.addStatusChangeListener(evt -> {
            ClientStatusEnums newValue = (ClientStatusEnums) evt.getNewValue();
            if (newValue == ClientStatusEnums.CONNECTED) {
                // 连接成功10秒后发送弹幕
                ThreadUtil.execAsync(() -> {
                    ThreadUtil.sleep(10000);
                    client.sendDanmu("6666a", () -> {
                        log.warn("弹幕发送成功");
                        client.clickLike(13, () -> {
                            log.warn("为直播间点赞成功");
                        });
                    });
                });
            }
        });

        client.connect();

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

}