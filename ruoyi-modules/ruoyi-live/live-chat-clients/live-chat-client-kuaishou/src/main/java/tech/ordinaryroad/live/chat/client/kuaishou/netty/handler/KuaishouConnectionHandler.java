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

package tech.ordinaryroad.live.chat.client.kuaishou.netty.handler;

import cn.hutool.core.util.RandomUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.kuaishou.api.KuaishouApis;
import tech.ordinaryroad.live.chat.client.kuaishou.client.KuaishouLiveChatClient;
import tech.ordinaryroad.live.chat.client.kuaishou.config.KuaishouLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.CSHeartbeatOuterClass;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.CSWebEnterRoomOuterClass;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.PayloadTypeOuterClass;
import tech.ordinaryroad.live.chat.client.kuaishou.protobuf.SocketMessageOuterClass;
import tech.ordinaryroad.live.chat.client.servers.netty.client.handler.BaseNettyClientConnectionHandler;

/**
 * @author mjz
 * @date 2024/1/5
 */
@Slf4j
@ChannelHandler.Sharable
public class KuaishouConnectionHandler extends BaseNettyClientConnectionHandler<KuaishouLiveChatClient, KuaishouConnectionHandler> {

    /**
     * 以ClientConfig为主
     */
    private final Object roomId;
    /**
     * 以ClientConfig为主
     */
    private String cookie;
    private final KuaishouApis.RoomInitResult roomInitResult;

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, KuaishouLiveChatClient client, IBaseConnectionListener<KuaishouConnectionHandler> listener) {
        super(handshaker, client, listener);
        this.roomId = client.getConfig().getRoomId();
        this.cookie = client.getConfig().getCookie();
        this.roomInitResult = client.getRoomInitResult();
    }

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, KuaishouLiveChatClient client) {
        this(handshaker, client, null);
    }

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, KuaishouApis.RoomInitResult roomInitResult, IBaseConnectionListener<KuaishouConnectionHandler> listener, String cookie) {
        super(handshaker, listener);
        this.roomId = roomId;
        this.cookie = cookie;
        this.roomInitResult = roomInitResult;
    }

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, KuaishouApis.RoomInitResult roomInitResult, IBaseConnectionListener<KuaishouConnectionHandler> listener) {
        this(handshaker, roomId, roomInitResult, listener, null);
    }

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, KuaishouApis.RoomInitResult roomInitResult, String cookie) {
        this(handshaker, roomId, roomInitResult, null, cookie);
    }

    public KuaishouConnectionHandler(WebSocketClientHandshaker handshaker, KuaishouApis.RoomInitResult roomInitResult, long roomId) {
        this(handshaker, roomId, roomInitResult, null, null);
    }

    @Override
    protected void sendHeartbeat(ChannelHandlerContext ctx) {
        ctx.writeAndFlush(
                new BinaryWebSocketFrame(
                        Unpooled.wrappedBuffer(SocketMessageOuterClass.SocketMessage.newBuilder()
                                .setPayloadType(PayloadTypeOuterClass.PayloadType.CS_HEARTBEAT)
                                .setPayload(
                                        CSHeartbeatOuterClass.CSHeartbeat.newBuilder()
                                                .setTimestamp(System.currentTimeMillis())
                                                .build()
                                                .toByteString()
                                )
                                .build()
                                .toByteArray()
                        )
                )
        );
    }

    @Override
    public void sendAuthRequest(Channel channel) {
        channel.writeAndFlush(
                new BinaryWebSocketFrame(
                        Unpooled.wrappedBuffer(SocketMessageOuterClass.SocketMessage.newBuilder()
                                .setPayloadType(PayloadTypeOuterClass.PayloadType.CS_ENTER_ROOM)
                                .setPayload(
                                        CSWebEnterRoomOuterClass.CSWebEnterRoom.newBuilder()
                                                .setToken(roomInitResult.getToken())
                                                .setLiveStreamId(roomInitResult.getLiveStreamId())
                                                .setPageId(RandomUtil.randomString(16) + System.currentTimeMillis())
                                                .build()
                                                .toByteString()
                                )
                                .build()
                                .toByteArray()
                        )
                )
        );
    }

    @Override
    protected long getHeartbeatPeriod() {
        if (client == null) {
            return KuaishouLiveChatClientConfig.DEFAULT_HEARTBEAT_PERIOD;
        } else {
            return client.getConfig().getHeartbeatPeriod();
        }
    }

    @Override
    protected long getHeartbeatInitialDelay() {
        if (client == null) {
            return KuaishouLiveChatClientConfig.DEFAULT_HEARTBEAT_INITIAL_DELAY;
        } else {
            return client.getConfig().getHeartbeatInitialDelay();
        }
    }

    public Object getRoomId() {
        return client != null ? client.getConfig().getRoomId() : roomId;
    }

    private String getCookie() {
        return client != null ? client.getConfig().getCookie() : cookie;
    }
}
