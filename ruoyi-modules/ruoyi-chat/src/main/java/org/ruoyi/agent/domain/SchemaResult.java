package org.ruoyi.agent.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 表结构查询结果
 * 返回数据库的表结构信息
 */
@Data
public class SchemaResult {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息或成功消息
     */
    private String message;

    /**
     * 表结构列表
     */
    private List<TableStructure> tables;

    /**
     * 创建成功结果
     * @param tables 表结构列表
     * @return SchemaResult 对象
     */
    public static SchemaResult success(List<TableStructure> tables) {
        SchemaResult result = new SchemaResult();
        result.success = true;
        result.tables = tables != null ? tables : new ArrayList<>();
        result.message = "Schema retrieved successfully";
        return result;
    }

    /**
     * 创建失败结果
     * @param message 错误消息
     * @return SchemaResult 对象
     */
    public static SchemaResult error(String message) {
        SchemaResult result = new SchemaResult();
        result.success = false;
        result.message = message;
        result.tables = new ArrayList<>();
        return result;
    }
}
