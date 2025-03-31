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

package tech.ordinaryroad.live.chat.client.huya.msg.dto;

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
 * @date 2023/10/5
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class MsgItem extends BaseHuyaCmdMsg {

    private byte[] sMsg;
    private long lMsgId;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(super.getLUri(), 0);
        os.write(this.sMsg, 1);
        os.write(this.lMsgId, 2);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        super.setLUri(is.read(super.getLUri(), 0, true));
        this.sMsg = is.read(this.sMsg, 1, true);
        this.lMsgId = is.read(this.lMsgId, 2, true);
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.EWSCmdS2C_MsgPushReq_V2;
    }
}
