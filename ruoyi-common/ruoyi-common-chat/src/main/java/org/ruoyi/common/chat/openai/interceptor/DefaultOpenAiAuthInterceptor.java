package org.ruoyi.common.chat.openai.interceptor;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 描述：请求增加header apikey
 *
 * @author https:www.unfbx.com
 * @since 2023-03-23
 */
@Slf4j
public class DefaultOpenAiAuthInterceptor extends OpenAiAuthInterceptor {
    /**
     * 请求头处理
     */
    public DefaultOpenAiAuthInterceptor() {
        super.setWarringConfig(null);
    }

    /**
     * 构造方法
     *
     * @param warringConfig 所有的key都失效后的告警参数配置
     */
    public DefaultOpenAiAuthInterceptor(Map warringConfig) {
        super.setWarringConfig(warringConfig);
    }

    /**
     * 拦截器鉴权
     *
     * @param chain Chain
     * @return Response对象
     * @throws IOException io异常
     */
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request original = chain.request();
        return chain.proceed(auth(super.getKey(), original));
    }

    /**
     * key失效或者禁用后的处理逻辑
     * 默认不处理
     *
     * @param apiKey 返回新的api keys集合
     * @return 新的apiKey集合
     */
    @Override
    protected List<String> onErrorDealApiKeys(String apiKey) {
        return super.getApiKey();
    }

    @Override
    protected void noHaveActiveKeyWarring() {
        log.error("--------> [告警] 没有可用的key！！！");
        return;
    }
}
