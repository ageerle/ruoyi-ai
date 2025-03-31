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

import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaCmdMsg;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PushMessage extends BaseHuyaCmdMsg {

    private int ePushType;
    private byte[] dataBytes;
    private int iProtocolType;
    private String sGroupId = "";
    private long lMsgId;
    private int iMsgTag;

    public PushMessage(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.ePushType, 0);
        os.write(super.getLUri(), 1);
        os.write(this.dataBytes, 2);
        os.write(this.iProtocolType, 3);
        os.write(this.sGroupId, 4);
        os.write(this.lMsgId, 5);
        os.write(this.iMsgTag, 6);

    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.ePushType = is.read(this.ePushType, 0, true);
        super.setLUri(is.read(super.getLUri(), 1, true));
        this.dataBytes = is.read(this.dataBytes, 2, true);
        this.iProtocolType = is.read(this.iProtocolType, 3, true);
        this.sGroupId = is.read(this.sGroupId, 4, true);
        this.lMsgId = is.read(this.lMsgId, 5, true);
        this.iMsgTag = is.read(this.iMsgTag, 6, true);
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.EWSCmdS2C_MsgPushReq_V2;
    }
}
