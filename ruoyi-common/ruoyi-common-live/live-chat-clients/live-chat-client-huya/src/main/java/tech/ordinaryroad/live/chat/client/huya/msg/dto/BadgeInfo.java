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
 * @date 2023/10/10
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BadgeInfo extends TarsStructBase {

    private long lUid;
    private long lBadgeId;
    private String sPresenterNickName = "";
    private String sBadgeName = "";
    private int iBadgeLevel;
    private int iRank;
    private int iScore;
    private int iNextScore;
    private int iQuotaUsed;
    private int iQuota;
    private long lQuotaTS;
    private long lOpenTS;
    private int iVFlag;
    private String sVLogo = "";
    private PresenterChannelInfo tChannelInfo = new PresenterChannelInfo();
    private String sPresenterLogo = "";
    private long lVExpiredTS;
    private int iBadgeType;
    private FaithInfo tFaithInfo = new FaithInfo();
    private SuperFansInfo tSuperFansInfo = new SuperFansInfo();
    private int iBaseQuota;
    private long lVConsumRank;
    private int iCustomBadgeFlag;
    private int iAgingDays;
    private int iDayScore;
    private CustomBadgeDynamicExternal tExternal = new CustomBadgeDynamicExternal();
    private int iExtinguished;
    private int iExtinguishDays;
    private int iBadgeCate;
    private int iLiveFlag;

    @Override
    public void writeTo(TarsOutputStream os) {

    }

    @Override
    public void readFrom(TarsInputStream is) {
        this.lUid = is.read(this.lUid, 0, false);
        this.lBadgeId = is.read(this.lBadgeId, 1, false);
        this.sPresenterNickName = is.read(this.sPresenterNickName, 2, false);
        this.sBadgeName = is.read(this.sBadgeName, 3, false);
        this.iBadgeLevel = is.read(this.iBadgeLevel, 4, false);
        this.iRank = is.read(this.iRank, 5, false);
        this.iScore = is.read(this.iScore, 6, false);
        this.iNextScore = is.read(this.iNextScore, 7, false);
        this.iQuotaUsed = is.read(this.iQuotaUsed, 8, false);
        this.iQuota = is.read(this.iQuota, 9, false);
        this.lQuotaTS = is.read(this.lQuotaTS, 10, false);
        this.lOpenTS = is.read(this.lOpenTS, 11, false);
        this.iVFlag = is.read(this.iVFlag, 12, false);
        this.sVLogo = is.read(this.sVLogo, 13, false);
        this.tChannelInfo = (PresenterChannelInfo) is.directRead(this.tChannelInfo, 14, false);
        this.sPresenterLogo = is.read(this.sPresenterLogo, 15, false);
        this.lVExpiredTS = is.read(this.lVExpiredTS, 16, false);
        this.iBadgeType = is.read(this.iBadgeType, 17, false);
        this.tFaithInfo = (FaithInfo) is.directRead(this.tFaithInfo, 18, false);
        this.tSuperFansInfo = (SuperFansInfo) is.directRead(this.tSuperFansInfo, 19, false);
        this.iBaseQuota = is.read(this.iBaseQuota, 20, false);
        this.lVConsumRank = is.read(this.lVConsumRank, 21, false);
        this.iCustomBadgeFlag = is.read(this.iCustomBadgeFlag, 22, false);
        this.iAgingDays = is.read(this.iAgingDays, 23, false);
        this.iDayScore = is.read(this.iDayScore, 24, false);
        this.tExternal = (CustomBadgeDynamicExternal) is.directRead(this.tExternal, 25, false);
        this.iExtinguished = is.read(this.iExtinguished, 26, false);
        this.iExtinguishDays = is.read(this.iExtinguishDays, 27, false);
        this.iBadgeCate = is.read(this.iBadgeCate, 28, false);
        this.iLiveFlag = is.read(this.iLiveFlag, 29, false);
    }

    @Override
    public TarsStructBase newInit() {
        return this;
    }
}
