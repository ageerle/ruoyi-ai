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

package tech.ordinaryroad.live.chat.client.huya.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import tech.ordinaryroad.live.chat.client.servers.netty.client.config.BaseNettyClientConfig;

/**
 * 直播间弹幕客户端配置
 *
 * @author mjz
 * @date 2023/9/5
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder(toBuilder = true)
public class HuyaLiveChatClientConfig extends BaseNettyClientConfig {

    public static final String VER = "2309271152";

    @Builder.Default
//    private String websocketUri = "wss://wsapi.huya.com:443";
    private String websocketUri = "wss://cdnws.api.huya.com:443";

    @Builder.Default
    private int aggregatorMaxContentLength = 64 * 1024 * 1024;

    @Builder.Default
    private int maxFramePayloadLength = 64 * 1024 * 1024;

    @Builder.Default
    private String ver = VER;

    @Builder.Default
    private String exp = "15547.23738,16582.25335,32083.50834";

    @Builder.Default
    private String appSrc = "HUYA&ZH&2052";

    public void setVer(String ver) {
        String oldValue = this.ver;
        this.ver = ver;
        super.propertyChangeSupport.firePropertyChange("ver", oldValue, ver);
    }

    public void setExp(String exp) {
        String oldValue = this.exp;
        this.exp = exp;
        super.propertyChangeSupport.firePropertyChange("exp", oldValue, exp);
    }

    public void setAppSrc(String appSrc) {
        String oldValue = this.appSrc;
        this.appSrc = appSrc;
        super.propertyChangeSupport.firePropertyChange("appSrc", oldValue, appSrc);
    }
}
