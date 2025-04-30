package org.ruoyi.util;

import java.util.LinkedHashMap;

/**
 * 微信二维码缓存工具类
 *
 * @author https://www.wdbyte.com
 */
public class WeixinQrCodeCacheUtil {
    private static long MAX_CACHE_SIZE = 10000;
    private static LinkedHashMap<String, String> QR_CODE_TICKET_MAP = new LinkedHashMap<>();

    /**
     * 增加一个 Ticket
     * 首次 put：value 为 ""
     * 再次 put: value 有 openId，若openId已经存在，则已被扫码
     *
     * @param key
     * @param value
     */
    public synchronized static void put(String key, String value) {
        QR_CODE_TICKET_MAP.put(key, value);
        if (QR_CODE_TICKET_MAP.size() > MAX_CACHE_SIZE) {
            String first = QR_CODE_TICKET_MAP.keySet().stream().findFirst().get();
            QR_CODE_TICKET_MAP.remove(first);
        }
    }

    public synchronized static String get(String key) {
        return QR_CODE_TICKET_MAP.remove(key);
    }

}
