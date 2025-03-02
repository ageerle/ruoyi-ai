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
 * @date 2023/10/10
 */
@Getter
@RequiredArgsConstructor
public enum HuyaDecorationAppTypeEnum {

    kDecorationAppTypeCommon(100),
    kDecorationAppTypeContentBubble(5000),
    kDecorationAppTypeContentBubbleNew(5001),
    kDecorationAppTypeEffectsMessenger(5002),
    kDecorationAppTypeMsgInterConnect(5010),
    kDecorationAppTypeMsgLocation(5011),
    kDecorationAppTypeChannel(10000),
    kDecorationAppTypeGuildAdmin(10090),
    kDecorationAppTypeAdmin(10100),
    kDecorationAppTypeDaiyanClub(10150),
    kDecorationAppTypeNoble(10200),
    KDecorationAppTypeGuildVip(10210),
    kDecorationAppTypeGuard(10300),
    kDecorationAppTypeDiamondUser_V2(10310),
    kDecorationAppTypeTeamMedalV2(10350),
    kDecorationAppTypeTrialFans(10399),
    kDecorationAppTypeFans(10400),
    kDecorationAppTypeWatchTogetherVip(10425),
    kDecorationAppTypeTeamMedal(10450),
    kDecorationAppTypeVIP(10500),
    kDecorationAppTypeUserProfile(10560),
    kDecorationAppTyperPurpleDiamond(10600),
    kDecorationAppTypeStamp(10700),
    KDecorationAppTypeNobleEmoticon(10800),
    KDecorationAppTypeAnotherAi(10801),
    KDecorationAppTypePresenter(10900),
    KDecorationAppTypeFirstRecharge(11000),
    kDecorationAppTypeCheckRoom(11100),
    kDecorationAppTypeTWatch(11101),
    kDecorationAppTypeEasterEgg(11102),
    kDecorationAppTypeRepeatMessengeFilter(11103),
    kDecorationAppTypeEasterEggCounter(11104),
    kDecorationAppTypeACOrderIntimacy(12001),
    kDecorationAppTypeSuperWord(13000),
    kDecorationAppTypeDiamondUser(14000),
    kDecorationAppTypeRedBag(15000),
    kDecorationAppTypeUsrAvatarDeco(100009),
    kDecorationAppTypeUsrBeautyId(100100),
    ;

    private final int code;

   public static HuyaDecorationAppTypeEnum getByCode(int code) {
        for (HuyaDecorationAppTypeEnum value : values()) {
            if (value.getCode() == code) {
                return value;
            }
        }
        return null;
    }
}
