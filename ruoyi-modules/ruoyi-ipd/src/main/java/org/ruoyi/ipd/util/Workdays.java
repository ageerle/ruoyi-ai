package org.ruoyi.ipd.util;

import java.util.Calendar;
import java.util.Date;

/**
 * 工作日推算（删除审核 F29：2+2 个工作日期限）
 * 规则：起始时刻起，逐日推进，跳过周六/周日，累计 N 个工作日后的同一时刻。
 */
public final class Workdays {

    private Workdays() {
    }

    public static Date add(Date from, int workdays) {
        Calendar c = Calendar.getInstance();
        c.setTime(from);
        int added = 0;
        while (added < workdays) {
            c.add(Calendar.DATE, 1);
            int dow = c.get(Calendar.DAY_OF_WEEK);
            if (dow != Calendar.SATURDAY && dow != Calendar.SUNDAY) {
                added++;
            }
        }
        return c.getTime();
    }
}