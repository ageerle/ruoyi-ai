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
public enum HuyaOperationEnum {
    EWSCmd_NULL(0),
    EWSCmd_RegisterReq(1),
    EWSCmd_RegisterRsp(2),
    EWSCmd_WupReq(3),
    EWSCmd_WupRsp(4),
    EWSCmdC2S_HeartBeat(5),
    EWSCmdS2C_HeartBeatAck(6),
    EWSCmdS2C_MsgPushReq(7),
    EWSCmdC2S_DeregisterReq(8),
    EWSCmdS2C_DeRegisterRsp(9),
    EWSCmdC2S_VerifyCookieReq(10),
    EWSCmdS2C_VerifyCookieRsp(11),
    EWSCmdC2S_VerifyHuyaTokenReq(12),
    EWSCmdS2C_VerifyHuyaTokenRsp(13),
    EWSCmdC2S_UNVerifyReq(14),
    EWSCmdS2C_UNVerifyRsp(15),
    EWSCmdC2S_RegisterGroupReq(16),
    EWSCmdS2C_RegisterGroupRsp(17),
    EWSCmdC2S_UnRegisterGroupReq(18),
    EWSCmdS2C_UnRegisterGroupRsp(19),
    EWSCmdC2S_HeartBeatReq(20),
    EWSCmdS2C_HeartBeatRsp(21),
    EWSCmdS2C_MsgPushReq_V2(22),
    EWSCmdC2S_UpdateUserExpsReq(23),
    EWSCmdS2C_UpdateUserExpsRsp(24),
    EWSCmdC2S_WSHistoryMsgReq(25),
    EWSCmdS2C_WSHistoryMsgRsp(26),
    EWSCmdS2C_EnterP2P(27),
    EWSCmdS2C_EnterP2PAck(28),
    EWSCmdS2C_ExitP2P(29),
    EWSCmdS2C_ExitP2PAck(30),
    EWSCmdC2S_SyncGroupReq(31),
    EWSCmdS2C_SyncGroupRsp(32),
    EWSCmdC2S_UpdateUserInfoReq(33),
    EWSCmdS2C_UpdateUserInfoRsp(34),
    EWSCmdC2S_MsgAckReq(35),
    EWSCmdS2C_MsgAckRsp(36),
    EWSCmdC2S_CloudGameReq(37),
    EWSCmdS2C_CloudGamePush(38),
    EWSCmdS2C_CloudGameRsp(39),
    EWSCmdS2C_RpcReq(40),
    EWSCmdC2S_RpcRsp(41),
    EWSCmdS2C_RpcRspRsp(42),
    EWSCmdC2S_GetStunPortReq(101),
    EWSCmdS2C_GetStunPortRsp(102),
    EWSCmdC2S_WebRTCOfferReq(103),
    EWSCmdS2C_WebRTCOfferRsp(104),
    EWSCmdC2S_SignalUpgradeReq(105),
    EWSCmdS2C_SignalUpgradeRsp(106),
    ;
    private final int code;

    public static HuyaOperationEnum getByCode(int code) {
        for (HuyaOperationEnum value : HuyaOperationEnum.values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }
}
