package org.ruoyi.common.social.gitea;

import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;

/**
 * gitea Oauth2 默认接口说明
 *
 * @author lcry
 */
public enum AuthGiteaSource implements AuthSource {

    /**
     * 自己搭建的 gitea 私服
     */
    GITEA {
        /**
         * 授权的api
         */
        @Override
        public String authorize() {
            return AuthGiteaRequest.SERVER_URL + "/login/oauth/authorize";
        }

        /**
         * 获取accessToken的api
         */
        @Override
        public String accessToken() {
            return AuthGiteaRequest.SERVER_URL + "/login/oauth/access_token";
        }

        /**
         * 获取用户信息的api
         */
        @Override
        public String userInfo() {
            return AuthGiteaRequest.SERVER_URL + "/login/oauth/userinfo";
        }

        /**
         * 平台对应的 AuthRequest 实现类，必须继承自 {@link AuthDefaultRequest}
         */
        @Override
        public Class<? extends AuthDefaultRequest> getTargetClass() {
            return AuthGiteaRequest.class;
        }

    }
}
