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

package tech.ordinaryroad.live.chat.client.commons.base.listener;


import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseCmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.BaseMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.ICmdMsg;
import tech.ordinaryroad.live.chat.client.commons.base.msg.IMsg;

/**
 * Base消息回调
 *
 * @author mjz
 * @date 2023/8/26
 */
public interface IBaseMsgListener<T, CmdEnum extends Enum<CmdEnum>> {

    /**
     * 收到消息（所有消息）
     *
     * @param msg IMsg
     */
    default void onMsg(T t, IMsg msg) {
        this.onMsg(msg);
    }

    default void onMsg(IMsg msg) {
        // ignore
    }

    /**
     * 收到cmd消息（所有cmd）
     *
     * @param cmd    CmdEnum
     * @param cmdMsg BaseCmdMsg
     */
    default void onCmdMsg(T t, CmdEnum cmd, ICmdMsg<CmdEnum> cmdMsg) {
        this.onCmdMsg(cmd, cmdMsg);
    }

    default void onCmdMsg(CmdEnum cmd, ICmdMsg<CmdEnum> cmdMsg) {
        // ignore
    }

    /**
     * 收到其他cmd消息（存在Enum，但Listener没有对应的回调）
     *
     * @param cmd    CmdEnum
     * @param cmdMsg BaseCmdMsg
     */
    default void onOtherCmdMsg(T t, CmdEnum cmd, ICmdMsg<CmdEnum> cmdMsg) {
        this.onOtherCmdMsg(cmd, cmdMsg);
    }

    default void onOtherCmdMsg(CmdEnum cmd, ICmdMsg<CmdEnum> cmdMsg) {
        // ignore
    }

    /**
     * 收到未知cmd消息
     *
     * @param cmdString 实际收到的cmd字符串
     * @param msg       BaseMsg
     */
    default void onUnknownCmd(T t, String cmdString, IMsg msg) {
        this.onUnknownCmd(cmdString, msg);
    }

    default void onUnknownCmd(String cmdString, IMsg msg) {
        // ignore
    }

    /**
     * 收到cmd消息（所有cmd）
     *
     * @param cmd    CmdEnum
     * @param cmdMsg BaseCmdMsg
     * @deprecated use {@link #onCmdMsg(T, Enum, ICmdMsg)}
     */
    default void onCmdMsg(T t, CmdEnum cmd, BaseCmdMsg<CmdEnum> cmdMsg) {
        this.onCmdMsg(cmd, cmdMsg);
    }

    /**
     * @deprecated use {@link #onCmdMsg(Enum, ICmdMsg)}
     */
    default void onCmdMsg(CmdEnum cmd, BaseCmdMsg<CmdEnum> cmdMsg) {
        // ignore
    }

    /**
     * 收到其他cmd消息（存在Enum，但Listener没有对应的回调）
     *
     * @param cmd    CmdEnum
     * @param cmdMsg BaseCmdMsg
     * @deprecated use {@link #onOtherCmdMsg(T, Enum, ICmdMsg)}
     */
    default void onOtherCmdMsg(T t, CmdEnum cmd, BaseCmdMsg<CmdEnum> cmdMsg) {
        this.onOtherCmdMsg(cmd, cmdMsg);
    }

    /**
     * @deprecated use {@link #onOtherCmdMsg(Enum, ICmdMsg)}
     */
    default void onOtherCmdMsg(CmdEnum cmd, BaseCmdMsg<CmdEnum> cmdMsg) {
        // ignore
    }

    /**
     * 收到未知cmd消息
     *
     * @param cmdString 实际收到的cmd字符串
     * @param msg       BaseMsg
     * @deprecated use {@link #onUnknownCmd(T, String, IMsg)}
     */
    default void onUnknownCmd(T t, String cmdString, BaseMsg msg) {
        this.onUnknownCmd(cmdString, msg);
    }

    /**
     * @deprecated use {@link #onUnknownCmd(String, IMsg)}
     */
    default void onUnknownCmd(String cmdString, BaseMsg msg) {
        // ignore
    }
}