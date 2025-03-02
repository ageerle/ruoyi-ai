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

import lombok.Getter;
import tech.ordinaryroad.live.chat.client.commons.base.listener.IBaseMsgListener;
import tech.ordinaryroad.live.chat.client.commons.client.config.BaseLiveChatClientConfig;
import tech.ordinaryroad.live.chat.client.commons.client.enums.ClientStatusEnums;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author mjz
 * @date 2023/8/26
 */
public abstract class BaseLiveChatClient<
        Config extends BaseLiveChatClientConfig,
        MsgListener extends IBaseMsgListener<?, ?>
        > implements IBaseLiveChatClient<MsgListener> {

    private final Config config;
    @Getter
    private volatile ClientStatusEnums status = ClientStatusEnums.NEW;
    protected PropertyChangeSupport statusChangeSupport = new PropertyChangeSupport(status);
    protected volatile boolean cancelReconnect = false;
    protected final List<MsgListener> msgListeners = Collections.synchronizedList(new ArrayList<>());

    protected BaseLiveChatClient(Config config) {
        this.config = config;
    }

    public Config getConfig() {
        return config;
    }

    @Override
    public void connect(Runnable success) {
        this.connect(success, null);
    }

    @Override
    public void connect() {
        this.connect(null, null);
    }

    @Override
    public void disconnect(boolean cancelReconnect) {
        this.cancelReconnect = cancelReconnect;
        this.disconnect();
    }

    @Override
    public void send(Object msg) {
        this.send(msg, null, null);
    }

    @Override
    public void send(Object msg, Runnable success) {
        this.send(msg, success, null);
    }

    @Override
    public void send(Object msg, Consumer<Throwable> failed) {
        this.send(msg, null, failed);
    }

    @Override
    public void sendDanmu(Object danmu) {
        this.sendDanmu(danmu, null, null);
    }

    @Override
    public void sendDanmu(Object danmu, Runnable success) {
        this.sendDanmu(danmu, success, null);
    }

    @Override
    public void sendDanmu(Object danmu, Consumer<Throwable> failed) {
        this.sendDanmu(danmu, null, failed);
    }

    @Override
    public void clickLike(int count) {
        this.clickLike(count, null, null);
    }

    @Override
    public void clickLike(int count, Runnable success) {
        this.clickLike(count, success, null);
    }

    @Override
    public void clickLike(int count, Consumer<Throwable> failed) {
        this.clickLike(count, null, failed);
    }

    protected abstract void tryReconnect();

    protected abstract String getWebSocketUriString();

    /**
     * 判断是否处于某个状态，或者处于后续状态
     *
     * @param status {@link ClientStatusEnums}
     * @return false: 还没有到达该状态
     */
    protected boolean checkStatus(ClientStatusEnums status) {
        return this.status.getCode() >= Objects.requireNonNull(status).getCode();
    }

    protected void setStatus(ClientStatusEnums status) {
        ClientStatusEnums oldStatus = this.status;
        if (oldStatus != status) {
            this.status = status;
            this.statusChangeSupport.firePropertyChange("status", oldStatus, status);
        }
    }

    public void addStatusChangeListener(PropertyChangeListener listener) {
        this.statusChangeSupport.addPropertyChangeListener(listener);
    }

    public void removeStatusChangeListener(PropertyChangeListener listener) {
        this.statusChangeSupport.removePropertyChangeListener(listener);
    }

    @Override
    public void destroy() {
        for (PropertyChangeListener propertyChangeListener : this.statusChangeSupport.getPropertyChangeListeners()) {
            this.statusChangeSupport.removePropertyChangeListener(propertyChangeListener);
        }
        this.msgListeners.clear();
    }

    @Override
    public boolean addMsgListener(MsgListener msgListener) {
        if (msgListener == null) {
            return false;
        }
        return this.msgListeners.add(msgListener);
    }

    @Override
    public boolean addMsgListeners(List<MsgListener> msgListeners) {
        if (msgListeners == null || msgListeners.isEmpty()) {
            return false;
        }
        return this.msgListeners.addAll(msgListeners);
    }

    @Override
    public boolean removeMsgListener(MsgListener msgListener) {
        if (msgListener == null) {
            return false;
        }
        return this.msgListeners.remove(msgListener);
    }

    @Override
    public boolean removeMsgListeners(List<MsgListener> msgListeners) {
        if (msgListeners == null || msgListeners.isEmpty()) {
            return false;
        }
        return this.msgListeners.removeAll(msgListeners);
    }

    @Override
    public void removeAllMsgListeners() {
        this.msgListeners.clear();
    }

}
