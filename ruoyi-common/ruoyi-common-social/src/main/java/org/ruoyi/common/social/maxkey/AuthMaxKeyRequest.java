package org.ruoyi.common.social.maxkey;

import cn.hutool.core.lang.Dict;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.AuthDefaultRequest;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.json.utils.JsonUtils;

/**
 *  @author 长春叭哥 2023年03月26日
 */
public class AuthMaxKeyRequest extends AuthDefaultRequest {

    public static final String SERVER_URL = SpringUtils.getProperty("justauth.type.maxkey.server-url");

    /**
     * 设定归属域
     */
    public AuthMaxKeyRequest(AuthConfig config) {
        super(config, AuthMaxKeySource.MAXKEY);
    }

    public AuthMaxKeyRequest(AuthConfig config, AuthStateCache authStateCache) {
        super(config, AuthMaxKeySource.MAXKEY, authStateCache);
    }

    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        String body = doPostAuthorizationCode(authCallback.getCode());
        Dict object = JsonUtils.parseMap(body);
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getStr("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getStr("message"));
        }
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
        // oauth/token 验证异常
        if (object.containsKey("error")) {
            throw new AuthException(object.getStr("error_description"));
        }
        // user 验证异常
        if (object.containsKey("message")) {
            throw new AuthException(object.getStr("message"));
        }
        return AuthUser.builder()
            .uuid(object.getStr("userId"))
            .username(object.getStr("username"))
            .nickname(object.getStr("displayName"))
            .avatar(object.getStr("avatar_url"))
            .blog(object.getStr("web_url"))
            .company(object.getStr("organization"))
            .location(object.getStr("location"))
            .email(object.getStr("email"))
            .remark(object.getStr("bio"))
            .token(authToken)
            .source(source.toString())
            .build();
    }

}
