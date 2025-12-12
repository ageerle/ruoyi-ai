package org.ruoyi.common.utils;

import cn.hutool.core.util.StrUtil;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;

/**
 * MD5 算法
 *
 * @author Admin
 */
public class MD5Util {

    /**
     * 全局数组
     */
    public final static String[] strDigits = {"0", "1", "2", "3", "4", "5",
            "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    public MD5Util() {
    }

    /**
     * 返回形式为数字跟字符串
     *
     * @param bByte
     * @return String
     * @Date 2023/7/3
     **/
    public static String byteToArrayString(byte bByte) {
        int iRet = bByte;
        if (iRet < 0) {
            iRet += 256;
        }
        int iD1 = iRet / 16;
        int iD2 = iRet % 16;
        return strDigits[iD1] + strDigits[iD2];
    }

    /**
     * 转换字节数组为16进制字串
     *
     * @param bByte
     * @return String
     * @Date 2023/7/3
     **/
    public static String byteToString(byte[] bByte) {
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < bByte.length; i++) {
            sBuffer.append(byteToArrayString(bByte[i]));
        }
        return sBuffer.toString();
    }

    /**
     * 生成md5代码
     *
     * @param strObj
     * @return String
     * @Date 2023/7/3
     **/
    public static String GetMD5Code(String strObj) {
        String resultString = null;
        try {
            resultString = new String(strObj);
            MessageDigest md = MessageDigest.getInstance("MD5");
            resultString = byteToString(md.digest(strObj.getBytes()));
        } catch (NoSuchAlgorithmException ex) {
            ex.printStackTrace();
        }
        return resultString;
    }

    /**
     * 组装签名的字段
     *
     * @param params     参数
     * @param urlEncoder 是否urlEncoder
     * @return {String}
     */
    public static String packageSign(Map<String, Object> params, boolean urlEncoder) {
        // 先将参数以其参数名的字典序升序进行排序
        TreeMap<String, Object> sortedParams = new TreeMap<String, Object>(params);
        // 遍历排序后的字典，将所有参数按"key=value"格式拼接在一起
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Map.Entry<String, Object> param : sortedParams.entrySet()) {
            String value = String.valueOf(param.getValue());
            if (StrUtil.isBlank(value)) {
                continue;
            }
            if (first) {
                first = false;
            } else {
                sb.append("&");
            }
            sb.append(param.getKey()).append("=");
            sb.append(value);
        }
        return sb.toString();
    }

}
