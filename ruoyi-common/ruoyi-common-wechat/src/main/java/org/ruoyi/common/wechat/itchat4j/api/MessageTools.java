package org.ruoyi.common.wechat.itchat4j.api;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.util.EntityUtils;
import org.ruoyi.common.wechat.itchat4j.beans.BaseMsg;
import org.ruoyi.common.wechat.itchat4j.beans.RecommendInfo;
import org.ruoyi.common.wechat.itchat4j.core.Core;
import org.ruoyi.common.wechat.itchat4j.core.CoreManage;
import org.ruoyi.common.wechat.itchat4j.utils.Config;
import org.ruoyi.common.wechat.itchat4j.utils.LogInterface;
import org.ruoyi.common.wechat.itchat4j.utils.enums.SendMsgType;
import org.ruoyi.common.wechat.itchat4j.utils.enums.StorageLoginInfoEnum;
import org.ruoyi.common.wechat.itchat4j.utils.enums.URLEnum;
import org.ruoyi.common.wechat.itchat4j.utils.enums.VerifyFriendEnum;
import org.ruoyi.common.wechat.web.constant.UploadConstant;

import javax.activation.MimetypesFileTypeMap;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 消息处理类
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午2:30:37
 * @version 1.0
 *
 * @author WesleyOne 修改
 */
public class MessageTools implements LogInterface {


	/**
	 * 通过UserName发送消息
	 * @param toUserName
	 * @param uniqueKey
	 * @param data
	 * @param type
	 * @return
	 */
	public static boolean send(String toUserName, String uniqueKey, String data, String type){
		String nickName = WechatTools.getNickNameByUserName(toUserName, uniqueKey);
		if (SendMsgType.TEXT.toValue().equals(type)){
			LOG.info("发送文本 {}:{}", nickName, data);
			return MessageTools.sendMsgById(data,toUserName,uniqueKey);
		}else if (SendMsgType.IMG.toValue().equals(type)){
			LOG.info("发送图片 {}:{}", nickName, UploadConstant.IMG_PATH_SEP+data);
			return MessageTools.sendPicMsgByUserId(toUserName,UploadConstant.IMG_PATH_SEP+data,uniqueKey);
		}else if (SendMsgType.FILE.toValue().equals(type)){
			LOG.info("发送文件 {}:{}",nickName,UploadConstant.FILE_PATH_SEP+data);
			return MessageTools.sendFileMsgByUserId(toUserName,UploadConstant.FILE_PATH_SEP+data,uniqueKey);
		}
		return false;
	}

	/**
	 * 通过NickName发送消息
	 * @param nickName
	 * @param uniqueKey
	 * @param data
	 * @param type
	 * @param isGroup
	 * @return
	 */
	public static boolean sendByNickName(String nickName, String uniqueKey, String data, String type,boolean isGroup){

		if (SendMsgType.TEXT.toValue().equals(type)){
			LOG.info("发送文本 {}:{}", nickName, data);
			return MessageTools.sendMsgByNickNameApi(data, nickName, uniqueKey, isGroup);
		}else if (SendMsgType.IMG.toValue().equals(type)){
			LOG.info("发送图片 {}:{}", nickName, UploadConstant.IMG_PATH_SEP+data);
			return MessageTools.sendPicMsgByNickNameApi(nickName, UploadConstant.IMG_PATH_SEP + data, uniqueKey, isGroup);
		}else if (SendMsgType.FILE.toValue().equals(type)){
			LOG.info("发送文件 {}:{}",nickName,UploadConstant.FILE_PATH_SEP+data);
			return MessageTools.sendFileMsgByNickNameApi(nickName,UploadConstant.FILE_PATH_SEP+data,uniqueKey,isGroup);
		}
		return false;
	}


	/**
	 * 根据UserName发送文本消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月6日 上午11:45:51
	 * @param text
	 * @param toUserName
	 * @param uniqueKey
	 */
	public static boolean sendMsgById(String text, String toUserName, String uniqueKey) {
		if (text == null) {
			return false;
		}
		return webWxSendMsg(1, text, toUserName, uniqueKey);
	}

	/**
	 * 根据NickName发送文本消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月4日 下午11:17:38
	 * @param text
	 * @param nickName
	 */
	public static boolean sendMsgByNickName(String text, String nickName, String uniqueKey) {
		if (nickName != null) {
			String toUserName = WechatTools.getContactUserNameByNickName(nickName,uniqueKey);
			if (toUserName != null) {
				return sendMsgById(text, toUserName, uniqueKey);
			}
		}
		return false;

	}

	public static boolean sendMsgByNickNameApi(String text, String nickName, String uniqueKey, boolean isGroup){
		if (isGroup){
			return sendGroupMsgByNickName(text,nickName,uniqueKey);
		}else{
			return sendMsgByNickName(text,nickName,uniqueKey);
		}
	}


	/**
	 * 根据NickName发送群文本消息
	 * @param text
	 * @param nickName
	 * @param uniqueKey
	 * @return
	 */
	private static boolean sendGroupMsgByNickName(String text, String nickName, String uniqueKey) {
		if (nickName != null) {
			String toUserName = WechatTools.getGroupUserNameByNickName(nickName,uniqueKey);
			if (toUserName != null) {
				return sendMsgById(text, toUserName, uniqueKey);
			}
		}
		return false;

	}

	/**
	 * 消息发送
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午2:32:02
	 * @param msgType
	 * @param content
	 * @param toUserName
	 */
	public static boolean webWxSendMsg(int msgType, String content, String toUserName, String uniqueKey) {
		Core core = CoreManage.getInstance(uniqueKey);
		if (!core.isAlive()){
			return false;
		}
		String url = String.format(URLEnum.WEB_WX_SEND_MSG.getUrl(), core.getLoginInfo().get("url"));
		Map<String, Object> msgMap = new HashMap<String, Object>(12);
		msgMap.put("Type", msgType);
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getUserName());
		msgMap.put("ToUserName", toUserName == null ? core.getUserName() : toUserName);
		msgMap.put("LocalID", System.currentTimeMillis() * 10);
		msgMap.put("ClientMsgId", System.currentTimeMillis() * 10);
		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		try {
			String paramStr = JSON.toJSONString(paramMap);
			HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
			EntityUtils.toString(entity, Consts.UTF_8);
			return true;
		} catch (Exception e) {
			LOG.error("webWxSendMsg", e);
			return false;
		}
	}

	/**
	 * 上传多媒体文件到 微信服务器，目前应该支持3种类型: 1. pic 直接显示，包含图片，表情 2.video 3.doc 显示为文件，包含PDF等
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 上午12:41:13
	 * @param filePath
	 * @return
	 */
	private static JSONObject webWxUploadMedia(String filePath, String uniqueKey) {
		Core core = CoreManage.getInstance(uniqueKey);
		File f = new File(filePath);
		if (!f.exists() || !f.isFile()) {
			LOG.info("file is not exist");
			return null;
		}
		String url = String.format(URLEnum.WEB_WX_UPLOAD_MEDIA.getUrl(), core.getLoginInfo().get("fileUrl"));
		String mimeType = new MimetypesFileTypeMap().getContentType(f);
		String mediaType = "";
		if (mimeType == null) {
			mimeType = "text/plain";
		} else {
			mediaType = mimeType.split("/")[0].equals("image") ? "pic" : "doc";
		}
		String lastModifieDate = new SimpleDateFormat("yyyy MM dd HH:mm:ss").format(new Date());
		long fileSize = f.length();
		String passTicket = (String) core.getLoginInfo().get("pass_ticket");
		String clientMediaId = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(new Random().nextLong()).substring(0, 4);
		String webwxDataTicket = core.getMyHttpClient().getCookie("webwx_data_ticket");
		if (webwxDataTicket == null) {
			LOG.error("get cookie webwx_data_ticket error");
			return null;
		}

		Map<String, Object> paramMap = core.getParamMap();

		paramMap.put("ClientMediaId", clientMediaId);
		paramMap.put("TotalLen", fileSize);
		paramMap.put("StartPos", 0);
		paramMap.put("DataLen", fileSize);
		paramMap.put("MediaType", 4);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);

		builder.addTextBody("id", "WU_FILE_0", ContentType.TEXT_PLAIN);
		builder.addTextBody("name", filePath, ContentType.TEXT_PLAIN);
		builder.addTextBody("type", mimeType, ContentType.TEXT_PLAIN);
		builder.addTextBody("lastModifieDate", lastModifieDate, ContentType.TEXT_PLAIN);
		builder.addTextBody("size", String.valueOf(fileSize), ContentType.TEXT_PLAIN);
		builder.addTextBody("mediatype", mediaType, ContentType.TEXT_PLAIN);
		builder.addTextBody("uploadmediarequest", JSON.toJSONString(paramMap), ContentType.TEXT_PLAIN);
		builder.addTextBody("webwx_data_ticket", webwxDataTicket, ContentType.TEXT_PLAIN);
		builder.addTextBody("pass_ticket", passTicket, ContentType.TEXT_PLAIN);
		builder.addBinaryBody("filename", f, ContentType.create(mimeType), filePath);
		HttpEntity reqEntity = builder.build();
		HttpEntity entity = core.getMyHttpClient().doPostFile(url, reqEntity);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result);
			} catch (Exception e) {
				LOG.error("webWxUploadMedia 错误： ", e);
			}

		}
		return null;
	}


	public static boolean sendPicMsgByNickNameApi(String nickName, String filePath, String uniqueKey,boolean isGroup){
		if (isGroup){
			return sendGroupPicMsgByNickName(nickName, filePath, uniqueKey);
		}else {
			return sendPicMsgByNickName(nickName, filePath, uniqueKey);
		}
	}

	/**
	 * 根据NickName发送图片消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:32:45
	 * @param nickName
	 * @param filePath
	 * @return
	 */
	public static boolean sendPicMsgByNickName(String nickName, String filePath, String uniqueKey) {
		String toUserName = WechatTools.getContactUserNameByNickName(nickName,uniqueKey);
		if (toUserName != null) {
			LOG.info("发送图片 {}:{}",nickName,filePath);
			return sendPicMsgByUserId(toUserName, filePath, uniqueKey);
		}
		return false;
	}

	/**
	 * 根据群的NickName发送图片消息到群
	 * @param nickName
	 * @param filePath
	 * @param uniqueKey
	 * @return
	 */
	public static boolean sendGroupPicMsgByNickName(String nickName, String filePath, String uniqueKey) {
		String toUserName = WechatTools.getGroupUserNameByNickName(nickName,uniqueKey);
		if (toUserName != null) {
			LOG.info("发送群图片 {}:{}",nickName,filePath);
			return sendPicMsgByUserId(toUserName, filePath, uniqueKey);
		}
		return false;
	}

	/**
	 * 根据用户id发送图片消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:34:24
	 * @param userId
	 * @param filePath
	 * @param uniqueKey
	 * @return
	 */
	public static boolean sendPicMsgByUserId(String userId, String filePath, String uniqueKey) {
		JSONObject responseObj = webWxUploadMedia(filePath, uniqueKey);
		if (responseObj != null) {
			String mediaId = responseObj.getString("MediaId");
			if (mediaId != null) {
				return webWxSendMsgImg(userId, mediaId, uniqueKey);
			}
		}
		return false;
	}

	/**
	 * 发送图片消息，内部调用
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午10:38:55
	 * @return
	 */
	private static boolean webWxSendMsgImg(String userId, String mediaId, String uniqueKey) {
		Core core = CoreManage.getInstance(uniqueKey);
		if (!core.isAlive()){
			return false;
		}
		String url = String.format("%s/webwxsendmsgimg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		Map<String, Object> msgMap = new HashMap<String, Object>(8);
		msgMap.put("Type", 3);
		msgMap.put("MediaId", mediaId);
		msgMap.put("FromUserName", core.getUserSelf().getString("UserName"));
		msgMap.put("ToUserName", userId);
		String clientMsgId = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("BaseRequest", core.getParamMap().get("BaseRequest"));
		paramMap.put("Msg", msgMap);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				LOG.error("webWxSendMsgImg 错误： ", e);
			}
		}
		return false;

	}

	/**
	 * 根据用户id发送文件
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月7日 下午11:57:36
	 * @param userId
	 * @param filePath
	 * @return
	 */
	private static boolean sendFileMsgByUserId(String userId, String filePath, String uniqueKey) {
		String title = new File(filePath).getName();
		Map<String, String> data = new HashMap<String, String>(12);
		data.put("appid", Config.API_WXAPPID);
		data.put("title", title);
		data.put("totallen", "");
		data.put("attachid", "");
		data.put("type", "6"); // APPMSGTYPE_ATTACH
		data.put("fileext", title.split("\\.")[1]); // 文件后缀
		JSONObject responseObj = webWxUploadMedia(filePath, uniqueKey);
		if (responseObj != null) {
			data.put("totallen", responseObj.getString("StartPos"));
			data.put("attachid", responseObj.getString("MediaId"));
		} else {
			LOG.error("sednFileMsgByUserId 错误: ", data);
		}
		return webWxSendAppMsg(userId, data, uniqueKey);
	}

	public static boolean sendFileMsgByNickNameApi(String nickName, String filePath, String uniqueKey, boolean isGroup){
		if (isGroup){
			return sendGroupFileMsgByNickName(nickName,filePath,uniqueKey);
		}else {
			return sendFileMsgByNickName(nickName,filePath,uniqueKey);
		}
	}


	/**
	 * 根据用户昵称发送文件消息
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月10日 下午10:59:27
	 * @param nickName
	 * @param filePath
	 * @return
	 */
	private static boolean sendFileMsgByNickName(String nickName, String filePath, String uniqueKey) {
		String toUserName = WechatTools.getContactUserNameByNickName(nickName,uniqueKey);
		if (toUserName != null) {
			LOG.info("发送文件 {}:{}",nickName,filePath);
			return sendFileMsgByUserId(toUserName, filePath, uniqueKey);
		}
		return false;
	}

	/**
	 * 根据群昵称发送文件消息
	 * @param nickName
	 * @param filePath
	 * @param uniqueKey
	 * @return
	 */
	private static boolean sendGroupFileMsgByNickName(String nickName, String filePath, String uniqueKey) {
		String toUserName = WechatTools.getGroupUserNameByNickName(nickName,uniqueKey);
		if (toUserName != null) {
			LOG.info("发送文件 {}:{}",nickName,filePath);
			return sendFileMsgByUserId(toUserName, filePath, uniqueKey);
		}
		return false;
	}

	/**
	 * 内部调用
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年5月10日 上午12:21:28
	 * @param userId
	 * @param data
	 * @param uniqueKey
	 * @return
	 */
	private static boolean webWxSendAppMsg(String userId, Map<String, String> data, String uniqueKey) {
		Core core = CoreManage.getInstance(uniqueKey);
		if (!core.isAlive()){
			return false;
		}
		String url = String.format("%s/webwxsendappmsg?fun=async&f=json&pass_ticket=%s", core.getLoginInfo().get("url"),
				core.getLoginInfo().get("pass_ticket"));
		String clientMsgId = String.valueOf(System.currentTimeMillis())
				+ String.valueOf(new Random().nextLong()).substring(1, 5);
		String content = "<appmsg appid='wxeb7ec651dd0aefa9' sdkver=''><title>" + data.get("title")
				+ "</title><des></des><action></action><type>6</type><content></content><url></url><lowurl></lowurl>"
				+ "<appattach><totallen>" + data.get("totallen") + "</totallen><attachid>" + data.get("attachid")
				+ "</attachid><fileext>" + data.get("fileext") + "</fileext></appattach><extinfo></extinfo></appmsg>";
		Map<String, Object> msgMap = new HashMap<String, Object>(12);
		msgMap.put("Type", data.get("type"));
		msgMap.put("Content", content);
		msgMap.put("FromUserName", core.getUserSelf().getString("UserName"));
		msgMap.put("ToUserName", userId);
		msgMap.put("LocalID", clientMsgId);
		msgMap.put("ClientMsgId", clientMsgId);
		/*
		 * Map<String, Object> paramMap = new HashMap<String, Object>();
		 *
		 * @SuppressWarnings("unchecked") Map<String, Map<String, String>>
		 * baseRequestMap = (Map<String, Map<String, String>>)
		 * core.getLoginInfo() .get("baseRequest"); paramMap.put("BaseRequest",
		 * baseRequestMap.get("BaseRequest"));
		 */

		Map<String, Object> paramMap = core.getParamMap();
		paramMap.put("Msg", msgMap);
		paramMap.put("Scene", 0);
		String paramStr = JSON.toJSONString(paramMap);
		HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
		if (entity != null) {
			try {
				String result = EntityUtils.toString(entity, Consts.UTF_8);
				return JSON.parseObject(result).getJSONObject("BaseResponse").getInteger("Ret") == 0;
			} catch (Exception e) {
				LOG.error("错误: ", e);
			}
		}
		return false;
	}

	/**
	 * 被动添加好友
	 *
	 * @date 2017年6月29日 下午10:08:43
	 * @param msg
	 * @param accept
	 *            true 接受 false 拒绝
	 */
	private static void addFriend(BaseMsg msg, boolean accept, String uniqueKey) {
		Core core = CoreManage.getInstance(uniqueKey);
		if (!accept) { // 不添加
			return;
		}
		int status = VerifyFriendEnum.ACCEPT.getCode(); // 接受好友请求
		RecommendInfo recommendInfo = msg.getRecommendInfo();
		String userName = recommendInfo.getUserName();
		String ticket = recommendInfo.getTicket();
		// 更新好友列表
		// TODO 此处需要更新好友列表
		// core.getContactList().add(msg.getJSONObject("RecommendInfo"));

		String url = String.format(URLEnum.WEB_WX_VERIFYUSER.getUrl(), core.getLoginInfo().get("url"),
				String.valueOf(System.currentTimeMillis() / 3158L), core.getLoginInfo().get("pass_ticket"));

		List<Map<String, Object>> verifyUserList = new ArrayList<Map<String, Object>>();
		Map<String, Object> verifyUser = new HashMap<String, Object>();
		verifyUser.put("Value", userName);
		verifyUser.put("VerifyUserTicket", ticket);
		verifyUserList.add(verifyUser);

		List<Integer> sceneList = new ArrayList<Integer>();
		sceneList.add(33);

		JSONObject body = new JSONObject();
		body.put("BaseRequest", core.getParamMap().get("BaseRequest"));
		body.put("Opcode", status);
		body.put("VerifyUserListSize", 1);
		body.put("VerifyUserList", verifyUserList);
		body.put("VerifyContent", "");
		body.put("SceneListCount", 1);
		body.put("SceneList", sceneList);
		body.put("skey", core.getLoginInfo().get(StorageLoginInfoEnum.skey.getKey()));

		String result = null;
		try {
			String paramStr = JSON.toJSONString(body);
			HttpEntity entity = core.getMyHttpClient().doPost(url, paramStr);
			result = EntityUtils.toString(entity, Consts.UTF_8);
		} catch (Exception e) {
			LOG.error("webWxSendMsg", e);
		}

		if (StringUtils.isBlank(result)) {
			LOG.error("被动添加好友失败");
		}

		LOG.debug(result);

	}

}
