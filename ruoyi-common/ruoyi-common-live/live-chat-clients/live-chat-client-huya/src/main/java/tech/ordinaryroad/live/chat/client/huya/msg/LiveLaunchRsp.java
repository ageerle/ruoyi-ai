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

package tech.ordinaryroad.live.chat.client.huya.msg;

import cn.hutool.core.collection.CollUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.LiveProxyValue;

import java.util.List;

/**
 * @author mjz
 * @date 2023/10/5
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LiveLaunchRsp extends BaseHuyaMsg {

    private String sGuid = "";
    private int iTime;
    private List<LiveProxyValue> vProxyList = CollUtil.newArrayList(new LiveProxyValue());
    private int eAccess;
    private String sClientIp = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.sGuid, 0);
        os.write(this.iTime, 1);
        os.write(this.vProxyList, 2);
        os.write(this.eAccess, 3);
        os.write(this.sClientIp, 4);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.sGuid = is.read(this.sGuid, 0, false);
        this.iTime = is.read(this.iTime, 1, false);
        this.vProxyList = is.readArray(this.vProxyList, 2, false);
        this.eAccess = is.read(this.eAccess, 3, false);
        this.sClientIp = is.read(this.sClientIp, 4, false);
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.EWSCmd_WupRsp;
    }
}
