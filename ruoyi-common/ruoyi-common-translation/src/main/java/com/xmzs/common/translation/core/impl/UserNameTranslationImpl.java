package com.xmzs.common.translation.core.impl;

import com.xmzs.common.core.service.UserService;
import com.xmzs.common.translation.annotation.TranslationType;
import com.xmzs.common.translation.constant.TransConstant;
import com.xmzs.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * 用户名翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.USER_ID_TO_NAME)
public class UserNameTranslationImpl implements TranslationInterface<String> {

    private final UserService userService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof Long id) {
            return userService.selectUserNameById(id);
        }
        return null;
    }
}
