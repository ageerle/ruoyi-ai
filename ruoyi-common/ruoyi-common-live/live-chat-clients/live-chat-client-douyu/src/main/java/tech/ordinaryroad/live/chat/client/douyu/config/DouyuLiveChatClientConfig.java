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

package tech.ordinaryroad.live.chat.client.douyu.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tech.ordinaryroad.live.chat.client.commons.util.OrLiveChatNumberUtil;
import tech.ordinaryroad.live.chat.client.servers.netty.client.config.BaseNettyClientConfig;

/**
 * Douyu直播间弹幕客户端配置
 *
 * @author mjz
 * @date 2023/8/21
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class DouyuLiveChatClientConfig extends BaseNettyClientConfig {

    @Builder.Default
    private String ver = "20220825";

    @Builder.Default
    private String aver = "218101901";

    @Override
    public Long getRoomId() {
        return OrLiveChatNumberUtil.parseLong(super.getRoomId());
    }

    public void setVer(String ver) {
        String oldValue = this.ver;
        this.ver = ver;
        super.propertyChangeSupport.firePropertyChange("ver", oldValue, ver);
    }

    public void setAver(String aver) {
        String oldValue = this.aver;
        this.aver = aver;
        super.propertyChangeSupport.firePropertyChange("aver", oldValue, aver);
    }
}
