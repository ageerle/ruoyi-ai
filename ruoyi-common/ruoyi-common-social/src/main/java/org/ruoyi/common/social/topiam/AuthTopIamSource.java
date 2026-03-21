package org.ruoyi.common.social.topiam;

import me.zhyd.oauth.config.AuthSource;
import me.zhyd.oauth.request.AuthDefaultRequest;

/**
 * Oauth2 默认接口说明
 *
 * @author xlsea
 * @since 2024-01-06
 */
public enum AuthTopIamSource implements AuthSource {

    /**
     * 测试
     */
    TOPIAM {
        /**
         * 授权的api
         */
        @Override
        public String authorize() {
            return AuthTopIamRequest.SERVER_URL + "/oauth2/auth";
        }

        /**
         * 获取accessToken的api
         */
        @Override
        public String accessToken() {
            return AuthTopIamRequest.SERVER_URL + "/oauth2/token";
        }

        /**
         * 获取用户信息的api
         */
        @Override
        public String userInfo() {
            return AuthTopIamRequest.SERVER_URL + "/oauth2/userinfo";
        }

        /**
         * 平台对应的 AuthRequest 实现类，必须继承自 {@link AuthDefaultRequest}
         */
        @Override
        public Class<? extends AuthDefaultRequest> getTargetClass() {
            return AuthTopIamRequest.class;
        }

    }
}
