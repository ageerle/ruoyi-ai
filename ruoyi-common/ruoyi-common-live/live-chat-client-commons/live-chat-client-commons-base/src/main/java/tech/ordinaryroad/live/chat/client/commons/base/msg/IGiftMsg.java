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

package tech.ordinaryroad.live.chat.client.commons.base.msg;

/**
 * @author mjz
 * @date 2023/9/8
 */
public interface IGiftMsg extends IMsg {

    /**
     * 粉丝牌名称
     */
    default String getBadgeName() {
        return "";
    }

    /**
     * 粉丝牌等级
     */
    default byte getBadgeLevel() {
        return 0;
    }

    /**
     * 发送方id
     */
    String getUid();

    /**
     * 发送方用户名
     */
    String getUsername();

    /**
     * 发送方头像地址
     *
     * @since 0.0.11
     */
    default String getUserAvatar() {
        return null;
    }

    /**
     * 礼物名称
     */
    String getGiftName();

    /**
     * 礼物图像地址
     */
    String getGiftImg();

    /**
     * 礼物id
     */
    String getGiftId();

    /**
     * 礼物数量
     */
    int getGiftCount();

    /**
     * 单个礼物价格
     */
    int getGiftPrice();

    /**
     * 接收方id
     */
    String getReceiveUid();

    /**
     * 接收方用户名
     */
    String getReceiveUsername();
}
