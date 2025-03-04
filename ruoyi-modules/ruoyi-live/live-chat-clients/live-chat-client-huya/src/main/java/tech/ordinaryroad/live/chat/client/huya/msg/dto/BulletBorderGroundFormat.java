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
 * @date 2023/10/5
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulletBorderGroundFormat extends TarsStructBase {

    private int iEnableUse;
    private int iBorderThickness;
    private int iBorderColour = -1;
    private int iBorderDiaphaneity = 100;
    private int iGroundColour = -1;
    private int iGroundColourDiaphaneity = 100;
    private String sAvatarDecorationUrl = "";
    private int iFontColor = -1;
    private int iTerminalFlag = -1;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iEnableUse, 0);
        os.write(this.iBorderThickness, 1);
        os.write(this.iBorderColour, 2);
        os.write(this.iBorderDiaphaneity, 3);
        os.write(this.iGroundColour, 4);
        os.write(this.iGroundColourDiaphaneity, 5);
        os.write(this.sAvatarDecorationUrl, 6);
        os.write(this.iFontColor, 7);
        os.write(this.iTerminalFlag, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iEnableUse = is.read(this.iEnableUse, 0, false);
        this.iBorderThickness = is.read(this.iBorderThickness, 1, false);
        this.iBorderColour = is.read(this.iBorderColour, 2, false);
        this.iBorderDiaphaneity = is.read(this.iBorderDiaphaneity, 3, false);
        this.iGroundColour = is.read(this.iGroundColour, 4, false);
        this.iGroundColourDiaphaneity = is.read(this.iGroundColourDiaphaneity, 5, false);
        this.sAvatarDecorationUrl = is.read(this.sAvatarDecorationUrl, 6, false);
        this.iFontColor = is.read(this.iFontColor, 7, false);
        this.iTerminalFlag = is.read(this.iTerminalFlag, 8, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
