package org.ruoyi.common.core.utils.ip;

import cn.hutool.http.HtmlUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.NetUtils;
import org.ruoyi.common.core.utils.StringUtils;

/**
 * 获取地址类
 *
 * @author Lion Li
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AddressUtils {

    // 未知IP
    public static final String UNKNOWN_IP = "XX XX";
    // 内网地址
    public static final String LOCAL_ADDRESS = "内网IP";
    // 未知地址
    public static final String UNKNOWN_ADDRESS = "未知";

    public static String getRealAddressByIP(String ip) {
        // 处理空串并过滤HTML标签
        ip = HtmlUtil.cleanHtmlTag(StringUtils.blankToDefault(ip,""));
        // 判断是否为IPv4
        if (NetUtils.isIPv4(ip)) {
            return resolverIPv4Region(ip);
        }
        // 判断是否为IPv6
        if (NetUtils.isIPv6(ip)) {
            return resolverIPv6Region(ip);
        }
        // 如果不是IPv4或IPv6，则返回未知IP
        return UNKNOWN_IP;
    }

    /**
     * 根据IPv4地址查询IP归属行政区域
     * @param ip ipv4地址
     * @return 归属行政区域
     */
    private static String resolverIPv4Region(String ip){
        // 内网不查询
        if (NetUtils.isInnerIP(ip)) {
            return LOCAL_ADDRESS;
        }
        return RegionUtils.getCityInfo(ip);
    }

    /**
     * 根据IPv6地址查询IP归属行政区域
     * @param ip ipv6地址
     * @return 归属行政区域
     */
    private static String resolverIPv6Region(String ip){
        // 内网不查询
        if (NetUtils.isInnerIPv6(ip)) {
            return LOCAL_ADDRESS;
        }
        log.warn("ip2region不支持IPV6地址解析：{}", ip);
        // 不支持IPv6，不再进行没有必要的IP地址信息的解析，直接返回
        // 如有需要，可自行实现IPv6地址信息解析逻辑，并在这里返回
        return UNKNOWN_ADDRESS;
    }

}
