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

package tech.ordinaryroad.live.chat.client.huya.netty.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.huya.client.HuyaLiveChatClient;
import tech.ordinaryroad.live.chat.client.huya.config.HuyaLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.huya.netty.frame.factory.HuyaWebSocketFrameFactory;
import tech.ordinaryroad.live.chat.client.servers.netty.client.handler.BaseNettyClientConnectionHandler;


/**
 * 连接处理器
 *
 * @author mjz
 * @date 2023/8/21
 */
@Slf4j
@ChannelHandler.Sharable
public class HuyaConnectionHandler extends BaseNettyClientConnectionHandler<HuyaLiveChatClient, HuyaConnectionHandler> {

    /**
     * 以ClientConfig为主
     */
    private final Object roomId;
    /**
     * 以ClientConfig为主
     */
    private final String ver;
    /**
     * 以ClientConfig为主
     */
    private String cookie;

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, HuyaLiveChatClient client, IBaseConnectionListener<HuyaConnectionHandler> listener) {
        super(handshaker, client, listener);
        this.roomId = client.getConfig().getRoomId();
        this.ver = client.getConfig().getVer();
        this.cookie = client.getConfig().getCookie();
    }

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, HuyaLiveChatClient client) {
        this(handshaker, client, null);
    }

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, String ver, IBaseConnectionListener<HuyaConnectionHandler> listener, String cookie) {
        super(handshaker, listener);
        this.roomId = roomId;
        this.ver = ver;
        this.cookie = cookie;
    }

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, String ver, IBaseConnectionListener<HuyaConnectionHandler> listener) {
        this(handshaker, roomId, ver, listener, null);
    }

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, String ver, String cookie) {
        this(handshaker, roomId, ver, null, cookie);
    }

    public HuyaConnectionHandler(WebSocketClientHandshaker handshaker, long roomId, String ver) {
        this(handshaker, roomId, ver, null, null);
    }

    @Override
    protected void sendHeartbeat(ChannelHandlerContext ctx) {
        if (log.isDebugEnabled()) {
            log.debug("发送心跳包");
        }
        ctx.writeAndFlush(
                getWebSocketFrameFactory(getRoomId()).createHeartbeat(getVer(), getCookie())
        ).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug("心跳包发送完成");
                }
            } else {
                log.error("心跳包发送失败", future.cause());
            }
        });
    }

    private static HuyaWebSocketFrameFactory getWebSocketFrameFactory(Object roomId) {
        return HuyaWebSocketFrameFactory.getInstance(roomId);
    }

    @Override
    public void sendAuthRequest(Channel channel) {
        if (log.isDebugEnabled()) {
            log.debug("发送认证包");
        }
        channel.writeAndFlush(getWebSocketFrameFactory(getRoomId()).createAuth(getVer(), getCookie()));
    }

    public Object getRoomId() {
        return client != null ? client.getConfig().getRoomId() : roomId;
    }

    public String getVer() {
        return client != null ? client.getConfig().getVer() : ver;
    }

    private String getCookie() {
        return client != null ? client.getConfig().getCookie() : cookie;
    }

    @Override
    protected long getHeartbeatPeriod() {
        if (client == null) {
            return HuyaLiveChatClientConfig.DEFAULT_HEARTBEAT_PERIOD;
        } else {
            return client.getConfig().getHeartbeatPeriod();
        }
    }

    @Override
    protected long getHeartbeatInitialDelay() {
        if (client == null) {
            return HuyaLiveChatClientConfig.DEFAULT_HEARTBEAT_INITIAL_DELAY;
        } else {
            return client.getConfig().getHeartbeatInitialDelay();
        }
    }
}
