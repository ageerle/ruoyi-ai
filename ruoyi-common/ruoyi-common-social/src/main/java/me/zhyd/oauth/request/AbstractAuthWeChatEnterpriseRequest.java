package me.zhyd.oauth.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import me.zhyd.oauth.cache.AuthStateCache;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.enums.AuthResponseStatus;
import me.zhyd.oauth.enums.AuthUserGender;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthToken;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.utils.HttpUtils;
import me.zhyd.oauth.utils.StringUtils;
import me.zhyd.oauth.utils.UrlBuilder;

/**
 * <p>
 * 企业微信登录父类
 * </p>
 *
 * @author liguanhua (347826496(a)qq.com)
 * @since 1.15.9
 */
public abstract class AbstractAuthWeChatEnterpriseRequest extends AuthDefaultRequest {

    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source) {
        super(config,source);
    }


    public AbstractAuthWeChatEnterpriseRequest(AuthConfig config, AuthSource source, AuthStateCache authStateCache) {
        super(config, source, authStateCache);
    }

    @Override
    public AuthToken getAccessToken(AuthCallback authCallback) {
        String response = doGetAuthorizationCode(accessTokenUrl(null));

        JsonNode object = this.checkResponse(response);

        return AuthToken.builder()
            .accessToken(object.get("access_token").asText())
            .expireIn(object.get("expires_in").asInt())
            .code(authCallback.getCode())
            .build();
    }

    @Override
    public AuthUser getUserInfo(AuthToken authToken) {
        String response = doGetUserInfo(authToken);
        JsonNode object = this.checkResponse(response);

        // 返回 OpenId 或其他，均代表非当前企业用户，不支持
        // https://github.com/justauth/JustAuth/issues/227 修复bug
        if (!object.has("userid")) {
            throw new AuthException(AuthResponseStatus.UNIDENTIFIED_PLATFORM, source);
        }
        String userId = object.get("userid").asText();
        String userTicket = object.has("user_ticket") ? object.get("user_ticket").asText() : null;
        JsonNode userDetail = getUserDetail(authToken.getAccessToken(), userId, userTicket);

        // rawUserInfo 为 JustAuth 的 fastjson 类型字段, 项目内无消费方, 不再设置
        return AuthUser.builder()
            .username(userDetail.has("name") ? userDetail.get("name").asText() : null)
            .nickname(userDetail.has("alias") ? userDetail.get("alias").asText() : null)
            .avatar(userDetail.has("avatar") ? userDetail.get("avatar").asText() : null)
            .location(userDetail.has("address") ? userDetail.get("address").asText() : null)
            .email(userDetail.has("email") ? userDetail.get("email").asText() : null)
            .uuid(userId)
            .gender(AuthUserGender.getWechatRealGender(userDetail.has("gender") ? userDetail.get("gender").asText() : null))
            .token(authToken)
            .source(source.toString())
            .build();
    }

    /**
     * 校验请求结果
     *
     * @param response 请求结果
     * @return 如果请求结果正常，则返回JsonNode
     */
    private JsonNode checkResponse(String response) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode object = objectMapper.readTree(response);

            if (object.has("errcode") && object.get("errcode").asInt() != 0) {
                throw new AuthException(object.get("errmsg").asText(), source);
            }

            return object;
        } catch (Exception e) {
            throw new AuthException("解析响应失败: " + e.getMessage(), source);
        }
    }


    /**
     * 返回获取accessToken的url
     *
     * @param code 授权码
     * @return 返回获取accessToken的url
     */
    @Override
    protected String accessTokenUrl(String code) {
        return UrlBuilder.fromBaseUrl(source.accessToken())
            .queryParam("corpid", config.getClientId())
            .queryParam("corpsecret", config.getClientSecret())
            .build();
    }

    /**
     * 返回获取userInfo的url
     *
     * @param authToken 用户授权后的token
     * @return 返回获取userInfo的url
     */
    @Override
    protected String userInfoUrl(AuthToken authToken) {
        return UrlBuilder.fromBaseUrl(source.userInfo())
            .queryParam("access_token", authToken.getAccessToken())
            .queryParam("code", authToken.getCode())
            .build();
    }

    /**
     * 用户详情
     *
     * @param accessToken accessToken
     * @param userId      企业内用户id
     * @param userTicket  成员票据，用于获取用户信息或敏感信息
     * @return 用户详情
     */
    private JsonNode getUserDetail(String accessToken, String userId, String userTicket) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // 用户基础信息
            String userInfoUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/user/get")
                .queryParam("access_token", accessToken)
                .queryParam("userid", userId)
                .build();
            String userInfoResponse = new HttpUtils(config.getHttpConfig()).get(userInfoUrl).getBody();
            JsonNode userInfo = checkResponse(userInfoResponse);

            // 用户敏感信息
            if (StringUtils.isNotEmpty(userTicket)) {
                String userDetailUrl = UrlBuilder.fromBaseUrl("https://qyapi.weixin.qq.com/cgi-bin/auth/getuserdetail")
                    .queryParam("access_token", accessToken)
                    .build();

                // 构建请求参数
                String paramJson = objectMapper.createObjectNode()
                    .put("user_ticket", userTicket)
                    .toString();

                String userDetailResponse = new HttpUtils(config.getHttpConfig()).post(userDetailUrl, paramJson).getBody();
                JsonNode userDetail = checkResponse(userDetailResponse);

                // 合并两个JsonNode
                ((com.fasterxml.jackson.databind.node.ObjectNode) userInfo).setAll((com.fasterxml.jackson.databind.node.ObjectNode) userDetail);
            }
            return userInfo;
        } catch (Exception e) {
            throw new AuthException("获取用户详情失败: " + e.getMessage(), source);
        }
    }

}
