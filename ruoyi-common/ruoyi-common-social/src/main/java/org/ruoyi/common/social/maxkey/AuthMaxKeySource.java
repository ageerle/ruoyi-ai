package org.ruoyi.common.social.maxkey;

import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;

/**
 * Oauth2 默认接口说明
 *
 * @author 长春叭哥 2023年03月26日
 *
 */
public enum AuthMaxKeySource implements AuthSource {

    /**
     * 自己搭建的 maxkey 私服
     */
    MAXKEY {

        /**
         * 授权的api
         */
        @Override
        public String authorize() {
            return AuthMaxKeyRequest.SERVER_URL + "/sign/authz/oauth/v20/authorize";
        }

        /**
         * 获取accessToken的api
         */
        @Override
        public String accessToken() {
            return AuthMaxKeyRequest.SERVER_URL + "/sign/authz/oauth/v20/token";
        }

        /**
         * 获取用户信息的api
         */
        @Override
        public String userInfo() {
            return AuthMaxKeyRequest.SERVER_URL + "/sign/api/oauth/v20/me";
        }

        /**
         * 平台对应的 AuthRequest 实现类，必须继承自 {@link AuthDefaultRequest}
         */
        @Override
        public Class<? extends AuthDefaultRequest> getTargetClass() {
            return AuthMaxKeyRequest.class;
        }

    }
}
