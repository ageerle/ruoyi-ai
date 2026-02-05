package org.ruoyi.common.core.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.utils.StringUtils;

/*
 * 日期格式
 * "yyyy"：4位数的年份，例如：2023年表示为"2023"。
 * "yy"：2位数的年份，例如：2023年表示为"23"。
 * "MM"：2位数的月份，取值范围为01到12，例如：7月表示为"07"。
 * "M"：不带前导零的月份，取值范围为1到12，例如：7月表示为"7"。
 * "dd"：2位数的日期，取值范围为01到31，例如：22日表示为"22"。
 * "d"：不带前导零的日期，取值范围为1到31，例如：22日表示为"22"。
 * "EEEE"：星期的全名，例如：星期三表示为"Wednesday"。
 * "E"：星期的缩写，例如：星期三表示为"Wed"。
 * "DDD" 或 "D"：一年中的第几天，取值范围为001到366，例如：第200天表示为"200"。
 * 时间格式
 * "HH"：24小时制的小时数，取值范围为00到23，例如：下午5点表示为"17"。
 * "hh"：12小时制的小时数，取值范围为01到12，例如：下午5点表示为"05"。
 * "mm"：分钟数，取值范围为00到59，例如：30分钟表示为"30"。
 * "ss"：秒数，取值范围为00到59，例如：45秒表示为"45"。
 * "SSS"：毫秒数，取值范围为000到999，例如：123毫秒表示为"123"。
 */

/**
 * 日期格式与时间格式枚举
 */
@Getter
@AllArgsConstructor
public enum FormatsType {

    /**
     * 例如：2023年表示为"23"
     */
    YY("yy"),

    /**
     * 例如：2023年表示为"2023"
     */
    YYYY("yyyy"),

    /**
     * 例例如，2023年7月可以表示为 "2023-07"
     */
    YYYY_MM("yyyy-MM"),

    /**
     * 例如，日期 "2023年7月22日" 可以表示为 "2023-07-22"
     */
    YYYY_MM_DD("yyyy-MM-dd"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分"，则可以表示为 "2023-07-22 15:30"
     */
    YYYY_MM_DD_HH_MM("yyyy-MM-dd HH:mm"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分45秒"，则可以表示为 "2023-07-22 15:30:45"
     */
    YYYY_MM_DD_HH_MM_SS("yyyy-MM-dd HH:mm:ss"),

    /**
     * 例如：下午3点30分45秒，表示为 "15:30:45"
     */
    HH_MM_SS("HH:mm:ss"),

    /**
     * 例例如，2023年7月可以表示为 "2023/07"
     */
    YYYY_MM_SLASH("yyyy/MM"),

    /**
     * 例如，日期 "2023年7月22日" 可以表示为 "2023/07/22"
     */
    YYYY_MM_DD_SLASH("yyyy/MM/dd"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分45秒"，则可以表示为 "2023/07/22 15:30:45"
     */
    YYYY_MM_DD_HH_MM_SLASH("yyyy/MM/dd HH:mm"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分45秒"，则可以表示为 "2023/07/22 15:30:45"
     */
    YYYY_MM_DD_HH_MM_SS_SLASH("yyyy/MM/dd HH:mm:ss"),

    /**
     * 例例如，2023年7月可以表示为 "2023.07"
     */
    YYYY_MM_DOT("yyyy.MM"),

    /**
     * 例如，日期 "2023年7月22日" 可以表示为 "2023.07.22"
     */
    YYYY_MM_DD_DOT("yyyy.MM.dd"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分"，则可以表示为 "2023.07.22 15:30"
     */
    YYYY_MM_DD_HH_MM_DOT("yyyy.MM.dd HH:mm"),

    /**
     * 例如，当前时间如果是 "2023年7月22日下午3点30分45秒"，则可以表示为 "2023.07.22 15:30:45"
     */
    YYYY_MM_DD_HH_MM_SS_DOT("yyyy.MM.dd HH:mm:ss"),

    /**
     * 例如，2023年7月可以表示为 "202307"
     */
    YYYYMM("yyyyMM"),

    /**
     * 例如，2023年7月22日可以表示为 "20230722"
     */
    YYYYMMDD("yyyyMMdd"),

    /**
     * 例如，2023年7月22日下午3点可以表示为 "2023072215"
     */
    YYYYMMDDHH("yyyyMMddHH"),

    /**
     * 例如，2023年7月22日下午3点30分可以表示为 "202307221530"
     */
    YYYYMMDDHHMM("yyyyMMddHHmm"),

    /**
     * 例如，2023年7月22日下午3点30分45秒可以表示为 "20230722153045"
     */
    YYYYMMDDHHMMSS("yyyyMMddHHmmss");

    /**
     * 时间格式
     */
    private final String timeFormat;

    public static FormatsType getFormatsType(String str) {
        for (FormatsType value : values()) {
            if (StringUtils.contains(str, value.getTimeFormat())) {
                return value;
            }
        }
        throw new RuntimeException("'FormatsType' not found By " + str);
    }
}
