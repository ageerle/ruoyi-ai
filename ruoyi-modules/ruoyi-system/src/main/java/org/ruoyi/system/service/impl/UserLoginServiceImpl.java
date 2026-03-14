package org.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.stp.parameter.SaLoginParameter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.crypto.digest.BCrypt;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;
import org.ruoyi.common.core.constant.Constants;
import org.ruoyi.common.core.constant.SystemConstants;
import org.ruoyi.common.core.domain.dto.VisitorLoginUserDto;
import org.ruoyi.common.core.enums.UserType;
import org.ruoyi.common.core.exception.ServiceException;
import org.ruoyi.common.core.service.UserLoginService;
import org.ruoyi.common.core.utils.MessageUtils;
import org.ruoyi.common.satoken.utils.LoginHelper;
import org.ruoyi.system.domain.bo.SysUserBo;
import org.ruoyi.system.domain.vo.SysClientVo;
import org.ruoyi.system.domain.vo.SysUserVo;
import org.ruoyi.system.service.ISysClientService;
import org.ruoyi.system.service.SysLoginService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserLoginServiceImpl implements UserLoginService {

    private final SysUserServiceImpl userService;

    private final ISysClientService clientService;

    private final SysLoginService sysLoginService;


    public VisitorLoginUserDto mpLogin(String openid, String clientId) {
        // 校验客户端
        SysClientVo client = clientService.queryByClientId(clientId);
        if (ObjectUtil.isNull(client)) {
            throw new ServiceException(MessageUtils.message("auth.grant.type.error"));
        } else if (!SystemConstants.NORMAL.equals(client.getStatus())) {
            throw new ServiceException(MessageUtils.message("auth.grant.type.blocked"));
        }
        // 使用 openid 查询绑定用户 如未绑定用户 则根据业务自行处理 例如 创建默认用户
        SysUserVo user = userService.selectUserByOpenId(openid);
        VisitorLoginUserDto loginUser = new VisitorLoginUserDto();
        if (ObjectUtil.isNull(user)) {
            SysUserBo sysUser = new SysUserBo();
            String name = "用户" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
            // 设置默认用户名
            sysUser.setUserName(name);
            // 设置默认昵称
            sysUser.setNickName(name);
            // 设置默认密码
            sysUser.setPassword(BCrypt.hashpw("123456"));
            // 设置微信openId
            sysUser.setOpenId(openid);
            // 设置用户类型
            sysUser.setUserType(UserType.SYS_USER.getUserType());
            // 设置默认余额
            sysUser.setUserBalance(NumberUtils.toDouble("1"));
            // 注册用户,设置默认租户为0
            userService.registerUser(sysUser, "0");
            // 构建登录用户信息
            loginUser.setUserId(sysUser.getUserId());
            loginUser.setUsername(sysUser.getUserName());
            loginUser.setUserType(UserType.SYS_USER.getUserType());
            loginUser.setOpenid(openid);
        } else {
            // 此处可根据登录用户的数据不同 自行创建 loginUser
            loginUser.setUserId(user.getUserId());
            loginUser.setUsername(user.getUserName());
            loginUser.setUserType(UserType.SYS_USER.getUserType());
            loginUser.setOpenid(openid);
        }
        SaLoginParameter model = new SaLoginParameter();
        model.setDeviceType(client.getDeviceType());
        // 自定义分配 不同用户体系 不同 token 授权时间 不设置默认走全局 yml 配置
        // 例如: 后台用户30分钟过期 app用户1天过期
        model.setTimeout(client.getTimeout());
        model.setActiveTimeout(client.getActiveTimeout());
        model.setExtra(LoginHelper.CLIENT_KEY, client.getClientId());
        // 生成token
        LoginHelper.login(loginUser, model);

        sysLoginService.recordLogininfor(loginUser.getTenantId(), loginUser.getUsername(), Constants.LOGIN_SUCCESS, MessageUtils.message("user.login.success"));

        loginUser.setToken(StpUtil.getTokenValue());

        return loginUser;
    }



}
