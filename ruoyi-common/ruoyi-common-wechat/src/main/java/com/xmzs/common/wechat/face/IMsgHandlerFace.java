package com.xmzs.common.wechat.face;

import com.xmzs.common.wechat.beans.BaseMsg;

/**
 * 消息处理接口
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月20日 上午12:13:49
 * @version 1.0
 *
 */
public interface IMsgHandlerFace {
	/**
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月20日 上午12:15:00
	 * @param msg
	 * @return
	 */
	public String textMsgHandle(BaseMsg msg);

	/**
	 * 处理图片消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月21日 下午11:07:06
	 * @param msg
	 * @return
	 */
	public String picMsgHandle(BaseMsg msg);

	/**
	 * 处理声音消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月22日 上午12:09:44
	 * @param msg
	 * @return
	 */
	public String voiceMsgHandle(BaseMsg msg);

	/**
	 * 处理小视频消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午12:19:50
	 * @param msg
	 * @return
	 */
	public String viedoMsgHandle(BaseMsg msg);

	/**
	 * 处理名片消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月1日 上午12:50:50
	 * @param msg
	 * @return
	 */
	public String nameCardMsgHandle(BaseMsg msg);

	/**
	 * 处理系统消息
	 *
	 * @author Relyn
	 * @date 2017年6月21日17:43:51
	 * @param msg
	 * @return
	 */
	public void sysMsgHandle(BaseMsg msg);

	/**
	 * 处理确认添加好友消息
	 *
	 * @date 2017年6月28日 下午10:15:30
	 * @param msg
	 * @return
	 */
	public String verifyAddFriendMsgHandle(BaseMsg msg);

	/**
	 * 处理收到的文件消息
	 *
	 * @date 2017年7月21日 下午11:59:14
	 * @param msg
	 * @return
	 */
	public String mediaMsgHandle(BaseMsg msg);

}
