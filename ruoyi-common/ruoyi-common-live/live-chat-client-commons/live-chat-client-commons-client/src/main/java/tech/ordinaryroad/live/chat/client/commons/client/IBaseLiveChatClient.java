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

package tech.ordinaryroad.live.chat.client.commons.client;

import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseMsgListener;

import java.util.List;
import java.util.function.Consumer;

/**
 * @author mjz
 * @date 2023/9/5
 */
public interface IBaseLiveChatClient<MsgListener extends IBaseMsgListener<?, ?>> {

    void init();

    boolean addMsgListener(MsgListener msgListener);

    boolean addMsgListeners(List<MsgListener> msgListeners);

    boolean removeMsgListener(MsgListener msgListener);

    boolean removeMsgListeners(List<MsgListener> msgListeners);

    void removeAllMsgListeners();

    void connect(Runnable success, Consumer<Throwable> failed);

    void connect(Runnable success);

    void connect();

    /**
     * 手动断开连接
     *
     * @param cancelReconnect 取消本次的自动重连（如果启用自动重连）
     */
    void disconnect(boolean cancelReconnect);

    void disconnect();

    void destroy();

    void send(Object msg);

    void send(Object msg, Runnable success, Consumer<Throwable> failed);

    void send(Object msg, Runnable success);

    void send(Object msg, Consumer<Throwable> failed);

    /**
     * 发送弹幕
     *
     * @param danmu 弹幕内容
     * @since 0.0.6
     */
    void sendDanmu(Object danmu);

    /**
     * 发送弹幕
     *
     * @param danmu 弹幕内容
     * @since 0.0.6
     */
    void sendDanmu(Object danmu, Runnable success, Consumer<Throwable> failed);

    /**
     * 发送弹幕
     *
     * @param danmu 弹幕内容
     * @since 0.0.6
     */
    void sendDanmu(Object danmu, Runnable success);

    /**
     * 发送弹幕
     *
     * @param danmu 弹幕内容
     * @since 0.0.6
     */
    void sendDanmu(Object danmu, Consumer<Throwable> failed);

    /**
     * 为直播间点赞
     *
     * @since 0.2.0
     */
    void clickLike(int count);

    /**
     * 为直播间点赞
     *
     * @since 0.2.0
     */
    void clickLike(int count, Runnable success, Consumer<Throwable> failed);

    /**
     * 为直播间点赞
     *
     * @since 0.2.0
     */
    void clickLike(int count, Runnable success);

    /**
     * 为直播间点赞
     *
     * @since 0.2.0
     */
    void clickLike(int count, Consumer<Throwable> failed);

}
