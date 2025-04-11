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
 * @date 2023/10/10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PresenterChannelInfo extends TarsStructBase {

    private long lYYId;
    private long lTid;
    private long lSid;
    private int iSourceType;
    private int iScreenType;
    private long lUid;
    private int iGameId;
    private int iRoomId;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lYYId, 0);
        os.write(this.lTid, 1);
        os.write(this.lSid, 3);
        os.write(this.iSourceType, 4);
        os.write(this.iScreenType, 5);
        os.write(this.lUid, 6);
        os.write(this.iGameId, 7);
        os.write(this.iRoomId, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lYYId = is.read(this.lYYId, 0, false);
        this.lTid = is.read(this.lTid, 1, false);
        this.lSid = is.read(this.lSid, 3, false);
        this.iSourceType = is.read(this.iSourceType, 4, false);
        this.iScreenType = is.read(this.iScreenType, 5, false);
        this.lUid = is.read(this.lUid, 6, false);
        this.iGameId = is.read(this.iGameId, 7, false);
        this.iRoomId = is.read(this.iRoomId, 8, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
