package com.xmzs.common.wechat.beans;

import java.io.Serializable;

/**
 * 收到的微信消息
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年7月3日 下午10:28:06
 * @version 1.0
 *
 */
public class BaseMsg implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 1L;
	private int subMsgType;
	private int voiceLength;
	private String fileName;
	private int imgHeight;
	private String toUserName;
	private int hasProductId;
	private int imgStatus;
	private String url;
	private int imgWidth;
	private int forwardFlag;
	private int status;
	private String Ticket;
	/** 推荐消息报文 **/
	private RecommendInfo recommendInfo;
	private long createTime;
	private String newMsgId;
	/** 文本消息内容 **/
	private String text;
	/** 消息类型 **/
	private int msgType;
	/** 是否为群消息 **/
	private boolean groupMsg;
	private String msgId;
	private int statusNotifyCode;
	private AppInfo appInfo;
	private int appMsgType;
	private String Type;
	private int playLength;
	private String mediaId;
	private String content;
	private String statusNotifyUserName;
	/** 消息发送者ID **/
	private String fromUserName;
	private String oriContent;
	private String fileSize;

	public int getSubMsgType() {
		return subMsgType;
	}

	public void setSubMsgType(int subMsgType) {
		this.subMsgType = subMsgType;
	}

	public int getVoiceLength() {
		return voiceLength;
	}

	public void setVoiceLength(int voiceLength) {
		this.voiceLength = voiceLength;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public int getImgHeight() {
		return imgHeight;
	}

	public void setImgHeight(int imgHeight) {
		this.imgHeight = imgHeight;
	}

	public String getToUserName() {
		return toUserName;
	}

	public void setToUserName(String toUserName) {
		this.toUserName = toUserName;
	}

	public int getHasProductId() {
		return hasProductId;
	}

	public void setHasProductId(int hasProductId) {
		this.hasProductId = hasProductId;
	}

	public int getImgStatus() {
		return imgStatus;
	}

	public void setImgStatus(int imgStatus) {
		this.imgStatus = imgStatus;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public int getImgWidth() {
		return imgWidth;
	}

	public void setImgWidth(int imgWidth) {
		this.imgWidth = imgWidth;
	}

	public int getForwardFlag() {
		return forwardFlag;
	}

	public void setForwardFlag(int forwardFlag) {
		this.forwardFlag = forwardFlag;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public String getTicket() {
		return Ticket;
	}

	public void setTicket(String ticket) {
		Ticket = ticket;
	}

	public RecommendInfo getRecommendInfo() {
		return recommendInfo;
	}

	public void setRecommendInfo(RecommendInfo recommendInfo) {
		this.recommendInfo = recommendInfo;
	}

	public long getCreateTime() {
		return createTime;
	}

	public void setCreateTime(long createTime) {
		this.createTime = createTime;
	}

	public String getNewMsgId() {
		return newMsgId;
	}

	public void setNewMsgId(String newMsgId) {
		this.newMsgId = newMsgId;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getMsgType() {
		return msgType;
	}

	public void setMsgType(int msgType) {
		this.msgType = msgType;
	}

	public boolean isGroupMsg() {
		return groupMsg;
	}

	public void setGroupMsg(boolean groupMsg) {
		this.groupMsg = groupMsg;
	}

	public String getMsgId() {
		return msgId;
	}

	public void setMsgId(String msgId) {
		this.msgId = msgId;
	}

	public int getStatusNotifyCode() {
		return statusNotifyCode;
	}

	public void setStatusNotifyCode(int statusNotifyCode) {
		this.statusNotifyCode = statusNotifyCode;
	}

	public AppInfo getAppInfo() {
		return appInfo;
	}

	public void setAppInfo(AppInfo appInfo) {
		this.appInfo = appInfo;
	}

	public int getAppMsgType() {
		return appMsgType;
	}

	public void setAppMsgType(int appMsgType) {
		this.appMsgType = appMsgType;
	}

	public String getType() {
		return Type;
	}

	public void setType(String type) {
		Type = type;
	}

	public int getPlayLength() {
		return playLength;
	}

	public void setPlayLength(int playLength) {
		this.playLength = playLength;
	}

	public String getMediaId() {
		return mediaId;
	}

	public void setMediaId(String mediaId) {
		this.mediaId = mediaId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getStatusNotifyUserName() {
		return statusNotifyUserName;
	}

	public void setStatusNotifyUserName(String statusNotifyUserName) {
		this.statusNotifyUserName = statusNotifyUserName;
	}

	public String getFromUserName() {
		return fromUserName;
	}

	public void setFromUserName(String fromUserName) {
		this.fromUserName = fromUserName;
	}

	public String getOriContent() {
		return oriContent;
	}

	public void setOriContent(String oriContent) {
		this.oriContent = oriContent;
	}

	public String getFileSize() {
		return fileSize;
	}

	public void setFileSize(String fileSize) {
		this.fileSize = fileSize;
	}
}
