package me.zhyd.oauth.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xkcoding.http.support.HttpHeader;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthDefaultSource;
import me.zhyd.oauth.enums.scope.AuthDingTalkScope;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.utils.AuthScopeUtils;
import me.zhyd.oauth.utils.GlobalAuthUtils;
import me.zhyd.oauth.utils.HttpUtils;
import me.zhyd.oauth.utils.UrlBuilder;

import java.util.HashMap;
import java.util.Map;

/**
 * 新版钉钉二维码登录
 *
 * @author yadong.zhang (yadong.zhang0415(a)gmail.com)
 * @since 1.16.7
 */
public class AuthDingTalkV2Request extends AuthDefaultRequest {

    public AuthDingTalkV2Request(AuthConfig config) {
        super(config, AuthDefaultSource.DINGTALK_V2);
    }

    public AuthDingTalkV2Request(AuthConfig config, AuthStateCache authStateCache) {
        super(config, AuthDefaultSource.DINGTALK_V2, authStateCache);
    }

    @Override
    public String authorize(String state) {
        return UrlBuilder.fromBaseUrl(source.authorize())
            .queryParam("response_type", "code")
            .queryParam("client_id", config.getClientId())
            .queryParam("scope", this.getScopes(",", true, AuthScopeUtils.getDefaultScopes(AuthDingTalkScope.values())))
            .queryParam("redirect_uri", GlobalAuthUtils.urlEncode(config.getRedirectUri()))
            .queryParam("prompt", "consent")
            .queryParam("org_type", config.getDingTalkOrgType())
            .queryParam("corpId", config.getDingTalkCorpId())
            .queryParam("exclusiveLogin", config.isDingTalkExclusiveLogin())
            .queryParam("exclusiveCorpId", config.getDingTalkExclusiveCorpId())
            .queryParam("state", getRealState(state))
            .build();
    }

    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, String> params = new HashMap<>();
            params.put("grantType", "authorization_code");
            params.put("clientId", config.getClientId());
            params.put("clientSecret", config.getClientSecret());
            params.put("code", authCallback.getCode());

            String paramsJson = objectMapper.writeValueAsString(params);
            String response = new HttpUtils(config.getHttpConfig()).post(this.source.accessToken(), paramsJson).getBody();
            JsonNode accessTokenObject = objectMapper.readTree(response);

            if (!accessTokenObject.has("accessToken")) {
                throw new AuthException(response, source);
            }
            return AuthToken.builder()
                .accessToken(accessTokenObject.get("accessToken").asText())
                .refreshToken(accessTokenObject.has("refreshToken") ? accessTokenObject.get("refreshToken").asText() : null)
                .expireIn(accessTokenObject.has("expireIn") ? accessTokenObject.get("expireIn").asInt() : 0)
                .corpId(accessTokenObject.has("corpId") ? accessTokenObject.get("corpId").asText() : null)
                .build();
        } catch (Exception e) {
            throw new AuthException("获取AccessToken失败: " + e.getMessage(), source);
        }
    }

    @Override
    public AuthUser getUserInfo(AuthToken authToken) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            HttpHeader header = new HttpHeader();
            header.add("x-acs-dingtalk-access-token", authToken.getAccessToken());

            String response = new HttpUtils(config.getHttpConfig()).get(this.source.userInfo(), null, header, false).getBody();
            JsonNode object = objectMapper.readTree(response);

            authToken.setOpenId(object.has("openId") ? object.get("openId").asText() : null);
            authToken.setUnionId(object.has("unionId") ? object.get("unionId").asText() : null);
            // rawUserInfo 为 JustAuth 的 fastjson 类型字段, 项目内无消费方, 不再设置
            return AuthUser.builder()
                .uuid(object.has("unionId") ? object.get("unionId").asText() : null)
                .username(object.has("nick") ? object.get("nick").asText() : null)
                .nickname(object.has("nick") ? object.get("nick").asText() : null)
                .avatar(object.has("avatarUrl") ? object.get("avatarUrl").asText() : null)
                .snapshotUser(object.has("visitor") && object.get("visitor").asBoolean())
                .token(authToken)
                .source(source.toString())
                .build();
        } catch (Exception e) {
            throw new AuthException("获取用户信息失败: " + e.getMessage(), source);
        }
    }

    /**
     * 返回获取accessToken的url
     *
     * @param code 授权码
     * @return 返回获取accessToken的url
     */
    protected String accessTokenUrl(String code) {
        return UrlBuilder.fromBaseUrl(source.accessToken())
            .queryParam("code", code)
            .queryParam("clientId", config.getClientId())
            .queryParam("clientSecret", config.getClientSecret())
            .queryParam("grantType", "authorization_code")
            .build();
    }
}
