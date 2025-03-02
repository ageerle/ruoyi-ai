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

import java.util.ArrayList;
import java.util.List;

/**
 * @author mjz
 * @date 2023/10/3
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PropsItem extends TarsStructBase {

    public static final PropsItem DEFAULT = new PropsItem() {{
        setSPropsName("未知礼物");
        setIPropsYb(-1);
    }};

    private int iPropsId = 0;
    private String sPropsName = "";
    private int iPropsYb = 0;
    private int iPropsGreenBean = 0;
    private int iPropsWhiteBean = 0;
    private int iPropsGoldenBean = 0;
    private int iPropsRed = 0;
    private int iPropsPopular = 0;
    private int iPropsExpendNum = -1;
    private int iPropsFansValue = -1;
    private List<Integer> vPropsNum = new ArrayList<Integer>() {{
        add(-1);
    }};
    private int iPropsMaxNum = 0;
    private int iPropsBatterFlag = 0;
    private List<Integer> vPropsChannel = new ArrayList<Integer>() {{
        add(-1);
    }};
    private String sPropsToolTip = "";
    private List<PropsIdentity> vPropsIdentity = new ArrayList<PropsIdentity>() {{
        add(new PropsIdentity());
    }};
    private int iPropsWeights = 0;
    private int iPropsLevel = 0;
    private DisplayInfo tDisplayInfo = new DisplayInfo();
    private SpecialInfo tSpecialInfo = new SpecialInfo();
    private int iPropsGrade = 0;
    private int iPropsGroupNum = 0;
    private String sPropsCommBannerResource = "";
    private String sPropsOwnBannerResource = "";
    private int iPropsShowFlag = 0;
    private int iTemplateType = 0;
    private int iShelfStatus = 0;
    private String sAndroidLogo = "";
    private String sIpadLogo = "";
    private String sIphoneLogo = "";
    private String sPropsCommBannerResourceEx = "";
    private String sPropsOwnBannerResourceEx = "";
    private List<Long> vPresenterUid = new ArrayList<Long>() {{
        add(-1L);
    }};
    private List<PropView> vPropView = new ArrayList<PropView>() {{
        add(new PropView());
    }};
    private short iFaceUSwitch = 0;

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iPropsId, 1);
        os.write(this.sPropsName, 2);
        os.write(this.iPropsYb, 3);
        os.write(this.iPropsGreenBean, 4);
        os.write(this.iPropsWhiteBean, 5);
        os.write(this.iPropsGoldenBean, 6);
        os.write(this.iPropsRed, 7);
        os.write(this.iPropsPopular, 8);
        os.write(this.iPropsExpendNum, 9);
        os.write(this.iPropsFansValue, 10);
        os.write(this.vPropsNum, 11);
        os.write(this.iPropsMaxNum, 12);
        os.write(this.iPropsBatterFlag, 13);
        os.write(this.vPropsChannel, 14);
        os.write(this.sPropsToolTip, 15);
        os.write(this.vPropsIdentity, 16);
        os.write(this.iPropsWeights, 17);
        os.write(this.iPropsLevel, 18);
        os.write(this.tDisplayInfo, 19);
        os.write(this.tSpecialInfo, 20);
        os.write(this.iPropsGrade, 21);
        os.write(this.iPropsGroupNum, 22);
        os.write(this.sPropsCommBannerResource, 23);
        os.write(this.sPropsOwnBannerResource, 24);
        os.write(this.iPropsShowFlag, 25);
        os.write(this.iTemplateType, 26);
        os.write(this.iShelfStatus, 27);
        os.write(this.sAndroidLogo, 28);
        os.write(this.sIpadLogo, 29);
        os.write(this.sIphoneLogo, 30);
        os.write(this.sPropsCommBannerResourceEx, 31);
        os.write(this.sPropsOwnBannerResourceEx, 32);
        os.write(this.vPresenterUid, 33);
        os.write(this.vPropView, 34);
        os.write(this.iFaceUSwitch, 35);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iPropsId = is.read(this.iPropsId, 1, true);
        this.sPropsName = is.read(this.sPropsName, 2, true);
        this.iPropsYb = is.read(this.iPropsYb, 3, true);
        this.iPropsGreenBean = is.read(this.iPropsGreenBean, 4, true);
        this.iPropsWhiteBean = is.read(this.iPropsWhiteBean, 5, true);
        this.iPropsGoldenBean = is.read(this.iPropsGoldenBean, 6, true);
        this.iPropsRed = is.read(this.iPropsRed, 7, true);
        this.iPropsPopular = is.read(this.iPropsPopular, 8, true);
        this.iPropsExpendNum = is.read(this.iPropsExpendNum, 9, true);
        this.iPropsFansValue = is.read(this.iPropsFansValue, 10, true);
        this.vPropsNum = is.readArray(this.vPropsNum, 11, true);
        this.iPropsMaxNum = is.read(this.iPropsMaxNum, 12, true);
        this.iPropsBatterFlag = is.read(this.iPropsBatterFlag, 13, true);
        this.vPropsChannel = is.readArray(this.vPropsChannel, 14, true);
        this.sPropsToolTip = is.read(this.sPropsToolTip, 15, true);
        this.vPropsIdentity = is.readArray(this.vPropsIdentity, 16, true);
        this.iPropsWeights = is.read(this.iPropsWeights, 17, true);
        this.iPropsLevel = is.read(this.iPropsLevel, 18, true);
        this.tDisplayInfo = (DisplayInfo) is.directRead(this.tDisplayInfo, 19, true);
        this.tSpecialInfo = (SpecialInfo) is.directRead(this.tSpecialInfo, 20, true);
        this.iPropsGrade = is.read(this.iPropsGrade, 21, true);
        this.iPropsGroupNum = is.read(this.iPropsGroupNum, 22, true);
        this.sPropsCommBannerResource = is.read(this.sPropsCommBannerResource, 23, true);
        this.sPropsOwnBannerResource = is.read(this.sPropsOwnBannerResource, 24, true);
        this.iPropsShowFlag = is.read(this.iPropsShowFlag, 25, true);
        this.iTemplateType = is.read(this.iTemplateType, 26, true);
        this.iShelfStatus = is.read(this.iShelfStatus, 27, true);
        this.sAndroidLogo = is.read(this.sAndroidLogo, 28, true);
        this.sIpadLogo = is.read(this.sIpadLogo, 29, true);
        this.sIphoneLogo = is.read(this.sIphoneLogo, 30, true);
        this.sPropsCommBannerResourceEx = is.read(this.sPropsCommBannerResourceEx, 31, true);
        this.sPropsOwnBannerResourceEx = is.read(this.sPropsOwnBannerResourceEx, 32, true);
        this.vPresenterUid = is.readArray(this.vPresenterUid, 33, true);
        this.vPropView = is.readArray(this.vPropView, 34, true);
        this.iFaceUSwitch = is.read(this.iFaceUSwitch, 35, true);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
