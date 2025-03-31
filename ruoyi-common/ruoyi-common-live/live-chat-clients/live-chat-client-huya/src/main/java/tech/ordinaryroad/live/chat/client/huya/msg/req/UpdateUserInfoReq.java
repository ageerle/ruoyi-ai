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

package tech.ordinaryroad.live.chat.client.huya.msg.req;

import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.MsgStatInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * @author mjz
 * @date 2023/10/5
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateUserInfoReq extends TarsStructBase {

    private String sAppSrc = "";
    private String sGuid = "";
    private int iReportMsgIdRatio;
    private int iSupportAck;
    private MsgStatInfo tWSMsgStatInfo = new MsgStatInfo();
    private Map<String, String> mCustomHeader = new HashMap<>();
    private int iMsgDegradeLevel;

    public UpdateUserInfoReq(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.sAppSrc, 0);
        os.write(this.sGuid, 1);
        os.write(this.iReportMsgIdRatio, 2);
        os.write(this.iSupportAck, 3);
        os.write(this.tWSMsgStatInfo, 6);
        os.write(this.mCustomHeader, 7);
        os.write(this.iMsgDegradeLevel, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.sAppSrc = is.read(this.sAppSrc, 0, true);
        this.sGuid = is.read(this.sGuid, 1, true);
        this.iReportMsgIdRatio = is.read(this.iReportMsgIdRatio, 2, true);
        this.iSupportAck = is.read(this.iSupportAck, 3, true);
        this.tWSMsgStatInfo = (MsgStatInfo) is.directRead(this.tWSMsgStatInfo, 6, true);
        this.mCustomHeader = is.readStringMap( 7, true);
        this.iMsgDegradeLevel = is.read(this.iMsgDegradeLevel, 8, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
