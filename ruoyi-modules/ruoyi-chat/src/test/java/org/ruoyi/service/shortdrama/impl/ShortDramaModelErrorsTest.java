package org.ruoyi.service.shortdrama.impl;

import dev.langchain4j.exception.AuthenticationException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

@Tag("dev")
class ShortDramaModelErrorsTest {
    @Test void nestedAuthenticationErrorNamesAtlasWithoutExposingSdkOrVendorResponse() {
        var error = new CompletionException(new AuthenticationException("{\"code\":401,\"msg\":\"unauthorized\"}"));
        assertEquals("Atlas 模型鉴权失败，请检查后台该模型的 API Key、接口地址和账号权限", ShortDramaModelErrors.message(error));
    }
    @Test void otherGenerationErrorsKeepTheOriginalActionableMessage() {
        assertEquals("正文格式不完整", ShortDramaModelErrors.message(new IllegalArgumentException("正文格式不完整")));
    }
}
