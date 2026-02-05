package org.ruoyi.common.core.utils.regex;

import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.core.lang.Validator;
import org.ruoyi.common.core.factory.RegexPatternPoolFactory;

import java.util.regex.Pattern;

/**
 * 正则字段校验器
 * 主要验证字段非空、是否为满足指定格式等
 *
 * @author Feng
 */
public class RegexValidator extends Validator {

    /**
     * 字典类型必须以字母开头，且只能为（小写字母，数字，下滑线）
     */
    public static final Pattern DICTIONARY_TYPE = RegexPatternPoolFactory.DICTIONARY_TYPE;

    /**
     * 身份证号码（后6位）
     */
    public static final Pattern ID_CARD_LAST_6 = RegexPatternPoolFactory.ID_CARD_LAST_6;

    /**
     * QQ号码
     */
    public static final Pattern QQ_NUMBER = RegexPatternPoolFactory.QQ_NUMBER;

    /**
     * 邮政编码
     */
    public static final Pattern POSTAL_CODE = RegexPatternPoolFactory.POSTAL_CODE;

    /**
     * 注册账号
     */
    public static final Pattern ACCOUNT = RegexPatternPoolFactory.ACCOUNT;

    /**
     * 密码：包含至少8个字符，包括大写字母、小写字母、数字和特殊字符
     */
    public static final Pattern PASSWORD = RegexPatternPoolFactory.PASSWORD;

    /**
     * 通用状态（0表示正常，1表示停用）
     */
    public static final Pattern STATUS = RegexPatternPoolFactory.STATUS;


    /**
     * 检查输入的账号是否匹配预定义的规则
     *
     * @param value 要验证的账号
     * @return 如果账号符合规则，返回 true；否则，返回 false。
     */
    public static boolean isAccount(CharSequence value) {
        return isMatchRegex(ACCOUNT, value);
    }

    /**
     * 验证输入的账号是否符合规则，如果不符合，则抛出 ValidateException 异常
     *
     * @param value    要验证的账号
     * @param errorMsg 验证失败时抛出的异常消息
     * @param <T>      CharSequence 的子类型
     * @return 如果验证通过，返回输入的账号
     * @throws ValidateException 如果验证失败
     */
    public static <T extends CharSequence> T validateAccount(T value, String errorMsg) throws ValidateException {
        if (!isAccount(value)) {
            throw new ValidateException(errorMsg);
        }
        return value;
    }

    /**
     * 检查输入的状态是否匹配预定义的规则
     *
     * @param value 要验证的状态
     * @return 如果状态符合规则，返回 true；否则，返回 false。
     */
    public static boolean isStatus(CharSequence value) {
        return isMatchRegex(STATUS, value);
    }

    /**
     * 验证输入的状态是否符合规则，如果不符合，则抛出 ValidateException 异常
     *
     * @param value    要验证的状态
     * @param errorMsg 验证失败时抛出的异常消息
     * @param <T>      CharSequence 的子类型
     * @return 如果验证通过，返回输入的状态
     * @throws ValidateException 如果验证失败
     */
    public static <T extends CharSequence> T validateStatus(T value, String errorMsg) throws ValidateException {
        if (!isStatus(value)) {
            throw new ValidateException(errorMsg);
        }
        return value;
    }

}
