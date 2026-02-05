package org.ruoyi.common.core.enums;

import cn.hutool.core.util.StrUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.utils.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 业务状态枚举
 *
 * @author may
 */
@Getter
@AllArgsConstructor
public enum BusinessStatusEnum {

    /**
     * 已撤销
     */
    CANCEL("cancel", "已撤销"),

    /**
     * 草稿
     */
    DRAFT("draft", "草稿"),

    /**
     * 待审核
     */
    WAITING("waiting", "待审核"),

    /**
     * 已完成
     */
    FINISH("finish", "已完成"),

    /**
     * 已作废
     */
    INVALID("invalid", "已作废"),

    /**
     * 已退回
     */
    BACK("back", "已退回"),

    /**
     * 已终止
     */
    TERMINATION("termination", "已终止");

    /**
     * 状态
     */
    private final String status;

    /**
     * 描述
     */
    private final String desc;

    private static final Map<String, BusinessStatusEnum> STATUS_MAP = Arrays.stream(BusinessStatusEnum.values())
        .collect(Collectors.toConcurrentMap(BusinessStatusEnum::getStatus, Function.identity()));

    /**
     * 根据状态获取对应的 BusinessStatusEnum 枚举
     *
     * @param status 业务状态码
     * @return 对应的 BusinessStatusEnum 枚举，如果找不到则返回 null
     */
    public static BusinessStatusEnum getByStatus(String status) {
        // 使用 STATUS_MAP 获取对应的枚举，若找不到则返回 null
        return STATUS_MAP.get(status);
    }

    /**
     * 根据状态获取对应的业务状态描述信息
     *
     * @param status 业务状态码
     * @return 返回业务状态描述，若状态码为空或未找到对应的枚举，返回空字符串
     */
    public static String findByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return StrUtil.EMPTY;
        }
        BusinessStatusEnum statusEnum = STATUS_MAP.get(status);
        return (statusEnum != null) ? statusEnum.getDesc() : StrUtil.EMPTY;
    }

    /**
     * 判断是否为指定的状态之一：草稿、已撤销或已退回
     *
     * @param status 要检查的状态
     * @return 如果状态为草稿、已撤销或已退回之一，则返回 true；否则返回 false
     */
    public static boolean isDraftOrCancelOrBack(String status) {
        return DRAFT.status.equals(status) || CANCEL.status.equals(status) || BACK.status.equals(status);
    }

    /**
     * 判断是否为撤销，退回，作废，终止
     *
     * @param status status
     * @return 结果
     */
    public static boolean initialState(String status) {
        return CANCEL.status.equals(status) || BACK.status.equals(status) || INVALID.status.equals(status) || TERMINATION.status.equals(status);
    }

    /**
     * 获取运行中的实例状态列表
     *
     * @return 包含运行中实例状态的不可变列表
     * （包含 DRAFT、WAITING、BACK 和 CANCEL 状态）
     */
    public static List<String> runningStatus() {
        return Arrays.asList(DRAFT.status, WAITING.status, BACK.status, CANCEL.status);
    }

    /**
     * 获取结束实例的状态列表
     *
     * @return 包含结束实例状态的不可变列表
     * （包含 FINISH、INVALID 和 TERMINATION 状态）
     */
    public static List<String> finishStatus() {
        return Arrays.asList(FINISH.status, INVALID.status, TERMINATION.status);
    }

    /**
     * 启动流程校验
     *
     * @param status 状态
     */
    public static void checkStartStatus(String status) {
        if (WAITING.getStatus().equals(status)) {
            throw new ServiceException("该单据已提交过申请,正在审批中！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 撤销流程校验
     *
     * @param status 状态
     */
    public static void checkCancelStatus(String status) {
        if (CANCEL.getStatus().equals(status)) {
            throw new ServiceException("该单据已撤销！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (BACK.getStatus().equals(status)) {
            throw new ServiceException("该单据已退回！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 驳回流程校验
     *
     * @param status 状态
     */
    public static void checkBackStatus(String status) {
        if (BACK.getStatus().equals(status)) {
            throw new ServiceException("该单据已退回！");
        } else if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (CANCEL.getStatus().equals(status)) {
            throw new ServiceException("该单据已撤销！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

    /**
     * 作废,终止流程校验
     *
     * @param status 状态
     */
    public static void checkInvalidStatus(String status) {
        if (FINISH.getStatus().equals(status)) {
            throw new ServiceException("该单据已完成申请！");
        } else if (INVALID.getStatus().equals(status)) {
            throw new ServiceException("该单据已作废！");
        } else if (TERMINATION.getStatus().equals(status)) {
            throw new ServiceException("该单据已终止！");
        } else if (StringUtils.isBlank(status)) {
            throw new ServiceException("流程状态为空！");
        }
    }

}
