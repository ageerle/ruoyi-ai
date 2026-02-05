package org.ruoyi.common.social.topiam;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.xkcoding.http.support.HttpHeader;
import lombok.extern.slf4j.Slf4j;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import me.zhyd.oauth.utils.HttpUtils;
import me.zhyd.oauth.utils.UrlBuilder;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.json.utils.JsonUtils;

import static org.ruoyi.common.social.topiam.AuthTopIamSource.TOPIAM;

/**
 * TopIAM 认证请求
 *
 * @author xlsea
 * @since 2024-01-06
 */
@Slf4j
public class AuthTopIamRequest extends AuthDefaultRequest {

    public static final String SERVER_URL = SpringUtils.getProperty("justauth.type.topiam.server-url");

    /**
     * 设定归属域
     */
    public AuthTopIamRequest(AuthConfig config) {
        super(config, TOPIAM);
    }

    public AuthTopIamRequest(AuthConfig config, AuthStateCache authStateCache) {
        super(config, TOPIAM, authStateCache);
    }

    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        String body = doPostAuthorizationCode(authCallback.getCode());
        Dict object = JsonUtils.parseMap(body);
        checkResponse(object);
        return AuthToken.builder()
            .accessToken(object.getStr("access_token"))
            .refreshToken(object.getStr("refresh_token"))
            .idToken(object.getStr("id_token"))
            .tokenType(object.getStr("token_type"))
            .scope(object.getStr("scope"))
            .build();
    }

    @Override
    public AuthUser getUserInfo(AuthToken authToken) {
        String body = doGetUserInfo(authToken);
        Dict object = JsonUtils.parseMap(body);
        checkResponse(object);
        return AuthUser.builder()
            .uuid(object.getStr("sub"))
            .username(object.getStr("preferred_username"))
            .nickname(object.getStr("nickname"))
            .avatar(object.getStr("picture"))
            .email(object.getStr("email"))
            .token(authToken)
            .source(source.toString())
            .build();
    }

    @Override
    protected String doPostAuthorizationCode(String code) {
        HttpRequest request = HttpRequest.post(source.accessToken())
            .header("Authorization", "Basic " + Base64.encode("%s:%s".formatted(config.getClientId(), config.getClientSecret())))
            .form("grant_type", "authorization_code")
            .form("code", code)
            .form("redirect_uri", config.getRedirectUri());
        HttpResponse response = request.execute();
        return response.body();
    }

    @Override
    protected String doGetUserInfo(AuthToken authToken) {
        return new HttpUtils(config.getHttpConfig()).get(source.userInfo(), null, new HttpHeader()
            .add("Content-Type", "application/json")
            .add("Authorization", "Bearer " + authToken.getAccessToken()), false).getBody();
    }


    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(super.authorize(state))
            .queryParam("scope", StrUtil.join("%20", config.getScopes()))
            .build();
    }

    private static void checkResponse(Dict object) {
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getStr("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getStr("message"));
        }
    }

}
