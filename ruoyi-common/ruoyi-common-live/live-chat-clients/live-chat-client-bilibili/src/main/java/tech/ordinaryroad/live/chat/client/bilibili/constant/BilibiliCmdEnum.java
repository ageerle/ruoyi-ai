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

package tech.ordinaryroad.live.chat.client.bilibili.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * @author mjz
 * @date 2023/1/6
 */
@Getter
@RequiredArgsConstructor
public enum BilibiliCmdEnum {
    /**
     * 游客状态下，5分钟后会出现登录提示，弹幕中的用户名、用户id等信息将不再可见
     */
    LOG_IN_NOTICE,
    /**
     * 收到弹幕
     */
    DANMU_MSG,
    /**
     * 收到礼物
     */
    SEND_GIFT,
    /**
     * 有人上舰
     */
    GUARD_BUY,
    /**
     * 欢迎舰长
     */
    WELCOME_GUARD,
    WELCOME,
    /**
     * 礼物连击
     */
    COMBO_SEND,
    /**
     * 欢迎高能用户、(舰长?待验证)特殊消息
     */
    ENTRY_EFFECT,
    HOT_RANK_CHANGED,
    HOT_RANK_CHANGED_V2,
    INTERACT_WORD,
    /**
     * 开始直播
     */
    LIVE,
    LIVE_INTERACTIVE_GAME,
    NOTICE_MSG,
    /**
     * 高能榜数量更新
     */
    ONLINE_RANK_COUNT,
    ONLINE_RANK_TOP3,
    ONLINE_RANK_V2,
    PK_BATTLE_END,
    PK_BATTLE_FINAL_PROCESS,
    PK_BATTLE_PROCESS,
    PK_BATTLE_PROCESS_NEW,
    PK_BATTLE_SETTLE,
    PK_BATTLE_SETTLE_USER,
    PK_BATTLE_SETTLE_V2,
    /**
     * 主播准备中
     */
    PREPARING,
    ROOM_REAL_TIME_MESSAGE_UPDATE,
    /**
     * 停止直播的房间ID列表
     */
    STOP_LIVE_ROOM_LIST,
    /**
     * 醒目留言
     */
    SUPER_CHAT_MESSAGE,
    SUPER_CHAT_MESSAGE_JPN,
    /**
     * 删除醒目留言
     */
    SUPER_CHAT_MESSAGE_DELETE,
    WIDGET_BANNER,
    /**
     * 点赞数更新
     */
    LIKE_INFO_V3_UPDATE,
    /**
     * 为主播点赞
     */
    LIKE_INFO_V3_CLICK,
    HOT_ROOM_NOTIFY,
    /**
     * 观看人数变化
     */
    WATCHED_CHANGE,
    POPULAR_RANK_CHANGED,
    COMMON_NOTICE_DANMAKU,
    LIVE_MULTI_VIEW_CHANGE,
    RECOMMEND_CARD,
    PK_BATTLE_START_NEW,
    PK_BATTLE_ENTRANCE,
    AREA_RANK_CHANGED,
    ROOM_BLOCK_MSG,
    USER_TOAST_MSG,
    PK_BATTLE_PRE_NEW,
    PK_BATTLE_RANK_CHANGE,
    PK_BATTLE_START,
    PK_BATTLE_PRE,
    PLAY_TAG,
    ;

    public static BilibiliCmdEnum getByString(String cmd) {
        try {
            return BilibiliCmdEnum.valueOf(cmd);
        } catch (Exception e) {
            return null;
        }
    }
}
