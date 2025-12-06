package org.ruoyi.common.chat.openai.interceptor;

import cn.hutool.json.JSONUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;
import org.ruoyi.common.chat.entity.common.OpenAiResponse;
import org.ruoyi.common.chat.openai.exception.CommonError;
import org.ruoyi.common.core.exception.base.BaseException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 动态处理key的鉴权拦截器
 *
 * @author https:www.unfbx.com
 * @since 2023-04-25
 */
@Getter
@Slf4j
public class DynamicKeyOpenAiAuthInterceptor extends OpenAiAuthInterceptor {
    /**
     * 账号被封了
     */
    private static final String ACCOUNT_DEACTIVATED = "account_deactivated";
    /**
     * key不正确
     */
    private static final String INVALID_API_KEY = "invalid_api_key";

    /**
     * 请求头处理
     */
    public DynamicKeyOpenAiAuthInterceptor() {
        this.setWarringConfig(null);
    }

    /**
     * 构造方法
     *
     * @param warringConfig 所有的key都失效后的告警参数配置
     */
    public DynamicKeyOpenAiAuthInterceptor(Map warringConfig) {
        this.setWarringConfig(warringConfig);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        String key = getKey();
        Request original = chain.request();
        Request request = this.auth(key, original);
        Response response = chain.proceed(request);
        if (!response.isSuccessful()) {
            String errorMsg = response.body().string();
            if (response.code() == CommonError.OPENAI_AUTHENTICATION_ERROR.code()
                    || response.code() == CommonError.OPENAI_LIMIT_ERROR.code()
                    || response.code() == CommonError.OPENAI_SERVER_ERROR.code()) {
                OpenAiResponse openAiResponse = JSONUtil.toBean(errorMsg, OpenAiResponse.class);
                String errorCode = openAiResponse.getError().getCode();
                log.error("--------> 请求openai异常，错误code：{}", errorCode);
                log.error("--------> 请求异常：{}", errorMsg);
                //账号被封或者key不正确就移除掉
                if (ACCOUNT_DEACTIVATED.equals(errorCode) || INVALID_API_KEY.equals(errorCode)) {
                    super.setApiKey(this.onErrorDealApiKeys(key));
                }
                throw new BaseException(openAiResponse.getError().getMessage());
            }
            //非官方定义的错误code
            log.error("--------> 请求异常：{}", errorMsg);
            OpenAiResponse openAiResponse = JSONUtil.toBean(errorMsg, OpenAiResponse.class);
            if (Objects.nonNull(openAiResponse.getError())) {
                log.error(openAiResponse.getError().getMessage());
                throw new BaseException(openAiResponse.getError().getMessage());
            }
            throw new BaseException(CommonError.RETRY_ERROR.msg());
        }
        return response;
    }


    @Override
    protected List<String> onErrorDealApiKeys(String errorKey) {
        List<String> apiKey = super.getApiKey().stream().filter(e -> !errorKey.equals(e)).collect(Collectors.toList());
        log.error("--------> 当前ApiKey：[{}] 失效了，移除！", errorKey);
        return apiKey;
    }

    /**
     * 所有的key都失效后，自定义预警配置
     * 不配置直接return
     */
    @Override
    protected void noHaveActiveKeyWarring() {
        log.error("--------> [告警] 没有可用的key！！！");
        return;
    }

    @Override
    public Request auth(String key, Request original) {
        return super.auth(key, original);
    }
}
