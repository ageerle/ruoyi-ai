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

package tech.ordinaryroad.live.chat.client.bilibili.netty.frame.factory;

import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.NumberUtil;
import com.fasterxml.jackson.databind.JsonNode;
import tech.ordinaryroad.live.chat.client.bilibili.api.BilibiliApis;
import tech.ordinaryroad.live.chat.client.bilibili.constant.ProtoverEnum;
import tech.ordinaryroad.live.chat.client.bilibili.msg.AuthMsg;
import tech.ordinaryroad.live.chat.client.bilibili.msg.HeartbeatMsg;
import tech.ordinaryroad.live.chat.client.bilibili.netty.frame.AuthWebSocketFrame;
import tech.ordinaryroad.live.chat.client.bilibili.netty.frame.HeartbeatWebSocketFrame;
import tech.ordinaryroad.live.chat.client.bilibili.util.BilibiliCodecUtil;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatCookieUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author mjz
 * @date 2023/1/5
 */
public class BilibiliWebSocketFrameFactory {

    private static final ConcurrentHashMap<Long, BilibiliWebSocketFrameFactory> CACHE = new ConcurrentHashMap<>();

    /**
     * 浏览器地址中的房间id，支持短id
     */
    private final long roomId;
    private volatile static HeartbeatMsg heartbeatMsg;

    public BilibiliWebSocketFrameFactory(long roomId) {
        this.roomId = roomId;
    }

    public synchronized static BilibiliWebSocketFrameFactory getInstance(long roomId) {
        return CACHE.computeIfAbsent(roomId, aLong -> new BilibiliWebSocketFrameFactory(roomId));
    }

    /**
     * 创建认证包
     *
     * @param protover {@link ProtoverEnum}
     * @param cookie   浏览器cookie，仅用来维持登录状态
     * @return AuthWebSocketFrame
     */
    public AuthWebSocketFrame createAuth(ProtoverEnum protover, String cookie) {
        try {
            Map<String, String> cookieMap = OrLiveChatCookieUtil.parseCookieString(cookie);
            String buvid3 = OrLiveChatCookieUtil.getCookieByName(cookieMap, "buvid3", () -> UUID.randomUUID().toString());
            String uid = OrLiveChatCookieUtil.getCookieByName(cookieMap, "DedeUserID", () -> "0");
            BilibiliApis.RoomInitResult data = BilibiliApis.roomInit(roomId, cookie);
            JsonNode danmuInfo = BilibiliApis.getDanmuInfo(roomId, 0, cookie);
            long realRoomId = data.getRoom_id();
            AuthMsg authMsg = new AuthMsg(realRoomId, protover.getCode(), buvid3, danmuInfo.get("token").asText());
            authMsg.setUid(NumberUtil.parseLong(uid));
            return new AuthWebSocketFrame(BilibiliCodecUtil.encode(authMsg));
        } catch (Exception e) {
            throw new BaseException(String.format("认证包创建失败，请检查房间号是否正确。roomId: %d, msg: %s", roomId, e.getMessage()));
        }
    }

    public AuthWebSocketFrame createAuth(ProtoverEnum protover) {
        return this.createAuth(protover, null);
    }

    public HeartbeatWebSocketFrame createHeartbeat(ProtoverEnum protover) {
        return new HeartbeatWebSocketFrame(BilibiliCodecUtil.encode(this.getHeartbeatMsg(protover)));
    }

    /**
     * 心跳包单例模式
     *
     * @param protover {@link ProtoverEnum}
     * @return HeartbeatWebSocketFrame
     */
    public HeartbeatMsg getHeartbeatMsg(ProtoverEnum protover) {
        if (heartbeatMsg == null) {
            synchronized (BilibiliWebSocketFrameFactory.this) {
                if (heartbeatMsg == null) {
                    heartbeatMsg = new HeartbeatMsg(protover.getCode());
                }
            }
        }
        return heartbeatMsg;
    }

}
