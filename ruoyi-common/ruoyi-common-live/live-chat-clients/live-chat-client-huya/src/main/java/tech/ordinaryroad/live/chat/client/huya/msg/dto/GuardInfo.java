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
 * @date 2023/12/27
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class GuardInfo extends TarsStructBase {

    private long lUid;
    private long lPid;
    private int iGuardLevel;
    private long lEndTime;
    private String sAttr = "";
    private String sIcon = "";
    private int iGuardType;
    private long lStartTime;
    private long lCommemorateDay;
    private int iAccompanyDay;
    private String sNewAttr = "";
    private String sEnterText = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lUid, 0);
        os.write(this.lPid, 1);
        os.write(this.iGuardLevel, 2);
        os.write(this.lEndTime, 3);
        os.write(this.sAttr, 4);
        os.write(this.sIcon, 5);
        os.write(this.iGuardType, 6);
        os.write(this.lStartTime, 7);
        os.write(this.lCommemorateDay, 8);
        os.write(this.iAccompanyDay, 9);
        os.write(this.sNewAttr, 10);
        os.write(this.sEnterText, 11);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, false);
        this.lPid = is.read(this.lPid, 1, false);
        this.iGuardLevel = is.read(this.iGuardLevel, 2, false);
        this.lEndTime = is.read(this.lEndTime, 3, false);
        this.sAttr = is.read(this.sAttr, 4, false);
        this.sIcon = is.read(this.sIcon, 5, false);
        this.iGuardType = is.read(this.iGuardType, 6, false);
        this.lStartTime = is.read(this.lStartTime, 7, false);
        this.lCommemorateDay = is.read(this.lCommemorateDay, 8, false);
        this.iAccompanyDay = is.read(this.iAccompanyDay, 9, false);
        this.sNewAttr = is.read(this.sNewAttr, 10, false);
        this.sEnterText = is.read(this.sEnterText, 11, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
