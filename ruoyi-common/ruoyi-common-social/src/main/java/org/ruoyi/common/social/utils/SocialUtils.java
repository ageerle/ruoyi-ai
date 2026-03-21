package org.ruoyi.common.social.utils;

import cn.hutool.core.util.ObjectUtil;
import me.zhyd.oauth.config.AuthConfig;
import me.zhyd.oauth.exception.AuthException;
import me.zhyd.oauth.model.AuthCallback;
import me.zhyd.oauth.model.AuthResponse;
import me.zhyd.oauth.model.AuthUser;
import me.zhyd.oauth.request.*;
import org.ruoyi.common.core.utils.SpringUtils;
import org.ruoyi.common.social.config.properties.SocialLoginConfigProperties;
import org.ruoyi.common.social.config.properties.SocialProperties;
import org.ruoyi.common.social.gitea.AuthGiteaRequest;
import org.ruoyi.common.social.maxkey.AuthMaxKeyRequest;
import org.ruoyi.common.social.topiam.AuthTopIamRequest;

/**
 * 认证授权工具类
 *
 * @author thiszhc
 */
public class SocialUtils  {

    private static final AuthRedisStateCache STATE_CACHE = SpringUtils.getBean(AuthRedisStateCache.class);

    @SuppressWarnings("unchecked")
    public static AuthResponse<AuthUser> loginAuth(String source, String code, String state, SocialProperties socialProperties) throws AuthException {
        AuthRequest authRequest = getAuthRequest(source, socialProperties);
        AuthCallback callback = new AuthCallback();
        callback.setCode(code);
        callback.setState(state);
        return authRequest.login(callback);
    }

    public static AuthRequest getAuthRequest(String source, SocialProperties socialProperties) throws AuthException {
        SocialLoginConfigProperties obj = socialProperties.getType().get(source);
         if (ObjectUtil.isNull(obj)) {
            throw new AuthException("不支持的第三方登录类型");
        }
        AuthConfig.AuthConfigBuilder builder = AuthConfig.builder()
            .clientId(obj.getClientId())
            .clientSecret(obj.getClientSecret())
            .redirectUri(obj.getRedirectUri())
            .scopes(obj.getScopes());
        return switch (source.toLowerCase()) {
            case "dingtalk" -> new AuthDingTalkV2Request(builder.build(), STATE_CACHE);
            case "baidu" -> new AuthBaiduRequest(builder.build(), STATE_CACHE);
            case "github" -> new AuthGithubRequest(builder.build(), STATE_CACHE);
            case "gitee" -> new AuthGiteeRequest(builder.build(), STATE_CACHE);
            case "weibo" -> new AuthWeiboRequest(builder.build(), STATE_CACHE);
            case "coding" -> new AuthCodingRequest(builder.build(), STATE_CACHE);
            case "oschina" -> new AuthOschinaRequest(builder.build(), STATE_CACHE);
            // 支付宝在创建回调地址时，不允许使用localhost或者127.0.0.1，所以这儿的回调地址使用的局域网内的ip
            case "alipay_wallet" -> new AuthAlipayRequest(builder.build(), socialProperties.getType().get("alipay_wallet").getAlipayPublicKey(), STATE_CACHE);
            case "qq" -> new AuthQqRequest(builder.build(), STATE_CACHE);
            case "wechat_open" -> new AuthWeChatOpenRequest(builder.build(), STATE_CACHE);
            case "taobao" -> new AuthTaobaoRequest(builder.build(), STATE_CACHE);
            case "douyin" -> new AuthDouyinRequest(builder.build(), STATE_CACHE);
            case "linkedin" -> new AuthLinkedinRequest(builder.build(), STATE_CACHE);
            case "microsoft" -> new AuthMicrosoftRequest(builder.build(), STATE_CACHE);
            case "renren" -> new AuthRenrenRequest(builder.build(), STATE_CACHE);
            case "stack_overflow" -> new AuthStackOverflowRequest(builder.stackOverflowKey(obj.getStackOverflowKey()).build(), STATE_CACHE);
            case "huawei" -> new AuthHuaweiV3Request(builder.build(), STATE_CACHE);
            case "wechat_enterprise" -> new AuthWeChatEnterpriseQrcodeV2Request(builder.agentId(obj.getAgentId()).build(), STATE_CACHE);
            case "gitlab" -> new AuthGitlabRequest(builder.build(), STATE_CACHE);
            case "wechat_mp" -> new AuthWeChatMpRequest(builder.build(), STATE_CACHE);
            case "aliyun" -> new AuthAliyunRequest(builder.build(), STATE_CACHE);
            case "maxkey" -> new AuthMaxKeyRequest(builder.build(), STATE_CACHE);
            case "topiam" -> new AuthTopIamRequest(builder.build(), STATE_CACHE);
            case "gitea" -> new AuthGiteaRequest(builder.build(), STATE_CACHE);
            default -> throw new AuthException("未获取到有效的Auth配置");
        };
    }
}

