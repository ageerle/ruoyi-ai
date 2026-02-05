package org.ruoyi.workflow.util;

import java.util.UUID;

public class UuidUtil {
    private UuidUtil() {
    }

    public static String createShort() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
