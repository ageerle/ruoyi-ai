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
 * @date 2023/10/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DisplayInfo extends TarsStructBase {

    private int iMarqueeScopeMin = 0;
    private int iMarqueeScopeMax = 0;
    private int iCurrentVideoNum = 0;
    private int iCurrentVideoMin = 0;
    private int iCurrentVideoMax = 0;
    private int iAllVideoNum = 0;
    private int iAllVideoMin = 0;
    private int iAllVideoMax = 0;
    private int iCurrentScreenNum = 0;
    private int iCurrentScreenMin = 0;
    private int iCurrentScreenMax = 0;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iMarqueeScopeMin, 1);
        os.write(this.iMarqueeScopeMax, 2);
        os.write(this.iCurrentVideoNum, 3);
        os.write(this.iCurrentVideoMin, 4);
        os.write(this.iCurrentVideoMax, 5);
        os.write(this.iAllVideoNum, 6);
        os.write(this.iAllVideoMin, 7);
        os.write(this.iAllVideoMax, 8);
        os.write(this.iCurrentScreenNum, 9);
        os.write(this.iCurrentScreenMin, 10);
        os.write(this.iCurrentScreenMax, 11);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iMarqueeScopeMin = is.read(this.iMarqueeScopeMin, 1, true);
        this.iMarqueeScopeMax = is.read(this.iMarqueeScopeMax, 2, true);
        this.iCurrentVideoNum = is.read(this.iCurrentVideoNum, 3, true);
        this.iCurrentVideoMin = is.read(this.iCurrentVideoMin, 4, true);
        this.iCurrentVideoMax = is.read(this.iCurrentVideoMax, 5, true);
        this.iAllVideoNum = is.read(this.iAllVideoNum, 6, true);
        this.iAllVideoMin = is.read(this.iAllVideoMin, 7, true);
        this.iAllVideoMax = is.read(this.iAllVideoMax, 8, true);
        this.iCurrentScreenNum = is.read(this.iCurrentScreenNum, 9, true);
        this.iCurrentScreenMin = is.read(this.iCurrentScreenMin, 10, true);
        this.iCurrentScreenMax = is.read(this.iCurrentScreenMax, 11, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
