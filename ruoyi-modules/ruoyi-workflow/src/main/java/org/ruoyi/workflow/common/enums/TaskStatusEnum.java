package org.ruoyi.workflow.common.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 任务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum TaskStatusEnum {

    /**
     * 撤销
     */
    CANCEL("cancel", "撤销"),

    /**
     * 通过
     */
    PASS("pass", "通过"),

    /**
     * 待审核
     */
    WAITING("waiting", "待审核"),

    /**
     * 作废
     */
    INVALID("invalid", "作废"),

    /**
     * 退回
     */
    BACK("back", "退回"),

    /**
     * 终止
     */
    TERMINATION("termination", "终止"),

    /**
     * 转办
     */
    TRANSFER("transfer", "转办"),

    /**
     * 委托
     */
    DEPUTE("depute", "委托"),

    /**
     * 抄送
     */
    COPY("copy", "抄送"),

    /**
     * 加签
     */
    SIGN("sign", "加签"),

    /**
     * 减签
     */
    SIGN_OFF("sign_off", "减签"),

    /**
     * 超时
     */
    TIMEOUT("timeout", "超时");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    private static final Map<String, String> STATUS_DESC_MAP = Arrays.stream(values())
        .collect(Collectors.toConcurrentMap(TaskStatusEnum::getStatus, TaskStatusEnum::getDesc));

    /**
     * 任务业务状态
     *
     * @param status 状态
     */
    public static String findByStatus(String status) {
        // 从缓存中直接获取描述
        return STATUS_DESC_MAP.getOrDefault(status, StrUtil.EMPTY);
    }

    /**
     * 判断状态是否为通过或退回
     *
     * @param status 状态值
     * @return true 表示是通过或退回状态
     */
    public static boolean isPassOrBack(String status) {
        return PASS.getStatus().equals(status) || BACK.getStatus().equals(status);
    }

}

