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
import tech.ordinaryroad.live.chat.client.huya.msg.dto.UserId;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GetPropsListReq extends TarsStructBase {

    private UserId tUserId = new UserId();
    private String sMd5 = "";
    private int iTemplateType;
    private String sVersion = "";
    private int iAppId;
    private long lPresenterUid;
    private long lSid;
    private long lSubSid;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.tUserId, 1);
        os.write(this.sMd5, 2);
        os.write(this.iTemplateType, 3);
        os.write(this.sVersion, 4);
        os.write(this.iAppId, 5);
        os.write(this.lPresenterUid, 6);
        os.write(this.lSid, 7);
        os.write(this.lSubSid, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        is.read(this.tUserId, 1, true);
        is.read(this.sMd5, 2, true);
        is.read(this.iTemplateType, 3, true);
        is.read(this.sVersion, 4, true);
        is.read(this.iAppId, 5, true);
        is.read(this.lPresenterUid, 6, true);
        is.read(this.lSid, 7, true);
        is.read(this.lSubSid, 8, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
