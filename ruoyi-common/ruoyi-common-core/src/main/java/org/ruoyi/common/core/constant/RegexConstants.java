package org.ruoyi.common.core.constant;

import cn.hutool.core.lang.RegexPool;

/**
 * 常用正则表达式字符串
 * <p>
 * 常用正则表达式集合，更多正则见: https://any86.github.io/any-rule/
 *
 * @author Feng
 */
public interface RegexConstants extends RegexPool {

    /**
     * 字典类型必须以字母开头，且只能为（小写字母，数字，下滑线）
     */
    String DICTIONARY_TYPE = "^[a-z][a-z0-9_]*$";

    /**
     * 权限标识必须符合以下格式：
     * 1. 标准格式：xxx:yyy:zzz
     * - 第一部分（xxx）：只能包含字母、数字和下划线（_），不能使用 `*`
     * - 第二部分（yyy）：可以包含字母、数字、下划线（_）和 `*`
     * - 第三部分（zzz）：可以包含字母、数字、下划线（_）和 `*`
     * 2. 允许空字符串（""），表示没有权限标识
     */
    String PERMISSION_STRING = "^$|^[a-zA-Z0-9_]+:[a-zA-Z0-9_*]+:[a-zA-Z0-9_*]+$";

    /**
     * 身份证号码（后6位）
     */
    String ID_CARD_LAST_6 = "^(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";

    /**
     * QQ号码
     */
    String QQ_NUMBER = "^[1-9][0-9]\\d{4,9}$";

    /**
     * 邮政编码
     */
    String POSTAL_CODE = "^[1-9]\\d{5}$";

    /**
     * 注册账号
     */
    String ACCOUNT = "^[a-zA-Z][a-zA-Z0-9_]{4,15}$";

    /**
     * 密码：包含至少8个字符，包括大写字母、小写字母、数字和特殊字符
     */
    String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    /**
     * 通用状态（0表示正常，1表示停用）
     */
    String STATUS = "^[01]$";

}
