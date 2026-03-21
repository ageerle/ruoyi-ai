package org.ruoyi.common.core.service;

import org.ruoyi.common.core.domain.dto.VisitorLoginUserDto;

public interface UserLoginService {

    /**
     * 通过openid获取登录信息
     *
     * @param openid    微信openid
     * @param clientId 客户端id
     * @return
     */
     VisitorLoginUserDto mpLogin(String openid, String clientId);

}
