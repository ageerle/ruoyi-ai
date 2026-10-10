package org.ruoyi.service.shortdrama.impl;

import dev.langchain4j.exception.AuthenticationException;

final class ShortDramaModelErrors {
    private ShortDramaModelErrors() {}
    static String message(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof AuthenticationException)
                return "Atlas 模型鉴权失败，请检查后台该模型的 API Key、接口地址和账号权限";
        }
        return failure.getMessage();
    }
}
