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

package tech.ordinaryroad.live.chat.client.commons.client.config;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tech.ordinaryroad.live.chat.client.commons.base.exception.BaseException;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * 直播间弹幕客户端配置
 *
 * @author mjz
 * @date 2023/8/26
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public abstract class BaseLiveChatClientConfig {

    protected PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
    public static final long DEFAULT_HEARTBEAT_INITIAL_DELAY = 15;
    public static final long DEFAULT_HEARTBEAT_PERIOD = 25;
    public static final long DEFAULT_MIN_SEND_DANMU_PERIOD = 3000L;

    private String websocketUri;

    /**
     * 浏览器中的Cookie
     */
    private String cookie;

    /**
     * 直播间id
     */
    private Object roomId;

    /**
     * 是否启用自动重连
     */
    @Builder.Default
    private boolean autoReconnect = Boolean.TRUE;

    /**
     * 重试延迟时间（秒），默认5s后重试
     */
    @Builder.Default
    private int reconnectDelay = 5;

    /**
     * 首次发送心跳包的延迟时间（秒）
     */
    @Builder.Default
    private long heartbeatInitialDelay = DEFAULT_HEARTBEAT_INITIAL_DELAY;

    /**
     * 心跳包发送周期（秒）
     */
    @Builder.Default
    private long heartbeatPeriod = DEFAULT_HEARTBEAT_PERIOD;

    /**
     * 最小发送弹幕时间间隔（毫秒）
     */
    @Builder.Default
    private long minSendDanmuPeriod = DEFAULT_MIN_SEND_DANMU_PERIOD;

    public void setCookie(String cookie) {
        String oldValue = this.cookie;
        this.cookie = cookie;
        this.propertyChangeSupport.firePropertyChange("cookie", oldValue, cookie);
    }

    public void setRoomId(Object roomId) {
        if (!(roomId instanceof Number || roomId instanceof String)) {
            throw new BaseException("房间ID仅支持数字或字符串，所传参数类型：" + roomId.getClass() + "值：" + roomId);
        }
        Object oldValue = this.roomId;
        this.roomId = roomId;
        this.propertyChangeSupport.firePropertyChange("roomId", oldValue, roomId);
    }

    public void setWebsocketUri(String websocketUri) {
        String oldValue = this.websocketUri;
        this.websocketUri = websocketUri;
        this.propertyChangeSupport.firePropertyChange("websocketUri", oldValue, websocketUri);
    }

    public void setMinSendDanmuPeriod(long minSendDanmuPeriod) {
        long oldValue = this.minSendDanmuPeriod;
        this.minSendDanmuPeriod = minSendDanmuPeriod;
        this.propertyChangeSupport.firePropertyChange("minSendDanmuPeriod", oldValue, minSendDanmuPeriod);
    }

    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(propertyName, listener);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(propertyName, listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        this.propertyChangeSupport.removePropertyChangeListener(listener);
    }
}
