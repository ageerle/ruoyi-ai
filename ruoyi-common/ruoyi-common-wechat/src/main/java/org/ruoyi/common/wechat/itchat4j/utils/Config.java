package org.ruoyi.common.wechat.itchat4j.utils;

import org.ruoyi.common.wechat.itchat4j.utils.enums.OsNameEnum;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 配置信息
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月23日 下午2:26:21
 * @version 1.0
 *
 */
public class Config {

	public static final String API_WXAPPID = "API_WXAPPID";

	public static final String picDir = "D://org.ruoyi.common.wechat";
	public static final String VERSION = "1.2.18";
	public static final String BASE_URL = "https://login.weixin.qq.com";
	public static final String OS = "";
	public static final String DIR = "";
	public static final String DEFAULT_QR = "QR.jpg";
	public static final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.71 Safari/537.36";

	/**
	 * 仅仅用于参考
	 */
	@Deprecated
	public static final ArrayList<String> API_SPECIAL_USER = new ArrayList<String>(Arrays.asList("filehelper", "weibo",
			"qqmail", "fmessage", "tmessage", "qmessage", "qqsync", "floatbottle", "lbsapp", "shakeapp", "medianote",
			"qqfriend", "readerapp", "blogapp", "facebookapp", "masssendapp", "meishiapp", "feedsapp", "voip",
			"blogappweixin", "brandsessionholder", "weixin", "weixinreminder", "officialaccounts", "wxitil",
			"notification_messages", "wxid_novlwrv3lqwv11", "gh_22b87fa7cb3c", "userexperience_alarm"));

	/**
	 * 获取文件目录
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月8日 下午10:27:42
	 * @return
	 */
	public static String getLocalPath() {
		String localPath = null;
		try {
			localPath = new File("").getCanonicalPath();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return localPath;
	}

	/**
	 * 获取系统平台
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月8日 下午10:27:53
	 */
	public static OsNameEnum getOsNameEnum() {
		String os = System.getProperty("os.name").toUpperCase();
		if (os.indexOf(OsNameEnum.DARWIN.toString()) >= 0) {
			return OsNameEnum.DARWIN;
		} else if (os.indexOf(OsNameEnum.WINDOWS.toString()) >= 0) {
			return OsNameEnum.WINDOWS;
		} else if (os.indexOf(OsNameEnum.LINUX.toString()) >= 0) {
			return OsNameEnum.LINUX;
		} else if (os.indexOf(OsNameEnum.MAC.toString()) >= 0) {
			return OsNameEnum.MAC;
		}
		return OsNameEnum.OTHER;
	}

}
