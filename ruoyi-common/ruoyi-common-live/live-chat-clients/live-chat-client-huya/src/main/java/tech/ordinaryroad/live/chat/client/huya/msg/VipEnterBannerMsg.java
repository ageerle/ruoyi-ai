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
import com.qq.tars.protocol.tars.TarsInputStream;
import com.qq.tars.protocol.tars.TarsOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IEnterRoomMsg;
import tech.ordinaryroad.live.chat.client.huya.constant.HuyaOperationEnum;
import tech.ordinaryroad.live.chat.client.huya.msg.base.BaseHuyaMsg;
import tech.ordinaryroad.live.chat.client.huya.msg.dto.*;

import java.util.List;

/**
 * @author mjz
 * @date 2023/12/27
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class VipEnterBannerMsg extends BaseHuyaMsg implements IEnterRoomMsg {

    private long lUid;
    private String sNickName = "";
    private long lPid;
    private NobleInfo tNobleInfo = new NobleInfo();
    private GuardInfo tGuardInfo = new GuardInfo();
    private WeekRankInfo tWeekRankInfo = new WeekRankInfo();
    private String sLogoURL = "";
    private boolean bFromNearby;
    private String sLocation = "";
    private DecorationInfoRsp tDecorationInfo = new DecorationInfoRsp();
    private WeekRankInfo tWeekHeartStirRankInfo = new WeekRankInfo();
    private WeekRankInfo tWeekHeartBlockRankInfo = new WeekRankInfo();
    private int iMasterRank;
    private ACEnterBanner tACInfo = new ACEnterBanner();
    private List<CommEnterBanner> vCommEnterBanner = CollUtil.newArrayList(new CommEnterBanner());
    private UserRidePetInfo tRidePetInfo = new UserRidePetInfo();

    public VipEnterBannerMsg(TarsInputStream is) {
        this.readFrom(is);
    }

    @Override
    public void writeTo(TarsOutputStream os) {
        os.write(this.lUid, 0);
        os.write(this.sNickName, 1);
        os.write(this.lPid, 2);
        os.write(this.tNobleInfo, 3);
        os.write(this.tGuardInfo, 4);
        os.write(this.tWeekRankInfo, 5);
        os.write(this.sLogoURL, 6);
        os.write(this.bFromNearby, 7);
        os.write(this.sLocation, 8);
        os.write(this.tDecorationInfo, 9);
        os.write(this.tWeekHeartStirRankInfo, 10);
        os.write(this.tWeekHeartBlockRankInfo, 11);
        os.write(this.iMasterRank, 12);
        os.write(this.tACInfo, 13);
        os.write(this.vCommEnterBanner, 14);
        os.write(this.tRidePetInfo, 15);
    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, false);
        this.sNickName = is.read(this.sNickName, 1, false);
        this.lPid = is.read(this.lPid, 2, false);
        this.tNobleInfo = (NobleInfo) is.directRead(this.tNobleInfo, 3, false);
        this.tGuardInfo = (GuardInfo) is.directRead(this.tGuardInfo, 4, false);
        this.tWeekRankInfo = (WeekRankInfo) is.directRead(this.tWeekRankInfo, 5, false);
        this.sLogoURL = is.read(this.sLogoURL, 6, false);
        this.bFromNearby = is.read(this.bFromNearby, 7, false);
        this.sLocation = is.read(this.sLocation, 8, false);
        this.tDecorationInfo = (DecorationInfoRsp) is.directRead(this.tDecorationInfo, 9, false);
        this.tWeekHeartStirRankInfo = (WeekRankInfo) is.directRead(this.tWeekHeartStirRankInfo, 10, false);
        this.tWeekHeartBlockRankInfo = (WeekRankInfo) is.directRead(this.tWeekHeartBlockRankInfo, 11, false);
        this.iMasterRank = is.read(this.iMasterRank, 12, false);
        this.tACInfo = (ACEnterBanner) is.directRead(this.tACInfo, 13, false);
        this.vCommEnterBanner = is.readArray(this.vCommEnterBanner, 14, false);
        this.tRidePetInfo = (UserRidePetInfo) is.directRead(this.tRidePetInfo, 15, false);
    }

    @Override
    public String getBadgeName() {
        // TODO
        return null;
    }

    @Override
    public byte getBadgeLevel() {
        // TODO
        return 0;
    }

    @Override
    public String getUid() {
        return Long.toString(lUid);
    }

    @Override
    public String getUsername() {
        return sNickName;
    }

    @Override
    public String getUserAvatar() {
        // TODO
        return IEnterRoomMsg.super.getUserAvatar();
    }

    @Override
    public HuyaOperationEnum getOperationEnum() {
        return HuyaOperationEnum.EWSCmdS2C_MsgPushReq;
    }
}
