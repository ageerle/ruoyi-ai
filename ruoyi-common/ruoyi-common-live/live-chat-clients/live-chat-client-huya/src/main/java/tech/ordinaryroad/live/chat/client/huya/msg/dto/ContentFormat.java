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
public class ContentFormat extends TarsStructBase {

    private int iFontColor = -1;
    private int iFontSize = 4;
    private int iPopupStyle = 0;
    private int iNickNameFontColor = -1;
    private int iDarkFontColor = -1;
    private int iDarkNickNameFontColor = -1;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iFontColor, 0);
        os.write(this.iFontSize, 1);
        os.write(this.iPopupStyle, 2);
        os.write(this.iNickNameFontColor, 3);
        os.write(this.iDarkFontColor, 4);
        os.write(this.iDarkNickNameFontColor, 5);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iFontColor = is.read(this.iFontColor, 0, false);
        this.iFontSize = is.read(this.iFontSize, 1, false);
        this.iPopupStyle = is.read(this.iPopupStyle, 2, false);
        this.iNickNameFontColor = is.read(this.iNickNameFontColor, 3, false);
        this.iDarkFontColor = is.read(this.iDarkFontColor, 4, false);
        this.iDarkNickNameFontColor = is.read(this.iDarkNickNameFontColor, 5, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
