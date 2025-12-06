package org.ruoyi.common.chat.openai.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * 日志
 *
 * @author https:www.unfbx.com
 * 2023-02-28
 */
@Slf4j
public class OpenAILogger implements HttpLoggingInterceptor.Logger {
    @Override
    public void log(String message) {
        log.info("OkHttp-------->:{}", message);
    }
}
