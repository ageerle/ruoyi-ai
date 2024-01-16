package com.xmzs.demo.controller.queue;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实体类 注意不允许使用内部类 否则会找不到类
 *
 * @author Lion Li
 * @version 3.6.0
 */
@Data
@NoArgsConstructor
public class PriorityDemo implements Comparable<PriorityDemo> {
    private String name;
    private Integer orderNum;

    @Override
    public int compareTo(PriorityDemo other) {
        return Integer.compare(getOrderNum(), other.getOrderNum());
    }
}
