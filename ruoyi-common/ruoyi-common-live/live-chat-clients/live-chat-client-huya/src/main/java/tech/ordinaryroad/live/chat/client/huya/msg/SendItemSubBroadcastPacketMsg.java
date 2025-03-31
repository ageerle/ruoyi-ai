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

package tech.ordinaryroad.live.chat.client.huya.msg;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import com.qq.tars.protocol.tars.TarsStructBase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IGiftMsg;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.*;
import tech.ordinaryroad.live.chat.client.huya.util.HuyaCodecUtil;

import java.util.List;
import java.util.Optional;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SendItemSubBroadcastPacketMsg extends BaseHuyaMsg implements IGiftMsg {

    private int iItemType;
    private String strPayId = "";
    private int iItemCount;
    private long lPresenterUid;
    private long lSenderUid;
    private String sPresenterNick = "";
    private String sSenderNick = "";
    private String sSendContent = "";
    private int iItemCountByGroup;
    private int iItemGroup;
    private int iSuperPupleLevel;
    private int iComboScore;
    private int iDisplayInfo;
    private int iEffectType;
    private String iSenderIcon = "";
    private String iPresenterIcon = "";
    private int iTemplateType;
    private String sExpand = "";
    private boolean bBusi;
    private int iColorEffectType;
    private String sPropsName = "";
    private short iAccpet = 0;
    private short iEventType = 0;
    private UserIdentityInfo userInfo = new UserIdentityInfo();
    private long lRoomId = 0;
    private long lHomeOwnerUid = 0;
    //    private int streamerInfo = new D.StreamerNode;
    private int iPayType = -1;
    private int iNobleLevel = 0;
    private NobleLevelInfo tNobleLevel = new NobleLevelInfo();
    private ItemEffectInfo tEffectInfo = new ItemEffectInfo();
    private List<Long> vExUid = CollUtil.newArrayList(-1L);
    private int iComboStatus = 0;
    private int iPidColorType = 0;
    private int iMultiSend = 0;
    private int iVFanLevel = 0;
    private int iUpgradeLevel = 0;
    private String sCustomText = "";
    private DIYBigGiftEffect tDIYEffect = new DIYBigGiftEffect();
    private long lComboSeqId = 0;
    private long lPayTotal = 0;
//    private int vBizData = new V.Vector(new D.ItemEffectBizData);

    // region 额外属性
    private BadgeInfo badgeInfo;
    private PropsItem propsItem = PropsItem.DEFAULT;
    // endregion

    public SendItemSubBroadcastPacketMsg(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.iItemType, 0);
        os.write(this.strPayId, 1);
        os.write(this.iItemCount, 2);
        os.write(this.lPresenterUid, 3);
        os.write(this.lSenderUid, 4);
        os.write(this.sPresenterNick, 5);
        os.write(this.sSenderNick, 6);
        os.write(this.sSendContent, 7);
        os.write(this.iItemCountByGroup, 8);
        os.write(this.iItemGroup, 9);
        os.write(this.iSuperPupleLevel, 10);
        os.write(this.iComboScore, 11);
        os.write(this.iDisplayInfo, 12);
        os.write(this.iEffectType, 13);
        os.write(this.iSenderIcon, 14);
        os.write(this.iPresenterIcon, 15);
        os.write(this.iTemplateType, 16);
        os.write(this.sExpand, 17);
        os.write(this.bBusi, 18);
        os.write(this.iColorEffectType, 19);
        os.write(this.sPropsName, 20);
        os.write(this.iAccpet, 21);
        os.write(this.iEventType, 22);
        os.write(this.userInfo, 23);
        os.write(this.lRoomId, 24);
        os.write(this.lHomeOwnerUid, 25);
//        os.write(this.streamerInfo, 26);
        os.write(this.iPayType, 27);
        os.write(this.iNobleLevel, 28);
        os.write(this.tNobleLevel, 29);
        os.write(this.tEffectInfo, 30);
        os.write(this.vExUid, 31);
        os.write(this.iComboStatus, 32);
        os.write(this.iPidColorType, 33);
        os.write(this.iMultiSend, 34);
        os.write(this.iVFanLevel, 35);
        os.write(this.iUpgradeLevel, 36);
        os.write(this.sCustomText, 37);
        os.write(this.tDIYEffect, 38);
        os.write(this.lComboSeqId, 39);
        os.write(this.lPayTotal, 41);
//        os.write(this.vBizData, 42);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.iItemType = is.read(this.iItemType, 0, true);
        this.strPayId = is.read(this.strPayId, 1, true);
        this.iItemCount = is.read(this.iItemCount, 2, true);
        this.lPresenterUid = is.read(this.lPresenterUid, 3, true);
        this.lSenderUid = is.read(this.lSenderUid, 4, true);
        this.sPresenterNick = is.read(this.sPresenterNick, 5, true);
        this.sSenderNick = is.read(this.sSenderNick, 6, true);
        this.sSendContent = is.read(this.sSendContent, 7, true);
        this.iItemCountByGroup = is.read(this.iItemCountByGroup, 8, true);
        this.iItemGroup = is.read(this.iItemGroup, 9, true);
        this.iSuperPupleLevel = is.read(this.iSuperPupleLevel, 10, true);
        this.iComboScore = is.read(this.iComboScore, 11, true);
        this.iDisplayInfo = is.read(this.iDisplayInfo, 12, true);
        this.iEffectType = is.read(this.iEffectType, 13, true);
        this.iSenderIcon = is.read(this.iSenderIcon, 14, true);
        this.iPresenterIcon = is.read(this.iPresenterIcon, 15, true);
        this.iTemplateType = is.read(this.iTemplateType, 16, true);
        this.sExpand = is.read(this.sExpand, 17, true);
        this.bBusi = is.read(this.bBusi, 18, true);
        this.iColorEffectType = is.read(this.iColorEffectType, 19, true);
        this.sPropsName = is.read(this.sPropsName, 20, true);
        this.iAccpet = is.read(this.iAccpet, 21, true);
        this.iEventType = is.read(this.iEventType, 22, true);
        this.userInfo = (UserIdentityInfo) is.directRead(this.userInfo, 23, true);
        this.lRoomId = is.read(this.lRoomId, 24, true);
        this.lHomeOwnerUid = is.read(this.lHomeOwnerUid, 25, true);
//        this.streamerInfo = is.read(this.streamerInfo, 26, true);
        this.iPayType = is.read(this.iPayType, 27, true);
        this.iNobleLevel = is.read(this.iNobleLevel, 28, true);
        this.tNobleLevel = (NobleLevelInfo) is.directRead(this.tNobleLevel, 29, true);
        this.tEffectInfo = (ItemEffectInfo) is.directRead(this.tEffectInfo, 30, true);
        this.vExUid = is.readArray(this.vExUid, 31, true);
        this.iComboStatus = is.read(this.iComboStatus, 32, true);
        this.iPidColorType = is.read(this.iPidColorType, 33, true);
        this.iMultiSend = is.read(this.iMultiSend, 34, true);
        this.iVFanLevel = is.read(this.iVFanLevel, 35, true);
        this.iUpgradeLevel = is.read(this.iUpgradeLevel, 36, true);
        this.sCustomText = is.read(this.sCustomText, 37, true);
        this.tDIYEffect = (DIYBigGiftEffect) is.directRead(this.tDIYEffect, 38, true);
        this.lComboSeqId = is.read(this.lComboSeqId, 39, true);
        this.lPayTotal = is.read(this.lPayTotal, 41, true);
//        this.vBizData = is.read(this.vBizData, 42, true);

        // 解析额外属性
        for (DecorationInfo decorationPrefix : userInfo.getVDecorationPrefix()) {
            Optional<? extends TarsStructBase> optional = HuyaCodecUtil.decodeDecorationInfo(decorationPrefix);
            if (optional.isPresent()) {
                TarsStructBase tarsStructBase = optional.get();
                if (tarsStructBase instanceof BadgeInfo) {
                    this.badgeInfo = (BadgeInfo) tarsStructBase;
                    break;
                }
            }
        }
    }

    @Override
    public String getUid() {
        return Long.toString(this.lSenderUid);
    }

    @Override
    public String getUsername() {
        return this.sSenderNick;
    }

    @Override
    public String getUserAvatar() {
        return this.iSenderIcon;
    }

    @Override
    public String getGiftName() {
        return this.sPropsName;
    }

    @Override
    public String getGiftImg() {
        if (this.propsItem == null) {
            return "";
        }

        List<PropsIdentity> vPropsIdentity = this.propsItem.getVPropsIdentity();
        if (vPropsIdentity.isEmpty()) {
            return "";
        }

        PropsIdentity propsIdentity = vPropsIdentity.get(0);
        String sPropsWeb = propsIdentity.getSPropsWeb();
        if (StrUtil.isBlank(sPropsWeb)) {
            return "";
        }

        return sPropsWeb.substring(0, sPropsWeb.indexOf("&"));
    }

    @Override
    public String getGiftId() {
        return Long.toString(this.iItemType);
    }

    @Override
    public int getGiftCount() {
        return this.iItemCount;
    }

    /**
     * 100 对应 1虎牙币
     */
    @Override
    public int getGiftPrice() {
        return (int) (this.lPayTotal / this.iItemCount);
    }

    @Override
    public String getReceiveUid() {
        return Long.toString(this.lPresenterUid);
    }

    @Override
    public String getReceiveUsername() {
        return this.sPresenterNick;
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.EWSCmdS2C_MsgPushReq;
    }
}
