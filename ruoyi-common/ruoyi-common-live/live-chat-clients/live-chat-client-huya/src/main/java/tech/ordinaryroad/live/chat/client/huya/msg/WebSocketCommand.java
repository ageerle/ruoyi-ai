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
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaMsg;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class WebSocketCommand extends BaseHuyaMsg {

    private int operation;
    private byte[] vData;
    private long lRequestId;
    private String traceId = "";
    private int iEncryptType;
    private long lTime;
    private String sMD5 = "";

    public WebSocketCommand(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.operation, 0);
        os.write(this.vData, 1);
        os.write(this.lRequestId, 2);
        os.write(this.traceId, 3);
        os.write(this.iEncryptType, 4);
        os.write(this.lTime, 5);
        os.write(this.sMD5, 6);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.operation = is.read(this.operation, 0, true);
        this.vData = is.read(this.vData, 1, true);
        this.lRequestId = is.read(this.lRequestId, 2, true);
        this.traceId = is.read(this.traceId, 3, true);
        this.iEncryptType = is.read(this.iEncryptType, 4, true);
        this.lTime = is.read(this.lTime, 5, true);
        this.sMD5 = is.read(this.sMD5, 6, true);
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.getByCode(operation);
    }
}
