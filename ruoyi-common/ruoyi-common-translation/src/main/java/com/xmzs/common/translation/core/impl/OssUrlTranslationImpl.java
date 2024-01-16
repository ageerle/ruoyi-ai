package com.xmzs.common.translation.core.impl;

import com.xmzs.common.core.service.OssService;
import com.xmzs.common.translation.annotation.TranslationType;
import com.xmzs.common.translation.constant.TransConstant;
import com.xmzs.common.translation.core.TranslationInterface;
import lombok.AllArgsConstructor;

/**
 * OSS翻译实现
 *
 * @author Lion Li
 */
@AllArgsConstructor
@TranslationType(type = TransConstant.OSS_ID_TO_URL)
public class OssUrlTranslationImpl implements TranslationInterface<String> {

    private final OssService ossService;

    @Override
    public String translation(Object key, String other) {
        if (key instanceof String ids) {
            return ossService.selectUrlByIds(ids);
        } else if (key instanceof Long id) {
            return ossService.selectUrlByIds(id.toString());
        }
        return null;
    }
}
