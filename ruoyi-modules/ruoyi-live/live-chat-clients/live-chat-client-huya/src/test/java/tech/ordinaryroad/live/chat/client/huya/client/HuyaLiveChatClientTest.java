/*
 * MIT License
 *
 * Copyright (c) 2023 OrdinaryRoad
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package tech.ordinaryroad.live.chat.client.huya.client;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.huya.config.HuyaLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaCmdEnum;
import tech.ordinaryroad.live.chat.client.huya.listener.IHuyaMsgListener;
import tech.ordinaryroad.live.chat.client.huya.msg.MessageNoticeMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.SendItemSubBroadcastPacketMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.VipEnterBannerMsg;
import tech.ordinaryroad.live.chat.client.huya.netty.handler.HuyaBinaryFrameHandler;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mjz
 * @date 2023/9/5
 */
@Slf4j
class HuyaLiveChatClientTest {

    static Object lock = new Object();
    HuyaLiveChatClient client;

    @Test
    void example() throws InterruptedException {
        HuyaLiveChatClientConfig config = HuyaLiveChatClientConfig.builder()
                .roomId(353322)
                .roomId(390001)
                .roomId(527988)
                .roomId(1995)
                .roomId(116)
                // bagea
                .roomId(189201)
                .build();

        client = new HuyaLiveChatClient(config, new IHuyaMsgListener() {
            @Override
            public void onDanmuMsg(HuyaBinaryFrameHandler binaryFrameHandler, MessageNoticeMsg msg) {
                log.info("{} 收到弹幕 {} {}({})：{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onGiftMsg(HuyaBinaryFrameHandler binaryFrameHandler, SendItemSubBroadcastPacketMsg msg) {
                long lPayTotal = msg.getLPayTotal();
                if (lPayTotal != 0) {
                    int giftPrice = msg.getGiftPrice();
                }
                log.info("{} 收到礼物 {}({}) {} {}({})x{}({})", binaryFrameHandler.getRoomId(), msg.getUsername(), msg.getUid(), "赠送", msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice());
            }

            @Override
            public void onEnterRoomMsg(HuyaBinaryFrameHandler binaryFrameHandler, VipEnterBannerMsg msg) {
                // 虎牙目前只支持监听VIP用户的入房消息
                log.info("{} {}({}) 进入直播间", msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }

            @Override
            public void onMsg(HuyaBinaryFrameHandler binaryFrameHandler, IMsg msg) {
                log.debug("{} 收到{}消息 {}", binaryFrameHandler.getRoomId(), msg.getClass(), msg);
            }

            @Override
            public void onCmdMsg(HuyaBinaryFrameHandler binaryFrameHandler, HuyaCmdEnum cmd, ICmdMsg<HuyaCmdEnum> cmdMsg) {
                log.info("{} 收到CMD消息{} {}", binaryFrameHandler.getRoomId(), cmd, cmdMsg);
            }

            @Override
            public void onOtherCmdMsg(HuyaBinaryFrameHandler binaryFrameHandler, HuyaCmdEnum cmd, ICmdMsg<HuyaCmdEnum> cmdMsg) {
                log.debug("{} 收到其他CMD消息 {}", binaryFrameHandler.getRoomId(), cmd);
            }

            @Override
            public void onUnknownCmd(HuyaBinaryFrameHandler binaryFrameHandler, String cmdString, IMsg msg) {
                log.debug("{} 收到未知CMD消息 {}", binaryFrameHandler.getRoomId(), cmdString);
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
    void sendDanmuTest() throws InterruptedException {
        String cookie = System.getenv("cookie");
        assertTrue(StrUtil.isNotBlank(cookie));
        log.error("cookie: {}", cookie);

        HuyaLiveChatClientConfig config = HuyaLiveChatClientConfig.builder()
                .cookie(cookie)
                .roomId(189201)
                .build();

        client = new HuyaLiveChatClient(config);
        client.connect(() -> {
            String danmu = "66666" + RandomUtil.randomNumber();
            log.info("连接成功，5s后发送弹幕{}", danmu);
            ThreadUtil.sleep(5000);
            client.sendDanmu(danmu);
        });
        client.addMsgListener(new IHuyaMsgListener() {
            @Override
            public void onMsg(IMsg msg) {
                log.info("收到消息{}", msg);
            }
        });

        // 防止测试时直接退出
        while (true) {
            synchronized (lock) {
                lock.wait();
            }
        }
    }

}