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
public class SpecialInfo extends TarsStructBase {

    private int iFirstSingle = 0;
    private int iFirstGroup = 0;
    private String sFirstTips = "";
    private int iSecondSingle = 0;
    private int iSecondGroup = 0;
    private String sSecondTips = "";
    private int iThirdSingle = 0;
    private int iThirdGroup = 0;
    private String sThirdTips = "";
    private int iWorldSingle = 0;
    private int iWorldGroup = 0;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iFirstSingle, 1);
        os.write(this.iFirstGroup, 2);
        os.write(this.sFirstTips, 3);
        os.write(this.iSecondSingle, 4);
        os.write(this.iSecondGroup, 5);
        os.write(this.sSecondTips, 6);
        os.write(this.iThirdSingle, 7);
        os.write(this.iThirdGroup, 8);
        os.write(this.sThirdTips, 9);
        os.write(this.iWorldSingle, 10);
        os.write(this.iWorldGroup, 11);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iFirstSingle = is.read(this.iFirstSingle, 1, true);
        this.iFirstGroup = is.read(this.iFirstGroup, 2, true);
        this.sFirstTips = is.read(this.sFirstTips, 3, true);
        this.iSecondSingle = is.read(this.iSecondSingle, 4, true);
        this.iSecondGroup = is.read(this.iSecondGroup, 5, true);
        this.sSecondTips = is.read(this.sSecondTips, 6, true);
        this.iThirdSingle = is.read(this.iThirdSingle, 7, true);
        this.iThirdGroup = is.read(this.iThirdGroup, 8, true);
        this.sThirdTips = is.read(this.sThirdTips, 9, true);
        this.iWorldSingle = is.read(this.iWorldSingle, 10, true);
        this.iWorldGroup = is.read(this.iWorldGroup, 11, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
