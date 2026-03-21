package org.ruoyi.common.core.utils;

import cn.hutool.core.lang.PatternPool;
import cn.hutool.core.net.NetUtil;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.ruoyi.common.core.utils.regex.RegexUtils;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 增强网络相关工具类
 *
 * @author 秋辞未寒
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class NetUtils extends NetUtil {

    /**
     * 判断是否为IPv6地址
     *
     * @param ip IP地址
     * @return 是否为IPv6地址
     */
    public static boolean isIPv6(String ip) {
        try {
            // 判断是否为IPv6地址
            return InetAddress.getByName(ip) instanceof Inet6Address;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * 判断IPv6地址是否为内网地址
     * <br><br>
     * 以下地址将归类为本地地址，如有业务场景有需要，请根据需求自行处理：
     * <pre>
     * 通配符地址 0:0:0:0:0:0:0:0
     * 链路本地地址 fe80::/10
     * 唯一本地地址 fec0::/10
     * 环回地址 ::1
     * </pre>
     *
     * @param ip IP地址
     * @return 是否为内网地址
     */
    public static boolean isInnerIPv6(String ip) {
        try {
            // 判断是否为IPv6地址
            if (InetAddress.getByName(ip) instanceof Inet6Address inet6Address) {
                // isAnyLocalAddress 判断是否为通配符地址，通常不会将其视为内网地址，根据业务场景自行处理判断
                // isLinkLocalAddress 判断是否为链路本地地址，通常不算内网地址，是否划分归属于内网需要根据业务场景自行处理判断
                // isLoopbackAddress 判断是否为环回地址，与IPv4的 127.0.0.1 同理，用于表示本机
                // isSiteLocalAddress 判断是否为本地站点地址，IPv6唯一本地地址（Unique Local Addresses，简称ULA）
                if (inet6Address.isAnyLocalAddress()
                    || inet6Address.isLinkLocalAddress()
                    || inet6Address.isLoopbackAddress()
                    || inet6Address.isSiteLocalAddress()) {
                    return true;
                }
            }
        } catch (UnknownHostException e) {
            // 注意，isInnerIPv6方法和isIPv6方法的适用范围不同，所以此处不能忽略其异常信息。
            throw new IllegalArgumentException("Invalid IPv6 address!", e);
        }
        return false;
    }

    /**
     * 判断是否为IPv4地址
     *
     * @param ip IP地址
     * @return 是否为IPv4地址
     */
    public static boolean isIPv4(String ip) {
        return RegexUtils.isMatch(PatternPool.IPV4, ip);
    }

}
