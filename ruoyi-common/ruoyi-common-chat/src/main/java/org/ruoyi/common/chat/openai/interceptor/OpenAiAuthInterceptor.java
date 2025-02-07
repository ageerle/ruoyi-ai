package org.ruoyi.common.chat.openai.interceptor;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import lombok.Getter;
import lombok.Setter;
import okhttp3.Interceptor;
import okhttp3.Request;
import org.ruoyi.common.chat.openai.exception.CommonError;
import org.ruoyi.common.chat.openai.function.KeyStrategyFunction;
import org.ruoyi.common.core.exception.base.BaseException;

import java.util.List;
import java.util.Map;

public abstract class OpenAiAuthInterceptor implements Interceptor {


    /**
     * key 集合
     */
    @Getter
    @Setter
    private List<String> apiKey;
    /**
     * 自定义的key的使用策略
     */
    @Getter
    @Setter
    private KeyStrategyFunction<List<String>, String> keyStrategy;

    /**
     * 预警触发参数配置，配置参数实现飞书、钉钉、企业微信、邮箱预警等功能
     */
    @Getter
    @Setter
    private Map warringConfig;

    /**
     * 自定义apiKeys的处理逻辑
     *
     * @param errorKey 错误的key
     * @return 返回值是新的apiKeys
     */
    protected abstract List<String> onErrorDealApiKeys(String errorKey);

    /**
     * 所有的key都失效后，自定义预警配置
     * 可以通过warringConfig配置参数实现飞书、钉钉、企业微信、邮箱预警等
     */
    protected abstract void noHaveActiveKeyWarring();


    /**
     * 获取请求key
     *
     * @return key
     */
    public final String getKey() {
        if (CollectionUtil.isEmpty(apiKey)) {
            this.noHaveActiveKeyWarring();
            throw new BaseException(CommonError.NO_ACTIVE_API_KEYS.msg());
        }
        return keyStrategy.apply(apiKey);
    }

    /**
     * 默认的鉴权处理方法
     *
     * @param key      api key
     * @param original 源请求体
     * @return 请求体
     */
    public Request auth(String key, Request original) {
        Request request = original.newBuilder()
                .header(Header.AUTHORIZATION.getValue(), "Bearer " + key)
                .header(Header.CONTENT_TYPE.getValue(), ContentType.JSON.getValue())
                .method(original.method(), original.body())
                .build();
        return request;
    }
}
