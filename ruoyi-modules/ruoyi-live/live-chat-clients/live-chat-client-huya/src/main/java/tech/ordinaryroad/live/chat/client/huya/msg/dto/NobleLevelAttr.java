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
public class NobleLevelAttr extends TarsStructBase {

    private int iAttrType;
    private long lValidDate;
    private int iAttrStatus;
    private int iProgress;
    private int iTask;
    private int iRemainDays;
    private String sReserve = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iAttrType, 0);
        os.write(this.lValidDate, 1);
        os.write(this.iAttrStatus, 2);
        os.write(this.iProgress, 3);
        os.write(this.iTask, 4);
        os.write(this.iRemainDays, 5);
        os.write(this.sReserve, 6);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iAttrType = is.read(this.iAttrType, 0, false);
        this.lValidDate = is.read(this.lValidDate, 1, false);
        this.iAttrStatus = is.read(this.iAttrStatus, 2, false);
        this.iProgress = is.read(this.iProgress, 3, false);
        this.iTask = is.read(this.iTask, 4, false);
        this.iRemainDays = is.read(this.iRemainDays, 5, false);
        this.sReserve = is.read(this.sReserve, 6, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
