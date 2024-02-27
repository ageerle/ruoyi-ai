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

package tech.ordinaryroad.live.chat.client.kuaishou;

import cn.hutool.core.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.commons.client.enums.ClientStatusEnums;
import tech.ordinaryroad.live.chat.client.kuaishou.client.KuaishouLiveChatClient;
import tech.ordinaryroad.live.chat.client.kuaishou.config.KuaishouLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.kuaishou.listener.IKuaishouMsgListener;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouDanmuMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.msg.KuaishouGiftMsg;
import tech.ordinaryroad.live.chat.client.kuaishou.netty.handler.KuaishouBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.PayloadTypeOuterClass;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ClientModeExample {
    static Logger log = LoggerFactory.getLogger(ClientModeExample.class);

    public static void main(String[] args) {
        // 1. 创建配置
        KuaishouLiveChatClientConfig config = KuaishouLiveChatClientConfig.builder()
                // TODO 浏览器Cookie
                .cookie("did=web_6c4ac2a8ef8855d35df4d564baeaa8e8; kuaishou.live.bfb1s=7206d814e5c089a58c910ed8bf52ace5; clientid=3; did=web_6c4ac2a8ef8855d35df4d564baeaa8e8; client_key=65890b29; kpn=GAME_ZONE; userId=3941614875; kuaishou.live.web_st=ChRrdWFpc2hvdS5saXZlLndlYi5zdBKgAbRhgemDxM_Z_lBn7EZ_-5unvtslsh7ci5VY_eg80qqjxy5yb7oBFrdcWSPlz6jMIBz9h_yoLzATE3ngj2WawpxvbJhmq0EnRYIHv308kTBmg4KN2JQf7w2mfrsp1vusFbZ3NkfsEO4PrGQfX0L6mJRbgXeBqyz4tUM5O0q2Jte_NzWkaOnezvIGRAG8Y6yA1dxGUmiA9syTrPrdnSOzZvAaEoJNhwQ4OUDtgURWN6k9Xgm8PSIgAfV-ZvahtgaYhopZno6OuS2pkaFZjrz4ymoEZ1DSnj0oBTAB; kuaishou.live.web_ph=a287be6ab01dce264c0554eed94c2d6ac991; userId=3941614875")
                // TODO 直播间id（支持短id）
                .roomId("tianci666")
                .build();

        // 2. 创建Client并传入配置、添加消息回调
        KuaishouLiveChatClient client = new KuaishouLiveChatClient(config, new IKuaishouMsgListener() {
            @Override
            public void onMsg(IMsg msg) {
                log.debug("收到{}消息 {}", msg.getClass(), msg);
            }

            @Override
            public void onCmdMsg(PayloadTypeOuterClass.PayloadType cmd, ICmdMsg<PayloadTypeOuterClass.PayloadType> cmdMsg) {
                log.debug("收到CMD消息{} {}", cmd, cmdMsg);
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
                log.info("{} 收到礼物 [{}] {}({}) {} {}({})x{}({}) mergeKey:{},comboCount:{}, batchSize:{}", binaryFrameHandler.getRoomId(), msg.getBadgeLevel() != 0 ? msg.getBadgeLevel() + msg.getBadgeName() : "", msg.getUsername(), msg.getUid(), "赠送", msg.getGiftName(), msg.getGiftId(), msg.getGiftCount(), msg.getGiftPrice(), msg.getMsg().getMergeKey(), msg.getMsg().getComboCount(), msg.getMsg().getBatchSize());
            }
        });
        // 3. 开始监听直播间
        client.connect();

        // 客户端连接状态回调
        client.addStatusChangeListener(evt -> {
            if (evt.getNewValue().equals(ClientStatusEnums.CONNECTED)) {
                // TODO 要发送的弹幕内容，请注意控制发送频率；框架内置支持设置发送弹幕的最少时间间隔，小于时将忽略该次发送
                client.sendDanmu("666666" + RandomUtil.randomNumbers(1));
            }
        });
    }
}
