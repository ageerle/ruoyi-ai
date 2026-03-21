package org.ruoyi.common.redis.utils;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.core.utils.StringUtils;
import org.redisson.api.RIdGenerator;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * 发号器工具类
 *
 * @author 秋辞未寒
 * @date 2024-12-10
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SequenceUtils {

    /**
     * 默认初始值
     */
    public static final long DEFAULT_INIT_VALUE = 1L;

    /**
     * 默认步长
     */
    public static final long DEFAULT_STEP_VALUE = 1L;

    /**
     * 默认过期时间-天
     */
    public static final Duration DEFAULT_EXPIRE_TIME_DAY = Duration.ofDays(1);

    /**
     * 默认过期时间-分钟
     */
    public static final Duration DEFAULT_EXPIRE_TIME_MINUTE = Duration.ofMinutes(1);

    /**
     * 默认最小ID容量位数 - 6位数（即至少可以生成的ID为999999个）
     */
    public static final int DEFAULT_MIN_ID_CAPACITY_BITS = 6;

    /**
     * 获取Redisson客户端实例
     */
    private static final RedissonClient REDISSON_CLIENT = SpringUtils.getBean(RedissonClient.class);

    /**
     * 获取ID生成器
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return ID生成器
     */
    public static RIdGenerator getIdGenerator(String key, Duration expireTime, long initValue, long stepValue) {
        RIdGenerator idGenerator = REDISSON_CLIENT.getIdGenerator(key);
        // 初始值和步长不能小于等于0
        initValue = initValue <= 0 ? DEFAULT_INIT_VALUE : initValue;
        stepValue = stepValue <= 0 ? DEFAULT_STEP_VALUE : stepValue;
        // 设置初始值和步长
        idGenerator.tryInit(initValue, stepValue);
        // 设置过期时间
        idGenerator.expire(expireTime);
        return idGenerator;
    }

    /**
     * 获取ID生成器
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @return ID生成器
     */
    public static RIdGenerator getIdGenerator(String key, Duration expireTime) {
        return getIdGenerator(key, expireTime, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    /**
     * 获取指定业务key的唯一id
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return 唯一id
     */
    public static long getNextId(String key, Duration expireTime, long initValue, long stepValue) {
        return getIdGenerator(key, expireTime, initValue, stepValue).nextId();
    }

    /**
     * 获取指定业务key的唯一id (ID初始值=1,ID步长=1)
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @return 唯一id
     */
    public static long getNextId(String key, Duration expireTime) {
        return getIdGenerator(key, expireTime).nextId();
    }

    /**
     * 获取指定业务key的唯一id字符串
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param initValue  ID初始值
     * @param stepValue  ID步长
     * @return 唯一id
     */
    public static String getNextIdString(String key, Duration expireTime, long initValue, long stepValue) {
        return Convert.toStr(getNextId(key, expireTime, initValue, stepValue));
    }

    /**
     * 获取指定业务key的唯一id字符串 (ID初始值=1,ID步长=1)
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @return 唯一id
     */
    public static String getNextIdString(String key, Duration expireTime) {
        return Convert.toStr(getNextId(key, expireTime));
    }

    /**
     * 获取指定业务key的唯一id字符串 (ID初始值=1,ID步长=1)，不足位数自动补零
     *
     * @param key        业务key
     * @param expireTime 过期时间
     * @param width      位数，不足左补0
     * @return 补零后的唯一id字符串
     */
    public static String getPaddedNextIdString(String key, Duration expireTime, Integer width) {
        return StringUtils.leftPad(getNextIdString(key, expireTime), width, '0');
    }

    /**
     * 获取 yyyyMMdd 格式的唯一id
     *
     * @return 唯一id
     * @deprecated 请使用 {@link #getDateId(String)} 或 {@link #getDateId(String, boolean)}、{@link #getDateId(String, boolean, int)}，确保不同业务的ID连续性
     */
    @Deprecated
    public static String getDateId() {
        return getDateId("");
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id
     *
     * @param prefix 业务前缀
     * @return 唯一id
     */
    public static String getDateId(String prefix) {
        return getDateId(prefix, true);
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id
     *
     * @param prefix       业务前缀
     * @param isWithPrefix id是否携带业务前缀
     * @return 唯一id
     */
    public static String getDateId(String prefix, boolean isWithPrefix) {
        return getDateId(prefix, isWithPrefix, -1);
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id (启用ID补位，补位长度 = {@link #DEFAULT_MIN_ID_CAPACITY_BITS})}）
     *
     * @param prefix       业务前缀
     * @param isWithPrefix id是否携带业务前缀
     * @return 唯一id
     */
    public static String getPaddedDateId(String prefix, boolean isWithPrefix) {
        return getDateId(prefix, isWithPrefix, DEFAULT_MIN_ID_CAPACITY_BITS);
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @return 唯一id
     */
    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits) {
        return getDateId(prefix, isWithPrefix, minIdCapacityBits, LocalDate.now());
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @param time              时间
     * @return 唯一id
     */
    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDate time) {
        return getDateId(prefix, isWithPrefix, minIdCapacityBits, time, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    /**
     * 获取 prefix + yyyyMMdd 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @param time              时间
     * @param initValue         ID初始值
     * @param stepValue         ID步长
     * @return 唯一id
     */
    public static String getDateId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDate time, long initValue, long stepValue) {
        return getDatePatternId(prefix, isWithPrefix, minIdCapacityBits, time, DatePattern.PURE_DATE_FORMATTER, DEFAULT_EXPIRE_TIME_DAY, initValue, stepValue);
    }

    /**
     * 获取 yyyyMMddHHmmss 格式的唯一id
     *
     * @return 唯一id
     * @deprecated 请使用 {@link #getDateTimeId(String)} 或 {@link #getDateTimeId(String, boolean)}、{@link #getDateTimeId(String, boolean, int)}，确保不同业务的ID连续性
     */
    @Deprecated
    public static String getDateTimeId() {
        return getDateTimeId("", false);
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id
     *
     * @param prefix 业务前缀
     * @return 唯一id
     */
    public static String getDateTimeId(String prefix) {
        return getDateTimeId(prefix, true);
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id
     *
     * @param prefix       业务前缀
     * @param isWithPrefix id是否携带业务前缀
     * @return 唯一id
     */
    public static String getDateTimeId(String prefix, boolean isWithPrefix) {
        return getDateTimeId(prefix, isWithPrefix, -1);
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id (启用ID补位，补位长度 = {@link #DEFAULT_MIN_ID_CAPACITY_BITS})}）
     *
     * @param prefix       业务前缀
     * @param isWithPrefix id是否携带业务前缀
     * @return 唯一id
     */
    public static String getPaddedDateTimeId(String prefix, boolean isWithPrefix) {
        return getDateTimeId(prefix, isWithPrefix, DEFAULT_MIN_ID_CAPACITY_BITS);
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @return 唯一id
     */
    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits) {
        return getDateTimeId(prefix, isWithPrefix, minIdCapacityBits, LocalDateTime.now());
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @param time              时间
     * @return 唯一id
     */
    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDateTime time) {
        return getDateTimeId(prefix, isWithPrefix, minIdCapacityBits, time, DEFAULT_INIT_VALUE, DEFAULT_STEP_VALUE);
    }

    /**
     * 获取 prefix + yyyyMMddHHmmss 格式的唯一id
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @param initValue         ID初始值
     * @param stepValue         ID步长
     * @return 唯一id
     */
    public static String getDateTimeId(String prefix, boolean isWithPrefix, int minIdCapacityBits, LocalDateTime time, long initValue, long stepValue) {
        return getDatePatternId(prefix, isWithPrefix, minIdCapacityBits, time, DatePattern.PURE_DATETIME_FORMATTER, DEFAULT_EXPIRE_TIME_MINUTE, initValue, stepValue);
    }

    /**
     * 获取指定业务key的指定时间格式的ID
     *
     * @param prefix            业务前缀
     * @param isWithPrefix      id是否携带业务前缀
     * @param minIdCapacityBits 最小ID容量位数，小于该位数的ID，左补0（小于等于0表示不启用补位）
     * @param temporalAccessor  时间访问器
     * @param timeFormatter     时间格式
     * @param expireTime        过期时间
     * @param initValue         ID初始值
     * @param stepValue         ID步长
     * @return 唯一id
     */
    private static String getDatePatternId(String prefix, boolean isWithPrefix, int minIdCapacityBits, TemporalAccessor temporalAccessor, DateTimeFormatter timeFormatter, Duration expireTime, long initValue, long stepValue) {
        // 时间前缀
        String timePrefix = timeFormatter.format(temporalAccessor);
        // 业务前缀 + 时间前缀 构建 prefixKey
        String prefixKey = StringUtils.format("{}{}", StringUtils.blankToDefault(prefix, ""), timePrefix);

        // 获取id，例 -> 1
        String nextId = getNextIdString(prefixKey, expireTime, initValue, stepValue);

        // minIdCapacityBits 大于0，且 nextId 的长度小于 minIdCapacityBits，则左补0
        if (minIdCapacityBits > 0 && nextId.length() < minIdCapacityBits) {
            nextId = StringUtils.leftPad(nextId, minIdCapacityBits, '0');
        }

        // 是否携带业务前缀
        if (isWithPrefix) {
            // 例 -> P202507031
            // 其中 P 为业务前缀，202507031 为 yyyyMMdd 格式时间, 1 为nextId
            return StringUtils.format("{}{}", prefixKey, nextId);
        }
        // 例 -> 202507031
        // 其中 202507031 为 yyyyMMdd 格式时间, 1 为nextId
        return StringUtils.format("{}{}", timePrefix, nextId);
    }
}
