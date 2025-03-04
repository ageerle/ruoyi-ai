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

import cn.hutool.core.collection.CollUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BulletFormat extends TarsStructBase {

    private int iFontColor = -1;
    private int iFontSize = 4;
    private int iTextSpeed = 0;
    private int iTransitionType = 1;
    private int iPopupStyle = 0;
    private BulletBorderGroundFormat tBorderGroundFormat = new BulletBorderGroundFormat();
    private List<Integer> vGraduatedColor = CollUtil.newArrayList(0);
    private int iAvatarFlag = 0;
    private int iAvatarTerminalFlag = -1;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iFontColor, 0);
        os.write(this.iFontSize, 1);
        os.write(this.iTextSpeed, 2);
        os.write(this.iTransitionType, 3);
        os.write(this.iPopupStyle, 4);
        os.write(this.tBorderGroundFormat, 5);
        os.write(this.vGraduatedColor, 6);
        os.write(this.iAvatarFlag, 7);
        os.write(this.iAvatarTerminalFlag, 8);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iFontColor = is.read(this.iFontColor, 0, false);
        this.iFontSize = is.read(this.iFontSize, 1, false);
        this.iTextSpeed = is.read(this.iTextSpeed, 2, false);
        this.iTransitionType = is.read(this.iTransitionType, 3, false);
        this.iPopupStyle = is.read(this.iPopupStyle, 4, false);
        this.tBorderGroundFormat = (BulletBorderGroundFormat) is.directRead(this.tBorderGroundFormat, 5, false);
        this.vGraduatedColor = is.readArray(this.vGraduatedColor, 6, false);
        this.iAvatarFlag = is.read(this.iAvatarFlag, 7, false);
        this.iAvatarTerminalFlag = is.read(this.iAvatarTerminalFlag, 8, false);

    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
