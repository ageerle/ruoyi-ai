package org.ruoyi.common.wechat.itchat4j.utils.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.vdurmont.emoji.EmojiParser;
import org.ruoyi.common.wechat.itchat4j.utils.Config;
import org.ruoyi.common.wechat.itchat4j.utils.enums.OsNameEnum;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 常用工具类
 *
 * @author https://github.com/yaphone
 * @date 创建时间：2017年4月8日 下午10:59:55
 * @version 1.0
 *
 */
public class CommonTools {

	public static boolean printQr(String qrPath) {

		switch (Config.getOsNameEnum()) {
		case WINDOWS:
			if (Config.getOsNameEnum().equals(OsNameEnum.WINDOWS)) {
				Runtime runtime = Runtime.getRuntime();
				try {
					runtime.exec("cmd /c start " + qrPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;
		case MAC:
			if (Config.getOsNameEnum().equals(OsNameEnum.MAC)) {
				Runtime runtime = Runtime.getRuntime();
				try {
					runtime.exec("open " + qrPath);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;

		default:
			break;
		}
		return true;
	}

	public static boolean clearScreen() {
		switch (Config.getOsNameEnum()) {
		case WINDOWS:
			if (Config.getOsNameEnum().equals(OsNameEnum.WINDOWS)) {
				Runtime runtime = Runtime.getRuntime();
				try {
					runtime.exec("cmd /c " + "cls");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			break;

		default:
			break;
		}
		return true;
	}

	/**
	 * 正则表达式处理工具
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月9日 上午12:27:10
	 * @return
	 */
	public static Matcher getMatcher(String regEx, String text) {
		Pattern pattern = Pattern.compile(regEx);
		Matcher matcher = pattern.matcher(text);
		return matcher;
	}

	/**
	 * xml解析器
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月9日 下午6:24:25
	 * @param text
	 * @return
	 */
	public static Document xmlParser(String text) {
		Document doc = null;
		StringReader sr = new StringReader(text);
		InputSource is = new InputSource(sr);
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			DocumentBuilder builder = factory.newDocumentBuilder();
			doc = builder.parse(is);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return doc;
	}

	public static JSONObject structFriendInfo(JSONObject userObj) {
		Map<String, Object> friendInfoTemplate = new HashMap<String, Object>();
		friendInfoTemplate.put("UserName", "");
		friendInfoTemplate.put("City", "");
		friendInfoTemplate.put("DisplayName", "");
		friendInfoTemplate.put("PYQuanPin", "");
		friendInfoTemplate.put("RemarkPYInitial", "");
		friendInfoTemplate.put("Province", "");
		friendInfoTemplate.put("KeyWord", "");
		friendInfoTemplate.put("RemarkName", "");
		friendInfoTemplate.put("PYInitial", "");
		friendInfoTemplate.put("EncryChatRoomId", "");
		friendInfoTemplate.put("Alias", "");
		friendInfoTemplate.put("Signature", "");
		friendInfoTemplate.put("NickName", "");
		friendInfoTemplate.put("RemarkPYQuanPin", "");
		friendInfoTemplate.put("HeadImgUrl", "");

		friendInfoTemplate.put("UniFriend", 0);
		friendInfoTemplate.put("Sex", 0);
		friendInfoTemplate.put("AppAccountFlag", 0);
		friendInfoTemplate.put("VerifyFlag", 0);
		friendInfoTemplate.put("ChatRoomId", 0);
		friendInfoTemplate.put("HideInputBarFlag", 0);
		friendInfoTemplate.put("AttrStatus", 0);
		friendInfoTemplate.put("SnsFlag", 0);
		friendInfoTemplate.put("MemberCount", 0);
		friendInfoTemplate.put("OwnerUin", 0);
		friendInfoTemplate.put("ContactFlag", 0);
		friendInfoTemplate.put("Uin", 0);
		friendInfoTemplate.put("StarFriend", 0);
		friendInfoTemplate.put("Statues", 0);

		friendInfoTemplate.put("MemberList", new ArrayList<Object>());

		JSONObject r = new JSONObject();
		Set<String> keySet = friendInfoTemplate.keySet();
		for (String key : keySet) {
			if (userObj.containsKey(key)) {
				r.put(key, userObj.get(key));
			} else {
				r.put(key, friendInfoTemplate.get(key));
			}
		}

		return r;
	}

	public static String getSynckey(JSONObject obj) {
		JSONArray obj2 = obj.getJSONArray("List");
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < obj2.size(); i++) {
			JSONObject obj3 = (JSONObject) JSON.toJSON(obj2.get(i));
			sb.append(obj3.get("Val") + "|");
		}
		return sb.substring(0, sb.length() - 1); // 656159784|656159911|656159873|1491905341

	}

	public static JSONObject searchDictList(List<JSONObject> list, String key, String value) {
		JSONObject r = null;
		for (JSONObject i : list) {
			if (i.getString(key).equals(value)) {
				r = i;
				break;
			}
		}
		return r;
	}

	/**
	 * 处理emoji表情
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午2:39:04
	 * @param d
	 * @param k
	 */
	public static void emojiFormatter(JSONObject d, String k) {
//		Matcher matcher = getMatcher("<span class=\"emoji emoji(.{1,10})\"></span>", d.getString(k));
		Matcher matcher = getMatcher("<span class=\"emoji emoji(.+?)\"></span>", d.getString(k));
		StringBuilder sb = new StringBuilder();
		String content = d.getString(k);
		int lastStart = 0;
		while (matcher.find()) {
			String str = matcher.group(1);
			if (str.length() == 6) {

			} else if (str.length() == 10) {

			} else {
				str = "&#x" + str + ";";
				String tmp = content.substring(lastStart, matcher.start());
				sb.append(tmp + str);
				lastStart = matcher.end();
			}
		}
		if (lastStart < content.length()) {
			sb.append(content.substring(lastStart));
		}
		if (sb.length() != 0) {
			d.put(k, EmojiParser.parseToUnicode(sb.toString()));
		} else {
			d.put(k, content);
		}

	}

	/**
	 * 转化成alias
	 * @param d
	 * @param k
	 */
	public static void emojiFormatter2(JSONObject d, String k) {
		Matcher matcher = getMatcher("<span class=\"emoji emoji(.+?)\"></span>", d.getString(k));
		StringBuilder sb = new StringBuilder();
		String content = d.getString(k);
		int lastStart = 0;
		while (matcher.find()) {
			String str = matcher.group(1);
			if (str.length() == 6) {

			} else if (str.length() == 10) {

			} else {
				str = "&#x" + str + ";";
				String tmp = content.substring(lastStart, matcher.start());
				sb.append(tmp + str);
				lastStart = matcher.end();
			}
		}
		if (lastStart < content.length()) {
			sb.append(content.substring(lastStart));
		}
		if (sb.length() != 0) {
			d.put(k, EmojiParser.parseToAliases(EmojiParser.parseToUnicode(sb.toString())));
		} else {
			d.put(k, content);
		}

	}

	/**
	 * 消息格式化
	 *
	 * @author https://github.com/yaphone
	 * @date 2017年4月23日 下午4:19:08
	 * @param d
	 * @param k
	 */
	public static void msgFormatter(JSONObject d, String k) {
		d.put(k, d.getString(k).replace("<br/>", "\n"));
		emojiFormatter(d, k);
		// TODO 与emoji表情有部分兼容问题，目前暂未处理解码处理 d.put(k,
		// StringEscapeUtils.unescapeHtml4(d.getString(k)));

	}

	public static void main(String[] args) {
		String str2 = "三生三世<span class=\"emoji emoji1f46f\"></span>十三水<span class=\"emoji emoji1f440\"></span>";

		Matcher matcher = getMatcher("<span class=\"emoji emoji(.+?)\"></span>", str2);
		StringBuilder sb = new StringBuilder();
		String content = str2;
		int lastStart = 0;
		while (matcher.find()) {
			String str = matcher.group(1);
			if (str.length() == 6) {

			} else if (str.length() == 10) {

			} else {
				str = "&#x" + str + ";";
				String tmp = content.substring(lastStart, matcher.start());
				sb.append(tmp + str);
				lastStart = matcher.end();
			}
		}
		if (lastStart < content.length()) {
			sb.append(content.substring(lastStart));
		}
		if (sb.length() != 0) {
		}
	}

}
