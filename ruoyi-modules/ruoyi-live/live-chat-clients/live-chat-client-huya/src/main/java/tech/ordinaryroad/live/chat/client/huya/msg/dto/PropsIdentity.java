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
public class PropsIdentity extends TarsStructBase {

    private int iPropsIdType = 0;
    private String sPropsPic18 = "";
    private String sPropsPic24 = "";
    private String sPropsPicGif = "";
    private String sPropsBannerResource = "";
    private String sPropsBannerSize = "";
    private String sPropsBannerMaxTime = "";
    private String sPropsChatBannerResource = "";
    private String sPropsChatBannerSize = "";
    private String sPropsChatBannerMaxTime = "";
    private int iPropsChatBannerPos = 0;
    private int iPropsChatBannerIsCombo = 0;
    private String sPropsRollContent = "";
    private int iPropsBannerAnimationstyle = 0;
    private String sPropFaceu = "";
    private String sPropH5Resource = "";
    private String sPropsWeb = "";
    private int sWitch = 0;
    private String sCornerMark = "";
    private int iPropViewId = 0;
    private String sPropStreamerResource = "";
    private short iStreamerFrameRate = 0;
    private String sPropsPic108 = "";
    private String sPcBannerResource = "";

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iPropsIdType, 1);
        os.write(this.sPropsPic18, 2);
        os.write(this.sPropsPic24, 3);
        os.write(this.sPropsPicGif, 4);
        os.write(this.sPropsBannerResource, 5);
        os.write(this.sPropsBannerSize, 6);
        os.write(this.sPropsBannerMaxTime, 7);
        os.write(this.sPropsChatBannerResource, 8);
        os.write(this.sPropsChatBannerSize, 9);
        os.write(this.sPropsChatBannerMaxTime, 10);
        os.write(this.iPropsChatBannerPos, 11);
        os.write(this.iPropsChatBannerIsCombo, 12);
        os.write(this.sPropsRollContent, 13);
        os.write(this.iPropsBannerAnimationstyle, 14);
        os.write(this.sPropFaceu, 15);
        os.write(this.sPropH5Resource, 16);
        os.write(this.sPropsWeb, 17);
        os.write(this.sWitch, 18);
        os.write(this.sCornerMark, 19);
        os.write(this.iPropViewId, 20);
        os.write(this.sPropStreamerResource, 21);
        os.write(this.iStreamerFrameRate, 22);
        os.write(this.sPropsPic108, 23);
        os.write(this.sPcBannerResource, 24);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iPropsIdType = is.read(this.iPropsIdType, 1, true);
        this.sPropsPic18 = is.read(this.sPropsPic18, 2, true);
        this.sPropsPic24 = is.read(this.sPropsPic24, 3, true);
        this.sPropsPicGif = is.read(this.sPropsPicGif, 4, true);
        this.sPropsBannerResource = is.read(this.sPropsBannerResource, 5, true);
        this.sPropsBannerSize = is.read(this.sPropsBannerSize, 6, true);
        this.sPropsBannerMaxTime = is.read(this.sPropsBannerMaxTime, 7, true);
        this.sPropsChatBannerResource = is.read(this.sPropsChatBannerResource, 8, true);
        this.sPropsChatBannerSize = is.read(this.sPropsChatBannerSize, 9, true);
        this.sPropsChatBannerMaxTime = is.read(this.sPropsChatBannerMaxTime, 10, true);
        this.iPropsChatBannerPos = is.read(this.iPropsChatBannerPos, 11, true);
        this.iPropsChatBannerIsCombo = is.read(this.iPropsChatBannerIsCombo, 12, true);
        this.sPropsRollContent = is.read(this.sPropsRollContent, 13, true);
        this.iPropsBannerAnimationstyle = is.read(this.iPropsBannerAnimationstyle, 14, true);
        this.sPropFaceu = is.read(this.sPropFaceu, 15, true);
        this.sPropH5Resource = is.read(this.sPropH5Resource, 16, true);
        this.sPropsWeb = is.read(this.sPropsWeb, 17, true);
        this.sWitch = is.read(this.sWitch, 18, true);
        this.sCornerMark = is.read(this.sCornerMark, 19, true);
        this.iPropViewId = is.read(this.iPropViewId, 20, true);
        this.sPropStreamerResource = is.read(this.sPropStreamerResource, 21, true);
        this.iStreamerFrameRate = is.read(this.iStreamerFrameRate, 22, true);
        this.sPropsPic108 = is.read(this.sPropsPic108, 23, true);
        this.sPcBannerResource = is.read(this.sPcBannerResource, 24, true);
    }
}
