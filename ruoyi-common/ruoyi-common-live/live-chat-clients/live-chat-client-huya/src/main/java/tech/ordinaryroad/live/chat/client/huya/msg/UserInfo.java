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
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author mjz
 * @date 2023/10/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserInfo extends TarsStructBase {

    private long lUid = 0;
    private boolean bAnonymous = true;
    private String sGuid = "";
    private String sToken = "";
    private long lTid = 0;
    private long lSid = 0;
    private long lGroupId = 0;
    private long lGroupType = 0;
    private String sAppId = "";
    private String sUA = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lUid, 0);
        os.write(this.bAnonymous, 1);
        os.write(this.sGuid, 2);
        os.write(this.sToken, 3);
        os.write(this.lTid, 4);
        os.write(this.lSid, 5);
        os.write(this.lGroupId, 6);
        os.write(this.lGroupType, 7);
        os.write(this.sAppId, 8);
        os.write(this.sUA, 9);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, true);
        this.bAnonymous = is.read(this.bAnonymous, 1, true);
        this.sGuid = is.read(this.sGuid, 2, true);
        this.sToken = is.read(this.sToken, 3, true);
        this.lTid = is.read(this.lTid, 4, true);
        this.lSid = is.read(this.lSid, 5, true);
        this.lGroupId = is.read(this.lGroupId, 6, true);
        this.lGroupType = is.read(this.lGroupType, 7, true);
        this.sAppId = is.read(this.sAppId, 8, true);
        this.sUA = is.read(this.sUA, 9, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
