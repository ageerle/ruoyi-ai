package org.ruoyi.agent.domain;

import lombok.Data;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据库查询结果
 * 返回 SELECT 查询的结果
 */
@Data
public class Result {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 错误消息或成功消息
     */
    private String message;

    /**
     * 查询结果数据
     * 每个 Map 代表一行，key 是字段名，value 是字段值
     */
    private List<Map<String, Object>> data;

    /**
     * 创建成功结果
     * @param data 查询数据
     * @return Result 对象
     */
    public static Result success(List<Map<String, Object>> data) {
        Result result = new Result();
        result.success = true;
        result.data = data;
        result.message = "Query successful";
        return result;
    }

    /**
     * 创建失败结果
     * @param message 错误消息
     * @return Result 对象
     */
    public static Result error(String message) {
        Result result = new Result();
        result.success = false;
        result.message = message;
        result.data = new ArrayList<>();
        return result;
    }
}
