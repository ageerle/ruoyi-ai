package org.ruoyi.common.wechat.web.utils;

import org.apache.commons.codec.binary.Base64;

/**
 * @author WesleyOne
 * @create 2018/12/19
 */
public class Base64Util {

    /**
     * 编码
     * @param source
     * @return
     */
    public static String encode(String source){
        byte[] result = Base64.encodeBase64(source.getBytes());
        return result.toString();
    }

    /**
     * 解码
     * @param source
     * @return
     */
    public static String decode(String source){
        byte[] result = Base64.decodeBase64(source);
        return result.toString();
    }
}
