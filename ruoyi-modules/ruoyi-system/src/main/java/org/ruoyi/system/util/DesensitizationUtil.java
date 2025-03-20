package org.ruoyi.system.util;


public class DesensitizationUtil {
    public static String maskData(String data) {
        if (data == null || data.length() <= 4) {
            return data;
        }
        int start = 2;
        int end = data.length() - 2;
        StringBuilder masked = new StringBuilder();
        masked.append(data, 0, start);
        for (int i = start; i < end; i++) {
            masked.append('*');
        }
        masked.append(data.substring(end));
        return masked.toString();
    }
}
