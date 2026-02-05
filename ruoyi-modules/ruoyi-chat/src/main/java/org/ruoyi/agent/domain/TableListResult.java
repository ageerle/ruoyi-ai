package org.ruoyi.agent.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 表列表查询结果
 * 返回允许查询的表的名称列表
 */
@Data
public class TableListResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息或成功消息
     */
    private String message;

    /**
     * 表名列表
     */
    private List<String> tables;

    /**
     * 创建成功结果
     * @param tables 表名列表
     * @return TableListResult 对象
     */
    public static TableListResult success(List<String> tables) {
        TableListResult result = new TableListResult();
        result.success = true;
        result.tables = tables != null ? tables : new ArrayList<>();
        result.message = "Tables listed successfully";
        return result;
    }

    /**
     * 创建失败结果
     * @param message 错误消息
     * @return TableListResult 对象
     */
    public static TableListResult error(String message) {
        TableListResult result = new TableListResult();
        result.success = false;
        result.message = message;
        result.tables = new ArrayList<>();
        return result;
    }
}
