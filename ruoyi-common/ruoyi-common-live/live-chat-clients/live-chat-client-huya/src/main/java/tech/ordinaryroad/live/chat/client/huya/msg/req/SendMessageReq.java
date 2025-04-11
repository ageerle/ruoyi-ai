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

import cn.hutool.core.collection.CollUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.*;

import java.util.List;

/**
 * @author mjz
 * @date 2023/10/5
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendMessageReq extends TarsStructBase {

    private UserId tUserId = new UserId();
    private long lTid;
    private long lSid;
    private String sContent = "";
    private int iShowMode;
    private ContentFormat tFormat = new ContentFormat();
    private BulletFormat tBulletFormat = new BulletFormat();
    private List<UidNickName> vAtSomeone;
    private long lPid;
    private List<MessageTagInfo> vTagInfo = CollUtil.newArrayList(new MessageTagInfo());
    private SendMessageFormat tSenceFormat = new SendMessageFormat();
    private int iMessageMode;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.tUserId, 0);
        os.write(this.lTid, 1);
        os.write(this.lSid, 2);
        os.write(this.sContent, 3);
        os.write(this.iShowMode, 4);
        os.write(this.tFormat, 5);
        os.write(this.tBulletFormat, 6);
        os.write(this.vAtSomeone, 7);
        os.write(this.lPid, 8);
        os.write(this.vTagInfo, 9);
        os.write(this.tSenceFormat, 10);
        os.write(this.iMessageMode, 11);

    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.tUserId = (UserId) is.directRead(this.tUserId, 0, false);
        this.lTid = is.read(this.lTid, 1, false);
        this.lSid = is.read(this.lSid, 2, false);
        this.sContent = is.read(this.sContent, 3, false);
        this.iShowMode = is.read(this.iShowMode, 4, false);
        this.tFormat = (ContentFormat) is.directRead(this.tFormat, 5, false);
        this.tBulletFormat = (BulletFormat) is.directRead(this.tBulletFormat, 6, false);
        this.vAtSomeone = is.readArray(this.vAtSomeone, 7, false);
        this.lPid = is.read(this.lPid, 8, false);
        this.vTagInfo = is.readArray(this.vTagInfo, 9, false);
        this.tSenceFormat = (SendMessageFormat) is.directRead(this.tSenceFormat, 10, false);
        this.iMessageMode = is.read(this.iMessageMode, 11, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
