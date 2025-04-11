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
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SenderInfo extends TarsStructBase {

    private long lUid;
    private long lImid;
    private String sNickName = "";
    private int iGender;
    private String sAvatarUrl;
    private int iNobleLevel;
    private NobleLevelInfo tNobleLevelInfo = new NobleLevelInfo();
    private String sGuid;
    private String sHuYaUA;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lUid, 0);
        os.write(this.lImid, 1);
        os.write(this.sNickName, 2);
        os.write(this.iGender, 3);
        os.write(this.sAvatarUrl, 4);
        os.write(this.iNobleLevel, 5);
        os.write(this.tNobleLevelInfo, 6);
        os.write(this.sGuid, 7);
        os.write(this.sHuYaUA, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, true);
        this.lImid = is.read(this.lImid, 1, true);
        this.sNickName = is.read(this.sNickName, 2, true);
        this.iGender = is.read(this.iGender, 3, true);
        this.sAvatarUrl = is.read(this.sAvatarUrl, 4, true);
        this.iNobleLevel = is.read(this.iNobleLevel, 5, true);
        this.tNobleLevelInfo = (NobleLevelInfo) is.directRead(this.tNobleLevelInfo, 6, true);
        this.sGuid = is.read(this.sGuid, 7, true);
        this.sHuYaUA = is.read(this.sHuYaUA, 8, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
