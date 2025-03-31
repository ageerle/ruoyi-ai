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
public class UserHeartBeatReq extends TarsStructBase {

    private UserId tId = new UserId();
    private long lTid;
    private long lSid;
    private long lPid;
    private boolean bWatchVideo;
    private int eLineType;
    private int iFps;
    private int iAttendee;
    private int iBandwidth;
    private int iLastHeartElapseTime;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.tId, 0);
        os.write(this.lTid, 1);
        os.write(this.lSid, 2);
        os.write(this.lPid, 4);
        os.write(this.bWatchVideo, 5);
        os.write(this.eLineType, 6);
        os.write(this.iFps, 7);
        os.write(this.iAttendee, 8);
        os.write(this.iBandwidth, 9);
        os.write(this.iLastHeartElapseTime, 10);

    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.tId = (UserId) is.directRead(this.tId, 0, false);
        this.lTid = is.read(this.lTid, 1, false);
        this.lSid = is.read(this.lSid, 2, false);
        this.lPid = is.read(this.lPid, 4, false);
        this.bWatchVideo = is.read(this.bWatchVideo, 5, false);
        this.eLineType = is.read(this.eLineType, 6, false);
        this.iFps = is.read(this.iFps, 7, false);
        this.iAttendee = is.read(this.iAttendee, 8, false);
        this.iBandwidth = is.read(this.iBandwidth, 9, false);
        this.iLastHeartElapseTime = is.read(this.iLastHeartElapseTime, 10, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
