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
public class NobleInfo extends TarsStructBase {

    private long lUid;
    private long lPid;
    private long lValidDate;
    private String sNobleName = "";
    private int iNobleLevel;
    private int iNoblePet;
    private int iNobleStatus;
    private int iNobleType;
    private int iRemainDays;
    private NobleLevelAttr tLevelAttr = new NobleLevelAttr();
    private NoblePetAttr tPetAttr = new NoblePetAttr();
    private long lOpenTime;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lUid, 0);
        os.write(this.lPid, 1);
        os.write(this.lValidDate, 2);
        os.write(this.sNobleName, 3);
        os.write(this.iNobleLevel, 4);
        os.write(this.iNoblePet, 5);
        os.write(this.iNobleStatus, 6);
        os.write(this.iNobleType, 7);
        os.write(this.iRemainDays, 8);
        os.write(this.tLevelAttr, 9);
        os.write(this.tPetAttr, 10);
        os.write(this.lOpenTime, 11);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, false);
        this.lPid = is.read(this.lPid, 1, false);
        this.lValidDate = is.read(this.lValidDate, 2, false);
        this.sNobleName = is.read(this.sNobleName, 3, false);
        this.iNobleLevel = is.read(this.iNobleLevel, 4, false);
        this.iNoblePet = is.read(this.iNoblePet, 5, false);
        this.iNobleStatus = is.read(this.iNobleStatus, 6, false);
        this.iNobleType = is.read(this.iNobleType, 7, false);
        this.iRemainDays = is.read(this.iRemainDays, 8, false);
        this.tLevelAttr = (NobleLevelAttr) is.directRead(this.tLevelAttr, 9, false);
        this.tPetAttr = (NoblePetAttr) is.directRead(this.tPetAttr, 10, false);
        this.lOpenTime = is.read(this.lOpenTime, 11, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
