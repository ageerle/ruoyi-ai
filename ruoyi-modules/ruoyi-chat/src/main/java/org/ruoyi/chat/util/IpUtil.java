package org.ruoyi.chat.util;


import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * @author WangLe
 */
public class IpUtil {

    public static String getClientIp(HttpServletRequest request) {
        String ip = null;

        // 获取 X-Forwarded-For 中的第一个非 unknown 的 IP
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasLength(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            String[] ipAddresses = xForwardedFor.split(",");
            for (String ipAddress : ipAddresses) {
                if (StringUtils.hasLength(ipAddress) && !"unknown".equalsIgnoreCase(ipAddress.trim())) {
                    ip = ipAddress.trim();
                    break;
                }
            }
        }

        // 如果 X-Forwarded-For 中没有找到，则依次尝试其他 header
        if (ip == null) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        // 如果以上都没有获取到，则使用 RemoteAddr
        if (ip == null || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }
}
