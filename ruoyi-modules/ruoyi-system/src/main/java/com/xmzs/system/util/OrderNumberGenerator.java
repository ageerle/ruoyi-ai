package com.xmzs.system.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadLocalRandom;
public class OrderNumberGenerator {
    // 订单编号前缀
    private static final String PREFIX = "NO";

    // 时间格式化
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmm");

    // 生成订单编号
    public static String generate() {
        // 获取当前日期时间字符串
        String dateTimeStr = DATE_FORMAT.format(new Date());

        // 生成随机数 (这里举例生成一个5位随机数)
        int randomNum = ThreadLocalRandom.current().nextInt(10000, 99999);

        // 拼接订单编号
        return dateTimeStr + randomNum;
    }
}
