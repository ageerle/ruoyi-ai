package com.xmzs.common.core.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;

/**
 * Validator 校验框架工具
 *
 * @author Lion Li
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ValidatorUtils {

    private static final Validator VALID = SpringUtils.getBean(Validator.class);

    public static <T> void validate(T object, Class<?>... groups) {
        Set<ConstraintViolation<T>> validate = VALID.validate(object, groups);
        if (!validate.isEmpty()) {
            throw new ConstraintViolationException("参数校验异常", validate);
        }
    }

}
