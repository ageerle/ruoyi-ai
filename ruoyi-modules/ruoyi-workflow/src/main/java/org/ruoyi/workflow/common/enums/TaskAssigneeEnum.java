package org.ruoyi.workflow.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务分配人枚举
 *
 * @author AprilWind
 */
@Getter
@AllArgsConstructor
public enum TaskAssigneeEnum {

    /**
     * 用户
     */
    USER("用户", ""),

    /**
     * 角色
     */
    ROLE("角色", "role:"),

    /**
     * 部门
     */
    DEPT("部门", "dept:"),

    /**
     * 岗位
     */
    POST("岗位", "post:"),

    /**
     * SPEL表达式
     */
    SPEL("SpEL表达式", "");

    private final String desc;
    private final String code;

    /**
     * 根据描述获取对应的枚举类型
     * <p>
     * 通过传入描述，查找并返回匹配的枚举项。如果未找到匹配项，会抛出 {@link ServiceException}。
     * </p>
     *
     * @param desc 描述，用于匹配对应的枚举项
     * @return TaskAssigneeEnum 返回对应的枚举类型
     * @throws ServiceException 如果未找到匹配的枚举项
     */
    public static TaskAssigneeEnum fromDesc(String desc) {
        for (TaskAssigneeEnum type : values()) {
            if (type.getDesc().equals(desc)) {
                return type;
            }
        }
        throw new ServiceException("未知的办理人类型: " + desc);
    }

    /**
     * 根据代码获取对应的枚举类型
     * <p>
     * 通过传入代码，查找并返回匹配的枚举项。如果未找到匹配项，会抛出 {@link ServiceException}。
     * </p>
     *
     * @param code 代码，用于匹配对应的枚举项
     * @return TaskAssigneeEnum 返回对应的枚举类型
     * @throws IllegalArgumentException 如果未找到匹配的枚举项
     */
    public static TaskAssigneeEnum fromCode(String code) {
        for (TaskAssigneeEnum type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new ServiceException("未知的办理人类型代码: " + code);
    }

    /**
     * 获取所有办理人类型的描述列表
     * <p>
     * 获取当前枚举类所有项的描述字段列表，通常用于展示选择项。
     * </p>
     *
     * @return List<String> 返回所有办理人类型的描述列表
     */
    public static List<String> getAssigneeTypeList() {
        return Arrays.stream(values())
            .map(TaskAssigneeEnum::getDesc)
            .collect(Collectors.toList());
    }

    /**
     * 获取所有办理人类型的代码列表
     * <p>
     * 获取当前枚举类所有项的代码字段列表，通常用于程序内部逻辑的判断。
     * </p>
     *
     * @return List<String> 返回所有办理人类型的代码列表
     */
    public static List<String> getAssigneeCodeList() {
        return Arrays.stream(values())
            .map(TaskAssigneeEnum::getCode)
            .collect(Collectors.toList());
    }

    /**
     * 判断当前办理人类型是否需要调用部门服务（deptService）
     *
     * @return 如果类型是 USER、DEPT 或 POST，则返回 true；否则返回 false
     */
    public boolean needsDeptService() {
        return this == USER || this == DEPT || this == POST;
    }

    /**
     * 判断给定字符串是否符合 SPEL 表达式格式（以 $ 或 # 开头）
     *
     * @param value 待判断字符串
     * @return 是否为 SPEL 表达式
     */
    public static boolean isSpelExpression(String value) {
        if (value == null) {
            return false;
        }
        // $前缀表示默认办理人变量策略
        // #前缀表示spel办理人变量策略
        return StringUtils.startsWith(value, "$") || StringUtils.startsWith(value, "#");
    }

}

