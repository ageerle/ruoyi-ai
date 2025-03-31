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

package tech.ordinaryroad.live.chat.client.huya.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author mjz
 * @date 2023/10/2
 */
@Getter
@RequiredArgsConstructor
public enum HuyaCmdEnum {

    NobleNotice(1001),
    // NobleEnterNotice(1002),
    NobleEnterNotice(1005),
    NobleLevelNotice(1006),
    MessageNotice(1400),
    // ExpressionEmoticonNotice(1420),
    ExpressionEmoticonNotice(1422),
    OpenEmojiTrialNotice(1441),
    SubscribeInfoNotify(3104),
    PublicMessageAreaGuideWindow(6074),
    //    WeekStarPropsIds(6100),
    WeekStarPropsIdsTab(6102),
    /**
     * <a href="https://dev.huya.com/docs/miniapp/dev/scenario/vip-event/">https://dev.huya.com/docs/miniapp/dev/scenario/vip-event/</a>
     */
    VipEnterBanner(6110),
    VipBarListStatInfo(6211),
    EnterPushInfo(6200),
    GameAdvertisement(6201),
    ViewerListRsp(6203),
    VipBarListRsp(6210),
    UserWeekRankScoreInfo(6219),
    WeekRankListRsp(6220),
    WeekRankEnterBanner(6221),
    FansSupportListRsp(6223),
    FansRankListRsp(6230),
    BadgeInfo(6231),
    BadgeScoreChanged(6232),
    FansInfoNotice(6233),
    UserGiftNotice(6234),
    WeekStarPropsIds(6235),
    SuperFansExtendInfo(6245),
    TrialFansBadgeScoreChanged(6246),
    GuardianCountChangedNotice(6249),
    GiftBarRsp(6250),
    GrandCeremonyChampionPresenter(6260),
    LotteryAnnounce(6289),
    NewsTicker(6290),
    SendItemSubBroadcastPacket(6501),
    SendItemNoticeWordBroadcastPacket(6502),
    ShowScreenSkinNotify(6640),
    HideScreenSkinNotify(6641),
    ActivetyBarrageNotice(6650),
    BannerNotice(6291),
    //    OnTVPanel(6294),
    OnTVPanel(6295),
    OnTVData(6296),
    OnTVEndNotice(6297),
    OnTVBarrageNotice(6298),
    CheckRoomStatus(6340),
    SendItemNoticeGameBroadcastPacket(6507),
    SendItemActivityNoticeBroadcastPacket(6508),
    SendItemOtherBroadcastPacket(6514),
    GiftStatusNotify(6515),
    ActivitySpecialNoticeBroadcastPacket(6540),
    UserDIYMountsChanged(6575),
    ObtainDecoNotify(6590),
    TreasureResultBroadcastPacket(6602),
    TreasureUpdateNotice(6604),
    TreasureLotteryResultNoticePacket(6605),
    TreasureBoxPanel(6606),
    TreasureBoxBigAwardNotice(6607),
    ItemLotterySubNotice(6616),
    ItemLotteryGameNotice(6617),
    FansBadgeLevelUpNotice(6710),
    FansPromoteNotice(6711),
    ActCommPanelChangeNotify(6647),
    MatchRaffleResultNotice(7055),
    BatchGameInfoNotice(7500),
    GameInfoChangeNotice(7501),
    EndHistoryGameNotice(7502),
    GameSettlementNotice(7503),
    PresenterEndGameNotice(7504),
    //    PresenterLevelNotice(7708),
    PresenterLevelNotice(7709),
    EffectsConfChangeNoticeMsg(7772),
    BeginLiveNotice(8000),
    EndLiveNotice(8001),
    StreamSettingNotice(8002),
    LiveInfoChangedNotice(8004),
    AttendeeCountNotice(8006),
    ReplayPresenterInLiveNotify(9010),
    RoomAuditWarningNotice(10039),
    AuditorEnterLiveNotice(10040),
    AuditorRoleChangeNotice(10041),
    GetRoomAuditConfRsp(10042),
    UserConsumePrivilegeChangeNotice(10047),
    LinkMicStatusChangeNotice(42008),
    InterveneCountRsp(44000),
    UserLevelUpgradeNotice(1000106),
    PushUserLevelTaskCompleteNotice(1130055),
    GuardianPresenterInfoNotice(1020001),
    SupportCampInfoRsp(1025300),
    UserSupportCampRsp(1025301),
    UserSupportEffectRsp(1025302),
    WSRedirect(1025305),
    HuYaUdbNotify(10220051),
    infoBody(10220053),
    UnionAuthPushMsg(10220054),
    RMessageNotify(1025000),
    PushPresenterAdNotice(1025493),
    RoomAdInfo(1025504),
    //    PushAdInfo(1025562),
//    PushAdInfo(1025564),
    PushAdInfo(1025566),
    //             AdExtServer.PushOfflineInfo(1025569),
    WSP2POpenNotify(1025307),
    WSP2PCloseNotify(1025308),
    LiveMeetingSyncNotice(1025601),
    MakeFriendsPKInfo(1025604),
    LiveRoomTransferNotice(1025605),
    GetPugcVipListRsp(1025800),
    PugcVipInfo(1025801),
    StreamChangeNotice(100000),
    PayLiveRoomNotice(1033001),
    MatchEndNotice(1034001),
    LiveRoomProfileChangedNotice(1035400),
    ACOrderInfo(1060003),
    //             WEBACT.Message(108e4),
    MultiPKNotice(1090007),
    MultiPKPanelInfo(1090009),
    AiBarrageDetectNotify(1100003),
    FloatMomentNotice(1130050),
    FloatBallNotice(1130052),
    VoiceMuteJsonInfo(1200000),
    PixelateInfo(1200001),
    //    MpsDeliverData(1210000),
    MpsDeliverData(1220000),
    ActivityMsgRsp(1010003),
    //    Message(1040000),
    Message(1040002),
    LiveEventMessage(1040003),
    LiveViewLimitChangeNotice(1035100),
    PrivilegeRenewalNotice(1035101),
    MatchRecLiveInfo(1029001),
    GetBattleTeamInfoRsp(1029002),
    //             MatchGuess.MatchCmdColorNotify(1025312),
    GameStatusInfo(1130003),
    MatchPlaybackPointNotice(1150001),
    PushFaceDirectorCurrentProgram(1130070),
    JoinSplitScreenNotice(1500001),
    LeaveSplitScreenNotice(1500002),
    GameLivePromoteNotify(1800009),
    MotorcadeGatherBeginNotice(2000001),
    MotorcadeGatherEndNotice(2000002),
    MotorcadeGatherResponseNotice(2000003),
    MotorcadeActivityPanel(2000041),
    MessageRichTextNotice(2001231),
    MultiVideoSyncNotice(2400001),
    PassParcelChangeNotify(2400002),
    MatchLiveCommentorChangeNotify(2400020),
    MessageEasterEggNotice(2001203),
    MessageEasterEggToastNotice(2001202),
    UserFollowStrollIconNotice(2410001),
    UserFollowStrollBarrageNotice(2410002),
//             LiveMatch.MatchLiveRoomRecMsg(2500406),
    ;

    private final long code;

    public static HuyaCmdEnum getByCode(long code) {
        for (HuyaCmdEnum value : HuyaCmdEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
