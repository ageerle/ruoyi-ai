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

package tech.ordinaryroad.live.chat.client.bilibili.client;

import cn.hutool.core.thread.ThreadUtil;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.ordinaryroad.live.chat.client.bilibili.config.BilibiliLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.bilibili.constant.BilibiliCmdEnum;
import tech.ordinaryroad.live.chat.client.bilibili.listener.IBilibiliMsgListener;
import tech.ordinaryroad.live.chat.client.bilibili.msg.*;
import tech.ordinaryroad.live.chat.client.bilibili.netty.handler.BilibiliBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.commons.client.enums.ClientStatusEnums;

/**
 * @author mjz
 * @date 2023/8/26
 */
@Slf4j
class BilibiliLiveChatClientTest {

    static Object lock = new Object();
    BilibiliLiveChatClient client;

    @Test
    void example() throws InterruptedException {
        String cookie = System.getenv("cookie");
        log.error("cookie: {}", cookie);
        BilibiliLiveChatClientConfig config = BilibiliLiveChatClientConfig.builder()
                // TODO 浏览器Cookie
                .cookie(cookie)
                .roomId(7777)
                .roomId(697)
                .build();

        client = new BilibiliLiveChatClient(config, new IBilibiliMsgListener() {
            @Override
            public void onDanmuMsg(BilibiliBinaryFrameHandler binaryFrameHandler, DanmuMsgMsg msg) {
                IBilibiliMsgListener.super.onDanmuMsg(binaryFrameHandler, msg);
                log.info("{} 收到弹幕 {} {}({})：{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onGiftMsg(BilibiliBinaryFrameHandler binaryFrameHandler, SendGiftMsg msg) {
                IBilibiliMsgListener.super.onGiftMsg(binaryFrameHandler, msg);
                log.info("{} 收到礼物 {} {}({}) {} {}({})x{}({})", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), msg.getData().getAction(), msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice());
            }

            @Override
            public void onSuperChatMsg(BilibiliBinaryFrameHandler binaryFrameHandler, SuperChatMessageMsg msg) {
                IBilibiliMsgListener.super.onSuperChatMsg(binaryFrameHandler, msg);
                log.info("{} 收到醒目留言 {}({})：{}", binaryFrameHandler.getRoomId(), msg.getUsername(), msg.getUid(), msg.getContent());
            }

            @Override
            public void onEnterRoomMsg(InteractWordMsg msg) {
                log.info("{} {}({}) 进入直播间", msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }

            @Override
            public void onLikeMsg(BilibiliBinaryFrameHandler binaryFrameHandler, LikeInfoV3ClickMsg msg) {
                IBilibiliMsgListener.super.onLikeMsg(binaryFrameHandler, msg);
                log.info("{} 收到点赞 {} {}({})", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid());
            }

            @Override
            public void onEntryEffect(SendSmsReplyMsg msg) {
                JsonNode data = msg.getData();
                String copyWriting = data.get("copy_writing").asText();
                log.info("入场效果 {}", copyWriting);
            }

            @Override
            public void onWatchedChange(SendSmsReplyMsg msg) {
                JsonNode data = msg.getData();
                int num = data.get("num").asInt();
                String textSmall = data.get("text_small").asText();
                String textLarge = data.get("text_large").asText();
                log.debug("观看人数变化 {} {} {}", num, textSmall, textLarge);
            }

            @Override
            public void onClickLike(SendSmsReplyMsg msg) {
                JsonNode data = msg.getData();
                String uname = data.get("uname").asText();
                String likeText = data.get("like_text").asText();
                log.debug("为主播点赞 {} {}", uname, likeText);
            }

            @Override
            public void onClickUpdate(SendSmsReplyMsg msg) {
                JsonNode data = msg.getData();
                int clickCount = data.get("click_count").asInt();
                log.debug("点赞数更新 {}", clickCount);
            }

            @Override
            public void onMsg(IMsg msg) {
                log.debug("收到{}消息 {}", msg.getClass(), msg);
            }

            @Override
            public void onCmdMsg(BilibiliCmdEnum cmd, ICmdMsg<BilibiliCmdEnum> cmdMsg) {
                log.debug("收到CMD消息{} {}", cmd, cmdMsg);
            }

            @Override
            public void onOtherCmdMsg(BilibiliCmdEnum cmd, ICmdMsg<BilibiliCmdEnum> cmdMsg) {
//                log.debug("收到其他CMD消息 {}", cmd);
                switch (cmd) {
                    case GUARD_BUY: {
                        // 有人上舰
                        SendSmsReplyMsg sendSmsReplyMsg = (SendSmsReplyMsg) cmdMsg;
                        break;
                    }
                    case SUPER_CHAT_MESSAGE_DELETE: {
                        // 删除醒目留言
                        SendSmsReplyMsg sendSmsReplyMsg = (SendSmsReplyMsg) cmdMsg;
                        break;
                    }
                }
            }

            @Override
            public void onUnknownCmd(String cmdString, IMsg msg) {
                log.debug("收到未知CMD消息 {}", cmdString);
            }
        });
        client.connect();

        client.addStatusChangeListener(evt -> {
            ClientStatusEnums newValue = (ClientStatusEnums) evt.getNewValue();
            if (newValue == ClientStatusEnums.CONNECTED) {
                ThreadUtil.execAsync(() -> {
                    ThreadUtil.sleep(5000);
                    client.clickLike(5, () -> {
                        log.warn("为主播点赞成功");
                    });
                });
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
