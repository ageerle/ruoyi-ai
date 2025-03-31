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

package tech.ordinaryroad.live.chat.client.servers.netty.client.base;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.stream.ChunkedWriteHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseConnectionListener;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseMsgListener;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;
import tech.ordinaryroad.live.chat.client.commons.client.BaseLiveChatClient;
import tech.ordinaryroad.live.chat.client.commons.client.enums.ClientStatusEnums;
import tech.ordinaryroad.live.chat.client.servers.netty.client.config.BaseNettyClientConfig;
import tech.ordinaryroad.live.chat.client.servers.netty.handler.base.BaseBinaryFrameHandler;
import tech.ordinaryroad.live.chat.client.servers.netty.handler.base.BaseConnectionHandler;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author mjz
 * @date 2023/8/26
 */
@Slf4j
public abstract class BaseNettyClient
        <Config extends BaseNettyClientConfig,
                CmdEnum extends Enum<CmdEnum>,
                Msg extends IMsg,
                MsgListener extends IBaseMsgListener<BinaryFrameHandler, CmdEnum>,
                ConnectionHandler extends BaseConnectionHandler<ConnectionHandler>,
                BinaryFrameHandler extends BaseBinaryFrameHandler<BinaryFrameHandler, CmdEnum, Msg, MsgListener>
                >
        extends BaseLiveChatClient<Config, MsgListener> {

    @Getter
    private final EventLoopGroup workerGroup;
    @Getter
    private final Bootstrap bootstrap = new Bootstrap();
    private BinaryFrameHandler binaryFrameHandler;
    private ConnectionHandler connectionHandler;
    private IBaseConnectionListener<ConnectionHandler> connectionListener;
    private Channel channel;
    @Getter
    private URI websocketUri;
    protected IBaseConnectionListener<ConnectionHandler> clientConnectionListener;
    /**
     * 控制弹幕发送频率
     */
    private volatile long lastSendDanmuTimeInMillis;

    public abstract ConnectionHandler initConnectionHandler(IBaseConnectionListener<ConnectionHandler> clientConnectionListener);

    public abstract BinaryFrameHandler initBinaryFrameHandler();

    protected BaseNettyClient(Config config, EventLoopGroup workerGroup, IBaseConnectionListener<ConnectionHandler> connectionListener) {
        super(config);
        this.workerGroup = workerGroup;
        this.connectionListener = connectionListener;
    }

    public void onConnected(ConnectionHandler connectionHandler) {
        this.setStatus(ClientStatusEnums.CONNECTED);
        if (this.connectionListener != null) {
            this.connectionListener.onConnected(connectionHandler);
        }
    }

    public void onConnectFailed(ConnectionHandler connectionHandler) {
        this.setStatus(ClientStatusEnums.CONNECT_FAILED);
        tryReconnect();
        if (this.connectionListener != null) {
            this.connectionListener.onConnectFailed(connectionHandler);
        }
    }

    public void onDisconnected(ConnectionHandler connectionHandler) {
        this.setStatus(ClientStatusEnums.DISCONNECTED);
        tryReconnect();
        if (this.connectionListener != null) {
            this.connectionListener.onDisconnected(connectionHandler);
        }
    }

    @Override
    public void init() {
        if (checkStatus(ClientStatusEnums.INITIALIZED)) {
            return;
        }
        try {
            this.websocketUri = new URI(getWebSocketUriString());
            SslContext sslCtx = SslContextBuilder.forClient().build();

            this.clientConnectionListener = new IBaseConnectionListener<ConnectionHandler>() {
                @Override
                public void onConnected(ConnectionHandler connectionHandler) {
                    BaseNettyClient.this.onConnected(connectionHandler);
                }

                @Override
                public void onConnectFailed(ConnectionHandler connectionHandler) {
                    BaseNettyClient.this.onConnectFailed(connectionHandler);
                }

                @Override
                public void onDisconnected(ConnectionHandler connectionHandler) {
                    BaseNettyClient.this.onDisconnected(connectionHandler);
                }
            };
            this.binaryFrameHandler = this.initBinaryFrameHandler();
            this.connectionHandler = this.initConnectionHandler(this.clientConnectionListener);

            this.bootstrap.group(this.workerGroup)
                    // 创建Channel
                    .channel(NioSocketChannel.class)
                    .remoteAddress(this.websocketUri.getHost(), getInetPort())
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    // Channel配置
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // 责任链
                            ChannelPipeline pipeline = ch.pipeline();

                            // 放到第一位 addFirst 支持wss链接服务端
                            pipeline.addFirst(sslCtx.newHandler(ch.alloc(), BaseNettyClient.this.websocketUri.getHost(), getInetPort()));

                            // 添加一个http的编解码器
                            pipeline.addLast(new HttpClientCodec());
                            // 添加一个用于支持大数据流的支持
                            pipeline.addLast(new ChunkedWriteHandler());
                            // 添加一个聚合器，这个聚合器主要是将HttpMessage聚合成FullHttpRequest/Response
                            pipeline.addLast(new HttpObjectAggregator(BaseNettyClient.this.getConfig().getAggregatorMaxContentLength()));

                            // 连接处理器
                            pipeline.addLast(BaseNettyClient.this.connectionHandler);
                            // 弹幕处理器
                            pipeline.addLast(BaseNettyClient.this.binaryFrameHandler);
                        }
                    });
            this.setStatus(ClientStatusEnums.INITIALIZED);
        } catch (URISyntaxException e) {
            throw new BaseException(e);
        } catch (SSLException e) {
            throw new BaseException(e);
        }
    }

    private int getInetPort() {
        int port = this.websocketUri.getPort();
        return port == -1 ? "wss".equalsIgnoreCase(websocketUri.getScheme()) ? 443 : 80 : port;
    }

    @Override
    public void connect(Runnable success, Consumer<Throwable> failed) {
        if (this.cancelReconnect) {
            this.cancelReconnect = false;
        }
        if (!checkStatus(ClientStatusEnums.INITIALIZED)) {
            return;
        }
        if (getStatus() == ClientStatusEnums.CONNECTED) {
            return;
        }
        if (getStatus() != ClientStatusEnums.RECONNECTING) {
            this.setStatus(ClientStatusEnums.CONNECTING);
        }
        this.bootstrap.connect().addListener((ChannelFutureListener) connectFuture -> {
            if (connectFuture.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug("连接建立成功！");
                }
                this.channel = connectFuture.channel();
                // 监听是否握手成功
                this.connectionHandler.getHandshakeFuture().addListener((ChannelFutureListener) handshakeFuture -> {
                    try {
                        connectionHandler.sendAuthRequest(channel);
                        if (success != null) {
                            success.run();
                        }
                    } catch (Exception e) {
                        log.error("认证包发送失败，断开连接", e);
                        this.disconnect();
                    }
                });
            } else {
                log.error("连接建立失败", connectFuture.cause());
                this.onConnectFailed(this.connectionHandler);
                if (failed != null) {
                    failed.accept(connectFuture.cause());
                }
            }
        });
    }

    @Override
    public void disconnect() {
        if (this.channel == null) {
            return;
        }
        this.channel.close();
    }

    @Override
    protected void tryReconnect() {
        if (this.cancelReconnect) {
            this.cancelReconnect = false;
            return;
        }
        if (!getConfig().isAutoReconnect()) {
            return;
        }
        if (log.isWarnEnabled()) {
            log.warn("{}s后将重新连接 {}", getConfig().getReconnectDelay(), getConfig().getRoomId());
        }
        workerGroup.schedule(() -> {
            this.setStatus(ClientStatusEnums.RECONNECTING);
            this.connect();
        }, getConfig().getReconnectDelay(), TimeUnit.SECONDS);
    }

    @Override
    public void send(Object msg, Runnable success, Consumer<Throwable> failed) {
        ChannelFuture future = this.channel.writeAndFlush(msg);
        if (success != null || failed != null) {
            future.addListener((ChannelFutureListener) channelFuture -> {
                if (channelFuture.isSuccess()) {
                    if (success != null) {
                        success.run();
                    }
                } else {
                    if (failed != null) {
                        failed.accept(channelFuture.cause());
                    }
                }
            });
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        // 销毁时不需要重连
        this.cancelReconnect = true;
        workerGroup.shutdownGracefully().addListener(future -> {
            if (future.isSuccess()) {
                this.setStatus(ClientStatusEnums.DESTROYED);
            } else {
                throw new BaseException("client销毁失败", future.cause());
            }
        });
    }

    @Override
    protected String getWebSocketUriString() {
        return getConfig().getWebsocketUri();
    }

    @Override
    protected void setStatus(ClientStatusEnums status) {
        if (log.isDebugEnabled()) {
            if (getStatus() != status) {
                log.debug("{} 状态变化 {} => {}\n", getClass().getSimpleName(), getStatus(), status);
            }
        }
        super.setStatus(status);
    }

    @Override
    public void sendDanmu(Object danmu, Runnable success, Consumer<Throwable> failed) {
        throw new BaseException("暂未支持该功能");
    }

    @Override
    public void clickLike(int count, Runnable success, Consumer<Throwable> failed) {
        throw new BaseException("暂未支持该功能");
    }

    /**
     * 发送弹幕前判断是否可以发送
     *
     * @param checkConnected 是否检查Client连接状态
     */
    protected boolean checkCanSendDanmu(boolean checkConnected) {
        if (checkConnected && getStatus() != ClientStatusEnums.CONNECTED) {
            throw new BaseException("连接未建立，无法发送弹幕");
        }
        if (System.currentTimeMillis() - this.lastSendDanmuTimeInMillis <= getConfig().getMinSendDanmuPeriod()) {
            if (log.isWarnEnabled()) {
                log.warn("发送弹幕频率过快，忽略该次发送");
            }
            return false;
        }
        return true;
    }

    protected boolean checkCanSendDanmu() {
        return checkCanSendDanmu(true);
    }

    /**
     * 发送弹幕后调用该方法
     */
    protected void finishSendDanmu() {
        this.lastSendDanmuTimeInMillis = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("弹幕发送完成");
        }
    }

    public void iteratorMsgListeners(Consumer<MsgListener> consumer) {
        binaryFrameHandler.iteratorMsgListeners(consumer);
    }
}
